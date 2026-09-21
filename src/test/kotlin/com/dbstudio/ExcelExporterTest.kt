package com.dbstudio

import com.dbstudio.data.ExcelExporter
import com.dbstudio.data.SqliteEngine
import com.dbstudio.model.Column
import com.dbstudio.model.ColumnType
import com.dbstudio.model.TableInfo
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExcelExporterTest {

    @Test
    fun testExportDatabaseWithMultipleTablesToExcel() {
        val tempDb = File.createTempFile("excel_export_test_", ".db")
        val tempXlsx = File.createTempFile("excel_export_out_", ".xlsx")
        tempDb.deleteOnExit()
        tempXlsx.deleteOnExit()

        val engine = SqliteEngine()
        try {
            engine.connect(tempDb.absolutePath, create = true)

            // Table 1: clientes
            val clientesTable = TableInfo(
                name = "clientes",
                columns = listOf(
                    Column(name = "id", type = ColumnType.AUTOINCREMENT, isPrimaryKey = true),
                    Column(name = "nombre", type = ColumnType.TEXT, isNullable = false),
                    Column(name = "activo", type = ColumnType.BOOLEAN, defaultValue = "1")
                )
            )
            engine.createTable(clientesTable)
            engine.insertRow("clientes", mapOf("nombre" to "Carlos Pérez", "activo" to "1"))
            engine.insertRow("clientes", mapOf("nombre" to "Ana Gómez", "activo" to "0"))

            // Table 2: productos
            val productosTable = TableInfo(
                name = "productos",
                columns = listOf(
                    Column(name = "id", type = ColumnType.AUTOINCREMENT, isPrimaryKey = true),
                    Column(name = "descripcion", type = ColumnType.TEXT, isNullable = false),
                    Column(name = "precio", type = ColumnType.DECIMAL)
                )
            )
            engine.createTable(productosTable)
            engine.insertRow("productos", mapOf("descripcion" to "Laptop Pro", "precio" to "1500.0"))

            // Export to Excel
            ExcelExporter.exportDatabase(engine, "TestDB", tempXlsx)

            // Assert file was created and is non-empty
            assertTrue(tempXlsx.exists())
            assertTrue(tempXlsx.length() > 0)

            // Read back with Apache POI to verify content
            FileInputStream(tempXlsx).use { fis ->
                val workbook = XSSFWorkbook(fis)
                assertEquals(2, workbook.numberOfSheets)

                // Verify "clientes" sheet
                val clientesSheet = workbook.getSheet("clientes")
                assertNotNull(clientesSheet, "Sheet 'clientes' should exist")
                assertEquals("id", clientesSheet.getRow(0).getCell(0).stringCellValue)
                assertEquals("nombre", clientesSheet.getRow(0).getCell(1).stringCellValue)
                assertEquals("activo", clientesSheet.getRow(0).getCell(2).stringCellValue)

                assertEquals("Carlos Pérez", clientesSheet.getRow(1).getCell(1).stringCellValue)
                assertEquals("Ana Gómez", clientesSheet.getRow(2).getCell(1).stringCellValue)

                // Verify "productos" sheet
                val productosSheet = workbook.getSheet("productos")
                assertNotNull(productosSheet, "Sheet 'productos' should exist")
                assertEquals("id", productosSheet.getRow(0).getCell(0).stringCellValue)
                assertEquals("descripcion", productosSheet.getRow(0).getCell(1).stringCellValue)
                assertEquals("precio", productosSheet.getRow(0).getCell(2).stringCellValue)

                assertEquals("Laptop Pro", productosSheet.getRow(1).getCell(1).stringCellValue)
                assertEquals(1500.0, productosSheet.getRow(1).getCell(2).numericCellValue, 0.001)

                workbook.close()
            }
        } finally {
            engine.disconnect()
            tempDb.delete()
            tempXlsx.delete()
        }
    }

    @Test
    fun testExportEmptyDatabaseToExcel() {
        val tempDb = File.createTempFile("excel_empty_test_", ".db")
        val tempXlsx = File.createTempFile("excel_empty_out_", ".xlsx")
        tempDb.deleteOnExit()
        tempXlsx.deleteOnExit()

        val engine = SqliteEngine()
        try {
            engine.connect(tempDb.absolutePath, create = true)

            // Export empty database
            ExcelExporter.exportDatabase(engine, "EmptyDB", tempXlsx)

            assertTrue(tempXlsx.exists())
            assertTrue(tempXlsx.length() > 0)

            FileInputStream(tempXlsx).use { fis ->
                val workbook = XSSFWorkbook(fis)
                assertEquals(1, workbook.numberOfSheets)
                val sheet = workbook.getSheetAt(0)
                assertEquals("Resumen", sheet.sheetName)
                assertTrue(sheet.getRow(0).getCell(0).stringCellValue.contains("EmptyDB"))
                workbook.close()
            }
        } finally {
            engine.disconnect()
            tempDb.delete()
            tempXlsx.delete()
        }
    }
}
