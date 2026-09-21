package com.dbstudio.data

import com.dbstudio.model.*
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types

/**
 * Microsoft Access (.accdb) implementation of DatabaseEngine using UCanAccess.
 */
class AccessEngine : DatabaseEngine {

    private var connection: Connection? = null
    private var dbPath: String? = null
    private val lock = Any()

    override val isConnected: Boolean get() = synchronized(lock) { connection != null && !connection!!.isClosed }
    override val databaseType: DatabaseType = DatabaseType.ACCESS

    companion object {
        init {
            try {
                Class.forName("net.ucanaccess.jdbc.UcanaccessDriver")
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
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver")
        } catch (_: Throwable) {}

        val normalizedPath = file.absolutePath.replace('\\', '/')
        val url = if (create && !file.exists()) {
            "jdbc:ucanaccess://$normalizedPath;newDatabaseVersion=V2010"
        } else {
            "jdbc:ucanaccess://$normalizedPath"
        }
        connection = DriverManager.getConnection(url)
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
            connection = DriverManager.getConnection("jdbc:ucanaccess://$normalizedPath")
        }
        connection!!
    }

    // ── Schema: Tables ──────────────────────────────────────────────

    override fun getTableNames(): List<String> = synchronized(lock) {
        val tables = mutableListOf<String>()
        val meta = conn().metaData
        meta.getTables(null, null, "%", arrayOf("TABLE")).use { rs ->
            while (rs.next()) {
                val name = rs.getString("TABLE_NAME")
                // Filter out system tables
                if (!name.startsWith("MSys") && !name.startsWith("~")) {
                    tables.add(name)
                }
            }
        }
        tables.sorted()
    }

    override fun getTableInfo(tableName: String): TableInfo = synchronized(lock) {
        val columns = mutableListOf<Column>()
        val meta = conn().metaData

        // Get primary keys
        val pkColumns = mutableSetOf<String>()
        meta.getPrimaryKeys(null, null, tableName).use { rs ->
            while (rs.next()) {
                pkColumns.add(rs.getString("COLUMN_NAME"))
            }
        }

        // Get columns
        meta.getColumns(null, null, tableName, "%").use { rs ->
            while (rs.next()) {
                val colName = rs.getString("COLUMN_NAME")
                val sqlType = rs.getString("TYPE_NAME") ?: "TEXT"
                val nullable = rs.getInt("NULLABLE") == 1
                val defaultVal = rs.getString("COLUMN_DEF")
                val colSize = rs.getInt("COLUMN_SIZE")
                val isAutoInc = sqlType.uppercase() == "COUNTER" ||
                        (rs.getString("IS_AUTOINCREMENT")?.equals("YES", ignoreCase = true) == true)
                val isPk = colName in pkColumns

                val colType = when {
                    isAutoInc -> ColumnType.AUTOINCREMENT
                    else -> ColumnType.fromSqlType(sqlType)
                }

                val length = if (colType == ColumnType.VARCHAR || colType == ColumnType.TEXT) {
                    if (colSize > 0 && colSize < 255) colSize else null
                } else null

                columns.add(
                    Column(
                        name = colName,
                        type = colType,
                        isPrimaryKey = isPk,
                        isAutoIncrement = isAutoInc,
                        isNullable = nullable,
                        defaultValue = defaultVal,
                        length = length
                    )
                )
            }
        }

        TableInfo(name = tableName, columns = columns)
    }

    override fun createTable(table: TableInfo): Unit = synchronized(lock) {
        conn().createStatement().use { stmt ->
            stmt.execute(table.toCreateAccessSql())
        }
    }

    override fun dropTable(tableName: String): Unit = synchronized(lock) {
        conn().createStatement().use { stmt ->
            stmt.execute("DROP TABLE [$tableName];")
        }
    }

