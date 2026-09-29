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
