package com.dbstudio

import com.dbstudio.data.SqliteEngine
import com.dbstudio.model.Column
import com.dbstudio.model.ColumnPresets
import com.dbstudio.model.ColumnType
import com.dbstudio.model.TableInfo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqliteNotNullMigrationTest {

    @Test
    fun testAddNotNullColumnsToTableWithExistingData() {
        val tempDb = File.createTempFile("test_sqlite_", ".db")
        tempDb.delete()
        try {
            val engine = SqliteEngine()
            engine.connect(tempDb.absolutePath, create = true)

            // Step 1: Create initial table with id and fecha_creacion
            val initialTable = TableInfo(
                name = "materia",
                columns = listOf(
                    ColumnPresets.id(),
                    ColumnPresets.fechaCreacion()
                )
            )
            engine.createTable(initialTable)

            // Step 2: Insert two rows
            engine.insertRow("materia", mapOf("fecha_creacion" to "2026-09-17 12:00:00"))
            engine.insertRow("materia", mapOf("fecha_creacion" to "2026-09-17 12:05:00"))

            val initialRows = engine.queryAll("materia")
            assertEquals(2, initialRows.size)

            // Step 3: Update schema adding nombre (NOT NULL), cedula (NOT NULL, UNIQUE), apellido (NOT NULL)
            val updatedColumns = listOf(
                ColumnPresets.id(),
                ColumnPresets.fechaCreacion(),
                Column(name = "nombre", type = ColumnType.TEXT, isNullable = false),
                Column(name = "cedula", type = ColumnType.LONG, isNullable = false, isUnique = true),
                Column(name = "apellido", type = ColumnType.TEXT, isNullable = false)
            )

            // This should NOT throw [SQLITE_CONSTRAINT_NOTNULL]!
            engine.updateTableSchema("materia", updatedColumns)

            // Step 4: Verify data is preserved and columns exist
            val newInfo = engine.getTableInfo("materia")
            assertEquals(5, newInfo.columns.size)
            val rowsAfter = engine.queryAll("materia")
            assertEquals(2, rowsAfter.size)
            assertTrue(rowsAfter[0].containsKey("nombre"))
            assertTrue(rowsAfter[0].containsKey("cedula"))

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }
}
