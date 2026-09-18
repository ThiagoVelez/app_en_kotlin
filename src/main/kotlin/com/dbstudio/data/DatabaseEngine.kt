package com.dbstudio.data

import com.dbstudio.model.*

/**
 * Common interface for database operations across SQLite and Access.
 * All implementations must be thread-safe for use with coroutines.
 */
interface DatabaseEngine {

    /** Whether the engine currently has an active connection. */
    val isConnected: Boolean

    /** The type of database this engine manages. */
    val databaseType: DatabaseType

    // ── Connection ──────────────────────────────────────────────────

    /** Open a connection to the database at the given path. Creates it if [create] is true. */
    fun connect(path: String, create: Boolean = false)

    /** Close the active connection. */
    fun disconnect()

    // ── Schema: Tables ──────────────────────────────────────────────

    /** Get names of all user tables. */
    fun getTableNames(): List<String>

    /** Get full table info including columns. */
    fun getTableInfo(tableName: String): TableInfo

    /** Create a new table with the given definition. */
    fun createTable(table: TableInfo)

    /** Drop a table by name. */
    fun dropTable(tableName: String)

    /** Rename a table. */
    fun renameTable(oldName: String, newName: String)

    // ── Schema: Columns ─────────────────────────────────────────────

    /** Add a column to an existing table. */
    fun addColumn(tableName: String, column: Column)

    /** Update a table's schema by recreating it with the new column definitions (full replace). */
    fun updateTableSchema(tableName: String, newColumns: List<Column>)

    // ── Schema: Relationships ───────────────────────────────────────

    /** Get all foreign key relationships. */
    fun getRelationships(): List<Relationship>

    /** Add a foreign key relationship. */
    fun addRelationship(relationship: Relationship)

    /** Remove a foreign key relationship by name. */
    fun removeRelationship(relationshipName: String, sourceTable: String)

    // ── CRUD ────────────────────────────────────────────────────────

    /**
     * Query all rows from a table with optional pagination.
     * Returns a list of maps where each map is column_name -> value.
     */
    fun queryAll(tableName: String, limit: Int = 100, offset: Int = 0): List<Map<String, Any?>>

    /** Count total rows in a table. */
    fun countRows(tableName: String): Int

    /** Insert a row into a table. Values map: column_name -> value. */
    fun insertRow(tableName: String, values: Map<String, String?>)

    /** Update a row. [primaryKeyValues] identifies the row, [newValues] are the changes. */
    fun updateRow(tableName: String, primaryKeyValues: Map<String, Any?>, newValues: Map<String, String?>)

    /** Delete a row by its primary key values. */
    fun deleteRow(tableName: String, primaryKeyValues: Map<String, Any?>)

    // ── Export ──────────────────────────────────────────────────────

    /**
     * Export the entire database schema and data as a SQL script string.
     */
    fun exportToSql(): String

    /**
     * Execute a raw SQL statement (DDL or DML). Returns affected rows for DML, 0 for DDL.
     */
    fun executeRaw(sql: String): Int

    /**
     * Execute any SQL statement and return structured results with column metadata, rows,
     * and execution time measurement.
     */
    fun executeQuery(sql: String): QueryResult
}

/**
 * Result of executing an arbitrary SQL query or statement.
 */
data class QueryResult(
    val columns: List<String> = emptyList(),
    val rows: List<List<Any?>> = emptyList(),
    val affectedRows: Int = 0,
    val executionTimeMs: Long = 0L,
    val isSelect: Boolean = false,
    val message: String? = null
)
