package com.dbstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dbstudio.viewmodel.AppViewModel
import com.dbstudio.viewmodel.Screen

data class PaletteAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val category: String,
    val onExecute: () -> Unit
)

/**
 * Universal Command Palette (Ctrl + K) inspired by Google Cloud / Chrome Omnibox.
 */
@Composable
fun CommandPaletteDialog(
    appViewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf(0) }

    val allActions = remember(appViewModel.tables, appViewModel.databaseInfo) {
        val actions = mutableListOf<PaletteAction>()

        // Navigation
        actions.add(
            PaletteAction(
                title = "Ir a Panel General",
                subtitle = "Ver métricas, KPIs y salud de la base de datos",
                icon = Icons.Default.Dashboard,
                category = "Navegación",
                onExecute = { appViewModel.navigateTo(Screen.OVERVIEW) }
            )
        )
        actions.add(
            PaletteAction(
                title = "Ir a Diseñador de Tablas",
                subtitle = "Crear o editar columnas y estructuras",
                icon = Icons.Default.TableChart,
                category = "Navegación",
                onExecute = { appViewModel.navigateTo(Screen.TABLE_DESIGNER) }
            )
        )
        actions.add(
            PaletteAction(
                title = "Ir a Vista de Datos",
                subtitle = "Ver y editar registros en cuadrícula",
                icon = Icons.Default.ViewList,
                category = "Navegación",
                onExecute = { appViewModel.navigateTo(Screen.DATA_VIEW) }
            )
        )
        actions.add(
            PaletteAction(
                title = "Ir a Consola SQL",
                subtitle = "Ejecutar consultas libres y scripts",
                icon = Icons.Default.Terminal,
                category = "Navegación",
                onExecute = { appViewModel.navigateTo(Screen.SQL_QUERY) }
            )
        )
        actions.add(
            PaletteAction(
                title = "Ir a Diagrama ER",
                subtitle = "Visualizar y mover entidades y relaciones",
                icon = Icons.Default.AccountTree,
                category = "Navegación",
                onExecute = { appViewModel.navigateTo(Screen.DIAGRAM) }
            )
        )

        // Actions
        actions.add(
            PaletteAction(
                title = "Nueva Tabla",
                subtitle = "Crear una nueva tabla en la base de datos",
                icon = Icons.Default.Add,
                category = "Acciones",
                onExecute = { appViewModel.showNewTableDialog = true }
            )
        )
        actions.add(
            PaletteAction(
                title = "Nueva Relación",
                subtitle = "Crear clave foránea entre tablas",
                icon = Icons.Default.Link,
                category = "Acciones",
                onExecute = { appViewModel.showRelationshipDialog = true }
            )
        )
        actions.add(
            PaletteAction(
                title = "Alternar Modo Oscuro / Claro",
                subtitle = if (appViewModel.isDarkMode) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                icon = if (appViewModel.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                category = "Preferencias",
                onExecute = { appViewModel.toggleDarkMode() }
            )
        )
        actions.add(
            PaletteAction(
                title = "Exportar Base de Datos",
                subtitle = "Guardar como script SQL o archivo Access",
                icon = Icons.Default.FileDownload,
                category = "Acciones",
                onExecute = { appViewModel.showExportDialog = true }
            )
        )
        actions.add(
            PaletteAction(
                title = "Abrir Manual de Usuario",
                subtitle = "Consultar la guía completa de DataBase Studio (F1)",
                icon = Icons.Default.MenuBook,
                category = "Ayuda",
                onExecute = { appViewModel.showUserManual = true }
            )
        )

        // Table actions
        for (tbl in appViewModel.tables) {
            actions.add(
                PaletteAction(
                    title = "Ver datos de '$tbl'",
                    subtitle = "Abrir la vista de datos de esta tabla",
                    icon = Icons.Default.Visibility,
                    category = "Tablas",
                    onExecute = {
                        appViewModel.selectTable(tbl)
                        appViewModel.navigateTo(Screen.DATA_VIEW)
                    }
                )
            )
            actions.add(
                PaletteAction(
                    title = "Diseñar estructura de '$tbl'",
                    subtitle = "Abrir editor de columnas de esta tabla",
                    icon = Icons.Default.Edit,
                    category = "Tablas",
                    onExecute = {
                        appViewModel.selectTable(tbl)
                        appViewModel.navigateTo(Screen.TABLE_DESIGNER)
                    }
                )
            )
        }

        actions
    }

    val filteredActions = remember(query, allActions) {
        if (query.isBlank()) allActions
        else {
            val q = query.trim().lowercase()
            allActions.filter {
                it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }
    }

    LaunchedEffect(filteredActions) {
        selectedIndex = 0
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(620.dp)
                .heightIn(max = 500.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Search Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Escribe un comando o busca una tabla... (Ctrl+K)") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ESC",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionDown -> {
                                        if (filteredActions.isNotEmpty()) {
                                            selectedIndex = (selectedIndex + 1).coerceAtMost(filteredActions.lastIndex)
                                        }
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        if (filteredActions.isNotEmpty()) {
                                            selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                        }
                                        true
                                    }
                                    Key.Enter -> {
                                        if (filteredActions.isNotEmpty() && selectedIndex in filteredActions.indices) {
                                            filteredActions[selectedIndex].onExecute()
                                            onDismiss()
                                        }
                                        true
                                    }
                                    Key.Escape -> {
                                        onDismiss()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                )

                Spacer(Modifier.height(12.dp))

                if (filteredActions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se encontraron comandos para '$query'",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(filteredActions) { index, action ->
                            val isSelected = index == selectedIndex
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        action.onExecute()
                                        onDismiss()
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        action.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            action.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            action.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            action.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
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
