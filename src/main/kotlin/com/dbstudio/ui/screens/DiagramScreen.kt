@file:OptIn(ExperimentalTextApi::class)

package com.dbstudio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.RelationType
import com.dbstudio.model.Relationship
import com.dbstudio.model.TableInfo
import com.dbstudio.ui.components.FileChooser
import com.dbstudio.ui.theme.AppColors
import com.dbstudio.viewmodel.DiagramViewModel
import java.awt.BasicStroke
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.hypot

/**
 * ER Diagram screen with interactive canvas.
 * Supports smooth, unrestricted entity dragging (even when tables are related),
 * visual relationship selection, editing, and deletion.
 */
@Composable
fun DiagramScreen(
    engine: DatabaseEngine?,
    viewModel: DiagramViewModel,
    onNewRelationship: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Dialog state for editing / managing relationships
    var showManageRelDialog by remember { mutableStateOf(false) }
    var relationshipToEdit by remember { mutableStateOf<Relationship?>(null) }
    var relationshipToDelete by remember { mutableStateOf<Relationship?>(null) }

    // Load diagram data
    LaunchedEffect(engine) {
        if (engine != null) {
            viewModel.loadDiagram(engine)
        }
    }

    // Drag tracking
    var draggingEntity by remember { mutableStateOf<String?>(null) }
    var dragStartScreenOffset by remember { mutableStateOf(Offset.Zero) }
    var hasMovedDuringDrag by remember { mutableStateOf(false) }

    // Capture state with rememberUpdatedState so pointerInput NEVER needs to restart
    val currentTables by rememberUpdatedState(viewModel.tables)
    val currentPositions by rememberUpdatedState(viewModel.entityPositions)
    val currentZoom by rememberUpdatedState(viewModel.zoom)
    val currentPan by rememberUpdatedState(viewModel.panOffset)
    val currentRelationships by rememberUpdatedState(viewModel.relationships)

    Column(modifier = modifier.fillMaxSize()) {
        // ── Toolbar ─────────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Diagrama Entidad-Relación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "${viewModel.tables.size} tablas | ${viewModel.relationships.size} relaciones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Manage / list relationships button
                    OutlinedButton(
                        onClick = {
                            relationshipToEdit = null
                            showManageRelDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gestionar Relaciones", style = MaterialTheme.typography.labelMedium)
                    }

                    // New relationship button
                    Button(
                        onClick = {
                            relationshipToEdit = null
                            showManageRelDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nueva Relación", style = MaterialTheme.typography.labelMedium)
                    }


                    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                    IconButton(onClick = { viewModel.applyZoom(0.1f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Acercar")
                    }
                    Text(
                        "${(viewModel.zoom * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(onClick = { viewModel.applyZoom(-0.1f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Alejar")
                    }
                    IconButton(onClick = { viewModel.resetView() }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Centrar Vista")
                    }
                    IconButton(onClick = {
                        engine?.let { viewModel.loadDiagram(it) }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            }
        }

        // ── Canvas Area ─────────────────────────────────────────
        if (viewModel.tables.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No hay tablas para mostrar. Cree tablas primero para ver el diagrama de entidades.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        // pointerInput(Unit) ensures the drag detector is NEVER cancelled when entity positions change!
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { screenOffset ->
                                    dragStartScreenOffset = screenOffset
                                    hasMovedDuringDrag = false
                                    val worldOffset = screenToWorld(screenOffset, currentZoom, currentPan)
                                    val entity = findEntityAt(worldOffset, currentTables, currentPositions, viewModel)
                                    if (entity != null) {
                                        draggingEntity = entity
                                        viewModel.bringToFront(entity)
                                    } else {
                                        draggingEntity = null
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (hypot(dragAmount.x.toDouble(), dragAmount.y.toDouble()) > 1.0) {
                                        hasMovedDuringDrag = true
                                    }
                                    if (draggingEntity != null) {
                                        val currentPos = currentPositions[draggingEntity!!] ?: Offset.Zero
                                        viewModel.moveEntity(
                                            draggingEntity!!,
                                            Offset(
                                                currentPos.x + dragAmount.x / currentZoom,
                                                currentPos.y + dragAmount.y / currentZoom
                                            )
                                        )
                                    } else {
                                        viewModel.applyPan(dragAmount)
                                    }
                                },
                                onDragEnd = {
                                    if (!hasMovedDuringDrag) {
                                        // Tap detected!
                                        val worldOffset = screenToWorld(dragStartScreenOffset, currentZoom, currentPan)
                                        val clickedRel = findRelationshipAt(worldOffset, currentRelationships, currentTables, currentPositions)
                                        if (clickedRel != null) {
                                            viewModel.selectRelationship(clickedRel)
                                        } else {
                                            val clickedEntity = findEntityAt(worldOffset, currentTables, currentPositions, viewModel)
                                            if (clickedEntity != null) {
                                                viewModel.bringToFront(clickedEntity)
                                            }
                                            viewModel.selectRelationship(null)
                                        }
                                    }
                                    draggingEntity = null
                                    hasMovedDuringDrag = false
                                },
                                onDragCancel = {
                                    draggingEntity = null
                                    hasMovedDuringDrag = false
                                }
                            )
                        }
                ) {
                    val zoom = viewModel.zoom
                    val pan = viewModel.panOffset

                    // Draw background grid
                    drawGrid(zoom, pan, size)

                    // Apply canvas transformations
                    translate(left = pan.x, top = pan.y) {
                        scale(zoom, pivot = Offset.Zero) {
                            // Draw relationships below entities
                            viewModel.relationships.forEach { rel ->
                                val isSelected = viewModel.selectedRelationship?.name == rel.name
                                drawRelationship(
                                    rel = rel,
                                    tables = viewModel.tables,
                                    positions = viewModel.entityPositions,
                                    viewModel = viewModel,
                                    textMeasurer = textMeasurer,
                                    isSelected = isSelected
                                )
                            }

                            // Draw entities
                            viewModel.tables.forEach { table ->
                                drawEntity(
                                    table = table,
                                    position = viewModel.entityPositions[table.name] ?: Offset.Zero,
                                    viewModel = viewModel,
                                    textMeasurer = textMeasurer,
                                    isDragging = draggingEntity == table.name,
                                    relationships = viewModel.relationships
                                )
                            }
                        }
                    }
                }

                // ── Selected Relationship Action Bar (Floating Overlay) ──
                viewModel.selectedRelationship?.let { selectedRel ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Column {
                                Text(
                                    "${selectedRel.sourceTable}.${selectedRel.sourceColumn} → ${selectedRel.targetTable}.${selectedRel.targetColumn}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Tipo: ${selectedRel.type.symbol} | On Delete: ${selectedRel.onDelete.sql} | On Update: ${selectedRel.onUpdate.sql}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            // Edit Button
                            Button(
                                onClick = {
                                    relationshipToEdit = selectedRel
                                    showManageRelDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Editar", style = MaterialTheme.typography.labelMedium)
                            }

                            Spacer(Modifier.width(6.dp))

                            // Delete Button
                            FilledTonalButton(
                                onClick = { relationshipToDelete = selectedRel },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Eliminar", style = MaterialTheme.typography.labelMedium)
                            }

                            Spacer(Modifier.width(6.dp))

                            // Deselect
                            IconButton(
                                onClick = { viewModel.selectRelationship(null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Deseleccionar", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // ── Legend ──────────────────────────────────
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 76.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Leyenda", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        LegendItem(AppColors.primaryKey, "Clave Primaria (PK)")
                        LegendItem(AppColors.foreignKey, "Clave Foránea (FK)")
                        LegendItem(AppColors.unique, "Único (U)")
                        LegendItem(AppColors.autoIncrement, "Autoincremental (AI)")
                        LegendItem(Color(0xFFF59E0B), "Relación Seleccionada")
                    }
                }
            }
        }
    }

    // ── Relationship Dialog (Create / Edit / Manage) ────────
    if (showManageRelDialog && engine != null) {
        RelationshipDialog(
            engine = engine,
            initialRelationship = relationshipToEdit,
            onDismiss = {
                showManageRelDialog = false
                relationshipToEdit = null
            },
            onCreated = {
                viewModel.loadDiagram(engine)
            },
            onUpdated = { _, newRel ->
                viewModel.loadDiagram(engine)
                viewModel.selectRelationship(newRel)
            },
            onDeleted = {
                viewModel.loadDiagram(engine)
                viewModel.selectRelationship(null)
            }
        )
    }

    // ── Confirm Delete Relationship Dialog ──────────────────
    relationshipToDelete?.let { rel ->
        AlertDialog(
            onDismissRequest = { relationshipToDelete = null },
            title = { Text("Eliminar Relación") },
            text = {
                Text("¿Estás seguro de que deseas eliminar la relación entre '${rel.sourceTable}.${rel.sourceColumn}' y '${rel.targetTable}.${rel.targetColumn}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (engine != null) {
                            viewModel.deleteRelationship(engine, rel) {
                                relationshipToDelete = null
                            }
                        } else {
                            relationshipToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { relationshipToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color, radius = 5.dp.toPx())
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Drawing Functions ───────────────────────────────────────────────

private fun DrawScope.drawGrid(zoom: Float, pan: Offset, canvasSize: Size) {
    val gridSize = 40f * zoom
    val gridColor = AppColors.diagramGrid

    val startX = (pan.x % gridSize)
    val startY = (pan.y % gridSize)

    var x = startX
    while (x < canvasSize.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, canvasSize.height), strokeWidth = 0.5f)
        x += gridSize
    }
    var y = startY
    while (y < canvasSize.height) {
        drawLine(gridColor, Offset(0f, y), Offset(canvasSize.width, y), strokeWidth = 0.5f)
        y += gridSize
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawEntity(
    table: TableInfo,
    position: Offset,
    viewModel: DiagramViewModel,
    textMeasurer: TextMeasurer,
    isDragging: Boolean,
    relationships: List<Relationship>
) {
    val width = DiagramViewModel.ENTITY_WIDTH
    val headerHeight = DiagramViewModel.ENTITY_HEADER_HEIGHT
    val rowHeight = DiagramViewModel.ENTITY_ROW_HEIGHT
    val padding = DiagramViewModel.ENTITY_PADDING
    val totalHeight = viewModel.entityHeight(table)

    val fkColumns = relationships
        .filter { it.sourceTable == table.name }
        .map { it.sourceColumn }
        .toSet()

    // Shadow
    drawRoundRect(
        color = if (isDragging) Color(0x35000000) else Color(0x12000000),
        topLeft = Offset(position.x + (if (isDragging) 4 else 2), position.y + (if (isDragging) 4 else 2)),
        size = Size(width, totalHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )

    // Entity body
    drawRoundRect(
        color = AppColors.entityBody,
        topLeft = position,
        size = Size(width, totalHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )

    // Border
    drawRoundRect(
        color = if (isDragging) AppColors.selectedBorder else Color(0xFFCBD5E1),
        topLeft = position,
        size = Size(width, totalHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
        style = Stroke(width = if (isDragging) 2.5f else 1f)
    )

    // Header background
    val headerPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = position.x,
                top = position.y,
                right = position.x + width,
                bottom = position.y + headerHeight,
                topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(0f, 0f),
                bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(0f, 0f)
            )
        )
    }
    drawPath(headerPath, color = AppColors.entityHeader)

    // Header text
    val headerResult = textMeasurer.measure(
        text = AnnotatedString(table.name),
        style = TextStyle(
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    )
    drawText(
        headerResult,
        topLeft = Offset(
            position.x + (width - headerResult.size.width) / 2,
            position.y + (headerHeight - headerResult.size.height) / 2
        )
    )

    // Separator line
    drawLine(
        color = Color(0xFFE2E8F0),
        start = Offset(position.x, position.y + headerHeight),
        end = Offset(position.x + width, position.y + headerHeight),
        strokeWidth = 1f
    )

    // Column rows
    table.columns.forEachIndexed { index, column ->
        val rowY = position.y + headerHeight + padding + (index * rowHeight)

        val iconColor = when {
            column.isPrimaryKey -> AppColors.primaryKey
            column.name in fkColumns -> AppColors.foreignKey
            column.isUnique -> AppColors.unique
            else -> Color(0xFF94A3B8)
        }

        val iconLabel = when {
            column.isPrimaryKey && column.name in fkColumns -> "PFK"
            column.isPrimaryKey -> "PK"
            column.name in fkColumns -> "FK"
            column.isUnique -> "U"
            else -> "•"
        }

        val iconResult = textMeasurer.measure(
            text = AnnotatedString(iconLabel),
            style = TextStyle(color = iconColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
        drawText(iconResult, topLeft = Offset(position.x + 8, rowY + 2))

        val nameText = column.name
        val nameResult = textMeasurer.measure(
            text = AnnotatedString(nameText),
            style = TextStyle(
                color = Color(0xFF0F172A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
        drawText(nameResult, topLeft = Offset(position.x + 36, rowY + 2))

        val typeLabel = column.type.displayName
        val typeResult = textMeasurer.measure(
            text = AnnotatedString(typeLabel),
            style = TextStyle(
                color = Color(0xFF64748B),
                fontSize = 10.sp
            )
        )
        drawText(
            typeResult,
            topLeft = Offset(position.x + width - typeResult.size.width - 8, rowY + 3)
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawRelationship(
    rel: Relationship,
    tables: List<TableInfo>,
    positions: Map<String, Offset>,
    viewModel: DiagramViewModel,
    textMeasurer: TextMeasurer,
    isSelected: Boolean
) {
    val sourcePos = positions[rel.sourceTable] ?: return
    val targetPos = positions[rel.targetTable] ?: return
    val sourceTable = tables.find { it.name == rel.sourceTable } ?: return
    val targetTable = tables.find { it.name == rel.targetTable } ?: return

    val width = DiagramViewModel.ENTITY_WIDTH
    val headerHeight = DiagramViewModel.ENTITY_HEADER_HEIGHT
    val rowHeight = DiagramViewModel.ENTITY_ROW_HEIGHT
    val padding = DiagramViewModel.ENTITY_PADDING

    val sourceColIdx = sourceTable.columns.indexOfFirst { it.name == rel.sourceColumn }
    val targetColIdx = targetTable.columns.indexOfFirst { it.name == rel.targetColumn }

    if (sourceColIdx < 0 || targetColIdx < 0) return

    val sourceRowY = sourcePos.y + headerHeight + padding + (sourceColIdx * rowHeight) + rowHeight / 2
    val targetRowY = targetPos.y + headerHeight + padding + (targetColIdx * rowHeight) + rowHeight / 2

    val sourceCenterX = sourcePos.x + width / 2
    val targetCenterX = targetPos.x + width / 2

    val startX: Float
    val endX: Float
    if (sourceCenterX < targetCenterX) {
        startX = sourcePos.x + width
        endX = targetPos.x
    } else {
        startX = sourcePos.x
        endX = targetPos.x + width
    }

    val startPoint = Offset(startX, sourceRowY)
    val endPoint = Offset(endX, targetRowY)
    val midX = (startPoint.x + endPoint.x) / 2

    val path = Path().apply {
        moveTo(startPoint.x, startPoint.y)
        cubicTo(midX, startPoint.y, midX, endPoint.y, endPoint.x, endPoint.y)
    }

    val lineColor = if (isSelected) Color(0xFFF59E0B) else AppColors.relationLine
    val lineWidth = if (isSelected) 3.5f else 2f

    // If selected, draw glowing outline
    if (isSelected) {
        drawPath(
            path = path,
            color = Color(0xFFFDE68A).copy(alpha = 0.6f),
            style = Stroke(width = 8f, pathEffect = PathEffect.cornerPathEffect(10f))
        )
    }

    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = lineWidth, pathEffect = PathEffect.cornerPathEffect(10f))
    )

    // Markers on ends
    val footSize = if (isSelected) 12f else 10f
    when (rel.type) {
        RelationType.ONE_TO_MANY -> {
            drawLine(
                lineColor,
                Offset(endPoint.x, endPoint.y - footSize),
                Offset(endPoint.x + if (endX == targetPos.x) -footSize else footSize, endPoint.y),
                strokeWidth = lineWidth
            )
            drawLine(
                lineColor,
                Offset(endPoint.x, endPoint.y + footSize),
                Offset(endPoint.x + if (endX == targetPos.x) -footSize else footSize, endPoint.y),
                strokeWidth = lineWidth
            )
            val oneDx = if (startX == sourcePos.x + width) footSize else -footSize
            drawLine(
                lineColor,
                Offset(startPoint.x + oneDx, startPoint.y - footSize / 2),
                Offset(startPoint.x + oneDx, startPoint.y + footSize / 2),
                strokeWidth = lineWidth
            )
        }
        RelationType.ONE_TO_ONE -> {
            val dx1 = if (startX == sourcePos.x + width) footSize else -footSize
            drawLine(lineColor, Offset(startPoint.x + dx1, startPoint.y - footSize / 2), Offset(startPoint.x + dx1, startPoint.y + footSize / 2), strokeWidth = lineWidth)
            val dx2 = if (endX == targetPos.x) -footSize else footSize
            drawLine(lineColor, Offset(endPoint.x + dx2, endPoint.y - footSize / 2), Offset(endPoint.x + dx2, endPoint.y + footSize / 2), strokeWidth = lineWidth)
        }
        RelationType.MANY_TO_MANY -> {
            listOf(startPoint to (startX == sourcePos.x + width), endPoint to (endX == targetPos.x)).forEach { (pt, isRight) ->
                val dx = if (isRight) footSize else -footSize
                drawLine(lineColor, Offset(pt.x + dx, pt.y - footSize), Offset(pt.x, pt.y), strokeWidth = lineWidth)
                drawLine(lineColor, Offset(pt.x + dx, pt.y + footSize), Offset(pt.x, pt.y), strokeWidth = lineWidth)
            }
        }
    }

    // Relationship label badge
    val labelResult = textMeasurer.measure(
        text = AnnotatedString(rel.type.symbol),
        style = TextStyle(
            color = if (isSelected) Color(0xFFD97706) else AppColors.relationLine,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    )
    val labelTopLeft = Offset(midX - labelResult.size.width / 2, minOf(startPoint.y, endPoint.y) - 16)
    
    // Background pill for label
    drawRoundRect(
        color = if (isSelected) Color(0xFFFEF3C7) else Color.White.copy(alpha = 0.85f),
        topLeft = Offset(labelTopLeft.x - 4, labelTopLeft.y - 2),
        size = Size(labelResult.size.width + 8f, labelResult.size.height + 4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )
    drawText(labelResult, topLeft = labelTopLeft)
}

// ── Utility Functions ───────────────────────────────────────────────

private fun screenToWorld(screenOffset: Offset, zoom: Float, pan: Offset): Offset {
    return Offset(
        (screenOffset.x - pan.x) / zoom,
        (screenOffset.y - pan.y) / zoom
    )
}

private fun findEntityAt(
    worldOffset: Offset,
    tables: List<TableInfo>,
    positions: Map<String, Offset>,
    viewModel: DiagramViewModel
): String? {
    for (table in tables.reversed()) {
        val pos = positions[table.name] ?: continue
        val height = viewModel.entityHeight(table)
        if (worldOffset.x in pos.x..(pos.x + DiagramViewModel.ENTITY_WIDTH) &&
            worldOffset.y in pos.y..(pos.y + height)
        ) {
            return table.name
        }
    }
    return null
}

private fun findRelationshipAt(
    worldOffset: Offset,
    relationships: List<Relationship>,
    tables: List<TableInfo>,
    positions: Map<String, Offset>
): Relationship? {
    val width = DiagramViewModel.ENTITY_WIDTH
    val headerHeight = DiagramViewModel.ENTITY_HEADER_HEIGHT
    val rowHeight = DiagramViewModel.ENTITY_ROW_HEIGHT
    val padding = DiagramViewModel.ENTITY_PADDING

    for (rel in relationships) {
        val sourcePos = positions[rel.sourceTable] ?: continue
        val targetPos = positions[rel.targetTable] ?: continue
        val sourceTable = tables.find { it.name == rel.sourceTable } ?: continue
        val targetTable = tables.find { it.name == rel.targetTable } ?: continue

        val sourceColIdx = sourceTable.columns.indexOfFirst { it.name == rel.sourceColumn }
        val targetColIdx = targetTable.columns.indexOfFirst { it.name == rel.targetColumn }
        if (sourceColIdx < 0 || targetColIdx < 0) continue

        val sourceRowY = sourcePos.y + headerHeight + padding + (sourceColIdx * rowHeight) + rowHeight / 2
        val targetRowY = targetPos.y + headerHeight + padding + (targetColIdx * rowHeight) + rowHeight / 2

        val sourceCenterX = sourcePos.x + width / 2
        val targetCenterX = targetPos.x + width / 2

        val startX = if (sourceCenterX < targetCenterX) sourcePos.x + width else sourcePos.x
        val endX = if (sourceCenterX < targetCenterX) targetPos.x else targetPos.x + width

        val startPoint = Offset(startX, sourceRowY)
        val endPoint = Offset(endX, targetRowY)
        val midX = (startPoint.x + endPoint.x) / 2
        val midY = (startPoint.y + endPoint.y) / 2

        // Proximity to midpoint or label
        val distToMid = hypot((worldOffset.x - midX).toDouble(), (worldOffset.y - midY).toDouble())
        if (distToMid <= 30.0) return rel

        // Proximity to bounding box
        val minX = minOf(startPoint.x, endPoint.x) - 15f
        val maxX = maxOf(startPoint.x, endPoint.x) + 15f
        val minY = minOf(startPoint.y, endPoint.y) - 20f
        val maxY = maxOf(startPoint.y, endPoint.y) + 20f

        if (worldOffset.x in minX..maxX && worldOffset.y in minY..maxY) {
            val tSamples = listOf(0.2f, 0.4f, 0.6f, 0.8f)
            for (t in tSamples) {
                val invT = 1f - t
                val cx = invT * invT * invT * startPoint.x + 3 * invT * invT * t * midX + 3 * invT * t * t * midX + t * t * t * endPoint.x
                val cy = invT * invT * invT * startPoint.y + 3 * invT * invT * t * startPoint.y + 3 * invT * t * t * endPoint.y + t * t * t * endPoint.y
                if (hypot((worldOffset.x - cx).toDouble(), (worldOffset.y - cy).toDouble()) <= 20.0) {
                    return rel
                }
            }
        }
    }
    return null
}

/**
 * Render and export the ER diagram entities and relationships into an HD PNG image.
 */
private fun exportDiagramToPng(
    tables: List<TableInfo>,
    positions: Map<String, Offset>,
    relationships: List<Relationship>,
    file: File
) {
    if (tables.isEmpty()) return

    val minX = positions.values.minOfOrNull { it.x } ?: 50f
    val minY = positions.values.minOfOrNull { it.y } ?: 50f
    val maxX = positions.values.maxOfOrNull { it.x + 260f } ?: 800f
    val maxY = positions.values.maxOfOrNull { it.y + 350f } ?: 600f

    val padding = 60
    val width = ((maxX - minX) + padding * 2).toInt().coerceIn(800, 4000)
    val height = ((maxY - minY) + padding * 2).toInt().coerceIn(600, 4000)

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    // Background
    g.color = java.awt.Color(248, 250, 252)
    g.fillRect(0, 0, width, height)

    val offsetX = padding - minX.toInt()
    val offsetY = padding - minY.toInt()

    // Title watermark
    g.color = java.awt.Color(100, 116, 139)
    g.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 16)
    g.drawString("Diagrama Entidad-Relación — DB Studio", padding, padding - 20)

    // Draw relationship lines
    g.color = java.awt.Color(79, 70, 229)
    g.stroke = BasicStroke(2.2f)
    for (rel in relationships) {
        val srcPos = positions[rel.sourceTable] ?: Offset.Zero
        val tgtPos = positions[rel.targetTable] ?: Offset.Zero
        val x1 = srcPos.x.toInt() + offsetX + 120
        val y1 = srcPos.y.toInt() + offsetY + 40
        val x2 = tgtPos.x.toInt() + offsetX + 120
        val y2 = tgtPos.y.toInt() + offsetY + 40
        g.drawLine(x1, y1, x2, y2)
    }

    // Draw table boxes
    for (tbl in tables) {
        val pos = positions[tbl.name] ?: Offset.Zero
        val x = pos.x.toInt() + offsetX
        val y = pos.y.toInt() + offsetY
        val w = 240
        val h = 42 + tbl.columns.size * 22

        // Card background
        g.color = java.awt.Color.WHITE
        g.fillRoundRect(x, y, w, h, 14, 14)
        g.color = java.awt.Color(203, 213, 225)
        g.drawRoundRect(x, y, w, h, 14, 14)

        // Header
        g.color = java.awt.Color(79, 70, 229)
        g.fillRoundRect(x, y, w, 36, 14, 14)
        g.fillRect(x, y + 20, w, 16)

        g.color = java.awt.Color.WHITE
        g.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 13)
        g.drawString(tbl.name, x + 12, y + 23)

        // Columns
        g.font = java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11)
        var rowY = y + 54
        for (col in tbl.columns) {
            if (col.isPrimaryKey) {
                g.color = java.awt.Color(217, 119, 6)
                g.drawString("PK", x + 10, rowY)
            } else {
                g.color = java.awt.Color(148, 163, 184)
                g.drawString("•", x + 12, rowY)
            }
            g.color = java.awt.Color(15, 23, 42)
            g.drawString(col.name, x + 34, rowY)
            g.color = java.awt.Color(100, 116, 139)
            g.drawString(col.type.displayName, x + 155, rowY)
            rowY += 22
        }
    }

    g.dispose()
    ImageIO.write(image, "PNG", file)
}
