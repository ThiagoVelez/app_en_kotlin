@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.screens

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
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.*
import com.dbstudio.ui.theme.AppColors

/**
 * Dialog for creating, editing, and deleting relationships (foreign keys) between tables.
 */
@Composable
fun RelationshipDialog(
    engine: DatabaseEngine,
    initialRelationship: Relationship? = null,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    onDeleted: (Relationship) -> Unit = {},
    onUpdated: (Relationship, Relationship) -> Unit = { _, _ -> }
) {
    var editingRel by remember { mutableStateOf(initialRelationship) }
    var relationshipsList by remember { mutableStateOf(try { engine.getRelationships() } catch (_: Exception) { emptyList() }) }
    val tableNames = remember { try { engine.getTableNames() } catch (_: Exception) { emptyList() } }

    var sourceTable by remember { mutableStateOf(initialRelationship?.sourceTable ?: "") }
    var sourceColumn by remember { mutableStateOf(initialRelationship?.sourceColumn ?: "") }
    var targetTable by remember { mutableStateOf(initialRelationship?.targetTable ?: "") }
    var targetColumn by remember { mutableStateOf(initialRelationship?.targetColumn ?: "") }
    var relationType by remember { mutableStateOf(initialRelationship?.type ?: RelationType.ONE_TO_MANY) }
    var onDelete by remember { mutableStateOf(initialRelationship?.onDelete ?: ReferentialAction.NO_ACTION) }
    var onUpdate by remember { mutableStateOf(initialRelationship?.onUpdate ?: ReferentialAction.NO_ACTION) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var relToDelete by remember { mutableStateOf<Relationship?>(null) }

    var sourceTableExpanded by remember { mutableStateOf(false) }
    var sourceColumnExpanded by remember { mutableStateOf(false) }
    var targetTableExpanded by remember { mutableStateOf(false) }
    var targetColumnExpanded by remember { mutableStateOf(false) }
    var relTypeExpanded by remember { mutableStateOf(false) }
    var onDeleteExpanded by remember { mutableStateOf(false) }
    var onUpdateExpanded by remember { mutableStateOf(false) }

    val sourceColumns = remember(sourceTable) {
        if (sourceTable.isNotEmpty()) {
            try { engine.getTableInfo(sourceTable).columns } catch (_: Exception) { emptyList() }
        } else emptyList()
    }
    val targetColumns = remember(targetTable) {
        if (targetTable.isNotEmpty()) {
            try { engine.getTableInfo(targetTable).columns } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    fun loadForEdit(rel: Relationship) {
        editingRel = rel
        sourceTable = rel.sourceTable
        sourceColumn = rel.sourceColumn
        targetTable = rel.targetTable
        targetColumn = rel.targetColumn
        relationType = rel.type
        onDelete = rel.onDelete
        onUpdate = rel.onUpdate
        errorMessage = null
        successMessage = null
    }

    fun resetForm() {
        editingRel = null
        sourceTable = ""
        sourceColumn = ""
        targetTable = ""
        targetColumn = ""
        relationType = RelationType.ONE_TO_MANY
        onDelete = ReferentialAction.NO_ACTION
        onUpdate = ReferentialAction.NO_ACTION
        errorMessage = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (editingRel != null) "Editar Relación" else "Gestor de Relaciones",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .width(520.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tableNames.size < 2) {
                    Text(
                        "Necesitas al menos 2 tablas creadas para establecer relaciones.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    return@Column
                }

                // Status messages
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

                successMessage?.let {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = Color(0xFF166534), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Edit mode notice
                if (editingRel != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Modificando: ${editingRel!!.displaySummary}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            TextButton(
                                onClick = { resetForm() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Crear Nueva", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // ── Source Table & Column ────────────────────────────
                Text(
                    "Tabla de Origen (Contiene la Clave Foránea / FK)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Source Table
                    ExposedDropdownMenuBox(
                        expanded = sourceTableExpanded,
                        onExpandedChange = { sourceTableExpanded = !sourceTableExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = sourceTable,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tabla") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceTableExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = sourceTableExpanded,
                            onDismissRequest = { sourceTableExpanded = false }
                        ) {
                            tableNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        sourceTable = name
                                        sourceColumn = ""
                                        sourceTableExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Source Column
                    ExposedDropdownMenuBox(
                        expanded = sourceColumnExpanded,
                        onExpandedChange = { if (sourceTable.isNotEmpty()) sourceColumnExpanded = !sourceColumnExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = sourceColumn,
                            onValueChange = {},
                            readOnly = true,
                            enabled = sourceTable.isNotEmpty(),
                            label = { Text("Columna FK") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceColumnExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = sourceColumnExpanded,
                            onDismissRequest = { sourceColumnExpanded = false }
                        ) {
                            sourceColumns.forEach { col ->
                                DropdownMenuItem(
                                    text = { Text("${col.name} (${col.type.displayName})") },
                                    onClick = {
                                        sourceColumn = col.name
                                        sourceColumnExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Target Table & Column ────────────────────────────
                Text(
                    "Tabla de Destino (Tabla Referenciada con PK)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Target Table
                    ExposedDropdownMenuBox(
                        expanded = targetTableExpanded,
                        onExpandedChange = { targetTableExpanded = !targetTableExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = targetTable,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tabla Referenciada") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetTableExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = targetTableExpanded,
                            onDismissRequest = { targetTableExpanded = false }
                        ) {
                            tableNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        targetTable = name
                                        targetColumn = ""
                                        targetTableExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Target Column
                    ExposedDropdownMenuBox(
                        expanded = targetColumnExpanded,
                        onExpandedChange = { if (targetTable.isNotEmpty()) targetColumnExpanded = !targetColumnExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = targetColumn,
                            onValueChange = {},
                            readOnly = true,
                            enabled = targetTable.isNotEmpty(),
                            label = { Text("Columna PK") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetColumnExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = targetColumnExpanded,
                            onDismissRequest = { targetColumnExpanded = false }
                        ) {
                            targetColumns.forEach { col ->
                                DropdownMenuItem(
                                    text = { Text("${col.name} (${col.type.displayName})") },
                                    onClick = {
                                        targetColumn = col.name
                                        targetColumnExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Relationship Type & Referential Actions ──────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Type
                    ExposedDropdownMenuBox(
                        expanded = relTypeExpanded,
                        onExpandedChange = { relTypeExpanded = !relTypeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = relationType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relTypeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = relTypeExpanded,
                            onDismissRequest = { relTypeExpanded = false }
                        ) {
                            RelationType.entries.forEach { rt ->
                                DropdownMenuItem(
                                    text = { Text(rt.displayName) },
                                    onClick = { relationType = rt; relTypeExpanded = false }
                                )
                            }
                        }
                    }

                    // On Delete
                    ExposedDropdownMenuBox(
                        expanded = onDeleteExpanded,
                        onExpandedChange = { onDeleteExpanded = !onDeleteExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = onDelete.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Al Eliminar") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = onDeleteExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = onDeleteExpanded,
                            onDismissRequest = { onDeleteExpanded = false }
                        ) {
                            ReferentialAction.entries.forEach { ra ->
                                DropdownMenuItem(
                                    text = { Text(ra.displayName) },
                                    onClick = { onDelete = ra; onDeleteExpanded = false }
                                )
                            }
                        }
                    }
                }

                // On Update
                ExposedDropdownMenuBox(
                    expanded = onUpdateExpanded,
                    onExpandedChange = { onUpdateExpanded = !onUpdateExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = onUpdate.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Al Actualizar (ON UPDATE)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = onUpdateExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = onUpdateExpanded,
                        onDismissRequest = { onUpdateExpanded = false }
                    ) {
                        ReferentialAction.entries.forEach { ra ->
                            DropdownMenuItem(
                                text = { Text(ra.displayName) },
                                onClick = { onUpdate = ra; onUpdateExpanded = false }
                            )
                        }
                    }
                }

                // ── Existing Relationships List ──────────────────────
                if (relationshipsList.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Relaciones Existentes (${relationshipsList.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    relationshipsList.forEach { rel ->
                        val isBeingEdited = editingRel?.name == rel.name
                        Surface(
                            color = if (isBeingEdited) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp),
                            border = if (isBeingEdited) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${rel.sourceTable}.${rel.sourceColumn} → ${rel.targetTable}.${rel.targetColumn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${rel.type.symbol} | Eliminar: ${rel.onDelete.sql} | Actualizar: ${rel.onUpdate.sql}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                // Edit button
                                IconButton(
                                    onClick = { loadForEdit(rel) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar relación",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = { relToDelete = rel },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar relación",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        sourceTable.isBlank() -> errorMessage = "Selecciona una tabla de origen"
                        sourceColumn.isBlank() -> errorMessage = "Selecciona una columna de origen"
                        targetTable.isBlank() -> errorMessage = "Selecciona una tabla de destino"
                        targetColumn.isBlank() -> errorMessage = "Selecciona una columna de destino"
                        else -> {
                            try {
                                val relName = editingRel?.name ?: "fk_${sourceTable}_${sourceColumn}_${targetTable}"
                                val newRel = Relationship(
                                    name = relName,
                                    sourceTable = sourceTable,
                                    sourceColumn = sourceColumn,
                                    targetTable = targetTable,
                                    targetColumn = targetColumn,
                                    type = relationType,
                                    onDelete = onDelete,
                                    onUpdate = onUpdate
                                )

                                if (editingRel != null) {
                                    // Update existing relationship
                                    engine.removeRelationship(editingRel!!.name, editingRel!!.sourceTable)
                                    engine.addRelationship(newRel)
                                    onUpdated(editingRel!!, newRel)
                                    successMessage = "Relación actualizada correctamente"
                                } else {
                                    // Add new relationship
                                    engine.addRelationship(newRel)
                                    onCreated()
                                    successMessage = "Relación creada correctamente"
                                }

                                relationshipsList = engine.getRelationships()
                                resetForm()
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            }
                        }
                    }
                },
                enabled = sourceTable.isNotBlank() && sourceColumn.isNotBlank() &&
                        targetTable.isNotBlank() && targetColumn.isNotBlank()
            ) {
                Text(if (editingRel != null) "Guardar Cambios" else "Crear Relación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )

    // Confirm Delete Dialog
    relToDelete?.let { rel ->
        AlertDialog(
            onDismissRequest = { relToDelete = null },
            title = { Text("Eliminar Relación") },
            text = {
                Text("¿Estás seguro de que deseas eliminar la relación entre '${rel.sourceTable}.${rel.sourceColumn}' y '${rel.targetTable}.${rel.targetColumn}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            engine.removeRelationship(rel.name, rel.sourceTable)
                            relationshipsList = engine.getRelationships()
                            if (editingRel?.name == rel.name) {
                                resetForm()
                            }
                            onDeleted(rel)
                            successMessage = "Relación eliminada"
                        } catch (e: Exception) {
                            errorMessage = "Error al eliminar: ${e.message}"
                        } finally {
                            relToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { relToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
