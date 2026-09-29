@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dbstudio.data.RecentDatabase
import com.dbstudio.model.DatabaseType
import com.dbstudio.viewmodel.Screen

enum class RibbonTab(val displayName: String) {
    ARCHIVO("Archivo"),
    INICIO("Inicio"),
    INSERTAR("Insertar"),
    DATOS("Datos"),
    VISTA("Vista")
}

/**
 * Microsoft Office (Excel / Access / Word) style top Ribbon Toolbar.
 * Replaces sidebars with an organized, horizontal ribbon tab bar that maximizes screen real estate.
 */
@Composable
fun OfficeRibbonBar(
    databaseName: String?,
    databaseType: DatabaseType?,
    tables: List<String> = emptyList(),
    selectedTable: String? = null,
    tableRowCounts: Map<String, Int> = emptyMap(),
    currentScreen: Screen,
    onScreenChanged: (Screen) -> Unit,
    onTableSelected: (String) -> Unit = {},
    onNewDatabase: () -> Unit = {},
    onOpenDatabase: () -> Unit = {},
    onNewTable: () -> Unit = {},
    onDeleteTable: (String) -> Unit = {},
    onRenameTable: (String) -> Unit = {},
    onNewRelationship: () -> Unit = {},
    onOpenIntermediateDialog: () -> Unit = {},
    onAddCustomField: () -> Unit = {},
    onInsertRecord: () -> Unit = {},
    onGenerateMockData: () -> Unit = {},
    onOpenBrainDoctor: () -> Unit = {},
    onBackupDatabase: () -> Unit = {},
    onExportExcel: () -> Unit = {},
    onImportExcel: () -> Unit = {},
    onExportSql: () -> Unit = {},
    recentDatabases: List<RecentDatabase> = emptyList(),
    onOpenRecent: (String) -> Unit = {},
    onAutoOrganizeDiagram: () -> Unit = {},
    onOpenCommandPalette: () -> Unit = {},
    onShowUserManual: () -> Unit = {},
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(RibbonTab.INICIO) }
    var isRibbonCollapsed by remember { mutableStateOf(false) }

    // Dropdown states
    var showRecentMenu by remember { mutableStateOf(false) }
    var showTableSelectMenu by remember { mutableStateOf(false) }
    var tableSearchQuery by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Top Title & Quick Access Bar ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: App Logo + Database Status Chip + Ribbon Tabs
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // App Logo Icon
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = "DataBase Studio",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Database Status Pill
                    Surface(
                        color = if (databaseName != null) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (databaseName != null) Color(0xFF22C55E) else Color(0xFF94A3B8))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = databaseName ?: "Sin base de datos",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            if (databaseType != null) {
                                Text(
                                    text = " (${databaseType.displayName})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Ribbon Tabs (ARCHIVO, INICIO, INSERTAR, DATOS, VISTA)
                    RibbonTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        val isFileTab = tab == RibbonTab.ARCHIVO

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .clickable {
                                    activeTab = tab
                                    if (isRibbonCollapsed) isRibbonCollapsed = false
                                },
                            color = when {
                                isFileTab && isSelected -> MaterialTheme.colorScheme.primary
                                isFileTab -> if (isDarkMode) Color(0xFF15803D) else Color(0xFF16A34A)
                                isSelected -> MaterialTheme.colorScheme.surface
                                else -> Color.Transparent
                            },
                            border = if (isSelected && !isFileTab) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)) else null
                        ) {
                            Text(
                                text = tab.displayName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = when {
                                    isFileTab -> Color.White
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                    }
                }

                // Right: Quick Search, Dark Mode, Collapse Ribbon Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Table Selector Chip (Accessible directly from header)
                    if (tables.isNotEmpty()) {
                        Box {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showTableSelectMenu = true },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = selectedTable?.let { "Tabla: $it" } ?: "Seleccionar tabla...",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Table Dropdown Menu
                            DropdownMenu(
                                expanded = showTableSelectMenu,
                                onDismissRequest = {
                                    showTableSelectMenu = false
                                    tableSearchQuery = ""
                                }
                            ) {
                                Column(modifier = Modifier.width(220.dp).padding(4.dp)) {
                                    Text(
                                        "Tablas de la Base de Datos (${tables.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    tables.forEach { tbl ->
                                        val isCurrent = tbl == selectedTable
                                        val count = tableRowCounts[tbl]
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        tbl,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (count != null) {
                                                        Text(
                                                            "$count reg.",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    if (isCurrent) Icons.Default.Check else Icons.Default.TableRows,
                                                    contentDescription = null,
                                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                onTableSelected(tbl)
                                                showTableSelectMenu = false
                                            }
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    DropdownMenuItem(
                                        text = { Text("+ Nueva Tabla...", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        },
                                        onClick = {
                                            showTableSelectMenu = false
                                            onNewTable()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Command Palette (Ctrl+K)
                    FilledTonalButton(
                        onClick = onOpenCommandPalette,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ctrl+K", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Dark Mode Toggle
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar tema",
                            tint = if (isDarkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // User Manual (F1)
                    IconButton(
                        onClick = onShowUserManual,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Manual de Usuario",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Ribbon Collapse / Expand Toggle
                    IconButton(
                        onClick = { isRibbonCollapsed = !isRibbonCollapsed },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isRibbonCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isRibbonCollapsed) "Expandir cinta de opciones" else "Contraer cinta de opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ── Ribbon Body: Tools Toolbar ───────────────────────────────
            if (!isRibbonCollapsed) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    val ribbonScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(ribbonScrollState)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (activeTab) {
                            RibbonTab.ARCHIVO -> {
                                // ── Group 1: Base de Datos ──
                                RibbonGroup(label = "Base de Datos") {
                                    RibbonBigButton(
                                        icon = Icons.Default.Add,
                                        label = "Nueva BD",
                                        onClick = onNewDatabase
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.FolderOpen,
                                        label = "Abrir BD",
                                        onClick = onOpenDatabase
                                    )
                                    Box {
                                        RibbonBigButton(
                                            icon = Icons.Default.History,
                                            label = "Recientes (${recentDatabases.size})",
                                            onClick = { showRecentMenu = true }
                                        )
                                        DropdownMenu(
                                            expanded = showRecentMenu,
                                            onDismissRequest = { showRecentMenu = false }
                                        ) {
                                            if (recentDatabases.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("No hay bases de datos recientes") },
                                                    onClick = {}
                                                )
                                            } else {
                                                recentDatabases.forEach { rec ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text(rec.name, fontWeight = FontWeight.Bold)
                                                                Text(rec.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                if (rec.type == DatabaseType.SQLITE) Icons.Default.Storage else Icons.Default.FolderOpen,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        },
                                                        onClick = {
                                                            showRecentMenu = false
                                                            onOpenRecent(rec.path)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                RibbonSeparator()

                                // ── Group 2: Seguridad & Copias ──
                                RibbonGroup(label = "Seguridad") {
                                    RibbonBigButton(
                                        icon = Icons.Default.Backup,
                                        label = "Copia Respaldo",
                                        onClick = onBackupDatabase,
                                        enabled = databaseName != null
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 3: Intercambio ──
                                RibbonGroup(label = "Importar / Exportar") {
                                    RibbonBigButton(
                                        icon = Icons.Default.FileDownload,
                                        label = "Exportar Excel",
                                        onClick = onExportExcel,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.FileUpload,
                                        label = "Importar Excel",
                                        onClick = onImportExcel,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.Code,
                                        label = "Exportar SQL",
                                        onClick = onExportSql,
                                        enabled = databaseName != null
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 4: Ayuda & Sistema ──
                                RibbonGroup(label = "General") {
                                    RibbonBigButton(
                                        icon = Icons.AutoMirrored.Filled.MenuBook,
                                        label = "Manual (F1)",
                                        onClick = onShowUserManual
                                    )
                                    RibbonBigButton(
                                        icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        label = if (isDarkMode) "Modo Claro" else "Modo Oscuro",
                                        onClick = onToggleDarkMode
                                    )
                                }
                            }

                            RibbonTab.INICIO -> {
                                // ── Group 1: Modos de Trabajo / Pantallas Principales ──
                                RibbonGroup(label = "Modos de Trabajo") {
                                    RibbonBigButton(
                                        icon = Icons.Default.Dashboard,
                                        label = "Panel General",
                                        isSelected = currentScreen == Screen.OVERVIEW,
                                        onClick = { onScreenChanged(Screen.OVERVIEW) }
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.TableChart,
                                        label = "Diseñador",
                                        isSelected = currentScreen == Screen.TABLE_DESIGNER,
                                        onClick = { onScreenChanged(Screen.TABLE_DESIGNER) }
                                    )
                                    RibbonBigButton(
                                        icon = Icons.AutoMirrored.Filled.ViewList,
                                        label = "Vista Datos",
                                        isSelected = currentScreen == Screen.DATA_VIEW,
                                        onClick = { onScreenChanged(Screen.DATA_VIEW) }
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.Terminal,
                                        label = "Consola SQL",
                                        isSelected = currentScreen == Screen.SQL_QUERY,
                                        onClick = { onScreenChanged(Screen.SQL_QUERY) }
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 2: Tabla Activa & Objetos ──
                                RibbonGroup(label = "Gestión de Tablas") {
                                    RibbonBigButton(
                                        icon = Icons.Default.Add,
                                        label = "Nueva Tabla",
                                        onClick = onNewTable,
                                        enabled = databaseName != null
                                    )
                                    if (selectedTable != null) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilledTonalButton(
                                                    onClick = { onRenameTable(selectedTable) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Renombrar", fontSize = 11.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { onDeleteTable(selectedTable) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Eliminar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Activa: $selectedTable",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                RibbonSeparator()

                                // ── Group 3: Herramientas Rápidas ──
                                RibbonGroup(label = "Operaciones Rápidas") {
                                    RibbonBigButton(
                                        icon = Icons.Default.Psychology,
                                        label = "Cerebro IA",
                                        onClick = onOpenBrainDoctor,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.Backup,
                                        label = "Copia Respaldo",
                                        onClick = onBackupDatabase,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.FileDownload,
                                        label = "A Excel (.xlsx)",
                                        onClick = onExportExcel,
                                        enabled = databaseName != null
                                    )
                                }
                            }

                            RibbonTab.INSERTAR -> {
                                // ── Group 1: Objetos Relacionales ──
                                RibbonGroup(label = "Nuevas Entidades") {
                                    RibbonBigButton(
                                        icon = Icons.Default.TableChart,
                                        label = "Nueva Tabla",
                                        onClick = onNewTable,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.AddLink,
                                        label = "Nueva Relación (1:N)",
                                        onClick = onNewRelationship,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.Hub,
                                        label = "Tabla N:M (Intermedia)",
                                        onClick = onOpenIntermediateDialog,
                                        enabled = databaseName != null
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 2: Estructura de Tabla ──
                                RibbonGroup(label = "Estructura") {
                                    RibbonBigButton(
                                        icon = Icons.Default.ViewColumn,
                                        label = "Campo Personalizado",
                                        onClick = onAddCustomField,
                                        enabled = databaseName != null && (selectedTable != null || tables.isNotEmpty())
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 3: Inserción de Registros ──
                                RibbonGroup(label = "Registros (Tabla Activa)") {
                                    RibbonBigButton(
                                        icon = Icons.Default.PostAdd,
                                        label = "Nuevo Registro",
                                        onClick = onInsertRecord,
                                        enabled = databaseName != null && selectedTable != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.AutoAwesome,
                                        label = "Llenar Datos (Mock)",
                                        onClick = onGenerateMockData,
                                        enabled = databaseName != null && selectedTable != null
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 3: Asistente ──
                                RibbonGroup(label = "Asistente Cerebro") {
                                    RibbonBigButton(
                                        icon = Icons.Default.Psychology,
                                        label = "Plantillas BD",
                                        onClick = onOpenBrainDoctor,
                                        enabled = databaseName != null
                                    )
                                }
                            }

                            RibbonTab.DATOS -> {
                                // ── Group 1: Operaciones Multitabla ──
                                RibbonGroup(label = "Consultas Avanzadas") {
                                    RibbonBigButton(
                                        icon = Icons.AutoMirrored.Filled.CallMerge,
                                        label = "Juntar Tablas (JOIN)",
                                        isSelected = currentScreen == Screen.JOIN_TABLES,
                                        onClick = { onScreenChanged(Screen.JOIN_TABLES) }
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.FilterAlt,
                                        label = "Consulta ETL",
                                        isSelected = currentScreen == Screen.ETL_QUERY,
                                        onClick = { onScreenChanged(Screen.ETL_QUERY) }
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.Terminal,
                                        label = "Consola SQL",
                                        isSelected = currentScreen == Screen.SQL_QUERY,
                                        onClick = { onScreenChanged(Screen.SQL_QUERY) }
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 2: Intercambio de Datos ──
                                RibbonGroup(label = "Importar / Exportar") {
                                    RibbonBigButton(
                                        icon = Icons.Default.FileDownload,
                                        label = "Exportar a Excel",
                                        onClick = onExportExcel,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.FileUpload,
                                        label = "Importar de Excel",
                                        onClick = onImportExcel,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.Code,
                                        label = "Exportar SQL DDL",
                                        onClick = onExportSql,
                                        enabled = databaseName != null
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 3: Diagnóstico Local ──
                                RibbonGroup(label = "Salud y Diagnóstico") {
                                    RibbonBigButton(
                                        icon = Icons.Default.Psychology,
                                        label = "Doctor Cerebro",
                                        onClick = onOpenBrainDoctor,
                                        enabled = databaseName != null
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.Backup,
                                        label = "Copia Respaldo",
                                        onClick = onBackupDatabase,
                                        enabled = databaseName != null
                                    )
                                }
                            }

                            RibbonTab.VISTA -> {
                                // ── Group 1: Diagramas ──
                                RibbonGroup(label = "Diagramas") {
                                    RibbonBigButton(
                                        icon = Icons.Default.AccountTree,
                                        label = "Diagrama ER",
                                        isSelected = currentScreen == Screen.DIAGRAM,
                                        onClick = { onScreenChanged(Screen.DIAGRAM) }
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.AutoFixHigh,
                                        label = "Auto-Organizar",
                                        onClick = onAutoOrganizeDiagram,
                                        enabled = databaseName != null
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 2: Modos Visuales ──
                                RibbonGroup(label = "Navegación") {
                                    RibbonBigButton(
                                        icon = Icons.Default.Dashboard,
                                        label = "Panel General",
                                        isSelected = currentScreen == Screen.OVERVIEW,
                                        onClick = { onScreenChanged(Screen.OVERVIEW) }
                                    )
                                    RibbonBigButton(
                                        icon = Icons.AutoMirrored.Filled.ViewList,
                                        label = "Vista Datos",
                                        isSelected = currentScreen == Screen.DATA_VIEW,
                                        onClick = { onScreenChanged(Screen.DATA_VIEW) }
                                    )
                                    RibbonBigButton(
                                        icon = Icons.Default.TableChart,
                                        label = "Diseñador",
                                        isSelected = currentScreen == Screen.TABLE_DESIGNER,
                                        onClick = { onScreenChanged(Screen.TABLE_DESIGNER) }
                                    )
                                }

                                RibbonSeparator()

                                // ── Group 3: Interfaz & Apariencia ──
                                RibbonGroup(label = "Apariencia") {
                                    RibbonBigButton(
                                        icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        label = if (isDarkMode) "Modo Claro" else "Modo Oscuro",
                                        onClick = onToggleDarkMode
                                    )
                                    RibbonBigButton(
                                        icon = Icons.AutoMirrored.Filled.MenuBook,
                                        label = "Manual (F1)",
                                        onClick = onShowUserManual
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

/**
 * Logical section/group inside a ribbon tab, with a subtle bottom label and vertical borders.
 */
@Composable
private fun RibbonGroup(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

/**
 * Vertical subtle divider separating groups in the ribbon.
 */
@Composable
private fun RibbonSeparator() {
    VerticalDivider(
        modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

/**
 * Large Ribbon Action button with top icon and bottom label, matching Excel/Access look.
 */
@Composable
private fun RibbonBigButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 58.dp, max = 80.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(6.dp),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 11.sp
            )
        }
    }
}
