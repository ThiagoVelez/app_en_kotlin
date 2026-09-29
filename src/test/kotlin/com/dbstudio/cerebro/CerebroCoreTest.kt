package com.dbstudio.cerebro

import com.dbstudio.cerebro.advisor.IssueLevel
import com.dbstudio.cerebro.advisor.RelationshipAdvisor
import com.dbstudio.cerebro.advisor.SchemaAdvisor
import com.dbstudio.cerebro.advisor.TypeInferrer
import com.dbstudio.cerebro.diagnostics.ActionType
import com.dbstudio.cerebro.diagnostics.ErrorDoctor
import com.dbstudio.cerebro.generator.MockDataGenerator
import com.dbstudio.data.AccessEngine
import com.dbstudio.data.SqliteEngine
import com.dbstudio.model.Column
import com.dbstudio.model.ColumnType
import com.dbstudio.model.Relationship
import com.dbstudio.model.TableInfo
import java.io.File
import java.sql.SQLException
import kotlin.test.*

class CerebroCoreTest {

    // ── 1. ErrorDoctor Tests ──────────────────────────────────────────

    @Test
    fun testErrorDoctorFkConstraint() {
        val ex = SQLException("FOREIGN KEY constraint failed: [FK_ESTUDIANTES_SEMILLEROS]")
        val report = ErrorDoctor.diagnose(ex)

        assertEquals("Violación de Clave Foránea (Integridad Referencial)", report.title)
        assertTrue(report.suggestedActions.any { it.actionType == ActionType.CREATE_PARENT_RECORD })
    }

    @Test
    fun testErrorDoctorUniqueConstraint() {
        val ex = SQLException("UNIQUE constraint failed: estudiantes.cedula")
        val report = ErrorDoctor.diagnose(ex)

        assertEquals("Clave Primaria o Valor Único Duplicado", report.title)
        assertTrue(report.suggestedActions.any { it.actionType == ActionType.REMOVE_DUPLICATE_KEY })
    }

    @Test
    fun testErrorDoctorNotNullConstraint() {
        val ex = SQLException("NOT NULL constraint failed: clientes.nombre")
        val report = ErrorDoctor.diagnose(ex)

        assertEquals("Campo Obligatorio Vacío (NOT NULL)", report.title)
        assertTrue(report.suggestedActions.any { it.actionType == ActionType.FILL_REQUIRED_FIELD })
    }

    @Test
    fun testErrorDoctorDatabaseLocked() {
        val ex = SQLException("database is locked")
        val report = ErrorDoctor.diagnose(ex)

        assertEquals("Base de Datos Bloqueada por Otro Proceso", report.title)
        assertTrue(report.suggestedActions.any { it.actionType == ActionType.CLOSE_EXTERNAL_APP })
    }

    // ── 2. TypeInferrer Tests ─────────────────────────────────────────

    @Test
    fun testTypeInferrerPatterns() {
        // ID
        val idInf = TypeInferrer.infer("id")
        assertEquals(ColumnType.AUTOINCREMENT, idInf.type)
        assertTrue(idInf.isPrimaryKey)
        assertTrue(idInf.isAutoIncrement)

        // FK
        val fkInf = TypeInferrer.infer("semillero_id")
        assertEquals(ColumnType.INTEGER, fkInf.type)
        assertFalse(fkInf.isPrimaryKey)

        // Cedula / Phone
        val cedulaInf = TypeInferrer.infer("cedula")
        assertEquals(ColumnType.LONG, cedulaInf.type)

        // Money
        val precioInf = TypeInferrer.infer("precio")
        assertEquals(ColumnType.DECIMAL, precioInf.type)
        assertEquals("0.0", precioInf.defaultValue)

        // Date
        val fechaInf = TypeInferrer.infer("fecha_nacimiento")
        assertEquals(ColumnType.DATE, fechaInf.type)

        // Boolean
        val activoInf = TypeInferrer.infer("activo")
        assertEquals(ColumnType.BOOLEAN, activoInf.type)
        assertEquals("1", activoInf.defaultValue)

        // Text
        val descInf = TypeInferrer.infer("observaciones")
        assertEquals(ColumnType.TEXT, descInf.type)
    }

    // ── 3. SchemaAdvisor Tests ────────────────────────────────────────

    @Test
    fun testSchemaAdvisorMissingPk() {
        val table = TableInfo(
            name = "articulos",
            columns = listOf(
                Column("nombre", ColumnType.VARCHAR, isPrimaryKey = false),
                Column("precio", ColumnType.DECIMAL, isPrimaryKey = false)
            )
        )
        val issues = SchemaAdvisor.auditTable(table)
        assertTrue(issues.any { it.level == IssueLevel.ERROR && it.title.contains("Clave Primaria") })
    }

