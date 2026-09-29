package com.dbstudio.cerebro.generator

import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.Column
import com.dbstudio.model.ColumnType
import com.dbstudio.model.TableInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class MockGenerationResult(
    val rowsInserted: Int,
    val warnings: List<String> = emptyList()
)

/**
 * Intelligent local mock data generator for filling tables with realistic, constraint-compliant test data
 * in Spanish, while respecting foreign key integrity.
 */
object MockDataGenerator {

    private val FIRST_NAMES = listOf(
        "Santiago", "Valentina", "Mateo", "Camila", "Sebastián", "Mariana",
        "Alejandro", "Daniela", "Nicolás", "Sofía", "Samuel", "Luciana",
        "Diego", "Paula", "Felipe", "Gabriela", "Andrés", "Valeria", "David", "Isabella"
    )

    private val LAST_NAMES = listOf(
        "Vélez", "Gómez", "Rodríguez", "Martínez", "López", "García",
        "Pérez", "González", "Hernández", "Jaramillo", "Ramírez", "Morales",
        "Castillo", "Vargas", "Restrepo", "Castro", "Ortiz", "Torres"
    )

    private val DOMAINS = listOf(
        "gmail.com", "hotmail.com", "outlook.com", "universidad.edu.co", "empresa.com"
    )

    private val SEMILLEROS = listOf(
        "Semillero de Inteligencia Artificial",
        "Semillero de Robótica y Automatización",
        "Semillero de Desarrollo Web & Móvil",
        "Semillero de Ciberseguridad",
        "Semillero de Ciencia de Datos",
        "Semillero de Internet de las Cosas (IoT)",
        "Semillero de Computación Cuántica",
        "Semillero de Bioinformática"
    )

    private val ROLES = listOf(
        "Coordinador", "Investigador Principal", "Estudiante Pasante", "Asistente Técnico", "Desarrollador"
    )

    private val CITIES = listOf(
        "Medellín", "Bogotá", "Cali", "Barranquilla", "Bucaramanga", "Pereira", "Manizales", "Cartagena"
    )

    private val PRODUCT_NAMES = listOf(
        "Laptop Pro 15\"", "Monitor UltraWide 29\"", "Teclado Mecánico RGB",
        "Mouse Inalámbrico Ergonómico", "Disco SSD NVMe 1TB", "Memoria RAM 16GB DDR5",
        "Auriculares con Cancelación de Ruido", "Silla Ergonómica de Oficina"
    )

    fun generateRows(
        engine: DatabaseEngine,
        tableName: String,
        rowCount: Int = 10
    ): MockGenerationResult {
        val tableInfo = engine.getTableInfo(tableName)
        val relationships = try {
            engine.getRelationships().filter { it.sourceTable.equals(tableName, ignoreCase = true) }
        } catch (_: Exception) { emptyList() }

        // Cache parent keys for foreign keys to preserve integrity
        val foreignKeyCache = mutableMapOf<String, List<Any>>()
        val warnings = mutableListOf<String>()

        for (rel in relationships) {
            try {
                val parentRows = engine.queryAll(rel.targetTable, limit = 100, offset = 0)
                val availableIds = parentRows.mapNotNull { it[rel.targetColumn] }
                if (availableIds.isEmpty()) {
                    warnings.add("La tabla padre '${rel.targetTable}' no contiene registros. Se intentará generar o dejar nulo en '${rel.sourceColumn}'.")
                } else {
                    foreignKeyCache[rel.sourceColumn.lowercase()] = availableIds
                }
            } catch (e: Exception) {
                warnings.add("No se pudieron cargar IDs de '${rel.targetTable}': ${e.message}")
            }
        }

        var inserted = 0
        for (i in 1..rowCount) {
            val rowValues = mutableMapOf<String, String?>()
            val firstName = FIRST_NAMES.random()
            val lastName = LAST_NAMES.random()
            val emailPrefix = "${firstName.lowercase().replace("í", "i").replace("á", "a")}.${lastName.lowercase().replace("é", "e").replace("ó", "o")}$i"

            for (col in tableInfo.columns) {
                // Skip autoincrement primary keys so the database engine generates them
                if (col.isAutoIncrement || col.type == ColumnType.AUTOINCREMENT) {
                    continue
                }

                val colLower = col.name.lowercase()

                // 1. Check if column is a Foreign Key
                val parentKeys = foreignKeyCache[colLower]
                if (parentKeys != null && parentKeys.isNotEmpty()) {
                    rowValues[col.name] = parentKeys.random().toString()
                    continue
                }

                // 2. Synthesize smart contextual value based on column name & type
                val value = synthesizeValue(col, firstName, lastName, emailPrefix, i)
                rowValues[col.name] = value
            }

            try {
                engine.insertRow(tableName, rowValues)
                inserted++
            } catch (e: Exception) {
                warnings.add("Error al insertar fila $i: ${e.message}")
            }
        }

        return MockGenerationResult(rowsInserted = inserted, warnings = warnings.distinct())
    }

