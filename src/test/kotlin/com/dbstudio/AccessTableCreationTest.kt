package com.dbstudio

import com.dbstudio.data.AccessEngine
import com.dbstudio.model.Column
import com.dbstudio.model.ColumnPresets
import com.dbstudio.model.ColumnType
import com.dbstudio.model.TableInfo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AccessTableCreationTest {

    @Test
    fun testCreateAccessTableWithFechaCreacion() {
        val table = TableInfo(
            name = "ola",
            columns = listOf(
                ColumnPresets.id(),
                ColumnPresets.fechaCreacion()
            )
        )
        val sql = table.toCreateAccessSql()
        println("Generated SQL: $sql")
        assertTrue(sql.contains("DEFAULT CURRENT_TIMESTAMP NOT NULL") || sql.contains("DEFAULT CURRENT_TIMESTAMP"))
        assertTrue(!sql.contains("NOT NULL DEFAULT"))

        // Now test actual execution in an Access database
        val tempDb = File.createTempFile("test_db_", ".accdb")
        tempDb.delete()
        try {
            val engine = AccessEngine()
            engine.connect(tempDb.absolutePath, create = true)
            engine.createTable(table)
            val info = engine.getTableInfo("ola")
            assertTrue(info.columns.any { it.name == "fecha_creacion" })
            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }

    @Test
    fun testCreateAndModifyEstudianteTable() {
        val tempDb = File.createTempFile("test_estudiante_", ".accdb")
        tempDb.delete()
        try {
            val engine = AccessEngine()
            engine.connect(tempDb.absolutePath, create = true)

            // Step 1: Initial table creation with default id
            val initialTable = TableInfo("Estudiante", listOf(ColumnPresets.id()))
            engine.createTable(initialTable)

            // Step 2: Add presets as shown in user screenshot (cedula, nombre, apellido, correo, celular, fecha_creacion)
            val updatedColumns = listOf(
                ColumnPresets.id(),
                ColumnPresets.cedula(),
                ColumnPresets.nombre(),
                ColumnPresets.apellido(),
                ColumnPresets.correo(),
                ColumnPresets.celular(),
                ColumnPresets.fechaCreacion()
            )
            engine.updateTableSchema("Estudiante", updatedColumns)

            val info = engine.getTableInfo("Estudiante")
            println("COLUMNS FOUND: " + info.columns.map { "${it.name} (type=${it.type}, pk=${it.isPrimaryKey}, ai=${it.isAutoIncrement})" })
            assertTrue(info.columns.any { it.name == "id" && it.isPrimaryKey && it.isAutoIncrement })
            assertTrue(info.columns.any { it.name == "cedula" })
            assertTrue(info.columns.any { it.name == "nombre" })
            assertTrue(info.columns.any { it.name == "apellido" })
            assertTrue(info.columns.any { it.name == "correo" })
            assertTrue(info.columns.any { it.name == "celular" })
            assertTrue(info.columns.any { it.name == "fecha_creacion" })

            // Step 3: Insert row to verify date and default value handling
            engine.insertRow("Estudiante", mapOf(
                "cedula" to "123456789",
                "nombre" to "Juan",
                "apellido" to "Perez",
                "correo" to "juan@example.com",
                "celular" to "3001234567"
            ))

            val rows = engine.queryAll("Estudiante")
            assertTrue(rows.size == 1)
            assertTrue(rows[0]["nombre"] == "Juan")

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }

    @Test
    fun testAddForeignKeyColumnAndAutoincrementSafeguard() {
        val tempDb = File.createTempFile("test_rel_fk_", ".accdb")
        tempDb.delete()
        try {
            val engine = AccessEngine()
            engine.connect(tempDb.absolutePath, create = true)

            // Create primary table "poli" with autoincrement ID
            val poliTable = TableInfo("poli", listOf(ColumnPresets.id(), ColumnPresets.nombre()))
            engine.createTable(poliTable)

            // Create secondary table "semillero"
            val semilleroTable = TableInfo("semillero", listOf(ColumnPresets.id(), ColumnPresets.nombre()))
            engine.createTable(semilleroTable)

            // Test 1: Even if ColumnType.AUTOINCREMENT is passed to addColumn, safeguard must coerce it to INTEGER and succeed
            engine.addColumn("semillero", Column(name = "poli_id", type = ColumnType.AUTOINCREMENT, isNullable = true))

            val info = engine.getTableInfo("semillero")
            val poliIdCol = info.columns.firstOrNull { it.name == "poli_id" }
            assertTrue(poliIdCol != null, "poli_id column should have been added")
            assertTrue(poliIdCol.type == ColumnType.INTEGER, "poli_id column should be INTEGER, not COUNTER")
            assertTrue(!poliIdCol.isAutoIncrement, "poli_id should not be autoincrement")

            // Test 2: Connection remains healthy and queries continue to work
            val tables = engine.getTableNames()
            assertTrue(tables.contains("poli") && tables.contains("semillero"))

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }
}
