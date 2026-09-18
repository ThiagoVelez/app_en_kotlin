package com.dbstudio.model

/**
 * Represents the type of database engine.
 */
enum class DatabaseType(val displayName: String, val extensions: List<String>) {
    SQLITE("SQLite", listOf("db", "sqlite", "sqlite3")),
    ACCESS("Microsoft Access", listOf("accdb", "mdb"));

    companion object {
        fun fromExtension(ext: String): DatabaseType? {
            return entries.find { ext.lowercase() in it.extensions }
        }
    }
}

/**
 * Metadata about an open database.
 */
data class DatabaseInfo(
    val name: String,
    val type: DatabaseType,
    val filePath: String,
    val isNew: Boolean = false
)
