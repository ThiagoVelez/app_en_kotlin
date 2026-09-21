package com.dbstudio

import com.dbstudio.data.AccessEngine
import com.dbstudio.data.SqliteEngine
import com.dbstudio.model.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RelationshipDataLossInvestigationTest {



    @Test
    fun testSqliteRelationshipDataPreservation() {
        val tempDb = File.createTempFile("test_rel_", ".db")
        tempDb.delete()
        try {
            val engine = SqliteEngine()
            engine.connect(tempDb.absolutePath, create = true)

            // Table 1: clientes (id, nombre, celular)
            val clientesTable = TableInfo(
                name = "clientes",
                columns = listOf(
                    ColumnPresets.id(),
                    Column(name = "nombre", type = ColumnType.TEXT),
                    Column(name = "celular", type = ColumnType.TEXT)
                )
            )
            engine.createTable(clientesTable)

            // Insert rows with nombre and celular
            engine.insertRow("clientes", mapOf("nombre" to "Juan Perez", "celular" to "3001234567"))
            engine.insertRow("clientes", mapOf("nombre" to "Maria Gomez", "celular" to "3109876543"))

            // Table 2: pedidos (id, total)
            val pedidosTable = TableInfo(
                name = "pedidos",
                columns = listOf(
                    ColumnPresets.id(),
                    Column(name = "total", type = ColumnType.DECIMAL)
                )
            )
            engine.createTable(pedidosTable)
            engine.insertRow("pedidos", mapOf("total" to "150.00"))

            println("--- Initial Clientes rows in SQLite ---")
            val initialRows = engine.queryAll("clientes")
            println(initialRows)
            assertEquals("Juan Perez", initialRows[0]["nombre"])
            assertEquals("3001234567", initialRows[0]["celular"])

            // Scenario 1: What happens if user relates clientes.nombre -> pedidos.id?
            println("--- Testing addRelationship with sourceColumn = nombre ---")
            try {
                val rel = Relationship(
                    name = "fk_clientes_pedidos",
                    sourceTable = "clientes",
                    sourceColumn = "nombre",
                    targetTable = "pedidos",
                    targetColumn = "id"
                )
                engine.addRelationship(rel)
                val rowsAfter = engine.queryAll("clientes")
                println("Rows after relating nombre -> pedidos.id: $rowsAfter")
                assertEquals("Juan Perez", rowsAfter[0]["nombre"])
                assertEquals("3001234567", rowsAfter[0]["celular"])
            } catch (e: Exception) {
                println("Exception relating nombre: ${e.message}")
            }

            // Scenario 2: What happens if user adds a new column 'id_pedido' via updateTableSchema or addColumn and relates?
            val updatedCols = clientesTable.columns + Column(name = "id_pedido", type = ColumnType.INTEGER)
            engine.updateTableSchema("clientes", updatedCols)
            val rowsAfterCol = engine.queryAll("clientes")
            println("Rows after adding column id_pedido: $rowsAfterCol")
            assertEquals("Juan Perez", rowsAfterCol[0]["nombre"])
            assertEquals("3001234567", rowsAfterCol[0]["celular"])

            val rel2 = Relationship(
                name = "fk_clientes_id_pedido",
                sourceTable = "clientes",
                sourceColumn = "id_pedido",
                targetTable = "pedidos",
                targetColumn = "id"
            )
            engine.addRelationship(rel2)
            val rowsAfterRel2 = engine.queryAll("clientes")
            println("Rows after relating id_pedido -> pedidos.id: $rowsAfterRel2")
            assertEquals("Juan Perez", rowsAfterRel2[0]["nombre"])
            assertEquals("3001234567", rowsAfterRel2[0]["celular"])

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }

    @Test
    fun testAccessRelationshipDataPreservation() {
        val tempAccdb = File.createTempFile("test_rel_", ".accdb")
        tempAccdb.delete()
        try {
            val engine = AccessEngine()
            engine.connect(tempAccdb.absolutePath, create = true)

            val clientesTable = TableInfo(
                name = "clientes",
                columns = listOf(
                    ColumnPresets.id(),
                    Column(name = "nombre", type = ColumnType.TEXT),
                    Column(name = "celular", type = ColumnType.TEXT)
                )
            )
            engine.createTable(clientesTable)

            engine.insertRow("clientes", mapOf("nombre" to "Juan Perez", "celular" to "3001234567"))
            engine.insertRow("clientes", mapOf("nombre" to "Maria Gomez", "celular" to "3109876543"))

            val pedidosTable = TableInfo(
                name = "pedidos",
                columns = listOf(
                    ColumnPresets.id(),
                    Column(name = "total", type = ColumnType.DECIMAL)
                )
            )
            engine.createTable(pedidosTable)
            engine.insertRow("pedidos", mapOf("total" to "150.00"))

            val initialRows = engine.queryAll("clientes")
            println("--- Initial Clientes rows in Access ---")
            println(initialRows)
            assertEquals("Juan Perez", initialRows[0]["nombre"])
            assertEquals("3001234567", initialRows[0]["celular"])

            // What if user relates id_pedido
            val updatedCols = clientesTable.columns + Column(name = "id_pedido", type = ColumnType.INTEGER)
            engine.updateTableSchema("clientes", updatedCols)
            val rowsAfterCol = engine.queryAll("clientes")
            println("Rows after adding column id_pedido in Access: $rowsAfterCol")
            assertEquals("Juan Perez", rowsAfterCol[0]["nombre"])
            assertEquals("3001234567", rowsAfterCol[0]["celular"])

            engine.disconnect()
        } finally {
            tempAccdb.delete()
        }
    }

    @Test
    fun testAllRelationshipPermutationsInSqlite() {
        val tempDb = File.createTempFile("test_rel_perm_", ".db")
        tempDb.delete()
        try {
            val engine = SqliteEngine()
            engine.connect(tempDb.absolutePath, create = true)

            // Table 1 with data
            val t1 = TableInfo(
                name = "estudiantes",
                columns = listOf(
                    ColumnPresets.id(),
                    Column(name = "nombre", type = ColumnType.TEXT),
                    Column(name = "celular", type = ColumnType.VARCHAR, length = 20)
                )
            )
            engine.createTable(t1)
            engine.insertRow("estudiantes", mapOf("nombre" to "Carlos", "celular" to "3001112233"))
            engine.insertRow("estudiantes", mapOf("nombre" to "Ana", "celular" to "3114445566"))

            // Table 2 with data
            val t2 = TableInfo(
                name = "materias",
                columns = listOf(
                    ColumnPresets.id(),
                    Column(name = "materia", type = ColumnType.TEXT)
                )
            )
            engine.createTable(t2)
            engine.insertRow("materias", mapOf("materia" to "Programacion"))

            println("--- Case A: sourceTable=estudiantes, sourceColumn=celular, targetTable=materias, targetColumn=id ---")
            val relA = Relationship(
                name = "fk_est_cel_mat",
                sourceTable = "estudiantes",
                sourceColumn = "celular",
                targetTable = "materias",
                targetColumn = "id"
            )
            engine.addRelationship(relA)
            val rowsA = engine.queryAll("estudiantes")
            println("Rows A: $rowsA")
            assertEquals("Carlos", rowsA[0]["nombre"])
            assertEquals("3001112233", rowsA[0]["celular"])

            println("--- Case B: What does DataViewModel do with these rows? ---")
            // Simulate DataViewModel lookup
            val rels = engine.getRelationships().filter { it.sourceTable == "estudiantes" }
            println("Rels found for estudiantes: $rels")
            for (rel in rels) {
                val targetRows = engine.queryAll(rel.targetTable)
                val targetInfo = engine.getTableInfo(rel.targetTable)
                val displayCol = targetInfo.columns.firstOrNull { col ->
                    val n = col.name.lowercase()
                    (n.contains("nombre") || n.contains("name") || n.contains("titulo") || n.contains("title") ||
                     n.contains("materia") || n.contains("descripcion") || n.contains("desc")) &&
                            col.type != ColumnType.AUTOINCREMENT
                }?.name ?: rel.targetColumn

                println("Display col in target: $displayCol")
                val colLookup = mutableMapOf<String, String>()
                for (tRow in targetRows) {
                    val keyVal = tRow[rel.targetColumn]?.toString() ?: continue
                    val displayVal = tRow[displayCol]?.toString() ?: keyVal
                    colLookup[keyVal] = displayVal
                }
                println("colLookup for ${rel.sourceColumn}: $colLookup")
                for (r in rowsA) {
                    val rawVal = r[rel.sourceColumn]
                    val resolved = colLookup[rawVal?.toString()]
                    println("Raw val: '$rawVal', Resolved FK: '$resolved'")
                }
            }

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }

    @Test
    fun testCaseSensitivityInUpdateTableSchema() {
        val tempDb = File.createTempFile("test_case_", ".db")
        tempDb.delete()
        try {
            val engine = SqliteEngine()
            engine.connect(tempDb.absolutePath, create = true)

            // Table created with Capitalized names: "Nombre", "Celular"
            val t1 = TableInfo(
                name = "estudiantes",
                columns = listOf(
                    ColumnPresets.id(),
                    Column(name = "Nombre", type = ColumnType.TEXT),
                    Column(name = "Celular", type = ColumnType.VARCHAR, length = 20)
                )
            )
            engine.createTable(t1)
            engine.insertRow("estudiantes", mapOf("Nombre" to "Carlos", "Celular" to "3001112233"))

            val rowsBefore = engine.queryAll("estudiantes")
            println("Rows before update: $rowsBefore")

            // User modifies table or presets uses lowercase "nombre", "celular", plus adds "id_materia"
            val newCols = listOf(
                ColumnPresets.id(),
                Column(name = "nombre", type = ColumnType.TEXT),
                Column(name = "celular", type = ColumnType.VARCHAR, length = 20),
                Column(name = "id_materia", type = ColumnType.INTEGER)
            )

            // Let's see if updateTableSchema wipes the data because "Nombre" != "nombre"!
            engine.updateTableSchema("estudiantes", newCols)
            val rowsAfter = engine.queryAll("estudiantes")
            println("Rows after updateTableSchema: $rowsAfter")

            // Check if data was preserved or erased!
            println("nombre value: ${rowsAfter[0]["nombre"]}")
            println("celular value: ${rowsAfter[0]["celular"]}")

            assertEquals("Carlos", rowsAfter[0]["nombre"], "Data in 'nombre' should not be wiped by case difference!")
            assertEquals("3001112233", rowsAfter[0]["celular"], "Data in 'celular' should not be wiped by case difference!")

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }

    @Test
    fun testAddRelationshipFailureDoesNotDropTable() {
        val tempDb = File.createTempFile("test_fail_", ".db")
        tempDb.delete()
        try {
            val engine = SqliteEngine()
            engine.connect(tempDb.absolutePath, create = true)

            val t1 = TableInfo(
                name = "estudiantes",
                columns = listOf(
                    ColumnPresets.id(),
                    Column(name = "nombre", type = ColumnType.TEXT),
                    Column(name = "celular", type = ColumnType.VARCHAR, length = 20)
                )
            )
            engine.createTable(t1)
            engine.insertRow("estudiantes", mapOf("nombre" to "Carlos", "celular" to "3001112233"))

            // Try to relate with an invalid relationship that causes an error
            // (or target table doesn't have a matching column or type)
            val invalidRel = Relationship(
                name = "fk_invalid",
                sourceTable = "estudiantes",
                sourceColumn = "celular",
                targetTable = "no_existe",
                targetColumn = "id"
            )
            try {
                engine.addRelationship(invalidRel)
            } catch (e: Exception) {
                println("Caught exception: ${e.message}")
            }

            // Check if estudiantes table and its data STILL EXIST!
            val tables = engine.getTableNames()
            println("Tables remaining: $tables")
            val rows = try { engine.queryAll("estudiantes") } catch (e: Exception) { emptyList() }
            println("Rows remaining: $rows")
            assertTrue(tables.contains("estudiantes"), "Table 'estudiantes' should NOT be dropped on relationship failure!")
            assertEquals(1, rows.size, "Row data should be preserved!")

            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }
}