    private fun synthesizeValue(
        col: Column,
        firstName: String,
        lastName: String,
        emailPrefix: String,
        index: Int
    ): String {
        val name = col.name.lowercase()

        // Name heuristics
        if (name.contains("nombre") && name.contains("completo")) return "$firstName $lastName"
        if (name == "nombre" || name == "nombres" || name == "first_name") return firstName
        if (name == "apellido" || name == "apellidos" || name == "last_name") return lastName
        if (name.contains("correo") || name.contains("email") || name.contains("mail")) {
            return "$emailPrefix@${DOMAINS.random()}"
        }
        if (name.contains("cedula") || name.contains("dni") || name.contains("documento")) {
            return "${1000000000L + Random.nextInt(10000000, 99999999) + index}"
        }
        if (name.contains("telefono") || name.contains("celular") || name.contains("movil") || name.contains("phone")) {
            return "300${Random.nextInt(1000000, 9999999)}"
        }
        if (name.contains("ciudad") || name.contains("municipio")) return CITIES.random()
        if (name.contains("semillero")) return SEMILLEROS.random()
        if (name.contains("rol") || name.contains("cargo") || name.contains("puesto")) return ROLES.random()
        if (name.contains("producto") || name.contains("articulo") || name.contains("item")) return PRODUCT_NAMES.random()
        if (name.contains("precio") || name.contains("costo") || name.contains("salario") || name.contains("monto")) {
            return "${Random.nextInt(20, 500) * 10000}.00"
        }
        if (name.contains("cantidad") || name.contains("stock") || name.contains("unidades")) {
            return "${Random.nextInt(1, 100)}"
        }
        if (name.contains("estado")) return if (Random.nextBoolean()) "ACTIVO" else "PENDIENTE"
        if (name.contains("descripcion") || name.contains("observacion") || name.contains("detalle") || name.contains("nota")) {
            return "Registro de prueba generado automáticamente para ${col.name} #$index"
        }

        // Type-based fallbacks
        return when (col.type) {
            ColumnType.INTEGER -> "${index * 10 + Random.nextInt(1, 9)}"
            ColumnType.LONG -> "${1000000L + index}"
            ColumnType.REAL, ColumnType.DECIMAL -> "${Random.nextInt(10, 500)}.${Random.nextInt(10, 99)}"
            ColumnType.BOOLEAN -> if (Random.nextBoolean()) "1" else "0"
            ColumnType.DATE -> LocalDate.now().minusDays(Random.nextLong(1, 365)).format(DateTimeFormatter.ISO_DATE)
            ColumnType.DATETIME -> LocalDateTime.now().minusDays(Random.nextLong(1, 100)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ColumnType.TEXT, ColumnType.VARCHAR -> "Dato ${col.name} $index"
            ColumnType.BLOB -> ""
            ColumnType.AUTOINCREMENT -> ""
        }
    }
}
