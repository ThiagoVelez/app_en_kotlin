package com.dbstudio.data

import com.dbstudio.model.DatabaseType
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.prefs.Preferences

data class RecentDatabase(
    val path: String,
    val name: String,
    val type: DatabaseType,
    val lastOpenedFormatted: String
)

/**
 * Manages recently opened SQLite and Access databases, persisting them across sessions
 * using standard Java Preferences.
 */
object RecentDatabasesManager {

    private const val PREF_KEY_RECENT = "recent_databases_list"
    private const val MAX_RECENT = 6
    private val prefs: Preferences = Preferences.userNodeForPackage(RecentDatabasesManager::class.java)

    fun getRecentDatabases(): List<RecentDatabase> {
        val raw = prefs.get(PREF_KEY_RECENT, "")
        if (raw.isBlank()) return emptyList()

        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                val path = parts[0]
                val name = parts[1]
                val type = DatabaseType.valueOf(parts[2])
                val date = parts[3]
                // Only return if file still exists on disk
                if (File(path).exists()) {
                    RecentDatabase(path, name, type, date)
                } else null
            } else null
        }
    }

    fun recordDatabase(path: String, type: DatabaseType) {
        val file = File(path)
        val name = file.nameWithoutExtension
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        val current = getRecentDatabases().toMutableList()
        // Remove if already in list to put it at top
        current.removeAll { it.path.equals(path, ignoreCase = true) }

        val newEntry = RecentDatabase(file.absolutePath, name, type, now)
        current.add(0, newEntry)

        val trimmed = current.take(MAX_RECENT)
        val serialized = trimmed.joinToString(";") { "${it.path}|${it.name}|${it.type.name}|${it.lastOpenedFormatted}" }
        prefs.put(PREF_KEY_RECENT, serialized)
    }

    fun removeDatabase(path: String) {
        val current = getRecentDatabases().toMutableList()
        current.removeAll { it.path.equals(path, ignoreCase = true) }
        val serialized = current.joinToString(";") { "${it.path}|${it.name}|${it.type.name}|${it.lastOpenedFormatted}" }
        prefs.put(PREF_KEY_RECENT, serialized)
    }

    fun clearRecent() {
        prefs.remove(PREF_KEY_RECENT)
    }
}
