@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
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
import com.dbstudio.data.QueryResult
import com.dbstudio.model.Relationship
import com.dbstudio.ui.components.FileChooser
import com.dbstudio.ui.components.SearchBarField
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

enum class JoinType(val displayName: String, val sql: String) {
    LEFT_JOIN("Unión Izquierda (LEFT JOIN - Completa)", "LEFT JOIN"),
    INNER_JOIN("Unión Interna (INNER JOIN - Coincidentes)", "INNER JOIN")
}

@Composable
fun JoinTablesScreen(
    engine: DatabaseEngine?,
    tables: List<String>,
    modifier: Modifier = Modifier
) {
    var primaryTable by remember { mutableStateOf<String?>(null) }
    var selectedSecondaryTables by remember { mutableStateOf<Set<String>>(emptySet()) }
    var joinType by remember { mutableStateOf(JoinType.LEFT_JOIN) }
    var searchQuery by remember { mutableStateOf("") }
    var queryResult by remember { mutableStateOf<QueryResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var relationships by remember { mutableStateOf<List<Relationship>>(emptyList()) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    // Table colors palette for header badges
    val tableColorPalette = listOf(
        Color(0xFF2563EB), // Blue
        Color(0xFF059669), // Green
        Color(0xFFD97706), // Amber
        Color(0xFF7C3AED), // Purple
        Color(0xFFDC2626), // Red
        Color(0xFF0891B2), // Cyan
        Color(0xFFDB2777), // Pink
        Color(0xFF4B5563)  // Gray
    )

    val tableColorMap = remember(tables) {
        tables.mapIndexed { index, name ->
            name to tableColorPalette[index % tableColorPalette.size]
        }.toMap()
    }

    // Load relationships when engine changes
    LaunchedEffect(engine, tables) {
        if (engine != null && tables.isNotEmpty()) {
            relationships = try {
                engine.getRelationships()
            } catch (_: Exception) {
                emptyList()
            }
            if (primaryTable == null || primaryTable !in tables) {
                primaryTable = tables.firstOrNull()
            }
        }
    }

    // Function to build and execute join query
    fun executeJoin() {
        val eng = engine ?: return
        val pTable = primaryTable ?: return
        isLoading = true
        errorMessage = null
        exportMessage = null

        try {
            val dbType = eng.databaseType
            val q = if (dbType == com.dbstudio.model.DatabaseType.ACCESS) "[" else "\""
            val qEnd = if (dbType == com.dbstudio.model.DatabaseType.ACCESS) "]" else "\""

            val activeTables = listOf(pTable) + selectedSecondaryTables.toList()

            // Build SELECT columns with aliases table_column
            val selectClauses = mutableListOf<String>()
            val colTableMap = mutableMapOf<String, String>() // colDisplay -> tableName

            for (tbl in activeTables) {
                val info = try { eng.getTableInfo(tbl) } catch (_: Exception) { null }
                if (info != null) {
                    for (col in info.columns) {
                        val colAlias = "${tbl}_${col.name}"
                        selectClauses.add("$q$tbl$qEnd.$q${col.name}$qEnd AS $q$colAlias$qEnd")
                        colTableMap[colAlias] = tbl
                    }
                } else {
                    selectClauses.add("$q$tbl$qEnd.*")
                }
            }

            val selectStr = if (selectClauses.isNotEmpty()) selectClauses.joinToString(", ") else "*"
            val sqlBuilder = StringBuilder("SELECT $selectStr FROM $q$pTable$qEnd")

            val joinedSoFar = mutableSetOf(pTable.lowercase())

            for (secTable in selectedSecondaryTables) {
                // Find FK relationship connecting secTable to any already joined table
                val matchingRel = relationships.find { rel ->
                    (rel.sourceTable.equals(secTable, ignoreCase = true) && rel.targetTable.lowercase() in joinedSoFar) ||
                    (rel.targetTable.equals(secTable, ignoreCase = true) && rel.sourceTable.lowercase() in joinedSoFar)
                }

                if (matchingRel != null) {
                    val isSource = matchingRel.sourceTable.equals(secTable, ignoreCase = true)
                    val fromTable = if (isSource) matchingRel.sourceTable else matchingRel.targetTable
                    val fromCol = if (isSource) matchingRel.sourceColumn else matchingRel.targetColumn
                    val toTable = if (isSource) matchingRel.targetTable else matchingRel.sourceTable
                    val toCol = if (isSource) matchingRel.targetColumn else matchingRel.sourceColumn

                    sqlBuilder.append(" ${joinType.sql} $q$secTable$qEnd ON $q$fromTable$qEnd.$q$fromCol$qEnd = $q$toTable$qEnd.$q$toCol$qEnd")
                } else {
                    // Fallback heuristics: check if secondary table has <primaryTable>_id or id
                    val secInfo = try { eng.getTableInfo(secTable) } catch (_: Exception) { null }
                    val pInfo = try { eng.getTableInfo(pTable) } catch (_: Exception) { null }

                    val fkCandidateInSec = secInfo?.columns?.find {
                        it.name.equals("${pTable}_id", ignoreCase = true) ||
                        it.name.equals("${pTable}id", ignoreCase = true) ||
                        it.name.equals("id_${pTable}", ignoreCase = true)
                    }

                    val fkCandidateInPrim = pInfo?.columns?.find {
                        it.name.equals("${secTable}_id", ignoreCase = true) ||
                        it.name.equals("${secTable}id", ignoreCase = true) ||
                        it.name.equals("id_${secTable}", ignoreCase = true)
                    }

                    if (fkCandidateInSec != null) {
                        sqlBuilder.append(" ${joinType.sql} $q$secTable$qEnd ON $q$secTable$qEnd.$q${fkCandidateInSec.name}$qEnd = $q$pTable$qEnd.$q${pInfo?.primaryKeys?.firstOrNull()?.name ?: "id"}$qEnd")
                    } else if (fkCandidateInPrim != null) {
                        sqlBuilder.append(" ${joinType.sql} $q$secTable$qEnd ON $q$pTable$qEnd.$q${fkCandidateInPrim.name}$qEnd = $q$secTable$qEnd.$q${secInfo?.primaryKeys?.firstOrNull()?.name ?: "id"}$qEnd")
                    } else {
                        // Natural join fallback or simple id match
                        sqlBuilder.append(" ${joinType.sql} $q$secTable$qEnd ON $q$pTable$qEnd.$q${pInfo?.primaryKeys?.firstOrNull()?.name ?: "id"}$qEnd = $q$secTable$qEnd.$q${secInfo?.primaryKeys?.firstOrNull()?.name ?: "id"}$qEnd")
                    }
                }
                joinedSoFar.add(secTable.lowercase())
            }

            val finalSql = sqlBuilder.toString()
            queryResult = eng.executeQuery(finalSql)
        } catch (e: Exception) {
            errorMessage = "Error al ejecutar unión: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Auto-execute join when tables or join type changes
    LaunchedEffect(primaryTable, selectedSecondaryTables, joinType) {
        if (primaryTable != null && engine != null) {
            executeJoin()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // ── Header Card ─────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.CallMerge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                "Juntar Tablas — Visor Multitabla Unificado",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Combina y visualiza información cruzada de múltiples tablas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                if (primaryTable != null) {
                                    val otherTables = tables.filter { it != primaryTable }.toSet()
                                    selectedSecondaryTables = if (selectedSecondaryTables.size == otherTables.size) emptySet() else otherTables
                                }
                            }
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (selectedSecondaryTables.size == tables.size - 1 && tables.size > 1) "Deseleccionar Todas" else "Unir Todas las Tablas")
                        }

                        Button(
                            onClick = { executeJoin() },
                            enabled = !isLoading && primaryTable != null
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Actualizar")
                        }

                        // Export button
                        OutlinedButton(
                            onClick = {
                                val res = queryResult ?: return@OutlinedButton
                                val savePath = FileChooser.saveFile(
                                    title = "Exportar Unión a Excel",
                                    defaultName = "Tablas_Unidas_${System.currentTimeMillis() % 10000}.xlsx",
                                    extension = "xlsx",
                                    description = "Libro de Excel (*.xlsx)"
                                )
                                if (savePath != null) {
                                    try {
                                        val wb = XSSFWorkbook()
                                        val sheet = wb.createSheet("Tablas_Unidas")
                                        val headerRow = sheet.createRow(0)
                                        res.columns.forEachIndexed { i, col ->
                                            headerRow.createCell(i).setCellValue(col)
                                        }
                                        res.rows.forEachIndexed { rIdx, row ->
                                            val dataRow = sheet.createRow(rIdx + 1)
                                            row.forEachIndexed { cIdx, cellVal ->
                                                dataRow.createCell(cIdx).setCellValue(cellVal?.toString() ?: "")
                                            }
                                        }
                                        res.columns.indices.forEach { sheet.autoSizeColumn(it) }
                                        FileOutputStream(File(savePath)).use { wb.write(it) }
                                        wb.close()
                                        exportMessage = "Exportado con éxito a '$savePath'"
                                    } catch (e: Exception) {
                                        errorMessage = "Error al exportar: ${e.message}"
                                    }
                                }
                            },
                            enabled = queryResult != null && (queryResult?.rows?.isNotEmpty() == true)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exportar a Excel")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))

                // ── Controls: Primary Table + Secondary Tables + Join Type ─────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Primary Table Selector
                    var primaryDropdownExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tabla Principal (Base):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Box {
                            OutlinedButton(
                                onClick = { primaryDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    primaryTable ?: "Seleccionar tabla...",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = primaryDropdownExpanded,
                                onDismissRequest = { primaryDropdownExpanded = false }
                            ) {
                                tables.forEach { tbl ->
                                    DropdownMenuItem(
                                        text = { Text(tbl, fontWeight = if (tbl == primaryTable) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(Icons.Default.TableRows, contentDescription = null) },
                                        onClick = {
                                            primaryTable = tbl
                                            selectedSecondaryTables = selectedSecondaryTables - tbl
                                            primaryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Join Type Selector
                    var joinDropdownExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tipo de Unión SQL:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Box {
                            OutlinedButton(
                                onClick = { joinDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    joinType.displayName,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = joinDropdownExpanded,
                                onDismissRequest = { joinDropdownExpanded = false }
                            ) {
                                JoinType.entries.forEach { jt ->
                                    DropdownMenuItem(
                                        text = { Text(jt.displayName, fontWeight = if (jt == joinType) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            joinType = jt
                                            joinDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Secondary Tables Multi-select Chips
                Column {
                    Text(
                        "Tablas Secundarias a Unir con '${primaryTable ?: "..."}':",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val availableSecondary = tables.filter { it != primaryTable }
                        if (availableSecondary.isEmpty()) {
                            Text(
                                "No hay más tablas en la base de datos para combinar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            availableSecondary.forEach { tbl ->
                                val isSelected = tbl in selectedSecondaryTables
                                val tblColor = tableColorMap[tbl] ?: MaterialTheme.colorScheme.primary
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSecondaryTables = if (isSelected) {
                                            selectedSecondaryTables - tbl
                                        } else {
                                            selectedSecondaryTables + tbl
                                        }
                                    },
                                    label = { Text(tbl) },
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(tblColor)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Messages / Feedback
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
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        exportMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.weight(1f))
                    IconButton(onClick = { exportMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Results Grid & Search Bar ───────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar above grid
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val res = queryResult
                        val totalRows = res?.rows?.size ?: 0
                        val totalCols = res?.columns?.size ?: 0

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Vista Combinada",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "$totalRows registros • $totalCols columnas",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Search box
                        SearchBarField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Filtrar registros...",
                            modifier = Modifier.width(260.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val res = queryResult
                    if (res == null || res.columns.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Selecciona una tabla principal y las tablas a unir para ver los datos combinados.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val filteredRows = remember(res.rows, searchQuery) {
                            if (searchQuery.isBlank()) res.rows
                            else {
                                res.rows.filter { row ->
                                    row.any { cell ->
                                        cell?.toString()?.contains(searchQuery.trim(), ignoreCase = true) == true
                                    }
                                }
                            }
                        }

                        val hScrollState = rememberScrollState()
                        val vScrollState = rememberScrollState()

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(hScrollState)
                                    .verticalScroll(vScrollState)
                            ) {
                                // Header Row
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                        Text(
                                            "#",
                                            modifier = Modifier.width(50.dp),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        res.columns.forEach { colName ->
                                            // Extract table prefix if present (e.g. Estudiante_nombre -> table: Estudiante, col: nombre)
                                            val parts = colName.split("_", limit = 2)
                                            val tablePrefix = if (parts.size == 2 && parts[0] in tables) parts[0] else null
                                            val cleanCol = if (tablePrefix != null) parts[1] else colName
                                            val badgeColor = tableColorMap[tablePrefix] ?: MaterialTheme.colorScheme.primary

                                            Row(
                                                modifier = Modifier.width(180.dp).padding(horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (tablePrefix != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = badgeColor.copy(alpha = 0.2f),
                                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    ) {
                                                        Text(
                                                            tablePrefix,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = badgeColor,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    cleanCol,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Data Rows
                                if (filteredRows.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (searchQuery.isNotEmpty()) "No se encontraron registros que coincidan con '$searchQuery'" else "La consulta no arrojó registros coincidentes.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    filteredRows.forEachIndexed { rIdx, row ->
                                        val isEven = rIdx % 2 == 0
                                        Surface(
                                            color = if (isEven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "${rIdx + 1}",
                                                    modifier = Modifier.width(50.dp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                                row.forEach { cellVal ->
                                                    val strVal = cellVal?.toString() ?: "—"
                                                    Text(
                                                        strVal,
                                                        modifier = Modifier.width(180.dp).padding(horizontal = 4.dp),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (cellVal == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
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
    }
}