    override fun renameTable(oldName: String, newName: String): Unit = synchronized(lock) {
        // UCanAccess doesn't support RENAME TABLE directly.
        // We must create a new table, copy data, and drop the old one.
        val info = getTableInfo(oldName)
        val newTable = TableInfo(newName, info.columns)
        val data = queryAll(oldName, limit = Int.MAX_VALUE)

        val allRels = try { getRelationships() } catch (_: Exception) { emptyList() }
        val sourceRels = allRels.filter { it.sourceTable.equals(oldName, ignoreCase = true) }
        val targetRels = allRels.filter { it.targetTable.equals(oldName, ignoreCase = true) }

        for (rel in (sourceRels + targetRels)) {
            try { removeRelationship(rel.name, rel.sourceTable) } catch (_: Exception) {}
        }

        try {
            createTable(newTable)
            for (row in data) {
                val stringValues = row.mapValues { (_, v) -> v?.toString() }
                insertRow(newName, stringValues)
            }
            dropTable(oldName)

            for (rel in sourceRels) {
                try { addRelationship(rel.copy(sourceTable = newName)) } catch (_: Exception) {}
            }
            for (rel in targetRels) {
                try { addRelationship(rel.copy(targetTable = newName)) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            for (rel in (sourceRels + targetRels)) {
                try { addRelationship(rel) } catch (_: Exception) {}
            }
            throw e
        }
    }

    // ── Schema: Columns ─────────────────────────────────────────────

    override fun addColumn(tableName: String, column: Column): Unit = synchronized(lock) {
        val typeSql = column.toAccessTypeSql()
        conn().createStatement().use { stmt ->
            stmt.execute("ALTER TABLE [$tableName] ADD COLUMN [${column.name}] $typeSql;")
        }
    }

    override fun updateTableSchema(tableName: String, newColumns: List<Column>): Unit = synchronized(lock) {
        val oldInfo = getTableInfo(tableName)
        val oldNames = oldInfo.columns.map { it.name.lowercase() }.toSet()
        val newNames = newColumns.map { it.name.lowercase() }.toSet()
        val newColMap = newColumns.associateBy { it.name.lowercase() }
        val addedCols = newColumns.filter { it.name.lowercase() !in oldNames }

        // Fast path: If all existing columns are preserved and new columns are simply being added
        val isOnlyAddingColumns = oldNames.all { it in newNames } &&
                addedCols.isNotEmpty() &&
                newColumns.size == oldInfo.columns.size + addedCols.size &&
                oldInfo.columns.all { oldCol ->
                    val newCol = newColMap[oldCol.name.lowercase()]
                    newCol != null && newCol.name == oldCol.name && newCol.isPrimaryKey == oldCol.isPrimaryKey
                }

        if (isOnlyAddingColumns) {
            for (col in addedCols) {
                addColumn(tableName, col)
            }
            return@synchronized
        }

        // Full schema rewrite path with safe relationship handling
        val allRels = try { getRelationships() } catch (_: Exception) { emptyList() }
        val sourceRels = allRels.filter { it.sourceTable.equals(tableName, ignoreCase = true) }
        val targetRels = allRels.filter { it.targetTable.equals(tableName, ignoreCase = true) }
        val involvedRels = sourceRels + targetRels

        for (rel in involvedRels) {
            try { removeRelationship(rel.name, rel.sourceTable) } catch (_: Exception) {}
        }

        val data = queryAll(tableName, limit = Int.MAX_VALUE)
        val tempName = "${tableName}_temp_${System.currentTimeMillis() % 100000}"

        try {
            // Create temp table with new schema
            val newTable = TableInfo(tempName, newColumns)
            createTable(newTable)

            // Copy data for common columns and provide safe fallbacks for new NOT NULL columns
            val oldColMap = oldInfo.columns.associateBy { it.name.lowercase() }

            var rowNum = 1
            for (row in data) {
                val values = mutableMapOf<String, String?>()
                for (col in newColumns) {
                    val matchedOld = oldColMap[col.name.lowercase()]
                    if (matchedOld != null) {
                        val oldVal = row[matchedOld.name]
                            ?: row.entries.firstOrNull { it.key.equals(matchedOld.name, ignoreCase = true) }?.value
                        values[col.name] = oldVal?.toString()
                    } else if (col.defaultValue != null) {
                        // Default value will be applied automatically
                    } else if (!col.isNullable && !col.isAutoIncrement) {
                        values[col.name] = when (col.type) {
                            ColumnType.TEXT, ColumnType.VARCHAR -> ""
                            ColumnType.INTEGER, ColumnType.LONG -> if (col.isUnique) "$rowNum" else "0"
                            ColumnType.REAL, ColumnType.DECIMAL -> "0.0"
                            ColumnType.BOOLEAN -> "0"
                            ColumnType.DATE, ColumnType.DATETIME -> java.time.LocalDate.now().toString()
                            else -> ""
                        }
                    }
                }
                rowNum++
                if (values.isNotEmpty()) {
                    insertRow(tempName, values)
                }
            }

            // Drop old, recreate new
            dropTable(tableName)
            val finalTable = TableInfo(tableName, newColumns)
            createTable(finalTable)

            val newData = queryAll(tempName, limit = Int.MAX_VALUE)
            for (row in newData) {
                val stringValues = mutableMapOf<String, String?>()
                for (col in newColumns) {
                    val v = row[col.name]
                        ?: row.entries.firstOrNull { it.key.equals(col.name, ignoreCase = true) }?.value
                    stringValues[col.name] = v?.toString()
                }
                insertRow(tableName, stringValues)
            }
            dropTable(tempName)

            // Restore relationships
            for (rel in involvedRels) {
                try { addRelationship(rel) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            try { dropTable(tempName) } catch (_: Exception) {}
            for (rel in involvedRels) {
                try { addRelationship(rel) } catch (_: Exception) {}
            }
            throw e
        }
    }

    // ── Schema: Relationships ───────────────────────────────────────

    override fun getRelationships(): List<Relationship> = synchronized(lock) {
        val relationships = mutableListOf<Relationship>()
        val tables = getTableNames()
        val meta = conn().metaData

        for (table in tables) {
            meta.getImportedKeys(null, null, table).use { rs ->
                while (rs.next()) {
                    val fkName = rs.getString("FK_NAME") ?: "fk_${table}_${rs.getInt("KEY_SEQ")}"
                    val pkTable = rs.getString("PKTABLE_NAME")
                    val pkCol = rs.getString("PKCOLUMN_NAME")
                    val fkCol = rs.getString("FKCOLUMN_NAME")
                    val deleteRule = when (rs.getInt("DELETE_RULE")) {
                        0 -> ReferentialAction.CASCADE
                        2 -> ReferentialAction.SET_NULL
                        4 -> ReferentialAction.SET_DEFAULT
                        1 -> ReferentialAction.RESTRICT
                        else -> ReferentialAction.NO_ACTION
                    }
                    val updateRule = when (rs.getInt("UPDATE_RULE")) {
                        0 -> ReferentialAction.CASCADE
                        2 -> ReferentialAction.SET_NULL
                        4 -> ReferentialAction.SET_DEFAULT
                        1 -> ReferentialAction.RESTRICT
                        else -> ReferentialAction.NO_ACTION
                    }

                    relationships.add(
                        Relationship(
                            name = fkName,
                            sourceTable = table,
                            sourceColumn = fkCol,
                            targetTable = pkTable,
                            targetColumn = pkCol,
                            type = RelationType.ONE_TO_MANY,
                            onDelete = deleteRule,
                            onUpdate = updateRule
                        )
                    )
                }
            }
        }
        relationships
    }

    override fun addRelationship(relationship: Relationship): Unit = synchronized(lock) {
        // Ensure the target table has a PRIMARY KEY constraint on the referenced column if not already present
        try {
            conn().createStatement().use { stmt ->
                stmt.execute("ALTER TABLE [${relationship.targetTable}] ADD CONSTRAINT PK_${relationship.targetTable} PRIMARY KEY ([${relationship.targetColumn}])")
            }
        } catch (_: Exception) {
            // Already has primary key or unique constraint, safe to proceed
        }

        conn().createStatement().use { stmt ->
            stmt.execute(relationship.toAccessAlterSql())
        }
    }

    override fun removeRelationship(relationshipName: String, sourceTable: String): Unit = synchronized(lock) {
        try {
            conn().createStatement().use { stmt ->
                stmt.execute("ALTER TABLE [$sourceTable] DROP CONSTRAINT [$relationshipName];")
            }
        } catch (_: Exception) {
            // UCanAccess does not support DROP CONSTRAINT via JDBC.
            // Safely recreate table without the constraint while preserving all columns and data.
            try {
                val info = getTableInfo(sourceTable)
                val rows = queryAll(sourceTable, limit = Int.MAX_VALUE)
                val otherRels = try { getRelationships() } catch (_: Exception) { emptyList() }
                    .filter { !it.name.equals(relationshipName, ignoreCase = true) && !it.name.contains(relationshipName, ignoreCase = true) }
                dropTable(sourceTable)
                createTable(info)
                for (r in rows) {
                    val strVals = r.filterKeys { k -> !info.columns.any { it.name == k && it.isAutoIncrement } }
                        .mapValues { it.value?.toString() }
                    if (strVals.isNotEmpty()) {
                        insertRow(sourceTable, strVals)
                    }
                }
                for (rel in otherRels) {
                    if (rel.sourceTable.equals(sourceTable, ignoreCase = true)) {
                        try { addRelationship(rel) } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── CRUD ────────────────────────────────────────────────────────

    override fun queryAll(tableName: String, limit: Int, offset: Int): List<Map<String, Any?>> = synchronized(lock) {
        val results = mutableListOf<Map<String, Any?>>()
        // Access doesn't support LIMIT/OFFSET natively, use TOP or subquery
        val sql = if (limit < Int.MAX_VALUE) {
            "SELECT * FROM [$tableName]"  // We'll handle pagination in memory for Access
        } else {
            "SELECT * FROM [$tableName]"
        }
        conn().createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val meta = rs.metaData
                var count = 0
                var skipped = 0
                while (rs.next()) {
                    if (skipped < offset) {
                        skipped++
                        continue
                    }
                    if (count >= limit) break
                    val row = mutableMapOf<String, Any?>()
                    for (i in 1..meta.columnCount) {
                        row[meta.getColumnName(i)] = rs.getObject(i)
                    }
                    results.add(row)
                    count++
                }
            }
        }
        results
    }

    override fun countRows(tableName: String): Int = synchronized(lock) {
        conn().createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM [$tableName]").use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    override fun insertRow(tableName: String, values: Map<String, String?>): Unit = synchronized(lock) {
        if (values.isEmpty()) return@synchronized
        // Filter out autoincrement columns
        val tableInfo = getTableInfo(tableName)
        val autoIncCols = tableInfo.columns.filter { it.isAutoIncrement }.map { it.name.lowercase() }
        val filteredValues = values.filterKeys { it.lowercase() !in autoIncCols }.toMutableMap()

        // Plan B: Automatic Foreign Key resolution if missing or blank
        val tableRels = try { getRelationships().filter { it.sourceTable.equals(tableName, ignoreCase = true) } } catch (_: Exception) { emptyList() }
        for (rel in tableRels) {
            val curVal = filteredValues[rel.sourceColumn]
                ?: filteredValues.entries.firstOrNull { it.key.equals(rel.sourceColumn, ignoreCase = true) }?.value
            if (curVal.isNullOrBlank()) {
                val targetRows = try { queryAll(rel.targetTable, limit = 1) } catch (_: Exception) { emptyList() }
                if (targetRows.isNotEmpty()) {
                    val defaultVal = targetRows[0][rel.targetColumn]
                        ?: targetRows[0].entries.firstOrNull { it.key.equals(rel.targetColumn, ignoreCase = true) }?.value
                    if (defaultVal != null) {
                        filteredValues[rel.sourceColumn] = defaultVal.toString()
                    }
                }
            }
        }

        if (filteredValues.isEmpty()) {
            // If all columns are autoincrement, just insert a default row
            conn().createStatement().use { stmt ->
                stmt.execute("INSERT INTO [$tableName] DEFAULT VALUES")
            }
            return@synchronized
        }

        fun doExecute(vals: Map<String, String?>) {
            val cols = vals.keys.joinToString(", ") { "[$it]" }
            val placeholders = vals.keys.joinToString(", ") { "?" }
            val sql = "INSERT INTO [$tableName] ($cols) VALUES ($placeholders)"
            val colTypeMap = tableInfo.columns.associate { it.name.lowercase() to it.type }

            conn().prepareStatement(sql).use { pstmt ->
                vals.entries.forEachIndexed { idx, entry ->
                    val value = entry.value
                    val colType = colTypeMap[entry.key.lowercase()] ?: ColumnType.TEXT
                    if (value == null || value.isBlank()) {
                        val sqlNullType = when (colType) {
                            ColumnType.INTEGER -> Types.INTEGER
                            ColumnType.LONG -> Types.BIGINT
                            ColumnType.REAL, ColumnType.DECIMAL -> Types.DOUBLE
                            ColumnType.BOOLEAN -> Types.BOOLEAN
                            ColumnType.DATE -> Types.DATE
                            ColumnType.DATETIME -> Types.TIMESTAMP
                            else -> Types.VARCHAR
                        }
                        pstmt.setNull(idx + 1, sqlNullType)
                    } else {
                        when (colType) {
                            ColumnType.INTEGER -> pstmt.setInt(idx + 1, value.toIntOrNull() ?: 0)
                            ColumnType.LONG -> pstmt.setLong(idx + 1, value.toLongOrNull() ?: 0L)
                            ColumnType.REAL, ColumnType.DECIMAL -> pstmt.setDouble(idx + 1, value.toDoubleOrNull() ?: 0.0)
                            ColumnType.BOOLEAN -> pstmt.setBoolean(idx + 1, value == "1" || value.equals("true", ignoreCase = true))
                            else -> pstmt.setString(idx + 1, value)
                        }
                    }
                }
                pstmt.executeUpdate()
            }
        }

        try {
            doExecute(filteredValues)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            // Plan B Auto-Recovery: if referential integrity failed due to a misconfigured PK constraint
            if (msg.contains("restricción de integridad", ignoreCase = true) ||
                msg.contains("integrity constraint", ignoreCase = true) ||
                msg.contains("clave foránea", ignoreCase = true)
            ) {
                val pks = tableInfo.columns.filter { it.isPrimaryKey }.map { it.name.lowercase() }
                val badPkRels = tableRels.filter { it.sourceColumn.lowercase() in pks }
                if (badPkRels.isNotEmpty()) {
                    for (bad in badPkRels) {
                        try { removeRelationship(bad.name, tableName) } catch (_: Exception) {}
                    }
                    doExecute(filteredValues)
                    return@synchronized
                }
            }
            throw e
        }
    }

    override fun updateRow(tableName: String, primaryKeyValues: Map<String, Any?>, newValues: Map<String, String?>): Unit = synchronized(lock) {
        if (newValues.isEmpty() || primaryKeyValues.isEmpty()) return@synchronized
        val tableInfo = getTableInfo(tableName)
        val autoIncCols = tableInfo.columns.filter { it.isAutoIncrement }.map { it.name.lowercase() }
        val updatableValues = newValues.filterKeys { it.lowercase() !in autoIncCols }
        if (updatableValues.isEmpty()) return@synchronized
        val colTypeMap = tableInfo.columns.associate { it.name to it.type }
        val setClauses = updatableValues.keys.joinToString(", ") { "[$it] = ?" }
        val whereClauses = primaryKeyValues.keys.joinToString(" AND ") { "[$it] = ?" }
        val sql = "UPDATE [$tableName] SET $setClauses WHERE $whereClauses"

        conn().prepareStatement(sql).use { pstmt ->
            var idx = 1
            for ((colName, value) in updatableValues) {
                val colType = colTypeMap[colName] ?: ColumnType.TEXT
                if (value == null || value.isBlank()) {
                    val sqlNullType = when (colType) {
                        ColumnType.INTEGER -> Types.INTEGER
                        ColumnType.LONG -> Types.BIGINT
                        ColumnType.REAL, ColumnType.DECIMAL -> Types.DOUBLE
                        ColumnType.BOOLEAN -> Types.BOOLEAN
                        ColumnType.DATE -> Types.DATE
                        ColumnType.DATETIME -> Types.TIMESTAMP
                        else -> Types.VARCHAR
                    }
                    pstmt.setNull(idx, sqlNullType)
                } else {
                    when (colType) {
                        ColumnType.INTEGER -> pstmt.setInt(idx, value.toIntOrNull() ?: 0)
                        ColumnType.LONG -> pstmt.setLong(idx, value.toLongOrNull() ?: 0L)
                        ColumnType.REAL, ColumnType.DECIMAL -> pstmt.setDouble(idx, value.toDoubleOrNull() ?: 0.0)
                        ColumnType.BOOLEAN -> pstmt.setBoolean(idx, value == "1" || value.equals("true", ignoreCase = true))
                        else -> pstmt.setString(idx, value)
                    }
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
        val whereClauses = primaryKeyValues.keys.joinToString(" AND ") { "[$it] = ?" }
        val sql = "DELETE FROM [$tableName] WHERE $whereClauses"

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
        sb.appendLine("-- DB Studio Access Export")
        sb.appendLine("-- Generated: ${java.time.LocalDateTime.now()}")
        sb.appendLine()

        val tables = getTableNames()

        for (table in tables) {
            val info = getTableInfo(table)
            sb.appendLine(info.toCreateAccessSql())
            sb.appendLine()

            // Export data
            val rows = queryAll(table, limit = Int.MAX_VALUE)
            if (rows.isNotEmpty()) {
                for (row in rows) {
                    val cols = info.columns
                        .filter { !it.isAutoIncrement }
                        .joinToString(", ") { "[${it.name}]" }
                    val vals = info.columns
                        .filter { !it.isAutoIncrement }
                        .joinToString(", ") { col ->
                            val v = row[col.name]
                            when (v) {
                                null -> "NULL"
                                is Number -> v.toString()
                                is Boolean -> if (v) "TRUE" else "FALSE"
                                else -> "'${v.toString().replace("'", "''")}'"
                            }
                        }
                    sb.appendLine("INSERT INTO [$table] ($cols) VALUES ($vals);")
                }
                sb.appendLine()
            }
        }

        // Export relationships
        val relationships = getRelationships()
        if (relationships.isNotEmpty()) {
            sb.appendLine("-- Relationships")
            for (rel in relationships) {
                sb.appendLine(rel.toAccessAlterSql())
            }
        }

        sb.toString()
    }

    override fun executeRaw(sql: String): Int = synchronized(lock) {
        conn().createStatement().use { stmt ->
            if (sql.trim().uppercase().startsWith("SELECT")) {
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
