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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.ui.components.ConfirmDialog
import com.dbstudio.ui.components.FileChooser
import com.dbstudio.ui.components.GenerateDataDialog
import com.dbstudio.ui.components.RowEditDialog
import com.dbstudio.ui.theme.AppColors
import com.dbstudio.viewmodel.DataViewModel
import java.io.File

/**
 * Data View screen for CRUD operations on table rows with relational lookup resolution.
 */
@Composable
fun DataViewScreen(
    tableName: String?,
    engine: DatabaseEngine?,
    viewModel: DataViewModel,
    dataSyncRevision: Int = 0,
    modifier: Modifier = Modifier
) {
    // Load or silently refresh data when table or sync revision changes
    LaunchedEffect(tableName, dataSyncRevision) {
        if (tableName != null && engine != null) {
            val isSameTable = viewModel.tableInfo?.name == tableName
            // If already on the same table, update seamlessly in the background without UI flicker or spinner!
            viewModel.loadData(engine, tableName, isSilent = isSameTable)
        } else {
            viewModel.clear()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ViewList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (tableName != null) "Datos de la Tabla: $tableName" else "Selecciona una tabla",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (tableName != null) {
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "${viewModel.totalRows} registros",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = if (tableName != null) "Visualiza, inserta y edita registros con relaciones resueltas"
                        else "Selecciona una tabla en el menú lateral para ver sus datos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (tableName != null) {
                var exportMenuExpanded by remember { mutableStateOf(false) }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { viewModel.showInsertDialog = true }) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nuevo Registro")
                    }
                    FilledTonalButton(onClick = { viewModel.showGenerateDataDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Llenar Datos")
                    }

                    // Export CSV / JSON Dropdown
                    Box {
                        OutlinedButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Exportar")
                        }
                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exportar a CSV (.csv)") },
                                leadingIcon = {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    exportMenuExpanded = false
                                    if (engine != null) {
                                        val path = FileChooser.saveFile(
                                            title = "Exportar tabla '$tableName' a CSV",
                                            defaultName = "${tableName}.csv",
                                            extension = "csv",
                                            description = "Archivos CSV (*.csv)"
                                        )
                                        if (path != null) {
                                            try {
                                                val allRows = engine.queryAll(tableName, limit = 100000)
                                                val cols = viewModel.tableInfo?.columns?.map { it.name } ?: emptyList()
                                                val csvText = buildString {
                                                    appendLine(cols.joinToString(","))
                                                    allRows.forEach { row ->
                                                        appendLine(cols.joinToString(",") { col ->
                                                            val v = row[col]?.toString() ?: ""
                                                            if (v.contains(",") || v.contains("\"") || v.contains("\n"))
                                                                "\"${v.replace("\"", "\"\"")}\""
                                                            else v
                                                        })
                                                    }
                                                }
                                                File(path).writeText(csvText, Charsets.UTF_8)
                                                viewModel.showSuccess("Tabla exportada a CSV en '$path'")
                                            } catch (e: Exception) {
                                                viewModel.showError("Error al exportar CSV: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar a JSON (.json)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    exportMenuExpanded = false
                                    if (engine != null) {
                                        val path = FileChooser.saveFile(
                                            title = "Exportar tabla '$tableName' a JSON",
                                            defaultName = "${tableName}.json",
                                            extension = "json",
                                            description = "Archivos JSON (*.json)"
                                        )
                                        if (path != null) {
                                            try {
                                                val allRows = engine.queryAll(tableName, limit = 100000)
                                                val jsonText = buildString {
                                                    appendLine("[")
                                                    allRows.forEachIndexed { i, row ->
                                                        append("  {")
                                                        val entries = row.entries.map { (k, v) ->
                                                            val escapedVal = v?.toString()
                                                                ?.replace("\\", "\\\\")
                                                                ?.replace("\"", "\\\"")
                                                                ?.replace("\n", "\\n")
                                                            "\"$k\": ${if (v == null) "null" else if (v is Number) "$v" else "\"$escapedVal\""}"
                                                        }
                                                        append(entries.joinToString(", "))
                                                        if (i < allRows.size - 1) appendLine("},") else appendLine("}")
                                                    }
                                                    appendLine("]")
                                                }
                                                File(path).writeText(jsonText, Charsets.UTF_8)
                                                viewModel.showSuccess("Tabla exportada a JSON en '$path'")
                                            } catch (e: Exception) {
                                                viewModel.showError("Error al exportar JSON: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    OutlinedButton(onClick = {
                        if (engine != null) viewModel.loadData(engine, tableName)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refrescar")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Messages ────────────────────────────────────────────
        viewModel.errorMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.dismissMessages() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        viewModel.successMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.dismissMessages() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Empty State ─────────────────────────────────────────
        if (tableName == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ViewList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Selecciona una tabla en el menú lateral", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return
        }

        val info = viewModel.tableInfo ?: return
        val columns = info.columns

        // ── Data Table ──────────────────────────────────────────
        val horizontalScroll = rememberScrollState()
        val verticalScroll = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                // Column headers
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        // Actions column
                        Box(modifier = Modifier.width(88.dp).padding(horizontal = 4.dp)) {
                            Text(
                                "Acciones",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        columns.forEach { col ->
                            val hasFk = col.name in viewModel.relationshipLookups
                            Box(
                                modifier = Modifier
                                    .width(170.dp)
                                    .clickable { viewModel.toggleSort(col.name) }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (col.isPrimaryKey) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = AppColors.primaryKey, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                    } else if (hasFk) {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = AppColors.foreignKey, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        col.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (viewModel.sortColumn == col.name) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            if (viewModel.sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Data rows
                Column(modifier = Modifier.verticalScroll(verticalScroll)) {
                    if (viewModel.rows.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No hay datos en esta tabla.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { viewModel.showInsertDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Insertar Primer Registro")
                                }
                            }
                        }
                    }

                    viewModel.rows.forEachIndexed { index, row ->
                        Surface(
                            color = if (index % 2 == 0) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Action buttons
                                Row(modifier = Modifier.width(88.dp), horizontalArrangement = Arrangement.Center) {
                                    IconButton(
                                        onClick = { viewModel.startEdit(row) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.startDelete(row) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                columns.forEach { col ->
                                    Box(
                                        modifier = Modifier.width(170.dp).padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        val rawValue = row[col.name]
                                        val resolvedFk = viewModel.relationshipLookups[col.name]?.get(rawValue?.toString())

                                        if (resolvedFk != null) {
                                            // Render resolved relation name
                                            Surface(
                                                color = AppColors.foreignKey.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Link, contentDescription = null, tint = AppColors.foreignKey, modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = resolvedFk,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = rawValue?.toString() ?: "NULL",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (rawValue == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.onSurface,
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

        // ── Pagination ──────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { engine?.let { viewModel.previousPage(it, tableName) } },
                enabled = viewModel.currentPage > 0
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Página anterior")
            }
            Text(
                "Página ${viewModel.currentPage + 1} de ${viewModel.totalPages}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(
                onClick = { engine?.let { viewModel.nextPage(it, tableName) } },
                enabled = viewModel.currentPage < viewModel.totalPages - 1
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Página siguiente")
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────
    if (viewModel.showInsertDialog && viewModel.tableInfo != null) {
        RowEditDialog(
            title = "Nuevo Registro en '$tableName'",
            columns = viewModel.tableInfo!!.columns,
            foreignKeyOptions = viewModel.foreignKeyOptions,
            onDismiss = { viewModel.showInsertDialog = false },
            onSave = { values ->
                engine?.let { viewModel.insertRow(it, tableName!!, values) }
            }
        )
    }

    if (viewModel.showEditDialog && viewModel.editingRow != null && viewModel.tableInfo != null) {
        RowEditDialog(
            title = "Editar Registro en '$tableName'",
            columns = viewModel.tableInfo!!.columns,
            initialValues = viewModel.editingRow!!,
            foreignKeyOptions = viewModel.foreignKeyOptions,
            onDismiss = {
                viewModel.showEditDialog = false
            },
            onSave = { values ->
                engine?.let { eng ->
                    val pkValues = viewModel.getPrimaryKeyValues(viewModel.editingRow!!)
                    viewModel.updateRow(eng, tableName!!, pkValues, values)
                }
            }
        )
    }

    if (viewModel.showDeleteConfirm && viewModel.editingRow != null) {
        ConfirmDialog(
            title = "Eliminar Registro",
            message = "¿Estás seguro de que deseas eliminar este registro? Esta acción no se puede deshacer.",
            confirmText = "Eliminar",
            onDismiss = { viewModel.showDeleteConfirm = false },
            onConfirm = {
                engine?.let { eng ->
                    val pkValues = viewModel.getPrimaryKeyValues(viewModel.editingRow!!)
                    viewModel.deleteRow(eng, tableName!!, pkValues)
                }
            }
        )
    }

    if (viewModel.showGenerateDataDialog && tableName != null) {
        GenerateDataDialog(
            tableName = tableName,
            onDismiss = { viewModel.showGenerateDataDialog = false },
            onGenerate = { count ->
                engine?.let { eng ->
                    viewModel.generateData(eng, tableName, count)
                }
            }
        )
    }
}

