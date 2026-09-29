package com.dbstudio.model

/**
 * Supported column data types across both SQLite and Access.
 */
enum class ColumnType(val displayName: String, val sqliteName: String, val accessName: String) {
    TEXT("Texto", "TEXT", "VARCHAR(255)"),
    VARCHAR("Texto Corto", "TEXT", "VARCHAR(255)"),
    INTEGER("Entero", "INTEGER", "INTEGER"),
    LONG("Cédula / Numérico", "INTEGER", "LONG"),
    REAL("Decimal / Real", "REAL", "DOUBLE"),
    DECIMAL("Decimal", "REAL", "DECIMAL"),
    BOOLEAN("Booleano (Sí/No)", "INTEGER", "YESNO"),
    DATE("Fecha", "TEXT", "DATETIME"),
    DATETIME("Fecha y Hora", "TEXT", "DATETIME"),
    BLOB("Blob", "BLOB", "LONGBINARY"),
    AUTOINCREMENT("ID (Autoincremental)", "INTEGER", "COUNTER");

    companion object {
        fun fromDisplayName(name: String): ColumnType {
            return entries.find { 
                it.displayName.equals(name, ignoreCase = true) || 
                it.name.equals(name, ignoreCase = true) 
            } ?: when (name.lowercase()) {
                "text" -> TEXT
                "varchar" -> VARCHAR
                "integer" -> INTEGER
                "long" -> LONG
                "real" -> REAL
                "decimal" -> DECIMAL
                "boolean" -> BOOLEAN
                "date" -> DATE
                "datetime" -> DATETIME
                "autoincrement" -> AUTOINCREMENT
                else -> TEXT
            }
        }

        fun fromSqlType(sqlType: String): ColumnType {
            val upper = sqlType.uppercase().trim()
            return when {
                upper.startsWith("VARCHAR") -> VARCHAR
                upper == "TEXT" || upper.startsWith("CHAR") || upper.startsWith("CLOB") -> TEXT
                upper == "COUNTER" || upper.contains("AUTOINCREMENT") || upper.contains("AUTO_INCREMENT") -> AUTOINCREMENT
                upper == "INTEGER" || upper == "INT" || upper.startsWith("SMALLINT") || upper.startsWith("TINYINT") || upper.startsWith("MEDIUMINT") -> INTEGER
                upper == "LONG" || upper == "BIGINT" -> LONG
                upper == "REAL" || upper == "FLOAT" || upper.startsWith("DOUBLE") -> REAL
                upper.startsWith("DECIMAL") || upper.startsWith("NUMERIC") -> DECIMAL
                upper == "BOOLEAN" || upper == "YESNO" || upper == "BIT" -> BOOLEAN
                upper == "DATE" -> DATE
                upper == "DATETIME" || upper == "TIMESTAMP" -> DATETIME
                upper == "BLOB" || upper == "LONGBINARY" || upper.startsWith("BINARY") -> BLOB
                else -> TEXT
            }
        }
    }
}

/**
 * Common column presets for quick one-click creation.
 */
object ColumnPresets {
    fun id() = Column(
        name = "id",
        type = ColumnType.AUTOINCREMENT,
        isPrimaryKey = true,
        isAutoIncrement = true,
        isNullable = false
    )

    fun cedula() = Column(
        name = "cedula",
        type = ColumnType.LONG,
        isPrimaryKey = false,
        isAutoIncrement = false,
        isNullable = true,
        isUnique = true
    )

    fun nombre() = Column(
        name = "nombre",
        type = ColumnType.TEXT,
        isNullable = true
    )

    fun apellido() = Column(
        name = "apellido",
        type = ColumnType.TEXT,
        isNullable = true
    )

    fun correo() = Column(
        name = "correo",
        type = ColumnType.VARCHAR,
        length = 150,
        isNullable = true
    )

    fun celular() = Column(
        name = "celular",
        type = ColumnType.VARCHAR,
        length = 20,
        isNullable = true
    )

    fun telefono() = Column(
        name = "telefono",
        type = ColumnType.VARCHAR,
        length = 20,
        isNullable = true
    )

    fun fechaCreacion() = Column(
        name = "fecha_creacion",
        type = ColumnType.DATETIME,
        isNullable = false,
        defaultValue = "CURRENT_TIMESTAMP"
    )

    fun observaciones() = Column(
        name = "observaciones",
        type = ColumnType.TEXT,
        isNullable = true,
        length = 255
    )

    fun descripcion() = Column(
        name = "descripcion",
        type = ColumnType.TEXT,
        isNullable = true,
        length = 255
    )
}

/**
 * Represents a column definition in a table.
 */
