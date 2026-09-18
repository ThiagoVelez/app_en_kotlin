package com.dbstudio.viewmodel

import androidx.compose.runtime.*
import com.dbstudio.data.*
import com.dbstudio.model.*
import kotlinx.coroutines.*
import java.io.File

/**
 * Main application ViewModel. Manages the database connection,
 * table list, and coordinates between sub-viewmodels.
 */
class AppViewModel {

    // ── State ───────────────────────────────────────────────────────

    var databaseInfo by mutableStateOf<DatabaseInfo?>(null)
        private set

    var tables by mutableStateOf<List<String>>(emptyList())
        private set

    var selectedTable by mutableStateOf<String?>(null)
        private set

    var currentScreen by mutableStateOf(Screen.OVERVIEW)
        private set

    var statusMessage by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var showNewDbDialog by mutableStateOf(false)
    var showNewTableDialog by mutableStateOf(false)
    var showRenameTableDialog by mutableStateOf(false)
    var showExportDialog by mutableStateOf(false)
    var showRelationshipDialog by mutableStateOf(false)
    var showConfirmDeleteTable by mutableStateOf(false)
    var showUserManual by mutableStateOf(false)
    var showCommandPalette by mutableStateOf(false)
    var isDarkMode by mutableStateOf(false)

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }

    /** Revision counter to trigger silent dynamic data updates across screens */
    var dataSyncRevision by mutableStateOf(0)
        private set

    fun notifyDataChanged() {
        dataSyncRevision++
        refreshOverviewMetrics()
    }

    // ── Overview & Health Check State ───────────────────────────────
    var tableRowCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var tableInfos by mutableStateOf<Map<String, TableInfo>>(emptyMap())
        private set
    var totalDbRows by mutableStateOf(0)
        private set
    var dbFileSizeFormatted by mutableStateOf("0 KB")
        private set
    var totalRelationships by mutableStateOf(0)
        private set
    var healthReport by mutableStateOf(SchemaHealthReport(100, "Excelente", emptyList()))
        private set

    // ── Engine ──────────────────────────────────────────────────────

    private var engine: DatabaseEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getEngine(): DatabaseEngine? = engine

    // ── Screen Navigation ───────────────────────────────────────────

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    // ── Database Operations ─────────────────────────────────────────

    fun createDatabase(name: String, type: DatabaseType, directory: String) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val ext = if (type == DatabaseType.SQLITE) "db" else "accdb"
                val cleanName = name.trim()
                    .removeSuffix(".db")
                    .removeSuffix(".sqlite")
                    .removeSuffix(".sqlite3")
                    .removeSuffix(".accdb")
                    .removeSuffix(".mdb")
                val dirFile = File(directory)
                if (!dirFile.exists()) {
                    dirFile.mkdirs()
                }
                val file = File(dirFile, "$cleanName.$ext")
                val path = file.absolutePath

                val newEngine = if (type == DatabaseType.SQLITE) SqliteEngine() else AccessEngine()
                newEngine.connect(path, create = true)

                withContext(Dispatchers.Main) {
                    engine?.disconnect()
                    engine = newEngine
                    databaseInfo = DatabaseInfo(cleanName, type, path, isNew = true)
                    refreshTables()
                    selectedTable = null
                    currentScreen = Screen.OVERVIEW
                    statusMessage = "Base de datos '$cleanName' creada con éxito"
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al crear la base de datos: ${e.message ?: e.javaClass.simpleName}"
                    statusMessage = "Error: ${e.message ?: e.javaClass.simpleName}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun openDatabase(path: String) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val file = File(path)
                if (!file.exists()) {
                    throw IllegalArgumentException("El archivo seleccionado no existe: $path")
                }
                val ext = file.extension.lowercase()
                val type = DatabaseType.fromExtension(ext)
                    ?: throw IllegalArgumentException("Tipo de archivo no soportado: .$ext (Formatos válidos: .db, .sqlite, .sqlite3, .accdb, .mdb)")

                val newEngine = if (type == DatabaseType.SQLITE) SqliteEngine() else AccessEngine()
                newEngine.connect(file.absolutePath)

                withContext(Dispatchers.Main) {
                    engine?.disconnect()
                    engine = newEngine
                    databaseInfo = DatabaseInfo(file.nameWithoutExtension, type, file.absolutePath)
                    refreshTables()
                    selectedTable = tables.firstOrNull()
                    currentScreen = Screen.OVERVIEW
                    statusMessage = "Base de datos abierta: '${file.name}'"
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al abrir la base de datos: ${e.message ?: e.javaClass.simpleName}"
                    statusMessage = "Error: ${e.message ?: e.javaClass.simpleName}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun closeDatabase() {
        engine?.disconnect()
        engine = null
        databaseInfo = null
        tables = emptyList()
        selectedTable = null
        statusMessage = "Base de datos cerrada"
    }

    // ── Table Operations ────────────────────────────────────────────

    fun refreshTables() {
        val eng = engine ?: return
        try {
            tables = eng.getTableNames()
            notifyDataChanged()
        } catch (e: Throwable) {
            e.printStackTrace()
            errorMessage = "Error al actualizar lista de tablas: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun selectTable(name: String?) {
        selectedTable = name
    }

    fun createTable(name: String) {
        val eng = engine ?: return
        scope.launch {
            try {
                errorMessage = null
                // Create table with a default ID column
                val defaultColumns = listOf(
                    Column(
                        name = "id",
                        type = ColumnType.AUTOINCREMENT,
                        isPrimaryKey = true,
                        isAutoIncrement = true,
                        isNullable = false
                    )
                )
                eng.createTable(TableInfo(name, defaultColumns))
                withContext(Dispatchers.Main) {
                    refreshTables()
                    selectedTable = name
                    statusMessage = "Tabla '$name' creada con éxito"
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al crear la tabla: ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }

    fun dropSelectedTable() {
        val eng = engine ?: return
        val table = selectedTable ?: return
        scope.launch {
            try {
                errorMessage = null
                eng.dropTable(table)
                withContext(Dispatchers.Main) {
                    refreshTables()
                    selectedTable = tables.firstOrNull()
                    statusMessage = "Tabla '$table' eliminada"
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al eliminar la tabla: ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }

    fun renameSelectedTable(newName: String) {
        val eng = engine ?: return
        val oldName = selectedTable ?: return
        scope.launch {
            try {
                errorMessage = null
                eng.renameTable(oldName, newName)
                withContext(Dispatchers.Main) {
                    refreshTables()
                    selectedTable = newName
                    statusMessage = "Tabla renombrada: '$oldName' → '$newName'"
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al renombrar la tabla: ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }

    // ── Export ──────────────────────────────────────────────────────

    fun exportToSql(outputPath: String) {
        val eng = engine ?: return
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                val sql = eng.exportToSql()
                File(outputPath).writeText(sql)
                withContext(Dispatchers.Main) {
                    statusMessage = "Exportado exitosamente a '$outputPath'"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al exportar: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun exportToAccdb(outputPath: String) {
        val eng = engine ?: return
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                // Create a new Access database and copy schema+data
                val accessEngine = AccessEngine()
                accessEngine.connect(outputPath, create = true)

                val tableNames = eng.getTableNames()
                for (tableName in tableNames) {
                    val info = eng.getTableInfo(tableName)
                    accessEngine.createTable(info)

                    val rows = eng.queryAll(tableName, limit = Int.MAX_VALUE)
                    for (row in rows) {
                        val stringValues = row
                            .filterKeys { key -> !info.columns.any { it.name == key && it.isAutoIncrement } }
                            .mapValues { (_, v) -> v?.toString() }
                        if (stringValues.isNotEmpty()) {
                            accessEngine.insertRow(tableName, stringValues)
                        }
                    }
                }

                // Copy relationships
                val rels = eng.getRelationships()
                for (rel in rels) {
                    try {
                        accessEngine.addRelationship(rel)
                    } catch (_: Exception) {
                        // Skip relationships that fail in Access
                    }
                }

                accessEngine.disconnect()

                withContext(Dispatchers.Main) {
                    statusMessage = "Exportado exitosamente a '$outputPath'"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al exportar: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun refreshOverviewMetrics() {
        val eng = engine ?: return
        val info = databaseInfo ?: return
        scope.launch {
            try {
                val currentTables = eng.getTableNames()
                val counts = mutableMapOf<String, Int>()
                val infos = mutableMapOf<String, TableInfo>()
                var totalRows = 0

                for (tbl in currentTables) {
                    try {
                        val count = eng.countRows(tbl)
                        counts[tbl] = count
                        totalRows += count
                        infos[tbl] = eng.getTableInfo(tbl)
                    } catch (_: Exception) {
                        counts[tbl] = 0
                    }
                }

                // File size
                val file = File(info.filePath)
                val sizeFormatted = if (file.exists()) {
                    val bytes = file.length()
                    if (bytes < 1024) "$bytes B"
                    else if (bytes < 1024 * 1024) "%.1f KB".format(bytes / 1024.0)
                    else "%.2f MB".format(bytes / (1024.0 * 1024.0))
                } else "0 KB"

                // Schema Health Check
                val issues = mutableListOf<SchemaHealthIssue>()
                for ((tblName, tblInfo) in infos) {
                    if (tblInfo.columns.none { it.isPrimaryKey }) {
                        issues.add(
                            SchemaHealthIssue(
                                severity = IssueSeverity.WARNING,
                                title = "Tabla '$tblName' sin Clave Primaria",
                                description = "Se recomienda definir al menos una columna como PK para identificar unívocamente cada registro.",
                                tableName = tblName
                            )
                        )
                    }
                    if (tblInfo.columns.isEmpty()) {
                        issues.add(
                            SchemaHealthIssue(
                                severity = IssueSeverity.ERROR,
                                title = "Tabla '$tblName' sin Columnas",
                                description = "Esta tabla no contiene campos definidos.",
                                tableName = tblName
                            )
                        )
                    }
                }

                val score = (100 - issues.count { it.severity == IssueSeverity.ERROR } * 30 - issues.count { it.severity == IssueSeverity.WARNING } * 10).coerceIn(0, 100)
                val statusText = when {
                    score >= 90 -> "Excelente"
                    score >= 70 -> "Aceptable"
                    else -> "Requiere Atención"
                }

                val relCount = try { eng.getRelationships().size } catch (_: Exception) { 0 }

                withContext(Dispatchers.Main) {
                    tableRowCounts = counts
                    tableInfos = infos
                    totalDbRows = totalRows
                    totalRelationships = relCount
                    dbFileSizeFormatted = sizeFormatted
                    healthReport = SchemaHealthReport(score, statusText, issues)
                }
            } catch (_: Exception) {
                // Ignore background metric failures
            }
        }
    }

    fun saveDatabase() {
        val info = databaseInfo ?: return
        statusMessage = "Base de datos '${info.name}' guardada"
        // SQLite auto-saves; Access via UCanAccess also auto-commits
    }

    fun dismissError() {
        errorMessage = null
    }

    fun cleanup() {
        engine?.disconnect()
        scope.cancel()
    }
}

/**
 * Screens available in the application navigation.
 */
enum class Screen(val displayName: String) {
    OVERVIEW("Panel General"),
    TABLE_DESIGNER("Diseñador de Tablas"),
    DATA_VIEW("Vista de Datos"),
    SQL_QUERY("Consola SQL"),
    DIAGRAM("Diagrama ER")
}

data class SchemaHealthIssue(
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val tableName: String? = null
)

enum class IssueSeverity {
    WARNING,
    ERROR,
    SUGGESTION
}

data class SchemaHealthReport(
    val score: Int,
    val statusText: String,
    val issues: List<SchemaHealthIssue>
)
