package com.dbstudio

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.res.painterResource
import com.dbstudio.model.DatabaseInfo
import com.dbstudio.ui.App
import com.dbstudio.ui.components.AppMenuBar
import com.dbstudio.ui.components.FileChooser
import com.dbstudio.viewmodel.*

fun main() = application {
    val appViewModel = remember { AppViewModel() }
    val tableDesignerViewModel = remember { TableDesignerViewModel() }
    val dataViewModel = remember { DataViewModel() }
    val diagramViewModel = remember { DiagramViewModel() }
    val sqlQueryViewModel = remember { SqlQueryViewModel() }

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
        size = DpSize(1280.dp, 800.dp),
        position = WindowPosition(Alignment.Center)
    )

    val dbInfo: DatabaseInfo? = appViewModel.databaseInfo
    val windowTitle = buildString {
        append("DataBase Studio")
        if (dbInfo != null) {
            append(" — ${dbInfo.name} (${dbInfo.type.displayName})")
        }
    }

    Window(
        onCloseRequest = {
            appViewModel.cleanup()
            sqlQueryViewModel.cleanup()
            exitApplication()
        },
        state = windowState,
        title = windowTitle,
        icon = painterResource("icon.png"),
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                if (keyEvent.isCtrlPressed && keyEvent.key == Key.K) {
                    appViewModel.showCommandPalette = true
                    true
                } else if (keyEvent.key == Key.F1) {
                    appViewModel.showUserManual = true
                    true
                } else false
            } else false
        }
    ) {
        // Menu bar
        AppMenuBar(
            hasDatabase = appViewModel.databaseInfo != null,
            onNewDatabase = { appViewModel.showNewDbDialog = true },
            onOpenDatabase = {
                val path: String? = FileChooser.openDatabase()
                if (path != null) {
                    appViewModel.openDatabase(path)
                }
            },
            onSave = { appViewModel.saveDatabase() },
            onExportSql = {
                val info: DatabaseInfo? = appViewModel.databaseInfo
                if (info != null) {
                    val path: String? = FileChooser.saveFile(
                        title = "Export as SQL",
                        defaultName = "${info.name}.sql",
                        extension = "sql",
                        description = "SQL Files (*.sql)"
                    )
                    if (path != null) {
                        appViewModel.exportToSql(path)
                    }
                }
            },
            onExportAccdb = {
                val info: DatabaseInfo? = appViewModel.databaseInfo
                if (info != null) {
                    val path: String? = FileChooser.saveFile(
                        title = "Export as Access",
                        defaultName = "${info.name}.accdb",
                        extension = "accdb",
                        description = "Access Database (*.accdb)"
                    )
                    if (path != null) {
                        appViewModel.exportToAccdb(path)
                    }
                }
            },
            onExportExcel = {
                val info: DatabaseInfo? = appViewModel.databaseInfo
                if (info != null) {
                    val path: String? = FileChooser.saveFile(
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
            onCloseDatabase = { appViewModel.closeDatabase() },
            onExit = {
                appViewModel.cleanup()
                exitApplication()
            },
            onNavigate = { screen: Screen -> appViewModel.navigateTo(screen) },
            onShowUserManual = { appViewModel.showUserManual = true }
        )

        // App content
        App(
            appViewModel = appViewModel,
            tableDesignerViewModel = tableDesignerViewModel,
            dataViewModel = dataViewModel,
            diagramViewModel = diagramViewModel,
            sqlQueryViewModel = sqlQueryViewModel
        )
    }
}
