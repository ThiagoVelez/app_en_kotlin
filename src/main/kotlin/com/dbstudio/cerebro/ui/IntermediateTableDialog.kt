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
import com.dbstudio.data.DatabaseEngine

/**
 * Dialog for generating a Many-to-Many (N:M) intermediate / junction table between two tables.
 */
@Composable
fun IntermediateTableDialog(
    engine: DatabaseEngine,
    availableTables: List<String>,
    initialTableA: String? = null,
    initialTableB: String? = null,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var tableA by remember { mutableStateOf(initialTableA ?: (availableTables.getOrNull(0) ?: "")) }
    var tableB by remember { mutableStateOf(initialTableB ?: (availableTables.getOrNull(1) ?: "")) }
    var tableName by remember(tableA, tableB) {
        val cleanA = tableA.lowercase()
        val cleanB = tableB.lowercase()
        mutableStateOf(if (cleanA.isNotBlank() && cleanB.isNotBlank() && cleanA != cleanB) "${cleanA}_${cleanB}" else "")
    }
    var includeAuditFields by remember { mutableStateOf(true) }

    var tableAExpanded by remember { mutableStateOf(false) }
    var tableBExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Generador de Tabla Intermedia (Muchos a Muchos)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .width(520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "En bases de datos relacionales, una relación Muchos a Muchos (N:M) no se conecta directamente con una sola clave foránea. Requiere una Tabla Intermedia con claves foráneas apuntando a ambas entidades.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                errorMessage?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Table A Dropdown
                Text("Primera Tabla (Entidad A)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = tableAExpanded,
                    onExpandedChange = { tableAExpanded = !tableAExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tableA,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tableAExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = tableAExpanded,
                        onDismissRequest = { tableAExpanded = false }
                    ) {
                        availableTables.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    tableA = name
                                    tableAExpanded = false
                                }
                            )
                        }
                    }
                }

                // Table B Dropdown
                Text("Segunda Tabla (Entidad B)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = tableBExpanded,
                    onExpandedChange = { tableBExpanded = !tableBExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tableB,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tableBExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = tableBExpanded,
                        onDismissRequest = { tableBExpanded = false }
                    ) {
                        availableTables.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    tableB = name
                                    tableBExpanded = false
                                }
                            )
                        }
                    }
                }

                // Intermediate Table Name
                Text("Nombre de la Nueva Tabla Intermedia", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = tableName,
                    onValueChange = { tableName = it.trim() },
                    placeholder = { Text("ej. estudiantes_semilleros") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Structure Preview
                if (tableA.isNotBlank() && tableB.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Columnas que se generarán:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("• id (INTEGER PRIMARY KEY AUTOINCREMENT)", style = MaterialTheme.typography.bodySmall)
                            Text("• ${tableA.lowercase()}_id (INTEGER FK ➔ $tableA.id)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("• ${tableB.lowercase()}_id (INTEGER FK ➔ $tableB.id)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            if (includeAuditFields) {
                                Text("• fecha_asignacion (DATETIME)", style = MaterialTheme.typography.bodySmall)
                                Text("• estado (VARCHAR)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeAuditFields,
                        onCheckedChange = { includeAuditFields = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Incluir campos adicionales (fecha de asignación y estado)", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tableA.isBlank() || tableB.isBlank()) {
                        errorMessage = "Selecciona ambas tablas."
                        return@Button
                    }
                    if (tableA.equals(tableB, ignoreCase = true)) {
                        errorMessage = "Las dos tablas deben ser diferentes."
                        return@Button
                    }
                    if (tableName.isBlank()) {
                        errorMessage = "Ingresa un nombre para la tabla intermedia."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    try {
                        Cerebro.createManytoManyIntermediateTable(
                            engine = engine,
                            tableA = tableA,
                            tableB = tableB,
                            customTableName = tableName,
                            includeAuditDates = includeAuditFields
                        )
                        onSuccess(tableName)
                    } catch (e: Exception) {
                        val diag = Cerebro.diagnoseError(e)
                        errorMessage = "${diag.title}: ${diag.friendlyExplanation}"
                    } finally {
                        isLoading = false
                    }
                },
                enabled = !isLoading && tableA.isNotBlank() && tableB.isNotBlank() && !tableA.equals(tableB, ignoreCase = true)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(6.dp))
                }
                Text("Crear Tabla Intermedia N:M")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
