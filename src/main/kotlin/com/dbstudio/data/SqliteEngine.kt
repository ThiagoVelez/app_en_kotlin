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

    override val isConnected: Boolean get() = connection != null && !connection!!.isClosed
    override val databaseType: DatabaseType = DatabaseType.SQLITE

    companion object {
        init {
            try {
                Class.forName("org.sqlite.JDBC")
            } catch (_: Throwable) {}
        }
    }

    override fun connect(path: String, create: Boolean) {
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
        connection = DriverManager.getConnection("jdbc:sqlite:$normalizedPath")
        // Enable foreign keys
        connection!!.createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
    }

    override fun disconnect() {
        connection?.close()
        connection = null
    }

    private fun conn(): Connection = connection ?: throw IllegalStateException("Not connected to any database")

    // ── Schema: Tables ──────────────────────────────────────────────

    override fun getTableNames(): List<String> {
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
        return tables
    }

    override fun getTableInfo(tableName: String): TableInfo {
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

        return TableInfo(name = tableName, columns = finalColumns)
    }

    override fun createTable(table: TableInfo) {
        conn().createStatement().use { stmt ->
            stmt.execute(table.toCreateSqliteSql())
        }
    }

    override fun dropTable(tableName: String) {
        conn().createStatement().use { stmt ->
            stmt.execute("DROP TABLE IF EXISTS \"$tableName\";")
        }
    }

    override fun renameTable(oldName: String, newName: String) {
        conn().createStatement().use { stmt ->
            stmt.execute("ALTER TABLE \"$oldName\" RENAME TO \"$newName\";")
        }
    }

    // ── Schema: Columns ─────────────────────────────────────────────

    override fun addColumn(tableName: String, column: Column) {
        val typeSql = column.toSqliteTypeSql()
        conn().createStatement().use { stmt ->
            stmt.execute("ALTER TABLE \"$tableName\" ADD COLUMN \"${column.name}\" $typeSql;")
        }
    }

    override fun updateTableSchema(tableName: String, newColumns: List<Column>) {
        // SQLite doesn't support ALTER COLUMN, so we recreate the table
        val tempName = "${tableName}_temp_${System.currentTimeMillis()}"
        val oldInfo = getTableInfo(tableName)
        val newTable = TableInfo(tempName, newColumns)

        conn().createStatement().use { it.execute("PRAGMA foreign_keys = OFF;") }
        conn().autoCommit = false
        try {
            // Create temp table with new schema
            conn().createStatement().use { it.execute(newTable.toCreateSqliteSql()) }

            // Copy data for all columns, supplying safe fallbacks for new NOT NULL columns
            val commonColumns = oldInfo.columns.map { it.name }.intersect(newColumns.map { it.name }.toSet())

            val targetCols = newColumns.joinToString(", ") { "\"${it.name}\"" }
            val selectExpressions = newColumns.map { col ->
                if (col.name in commonColumns) {
                    "\"${col.name}\""
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
            throw e
        } finally {
            conn().autoCommit = true
            conn().createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
        }
    }

    // ── Schema: Relationships ───────────────────────────────────────

    override fun getRelationships(): List<Relationship> {
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

                        relationships.add(
                            Relationship(
                                name = "fk_${table}_${fromCol}_${targetTable}_$id",
                                sourceTable = table,
                                sourceColumn = fromCol,
                                targetTable = targetTable,
                                targetColumn = toCol,
                                type = RelationType.ONE_TO_MANY,
                                onDelete = ReferentialAction.entries.find { it.sql == onDelete }
                                    ?: ReferentialAction.NO_ACTION,
                                onUpdate = ReferentialAction.entries.find { it.sql == onUpdate }
                                    ?: ReferentialAction.NO_ACTION
                            )
                        )
                    }
                }
            }
        }
        return relationships
    }

    override fun addRelationship(relationship: Relationship) {
        // SQLite requires table recreation to add FK constraints
        val tableInfo = getTableInfo(relationship.sourceTable)
        val allRels = getRelationships().filter { 
            it.sourceTable == relationship.sourceTable && 
            it.sourceColumn != relationship.sourceColumn 
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
            // Try to recover
            try {
                conn().createStatement().use { it.execute("DROP TABLE IF EXISTS \"${relationship.sourceTable}\";") }
                conn().createStatement().use { it.execute("ALTER TABLE \"$tempName\" RENAME TO \"${relationship.sourceTable}\";") }
                conn().commit()
            } catch (_: Exception) { }
            throw e
        } finally {
            conn().autoCommit = true
            conn().createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
        }
    }

    override fun removeRelationship(relationshipName: String, sourceTable: String) {
        // Recreate table without the specified FK
        val tableInfo = getTableInfo(sourceTable)
        val allRels = getRelationships()
            .filter { it.sourceTable == sourceTable && it.name != relationshipName }

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
            throw e
        } finally {
            conn().autoCommit = true
            conn().createStatement().use { it.execute("PRAGMA foreign_keys = ON;") }
        }
    }

    // ── CRUD ────────────────────────────────────────────────────────

    override fun queryAll(tableName: String, limit: Int, offset: Int): List<Map<String, Any?>> {
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
        return results
    }

    override fun countRows(tableName: String): Int {
        conn().createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM \"$tableName\"").use { rs ->
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    override fun insertRow(tableName: String, values: Map<String, String?>) {
        val nonNullValues = values.filterValues { !it.isNullOrBlank() }
        if (nonNullValues.isEmpty()) {
            conn().createStatement().use { stmt ->
                stmt.execute("INSERT INTO \"$tableName\" DEFAULT VALUES;")
            }
            return
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

    override fun updateRow(tableName: String, primaryKeyValues: Map<String, Any?>, newValues: Map<String, String?>) {
        if (newValues.isEmpty() || primaryKeyValues.isEmpty()) return
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

    override fun deleteRow(tableName: String, primaryKeyValues: Map<String, Any?>) {
        if (primaryKeyValues.isEmpty()) return
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

    override fun exportToSql(): String {
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

        return sb.toString()
    }

    override fun executeRaw(sql: String): Int {
        return conn().createStatement().use { stmt ->
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

    override fun executeQuery(sql: String): QueryResult {
        val trimmed = sql.trim()
        val startTime = System.currentTimeMillis()
        return conn().createStatement().use { stmt ->
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
