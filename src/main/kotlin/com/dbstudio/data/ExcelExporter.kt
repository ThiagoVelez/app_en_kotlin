package com.dbstudio.data

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

/**
 * Utility to export the complete database structure and records into a single Excel (.xlsx) file,
 * where each database table is represented as an independent sheet.
 */
object ExcelExporter {

    /**
     * Exports all user tables in [engine] into a single Excel (.xlsx) workbook at [outputFile].
     *
     * @param engine The active database engine (SQLite or Access).
     * @param databaseName Display name of the database.
     * @param outputFile Target .xlsx file to write.
     */
    fun exportDatabase(engine: DatabaseEngine, databaseName: String, outputFile: File) {
        val workbook = XSSFWorkbook()
        try {
            val tableNames = engine.getTableNames()

            // Header Font & Style (Excel Green theme)
            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
                fontHeightInPoints = 11.toShort()
            }

            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                fillForegroundColor = IndexedColors.SEA_GREEN.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            val dataCellStyle = workbook.createCellStyle().apply {
                verticalAlignment = VerticalAlignment.CENTER
                borderBottom = BorderStyle.HAIR
                borderTop = BorderStyle.HAIR
                borderLeft = BorderStyle.HAIR
                borderRight = BorderStyle.HAIR
            }

            if (tableNames.isEmpty()) {
                // When there are no tables, create an informational summary sheet
                val sheet = workbook.createSheet("Resumen")
                val row0 = sheet.createRow(0)
                row0.createCell(0).apply {
                    setCellValue("Base de Datos: $databaseName")
                    cellStyle = headerStyle
                }

                val row1 = sheet.createRow(2)
                row1.createCell(0).setCellValue("Esta base de datos no contiene tablas de usuario actualmente.")
                sheet.autoSizeColumn(0)
            } else {
                val usedSheetNames = mutableSetOf<String>()

                for (tableName in tableNames) {
                    // Sanitize sheet name for Excel rules (max 31 chars, no invalid chars)
                    var safeName = WorkbookUtil.createSafeSheetName(tableName)
                    if (safeName.isBlank()) safeName = "Tabla"

                    var uniqueName = safeName
                    var counter = 1
                    while (uniqueName in usedSheetNames) {
                        val suffix = "_$counter"
                        uniqueName = if (safeName.length + suffix.length > 31) {
                            safeName.take(31 - suffix.length) + suffix
                        } else {
                            safeName + suffix
                        }
                        counter++
                    }
                    usedSheetNames.add(uniqueName)

                    val sheet = workbook.createSheet(uniqueName)

                    val tableInfo = engine.getTableInfo(tableName)
                    val columns = tableInfo.columns.map { it.name }

                    // Header row
                    val headerRow = sheet.createRow(0)
                    headerRow.heightInPoints = 24f

                    columns.forEachIndexed { colIndex, colName ->
                        val cell = headerRow.createCell(colIndex)
                        cell.setCellValue(colName)
                        cell.cellStyle = headerStyle
                    }

                    // Data rows
                    val rows = engine.queryAll(tableName, limit = Int.MAX_VALUE)
                    rows.forEachIndexed { rowIndex, rowData ->
                        val dataRow = sheet.createRow(rowIndex + 1)
                        dataRow.heightInPoints = 20f

                        columns.forEachIndexed { colIndex, colName ->
                            val value = rowData[colName]
                            val cell = dataRow.createCell(colIndex)
                            cell.cellStyle = dataCellStyle

                            when (value) {
                                null -> cell.setBlank()
                                is Number -> cell.setCellValue(value.toDouble())
                                is Boolean -> cell.setCellValue(value)
                                else -> cell.setCellValue(value.toString())
                            }
                        }
                    }

                    // Auto-size columns with guardrails
                    columns.indices.forEach { colIndex ->
                        sheet.autoSizeColumn(colIndex)
                        val currentWidth = sheet.getColumnWidth(colIndex)
                        if (currentWidth < 3200) {
                            sheet.setColumnWidth(colIndex, 3200)
                        } else if (currentWidth > 18000) {
                            sheet.setColumnWidth(colIndex, 18000)
                        }
                    }
                }
            }

            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()

            FileOutputStream(outputFile).use { fos ->
                workbook.write(fos)
            }
        } finally {
            workbook.close()
        }
    }
}
