package com.dbstudio.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.Column
import com.dbstudio.model.ColumnPresets
import com.dbstudio.model.ColumnType
import com.dbstudio.ui.components.ColumnEditDialog
import com.dbstudio.ui.theme.AppColors
import com.dbstudio.viewmodel.TableDesignerViewModel

/**
 * Table Designer screen with a simple and friendly MySQL / phpMyAdmin-style grid layout.
 */
@Composable
fun TableDesignerScreen(
    tableName: String?,
    engine: DatabaseEngine?,
    viewModel: TableDesignerViewModel,
    onNavigateToData: ((String) -> Unit)? = null,
    onSchemaSaved: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editingColumnIndex by remember { mutableStateOf<Int?>(null) }
    var showSqlPreview by remember { mutableStateOf(false) }
    var copiedSqlMessage by remember { mutableStateOf<String?>(null) }

    // Load table when it changes
    LaunchedEffect(tableName) {
        if (tableName != null && engine != null) {
            viewModel.loadTable(engine, tableName)
        } else {
            viewModel.clear()
        }
    }

    // Plan B: Guardado automático silencioso si el usuario olvida hacer clic en "Guardar Esquema"
    LaunchedEffect(viewModel.hasUnsavedChanges, viewModel.columns.size) {
        if (viewModel.hasUnsavedChanges && engine != null && tableName != null && viewModel.columns.isNotEmpty()) {
            kotlinx.coroutines.delay(4000)
            if (viewModel.hasUnsavedChanges) {
                viewModel.saveChanges(engine, tableName) {
                    onSchemaSaved()
                }
            }
        }
    }

    // Plan B: Si el usuario sale de la pantalla o cambia de tabla teniendo cambios pendientes, se guardan automáticamente
    DisposableEffect(tableName) {
        onDispose {
            if (viewModel.hasUnsavedChanges && engine != null && tableName != null && viewModel.columns.isNotEmpty()) {
                viewModel.saveChanges(engine, tableName) {
                    onSchemaSaved()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (tableName != null) "Tabla: $tableName" else "Selecciona una tabla",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (viewModel.hasUnsavedChanges) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Cambios pendientes",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "${viewModel.columns.size} campos configurados — Editor visual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (tableName != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            tint = if (viewModel.canUndo) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.canRedo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            tint = if (viewModel.canRedo) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onNavigateToData != null) {
                        FilledTonalButton(
                            onClick = { onNavigateToData(tableName) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver Datos", maxLines = 1, softWrap = false)
                        }
                    }
                    FilledTonalButton(
                        onClick = { showSqlPreview = !showSqlPreview },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (showSqlPreview) "Ocultar SQL" else "Vista SQL", maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = { viewModel.showAddColumnDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir Campo", maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            if (engine != null) {
                                viewModel.saveChanges(engine, tableName) {
                                    onSchemaSaved()
                                }
                            }
                        },
                        enabled = viewModel.hasUnsavedChanges,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Icon(
                            imageVector = if (viewModel.hasUnsavedChanges) Icons.Default.Save else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (viewModel.hasUnsavedChanges) "Guardar Esquema" else "Guardado",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Messages ────────────────────────────────────────────
        viewModel.errorMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.dismissMessages() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        viewModel.successMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.dismissMessages() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (tableName == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Selecciona una tabla en el menú lateral para ver y diseñar sus columnas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        // ── Quick Column Presets Bar ────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Plantillas Rápidas:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.id()) },
                    label = { Text("+ ID") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.cedula()) },
                    label = { Text("+ Cédula") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.nombre()) },
                    label = { Text("+ Nombre") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.apellido()) },
                    label = { Text("+ Apellido") },
                    leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.correo()) },
                    label = { Text("+ Correo") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.celular()) },
                    label = { Text("+ Celular") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.telefono()) },
                    label = { Text("+ Teléfono") },
                    leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.fechaCreacion()) },
                    label = { Text("+ Fecha Creación") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.observaciones()) },
                    label = { Text("+ Observaciones") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.addPreset(ColumnPresets.descripcion()) },
                    label = { Text("+ Descripción") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Main Structure Editor: MySQL-Style Grid ──────────────
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Grid Header (MySQL Workbench Style)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Nombre de Columna", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tipo de Dato", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("PK", modifier = Modifier.width(55.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.primaryKey)
                        Text("NN", modifier = Modifier.width(55.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        Text("UQ", modifier = Modifier.width(55.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.unique)
                        Text("AI", modifier = Modifier.width(55.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.autoIncrement)
                        Text("Predeterminado", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Acciones", modifier = Modifier.width(130.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Rows List
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    if (viewModel.columns.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "La tabla aún no tiene columnas. Haz clic en 'Añadir Campo' o usa las Plantillas Rápidas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    viewModel.columns.forEachIndexed { index, column ->
                        MySqlColumnRow(
                            index = index,
                            column = column,
                            totalCount = viewModel.columns.size,
                            onTogglePk = { viewModel.togglePrimaryKey(index) },
                            onToggleNn = { viewModel.toggleNotNull(index) },
                            onToggleUq = { viewModel.toggleUnique(index) },
                            onToggleAi = { viewModel.toggleAutoIncrement(index) },
                            onMoveUp = { viewModel.moveColumnUp(index) },
                            onMoveDown = { viewModel.moveColumnDown(index) },
                            onEdit = { editingColumnIndex = index },
                            onDelete = { viewModel.removeColumn(index) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // Quick Add Row button at the bottom of the table
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showAddColumnDialog = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "+ Añadir nueva columna a la tabla...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val generatedDdl = remember(viewModel.columns, tableName, engine?.databaseType) {
            if (engine?.databaseType == com.dbstudio.model.DatabaseType.ACCESS) {
                com.dbstudio.model.TableInfo(tableName, viewModel.columns).toCreateAccessSql()
            } else {
                com.dbstudio.model.TableInfo(tableName, viewModel.columns).toCreateSqliteSql()
            }
        }

        if (showSqlPreview) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Sentencia SQL DDL Generada (${engine?.databaseType?.displayName ?: "SQLite"}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = {
                                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                    clipboard.setContents(java.awt.datatransfer.StringSelection(generatedDdl), null)
                                    copiedSqlMessage = "¡Copiado!"
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(copiedSqlMessage ?: "Copiar SQL", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(
                                onClick = { showSqlPreview = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val ddlScroll = rememberScrollState()
                        Text(
                            text = generatedDdl,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp).verticalScroll(ddlScroll)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Attributes Legend & Live Table Preview ──────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // MySQL Legend Card
            Surface(
                modifier = Modifier.width(360.dp).height(120.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "Guía de Atributos MySQL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    LegendRow("PK", "Clave Primaria (identificador único)", AppColors.primaryKey)
                    LegendRow("NN", "No Nulo (el campo es obligatorio)", Color(0xFF0284C7))
                    LegendRow("UQ", "Único (no admite valores repetidos)", AppColors.unique)
                    LegendRow("AI", "Autoincremental (número automático)", AppColors.autoIncrement)
                }
            }

            // Live Table Preview
            Surface(
                modifier = Modifier.weight(1f).height(120.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Vista Previa de la Tabla (Columnas Horizontales):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val previewScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(previewScroll)
                            .padding(6.dp)
                    ) {
                        // Headers
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)) {
                                viewModel.columns.forEach { col ->
                                    Box(
                                        modifier = Modifier.width(130.dp).padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (col.isPrimaryKey) {
                                                Icon(Icons.Default.Key, contentDescription = null, tint = AppColors.primaryKey, modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(
                                                col.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sample Row
                        listOf("1" to "Carlos", "2" to "María").forEachIndexed { rIdx, (num, name) ->
                            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                viewModel.columns.forEach { col ->
                                    Box(
                                        modifier = Modifier.width(130.dp).padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        val mockVal = when {
                                            col.isAutoIncrement -> "#$num"
                                            col.name.contains("cedula") -> "${1010203040 + rIdx}"
                                            col.name.contains("nombre") -> name
                                            col.name.contains("apellido") -> if (rIdx == 0) "Pérez" else "Gómez"
                                            col.name.contains("correo") -> if (rIdx == 0) "carlos@mail.com" else "maria@mail.com"
                                            col.name.contains("fecha") -> "2026-09-17"
                                            else -> "—"
                                        }
                                        Text(
                                            mockVal,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────
    if (viewModel.showAddColumnDialog) {
        ColumnEditDialog(
            title = "Añadir Columna a '$tableName'",
            onDismiss = { viewModel.showAddColumnDialog = false },
            onSave = { col ->
                viewModel.addColumn(col)
                viewModel.showAddColumnDialog = false
            }
        )
    }

    editingColumnIndex?.let { idx ->
        val col = viewModel.columns.getOrNull(idx)
        if (col != null) {
            ColumnEditDialog(
                title = "Editar Columna '${col.name}'",
                initialColumn = col,
                onDismiss = { editingColumnIndex = null },
                onSave = { newCol ->
                    viewModel.updateColumn(idx, newCol)
                    editingColumnIndex = null
                }
            )
        }
    }
}

/**
 * A single row representing a column definition in the MySQL-style table editor.
 */
@Composable
private fun MySqlColumnRow(
    index: Int,
    column: Column,
    totalCount: Int,
    onTogglePk: () -> Unit,
    onToggleNn: () -> Unit,
    onToggleUq: () -> Unit,
    onToggleAi: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp)
            )

            // Column Name + Icon
            Row(
                modifier = Modifier.weight(1.8f).clickable { onEdit() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when {
                        column.isPrimaryKey -> Icons.Default.Key
                        column.name.contains("cedula") -> Icons.Default.Badge
                        column.name.contains("nombre") || column.name.contains("apellido") -> Icons.Default.Person
                        column.name.contains("correo") -> Icons.Default.Email
                        column.type == ColumnType.DATETIME || column.type == ColumnType.DATE -> Icons.Default.CalendarToday
                        else -> Icons.Default.TextFields
                    },
                    contentDescription = null,
                    tint = if (column.isPrimaryKey) AppColors.primaryKey else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Data Type badge
            Box(modifier = Modifier.weight(1.4f)) {
                Surface(
                    color = getTypeColor(column.type).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { onEdit() }
                ) {
                    Text(
                        text = column.type.displayName + (column.length?.let { "($it)" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = getTypeColor(column.type),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // PK (Primary Key)
            Box(modifier = Modifier.width(55.dp), contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = column.isPrimaryKey,
                    onCheckedChange = { onTogglePk() },
                    colors = CheckboxDefaults.colors(checkedColor = AppColors.primaryKey)
                )
            }

            // NN (Not Null)
            Box(modifier = Modifier.width(55.dp), contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = !column.isNullable,
                    onCheckedChange = { onToggleNn() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7)),
                    enabled = !column.isPrimaryKey
                )
            }

            // UQ (Unique)
            Box(modifier = Modifier.width(55.dp), contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = column.isUnique,
                    onCheckedChange = { onToggleUq() },
                    colors = CheckboxDefaults.colors(checkedColor = AppColors.unique)
                )
            }

            // AI (Auto Increment)
            Box(modifier = Modifier.width(55.dp), contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = column.isAutoIncrement,
                    onCheckedChange = { onToggleAi() },
                    colors = CheckboxDefaults.colors(checkedColor = AppColors.autoIncrement)
                )
            }

            // Default value
            Text(
                text = column.defaultValue ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.2f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Action buttons (Up, Down, Edit, Delete)
            Row(
                modifier = Modifier.width(130.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Mover Arriba", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Mover Abajo", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LegendRow(code: String, description: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(3.dp),
            modifier = Modifier.size(20.dp, 16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(code, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getTypeColor(type: ColumnType): Color {
    return when (type) {
        ColumnType.INTEGER, ColumnType.LONG -> Color(0xFF0284C7)
        ColumnType.REAL, ColumnType.DECIMAL -> Color(0xFFD97706)
        ColumnType.TEXT, ColumnType.VARCHAR -> Color(0xFF059669)
        ColumnType.BOOLEAN -> Color(0xFF7C3AED)
        ColumnType.DATE, ColumnType.DATETIME -> Color(0xFFDB2777)
        ColumnType.BLOB -> Color(0xFF64748B)
        ColumnType.AUTOINCREMENT -> AppColors.primaryKey
    }
}
