package com.dbstudio.ui.screens

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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.ui.components.FileChooser
import com.dbstudio.viewmodel.SqlQueryViewModel
import java.io.File

/**
 * BigQuery / Google Cloud SQL Studio inspired SQL Query Editor and Runner.
 */
@Composable
fun SqlQueryScreen(
    engine: DatabaseEngine?,
    viewModel: SqlQueryViewModel,
    tables: List<String>,
    modifier: Modifier = Modifier
) {
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Consola y Editor SQL",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Escribe y ejecuta consultas SQL directamente con métricas de tiempo de respuesta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (viewModel.queryResult != null && viewModel.queryResult!!.rows.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            val path = FileChooser.saveFile(
                                title = "Exportar resultados como CSV",
                                defaultName = "resultado_consulta.csv",
                                extension = "csv",
                                description = "CSV (*.csv)"
                            )
                            if (path != null) {
                                val ok = viewModel.exportResultToCsv(File(path))
                                if (ok) exportSuccessMessage = "Resultados exportados a '$path'"
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar CSV")
                    }
                }

                Button(
                    onClick = { viewModel.execute(engine) },
                    enabled = !viewModel.isExecuting && viewModel.queryText.isNotBlank() && engine != null,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (viewModel.isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Ejecutar (Ctrl + Enter)")
                }
            }
        }

        // ── Quick Query Templates & History ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { showHistory = !showHistory },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (showHistory) "Ocultar Historial" else "Historial (${viewModel.queryHistory.size})",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

            Text(
                "Fragmentos:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (tables.isNotEmpty()) {
                val firstTable = tables.first()
                FilterChip(
                    selected = false,
                    onClick = { viewModel.setQuery("SELECT * FROM $firstTable LIMIT 50;") },
                    label = { Text("SELECT * ($firstTable)") }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.setQuery("SELECT COUNT(*) AS total FROM $firstTable;") },
                    label = { Text("COUNT(*) $firstTable") }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.setQuery("SELECT * FROM $firstTable ORDER BY id DESC LIMIT 20;") },
                    label = { Text("ORDER BY DESC") }
                )
                if (tables.size >= 2) {
                    val secondTable = tables[1]
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.setQuery("SELECT * FROM $firstTable INNER JOIN $secondTable ON $firstTable.id = $secondTable.${firstTable}_id LIMIT 20;") },
                        label = { Text("JOIN $firstTable + $secondTable") }
                    )
                }
            } else {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.setQuery("SELECT 1 AS test;") },
                    label = { Text("SELECT 1") }
                )
            }
            FilterChip(
                selected = false,
                onClick = { viewModel.setQuery("SELECT name, type FROM sqlite_master WHERE type='table';") },
                label = { Text("Ver Tablas BD") }
            )
        }

        // ── Query History Drawer / Card ──────────────────────────────
        if (showHistory) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Historial de Consultas Recientes (${viewModel.queryHistory.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "Haz clic en cualquier consulta para recargarla",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (viewModel.queryHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No hay consultas ejecutadas todavía en esta sesión.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val histScroll = rememberScrollState()
                        Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(histScroll),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            viewModel.queryHistory.forEach { sql ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.setQuery(sql)
                                            showHistory = false
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = sql.replace("\n", " ").trim(),
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                viewModel.setQuery(sql)
                                                showHistory = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Cargar", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── SQL Code Editor Area ────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            OutlinedTextField(
                value = viewModel.queryText,
                onValueChange = { viewModel.queryText = it },
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.isCtrlPressed && keyEvent.key == Key.Enter) {
                            viewModel.execute(engine)
                            true
                        } else false
                    },
                placeholder = {
                    Text(
                        "Escribe tu consulta SQL aquí (ej: SELECT * FROM estudiantes WHERE edad > 18)...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // ── Status Banner (Execution metrics or Error) ──────────────
        viewModel.errorMessage?.let { err ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        exportSuccessMessage?.let { msg ->
            Surface(
                color = Color(0xFFD1FAE5),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                    Spacer(Modifier.width(10.dp))
                    Text(msg, color = Color(0xFF065F46), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        viewModel.queryResult?.let { res ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF22C55E), RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            res.message ?: "${res.rows.size} filas devueltas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Tiempo: ${res.executionTimeMs} ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Query Results Grid ──────────────────────────────────────
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            val result = viewModel.queryResult
            if (result == null || (result.columns.isEmpty() && result.rows.isEmpty())) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Escribe una consulta SQL y presiona Ejecutar para ver los resultados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val horizontalScroll = rememberScrollState()
                val verticalScroll = rememberScrollState()

                Box(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScroll)
                            .verticalScroll(verticalScroll)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            result.columns.forEach { colName ->
                                Text(
                                    text = colName,
                                    modifier = Modifier.width(160.dp).padding(horizontal = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Data Rows
                        result.rows.forEachIndexed { rowIndex, row ->
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row.forEach { cellValue ->
                                    Text(
                                        text = cellValue?.toString() ?: "NULL",
                                        modifier = Modifier.width(160.dp).padding(horizontal = 6.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = if (cellValue == null) FontFamily.Monospace else FontFamily.Default,
                                        color = if (cellValue == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}