data class Column(
    val name: String,
    val type: ColumnType,
    val isPrimaryKey: Boolean = false,
    val isAutoIncrement: Boolean = false,
    val isNullable: Boolean = true,
    val isUnique: Boolean = false,
    val defaultValue: String? = null,
    val length: Int? = null
) {
    /**
     * Format the default value safely for SQL statements.
     */
    fun formattedDefaultValue(): String? {
        val dv = defaultValue?.trim() ?: return null
        if (dv.isEmpty()) return null
        val upper = dv.uppercase()
        if (upper in listOf("CURRENT_TIMESTAMP", "CURRENT_DATE", "CURRENT_TIME", "NOW()", "NOW", "NULL", "TRUE", "FALSE")) {
            return dv
        }
        if (dv.toDoubleOrNull() != null) {
            return dv
        }
        if ((dv.startsWith("'") && dv.endsWith("'")) || (dv.startsWith("\"") && dv.endsWith("\""))) {
            return dv
        }
        return "'${dv.replace("'", "''")}'"
    }

    /**
     * Generate the SQL type declaration for this column.
     */
    fun toSqliteTypeSql(): String {
        val sb = StringBuilder()
        when {
            isAutoIncrement -> sb.append("INTEGER")
            type == ColumnType.VARCHAR && length != null -> sb.append("VARCHAR($length)")
            type == ColumnType.VARCHAR -> sb.append("TEXT")
            else -> sb.append(type.sqliteName)
        }
        if (isPrimaryKey) {
            sb.append(" PRIMARY KEY")
            if (isAutoIncrement) sb.append(" AUTOINCREMENT")
        }
        val formattedDef = formattedDefaultValue()
        if (formattedDef != null && !isAutoIncrement) {
            sb.append(" DEFAULT $formattedDef")
        }
        if (!isNullable && !isPrimaryKey) sb.append(" NOT NULL")
        if (isUnique && !isPrimaryKey) sb.append(" UNIQUE")
        return sb.toString()
    }

    fun toAccessTypeSql(): String {
        val sb = StringBuilder()
        when {
            isAutoIncrement && isPrimaryKey -> sb.append("COUNTER PRIMARY KEY")
            isAutoIncrement -> sb.append("COUNTER")
            type == ColumnType.VARCHAR && length != null && length <= 255 -> sb.append("VARCHAR($length)")
            type == ColumnType.VARCHAR && length != null && length > 255 -> sb.append("LONGVARCHAR")
            type == ColumnType.VARCHAR -> sb.append("VARCHAR(255)")
            type == ColumnType.TEXT -> sb.append("VARCHAR(255)")
            else -> sb.append(type.accessName)
        }
        if (isPrimaryKey && !isAutoIncrement) {
            // Access PKs defined via CONSTRAINT PK_$name
        }
        val formattedDef = formattedDefaultValue()
        if (formattedDef != null && !isAutoIncrement) {
            sb.append(" DEFAULT $formattedDef")
        }
        if (!isNullable && !isPrimaryKey && !isAutoIncrement) sb.append(" NOT NULL")
        if (isUnique && !isPrimaryKey) sb.append(" UNIQUE")
        return sb.toString()
    }
}

/**
 * Represents a table definition with its columns.
 */
data class TableInfo(
    val name: String,
    val columns: List<Column> = emptyList()
) {
    val primaryKeys: List<Column>
        get() = columns.filter { it.isPrimaryKey }

    /**
     * Generate the full CREATE TABLE SQL for SQLite.
     */
    fun toCreateSqliteSql(): String {
        if (columns.isEmpty()) return "CREATE TABLE IF NOT EXISTS \"$name\" (id INTEGER PRIMARY KEY AUTOINCREMENT);"
        val colDefs = columns.joinToString(",\n    ") { "\"${it.name}\" ${it.toSqliteTypeSql()}" }
        return "CREATE TABLE IF NOT EXISTS \"$name\" (\n    $colDefs\n);"
    }

    /**
     * Generate the full CREATE TABLE SQL for Access.
     */
    fun toCreateAccessSql(): String {
        if (columns.isEmpty()) return "CREATE TABLE [$name] (id COUNTER PRIMARY KEY);"
        val colDefs = columns.joinToString(",\n    ") { "[${it.name}] ${it.toAccessTypeSql()}" }
        val pks = columns.filter { it.isPrimaryKey && !it.isAutoIncrement }
        val pkConstraint = if (pks.isNotEmpty()) {
            ",\n    CONSTRAINT PK_$name PRIMARY KEY (${pks.joinToString(", ") { "[${it.name}]" }})"
        } else ""
        return "CREATE TABLE [$name] (\n    $colDefs$pkConstraint\n);"
    }
}
