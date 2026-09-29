package com.dbstudio.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.*
import kotlinx.coroutines.*

/**
 * ViewModel for the ER Diagram screen.
 * Manages entity positions, zoom, pan, and relationship visualization.
 */
class DiagramViewModel {

    // ── Entity positions ────────────────────────────────────────────

    /** Map of table name to position (top-left corner). */
    var entityPositions by mutableStateOf<Map<String, Offset>>(emptyMap())
        private set

    var tables by mutableStateOf<List<TableInfo>>(emptyList())
        private set

    var relationships by mutableStateOf<List<Relationship>>(emptyList())
        private set

    var selectedRelationship by mutableStateOf<Relationship?>(null)

    var isOperatingOnRelationship by mutableStateOf(false)
        private set

    // ── Viewport ────────────────────────────────────────────────────

    var zoom by mutableStateOf(1f)
        private set

    var panOffset by mutableStateOf(Offset.Zero)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Entity dimensions (constants for layout) ────────────────────

    companion object {
        const val ENTITY_WIDTH = 220f
        const val ENTITY_HEADER_HEIGHT = 36f
        const val ENTITY_ROW_HEIGHT = 24f
        const val ENTITY_PADDING = 8f
        const val LAYOUT_SPACING_X = 300f
        const val LAYOUT_SPACING_Y = 40f
    }

    /**
     * Calculate the height of an entity box based on column count.
     */
    fun entityHeight(table: TableInfo): Float {
        return ENTITY_HEADER_HEIGHT + (table.columns.size * ENTITY_ROW_HEIGHT) + ENTITY_PADDING * 2
    }

    /**
     * Load all tables and relationships for the diagram.
     */
    fun loadDiagram(engine: DatabaseEngine) {
        scope.launch {
            try {
                errorMessage = null
                val tableNames = engine.getTableNames()
                val tableInfos = tableNames.map { engine.getTableInfo(it) }
                val rels = engine.getRelationships()

                withContext(Dispatchers.Main) {
                    tables = tableInfos
                    relationships = rels

                    // Only auto-layout if positions haven't been set or new tables appeared
                    val existingNames = entityPositions.keys
                    val newTables = tableInfos.filter { it.name !in existingNames }

                    if (newTables.isNotEmpty() || entityPositions.isEmpty()) {
                        autoLayout(tableInfos)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error loading diagram: ${e.message}"
                }
            }
        }
    }

    /**
     * Arrange entities in an optimal multi-column layout automatically,
     * packing entities cleanly without overlap based on individual table heights.
     */
    fun autoLayout(tableList: List<TableInfo> = tables) {
        if (tableList.isEmpty()) return
        val positions = mutableMapOf<String, Offset>()
        val cols = if (tableList.size <= 2) tableList.size.coerceAtLeast(1) else 3
        val colHeights = FloatArray(cols) { 40f }

        tableList.forEach { table ->
            // Find column with the minimum height
            var bestCol = 0
            var minH = colHeights[0]
            for (c in 1 until cols) {
                if (colHeights[c] < minH) {
                    minH = colHeights[c]
                    bestCol = c
                }
            }

            val x = 40f + bestCol * LAYOUT_SPACING_X
            val y = colHeights[bestCol]
            positions[table.name] = Offset(x, y)

            // Calculate height of this table box
            val h = entityHeight(table)
            colHeights[bestCol] += h + LAYOUT_SPACING_Y
        }
        entityPositions = positions
    }

    /**
     * Move an entity to a new position (drag).
     */
    fun moveEntity(tableName: String, newPosition: Offset) {
        entityPositions = entityPositions.toMutableMap().also {
            it[tableName] = newPosition
        }
    }

    /**
     * Apply zoom change.
     */
    fun applyZoom(delta: Float) {
        zoom = (zoom + delta).coerceIn(0.3f, 3f)
    }

    /**
     * Apply pan change.
     */
    fun applyPan(delta: Offset) {
        panOffset = Offset(panOffset.x + delta.x, panOffset.y + delta.y)
    }

    /**
     * Reset zoom and pan to defaults.
     */
    fun resetView() {
        zoom = 1f
        panOffset = Offset.Zero
    }

    /**
     * Fit all entities in the view.
     */
    fun fitToView(viewportWidth: Float, viewportHeight: Float) {
        if (entityPositions.isEmpty()) return

        val minX = entityPositions.values.minOf { it.x }
        val minY = entityPositions.values.minOf { it.y }
        val maxX = entityPositions.values.maxOf { it.x } + ENTITY_WIDTH
        val maxY = entityPositions.values.maxOf { it.y } + 300f

        val contentWidth = maxX - minX
        val contentHeight = maxY - minY

        zoom = minOf(
            viewportWidth / (contentWidth + 80f),
            viewportHeight / (contentHeight + 80f)
        ).coerceIn(0.3f, 2f)

        panOffset = Offset(
            x = (viewportWidth - contentWidth * zoom) / 2 - minX * zoom,
            y = (viewportHeight - contentHeight * zoom) / 2 - minY * zoom
        )
    }

    fun dismissError() {
        errorMessage = null
    }

    fun selectRelationship(relationship: Relationship?) {
        selectedRelationship = relationship
    }

    fun bringToFront(tableName: String) {
        val current = tables
        val entity = current.find { it.name == tableName } ?: return
        tables = current.filter { it.name != tableName } + entity
    }

    fun deleteRelationship(engine: DatabaseEngine, relationship: Relationship, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                isOperatingOnRelationship = true
                errorMessage = null
                engine.removeRelationship(relationship.name, relationship.sourceTable)
                val rels = engine.getRelationships()
                withContext(Dispatchers.Main) {
                    relationships = rels
                    if (selectedRelationship?.name == relationship.name) {
                        selectedRelationship = null
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al eliminar relación: ${e.message}"
                }
            } finally {
                isOperatingOnRelationship = false
            }
        }
    }

    fun updateRelationship(
        engine: DatabaseEngine,
        oldRelationship: Relationship,
        newRelationship: Relationship,
        onComplete: () -> Unit = {}
    ) {
        scope.launch {
            try {
                isOperatingOnRelationship = true
                errorMessage = null
                engine.removeRelationship(oldRelationship.name, oldRelationship.sourceTable)
                engine.addRelationship(newRelationship)
                val rels = engine.getRelationships()
                withContext(Dispatchers.Main) {
                    relationships = rels
                    selectedRelationship = newRelationship
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error al actualizar relación: ${e.message}"
                }
            } finally {
                isOperatingOnRelationship = false
            }
        }
    }
}

