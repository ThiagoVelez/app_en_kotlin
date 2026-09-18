package com.dbstudio

import com.dbstudio.data.SqliteEngine
import com.dbstudio.model.Column
import com.dbstudio.model.ColumnType
import com.dbstudio.model.TableInfo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqlQueryEngineTest {

    @Test
    fun testExecuteQuerySelectAndDml() {
        val tempDbFile = File.createTempFile("sql_query_test_", ".db")
        tempDbFile.deleteOnExit()
        val engine = SqliteEngine()
        try {
            engine.connect(tempDbFile.absolutePath, create = true)

            // Create table
            val table = TableInfo(
                name = "productos",
                columns = listOf(
                    Column(name = "id", type = ColumnType.AUTOINCREMENT, isPrimaryKey = true),
                    Column(name = "nombre", type = ColumnType.TEXT, isNullable = false),
                    Column(name = "precio", type = ColumnType.DECIMAL, defaultValue = "0.0")
                )
            )
            engine.createTable(table)

            // Insert rows with executeQuery
            val dmlResult = engine.executeQuery("INSERT INTO productos (nombre, precio) VALUES ('Laptop', 1200.50), ('Mouse', 25.0)")
            assertFalse(dmlResult.isSelect)
            assertEquals(2, dmlResult.affectedRows)

            // Select rows with executeQuery
            val selectResult = engine.executeQuery("SELECT id, nombre, precio FROM productos ORDER BY id ASC")
            assertTrue(selectResult.isSelect)
            assertEquals(listOf("id", "nombre", "precio"), selectResult.columns)
            assertEquals(2, selectResult.rows.size)
            assertEquals("Laptop", selectResult.rows[0][1])
            assertEquals("Mouse", selectResult.rows[1][1])
            assertTrue(selectResult.executionTimeMs >= 0)
        } finally {
            engine.disconnect()
            tempDbFile.delete()
        }
    }
}
