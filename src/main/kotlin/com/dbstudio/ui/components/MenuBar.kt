package com.dbstudio.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.dbstudio.viewmodel.Screen

/**
 * Top menu bar for the application window.
 */
@Composable
fun FrameWindowScope.AppMenuBar(
    hasDatabase: Boolean,
    onNewDatabase: () -> Unit,
    onOpenDatabase: () -> Unit,
    onSave: () -> Unit,
    onExportSql: () -> Unit,
    onExportAccdb: () -> Unit,
    onExportExcel: () -> Unit = {},
    onImportExcel: () -> Unit = {},
    onCloseDatabase: () -> Unit,
    onExit: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onShowUserManual: () -> Unit = {}
) {
    MenuBar {
        Menu("Archivo") {
            Item("Nueva Base de Datos...", onClick = onNewDatabase)
            Item("Abrir Base de Datos...", onClick = onOpenDatabase)
            Separator()
            Item("Guardar", onClick = onSave, enabled = hasDatabase)
            Separator()
            Item("Importar desde Excel (.xlsx, .xls)...", onClick = onImportExcel, enabled = hasDatabase)
            Item("Exportar como Excel (.xlsx)...", onClick = onExportExcel, enabled = hasDatabase)
            Item("Exportar como SQL...", onClick = onExportSql, enabled = hasDatabase)
            Item("Exportar como Access (.accdb)...", onClick = onExportAccdb, enabled = hasDatabase)
            Separator()
            Item("Cerrar Base de Datos", onClick = onCloseDatabase, enabled = hasDatabase)
            Separator()
            Item("Salir", onClick = onExit)
        }
        Menu("Ver") {
            Item("Panel General", onClick = { onNavigate(Screen.OVERVIEW) }, enabled = hasDatabase)
            Item("Diseñador de Tablas", onClick = { onNavigate(Screen.TABLE_DESIGNER) }, enabled = hasDatabase)
            Item("Vista de Datos", onClick = { onNavigate(Screen.DATA_VIEW) }, enabled = hasDatabase)
            Item("Consola SQL", onClick = { onNavigate(Screen.SQL_QUERY) }, enabled = hasDatabase)
            Item("Diagrama ER", onClick = { onNavigate(Screen.DIAGRAM) }, enabled = hasDatabase)
            Separator()
            Item("Juntar Tablas", onClick = { onNavigate(Screen.JOIN_TABLES) }, enabled = hasDatabase)
            Item("Consulta ETL", onClick = { onNavigate(Screen.ETL_QUERY) }, enabled = hasDatabase)
        }
        Menu("Ayuda") {
            Item("Manual de Usuario", onClick = onShowUserManual)
        }
    }
}
