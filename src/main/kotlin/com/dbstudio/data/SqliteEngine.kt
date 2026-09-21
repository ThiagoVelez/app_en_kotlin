package com.dbstudio.data

import com.dbstudio.model.*
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types

/**
 * SQLite implementation of DatabaseEngine using JDBC.
 */
class SqliteEngine : DatabaseEngine {

    private var connection: Connection? = null
    private var dbPath: String? = null
    private val lock = Any()

    override val isConnected: Boolean get() = synchronized(lock) { connection != null && !connection!!.isClosed }
    override val databaseType: DatabaseType = DatabaseType.SQLITE

    companion object {
        init {
            try {
                Class.forName("org.sqlite.JDBC")
            } catch (_: Throwable) {}
        }
    }

    override fun connect(path: String, create: Boolean) = synchronized(lock) {
        if (isConnected) disconnect()
        val file = File(path)
        if (!create && !file.exists()) {
            throw IllegalArgumentException("El archivo de base de datos no existe: $path")
        }
        if (create) {
            file.parentFile?.mkdirs()
        }
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (_: Throwable) {}

        val normalizedPath = file.absolutePath.replace('\\', '/')
        val conn = DriverManager.getConnection("jdbc:sqlite:$normalizedPath")
        // Enable foreign keys
        conn.createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
        connection = conn
        dbPath = path
    }

    override fun disconnect() = synchronized(lock) {
        try {
            connection?.close()
        } catch (_: Throwable) {}
        connection = null
    }

    internal fun conn(): Connection = synchronized(lock) {
        val current = connection
        if (current == null || current.isClosed) {
            val path = dbPath ?: throw IllegalStateException("Not connected to any database")
            val file = File(path)
            if (!file.exists()) {
                throw IllegalStateException("El archivo de base de datos no existe: $path")
            }
            val normalizedPath = file.absolutePath.replace('\\', '/')
            val conn = DriverManager.getConnection("jdbc:sqlite:$normalizedPath")
            conn.createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
            connection = conn
        }
        connection!!
    }

    // ── Schema: Tables ──────────────────────────────────────────────

