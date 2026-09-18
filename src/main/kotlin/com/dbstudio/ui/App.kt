package com.dbstudio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbstudio.ui.components.*
import com.dbstudio.ui.screens.*
import com.dbstudio.ui.theme.DBStudioTheme
import com.dbstudio.viewmodel.*

/**
 * Root composable for the DB Studio application.
 */
@Composable
fun App(
    appViewModel: AppViewModel,
    tableDesignerViewModel: TableDesignerViewModel,
    dataViewModel: DataViewModel,
    diagramViewModel: DiagramViewModel,
    sqlQueryViewModel: SqlQueryViewModel
) {
    DBStudioTheme(isDark = appViewModel.isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main content
                Row(modifier = Modifier.weight(1f)) {
                    // ── Sidebar ──────────────────────────────────
                    Sidebar(
                        databaseName = appViewModel.databaseInfo?.name,
                        databaseType = appViewModel.databaseInfo?.type,
                        tables = appViewModel.tables,
                        selectedTable = appViewModel.selectedTable,
                        currentScreen = appViewModel.currentScreen,
                        onTableSelected = { name ->
                            appViewModel.selectTable(name)
                        },
                        onScreenChanged = { screen ->
                            appViewModel.navigateTo(screen)
                        },
                        onNewTable = { appViewModel.showNewTableDialog = true },
                        onDeleteTable = { appViewModel.showConfirmDeleteTable = true },
                        onRenameTable = { appViewModel.showRenameTableDialog = true },
                        onNewRelationship = { appViewModel.showRelationshipDialog = true },
                        onRenameTableSpecific = { tableName ->
                            appViewModel.selectTable(tableName)
                            appViewModel.showRenameTableDialog = true
                        },
                        onDeleteTableSpecific = { tableName ->
                            appViewModel.selectTable(tableName)
                            appViewModel.showConfirmDeleteTable = true
                        },
                        onViewTableData = { tableName ->
                            appViewModel.selectTable(tableName)
                            appViewModel.navigateTo(Screen.DATA_VIEW)
                        },
                        onDesignTable = { tableName ->
                            appViewModel.selectTable(tableName)
                            appViewModel.navigateTo(Screen.TABLE_DESIGNER)
                        },
                        onOpenCommandPalette = { appViewModel.showCommandPalette = true },
                        onNewDatabase = { appViewModel.showNewDbDialog = true },
                        onOpenDatabase = {
                            val path = com.dbstudio.ui.components.FileChooser.openDatabase()
                            if (path != null) {
                                appViewModel.openDatabase(path)
                            }
                        },
                        isDarkMode = appViewModel.isDarkMode,
                        onToggleDarkMode = { appViewModel.toggleDarkMode() },
                        onShowUserManual = { appViewModel.showUserManual = true }
                    )

                    // ── Main Content Area ────────────────────────
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (appViewModel.currentScreen) {
                            Screen.OVERVIEW -> {
                                OverviewScreen(
                                    appViewModel = appViewModel,
                                    engine = appViewModel.getEngine(),
                                    onNavigateTo = { screen -> appViewModel.navigateTo(screen) },
                                    onSelectTable = { tbl -> appViewModel.selectTable(tbl) }
                                )
                            }
                            Screen.TABLE_DESIGNER -> {
                                TableDesignerScreen(
                                    tableName = appViewModel.selectedTable,
                                    engine = appViewModel.getEngine(),
                                    viewModel = tableDesignerViewModel,
                                    onNavigateToData = { tbl ->
                                        appViewModel.selectTable(tbl)
                                        appViewModel.navigateTo(Screen.DATA_VIEW)
                                    },
                                    onSchemaSaved = {
                                        appViewModel.notifyDataChanged()
                                    }
                                )
                            }
                            Screen.DATA_VIEW -> {
                                DataViewScreen(
                                    tableName = appViewModel.selectedTable,
                                    engine = appViewModel.getEngine(),
                                    viewModel = dataViewModel,
                                    dataSyncRevision = appViewModel.dataSyncRevision
                                )
                            }
                            Screen.SQL_QUERY -> {
                                SqlQueryScreen(
                                    engine = appViewModel.getEngine(),
                                    viewModel = sqlQueryViewModel,
                                    tables = appViewModel.tables
                                )
                            }
                            Screen.DIAGRAM -> {
                                DiagramScreen(
                                    engine = appViewModel.getEngine(),
                                    viewModel = diagramViewModel,
                                    onNewRelationship = {
                                        appViewModel.showRelationshipDialog = true
                                    }
                                )
                            }
                        }



                        // Loading overlay
                        if (appViewModel.isLoading) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }

                // ── Status Bar ───────────────────────────────────
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = appViewModel.statusMessage.ifEmpty { "Listo" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        appViewModel.databaseInfo?.let { info ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = info.filePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Dialogs ─────────────────────────────────────────────

        // New Database Dialog
        if (appViewModel.showNewDbDialog) {
            NewDatabaseDialog(
                onDismiss = { appViewModel.showNewDbDialog = false },
                onCreate = { name, type, dir ->
                    appViewModel.createDatabase(name, type, dir)
                    appViewModel.showNewDbDialog = false
                }
            )
        }

        // New Table Dialog
        if (appViewModel.showNewTableDialog) {
            TableNameDialog(
                title = "Nueva Tabla",
                existingNames = appViewModel.tables,
                onDismiss = { appViewModel.showNewTableDialog = false },
                onConfirm = { name ->
                    appViewModel.createTable(name)
                    appViewModel.showNewTableDialog = false
                }
            )
        }

        // Rename Table Dialog
        if (appViewModel.showRenameTableDialog && appViewModel.selectedTable != null) {
            TableNameDialog(
                title = "Renombrar Tabla",
                initialName = appViewModel.selectedTable!!,
                existingNames = appViewModel.tables,
                onDismiss = { appViewModel.showRenameTableDialog = false },
                onConfirm = { name ->
                    appViewModel.renameSelectedTable(name)
                    appViewModel.showRenameTableDialog = false
                }
            )
        }

        // Delete Table Confirmation
        if (appViewModel.showConfirmDeleteTable && appViewModel.selectedTable != null) {
            ConfirmDialog(
                title = "Eliminar Tabla",
                message = "¿Está seguro de que desea eliminar la tabla '${appViewModel.selectedTable}'? Esta acción no se puede deshacer.",
                onDismiss = { appViewModel.showConfirmDeleteTable = false },
                onConfirm = {
                    appViewModel.dropSelectedTable()
                    appViewModel.showConfirmDeleteTable = false
                }
            )
        }

        // Export Dialog
        if (appViewModel.showExportDialog && appViewModel.databaseInfo != null) {
            ExportDialog(
                databaseName = appViewModel.databaseInfo!!.name,
                onDismiss = { appViewModel.showExportDialog = false },
                onExportSql = { path ->
                    appViewModel.exportToSql(path)
                    appViewModel.showExportDialog = false
                },
                onExportAccdb = { path ->
                    appViewModel.exportToAccdb(path)
                    appViewModel.showExportDialog = false
                }
            )
        }

        // Relationship Dialog
        if (appViewModel.showRelationshipDialog && appViewModel.getEngine() != null) {
            RelationshipDialog(
                engine = appViewModel.getEngine()!!,
                onDismiss = { appViewModel.showRelationshipDialog = false },
                onCreated = {
                    appViewModel.refreshTables()
                    appViewModel.showRelationshipDialog = false
                    if (appViewModel.currentScreen == Screen.DIAGRAM) {
                        appViewModel.getEngine()?.let { diagramViewModel.loadDiagram(it) }
                    }
                },
                onUpdated = { _, _ ->
                    appViewModel.refreshTables()
                    if (appViewModel.currentScreen == Screen.DIAGRAM) {
                        appViewModel.getEngine()?.let { diagramViewModel.loadDiagram(it) }
                    }
                },
                onDeleted = {
                    appViewModel.refreshTables()
                    if (appViewModel.currentScreen == Screen.DIAGRAM) {
                        appViewModel.getEngine()?.let { diagramViewModel.loadDiagram(it) }
                    }
                }
            )
        }

        // User Manual Dialog
        if (appViewModel.showUserManual) {
            UserManualDialog(
                onDismiss = { appViewModel.showUserManual = false }
            )
        }

        // Command Palette Dialog (Ctrl + K)
        if (appViewModel.showCommandPalette) {
            CommandPaletteDialog(
                appViewModel = appViewModel,
                onDismiss = { appViewModel.showCommandPalette = false }
            )
        }

        // Error Snackbar
        appViewModel.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { appViewModel.dismissError() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { appViewModel.dismissError() }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }
}
