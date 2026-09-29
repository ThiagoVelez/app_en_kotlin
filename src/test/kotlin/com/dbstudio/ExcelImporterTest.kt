package com.dbstudio

import com.dbstudio.data.AccessEngine
import com.dbstudio.data.ExcelImporter
import com.dbstudio.data.SqliteEngine
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExcelImporterTest {

    private fun createTestExcelFile(): File {
        val tempXlsx = File.createTempFile("test_import_", ".xlsx")
        tempXlsx.deleteOnExit()

        XSSFWorkbook().use { workbook ->
            // Sheet 1: semilleros (has explicit id)
            val sheetSemilleros = workbook.createSheet("semilleros")
            val hRow0 = sheetSemilleros.createRow(0)
            hRow0.createCell(0).setCellValue("id")
            hRow0.createCell(1).setCellValue("nombre")
            hRow0.createCell(2).setCellValue("descripcion")

            val r1 = sheetSemilleros.createRow(1)
            r1.createCell(0).setCellValue(1.0)
            r1.createCell(1).setCellValue("Semillero IA")
            r1.createCell(2).setCellValue("Investigación en Inteligencia Artificial")

            val r2 = sheetSemilleros.createRow(2)
            r2.createCell(0).setCellValue(2.0)
            r2.createCell(1).setCellValue("Semillero Robótica")
            r2.createCell(2).setCellValue("Desarrollo de robótica móvil")

            // Sheet 2: estudiantes (no id column - should auto-create id PK)
            val sheetEstudiantes = workbook.createSheet("estudiantes")
            val hRow1 = sheetEstudiantes.createRow(0)
            hRow1.createCell(0).setCellValue("nombre")
            hRow1.createCell(1).setCellValue("cedula")
            hRow1.createCell(2).setCellValue("correo")
            hRow1.createCell(3).setCellValue("semillero_id")

            val er1 = sheetEstudiantes.createRow(1)
            er1.createCell(0).setCellValue("Thiago Vélez")
            er1.createCell(1).setCellValue("1002345678")
            er1.createCell(2).setCellValue("thiago@example.com")
            er1.createCell(3).setCellValue(1.0)

            val er2 = sheetEstudiantes.createRow(2)
            er2.createCell(0).setCellValue("Ana Martínez")
            er2.createCell(1).setCellValue("1009876543")
            er2.createCell(2).setCellValue("ana@example.com")
            er2.createCell(3).setCellValue(2.0)

            FileOutputStream(tempXlsx).use { fos ->
                workbook.write(fos)
            }
        }

        return tempXlsx
    }

    @Test
    fun testImportExcelIntoSqlite() {
        val tempXlsx = createTestExcelFile()
        val tempDb = File.createTempFile("test_sqlite_import_", ".db")
        tempDb.deleteOnExit()

        val engine = SqliteEngine()
        try {
            engine.connect(tempDb.absolutePath, create = true)

            val result = ExcelImporter.importDatabase(engine, tempXlsx)

            assertTrue(result.errors.isEmpty(), "Import errors: ${result.errors.joinToString()}")
            assertEquals(2, result.tablesImported.size)
            assertTrue(result.tablesImported.contains("semilleros"))
            assertTrue(result.tablesImported.contains("estudiantes"))
            assertEquals(4, result.totalRowsImported)

            // Verify tables in DB
            val tables = engine.getTableNames()
            assertTrue(tables.contains("semilleros"))
            assertTrue(tables.contains("estudiantes"))

            // Verify semilleros rows
            val semillerosData = engine.queryAll("semilleros", 10, 0)
            assertEquals(2, semillerosData.size)
            val firstSemillero = semillerosData[0]
            assertEquals("Semillero IA", firstSemillero["nombre"]?.toString())

            // Verify estudiantes rows & auto-created PK
            val estudiantesInfo = engine.getTableInfo("estudiantes")
            assertNotNull(estudiantesInfo)
            assertTrue(estudiantesInfo.columns.any { it.name == "id" && it.isPrimaryKey })

            val estudiantesData = engine.queryAll("estudiantes", 10, 0)
            assertEquals(2, estudiantesData.size)
            val firstEstudiante = estudiantesData[0]
            assertEquals("Thiago Vélez", firstEstudiante["nombre"]?.toString())
            assertEquals("1002345678", firstEstudiante["cedula"]?.toString())
        } finally {
            engine.disconnect()
            tempDb.delete()
            tempXlsx.delete()
        }
    }

    @Test
    fun testImportExcelIntoAccess() {
        val tempXlsx = createTestExcelFile()
        val tempAccdb = File.createTempFile("test_access_import_", ".accdb")
        tempAccdb.delete()
        tempAccdb.deleteOnExit()

        val engine = AccessEngine()
        try {
            engine.connect(tempAccdb.absolutePath, create = true)

            val result = ExcelImporter.importDatabase(engine, tempXlsx)

            assertTrue(result.errors.isEmpty(), "Import errors: ${result.errors.joinToString()}")
            assertEquals(2, result.tablesImported.size)
            assertTrue(result.tablesImported.contains("semilleros"))
            assertTrue(result.tablesImported.contains("estudiantes"))
            assertEquals(4, result.totalRowsImported)

            // Verify tables in Access
            val tables = engine.getTableNames()
            assertTrue(tables.contains("semilleros"))
            assertTrue(tables.contains("estudiantes"))

            val semillerosData = engine.queryAll("semilleros", 10, 0)
            assertEquals(2, semillerosData.size)

            val estudiantesData = engine.queryAll("estudiantes", 10, 0)
            assertEquals(2, estudiantesData.size)
        } finally {
            engine.disconnect()
            tempAccdb.delete()
            tempXlsx.delete()
        }
    }
}
