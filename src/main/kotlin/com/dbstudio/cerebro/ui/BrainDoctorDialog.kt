@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.cerebro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dbstudio.cerebro.Cerebro
import com.dbstudio.cerebro.advisor.IssueLevel
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.Relationship

/**
 * The Central Brain Doctor Dialog - Health Audit, Smart Suggestions,
 * Many-to-Many Intermediate Tables, and Mock Data Population.
 */
@Composable
fun BrainDoctorDialog(
    engine: DatabaseEngine,
    onDismiss: () -> Unit,
    onSchemaChanged: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Auditoría, 1: Relaciones y N:M, 2: Rellenar Datos
    val tables = remember {
        try {
            engine.getTableNames().map { engine.getTableInfo(it) }
        } catch (_: Exception) { emptyList() }
    }
    val relationships = remember {
        try { engine.getRelationships() } catch (_: Exception) { emptyList() }
    }

    val auditIssues = remember(tables) { Cerebro.auditDatabase(tables) }
    val suggestedRels = remember(tables, relationships) {
        Cerebro.detectPossibleRelationships(tables, relationships)
    }
    val junctionTables = remember(tables, relationships) {
        Cerebro.findJunctionTables(tables, relationships)
    }

    var showIntermediateDialog by remember { mutableStateOf(false) }
    var selectedTableForMock by remember { mutableStateOf(tables.firstOrNull()?.name ?: "") }
    var mockRowCount by remember { mutableStateOf(10) }
    var mockStatusMessage by remember { mutableStateOf<String?>(null) }
    var isGeneratingMock by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Cerebro - Asistente de Base de Datos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Optimización, auditoría y relaciones locales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .width(620.dp)
                    .heightIn(max = 560.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Auditoría")
                                if (auditIssues.isNotEmpty()) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("${auditIssues.size}")
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Relaciones y N:M")
                                if (suggestedRels.isNotEmpty()) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${suggestedRels.size}")
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Llenar Datos (Mock)") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // ── TAB 0: AUDITORÍA ────────────────────────────────
                            if (auditIssues.isEmpty()) {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text("¡Esquema en Excelente Estado!", fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                            Text("No se detectaron problemas de diseño, palabras reservadas o claves primarias faltantes.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF166534))
                                        }
                                    }
                                }
                            } else {
                                Text("Recomendaciones de diseño detectadas:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                auditIssues.forEach { issue ->
                                    val (bgColor, borderColor, iconColor) = when (issue.level) {
                                        IssueLevel.ERROR -> Triple(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
                                        IssueLevel.WARNING -> Triple(Color(0xFFFEF3C7), Color(0xFFF59E0B), Color(0xFFD97706))
                                        IssueLevel.INFO -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.primary)
                                    }

                                    Surface(
                                        color = bgColor,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("[${issue.tableName}] ${issue.title}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                            }
                                            Text(issue.description, style = MaterialTheme.typography.bodySmall)
                                            Text("💡 Solución: ${issue.recommendation}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ── TAB 1: RELACIONES Y TABLAS INTERMEDIAS ──────────
                            // Action button for Many-to-Many intermediate tables
                            Button(
                                onClick = { showIntermediateDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Crear Tabla Intermedia (Muchos a Muchos N:M)")
                            }

                            if (junctionTables.isNotEmpty()) {
                                Text("Tablas Intermedias N:M identificadas:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                junctionTables.forEach { jt ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(jt.tableName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                                Text("Conecta '${jt.tableA}' ⮂ '${jt.tableB}' (Muchos a Muchos)", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }

                            if (suggestedRels.isNotEmpty()) {
                                Text("Relaciones 1:N sugeridas automáticamente:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                suggestedRels.forEach { sug ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${sug.sourceTable}.${sug.sourceColumn} ➔ ${sug.targetTable}.${sug.targetColumn}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                                Text(sug.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Button(
                                                onClick = {
                                                    try {
                                                        engine.addRelationship(
                                                            Relationship(
                                                                name = "fk_${sug.sourceTable}_${sug.sourceColumn}",
                                                                sourceTable = sug.sourceTable,
                                                                sourceColumn = sug.sourceColumn,
                                                                targetTable = sug.targetTable,
                                                                targetColumn = sug.targetColumn
                                                            )
                                                        )
                                                        onSchemaChanged()
                                                    } catch (_: Exception) {}
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Conectar", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            } else if (junctionTables.isEmpty()) {
                                Text("No hay sugerencias de relaciones pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        2 -> {
                            // ── TAB 2: RELLENAR DATOS DE PRUEBA ─────────────────
                            Text("Generador de datos realistas (respeta claves foráneas y restricciones)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Text("Selecciona la tabla a rellenar:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tables.forEach { t ->
                                    FilterChip(
                                        selected = selectedTableForMock == t.name,
                                        onClick = { selectedTableForMock = t.name },
                                        label = { Text(t.name) }
                                    )
                                }
                            }

                            Text("Cantidad de filas a generar:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5, 10, 25, 50).forEach { count ->
                                    FilterChip(
                                        selected = mockRowCount == count,
                                        onClick = { mockRowCount = count },
                                        label = { Text("$count filas") }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (selectedTableForMock.isBlank()) return@Button
                                    isGeneratingMock = true
                                    mockStatusMessage = null
                                    try {
                                        val res = Cerebro.generateMockData(engine, selectedTableForMock, mockRowCount)
                                        mockStatusMessage = "¡Se generaron e insertaron ${res.rowsInserted} filas con éxito!"
                                        onSchemaChanged()
                                    } catch (e: Exception) {
                                        val diag = Cerebro.diagnoseError(e)
                                        mockStatusMessage = "Error: ${diag.friendlyExplanation}"
                                    } finally {
                                        isGeneratingMock = false
                                    }
                                },
                                enabled = !isGeneratingMock && selectedTableForMock.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isGeneratingMock) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text("Generar $mockRowCount Registros en '$selectedTableForMock'")
                            }

                            mockStatusMessage?.let { msg ->
                                Surface(
                                    color = if (msg.startsWith("¡")) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        msg,
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (msg.startsWith("¡")) Color(0xFF166534) else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )

    if (showIntermediateDialog) {
        IntermediateTableDialog(
            engine = engine,
            availableTables = tables.map { it.name },
            onDismiss = { showIntermediateDialog = false },
            onSuccess = { createdName ->
                showIntermediateDialog = false
                onSchemaChanged()
            }
        )
    }
}