    override fun getTableNames(): List<String> = synchronized(lock) {
        val tables = mutableListOf<String>()
        conn().createStatement().use { stmt ->
            stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
            ).use { rs ->
                while (rs.next()) {
                    tables.add(rs.getString("name"))
                }
            }
        }
        tables
    }

    override fun getTableInfo(tableName: String): TableInfo = synchronized(lock) {
        val columns = mutableListOf<Column>()
        // Get primary key columns first
        val pkColumns = mutableSetOf<String>()
        conn().createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA table_info(\"$tableName\")").use { rs ->
                while (rs.next()) {
                    if (rs.getInt("pk") > 0) {
                        pkColumns.add(rs.getString("name"))
                    }
                }
            }
        }

        // Check for autoincrement
        var hasAutoincrement = false
        conn().createStatement().use { stmt ->
            stmt.executeQuery(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='$tableName'"
            ).use { rs ->
                if (rs.next()) {
                    val sql = rs.getString("sql") ?: ""
                    hasAutoincrement = sql.uppercase().contains("AUTOINCREMENT")
                }
            }
        }

        conn().createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA table_info(\"$tableName\")").use { rs ->
                while (rs.next()) {
                    val colName = rs.getString("name")
                    val rawType = rs.getString("type") ?: "TEXT"
                    val notNull = rs.getInt("notnull") == 1
                    val defaultVal = rs.getString("dflt_value")
                    val isPk = colName in pkColumns

                    val isAutoInc = isPk && hasAutoincrement && rawType.uppercase() == "INTEGER"

                    val colType = if (isAutoInc) ColumnType.AUTOINCREMENT else ColumnType.fromSqlType(rawType)

                    // Extract length from VARCHAR(n)
                    val length = if (rawType.uppercase().startsWith("VARCHAR")) {
                        val match = Regex("\\((\\d+)\\)").find(rawType)
                        match?.groupValues?.get(1)?.toIntOrNull()
                    } else null

                    columns.add(
                        Column(
                            name = colName,
                            type = colType,
                            isPrimaryKey = isPk,
                            isAutoIncrement = isAutoInc,
                            isNullable = !notNull,
                            defaultValue = defaultVal,
                            length = length
                        )
                    )
                }
            }
        }

        // Detect UNIQUE constraints
        val uniqueCols = mutableSetOf<String>()
        conn().createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA index_list(\"$tableName\")").use { rs ->
                while (rs.next()) {
                    if (rs.getInt("unique") == 1) {
                        val idxName = rs.getString("name")
                        conn().createStatement().use { stmt2 ->
                            stmt2.executeQuery("PRAGMA index_info(\"$idxName\")").use { rs2 ->
                                while (rs2.next()) {
                                    uniqueCols.add(rs2.getString("name"))
                                }
                            }
                        }
                    }
                }
            }
        }

        val finalColumns = columns.map { col ->
            if (col.name in uniqueCols && !col.isPrimaryKey) col.copy(isUnique = true) else col
        }

        TableInfo(name = tableName, columns = finalColumns)
    }

    override fun createTable(table: TableInfo): Unit = synchronized(lock) {
        conn().createStatement().use { stmt ->
            stmt.execute(table.toCreateSqliteSql())
        }
    }

    override fun dropTable(tableName: String): Unit = synchronized(lock) {
        conn().createStatement().use { stmt ->
            stmt.execute("DROP TABLE IF EXISTS \"$tableName\";")
        }
    }

    override fun renameTable(oldName: String, newName: String): Unit = synchronized(lock) {
        conn().createStatement().use { stmt ->
            stmt.execute("ALTER TABLE \"$oldName\" RENAME TO \"$newName\";")
        }
    }

    // ── Schema: Columns ─────────────────────────────────────────────

    override fun addColumn(tableName: String, column: Column): Unit = synchronized(lock) {
        val typeSql = column.toSqliteTypeSql()
        conn().createStatement().use { stmt ->
            stmt.execute("ALTER TABLE \"$tableName\" ADD COLUMN \"${column.name}\" $typeSql;")
        }
    }

    override fun updateTableSchema(tableName: String, newColumns: List<Column>): Unit = synchronized(lock) {
        val oldInfo = getTableInfo(tableName)
        val oldNames = oldInfo.columns.map { it.name.lowercase() }.toSet()
        val newNames = newColumns.map { it.name.lowercase() }.toSet()
        val newColMap = newColumns.associateBy { it.name.lowercase() }
        val addedCols = newColumns.filter { it.name.lowercase() !in oldNames }

        // Fast path: If all existing columns are preserved and new columns are nullable or have default values
        val isOnlyAddingColumns = oldNames.all { it in newNames } &&
                addedCols.isNotEmpty() &&
                newColumns.size == oldInfo.columns.size + addedCols.size &&
                oldInfo.columns.all { oldCol ->
                    val newCol = newColMap[oldCol.name.lowercase()]
                    newCol != null && newCol.name == oldCol.name && newCol.isPrimaryKey == oldCol.isPrimaryKey
                } &&
                addedCols.all { it.isNullable || it.defaultValue != null }

        if (isOnlyAddingColumns) {
            for (col in addedCols) {
                addColumn(tableName, col)
            }
            return@synchronized
        }

        // SQLite doesn't support ALTER COLUMN, so we recreate the table
        val tempName = "${tableName}_temp_${System.currentTimeMillis()}"
        val existingFks = try {
            getRelationships().filter { it.sourceTable.equals(tableName, ignoreCase = true) }
        } catch (_: Exception) { emptyList() }

        val colDefs = newColumns.joinToString(",\n    ") { "\"${it.name}\" ${it.toSqliteTypeSql()}" }
        val fkDefs = if (existingFks.isNotEmpty()) {
            ",\n    " + existingFks.joinToString(",\n    ") { it.toSqliteFkClause() }
        } else ""
        val createSql = "CREATE TABLE \"$tempName\" (\n    $colDefs$fkDefs\n);"

        conn().createStatement().use { it.execute("PRAGMA foreign_keys = OFF;") }
        conn().autoCommit = false
        try {
            // Create temp table with new schema and existing FK constraints
            conn().createStatement().use { it.execute(createSql) }

            // Copy data for all columns, matching column names case-insensitively
            val oldColMap = oldInfo.columns.associateBy { it.name.lowercase() }
            val targetCols = newColumns.joinToString(", ") { "\"${it.name}\"" }
            val selectExpressions = newColumns.map { col ->
                val matchedOldCol = oldColMap[col.name.lowercase()]
                if (matchedOldCol != null) {
                    "\"${matchedOldCol.name}\""
                } else if (col.defaultValue != null) {
                    col.formattedDefaultValue() ?: "NULL"
                } else if (col.isNullable) {
                    "NULL"
                } else {
                    // Safe fallback for new NOT NULL columns without default value on existing rows
                    when (col.type) {
                        ColumnType.TEXT, ColumnType.VARCHAR -> "''"
                        ColumnType.INTEGER, ColumnType.LONG -> if (col.isUnique) "ROW_NUMBER() OVER ()" else "0"
                        ColumnType.REAL, ColumnType.DECIMAL -> "0.0"
                        ColumnType.BOOLEAN -> "0"
                        ColumnType.DATE -> "CURRENT_DATE"
                        ColumnType.DATETIME -> "CURRENT_TIMESTAMP"
                        ColumnType.BLOB -> "X''"
                        ColumnType.AUTOINCREMENT -> "NULL"
                    }
                }
            }.joinToString(", ")

            conn().createStatement().use {
                it.execute("INSERT INTO \"$tempName\" ($targetCols) SELECT $selectExpressions FROM \"$tableName\";")
            }

            // Drop old table
            conn().createStatement().use { it.execute("DROP TABLE \"$tableName\";") }

            // Rename temp to original
            conn().createStatement().use { it.execute("ALTER TABLE \"$tempName\" RENAME TO \"$tableName\";") }

            conn().commit()
        } catch (e: Exception) {
            conn().rollback()
            try {
                conn().createStatement().use { it.execute("DROP TABLE IF EXISTS \"$tempName\";") }
            } catch (_: Exception) {}
            throw e
        } finally {
            conn().autoCommit = true
            conn().createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
        }
    }

    // ── Schema: Relationships ───────────────────────────────────────

    override fun getRelationships(): List<Relationship> = synchronized(lock) {
        val relationships = mutableListOf<Relationship>()
        val tables = getTableNames()

        for (table in tables) {
            conn().createStatement().use { stmt ->
                stmt.executeQuery("PRAGMA foreign_key_list(\"$table\")").use { rs ->
                    while (rs.next()) {
                        val id = rs.getInt("id")
                        val targetTable = rs.getString("table")
                        val fromCol = rs.getString("from")
                        val toCol = rs.getString("to")
                        val onUpdate = rs.getString("on_update") ?: "NO ACTION"
                        val onDelete = rs.getString("on_delete") ?: "NO ACTION"

                        val deleteRule = when (onDelete.uppercase()) {
                            "CASCADE" -> ReferentialAction.CASCADE
                            "SET NULL" -> ReferentialAction.SET_NULL
                            "SET DEFAULT" -> ReferentialAction.SET_DEFAULT
                            "RESTRICT" -> ReferentialAction.RESTRICT
                            else -> ReferentialAction.NO_ACTION
                        }
                        val updateRule = when (onUpdate.uppercase()) {
                            "CASCADE" -> ReferentialAction.CASCADE
                            "SET NULL" -> ReferentialAction.SET_NULL
                            "SET DEFAULT" -> ReferentialAction.SET_DEFAULT
                            "RESTRICT" -> ReferentialAction.RESTRICT
                            else -> ReferentialAction.NO_ACTION
                        }

                        relationships.add(
                            Relationship(
                                name = "fk_${table}_${fromCol}_${targetTable}_$id",
                                sourceTable = table,
                                sourceColumn = fromCol,
                                targetTable = targetTable,
                                targetColumn = toCol,
                                type = RelationType.ONE_TO_MANY,
                                onDelete = deleteRule,
                                onUpdate = updateRule
                            )
                        )
                    }
                }
            }
        }
        relationships
    }

    override fun addRelationship(relationship: Relationship): Unit = synchronized(lock) {
        // SQLite requires table recreation to add FK constraints
        val tableInfo = getTableInfo(relationship.sourceTable)
        val allRels = getRelationships().filter { 
            it.sourceTable.equals(relationship.sourceTable, ignoreCase = true) && 
            !it.sourceColumn.equals(relationship.sourceColumn, ignoreCase = true)
        }
        val allFks = allRels + relationship

        // Build CREATE TABLE with FK constraints
        val colDefs = tableInfo.columns.joinToString(",\n    ") {
            "\"${it.name}\" ${it.toSqliteTypeSql()}"
        }
        val fkDefs = allFks.joinToString(",\n    ") { it.toSqliteFkClause() }
        val createSql = "CREATE TABLE \"${relationship.sourceTable}\" (\n    $colDefs,\n    $fkDefs\n);"

        val tempName = "${relationship.sourceTable}_bak_${System.currentTimeMillis()}"
        val colNames = tableInfo.columns.joinToString(", ") { "\"${it.name}\"" }

        conn().createStatement().use { it.execute("PRAGMA foreign_keys = OFF;") }
        conn().autoCommit = false
        try {
            conn().createStatement().use { it.execute("ALTER TABLE \"${relationship.sourceTable}\" RENAME TO \"$tempName\";") }
            conn().createStatement().use { it.execute(createSql) }
            conn().createStatement().use {
                it.execute("INSERT INTO \"${relationship.sourceTable}\" ($colNames) SELECT $colNames FROM \"$tempName\";")
            }
            conn().createStatement().use { it.execute("DROP TABLE \"$tempName\";") }
            conn().commit()
        } catch (e: Exception) {
            conn().rollback()
            try {
                conn().createStatement().use { it.execute("DROP TABLE IF EXISTS \"$tempName\";") }
            } catch (_: Exception) { }
            throw e
        } finally {
            conn().autoCommit = true
            conn().createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
        }
    }

    override fun removeRelationship(relationshipName: String, sourceTable: String): Unit = synchronized(lock) {
        // Recreate table without the specified FK
        val tableInfo = getTableInfo(sourceTable)
        val allRels = getRelationships()
            .filter { it.sourceTable.equals(sourceTable, ignoreCase = true) && it.name != relationshipName }

        val colDefs = tableInfo.columns.joinToString(",\n    ") {
            "\"${it.name}\" ${it.toSqliteTypeSql()}"
        }
        val fkDefs = if (allRels.isNotEmpty()) {
            ",\n    " + allRels.joinToString(",\n    ") { it.toSqliteFkClause() }
        } else ""
        val createSql = "CREATE TABLE \"$sourceTable\" (\n    $colDefs$fkDefs\n);"

        val tempName = "${sourceTable}_bak_${System.currentTimeMillis()}"
        val colNames = tableInfo.columns.joinToString(", ") { "\"${it.name}\"" }

        conn().createStatement().use { it.execute("PRAGMA foreign_keys = OFF;") }
        conn().autoCommit = false
        try {
            conn().createStatement().use { it.execute("ALTER TABLE \"$sourceTable\" RENAME TO \"$tempName\";") }
            conn().createStatement().use { it.execute(createSql) }
            conn().createStatement().use {
                it.execute("INSERT INTO \"$sourceTable\" ($colNames) SELECT $colNames FROM \"$tempName\";")
            }
            conn().createStatement().use { it.execute("DROP TABLE \"$tempName\";") }
            conn().commit()
        } catch (e: Exception) {
            conn().rollback()
            try {
                conn().createStatement().use { it.execute("DROP TABLE IF EXISTS \"$tempName\";") }
            } catch (_: Exception) { }
            throw e
        } finally {
            conn().autoCommit = true
            conn().createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
        }
    }

    // ── CRUD ────────────────────────────────────────────────────────

    override fun queryAll(tableName: String, limit: Int, offset: Int): List<Map<String, Any?>> = synchronized(lock) {
        val results = mutableListOf<Map<String, Any?>>()
        conn().createStatement().use { stmt ->
            stmt.executeQuery("SELECT * FROM \"$tableName\" LIMIT $limit OFFSET $offset").use { rs ->
                val meta = rs.metaData
                while (rs.next()) {
                    val row = mutableMapOf<String, Any?>()
                    for (i in 1..meta.columnCount) {
                        row[meta.getColumnName(i)] = rs.getObject(i)
                    }
                    results.add(row)
                }
            }
        }
        results
    }

    override fun countRows(tableName: String): Int = synchronized(lock) {
        conn().createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM \"$tableName\"").use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    override fun insertRow(tableName: String, values: Map<String, String?>): Unit = synchronized(lock) {
        val resolvedValues = values.toMutableMap()
        val tableRels = try { getRelationships().filter { it.sourceTable.equals(tableName, ignoreCase = true) } } catch (_: Exception) { emptyList() }
        for (rel in tableRels) {
            val curVal = resolvedValues[rel.sourceColumn]
                ?: resolvedValues.entries.firstOrNull { it.key.equals(rel.sourceColumn, ignoreCase = true) }?.value
            if (curVal.isNullOrBlank()) {
                val targetRows = try { queryAll(rel.targetTable, limit = 1) } catch (_: Exception) { emptyList() }
                if (targetRows.isNotEmpty()) {
                    val defaultVal = targetRows[0][rel.targetColumn]
                        ?: targetRows[0].entries.firstOrNull { it.key.equals(rel.targetColumn, ignoreCase = true) }?.value
                    if (defaultVal != null) {
                        resolvedValues[rel.sourceColumn] = defaultVal.toString()
                    }
                }
            }
        }

        val nonNullValues = resolvedValues.filterValues { !it.isNullOrBlank() }
        if (nonNullValues.isEmpty()) {
            conn().createStatement().use { stmt ->
                stmt.execute("INSERT INTO \"$tableName\" DEFAULT VALUES;")
            }
            return@synchronized
        }

        val cols = nonNullValues.keys.joinToString(", ") { "\"$it\"" }
        val placeholders = nonNullValues.keys.joinToString(", ") { "?" }
        val sql = "INSERT INTO \"$tableName\" ($cols) VALUES ($placeholders)"

        conn().prepareStatement(sql).use { pstmt ->
            nonNullValues.values.forEachIndexed { idx, value ->
                pstmt.setString(idx + 1, value)
            }
            pstmt.executeUpdate()
        }
    }

    override fun updateRow(tableName: String, primaryKeyValues: Map<String, Any?>, newValues: Map<String, String?>): Unit = synchronized(lock) {
        if (newValues.isEmpty() || primaryKeyValues.isEmpty()) return@synchronized
        val setClauses = newValues.keys.joinToString(", ") { "\"$it\" = ?" }
        val whereClauses = primaryKeyValues.keys.joinToString(" AND ") { "\"$it\" = ?" }
        val sql = "UPDATE \"$tableName\" SET $setClauses WHERE $whereClauses"

        conn().prepareStatement(sql).use { pstmt ->
            var idx = 1
            for (value in newValues.values) {
                if (value == null || value.isBlank()) {
                    pstmt.setNull(idx, Types.VARCHAR)
                } else {
                    pstmt.setString(idx, value)
                }
                idx++
            }
            for (value in primaryKeyValues.values) {
                pstmt.setObject(idx, value)
                idx++
            }
            pstmt.executeUpdate()
        }
    }

    override fun deleteRow(tableName: String, primaryKeyValues: Map<String, Any?>): Unit = synchronized(lock) {
        if (primaryKeyValues.isEmpty()) return@synchronized
        val whereClauses = primaryKeyValues.keys.joinToString(" AND ") { "\"$it\" = ?" }
        val sql = "DELETE FROM \"$tableName\" WHERE $whereClauses"

        conn().prepareStatement(sql).use { pstmt ->
            primaryKeyValues.values.forEachIndexed { idx, value ->
                pstmt.setObject(idx + 1, value)
            }
            pstmt.executeUpdate()
        }
    }

    // ── Export ──────────────────────────────────────────────────────

    override fun exportToSql(): String = synchronized(lock) {
        val sb = StringBuilder()
        sb.appendLine("-- DB Studio SQLite Export")
        sb.appendLine("-- Generated: ${java.time.LocalDateTime.now()}")
        sb.appendLine("PRAGMA foreign_keys = ON;")
        sb.appendLine()

        val tables = getTableNames()

        // Export CREATE TABLE statements (from sqlite_master to preserve exact DDL)
        for (table in tables) {
            conn().createStatement().use { stmt ->
                stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='$table'").use { rs ->
                    if (rs.next()) {
                        val sql = rs.getString("sql")
                        if (sql != null) {
                            sb.appendLine("$sql;")
                            sb.appendLine()
                        }
                    }
                }
            }

            // Export data
            val rows = queryAll(table, limit = Int.MAX_VALUE)
            if (rows.isNotEmpty()) {
                val tableInfo = getTableInfo(table)
                val colNames = tableInfo.columns.map { it.name }
                for (row in rows) {
                    val cols = colNames.joinToString(", ") { "\"$it\"" }
                    val vals = colNames.joinToString(", ") { colName ->
                        val v = row[colName]
                        when (v) {
                            null -> "NULL"
                            is Number -> v.toString()
                            is ByteArray -> "X'${v.joinToString("") { "%02x".format(it) }}'"
                            else -> "'${v.toString().replace("'", "''")}'"
                        }
                    }
                    sb.appendLine("INSERT INTO \"$table\" ($cols) VALUES ($vals);")
                }
                sb.appendLine()
            }
        }

        // Export indexes
        conn().createStatement().use { stmt ->
            stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='index' AND sql IS NOT NULL").use { rs ->
                while (rs.next()) {
                    sb.appendLine("${rs.getString("sql")};")
                }
            }
        }

        sb.toString()
    }

    override fun executeRaw(sql: String): Int = synchronized(lock) {
        conn().createStatement().use { stmt ->
            if (sql.trim().uppercase().let {
                    it.startsWith("SELECT") || it.startsWith("PRAGMA")
                }) {
                stmt.executeQuery(sql)
                0
            } else {
                stmt.executeUpdate(sql)
            }
        }
    }

    override fun executeQuery(sql: String): QueryResult = synchronized(lock) {
        val trimmed = sql.trim()
        val startTime = System.currentTimeMillis()
        conn().createStatement().use { stmt ->
            val hasResultSet = stmt.execute(trimmed)
            val elapsed = System.currentTimeMillis() - startTime
            if (hasResultSet) {
                stmt.resultSet.use { rs ->
                    val meta = rs.metaData
                    val count = meta.columnCount
                    val columns = (1..count).map { meta.getColumnLabel(it) ?: meta.getColumnName(it) ?: "Col_$it" }
                    val rows = mutableListOf<List<Any?>>()
                    while (rs.next()) {
                        val row = (1..count).map { rs.getObject(it) }
                        rows.add(row)
                    }
                    QueryResult(
                        columns = columns,
                        rows = rows,
                        executionTimeMs = elapsed,
                        isSelect = true,
                        message = "${rows.size} fila(s) obtenida(s) en ${elapsed} ms"
                    )
                }
            } else {
                val affected = stmt.updateCount
                QueryResult(
                    affectedRows = affected,
                    executionTimeMs = elapsed,
                    isSelect = false,
                    message = "Sentencia ejecutada con éxito. Filas afectadas: $affected ($elapsed ms)"
                )
            }
        }
    }
}
