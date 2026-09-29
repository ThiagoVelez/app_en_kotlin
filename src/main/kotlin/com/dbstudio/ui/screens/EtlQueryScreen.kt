@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.Column
import com.dbstudio.model.ColumnPresets
import com.dbstudio.model.ColumnType
import com.dbstudio.model.TableInfo
import com.dbstudio.ui.components.FileChooser
import com.dbstudio.ui.components.SearchBarField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

data class EtlRecord(
    val id: String,
    val sourceTable: String,
    val semillero: String,
    val descripcion: String,
    val observaciones: String,
    val rawData: Map<String, Any?>
)

@Composable
fun EtlQueryScreen(
    engine: DatabaseEngine?,
    tables: List<String>,
    onTableCreated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTables by remember { mutableStateOf<Set<String>>(emptySet()) }
    var semilleroFilter by remember { mutableStateOf("") }
    var observationFilter by remember { mutableStateOf("") }
    var observationStatusFilter by remember { mutableStateOf("all") } // "all", "with_obs", "without_obs"
    var generalSearch by remember { mutableStateOf("") }
    var isFilterAccordionExpanded by remember { mutableStateOf(true) }

    // Observation Editing Dialog State
    var editingRecord by remember { mutableStateOf<EtlRecord?>(null) }
    var editObservationText by remember { mutableStateOf("") }
    var showObservationDialog by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Tarjetas Semilleros, 1: Vista Tabular

    var extractedRecords by remember { mutableStateOf<List<EtlRecord>>(emptyList()) }
    var allColumnsFound by remember { mutableStateOf<List<String>>(emptyList()) }

    // Auto-select relevant tables on initial load
    LaunchedEffect(tables) {
        if (tables.isNotEmpty() && selectedTables.isEmpty()) {
            val relevant = tables.filter { tbl ->
                val lower = tbl.lowercase()
                lower.contains("semiller") || lower.contains("estudiante") || 
                lower.contains("investiga") || lower.contains("observac") || lower.contains("proyect")
            }.toSet()
            selectedTables = if (relevant.isNotEmpty()) relevant else setOf(tables.first())
        }
    }

    // Function to run the ETL process
    fun runEtl() {
        val eng = engine ?: return
        if (selectedTables.isEmpty()) {
            extractedRecords = emptyList()
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        try {
            val records = mutableListOf<EtlRecord>()
            val discoveredCols = mutableSetOf<String>()

            for (tableName in selectedTables) {
                val tableInfo = try { eng.getTableInfo(tableName) } catch (_: Exception) { null }
                val rows = try { eng.queryAll(tableName, limit = 5000) } catch (_: Exception) { emptyList() }

                val colNames = tableInfo?.columns?.map { it.name } ?: (rows.firstOrNull()?.keys?.toList() ?: emptyList())
                discoveredCols.addAll(colNames)

                // Detect semillero column
                val semilleroCol = colNames.find {
                    val l = it.lowercase()
                    l.contains("semillero") || l.contains("grupo") || l.contains("programa")
                }

                // Detect description column
                val descCol = colNames.find {
                    val l = it.lowercase()
                    l.contains("descripcion") || l.contains("desc") || l.contains("detalle") || l.contains("resumen")
                }

                // Detect observations column
                val obsCol = colNames.find {
                    val l = it.lowercase()
                    l.contains("observac") || l.contains("comentario") || l.contains("nota") || l.contains("bitacora") || l.contains("retroalimenta")
                }

                for ((idx, row) in rows.withIndex()) {
                    val semVal = if (semilleroCol != null) {
                        row[semilleroCol]?.toString() ?: "Sin Semillero Asignado"
                    } else {
                        row.entries.find { it.key.lowercase().contains("semillero") }?.value?.toString() ?: "General ($tableName)"
                    }

                    val descVal = if (descCol != null) {
                        row[descCol]?.toString() ?: ""
                    } else {
                        row.entries.find { it.key.lowercase().contains("desc") }?.value?.toString() ?: ""
                    }

                    val obsVal = if (obsCol != null) {
                        row[obsCol]?.toString() ?: ""
                    } else {
                        row.entries.find { it.key.lowercase().contains("obs") || it.key.lowercase().contains("nota") }?.value?.toString() ?: ""
                    }

                    records.add(
                        EtlRecord(
                            id = row["id"]?.toString() ?: "${tableName}_$idx",
                            sourceTable = tableName,
                            semillero = semVal,
                            descripcion = descVal,
                            observaciones = obsVal,
                            rawData = row
                        )
                    )
                }
            }

            allColumnsFound = discoveredCols.toList()
            extractedRecords = records
            successMessage = "Proceso ETL completado: ${records.size} registros extraídos de ${selectedTables.size} tabla(s)"
        } catch (e: Exception) {
            errorMessage = "Error en proceso ETL: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Auto-run when tables change
    LaunchedEffect(selectedTables) {
        if (selectedTables.isNotEmpty() && engine != null) {
            runEtl()
        }
    }

    // Filter records according to user criteria
    val filteredRecords = remember(extractedRecords, semilleroFilter, observationFilter, observationStatusFilter, generalSearch) {
        extractedRecords.filter { rec ->
            val matchSemillero = semilleroFilter.isBlank() || 
                rec.semillero.equals(semilleroFilter.trim(), ignoreCase = true) || 
                rec.semillero.contains(semilleroFilter.trim(), ignoreCase = true)

            val matchObsText = observationFilter.isBlank() || 
                rec.observaciones.contains(observationFilter.trim(), ignoreCase = true) || 
                rec.descripcion.contains(observationFilter.trim(), ignoreCase = true)

            val matchObsStatus = when (observationStatusFilter) {
                "with_obs" -> rec.observaciones.isNotBlank() && !rec.observaciones.contains("Sin observaciones", ignoreCase = true)
                "without_obs" -> rec.observaciones.isBlank() || rec.observaciones.contains("Sin observaciones", ignoreCase = true)
                else -> true
            }

            val matchGeneral = generalSearch.isBlank() ||
                rec.semillero.contains(generalSearch.trim(), ignoreCase = true) ||
                rec.descripcion.contains(generalSearch.trim(), ignoreCase = true) ||
                rec.observaciones.contains(generalSearch.trim(), ignoreCase = true) ||
                rec.rawData.values.any { it?.toString()?.contains(generalSearch.trim(), ignoreCase = true) == true }

            matchSemillero && matchObsText && matchObsStatus && matchGeneral
        }
    }

    // Distinct semilleros and counts for quick accordion filtering
    val semilleroCounts = remember(extractedRecords) {
        extractedRecords.groupBy { it.semillero }.mapValues { it.value.size }
    }
    val detectedSemilleros = remember(semilleroCounts) {
        semilleroCounts.keys.filter { it.isNotBlank() }.sorted()
    }

    Column(modifier = modifier.fillMaxSize().padding(14.dp)) {
        // ── Header Card ─────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Proceso de Consulta ETL — Semilleros & Observaciones",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Extracción, Transformación por acordeón interactivo y edición directa de observaciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { runEtl() },
                            enabled = !isLoading && selectedTables.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ejecutar ETL")
                        }

                        // Export ETL to Excel
                        OutlinedButton(
                            onClick = {
                                if (filteredRecords.isEmpty()) return@OutlinedButton
                                val savePath = FileChooser.saveFile(
                                    title = "Exportar Reporte ETL a Excel",
                                    defaultName = "Reporte_ETL_Semilleros_${System.currentTimeMillis() % 10000}.xlsx",
                                    extension = "xlsx",
                                    description = "Libro de Excel (*.xlsx)"
                                )
                                if (savePath != null) {
                                    try {
                                        val file = File(savePath)
                                        XSSFWorkbook().use { wb ->
                                            val sheet = wb.createSheet("ETL_Semilleros")
                                            val headerRow = sheet.createRow(0)
                                            val headers = listOf("ID", "Tabla_Origen", "Semillero", "Descripcion", "Observaciones") + allColumnsFound
                                            headers.forEachIndexed { i, h ->
                                                headerRow.createCell(i).setCellValue(h)
                                            }

                                            filteredRecords.forEachIndexed { rIdx, rec ->
                                                val row = sheet.createRow(rIdx + 1)
                                                row.createCell(0).setCellValue(rec.id)
                                                row.createCell(1).setCellValue(rec.sourceTable)
                                                row.createCell(2).setCellValue(rec.semillero)
                                                row.createCell(3).setCellValue(rec.descripcion)
                                                row.createCell(4).setCellValue(rec.observaciones)

                                                allColumnsFound.forEachIndexed { cIdx, colName ->
                                                    val cellVal = rec.rawData[colName]?.toString() ?: ""
                                                    row.createCell(5 + cIdx).setCellValue(cellVal)
                                                }
                                            }

                                            FileOutputStream(file).use { fos -> wb.write(fos) }
                                        }
                                        successMessage = "Reporte ETL exportado exitosamente a '$savePath'"
                                    } catch (e: Exception) {
                                        errorMessage = "Error al exportar: ${e.message}"
                                    }
                                }
                            },
                            enabled = filteredRecords.isNotEmpty()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exportar a Excel")
                        }

                        // Save ETL to DB as a summarized table
                        OutlinedButton(
                            onClick = {
                                if (filteredRecords.isEmpty() || engine == null) return@OutlinedButton
                                val newTblName = "etl_semilleros_resumen_${System.currentTimeMillis() % 1000}"
                                try {
                                    val summaryTable = TableInfo(
                                        name = newTblName,
                                        columns = listOf(
                                            ColumnPresets.id(),
                                            Column("tabla_origen", ColumnType.TEXT, isNullable = false),
                                            Column("semillero", ColumnType.TEXT, isNullable = false),
                                            Column("descripcion", ColumnType.TEXT, isNullable = true),
                                            Column("observaciones", ColumnType.TEXT, isNullable = true),
                                            ColumnPresets.fechaCreacion()
                                        )
                                    )
                                    engine.createTable(summaryTable)

                                    for (rec in filteredRecords) {
                                        engine.insertRow(
                                            newTblName,
                                            mapOf(
                                                "tabla_origen" to rec.sourceTable,
                                                "semillero" to rec.semillero,
                                                "descripcion" to rec.descripcion,
                                                "observaciones" to rec.observaciones
                                            )
                                        )
                                    }
                                    successMessage = "Tabla '$newTblName' creada exitosamente en la base de datos con los datos del ETL"
                                    onTableCreated()
                                } catch (e: Exception) {
                                    errorMessage = "Error al cargar tabla en la BD: ${e.message}"
                                }
                            },
                            enabled = filteredRecords.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar en BD")
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Acordeón Interactivo de Filtros ETL (Transformación) ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Barra de Cabecera del Acordeón (Clicable para expandir/colapsar)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                .clickable { isFilterAccordionExpanded = !isFilterAccordionExpanded }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Filtros de Consulta ETL (Acordeón Rápido)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (semilleroFilter.isNotBlank() || observationFilter.isNotBlank() || observationStatusFilter != "all") {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    "Filtros activos",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Resumen rápido en texto
                                    val summary = buildString {
                                        append("Semillero: ${semilleroFilter.ifBlank { "Todos" }}")
                                        append(" • ")
                                        append(when (observationStatusFilter) {
                                            "with_obs" -> "Con observaciones"
                                            "without_obs" -> "Sin observaciones"
                                            else -> "Todas las observaciones"
                                        })
                                        if (observationFilter.isNotBlank()) append(" ('$observationFilter')")
                                        append(" • Tablas: ${selectedTables.size}")
                                    }
                                    Text(
                                        summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (semilleroFilter.isNotBlank() || observationFilter.isNotBlank() || observationStatusFilter != "all") {
                                    TextButton(
                                        onClick = {
                                            semilleroFilter = ""
                                            observationFilter = ""
                                            observationStatusFilter = "all"
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Limpiar Filtros", fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                }

                                IconButton(
                                    onClick = { isFilterAccordionExpanded = !isFilterAccordionExpanded },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (isFilterAccordionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isFilterAccordionExpanded) "Colapsar filtros" else "Expandir filtros",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Cuerpo Desplegable del Acordeón
                        AnimatedVisibility(visible = isFilterAccordionExpanded) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(10.dp))

                                // A. Semilleros Clicables (Sin escribir)
                                Text(
                                    "A. Seleccionar Semillero (Un Clic):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = semilleroFilter.isBlank(),
                                        onClick = { semilleroFilter = "" },
                                        label = { Text("Todos los Semilleros (${extractedRecords.size})") },
                                        leadingIcon = {
                                            if (semilleroFilter.isBlank()) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    )

                                    detectedSemilleros.forEach { sem ->
                                        val count = semilleroCounts[sem] ?: 0
                                        val isSel = semilleroFilter.equals(sem, ignoreCase = true)
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { semilleroFilter = if (isSel) "" else sem },
                                            label = { Text("$sem ($count)") },
                                            leadingIcon = {
                                                Icon(
                                                    if (isSel) Icons.Default.Check else Icons.Default.School,
                                                    null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // B. Estado de Observaciones (Un Clic)
                                Text(
                                    "B. Estado de Observaciones (Un Clic):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = observationStatusFilter == "all",
                                        onClick = { observationStatusFilter = "all" },
                                        label = { Text("Todas (${extractedRecords.size})") },
                                        leadingIcon = {
                                            if (observationStatusFilter == "all") {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    )

                                    val withObsCount = extractedRecords.count { 
                                        it.observaciones.isNotBlank() && !it.observaciones.contains("Sin observaciones", ignoreCase = true) 
                                    }
                                    FilterChip(
                                        selected = observationStatusFilter == "with_obs",
                                        onClick = { observationStatusFilter = "with_obs" },
                                        label = { Text("Con Observaciones ($withObsCount)") },
                                        leadingIcon = {
                                            Icon(
                                                if (observationStatusFilter == "with_obs") Icons.Default.Check else Icons.Default.Comment,
                                                null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )

                                    val withoutObsCount = extractedRecords.size - withObsCount
                                    FilterChip(
                                        selected = observationStatusFilter == "without_obs",
                                        onClick = { observationStatusFilter = "without_obs" },
                                        label = { Text("Sin Observaciones / Pendientes ($withoutObsCount)") },
                                        leadingIcon = {
                                            Icon(
                                                if (observationStatusFilter == "without_obs") Icons.Default.Check else Icons.Default.CommentsDisabled,
                                                null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                // C. Tablas Origen
                                Text(
                                    "C. Tablas de Origen Extraídas:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    tables.forEach { tbl ->
                                        val isSel = tbl in selectedTables
                                        FilterChip(
                                            selected = isSel,
                                            onClick = {
                                                selectedTables = if (isSel) selectedTables - tbl else selectedTables + tbl
                                            },
                                            label = { Text(tbl) },
                                            leadingIcon = {
                                                Icon(
                                                    if (isSel) Icons.Default.Check else Icons.Default.TableRows,
                                                    null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                // Búsqueda opcional en texto
                                Spacer(Modifier.height(10.dp))
                                SearchBarField(
                                    value = observationFilter,
                                    onValueChange = { observationFilter = it },
                                    placeholder = "Búsqueda en texto de observaciones (opcional)...",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Feedback / Messages
        errorMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        successMessage?.let { msg ->
            Surface(
                color = Color(0xFFDCFCE7),
                border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = Color(0xFF14532D), modifier = Modifier.weight(1f))
                    IconButton(onClick = { successMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── 3. Visualización y Carga (L) ─────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Tabs and Search
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.width(360.dp),
                            containerColor = Color.Transparent
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Fichas de Semillero (${filteredRecords.size})") },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Vista Tabular") },
                                icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        // Search general
                        SearchBarField(
                            value = generalSearch,
                            onValueChange = { generalSearch = it },
                            placeholder = "Buscar en todo el resultado...",
                            modifier = Modifier.width(280.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (filteredRecords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FilterListOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (extractedRecords.isEmpty()) "Selecciona tablas para iniciar la extracción ETL."
                                else "No se encontraron registros con los filtros de Semillero u Observaciones actuales.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    if (selectedTab == 0) {
                        // ── Tab 0: Fichas de Semillero ──────────────────────
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Group records by semillero
                            val grouped = filteredRecords.groupBy { it.semillero }
                            grouped.forEach { (semName, recList) ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    semName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    "${recList.size} registro(s) asociado(s)",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        recList.forEachIndexed { idx, rec ->
                                            if (idx > 0) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Tabla origen & ID badge
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.width(130.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(
                                                            rec.sourceTable,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            "ID: ${rec.id}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }

                                                // Descripción del Semillero
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "Descripción del Semillero:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        rec.descripcion.ifBlank { "Sin descripción registrada" },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (rec.descripcion.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                // Observaciones Realizadas al Semillero con Botón de Edición Directa
                                                Surface(
                                                    modifier = Modifier.weight(1.3f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFEF3C7).copy(alpha = 0.4f),
                                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    Icons.Default.Comment,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(14.dp),
                                                                    tint = Color(0xFFD97706)
                                                                )
                                                                Spacer(Modifier.width(6.dp))
                                                                Text(
                                                                    "Observaciones al Semillero:",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFFB45309)
                                                                )
                                                            }

                                                            // Botón Agregar / Editar Observación
                                                            TextButton(
                                                                onClick = {
                                                                    editingRecord = rec
                                                                    editObservationText = if (rec.observaciones.startsWith("Sin observaciones")) "" else rec.observaciones
                                                                    showObservationDialog = true
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                                modifier = Modifier.height(24.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Edit,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(12.dp),
                                                                    tint = Color(0xFFB45309)
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(
                                                                    if (rec.observaciones.isBlank() || rec.observaciones.contains("Sin observaciones", ignoreCase = true)) "+ Agregar" else "Editar",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color(0xFFB45309),
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(
                                                            rec.observaciones.ifBlank { "Sin observaciones registradas" },
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (rec.observaciones.isBlank() || rec.observaciones.contains("Sin observaciones", ignoreCase = true)) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ── Tab 1: Vista Tabular ─────────────────────────────
                        val hScroll = rememberScrollState()
                        val vScroll = rememberScrollState()

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(hScroll)
                                    .verticalScroll(vScroll)
                            ) {
                                // Header
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("#", modifier = Modifier.width(45.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text("Acción", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text("Tabla", modifier = Modifier.width(130.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text("Semillero", modifier = Modifier.width(180.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text("Descripción", modifier = Modifier.width(220.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text("Observaciones", modifier = Modifier.width(260.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        allColumnsFound.forEach { extraCol ->
                                            Text(extraCol, modifier = Modifier.width(150.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                filteredRecords.forEachIndexed { rIdx, rec ->
                                    val isEven = rIdx % 2 == 0
                                    Surface(
                                        color = if (isEven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${rIdx + 1}", modifier = Modifier.width(45.dp), style = MaterialTheme.typography.bodySmall)

                                            // Acción rápida para editar observación
                                            Box(modifier = Modifier.width(80.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        editingRecord = rec
                                                        editObservationText = if (rec.observaciones.startsWith("Sin observaciones")) "" else rec.observaciones
                                                        showObservationDialog = true
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(2.dp))
                                                    Text("Nota", fontSize = 10.sp)
                                                }
                                            }

                                            Text(rec.sourceTable, modifier = Modifier.width(130.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text(rec.semillero, modifier = Modifier.width(180.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                            Text(rec.descripcion.ifBlank { "—" }, modifier = Modifier.width(220.dp), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(rec.observaciones.ifBlank { "—" }, modifier = Modifier.width(260.dp), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            allColumnsFound.forEach { extraCol ->
                                                val v = rec.rawData[extraCol]?.toString() ?: "—"
                                                Text(v, modifier = Modifier.width(150.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Diálogo Automático para Añadir/Editar Observación ─────────────
    if (showObservationDialog && editingRecord != null) {
        val rec = editingRecord!!
        var isSavingObs by remember { mutableStateOf(false) }
        var obsDialogError by remember { mutableStateOf<String?>(null) }

        val hasObsColumn = remember(rec.sourceTable) {
            val info = try { engine?.getTableInfo(rec.sourceTable) } catch (_: Exception) { null }
            info?.columns?.any { 
                val l = it.name.lowercase()
                l == "observaciones" || l == "observacion" || l == "comentarios" || l == "notas"
            } == true
        }

        AlertDialog(
            onDismissRequest = { if (!isSavingObs) showObservationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (rec.observaciones.isBlank() || rec.observaciones.contains("Sin observaciones", ignoreCase = true)) 
                            "Añadir Observación a Registro" 
                        else "Editar Observación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Tabla: '${rec.sourceTable}' • Registro ID: ${rec.id}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!hasObsColumn) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "La tabla '${rec.sourceTable}' no tiene una columna 'observaciones'. Se creará automáticamente en la base de datos al guardar.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editObservationText,
                        onValueChange = { editObservationText = it },
                        label = { Text("Escribe la observación o novedad") },
                        placeholder = { Text("Ej: Semillero en etapa de formulación de propuesta, excelente avance técnico...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    obsDialogError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (engine == null) return@Button
                        isSavingObs = true
                        obsDialogError = null
                        scope.launch {
                            try {
                                val tblInfo = engine.getTableInfo(rec.sourceTable)
                                // 1. If table lacks an observation column, add it automatically
                                val existingObsCol = tblInfo.columns.find { 
                                    val l = it.name.lowercase()
                                    l == "observaciones" || l == "observacion" || l == "comentarios" || l == "notas"
                                }
                                val targetColName = existingObsCol?.name ?: "observaciones"

                                if (existingObsCol == null) {
                                    engine.addColumn(
                                        rec.sourceTable,
                                        Column(
                                            name = "observaciones",
                                            type = ColumnType.TEXT,
                                            isNullable = true,
                                            length = 255
                                        )
                                    )
                                }

                                // 2. Update the row using primary key or row id
                                val pkCol = tblInfo.primaryKeys.firstOrNull()?.name ?: (if (rec.rawData.containsKey("id")) "id" else tblInfo.columns.first().name)
                                val pkVal = rec.rawData[pkCol] ?: rec.id

                                engine.updateRow(
                                    rec.sourceTable,
                                    mapOf(pkCol to pkVal),
                                    mapOf(targetColName to editObservationText.trim())
                                )

                                withContext(Dispatchers.Main) {
                                    showObservationDialog = false
                                    successMessage = "Observación guardada correctamente en la tabla '${rec.sourceTable}'"
                                    runEtl()
                                    onTableCreated()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    obsDialogError = "Error al guardar: ${e.message}"
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isSavingObs = false
                                }
                            }
                        }
                    },
                    enabled = !isSavingObs
                ) {
                    if (isSavingObs) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Guardar Observación")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showObservationDialog = false },
                    enabled = !isSavingObs
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
