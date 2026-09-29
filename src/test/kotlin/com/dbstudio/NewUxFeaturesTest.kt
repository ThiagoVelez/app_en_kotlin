package com.dbstudio

import com.dbstudio.data.RecentDatabase
import com.dbstudio.data.RecentDatabasesManager
import com.dbstudio.model.*
import com.dbstudio.viewmodel.DiagramViewModel
import com.dbstudio.viewmodel.SqlQueryViewModel
import java.io.File
import kotlin.test.*

class NewUxFeaturesTest {

    @Test
    fun testRecentDatabasesManager() {
        val tempDb = File.createTempFile("test_recent_db", ".sqlite")
        try {
            RecentDatabasesManager.recordDatabase(tempDb.absolutePath, DatabaseType.SQLITE)
            val list = RecentDatabasesManager.getRecentDatabases()
            assertTrue(list.any { it.path == tempDb.absolutePath })
        } finally {
            RecentDatabasesManager.removeDatabase(tempDb.absolutePath)
            tempDb.delete()
        }
    }

    @Test
    fun testTableDdlGeneration() {
        val columns = listOf(
            Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
            Column("nombre", ColumnType.VARCHAR, length = 100, isNullable = false),
            Column("email", ColumnType.VARCHAR, length = 150, isUnique = true),
            Column("activo", ColumnType.BOOLEAN, defaultValue = "1")
        )
        val table = TableInfo("usuarios", columns)

        val sqliteDdl = table.toCreateSqliteSql()
        assertTrue(sqliteDdl.contains("CREATE TABLE IF NOT EXISTS \"usuarios\""))
        assertTrue(sqliteDdl.contains("\"id\" INTEGER PRIMARY KEY AUTOINCREMENT"))
        assertTrue(sqliteDdl.contains("\"nombre\" VARCHAR(100) NOT NULL"))

        val accessDdl = table.toCreateAccessSql()
        assertTrue(accessDdl.contains("CREATE TABLE [usuarios]"))
        assertTrue(accessDdl.contains("[id] COUNTER PRIMARY KEY"))
        assertTrue(accessDdl.contains("[nombre] VARCHAR(100) NOT NULL"))
    }

    @Test
    fun testDiagramAutoLayout() {
        val vm = DiagramViewModel()
        val tables = listOf(
            TableInfo("clientes", listOf(Column("id", ColumnType.INTEGER), Column("nombre", ColumnType.VARCHAR))),
            TableInfo("pedidos", listOf(Column("id", ColumnType.INTEGER), Column("cliente_id", ColumnType.INTEGER))),
            TableInfo("productos", listOf(Column("id", ColumnType.INTEGER), Column("precio", ColumnType.DECIMAL))),
            TableInfo("detalles", listOf(Column("id", ColumnType.INTEGER), Column("pedido_id", ColumnType.INTEGER)))
        )
        vm.autoLayout(tables)
        val pos = vm.entityPositions
        assertEquals(4, pos.size)
        // Verify distinct positions (no table exactly on top of another)
        val uniqueOffsets = pos.values.toSet()
        assertEquals(4, uniqueOffsets.size)
    }

    @Test
    fun testSqlQueryHistoryTracking() {
        val vm = SqlQueryViewModel()
        vm.setQuery("SELECT * FROM test;")
        assertEquals("SELECT * FROM test;", vm.queryText)
        assertTrue(vm.queryHistory.isEmpty())
    }
}
