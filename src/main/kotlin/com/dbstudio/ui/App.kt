package com.dbstudio.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                // ── Office Ribbon Top Bar (Excel / Access / Word Style) ──
                OfficeRibbonBar(
                    databaseName = appViewModel.databaseInfo?.name,
                    databaseType = appViewModel.databaseInfo?.type,
                    tables = appViewModel.tables,
                    selectedTable = appViewModel.selectedTable,
                    tableRowCounts = appViewModel.tableRowCounts,
                    currentScreen = appViewModel.currentScreen,
                    onScreenChanged = { screen -> appViewModel.navigateTo(screen) },
                    onTableSelected = { name -> appViewModel.selectTable(name) },
                    onNewDatabase = { appViewModel.showNewDbDialog = true },
                    onOpenDatabase = {
                        val path = com.dbstudio.ui.components.FileChooser.openDatabase()
                        if (path != null) {
                            appViewModel.openDatabase(path)
                        }
                    },
                    onNewTable = { appViewModel.showNewTableDialog = true },
                    onDeleteTable = { tableName ->
                        appViewModel.selectTable(tableName)
                        appViewModel.showConfirmDeleteTable = true
                    },
                    onRenameTable = { tableName ->
                        appViewModel.selectTable(tableName)
                        appViewModel.showRenameTableDialog = true
                    },
                    onNewRelationship = { appViewModel.showRelationshipDialog = true },
                    onOpenIntermediateDialog = { appViewModel.showIntermediateTableDialog = true },
                    onAddCustomField = {
                        if (appViewModel.selectedTable == null && appViewModel.tables.isNotEmpty()) {
                            appViewModel.selectTable(appViewModel.tables.first())
                        }
                        if (appViewModel.currentScreen != Screen.TABLE_DESIGNER) {
                            appViewModel.navigateTo(Screen.TABLE_DESIGNER)
                        }
                        tableDesignerViewModel.showAddColumnDialog = true
                    },
                    onInsertRecord = {
                        if (appViewModel.currentScreen != Screen.DATA_VIEW) {
                            appViewModel.navigateTo(Screen.DATA_VIEW)
                        }
                        dataViewModel.showInsertDialog = true
                    },
                    onGenerateMockData = {
                        if (appViewModel.currentScreen != Screen.DATA_VIEW) {
                            appViewModel.navigateTo(Screen.DATA_VIEW)
                        }
                        dataViewModel.showGenerateDataDialog = true
                    },
                    onOpenBrainDoctor = { appViewModel.showBrainDoctorDialog = true },
                    onBackupDatabase = { appViewModel.createBackupCopy() },
                    onExportExcel = {
                        val info = appViewModel.databaseInfo
                        if (info != null) {
                            val path = com.dbstudio.ui.components.FileChooser.saveFile(
                                title = "Exportar Base de Datos a Excel",
                                defaultName = "${info.name}.xlsx",
                                extension = "xlsx",
                                description = "Libro de Excel (*.xlsx)"
                            )
                            if (path != null) {
                                appViewModel.exportToExcel(path)
                            }
                        }
                    },
                    onImportExcel = {
                        val path = com.dbstudio.ui.components.FileChooser.openExcelFile()
                        if (path != null) {
                            appViewModel.importFromExcel(path)
                        }
                    },
                    onExportSql = {
                        appViewModel.showExportDialog = true
                    },
                    recentDatabases = appViewModel.recentDatabases,
                    onOpenRecent = { path -> appViewModel.openDatabase(path) },
                    onAutoOrganizeDiagram = {
                        if (appViewModel.currentScreen != Screen.DIAGRAM) {
                            appViewModel.navigateTo(Screen.DIAGRAM)
                        }
                        diagramViewModel.autoLayout()
                    },
                    onOpenCommandPalette = { appViewModel.showCommandPalette = true },
                    onShowUserManual = { appViewModel.showUserManual = true },
                    isDarkMode = appViewModel.isDarkMode,
                    onToggleDarkMode = { appViewModel.toggleDarkMode() }
                )

                // ── Main Content Area (100% full screen width) ───────────
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                        Screen.JOIN_TABLES -> {
                            JoinTablesScreen(
                                engine = appViewModel.getEngine(),
                                tables = appViewModel.tables
                            )
                        }
                        Screen.ETL_QUERY -> {
                            EtlQueryScreen(
                                engine = appViewModel.getEngine(),
                                tables = appViewModel.tables,
                                onTableCreated = {
                                    appViewModel.refreshTables()
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
                },
                onExportExcel = { path ->
                    appViewModel.exportToExcel(path)
                    appViewModel.showExportDialog = false
                }
            )
        }

        // Relationship Dialog
        if (appViewModel.showRelationshipDialog && appViewModel.getEngine() != null) {
            RelationshipDialog(
                engine = appViewModel.getEngine()!!,
                availableTables = appViewModel.tables,
                onDismiss = {
                    appViewModel.showRelationshipDialog = false
                    appViewModel.notifyDataChanged()
                },
                onCreated = {
                    appViewModel.refreshTables()
                    appViewModel.notifyDataChanged()
                    appViewModel.showRelationshipDialog = false
                    if (appViewModel.currentScreen == Screen.DIAGRAM) {
                        appViewModel.getEngine()?.let { diagramViewModel.loadDiagram(it) }
                    }
                },
                onUpdated = { _, _ ->
                    appViewModel.refreshTables()
                    appViewModel.notifyDataChanged()
                    if (appViewModel.currentScreen == Screen.DIAGRAM) {
                        appViewModel.getEngine()?.let { diagramViewModel.loadDiagram(it) }
                    }
                },
                onDeleted = {
                    appViewModel.refreshTables()
                    appViewModel.notifyDataChanged()
                    if (appViewModel.currentScreen == Screen.DIAGRAM) {
                        appViewModel.getEngine()?.let { diagramViewModel.loadDiagram(it) }
                    }
                }
            )
        }

        // Brain Doctor (Asistente Inteligente) Dialog
        if (appViewModel.showBrainDoctorDialog && appViewModel.getEngine() != null) {
            com.dbstudio.cerebro.ui.BrainDoctorDialog(
                engine = appViewModel.getEngine()!!,
                onDismiss = { appViewModel.showBrainDoctorDialog = false },
                onSchemaChanged = {
                    appViewModel.refreshTables()
                    appViewModel.notifyDataChanged()
                    if (appViewModel.currentScreen == Screen.DIAGRAM) {
                        appViewModel.getEngine()?.let { diagramViewModel.loadDiagram(it) }
                    }
                }
            )
        }

        // Intermediate Table (Many-to-Many) Dialog
        if (appViewModel.showIntermediateTableDialog && appViewModel.getEngine() != null) {
            com.dbstudio.cerebro.ui.IntermediateTableDialog(
                engine = appViewModel.getEngine()!!,
                availableTables = appViewModel.tables,
                onDismiss = { appViewModel.showIntermediateTableDialog = false },
                onSuccess = { newTable ->
                    appViewModel.showIntermediateTableDialog = false
                    appViewModel.refreshTables()
                    appViewModel.notifyDataChanged()
                    appViewModel.selectTable(newTable)
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

        // Intelligent Error Dialog with Cerebro diagnosis
        appViewModel.errorMessage?.let { error ->
            val diag = remember(error) {
                com.dbstudio.cerebro.Cerebro.diagnoseError(RuntimeException(error))
            }
            AlertDialog(
                onDismissRequest = { appViewModel.dismissError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(diag.title, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(diag.friendlyExplanation, style = MaterialTheme.typography.bodyMedium)
                        if (diag.possibleCauses.isNotEmpty()) {
                            Text("Posibles causas:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                            diag.possibleCauses.forEach { cause ->
                                Text("• $cause", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (diag.suggestedActions.isNotEmpty()) {
                            Text("💡 Sugerencias del Asistente:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                            diag.suggestedActions.forEach { action ->
                                Text("• ${action.title}: ${action.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { appViewModel.dismissError() }) {
                        Text("Entendido")
                    }
                }
            )
        }
    }
}
