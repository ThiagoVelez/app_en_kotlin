package com.dbstudio.viewmodel

import androidx.compose.runtime.*
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.*
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * ViewModel for the Data View screen.
 * Manages CRUD operations on table rows with pagination, batch filling, and data generation.
 */
class DataViewModel {

    var rows by mutableStateOf<List<Map<String, Any?>>>(emptyList())
        private set

    var tableInfo by mutableStateOf<TableInfo?>(null)
        private set

    var totalRows by mutableStateOf(0)
        private set

    var currentPage by mutableStateOf(0)
        private set

    var pageSize by mutableStateOf(50)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var showInsertDialog by mutableStateOf(false)
    var showEditDialog by mutableStateOf(false)
    var showDeleteConfirm by mutableStateOf(false)
    var showGenerateDataDialog by mutableStateOf(false)

    var editingRow by mutableStateOf<Map<String, Any?>?>(null)
        private set

    var relationships by mutableStateOf<List<Relationship>>(emptyList())
        private set

    var relationshipLookups by mutableStateOf<Map<String, Map<String, String>>>(emptyMap())
        private set

    var foreignKeyOptions by mutableStateOf<Map<String, List<Pair<String, String>>>>(emptyMap())
        private set

    var sortColumn by mutableStateOf<String?>(null)
        private set

    var sortAscending by mutableStateOf(true)
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val totalPages: Int
        get() = if (totalRows == 0) 1 else ((totalRows - 1) / pageSize) + 1

