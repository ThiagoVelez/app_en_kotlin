package com.dbstudio.cerebro.advisor

import com.dbstudio.model.Column
import com.dbstudio.model.ColumnType
import com.dbstudio.model.TableInfo

enum class IssueLevel {
    INFO,
    WARNING,
    ERROR
}

data class SchemaAuditIssue(
    val tableName: String,
    val columnName: String? = null,
    val level: IssueLevel,
    val title: String,
    val description: String,
    val autoFixAvailable: Boolean = false,
    val recommendation: String
)

/**
 * Linter and advisor that analyzes database schemas to prevent design errors,
 * reserved keyword conflicts, and compatibility issues across SQLite and Access.
 */
object SchemaAdvisor {

    private val RESERVED_KEYWORDS = setOf(
        "select", "table", "from", "where", "insert", "update", "delete", "drop",
        "alter", "create", "order", "group", "by", "having", "join", "inner", "left",
        "right", "outer", "index", "view", "trigger", "key", "primary", "foreign",
        "check", "default", "null", "not", "and", "or", "in", "like", "as", "into",
        "values", "set", "limit", "offset", "union", "all", "distinct", "case", "when",
        "then", "else", "end", "user", "date", "time", "year", "month", "day"
    )

    fun auditTable(table: TableInfo): List<SchemaAuditIssue> {
        val issues = mutableListOf<SchemaAuditIssue>()

        // 1. Check Table Name
        val tableClean = table.name.trim().lowercase()
        if (tableClean in RESERVED_KEYWORDS) {
            issues.add(
                SchemaAuditIssue(
                    tableName = table.name,
                    level = IssueLevel.WARNING,
                    title = "Nombre de tabla reservado",
                    description = "El nombre '${table.name}' es una palabra reservada en SQL. Puede requerir corchetes o comillas constantes.",
                    recommendation = "Renombra la tabla a una forma plural o específica (ejemplo: '${table.name}s' o 'tbl_${table.name}')."
                )
            )
        }

        if (table.columns.isEmpty()) {
            issues.add(
                SchemaAuditIssue(
                    tableName = table.name,
                    level = IssueLevel.ERROR,
                    title = "Tabla sin columnas",
                    description = "La tabla '${table.name}' no tiene columnas definidas.",
                    recommendation = "Agrega al menos una columna de clave primaria identificadora ('id')."
                )
            )
            return issues
        }

        // 2. Check Primary Key existence
        val pkCols = table.columns.filter { it.isPrimaryKey }
        if (pkCols.isEmpty()) {
            issues.add(
                SchemaAuditIssue(
                    tableName = table.name,
                    level = IssueLevel.ERROR,
                    title = "Sin Clave Primaria (PK)",
                    description = "La tabla '${table.name}' no tiene ninguna clave primaria definida. Esto dificulta la edición de registros y las relaciones.",
                    autoFixAvailable = true,
                    recommendation = "Agrega una columna 'id' de tipo Autoincremental como Clave Primaria."
                )
            )
        }

        // 3. Check Autoincrement validity
        val autoIncCols = table.columns.filter { it.isAutoIncrement || it.type == ColumnType.AUTOINCREMENT }
        if (autoIncCols.size > 1) {
            issues.add(
                SchemaAuditIssue(
                    tableName = table.name,
                    level = IssueLevel.ERROR,
                    title = "Múltiples columnas autoincrementales",
                    description = "La tabla tiene ${autoIncCols.size} columnas autoincrementales. Tanto SQLite como Access solo permiten una por tabla.",
                    recommendation = "Conserva únicamente una columna autoincremental como clave primaria."
                )
            )
        }

        autoIncCols.forEach { col ->
            if (!col.isPrimaryKey) {
                issues.add(
                    SchemaAuditIssue(
                        tableName = table.name,
                        columnName = col.name,
                        level = IssueLevel.WARNING,
                        title = "Autoincremento sin Clave Primaria",
                        description = "La columna '${col.name}' es autoincremental pero no está marcada como Clave Primaria.",
                        recommendation = "Marca '${col.name}' como Clave Primaria."
                    )
                )
            }
        }

        // 4. Column Level checks
        val colNamesSet = mutableSetOf<String>()
        table.columns.forEach { col ->
            val colLower = col.name.trim().lowercase()

            // Duplicate column name
            if (colLower in colNamesSet) {
                issues.add(
                    SchemaAuditIssue(
                        tableName = table.name,
                        columnName = col.name,
                        level = IssueLevel.ERROR,
                        title = "Nombre de columna duplicado",
                        description = "El nombre de columna '${col.name}' se encuentra repetido en la tabla.",
                        recommendation = "Renombra una de las columnas repetidas para evitar colisiones."
                    )
                )
            }
            colNamesSet.add(colLower)

            // Reserved keyword check for columns
            if (colLower in RESERVED_KEYWORDS) {
                issues.add(
                    SchemaAuditIssue(
                        tableName = table.name,
                        columnName = col.name,
                        level = IssueLevel.INFO,
                        title = "Columna con nombre de palabra reservada",
                        description = "La columna '${col.name}' coincide con la palabra reservada SQL '$colLower'.",
                        recommendation = "Considera usar prefijos o sufijos más descriptivos (ej. 'fecha_${col.name}')."
                    )
                )
            }

            // Spaces and special characters
            if (col.name.any { it.isWhitespace() || "!@#$%^&*()+=[]{}\\|;:'\",<>/?".contains(it) }) {
                issues.add(
                    SchemaAuditIssue(
                        tableName = table.name,
                        columnName = col.name,
                        level = IssueLevel.WARNING,
                        title = "Caracteres especiales o espacios en columna",
                        description = "La columna '${col.name}' contiene espacios o caracteres especiales que pueden causar fallas en Access o consultas SQL.",
                        recommendation = "Utiliza snake_case (ej. '${col.name.trim().replace(Regex("[\\s\\W]+"), "_").lowercase()}')."
                    )
                )
            }
        }

        return issues
    }

    fun auditDatabase(tables: List<TableInfo>): List<SchemaAuditIssue> {
        return tables.flatMap { auditTable(it) }
    }
}
