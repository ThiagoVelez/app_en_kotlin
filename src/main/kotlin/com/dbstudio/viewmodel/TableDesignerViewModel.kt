package com.dbstudio.viewmodel

import androidx.compose.runtime.*
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.*
import kotlinx.coroutines.*

/**
 * ViewModel for the Table Designer screen.
 * Manages column editing for the selected table.
 */
class TableDesignerViewModel {

    var columns by mutableStateOf<List<Column>>(emptyList())
        private set

    var hasUnsavedChanges by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    private var originalColumns: List<Column> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val undoStack = mutableListOf<List<Column>>()
    private val redoStack = mutableListOf<List<Column>>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private fun pushHistory() {
        undoStack.add(columns.toList())
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(columns.toList())
        columns = undoStack.removeAt(undoStack.lastIndex)
        hasUnsavedChanges = columns != originalColumns
        updateUndoRedoState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(columns.toList())
        columns = redoStack.removeAt(redoStack.lastIndex)
        hasUnsavedChanges = columns != originalColumns
        updateUndoRedoState()
    }

    /**
     * Load columns from the database for the given table.
     */
    fun loadTable(engine: DatabaseEngine, tableName: String) {
        scope.launch {
            try {
                errorMessage = null
                successMessage = null
                val info = engine.getTableInfo(tableName)
                withContext(Dispatchers.Main) {
                    columns = info.columns
                    originalColumns = info.columns.toList()
                    undoStack.clear()
                    redoStack.clear()
                    updateUndoRedoState()
                    hasUnsavedChanges = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al cargar tabla: ${e.message}"
                }
            }
        }
    }

    fun clear() {
        columns = emptyList()
        originalColumns = emptyList()
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoState()
        hasUnsavedChanges = false
        errorMessage = null
        successMessage = null
    }

    /**
     * Add a new column to the list (unsaved).
     */
    fun addColumn(column: Column) {
        // Check for duplicate names
        if (columns.any { it.name.equals(column.name, ignoreCase = true) }) {
            errorMessage = "Ya existe una columna llamada '${column.name}'"
            return
        }
        pushHistory()
        columns = columns + column
        hasUnsavedChanges = true
        errorMessage = null
    }

    /**
     * Add a column preset with automatic name collision handling.
     */
    fun addPreset(preset: Column) {
        var baseName = preset.name
        var finalName = baseName
        var counter = 2
        while (columns.any { it.name.equals(finalName, ignoreCase = true) }) {
            finalName = "${baseName}_$counter"
            counter++
        }
        val col = preset.copy(name = finalName)
        pushHistory()
        columns = columns + col
        hasUnsavedChanges = true
        errorMessage = null
        successMessage = "Columna '${col.name}' añadida"
    }

    /**
     * Update a column at the given index.
     */
    fun updateColumn(index: Int, column: Column) {
        if (index < 0 || index >= columns.size) return
        // Check for duplicate names (excluding self)
        if (columns.filterIndexed { i, _ -> i != index }
                .any { it.name.equals(column.name, ignoreCase = true) }) {
            errorMessage = "Ya existe una columna llamada '${column.name}'"
            return
        }
        pushHistory()
        columns = columns.toMutableList().also { it[index] = column }
        hasUnsavedChanges = true
        errorMessage = null
    }

    /**
     * Remove a column at the given index.
     */
    fun removeColumn(index: Int) {
        if (index < 0 || index >= columns.size) return
        pushHistory()
        columns = columns.toMutableList().also { it.removeAt(index) }
        hasUnsavedChanges = true
    }

    /**
     * Move a column to the left (up in the list).
     */
    fun moveColumnLeft(index: Int) {
        moveColumnUp(index)
    }

    /**
     * Move a column to the right (down in the list).
     */
    fun moveColumnRight(index: Int) {
        moveColumnDown(index)
    }

    /**
     * Move a column up in the list.
     */
    fun moveColumnUp(index: Int) {
        if (index <= 0 || index >= columns.size) return
        pushHistory()
        val mutable = columns.toMutableList()
        val temp = mutable[index]
        mutable[index] = mutable[index - 1]
        mutable[index - 1] = temp
        columns = mutable
        hasUnsavedChanges = true
    }

    /**
     * Move a column down in the list.
     */
    fun moveColumnDown(index: Int) {
        if (index < 0 || index >= columns.size - 1) return
        pushHistory()
        val mutable = columns.toMutableList()
        val temp = mutable[index]
        mutable[index] = mutable[index + 1]
        mutable[index + 1] = temp
        columns = mutable
        hasUnsavedChanges = true
    }

    fun togglePrimaryKey(index: Int) {
        if (index !in columns.indices) return
        val col = columns[index]
        val newPk = !col.isPrimaryKey
        updateColumn(index, col.copy(
            isPrimaryKey = newPk,
            isNullable = if (newPk) false else col.isNullable
        ))
    }

    fun toggleNotNull(index: Int) {
        if (index !in columns.indices) return
        val col = columns[index]
        if (col.isPrimaryKey) return // PK is always NOT NULL
        updateColumn(index, col.copy(isNullable = !col.isNullable))
    }

    fun toggleUnique(index: Int) {
        if (index !in columns.indices) return
        val col = columns[index]
        updateColumn(index, col.copy(isUnique = !col.isUnique))
    }

    fun toggleAutoIncrement(index: Int) {
        if (index !in columns.indices) return
        val col = columns[index]
        val newAi = !col.isAutoIncrement
        updateColumn(index, col.copy(
            isAutoIncrement = newAi,
            type = if (newAi) ColumnType.AUTOINCREMENT else ColumnType.INTEGER,
            isPrimaryKey = if (newAi) true else col.isPrimaryKey,
            isNullable = if (newAi) false else col.isNullable
        ))
    }

    /**
     * Save the current column definitions to the database.
     */
    fun saveChanges(engine: DatabaseEngine, tableName: String, onSaved: () -> Unit = {}) {
        if (columns.isEmpty()) {
            errorMessage = "La tabla debe tener al menos una columna"
            return
        }
        scope.launch {
            try {
                errorMessage = null
                engine.updateTableSchema(tableName, columns)
                withContext(Dispatchers.Main) {
                    originalColumns = columns.toList()
                    hasUnsavedChanges = false
                    successMessage = "Estructura de la tabla '$tableName' guardada exitosamente"
                    onSaved()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al guardar tabla: ${e.message}"
                }
            }
        }
    }

    fun dismissMessages() {
        errorMessage = null
        successMessage = null
    }
}