    /**
     * Load data for a table with pagination and resolve relationships.
     * When isSilent is true, performs the refresh in the background without loading spinner or UI flicker.
     */
    fun loadData(engine: DatabaseEngine, tableName: String, isSilent: Boolean = false) {
        scope.launch {
            try {
                if (!isSilent) {
                    isLoading = true
                }
                errorMessage = null

                val info = engine.getTableInfo(tableName)
                val count = engine.countRows(tableName)
                val data = engine.queryAll(tableName, limit = pageSize, offset = currentPage * pageSize)

                // Fetch relationships and lookups
                val rels = try {
                    engine.getRelationships().filter { it.sourceTable.equals(tableName, ignoreCase = true) }
                } catch (_: Exception) {
                    emptyList()
                }

                val lookups = mutableMapOf<String, MutableMap<String, String>>()
                val options = mutableMapOf<String, MutableList<Pair<String, String>>>()

                for (rel in rels) {
                    val targetRows = try {
                        engine.queryAll(rel.targetTable, limit = 500)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    val targetInfo = try {
                        engine.getTableInfo(rel.targetTable)
                    } catch (_: Exception) {
                        null
                    }

                    val displayCol = targetInfo?.columns?.firstOrNull { col ->
                        val n = col.name.lowercase()
                        (n.contains("nombre") || n.contains("name") || n.contains("titulo") || n.contains("title") ||
                         n.contains("materia") || n.contains("descripcion") || n.contains("desc") ||
                         n.contains("usuario") || n.contains("cliente") || n.contains("codigo") || n.contains("detalle")) &&
                                col.type != ColumnType.AUTOINCREMENT
                    }?.name ?: targetInfo?.columns?.firstOrNull {
                        it.type == ColumnType.TEXT || it.type == ColumnType.VARCHAR
                    }?.name ?: rel.targetColumn

                    val colLookup = mutableMapOf<String, String>()
                    val colOptions = mutableListOf<Pair<String, String>>()

                    for (tRow in targetRows) {
                        val keyVal = (tRow[rel.targetColumn]
                            ?: tRow.entries.firstOrNull { it.key.equals(rel.targetColumn, ignoreCase = true) }?.value)
                            ?.toString() ?: continue
                        val displayVal = (tRow[displayCol]
                            ?: tRow.entries.firstOrNull { it.key.equals(displayCol, ignoreCase = true) }?.value)
                            ?.toString()?.ifBlank { null } ?: keyVal
                        val label = if (displayCol != rel.targetColumn) "$displayVal (#$keyVal)" else displayVal
                        colLookup[keyVal] = displayVal
                        colOptions.add(keyVal to label)
                    }

                    lookups[rel.sourceColumn] = colLookup
                    options[rel.sourceColumn] = colOptions
                    lookups[rel.sourceColumn.lowercase()] = colLookup
                    options[rel.sourceColumn.lowercase()] = colOptions
                }

                withContext(Dispatchers.Main) {
                    tableInfo = info
                    totalRows = count
                    relationships = rels
                    relationshipLookups = lookups
                    foreignKeyOptions = options
                    rows = if (sortColumn != null) {
                        sortRows(data)
                    } else {
                        data
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al cargar datos: ${e.message}"
                }
            } finally {
                if (!isSilent) {
                    isLoading = false
                }
            }
        }
    }

    /**
     * Silently reload current table without showing any loading spinner or wiping out the UI.
     */
    fun refreshSilently(engine: DatabaseEngine) {
        val currentTbl = tableInfo?.name ?: return
        loadData(engine, currentTbl, isSilent = true)
    }

    fun clear() {
        rows = emptyList()
        tableInfo = null
        totalRows = 0
        currentPage = 0
        relationships = emptyList()
        relationshipLookups = emptyMap()
        foreignKeyOptions = emptyMap()
        errorMessage = null
        successMessage = null
        sortColumn = null
    }

    fun nextPage(engine: DatabaseEngine, tableName: String) {
        if (currentPage < totalPages - 1) {
            currentPage++
            loadData(engine, tableName)
        }
    }

    fun previousPage(engine: DatabaseEngine, tableName: String) {
        if (currentPage > 0) {
            currentPage--
            loadData(engine, tableName)
        }
    }

    fun goToPage(page: Int, engine: DatabaseEngine, tableName: String) {
        if (page in 0 until totalPages) {
            currentPage = page
            loadData(engine, tableName)
        }
    }

    fun toggleSort(column: String) {
        if (sortColumn == column) {
            sortAscending = !sortAscending
        } else {
            sortColumn = column
            sortAscending = true
        }
        rows = sortRows(rows)
    }

    private fun sortRows(data: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val col = sortColumn ?: return data
        return if (sortAscending) {
            data.sortedWith(compareBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it[col]?.toString() })
        } else {
            data.sortedWith(compareByDescending(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it[col]?.toString() })
        }
    }

    /**
     * Insert a single row into the table.
     */
    fun insertRow(engine: DatabaseEngine, tableName: String, values: Map<String, String?>) {
        scope.launch {
            try {
                errorMessage = null
                engine.insertRow(tableName, values)
                withContext(Dispatchers.Main) {
                    successMessage = "Fila insertada correctamente"
                    showInsertDialog = false
                    loadData(engine, tableName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al insertar fila: ${e.message}"
                }
            }
        }
    }

    /**
     * Generate N realistic rows according to the table column types and schema.
     */
    fun generateData(engine: DatabaseEngine, tableName: String, count: Int) {
        val info = tableInfo ?: return
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val names = listOf("Carlos", "Maria", "Juan", "Ana", "Pedro", "Laura", "Diego", "Sofia", "Mateo", "Valentina", "Andres", "Camila")
                val lastNames = listOf("Perez", "Gomez", "Rodriguez", "Lopez", "Garcia", "Martinez", "Sanchez", "Ramirez", "Torres", "Flores")
                val cities = listOf("Bogota", "Medellin", "Cali", "Barranquilla", "Cartagena", "Pereira", "Manizales", "Bucaramanga")
                val products = listOf("Laptop Pro", "Teclado RGB", "Monitor 4K", "Mouse Gamer", "Impresora Láser", "Auriculares", "SSD 1TB", "Memoria RAM 16GB")

                val currentCount = totalRows
                for (i in 1..count) {
                    val rowValues = mutableMapOf<String, String?>()
                    val idx = currentCount + i
                    for (col in info.columns) {
                        if (col.isAutoIncrement) {
                            continue // Database assigns autoincrement
                        }

                        // Check if column is a foreign key
                        val fkOpts = foreignKeyOptions[col.name]
                        if (!fkOpts.isNullOrEmpty()) {
                            rowValues[col.name] = fkOpts[(idx - 1) % fkOpts.size].first
                            continue
                        }

                        val colNameLower = col.name.lowercase()
                        val value: String = when {
                            colNameLower.contains("cedula") || colNameLower.contains("dni") || colNameLower.contains("documento") || colNameLower.contains("identificacion") -> {
                                "${1010203040L + idx * 7L}"
                            }
                            colNameLower.contains("nombre") || colNameLower.contains("name") || colNameLower.contains("first") -> {
                                names[(idx - 1) % names.size]
                            }
                            colNameLower.contains("apellido") || colNameLower.contains("last") -> {
                                lastNames[(idx - 1) % lastNames.size]
                            }
                            colNameLower.contains("email") || colNameLower.contains("correo") -> {
                                "usuario$idx@email.com"
                            }
                            colNameLower.contains("telefono") || colNameLower.contains("phone") || colNameLower.contains("tel") || colNameLower.contains("celular") -> {
                                "300${1000000 + idx}"
                            }
                            colNameLower.contains("ciudad") || colNameLower.contains("city") -> {
                                cities[(idx - 1) % cities.size]
                            }
                            colNameLower.contains("producto") || colNameLower.contains("product") || colNameLower.contains("item") -> {
                                products[(idx - 1) % products.size]
                            }
                            colNameLower.contains("precio") || colNameLower.contains("price") || colNameLower.contains("valor") || colNameLower.contains("total") -> {
                                "%.2f".format(Random.nextDouble(10.0, 500.0)).replace(",", ".")
                            }
                            colNameLower.contains("edad") || colNameLower.contains("age") -> {
                                "${Random.nextInt(18, 65)}"
                            }
                            colNameLower.contains("cantidad") || colNameLower.contains("stock") || colNameLower.contains("qty") -> {
                                "${Random.nextInt(1, 100)}"
                            }
                            colNameLower.contains("creacion") || colNameLower.contains("fecha") || col.type == ColumnType.DATETIME -> {
                                LocalDateTime.now().minusHours((idx * 5L) % 500).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            }
                            col.type == ColumnType.DATE -> {
                                LocalDate.now().minusDays((idx * 3L) % 365).format(DateTimeFormatter.ISO_LOCAL_DATE)
                            }
                            else -> when (col.type) {
                                ColumnType.TEXT, ColumnType.VARCHAR -> "Texto_${col.name}_$idx"
                                ColumnType.INTEGER -> "$idx"
                                ColumnType.LONG -> "${1000000L + idx}"
                                ColumnType.REAL, ColumnType.DECIMAL -> "%.2f".format(Random.nextDouble(5.0, 100.0)).replace(",", ".")
                                ColumnType.BOOLEAN -> if (idx % 2 == 0) "1" else "0"
                                ColumnType.BLOB -> null.toString()
                                ColumnType.AUTOINCREMENT -> continue
                                ColumnType.DATE, ColumnType.DATETIME -> continue
                            }
                        }
                        rowValues[col.name] = value
                    }
                    engine.insertRow(tableName, rowValues)
                }

                withContext(Dispatchers.Main) {
                    successMessage = "Se generaron y llenaron $count registros en '$tableName'"
                    showGenerateDataDialog = false
                    loadData(engine, tableName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al llenar datos: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Update an existing row.
     */
    fun updateRow(engine: DatabaseEngine, tableName: String, pkValues: Map<String, Any?>, newValues: Map<String, String?>) {
        scope.launch {
            try {
                errorMessage = null
                engine.updateRow(tableName, pkValues, newValues)
                withContext(Dispatchers.Main) {
                    successMessage = "Registro actualizado correctamente"
                    showEditDialog = false
                    editingRow = null
                    loadData(engine, tableName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al actualizar fila: ${e.message}"
                }
            }
        }
    }

    /**
     * Delete a row by its primary key values.
     */
    fun deleteRow(engine: DatabaseEngine, tableName: String, pkValues: Map<String, Any?>) {
        scope.launch {
            try {
                errorMessage = null
                engine.deleteRow(tableName, pkValues)
                withContext(Dispatchers.Main) {
                    successMessage = "Fila eliminada correctamente"
                    showDeleteConfirm = false
                    editingRow = null
                    loadData(engine, tableName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al eliminar fila: ${e.message}"
                }
            }
        }
    }

    fun startEdit(row: Map<String, Any?>) {
        editingRow = row
        showEditDialog = true
    }

    fun startDelete(row: Map<String, Any?>) {
        editingRow = row
        showDeleteConfirm = true
    }

    /**
     * Get primary key values from a row.
     */
    fun getPrimaryKeyValues(row: Map<String, Any?>): Map<String, Any?> {
        val info = tableInfo ?: return emptyMap()
        val pkCols = info.primaryKeys.map { it.name }
        return if (pkCols.isNotEmpty()) {
            pkCols.associateWith { pk ->
                row[pk] ?: row.entries.firstOrNull { it.key.equals(pk, ignoreCase = true) }?.value
            }
        } else {
            row
        }
    }

    fun dismissMessages() {
        errorMessage = null
        successMessage = null
    }

    fun showSuccess(msg: String) {
        successMessage = msg
    }

    fun showError(msg: String) {
        errorMessage = msg
    }
}
