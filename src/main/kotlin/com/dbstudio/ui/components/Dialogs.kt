@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dbstudio.model.*
import com.dbstudio.ui.theme.AppColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ── New Database Dialog ─────────────────────────────────────────────

@Composable
fun NewDatabaseDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: DatabaseType, directory: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DatabaseType.SQLITE) }
    var directory by remember { mutableStateOf(System.getProperty("user.home")) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Nueva Base de Datos", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.width(420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isBlank()) "El nombre es obligatorio" else null
                    },
                    label = { Text("Nombre de la Base de Datos") },
                    placeholder = { Text("ej. MiBaseDeDatos") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type selector
                Text("Tipo de Motor:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DatabaseType.entries.forEach { dbType ->
                        FilterChip(
                            selected = type == dbType,
                            onClick = { type = dbType },
                            label = { Text(dbType.displayName) },
                            leadingIcon = {
                                if (type == dbType) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }

                // Directory
                OutlinedTextField(
                    value = directory,
                    onValueChange = { directory = it },
                    label = { Text("Ubicación de guardado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = {
                            FileChooser.chooseDirectory("Seleccionar Carpeta")?.let {
                                directory = it
                            }
                        }) {
                            Text("Examinar")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "El nombre es obligatorio"
                    } else {
                        onCreate(name.trim(), type, directory)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Crear Base de Datos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── New / Rename Table Dialog ───────────────────────────────────────

@Composable
fun TableNameDialog(
    title: String,
    initialName: String = "",
    existingNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = when {
                        it.isBlank() -> "El nombre de la tabla es obligatorio"
                        it.equals(initialName, ignoreCase = true) -> null
                        existingNames.any { n -> n.equals(it, ignoreCase = true) } -> "La tabla '$it' ya existe"
                        else -> null
                    }
                },
                label = { Text("Nombre de la Tabla") },
                placeholder = { Text("ej. estudiantes, materias, usuarios") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "El nombre de la tabla es obligatorio"
                    } else if (existingNames.any { it.equals(name, ignoreCase = true) } && !name.equals(initialName, ignoreCase = true)) {
                        error = "La tabla '$name' ya existe"
                    } else {
                        onConfirm(name.trim())
                    }
                },
                enabled = name.isNotBlank() && error == null
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Confirm Dialog ──────────────────────────────────────────────────

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Eliminar",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Row Edit / Insert Form Dialog ────────────────────────────────────

@Composable
fun RowEditDialog(
    title: String,
    columns: List<Column>,
    initialValues: Map<String, Any?> = emptyMap(),
    foreignKeyOptions: Map<String, List<Pair<String, String>>> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (Map<String, String?>) -> Unit
) {
    val isEditing = initialValues.isNotEmpty()
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            columns.forEach { col ->
                val init = initialValues[col.name]?.toString() ?: ""
                put(col.name, init)
            }
        }
    }

    // Auto-fill creation timestamp if empty
    LaunchedEffect(Unit) {
        columns.forEach { col ->
            val colLower = col.name.lowercase()
            if ((col.type == ColumnType.DATETIME || colLower.contains("creacion") || colLower.contains("fecha")) &&
                values[col.name].isNullOrBlank() && !isEditing
            ) {
                values[col.name] = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isEditing) Icons.Default.EditNote else Icons.Default.PostAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .width(520.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isEditing) "Modifica los datos del registro en el formulario:"
                    else "Completa los campos del formulario para registrar una nueva fila:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                columns.forEach { col ->
                    val isAutoInc = col.isAutoIncrement
                    val colNameLower = col.name.lowercase()
                    val fkOpts = foreignKeyOptions[col.name]

                    if (isAutoInc) {
                        // ID Autoincremental badge / box
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = AppColors.primaryKey, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "${col.name} (Clave Primaria)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isEditing) "ID actual: #${values[col.name]}"
                                        else "Autoincremental (se asignará automáticamente al guardar)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (fkOpts != null) {
                        // Related Table Column -> Dropdown
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        val currentKey = values[col.name]
                        val selectedLabel = fkOpts.find { it.first == currentKey }?.second
                            ?: if (currentKey.isNullOrBlank()) "" else "ID: $currentKey"

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("${col.name} (🔗 Seleccionar de tabla relacionada)") },
                                placeholder = { Text("Haz clic para elegir...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = AppColors.foreignKey)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                supportingText = {
                                    if (fkOpts.isEmpty()) {
                                        Text("⚠️ No hay registros en la tabla relacionada. Registra datos en ella primero.")
                                    } else {
                                        Text("Opciones disponibles: ${fkOpts.size}")
                                    }
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                if (col.isNullable) {
                                    DropdownMenuItem(
                                        text = { Text("— Ninguno (Vacío) —", color = MaterialTheme.colorScheme.outline) },
                                        onClick = {
                                            values[col.name] = ""
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                                fkOpts.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(8.dp))
                                                Text(label)
                                            }
                                        },
                                        onClick = {
                                            values[col.name] = key
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (colNameLower.contains("cedula") || colNameLower.contains("dni") || colNameLower.contains("identificacion")) {
                        // Cédula: Numeric only
                        OutlinedTextField(
                            value = values[col.name] ?: "",
                            onValueChange = { input ->
                                values[col.name] = input.filter { it.isDigit() }
                            },
                            label = { Text("${col.name} (Numérico / Cédula)") },
                            placeholder = { Text("ej. 1020304050") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (col.type == ColumnType.DATETIME || colNameLower.contains("fecha") || colNameLower.contains("creacion")) {
                        // Date / DateTime field
                        OutlinedTextField(
                            value = values[col.name] ?: "",
                            onValueChange = { values[col.name] = it },
                            label = { Text("${col.name} (Fecha y Hora)") },
                            placeholder = { Text("YYYY-MM-DD HH:MM:SS") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                TextButton(onClick = {
                                    values[col.name] = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                }) {
                                    Text("Ahora")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (colNameLower.contains("email") || colNameLower.contains("correo")) {
                        // Email field
                        OutlinedTextField(
                            value = values[col.name] ?: "",
                            onValueChange = { values[col.name] = it },
                            label = { Text("${col.name} (Correo)") },
                            placeholder = { Text("ejemplo@correo.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (colNameLower.contains("nombre") || colNameLower.contains("apellido") || colNameLower.contains("persona")) {
                        // Name field
                        OutlinedTextField(
                            value = values[col.name] ?: "",
                            onValueChange = { values[col.name] = it },
                            label = { Text("${col.name} (Texto)") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // General text / number field
                        OutlinedTextField(
                            value = values[col.name] ?: "",
                            onValueChange = { values[col.name] = it },
                            label = {
                                val suffix = buildString {
                                    append(col.type.displayName)
                                    if (!col.isNullable) append(" *Requerido")
                                }
                                Text("${col.name} ($suffix)")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val result = mutableMapOf<String, String?>()
                columns.forEach { col ->
                    if (!col.isAutoIncrement || isEditing) {
                        val v = values[col.name]
                        result[col.name] = if (v.isNullOrBlank()) null else v.trim()
                    }
                }
                onSave(result)
            }) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isEditing) "Actualizar Registro" else "Guardar en Tabla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Column Edit Dialog ──────────────────────────────────────────────

@Composable
fun ColumnEditDialog(
    title: String,
    initialColumn: Column? = null,
    onDismiss: () -> Unit,
    onSave: (Column) -> Unit
) {
    var name by remember { mutableStateOf(initialColumn?.name ?: "") }
    var type by remember { mutableStateOf(initialColumn?.type ?: ColumnType.TEXT) }
    var isPK by remember { mutableStateOf(initialColumn?.isPrimaryKey ?: false) }
    var isAutoInc by remember { mutableStateOf(initialColumn?.isAutoIncrement ?: false) }
    var isNullable by remember { mutableStateOf(initialColumn?.isNullable ?: true) }
    var isUnique by remember { mutableStateOf(initialColumn?.isUnique ?: false) }
    var defaultValue by remember { mutableStateOf(initialColumn?.defaultValue ?: "") }
    var length by remember { mutableStateOf(initialColumn?.length?.toString() ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ViewColumn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.width(440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Preset chips
                if (initialColumn == null) {
                    Text("Plantillas Rápidas:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Cédula" to ColumnPresets.cedula(),
                            "Nombre" to ColumnPresets.nombre(),
                            "Apellido" to ColumnPresets.apellido(),
                            "Correo" to ColumnPresets.correo(),
                            "Fecha" to ColumnPresets.fechaCreacion()
                        ).forEach { (label, preset) ->
                            SuggestionChip(
                                onClick = {
                                    name = preset.name
                                    type = preset.type
                                    isPK = preset.isPrimaryKey
                                    isAutoInc = preset.isAutoIncrement
                                    isNullable = preset.isNullable
                                    isUnique = preset.isUnique
                                    defaultValue = preset.defaultValue ?: ""
                                    length = preset.length?.toString() ?: ""
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isBlank()) "El nombre de la columna es obligatorio" else null
                    },
                    label = { Text("Nombre de la Columna") },
                    placeholder = { Text("ej. cedula, nombre, materia_id") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = type.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Dato") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        ColumnType.entries.forEach { ct ->
                            DropdownMenuItem(
                                text = { Text(ct.displayName) },
                                onClick = {
                                    type = ct
                                    if (ct == ColumnType.AUTOINCREMENT) {
                                        isPK = true
                                        isAutoInc = true
                                        isNullable = false
                                    }
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Length (for VARCHAR)
                if (type == ColumnType.VARCHAR || type == ColumnType.TEXT) {
                    OutlinedTextField(
                        value = length,
                        onValueChange = { length = it.filter { c -> c.isDigit() } },
                        label = { Text("Longitud Máxima (opcional)") },
                        placeholder = { Text("ej. 100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Checkboxes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPK, onCheckedChange = {
                        isPK = it
                        if (it) isNullable = false
                    })
                    Text("Clave Primaria (PK)", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAutoInc, onCheckedChange = {
                        isAutoInc = it
                        if (it) {
                            isPK = true
                            isNullable = false
                            type = ColumnType.AUTOINCREMENT
                        }
                    })
                    Text("Autoincremental (ID)", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isNullable, onCheckedChange = {
                        if (!isPK) isNullable = it
                    }, enabled = !isPK)
                    Text("Permite Nulos (Opcional)", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isUnique, onCheckedChange = { isUnique = it })
                    Text("Valor Único (Unique)", style = MaterialTheme.typography.bodyMedium)
                }

                // Default value
                if (!isAutoInc) {
                    OutlinedTextField(
                        value = defaultValue,
                        onValueChange = { defaultValue = it },
                        label = { Text("Valor por Defecto (opcional)") },
                        placeholder = { Text("ej. CURRENT_TIMESTAMP, 0, 'Activo'") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "El nombre de la columna es obligatorio"
                        return@Button
                    }
                    onSave(
                        Column(
                            name = name.trim().lowercase().replace(" ", "_"),
                            type = type,
                            isPrimaryKey = isPK,
                            isAutoIncrement = isAutoInc,
                            isNullable = isNullable,
                            isUnique = isUnique,
                            defaultValue = defaultValue.ifBlank { null },
                            length = length.toIntOrNull()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar Columna")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Export Dialog ────────────────────────────────────────────────────

@Composable
fun ExportDialog(
    databaseName: String,
    onDismiss: () -> Unit,
    onExportSql: (String) -> Unit,
    onExportAccdb: (String) -> Unit
) {
    var exportType by remember { mutableStateOf("sql") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Exportar Base de Datos", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Selecciona el formato de exportación:")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = exportType == "sql",
                        onClick = { exportType = "sql" },
                        label = { Text("Script SQL (.sql)") }
                    )
                    FilterChip(
                        selected = exportType == "accdb",
                        onClick = { exportType = "accdb" },
                        label = { Text("Microsoft Access (.accdb)") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (exportType == "sql") {
                    val path = FileChooser.saveFile(
                        title = "Exportar como SQL",
                        defaultName = "$databaseName.sql",
                        extension = "sql",
                        description = "Archivos SQL (*.sql)"
                    )
                    if (path != null) onExportSql(path)
                } else {
                    val path = FileChooser.saveFile(
                        title = "Exportar como Access",
                        defaultName = "$databaseName.accdb",
                        extension = "accdb",
                        description = "Base de datos Access (*.accdb)"
                    )
                    if (path != null) onExportAccdb(path)
                }
            }) {
                Text("Exportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Generate / Fill Data Dialog ──────────────────────────────────────

@Composable
fun GenerateDataDialog(
    tableName: String,
    onDismiss: () -> Unit,
    onGenerate: (count: Int) -> Unit
) {
    var countText by remember { mutableStateOf("10") }
    var selectedPreset by remember { mutableStateOf(10) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Llenar Tabla con Datos", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Genera y llena registros automáticamente para la tabla '$tableName' respetando los tipos de columnas configurados y sus relaciones:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Cantidad de registros a insertar:", style = MaterialTheme.typography.labelMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 25, 50, 100).forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset && countText == preset.toString(),
                            onClick = {
                                selectedPreset = preset
                                countText = preset.toString()
                            },
                            label = { Text("$preset filas") }
                        )
                    }
                }

                OutlinedTextField(
                    value = countText,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }
                        countText = filtered
                        selectedPreset = filtered.toIntOrNull() ?: 0
                    },
                    label = { Text("O escribe una cantidad personalizada") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = countText.toIntOrNull() ?: 10
                    if (count > 0) {
                        onGenerate(count.coerceIn(1, 1000))
                    }
                },
                enabled = (countText.toIntOrNull() ?: 0) > 0
            ) {
                Text("Llenar Tabla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}


