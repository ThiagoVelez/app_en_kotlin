package com.dbstudio.data

import com.dbstudio.model.Column
import com.dbstudio.model.ColumnPresets
import com.dbstudio.model.ColumnType
import com.dbstudio.model.TableInfo
import org.apache.poi.ss.usermodel.*
import java.io.File

/**
 * Result of an Excel import operation.
 */
data class ExcelImportResult(
    val tablesImported: List<String>,
    val totalRowsImported: Int,
    val errors: List<String> = emptyList()
)

/**
 * Utility to convert an Excel workbook (.xlsx or .xls) into tables and records in a database.
 * Each sheet in the workbook is imported as an individual table.
 */
object ExcelImporter {

    /**
     * Imports all sheets from [inputFile] into the given [engine].
     */
    fun importDatabase(engine: DatabaseEngine, inputFile: File): ExcelImportResult {
        if (!inputFile.exists()) {
            throw IllegalArgumentException("El archivo Excel no existe: ${inputFile.absolutePath}")
        }

        val importedTables = mutableListOf<String>()
        var totalRows = 0
        val errors = mutableListOf<String>()

        val workbook = WorkbookFactory.create(inputFile)
        val dataFormatter = DataFormatter()

        try {
            val existingTableNames = engine.getTableNames().map { it.lowercase() }.toSet()

            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                val rawSheetName = sheet.sheetName?.trim() ?: continue
                if (rawSheetName.isBlank()) continue

                // Sanitize table name
                var cleanTableName = rawSheetName.replace(Regex("[^a-zA-Z0-9_]"), "_")
                if (cleanTableName.firstOrNull()?.isDigit() == true) {
                    cleanTableName = "tbl_$cleanTableName"
                }
                if (cleanTableName.isBlank()) cleanTableName = "tabla_${sheetIndex + 1}"

                if (sheet.physicalNumberOfRows < 1) {
                    // Empty sheet, skip
                    continue
                }

                val headerRow = sheet.getRow(sheet.firstRowNum) ?: continue
                val columnNames = mutableListOf<String>()
                val colIndices = mutableListOf<Int>()

                for (cellIndex in headerRow.firstCellNum until headerRow.lastCellNum) {
                    val cell = headerRow.getCell(cellIndex)
                    val rawName = if (cell != null) dataFormatter.formatCellValue(cell).trim() else ""
                    if (rawName.isNotBlank()) {
                        var safeCol = rawName.replace(Regex("[^a-zA-Z0-9_]"), "_")
                        if (safeCol.firstOrNull()?.isDigit() == true) {
                            safeCol = "col_$safeCol"
                        }
                        if (safeCol.isBlank()) safeCol = "col_${cellIndex + 1}"

                        // Avoid duplicate column names
                        var finalColName = safeCol
                        var counter = 2
                        while (finalColName.lowercase() in columnNames.map { it.lowercase() }) {
                            finalColName = "${safeCol}_$counter"
                            counter++
                        }
                        columnNames.add(finalColName)
                        colIndices.add(cellIndex)
                    }
                }

                if (columnNames.isEmpty()) {
                    continue
                }

                // Sample data rows to infer column types
                val colTypes = mutableListOf<ColumnType>()
                val startDataRow = sheet.firstRowNum + 1
                val endDataRow = minOf(sheet.lastRowNum, startDataRow + 100)

                for (idx in colIndices.indices) {
                    val colIdx = colIndices[idx]
                    var hasNumeric = false
                    var hasDecimal = false
                    var hasDate = false
                    var hasBoolean = false
                    var hasString = false

                    for (r in startDataRow..endDataRow) {
                        val row = sheet.getRow(r) ?: continue
                        val cell = row.getCell(colIdx) ?: continue
                        when (cell.cellType) {
                            CellType.BOOLEAN -> hasBoolean = true
                            CellType.NUMERIC -> {
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    hasDate = true
                                } else {
                                    hasNumeric = true
                                    val numVal = cell.numericCellValue
                                    if (numVal != Math.floor(numVal)) {
                                        hasDecimal = true
                                    }
                                }
                            }
                            CellType.STRING -> {
                                val s = cell.stringCellValue.trim()
                                if (s.isNotEmpty()) {
                                    if (s.equals("true", true) || s.equals("false", true)) {
                                        hasBoolean = true
                                    } else if (s.toDoubleOrNull() != null) {
                                        hasNumeric = true
                                        if (s.contains(".") || s.contains(",")) hasDecimal = true
                                    } else {
                                        hasString = true
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    val inferredType = when {
                        hasString -> ColumnType.VARCHAR
                        hasDate -> ColumnType.DATETIME
                        hasDecimal -> ColumnType.REAL
                        hasNumeric -> ColumnType.INTEGER
                        hasBoolean -> ColumnType.BOOLEAN
                        else -> ColumnType.VARCHAR
                    }
                    colTypes.add(inferredType)
                }

                // Check if table has an ID / PK
                val hasIdCol = columnNames.any { it.equals("id", ignoreCase = true) }
                val tableColumns = mutableListOf<Column>()

                if (!hasIdCol) {
                    tableColumns.add(ColumnPresets.id())
                }

                columnNames.forEachIndexed { i, name ->
                    val isId = name.equals("id", ignoreCase = true)
                    tableColumns.add(
                        Column(
                            name = name,
                            type = if (isId) ColumnType.AUTOINCREMENT else colTypes[i],
                            isPrimaryKey = isId,
                            isAutoIncrement = isId,
                            isNullable = !isId,
                            length = if (colTypes[i] == ColumnType.VARCHAR) 255 else null
                        )
                    )
                }

                // Create table if it doesn't already exist
                val isNewTable = cleanTableName.lowercase() !in existingTableNames
                if (isNewTable) {
                    try {
                        engine.createTable(TableInfo(cleanTableName, tableColumns))
                    } catch (e: Exception) {
                        errors.add("Error al crear la tabla '$cleanTableName': ${e.message}")
                        continue
                    }
                }

                // Insert rows
                var rowsInTable = 0
                for (r in startDataRow..sheet.lastRowNum) {
                    val row = sheet.getRow(r) ?: continue
                    val values = mutableMapOf<String, String?>()
                    var rowHasAnyData = false

                    columnNames.forEachIndexed { i, colName ->
                        val colIdx = colIndices[i]
                        val cell = row.getCell(colIdx)
                        if (cell != null) {
                            val strVal = when (cell.cellType) {
                                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                CellType.NUMERIC -> {
                                    if (DateUtil.isCellDateFormatted(cell)) {
                                        val d = cell.dateCellValue
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                        sdf.format(d)
                                    } else {
                                        val d = cell.numericCellValue
                                        if (d == Math.floor(d) && !d.isInfinite() && d in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
                                            d.toLong().toString()
                                        } else {
                                            d.toString()
                                        }
                                    }
                                }
                                CellType.STRING -> cell.stringCellValue.trim()
                                CellType.FORMULA -> {
                                    try {
                                        dataFormatter.formatCellValue(cell).trim()
                                    } catch (_: Exception) {
                                        ""
                                    }
                                }
                                else -> ""
                            }
                            if (strVal.isNotBlank()) {
                                rowHasAnyData = true
                                values[colName] = strVal
                            }
                        }
                    }

                    if (rowHasAnyData && values.isNotEmpty()) {
                        try {
                            engine.insertRow(cleanTableName, values)
                            rowsInTable++
                            totalRows++
                        } catch (e: Exception) {
                            errors.add("Fila $r en '$cleanTableName': ${e.message}")
                        }
                    }
                }

                importedTables.add(cleanTableName)
            }
        } finally {
            workbook.close()
        }

        return ExcelImportResult(
            tablesImported = importedTables,
            totalRowsImported = totalRows,
            errors = errors
        )
    }
}
