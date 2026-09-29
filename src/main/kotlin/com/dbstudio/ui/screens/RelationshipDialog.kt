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
    availableTables: List<String> = emptyList(),
    initialRelationship: Relationship? = null,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    onDeleted: (Relationship) -> Unit = {},
    onUpdated: (Relationship, Relationship) -> Unit = { _, _ -> }
) {
    var editingRel by remember { mutableStateOf(initialRelationship) }
    var relationshipsList by remember { mutableStateOf(try { engine.getRelationships() } catch (_: Exception) { emptyList() }) }
    val tableNames = remember(availableTables) {
        if (availableTables.isNotEmpty()) availableTables
        else try { engine.getTableNames() } catch (_: Exception) { emptyList() }
    }

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
    var showIntermediateDialog by remember { mutableStateOf(false) }

    var sourceTableExpanded by remember { mutableStateOf(false) }
    var sourceColumnExpanded by remember { mutableStateOf(false) }
    var targetTableExpanded by remember { mutableStateOf(false) }
    var targetColumnExpanded by remember { mutableStateOf(false) }
    var relTypeExpanded by remember { mutableStateOf(false) }
    var onDeleteExpanded by remember { mutableStateOf(false) }
    var onUpdateExpanded by remember { mutableStateOf(false) }

    var sourceColumnsRefreshKey by remember { mutableStateOf(0) }
    val sourceColumns = remember(sourceTable, sourceColumnsRefreshKey) {
        if (sourceTable.isNotEmpty()) {
            try { engine.getTableInfo(sourceTable).columns } catch (_: Exception) { emptyList() }
        } else emptyList()
    }
    val targetColumns = remember(targetTable) {
        if (targetTable.isNotEmpty()) {
            try { engine.getTableInfo(targetTable).columns } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    val personalColumnNames = remember {
        setOf(
            "nombre", "name", "nombres", "apellido", "apellidos", "celular", "telefono",
            "phone", "mobile", "correo", "email", "mail", "cedula", "dni", "direccion",
            "address", "descripcion", "observacion", "nota", "comentario", "foto", "avatar"
        )
    }
    val isSuspectPersonalColumn = sourceColumn.isNotBlank() &&
            personalColumnNames.any { it.equals(sourceColumn.trim(), ignoreCase = true) }

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
                                        val pk = try { engine.getTableInfo(name).columns.firstOrNull { it.isPrimaryKey }?.name } catch (_: Exception) { null }
                                        targetColumn = pk ?: ""
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

                // Helper: Quick Foreign Key Column creation in source table
                if (sourceTable.isNotBlank() && targetTable.isNotBlank() && sourceTable != targetTable) {
                    val suggestedFkCol = "${targetTable.lowercase()}_id"
                    val sourceHasFk = sourceColumns.any {
                        it.name.equals(suggestedFkCol, ignoreCase = true) ||
                        it.name.equals("id_${targetTable.lowercase()}", ignoreCase = true)
                    }
                    if (!sourceHasFk) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val targetPk = targetColumns.firstOrNull { it.isPrimaryKey }
                                    val fkType = when (targetPk?.type) {
                                        ColumnType.AUTOINCREMENT, ColumnType.INTEGER, null -> ColumnType.INTEGER
                                        ColumnType.LONG -> ColumnType.LONG
                                        else -> targetPk.type
                                    }
                                    engine.addColumn(
                                        sourceTable,
                                        Column(
                                            name = suggestedFkCol,
                                            type = fkType,
                                            isNullable = true,
                                            isPrimaryKey = false,
                                            isAutoIncrement = false
                                        )
                                    )
                                    sourceColumnsRefreshKey++
                                    sourceColumn = suggestedFkCol
                                    successMessage = "Columna '$suggestedFkCol' creada en '$sourceTable' y seleccionada como FK"
                                } catch (e: Exception) {
                                    errorMessage = "Error al crear la columna: ${e.message}"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Crear y usar columna '$suggestedFkCol' en '$sourceTable' (Recomendado)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Warning if user selects a personal data column as foreign key
                if (isSuspectPersonalColumn) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Advertencia sobre '$sourceColumn'",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Has seleccionado una columna de datos personales ('$sourceColumn') como Clave Foránea. Las relaciones deben enlazar identificadores numéricos (ej. '${targetTable.lowercase().ifBlank { "tabla" }}_id'), no nombres ni teléfonos. Esto evita que tus datos se alteren o queden inaccesibles.",
                                    color = Color(0xFF78350F),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                val isSourceColPk = sourceColumns.firstOrNull { it.name.equals(sourceColumn, ignoreCase = true) }?.isPrimaryKey == true
                if (isSourceColPk && sourceColumn.isNotBlank()) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Advertencia: '$sourceColumn' es la Clave Primaria de '$sourceTable'",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Has seleccionado la Clave Primaria única de la tabla. Al ser la PK, cada fila de '$sourceTable' debe tener un ID único y no podrá asociarse a la misma opción que otra fila (ej. dos personas no podrán elegir el mismo curso ni cambiar de opción libremente al editar). Usa el botón de arriba para crear una columna foránea dedicada (ej. '${targetTable.lowercase().ifBlank { "tabla" }}_id').",
                                    color = Color(0xFF78350F),
                                    style = MaterialTheme.typography.bodySmall
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

                // ── Many-to-Many Guidance & Intermediate Table Creator ──
                if (relationType == RelationType.MANY_TO_MANY) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Relación Muchos a Muchos (N:M)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(
                                "En bases de datos relacionales, una relación N:M requiere una Tabla Intermedia asociativa que contenga las dos claves foráneas correspondientes hacia ambas entidades.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { showIntermediateDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (sourceTable.isNotBlank() && targetTable.isNotBlank() && sourceTable != targetTable)
                                        "Generar Tabla Intermedia ('${sourceTable.lowercase()}_${targetTable.lowercase()}')"
                                    else "Crear Tabla Intermedia N:M",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
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
                                val diag = com.dbstudio.cerebro.Cerebro.diagnoseError(e)
                                errorMessage = "${diag.title}: ${diag.friendlyExplanation}"
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

    if (showIntermediateDialog) {
        com.dbstudio.cerebro.ui.IntermediateTableDialog(
            engine = engine,
            availableTables = tableNames,
            initialTableA = sourceTable.ifBlank { null },
            initialTableB = targetTable.ifBlank { null },
            onDismiss = { showIntermediateDialog = false },
            onSuccess = { newTableName ->
                showIntermediateDialog = false
                relationshipsList = try { engine.getRelationships() } catch (_: Exception) { emptyList() }
                successMessage = "Tabla intermedia '$newTableName' creada exitosamente con sus claves foráneas."
                onCreated()
            }
        )
    }
}