    @Test
    fun testSchemaAdvisorReservedKeyword() {
        val table = TableInfo(
            name = "select",
            columns = listOf(
                Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true),
                Column("order", ColumnType.VARCHAR)
            )
        )
        val issues = SchemaAdvisor.auditTable(table)
        assertTrue(issues.any { it.title.contains("Nombre de tabla reservado") })
        assertTrue(issues.any { it.title.contains("palabra reservada") })
    }

    // ── 4. RelationshipAdvisor & Many-to-Many Tests ───────────────────

    @Test
    fun testDetectPossible1NRelationships() {
        val semilleros = TableInfo(
            name = "semilleros",
            columns = listOf(Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true))
        )
        val estudiantes = TableInfo(
            name = "estudiantes",
            columns = listOf(
                Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true),
                Column("semillero_id", ColumnType.INTEGER)
            )
        )

        val suggestions = RelationshipAdvisor.detectPossibleRelationships(listOf(semilleros, estudiantes), emptyList())
        assertEquals(1, suggestions.size)
        assertEquals("estudiantes", suggestions[0].sourceTable)
        assertEquals("semillero_id", suggestions[0].sourceColumn)
        assertEquals("semilleros", suggestions[0].targetTable)
        assertEquals("id", suggestions[0].targetColumn)
    }

    @Test
    fun testCreateManyToManyIntermediateTableSqlite() {
        val tempDb = File.createTempFile("test_cerebro_sqlite_", ".db")
        tempDb.deleteOnExit()

        val engine = SqliteEngine()
        try {
            engine.connect(tempDb.absolutePath, create = true)

            // Create parent tables A and B
            engine.createTable(
                TableInfo(
                    name = "estudiantes",
                    columns = listOf(
                        Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                        Column("nombre", ColumnType.VARCHAR, isNullable = false)
                    )
                )
            )
            engine.createTable(
                TableInfo(
                    name = "cursos",
                    columns = listOf(
                        Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                        Column("titulo", ColumnType.VARCHAR, isNullable = false)
                    )
                )
            )

            // Create Many-to-Many junction table
            val result = Cerebro.createManytoManyIntermediateTable(
                engine = engine,
                tableA = "estudiantes",
                tableB = "cursos",
                customTableName = "estudiantes_cursos"
            )

            assertEquals("estudiantes_cursos", result.intermediateTable.name)
            assertTrue(engine.getTableNames().contains("estudiantes_cursos"))

            // Verify the intermediate table has columns for both FKs
            val createdInfo = engine.getTableInfo("estudiantes_cursos")
            assertTrue(createdInfo.columns.any { it.name == "id" && it.isPrimaryKey })
            assertTrue(createdInfo.columns.any { it.name == "estudiantes_id" })
            assertTrue(createdInfo.columns.any { it.name == "cursos_id" })

            // Verify Junction table detection
            val junctions = Cerebro.findJunctionTables(
                tables = engine.getTableNames().map { engine.getTableInfo(it) },
                relationships = engine.getRelationships()
            )
            assertTrue(junctions.any { it.tableName == "estudiantes_cursos" })
        } finally {
            engine.disconnect()
            tempDb.delete()
        }
    }

    @Test
    fun testCreateManyToManyIntermediateTableAccess() {
        val tempAccdb = File.createTempFile("test_cerebro_access_", ".accdb")
        tempAccdb.delete()
        tempAccdb.deleteOnExit()

        val engine = AccessEngine()
        try {
            engine.connect(tempAccdb.absolutePath, create = true)

            engine.createTable(
                TableInfo(
                    name = "profesores",
                    columns = listOf(
                        Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                        Column("nombre", ColumnType.VARCHAR, isNullable = false)
                    )
                )
            )
            engine.createTable(
                TableInfo(
                    name = "materias",
                    columns = listOf(
                        Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                        Column("nombre", ColumnType.VARCHAR, isNullable = false)
                    )
                )
            )

            val result = Cerebro.createManytoManyIntermediateTable(
                engine = engine,
                tableA = "profesores",
                tableB = "materias",
                customTableName = "profesores_materias"
            )

            assertEquals("profesores_materias", result.intermediateTable.name)
            assertTrue(engine.getTableNames().contains("profesores_materias"))

            val createdInfo = engine.getTableInfo("profesores_materias")
            assertTrue(createdInfo.columns.any { it.name == "profesores_id" })
            assertTrue(createdInfo.columns.any { it.name == "materias_id" })
        } finally {
            engine.disconnect()
            tempAccdb.delete()
        }
    }

    // ── 5. MockDataGenerator Tests ────────────────────────────────────

    @Test
    fun testMockDataGeneratorSqlite() {
        val tempDb = File.createTempFile("test_mock_sqlite_", ".db")
        tempDb.deleteOnExit()

        val engine = SqliteEngine()
        try {
            engine.connect(tempDb.absolutePath, create = true)

            engine.createTable(
                TableInfo(
                    name = "usuarios",
                    columns = listOf(
                        Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                        Column("nombre", ColumnType.VARCHAR, isNullable = false),
                        Column("correo", ColumnType.VARCHAR, isNullable = false),
                        Column("cedula", ColumnType.LONG, isNullable = false),
                        Column("telefono", ColumnType.VARCHAR, isNullable = true),
                        Column("fecha_creacion", ColumnType.DATETIME, isNullable = false)
                    )
                )
            )

            val result = MockDataGenerator.generateRows(engine, "usuarios", rowCount = 5)
            assertEquals(5, result.rowsInserted)
            assertEquals(5, engine.countRows("usuarios"))

            val data = engine.queryAll("usuarios", limit = 10, offset = 0)
            assertEquals(5, data.size)
            assertNotNull(data[0]["nombre"])
            assertTrue(data[0]["correo"].toString().contains("@"))
        } finally {
            engine.disconnect()
            tempDb.delete()
        }
    }
}
