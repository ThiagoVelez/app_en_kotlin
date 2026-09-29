@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.dbstudio.ui.theme.AppColors

/**
 * Collapsible Right Sidebar dedicated exclusively to database tables,
 * quick table search, table metrics, and table contextual management actions.
 */
@Composable
fun RightTableSidebar(
    tables: List<String>,
    selectedTable: String?,
    tableRowCounts: Map<String, Int> = emptyMap(),
    onTableSelected: (String) -> Unit,
    onViewTableData: (String) -> Unit,
    onDesignTable: (String) -> Unit,
    onRenameTable: (String) -> Unit,
    onDeleteTable: (String) -> Unit,
    onNewTable: () -> Unit,
    onNewRelationship: () -> Unit,
    onClose: () -> Unit,
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredTables = remember(tables, searchQuery) {
        if (searchQuery.isBlank()) tables
        else tables.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Surface(
        modifier = modifier.width(260.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "TABLAS (${tables.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Shortcut + Relación
                        IconButton(
                            onClick = onNewRelationship,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Nueva Relación",
                                tint = AppColors.foreignKey,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Close / Hide Right Sidebar
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Ocultar panel de tablas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ── Table Search ────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                SearchBarField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Buscar tabla...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Table List ──────────────────────────────────────
            val scrollState = rememberScrollState()
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (tables.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Sin tablas aún",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = onNewTable,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Crear Tabla")
                                }
                            }
                        }
                    } else if (filteredTables.isEmpty()) {
                        Text(
                            text = "No se encontraron tablas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    filteredTables.forEach { table ->
                        val isSelected = table == selectedTable
                        var menuExpanded by remember { mutableStateOf(false) }
                        val rowCount = tableRowCounts[table] ?: 0

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTableSelected(table) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = table,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$rowCount registros",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }

                                // Contextual menu trigger
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Opciones",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Ver datos") },
                                            onClick = {
                                                menuExpanded = false
                                                onViewTableData(table)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Diseñar estructura") },
                                            onClick = {
                                                menuExpanded = false
                                                onDesignTable(table)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Renombrar") },
                                            onClick = {
                                                menuExpanded = false
                                                onRenameTable(table)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.DriveFileRenameOutline,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Eliminar",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onDeleteTable(table)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scrollState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }

            // ── Bottom Action: Nueva Tabla ──────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Surface(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = onNewTable,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nueva Tabla")
                }
            }
        }
    }
}
