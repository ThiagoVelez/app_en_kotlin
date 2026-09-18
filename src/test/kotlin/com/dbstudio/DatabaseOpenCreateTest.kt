package com.dbstudio

import com.dbstudio.data.AccessEngine
import com.dbstudio.data.SqliteEngine
import com.dbstudio.model.ColumnPresets
import com.dbstudio.model.DatabaseType
import com.dbstudio.model.TableInfo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseOpenCreateTest {

    @Test
    fun testDatabaseTypeExtensions() {
        assertEquals(DatabaseType.SQLITE, DatabaseType.fromExtension("db"))
        assertEquals(DatabaseType.SQLITE, DatabaseType.fromExtension("sqlite"))
        assertEquals(DatabaseType.SQLITE, DatabaseType.fromExtension("sqlite3"))
        assertEquals(DatabaseType.ACCESS, DatabaseType.fromExtension("accdb"))
        assertEquals(DatabaseType.ACCESS, DatabaseType.fromExtension("mdb"))
    }

    @Test
    fun testCreateAndOpenSqliteDatabase() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "db studio test folder")
        tempDir.mkdirs()
        val dbFile = File(tempDir, "test_sqlite_db.db")
        if (dbFile.exists()) dbFile.delete()

        try {
            // Create
            val createEngine = SqliteEngine()
            createEngine.connect(dbFile.absolutePath, create = true)
            assertTrue(createEngine.isConnected)

            val testTable = TableInfo(
                name = "articulos",
                columns = listOf(
                    ColumnPresets.id(),
                    ColumnPresets.nombre(),
                    ColumnPresets.fechaCreacion()
                )
            )
            createEngine.createTable(testTable)
            assertEquals(listOf("articulos"), createEngine.getTableNames())
            createEngine.disconnect()

            // Open existing
            val openEngine = SqliteEngine()
            openEngine.connect(dbFile.absolutePath, create = false)
            assertTrue(openEngine.isConnected)
            assertEquals(listOf("articulos"), openEngine.getTableNames())
            val info = openEngine.getTableInfo("articulos")
            assertEquals(3, info.columns.size)
            openEngine.disconnect()
        } finally {
            dbFile.delete()
            tempDir.delete()
        }
    }

    @Test
    fun testCreateAndOpenAccessDatabase() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "db studio test folder")
        tempDir.mkdirs()
        val accdbFile = File(tempDir, "test_access_db.accdb")
        if (accdbFile.exists()) accdbFile.delete()

        try {
            // Create
            val createEngine = AccessEngine()
            createEngine.connect(accdbFile.absolutePath, create = true)
            assertTrue(createEngine.isConnected)

            val testTable = TableInfo(
                name = "clientes",
                columns = listOf(
                    ColumnPresets.id(),
                    ColumnPresets.nombre(),
                    ColumnPresets.fechaCreacion()
                )
            )
            createEngine.createTable(testTable)
            assertTrue(createEngine.getTableNames().contains("clientes"))
            createEngine.disconnect()

            // Open existing
            val openEngine = AccessEngine()
            openEngine.connect(accdbFile.absolutePath, create = false)
            assertTrue(openEngine.isConnected)
            assertTrue(openEngine.getTableNames().contains("clientes"))
            val info = openEngine.getTableInfo("clientes")
            assertNotNull(info.columns.find { it.name == "nombre" })
            openEngine.disconnect()
        } finally {
            accdbFile.delete()
            tempDir.delete()
        }
    }
}
