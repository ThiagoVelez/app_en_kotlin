package com.dbstudio

import com.dbstudio.data.AccessEngine
import com.dbstudio.model.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AccessSchemaUpdateTest {

    @Test
    fun testUpdateTableSchemaWithRelationship() {
        val tempDb = File.createTempFile("test_access_schema_", ".accdb")
        tempDb.delete()

        val engine = AccessEngine()
        try {
            engine.connect(tempDb.absolutePath, create = true)

            // 1. Create table loil
            val loilTable = TableInfo(
                name = "loil",
                columns = listOf(
                    Column(name = "id", type = ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true),
                    Column(name = "nombre", type = ColumnType.TEXT)
                )
            )
            engine.createTable(loilTable)

            // 2. Create table poli with relationship to loil
            val poliTable = TableInfo(
                name = "poli",
                columns = listOf(
                    Column(name = "id", type = ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true),
                    Column(name = "loil_id", type = ColumnType.LONG)
                )
            )
            engine.createTable(poliTable)

            // 3. Add relationship
            engine.addRelationship(
                Relationship(
                    name = "fk_poli_loil",
                    sourceTable = "poli",
                    sourceColumn = "loil_id",
                    targetTable = "loil",
                    targetColumn = "id"
                )
            )

            // 4. Try to update schema of poli by adding 'celular' column
            val newColumns = listOf(
                Column(name = "id", type = ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true),
                Column(name = "loil_id", type = ColumnType.LONG),
                ColumnPresets.celular()
            )
            engine.updateTableSchema("poli", newColumns)

            // 5. Query data from poli and loil
            val rows = engine.queryAll("poli")
            assertTrue(rows.isEmpty())

            val info = engine.getTableInfo("poli")
            assertTrue(info.columns.any { it.name == "celular" })

            val newLoilColumns = listOf(
                Column(name = "id", type = ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true),
                Column(name = "nombre", type = ColumnType.TEXT),
                ColumnPresets.celular()
            )
            val currentLoil = engine.getTableInfo("loil")
            println("CURRENT LOIL COLS: " + currentLoil.columns.map { "${it.name}: type=${it.type}, pk=${it.isPrimaryKey}, ai=${it.isAutoIncrement}" })
            engine.updateTableSchema("loil", newLoilColumns)

            val loilInfo = engine.getTableInfo("loil")
            assertTrue(loilInfo.columns.any { it.name == "celular" })
            val loilRows = engine.queryAll("loil")
            assertTrue(loilRows.isEmpty())
        } finally {
            engine.disconnect()
            tempDb.delete()
        }
    }
}
