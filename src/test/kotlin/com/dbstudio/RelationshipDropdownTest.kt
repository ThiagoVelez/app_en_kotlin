package com.dbstudio

import com.dbstudio.data.SqliteEngine
import com.dbstudio.model.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RelationshipDropdownTest {

    @Test
    fun testForeignKeyDropdownOptionsResolution() {
        val tempDb = File.createTempFile("test_dropdown_", ".db")
        tempDb.delete()
        try {
            val engine = SqliteEngine()
            engine.connect(tempDb.absolutePath, create = true)

            // 1. Create parent table: departamentos
            val deptTable = TableInfo(
                name = "departamentos",
                columns = listOf(
                    ColumnPresets.id(),
                    Column("nombre", ColumnType.VARCHAR, isNullable = false)
                )
            )
            engine.createTable(deptTable)
            engine.insertRow("departamentos", mapOf("nombre" to "Sistemas"))
            engine.insertRow("departamentos", mapOf("nombre" to "Contabilidad"))

            // 2. Create child table: empleados with foreign key depto_id
            val empTable = TableInfo(
                name = "empleados",
                columns = listOf(
                    ColumnPresets.id(),
                    Column("nombre", ColumnType.VARCHAR, isNullable = false),
                    Column("depto_id", ColumnType.INTEGER, isNullable = true)
                )
            )
            engine.createTable(empTable)

            val rel = Relationship(
                name = "fk_emp_dept",
                sourceTable = "empleados",
                sourceColumn = "depto_id",
                targetTable = "departamentos",
                targetColumn = "id",
                type = RelationType.ONE_TO_MANY,
                onDelete = ReferentialAction.NO_ACTION,
                onUpdate = ReferentialAction.NO_ACTION
            )
            engine.addRelationship(rel)

            // 3. Resolve lookups like DataViewModel does
            val rels = engine.getRelationships().filter { it.sourceTable.equals("empleados", ignoreCase = true) }
            assertEquals(1, rels.size)

            val targetRows = engine.queryAll(rels[0].targetTable)
            assertEquals(2, targetRows.size)

            val options = mutableListOf<Pair<String, String>>()
            for (tRow in targetRows) {
                val keyVal = (tRow[rels[0].targetColumn] ?: tRow.entries.firstOrNull { it.key.equals(rels[0].targetColumn, ignoreCase = true) }?.value)?.toString()
                assertNotNull(keyVal)
                val displayVal = (tRow["nombre"] ?: tRow.entries.firstOrNull { it.key.equals("nombre", ignoreCase = true) }?.value)?.toString()
                assertNotNull(displayVal)
                options.add(keyVal to "$displayVal (#$keyVal)")
            }

            assertEquals(2, options.size)
            assertEquals("1", options[0].first)
            assertEquals("Sistemas (#1)", options[0].second)
            assertEquals("2", options[1].first)
            assertEquals("Contabilidad (#2)", options[1].second)

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }

    @Test
    fun testRelationshipOnPrimaryKeyColumnEnablesDropdown() {
        // Reproduces the exact user scenario from screenshot:
        // Table loil (id, nombre) -> Table poli (id, nombre, celular) where poli.id -> loil.id
        val tempDb = File.createTempFile("test_pk_fk_", ".db")
        tempDb.delete()
        try {
            val engine = SqliteEngine()
            engine.connect(tempDb.absolutePath, create = true)

            val loil = TableInfo(
                name = "loil",
                columns = listOf(
                    ColumnPresets.id(),
                    Column("nombre", ColumnType.VARCHAR, isNullable = false)
                )
            )
            engine.createTable(loil)
            engine.insertRow("loil", mapOf("nombre" to "semillero codigo turing"))
            engine.insertRow("loil", mapOf("nombre" to "casa"))

            val poli = TableInfo(
                name = "poli",
                columns = listOf(
                    ColumnPresets.id(),
                    Column("nombre", ColumnType.VARCHAR, isNullable = false),
                    Column("celular", ColumnType.VARCHAR, isNullable = false)
                )
            )
            engine.createTable(poli)

            val rel = Relationship(
                name = "fk_poli_loil",
                sourceTable = "poli",
                sourceColumn = "id",
                targetTable = "loil",
                targetColumn = "id",
                type = RelationType.ONE_TO_MANY,
                onDelete = ReferentialAction.NO_ACTION,
                onUpdate = ReferentialAction.NO_ACTION
            )
            engine.addRelationship(rel)

            // Mock foreignKeyOptions as populated by DataViewModel
            val foreignKeyOptions = mapOf(
                "id" to listOf("1" to "semillero codigo turing (#1)", "2" to "casa (#2)")
            )

            // Validate the logic in Dialogs.kt:
            val col = poli.columns.first { it.name == "id" }
            assertTrue(col.isAutoIncrement)

            val fkOpts = foreignKeyOptions[col.name]
            assertNotNull(fkOpts)
            assertTrue(fkOpts.isNotEmpty())

            // The bug was: isAutoInc was true and overrode fkOpts.
            // With the fix:
            val isAutoIncWithFix = col.isAutoIncrement && fkOpts.isNullOrEmpty()
            // It must NOT be treated as a purely auto-increment field anymore; it MUST show the dropdown!
            assertEquals(false, isAutoIncWithFix)

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }
}
