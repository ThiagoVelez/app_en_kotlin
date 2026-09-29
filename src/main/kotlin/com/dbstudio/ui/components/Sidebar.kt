@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dbstudio.model.DatabaseType
import com.dbstudio.viewmodel.Screen

/**
 * Modern, collapsible left sidebar showing database status, screen navigation modes,
 * and quick database actions. Supports expanding and collapsing to an icon-only rail.
 */
@Composable
fun Sidebar(
    databaseName: String?,
    databaseType: DatabaseType?,
    tables: List<String> = emptyList(),
    selectedTable: String? = null,
    currentScreen: Screen,
    onTableSelected: (String) -> Unit = {},
    onScreenChanged: (Screen) -> Unit,
    onNewTable: () -> Unit,
    onDeleteTable: () -> Unit = {},
    onRenameTable: () -> Unit = {},
    onNewRelationship: () -> Unit = {},
    onRenameTableSpecific: (String) -> Unit = {},
    onDeleteTableSpecific: (String) -> Unit = {},
    onViewTableData: (String) -> Unit = {},
    onDesignTable: (String) -> Unit = {},
    onOpenCommandPalette: () -> Unit = {},
    onNewDatabase: () -> Unit = {},
    onOpenDatabase: () -> Unit = {},
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onShowUserManual: () -> Unit = {},
    onExportExcel: () -> Unit = {},
    onImportExcel: () -> Unit = {},
    onOpenBrainDoctor: () -> Unit = {},
    onBackupDatabase: () -> Unit = {},
    recentDatabases: List<com.dbstudio.data.RecentDatabase> = emptyList(),
    onOpenRecent: (String) -> Unit = {},
    isCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentWidth = if (isCollapsed) 68.dp else 240.dp

    Surface(
        modifier = modifier.width(currentWidth).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val sidebarScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(sidebarScrollState),
                    horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    // ── Database Header Card ────────────────────────
                    if (isCollapsed) {
                        // Collapsed Mode Header: DB Icon + Expand button
                        Spacer(Modifier.height(10.dp))
                        IconButton(
                            onClick = onToggleCollapse,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Expandir menú lateral",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (databaseType == DatabaseType.SQLITE)
                                        Icons.Default.Storage else Icons.Default.FolderOpen,
                                    contentDescription = databaseName ?: "DB",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Expanded Mode Header Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (databaseName != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (databaseType == DatabaseType.SQLITE)
                                                            Icons.Default.Storage else Icons.Default.FolderOpen,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = databaseName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF22C55E))
                                                    )
                                                    Spacer(Modifier.width(5.dp))
                                                    Text(
                                                        text = databaseType?.displayName ?: "Conectado",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }

                                        // Collapse button
                                        IconButton(
                                            onClick = onToggleCollapse,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ChevronLeft,
                                                contentDescription = "Ocultar menú lateral",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.CloudOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "Sin base de datos",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            IconButton(
                                                onClick = onToggleCollapse,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ChevronLeft,
                                                    contentDescription = "Ocultar menú lateral",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = onNewDatabase,
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Nueva", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = onOpenDatabase,
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(30.dp)
                                            ) {
                                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Abrir", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        if (recentDatabases.isNotEmpty()) {
                                            Spacer(Modifier.height(10.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "RECIENTES",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            recentDatabases.take(5).forEach { rec ->
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { onOpenRecent(rec.path) },
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (rec.type == DatabaseType.SQLITE) Icons.Default.Storage else Icons.Default.FolderOpen,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = rec.name,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = rec.path,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontSize = 9.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
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
                    }

                    Spacer(Modifier.height(6.dp))

                    // ── Command Palette (Ctrl+K) ─────────────────────
                    if (!isCollapsed) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenCommandPalette() },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Buscar acciones...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Ctrl+K",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        IconButton(
                            onClick = onOpenCommandPalette,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar (Ctrl+K)",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    // ── Navigation Section ───────────────────────────
                    if (!isCollapsed) {
                        Text(
                            text = "MODOS DE TRABAJO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isCollapsed) 6.dp else 8.dp, vertical = 2.dp),
                        horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Screen.entries.forEach { screen ->
                            val isSelected = currentScreen == screen
                            val icon = when (screen) {
                                Screen.OVERVIEW -> Icons.Default.Dashboard
                                Screen.TABLE_DESIGNER -> Icons.Default.TableChart
                                Screen.DATA_VIEW -> Icons.Default.ViewList
                                Screen.SQL_QUERY -> Icons.Default.Terminal
                                Screen.DIAGRAM -> Icons.Default.AccountTree
                                Screen.JOIN_TABLES -> Icons.Default.CallMerge
                                Screen.ETL_QUERY -> Icons.Default.FilterAlt
                            }

                            if (isCollapsed) {
                                // Collapsed Icon-Only Button
                                Surface(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onScreenChanged(screen) },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = screen.displayName,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                // Expanded Row
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onScreenChanged(screen) },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = screen.displayName,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = screen.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )

                    // ── Database Quick Operations ────────────────────
                    if (databaseName != null) {
                        if (isCollapsed) {
                            // Collapsed: Quick Action Icons
                            IconButton(
                                onClick = onNewTable,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Nueva Tabla",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            IconButton(
                                onClick = onExportExcel,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Exportar a Excel",
                                    tint = if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF15803D),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            IconButton(
                                onClick = onImportExcel,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.FileUpload,
                                    contentDescription = "Importar de Excel",
                                    tint = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            IconButton(
                                onClick = onOpenBrainDoctor,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = "Asistente Cerebro",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            IconButton(
                                onClick = onBackupDatabase,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Backup,
                                    contentDescription = "Copia de Respaldo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Expanded Action Buttons
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Button(
                                    onClick = onNewTable,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 7.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Nueva Tabla", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = onExportExcel,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF15803D) else Color(0xFF16A34A).copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isDarkMode) Color(0xFF14532D).copy(alpha = 0.25f) else Color(0xFFDCFCE7).copy(alpha = 0.5f)
                                    ),
                                    contentPadding = PaddingValues(vertical = 7.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF15803D)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Exportar a Excel",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF15803D)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Surface(
                                        color = if (isDarkMode) Color(0xFF16A34A).copy(alpha = 0.4f) else Color(0xFF16A34A),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            ".xlsx",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onImportExcel,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF0284C7) else Color(0xFF0284C7).copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isDarkMode) Color(0xFF0369A1).copy(alpha = 0.25f) else Color(0xFFE0F2FE).copy(alpha = 0.5f)
                                    ),
                                    contentPadding = PaddingValues(vertical = 7.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FileUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Importar de Excel",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Surface(
                                        color = if (isDarkMode) Color(0xFF0284C7).copy(alpha = 0.4f) else Color(0xFF0284C7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "BD",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onOpenBrainDoctor,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    ),
                                    contentPadding = PaddingValues(vertical = 7.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Asistente Cerebro",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedButton(
                                    onClick = onBackupDatabase,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    contentPadding = PaddingValues(vertical = 7.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Backup,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Copia de Respaldo",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }

                if (!isCollapsed) {
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(sidebarScrollState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }

            // ── Bottom Section: Manual & Dark/Light Mode ──────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            if (isCollapsed) {
                // Collapsed bottom buttons
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onShowUserManual,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Manual de Usuario (F1)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Modo Oscuro",
                            tint = if (isDarkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Expanded bottom buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onShowUserManual() },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = "Manual de Usuario",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Manual de Usuario",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "F1",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleDarkMode() },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (isDarkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (isDarkMode) "Modo Claro" else "Modo Oscuro",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
