@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dbstudio.ui.theme.AppColors

/**
 * Interactive User Manual modal providing comprehensive documentation
 * of all DB Studio features, guidelines, and tips.
 */
@Composable
fun UserManualDialog(
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ManualCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTopics = remember(selectedCategory, searchQuery) {
        manualTopics.filter { topic ->
            (selectedCategory == ManualCategory.ALL || topic.category == selectedCategory) &&
                    (searchQuery.isBlank() ||
                            topic.title.contains(searchQuery.trim(), ignoreCase = true) ||
                            topic.content.contains(searchQuery.trim(), ignoreCase = true) ||
                            topic.tags.any { it.contains(searchQuery.trim(), ignoreCase = true) })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(850.dp)
                .height(650.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Dialog Header ───────────────────────────────────
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Manual de Usuario — DataBase Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Guía completa sobre el funcionamiento, diseño de tablas, datos y diagramas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                }

                // ── Search Bar & Filter Chips ───────────────────────
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        placeholder = { Text("Buscar en el manual (ej. relaciones, primary key, access, fecha)...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(8.dp))

                    // Category Tabs / Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ManualCategory.entries.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(cat.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Topics Content ──────────────────────────────────
                val contentScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(contentScroll)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (filteredTopics.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(8.dp))
                                Text("No se encontraron temas con esa búsqueda", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        filteredTopics.forEach { topic ->
                            TopicCard(topic)
                        }
                    }
                }

                // ── Footer ──────────────────────────────────────────
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DataBase Studio v2.0 — SQLite & Microsoft Access Manager",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Entendido / Cerrar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicCard(topic: ManualTopic) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(topic.category.color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(topic.category.icon, contentDescription = null, tint = topic.category.color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    topic.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    color = topic.category.color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        topic.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = topic.category.color,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                topic.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )

            if (topic.keyPoints.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    topic.keyPoints.forEach { point ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", color = topic.category.color, fontWeight = FontWeight.Bold)
                            Text(point, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

// ── Categories & Data ────────────────────────────────────────────────

enum class ManualCategory(val displayName: String, val icon: ImageVector, val color: Color) {
    ALL("Todos", Icons.Default.AllInclusive, Color(0xFF6366F1)),
    BASICS("General y Métricas", Icons.Default.Dashboard, Color(0xFF3B82F6)),
    SQL_CONSOLE("Consola SQL", Icons.Default.Terminal, Color(0xFF0EA5E9)),
    TABLE_DESIGNER("Diseñador de Tablas", Icons.Default.TableChart, Color(0xFF8B5CF6)),
    DATA_VIEW("Vista de Datos", Icons.Default.ViewList, Color(0xFF10B981)),
    DIAGRAM("Diagrama ER", Icons.Default.AccountTree, Color(0xFFF59E0B)),
    COMMANDS_THEME("Atajos y Tema", Icons.Default.Bolt, Color(0xFFEC4899))
}

data class ManualTopic(
    val title: String,
    val category: ManualCategory,
    val content: String,
    val keyPoints: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

val manualTopics = listOf(
    ManualTopic(
        title = "Inicio Rápido: Crear y Abrir Bases de Datos",
        category = ManualCategory.BASICS,
        content = "DB Studio permite gestionar bases de datos relacionales en dos formatos populares: SQLite (.db) y Microsoft Access (.accdb). No necesitas software adicional instalado en tu equipo.",
        keyPoints = listOf(
            "Archivo > Nueva Base de Datos: Ingresa el nombre, elige el tipo (SQLite o Access) y selecciona la carpeta donde guardarla.",
            "Archivo > Abrir Base de Datos: Selecciona cualquier archivo .db, .sqlite, .accdb o .mdb de tu disco.",
            "El panel lateral mostrará inmediatamente el estado 'Conectado' y la lista de tablas disponibles."
        ),
        tags = listOf("conectar", "crear", "abrir", "sqlite", "access", "accdb", "db")
    ),
    ManualTopic(
        title = "Panel Lateral: Gestión Ágil y Modos de Trabajo",
        category = ManualCategory.BASICS,
        content = "El panel lateral izquierdo es el centro neurálgico para navegar entre los 5 modos de trabajo y administrar tus tablas de forma amigable e intuitiva.",
        keyPoints = listOf(
            "Modos de Trabajo: Accede con un solo clic a 'Panel General', 'Diseñador de Tablas', 'Vista de Datos', 'Consola SQL' y 'Diagrama ER'.",
            "Botón '+ Nueva Tabla': Crea una tabla con su clave primaria preconfigurada al instante.",
            "Buscador de tablas: Filtra instantáneamente entre decenas de tablas por nombre.",
            "Menú de acciones por tabla (⋮): Ver Datos, Diseñar Estructura, Renombrar o Eliminar sin cambiar de contexto.",
            "Acceso directo a Relaciones: Crea o gestiona claves foráneas rápidamente con el botón de enlace."
        ),
        tags = listOf("panel", "sidebar", "tabla", "renombrar", "eliminar", "buscar", "modos")
    ),
    ManualTopic(
        title = "Panel General: Métricas KPI y Salud del Esquema (Health Check)",
        category = ManualCategory.BASICS,
        content = "El Panel General ofrece un tablero ejecutivo estilo Google Cloud con analítica en tiempo real de tu base de datos y auditoría de calidad de esquema.",
        keyPoints = listOf(
            "Tarjetas KPI: Muestra el total de Tablas, Registros globales almacenados, Relaciones activas y Tamaño del archivo en disco.",
            "Score de Salud del Esquema: Evalúa la solidez de tu arquitectura relacional (0-100%) detectando tablas sin Clave Primaria o claves foráneas huérfanas.",
            "Listado de Problemas y Sugerencias: Te advierte si alguna tabla carece de PK para evitar problemas de duplicidad o rendimiento.",
            "Explorador Rápido de Tablas: Consulta el conteo de registros y campos de cada tabla con botones directos para 'Datos' y 'Diseño'."
        ),
        tags = listOf("overview", "dashboard", "metricas", "kpi", "salud", "health", "auditoria", "tamano")
    ),
    ManualTopic(
        title = "Consola y Editor SQL Avanzado (Estilo BigQuery)",
        category = ManualCategory.SQL_CONSOLE,
        content = "Ejecuta consultas relacionales arbitrarias en tiempo real con una interfaz inspirada en BigQuery y Cloud SQL Studio.",
        keyPoints = listOf(
            "Editor Monospace: Escribe cualquier sentencia SQL (SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER).",
            "Atajo Ctrl + Enter: Ejecuta la consulta seleccionada al instante sin usar el ratón.",
            "Chips de Plantillas Rápidas: Inserta con un clic plantillas de SELECT *, COUNT(*), INNER JOIN y CREATE TABLE.",
            "Métricas de Ejecución: Visualiza el tiempo exacto en milisegundos (ms) y la cantidad de filas afectadas o devueltas.",
            "Historial de Consultas: Reutiliza consultas ejecutadas anteriormente desde la barra lateral de historial.",
            "Exportar Resultado a CSV: Descarga la cuadrícula resultante directamente en un archivo .csv listo para Excel."
        ),
        tags = listOf("sql", "query", "consola", "bigquery", "select", "join", "tiempo", "historial", "csv")
    ),
    ManualTopic(
        title = "Paleta de Comandos Universal (Ctrl + K / F1)",
        category = ManualCategory.COMMANDS_THEME,
        content = "Inspirada en el Omnibox de Google Chrome y Command Palette de VS Code, permite controlar toda la aplicación a la velocidad del pensamiento.",
        keyPoints = listOf(
            "Abrir Paleta: Presiona Ctrl + K o F1 desde cualquier pantalla de DB Studio.",
            "Salto Directo: Escribe 'panel', 'diseñador', 'datos', 'consola' o 'diagrama' para cambiar de modo al instante.",
            "Ir a Tabla: Escribe el nombre de cualquier tabla para saltar de inmediato a sus registros.",
            "Acciones Globales: Crear nueva tabla, alternar Modo Oscuro/Claro o exportar sin abrir menús.",
            "Navegación con teclado: Usa las flechas Arriba/Abajo y pulsa Enter para ejecutar la acción."
        ),
        tags = listOf("comandos", "ctrl+k", "f1", "omnibox", "atajo", "buscar", "paleta")
    ),
    ManualTopic(
        title = "Tema Oscuro / Claro Profesional (Material 3)",
        category = ManualCategory.COMMANDS_THEME,
        content = "DB Studio incorpora un sistema de temas Google Material 3 de alta fidelidad, con contrastes calibrados para reducir la fatiga visual.",
        keyPoints = listOf(
            "Botón de Sol / Luna: Ubicado en la barra superior derecha para alternar en un clic.",
            "Paleta Tonal Google Dark: Tonos profundos de gris grafito con acentos azul índigo para máxima legibilidad.",
            "Persistencia instantánea: Los colores de cuadrículas, diagramas, editores y menús se sincronizan de inmediato."
        ),
        tags = listOf("tema", "dark", "oscuro", "claro", "material3", "modo oscuro")
    ),
    ManualTopic(
        title = "Diseñador de Tablas: Columnas y Atributos",
        category = ManualCategory.TABLE_DESIGNER,
        content = "El Diseñador de Tablas proporciona una interfaz visual profesional estilo MySQL para estructurar campos sin escribir código SQL manualmente.",
        keyPoints = listOf(
            "PK (Clave Primaria): Identificador único obligatorio para identificar cada registro (ej. id).",
            "NN (No Nulo): Garantiza que el campo no pueda quedar vacío.",
            "UQ (Único): Asegura que no existan valores duplicados en esa columna (ej. cédula, correo).",
            "AI (Autoincremental): El valor numérico se incrementa automáticamente (#1, #2, #3...).",
            "Predeterminado: Define un valor por defecto para nuevos registros (ej. CURRENT_TIMESTAMP, 0, 'Activo')."
        ),
        tags = listOf("pk", "nn", "uq", "ai", "primary key", "unique", "not null", "autoincrement", "columnas")
    ),
    ManualTopic(
        title = "Deshacer y Rehacer en el Diseñador (Ctrl + Z / Ctrl + Y)",
        category = ManualCategory.TABLE_DESIGNER,
        content = "Historial completo de modificaciones en la estructura de la tabla para diseñar con total seguridad y confianza.",
        keyPoints = listOf(
            "Deshacer (Ctrl + Z): Revierte cambios accidentales en nombres de columnas, tipos de datos o eliminación de campos.",
            "Rehacer (Ctrl + Y / Ctrl + Shift + Z): Restaura modificaciones deshechas previamente.",
            "Botones en la Barra de Herramientas: Íconos visuales de Deshacer y Rehacer con estado activo/inactivo."
        ),
        tags = listOf("deshacer", "rehacer", "undo", "redo", "ctrl+z", "ctrl+y", "historial")
    ),
    ManualTopic(
        title = "Plantillas Rápidas de Campos y Preservación de Datos",
        category = ManualCategory.TABLE_DESIGNER,
        content = "Agrega campos comunes con un solo clic utilizando las plantillas rápidas sobre la cuadrícula:",
        keyPoints = listOf(
            "+ Cédula: Campo numérico de tipo LONG con restricción de unicidad.",
            "+ Nombre y + Apellido: Campos de texto estándar para identificación de personas.",
            "+ Correo: Campo de texto VARCHAR(150).",
            "+ Celular: Campo de teléfono móvil VARCHAR(20).",
            "+ Fecha Creación: Campo de Fecha y Hora con valor predeterminado CURRENT_TIMESTAMP.",
            "Preservación de datos: Al hacer clic en 'Guardar Esquema', los datos ya existentes en la tabla se preservan automáticamente."
        ),
        tags = listOf("plantillas", "cedula", "nombre", "apellido", "correo", "celular", "fecha_creacion", "presets")
    ),
    ManualTopic(
        title = "Vista de Datos: Inserción, Edición y Exportación CSV/JSON",
        category = ManualCategory.DATA_VIEW,
        content = "Visualiza y manipula los registros almacenados en cada tabla como una hoja de cálculo moderna y exporta universalmente.",
        keyPoints = listOf(
            "+ Añadir Registro: Formulario para insertar una nueva fila con validación de tipos.",
            "+ Generar Datos: Crea registros de prueba realistas (nombres, correos, cédulas) automáticamente.",
            "Exportar a CSV y JSON: Menú desplegable 'Exportar' para descargar los datos de la tabla en formato CSV o JSON estructurado.",
            "Edición en línea: Haz clic en el botón de lápiz de cualquier fila para editar sus valores.",
            "Sincronización silenciosa: Los datos y claves foráneas se actualizan automáticamente en segundo plano sin parpadeos ni bloqueos."
        ),
        tags = listOf("datos", "insertar", "editar", "eliminar", "registros", "exportar csv", "exportar json")
    ),
    ManualTopic(
        title = "Diagrama Entidad-Relación (ER): Movimiento Libre y Navegación",
        category = ManualCategory.DIAGRAM,
        content = "Mapa interactivo de tus tablas y relaciones con libertad total de manipulación y exploración gráfica.",
        keyPoints = listOf(
            "Movimiento 100% Libre: Haz clic y arrastra cualquier tabla por el lienzo para organizarlas libremente, incluso si tienen relaciones activas.",
            "Líneas dinámicas: Las líneas conectoras se recalculan y adaptan en tiempo real mientras mueves las entidades.",
            "Zoom y Centrado: Usa los botones de acercar (+), alejar (-) o centrar para la mejor panorámica."
        ),
        tags = listOf("diagrama", "er", "arrastrar", "mover", "lienzo", "zoom")
    ),
    ManualTopic(
        title = "Diagrama ER: Crear, Editar y Eliminar Relaciones",
        category = ManualCategory.DIAGRAM,
        content = "Establece y modifica restricciones de clave foránea directamente en el diagrama:",
        keyPoints = listOf(
            "Crear Relación: Haz clic en 'Nueva Relación', elige la Tabla y Columna Origen (FK), y la Tabla y Columna Destino (PK).",
            "Selección interactiva: Haz clic sobre cualquier línea o conector en el diagrama; se resaltará y mostrará una tarjeta flotante con botones directos para 'Editar' o 'Eliminar'.",
            "Gestionar Relaciones: Modifica sus reglas de integridad referencial (CASCADE, SET NULL, RESTRICT) o bórralas con confirmación."
        ),
        tags = listOf("relaciones", "foreign key", "fk", "editar relacion", "eliminar relacion", "cascade")
    ),
    ManualTopic(
        title = "Relaciones Seguras: Claves Foráneas (FK) y Protección de Datos",
        category = ManualCategory.DIAGRAM,
        content = "Aprende a vincular tablas correctamente mediante Claves Foráneas sin comprometer tus datos personales ni generar inconsistencias:",
        keyPoints = listOf(
            "Uso de Identificadores (IDs): Una Clave Foránea (FK) debe almacenar el identificador de la otra tabla (ej. 'cliente_id' o 'id_cliente' que apunta a 'clientes.id').",
            "No usar columnas personales como FK: Nunca uses campos como 'nombre', 'celular', 'correo' o 'cédula' como clave foránea; esos campos contienen información descriptiva personal y no deben reemplazarse por IDs.",
            "Botón de Creación Rápida de FK: En el diálogo de relaciones, si tu tabla no tiene una columna de vínculo, DB Studio te ofrece el botón directo 'Crear y usar columna [tabla]_id' para generarla al instante con el tipo correcto.",
            "Advertencias Automáticas: El sistema te alertará en color amarillo si seleccionas accidentalmente una columna de datos personales como clave foránea.",
            "Preservación Total de Datos: Al crear o modificar relaciones y esquemas, DB Studio preserva el 100% de los registros existentes sin importar mayúsculas o minúsculas en los nombres de columnas."
        ),
        tags = listOf("relaciones", "foreign key", "fk", "clave foranea", "nombre", "celular", "seguridad", "preservacion", "datos")
    ),
    ManualTopic(
        title = "Exportación Completa a Microsoft Excel (.xlsx)",
        category = ManualCategory.DATA_VIEW,
        content = "Genera un libro de cálculo Microsoft Excel (.xlsx) nativo que reúne la base de datos completa con una pestaña u hoja independiente para cada tabla existente.",
        keyPoints = listOf(
            "Botón en Menú Lateral: Haz clic en el botón verde 'Exportar a Excel (.xlsx)' ubicado directamente en la barra lateral izquierda.",
            "Multi-Hoja Automático: Cada tabla de la base de datos se guarda en su propia hoja/pestaña dentro de un único archivo .xlsx.",
            "Formato y Estilos Profesionales: Fila 1 destacada con encabezados en color verde esmeralda y texto blanco; auto-ajuste de ancho de columnas y tipos de datos preservados (números, booleanos, textos).",
            "Accesos Alternativos: Disponible también en el Menú Archivo > Exportar como Excel, en el Diálogo de Exportación y en la Paleta de Comandos (Ctrl + K).",
            "Compatibilidad Universal: Archivos 100% compatibles con Microsoft Excel, Google Sheets, LibreOffice Calc y Apple Numbers."
        ),
        tags = listOf("excel", "xlsx", "exportar", "hoja de calculo", "tablas", "reporte", "descargar", "sheets")
    ),
    ManualTopic(
        title = "Exportación Global y Copias de Seguridad",
        category = ManualCategory.COMMANDS_THEME,
        content = "Herramientas integradas para interoperabilidad y migración de tus bases de datos:",
        keyPoints = listOf(
            "Exportar a Excel (.xlsx): Exporta todas las tablas a un solo libro con una pestaña por cada tabla desde el botón del menú lateral.",
            "Exportar como SQL: Menú Archivo > Exportar como SQL para generar el archivo .sql completo con sentencias DDL e INSERT.",
            "Exportar como Access: Menú Archivo > Exportar como Access (.accdb) para convertir una base de datos SQLite directamente al formato de Microsoft Access.",
            "Guardado automático: Las modificaciones en SQLite y Access se guardan de forma permanente e inmediata al confirmar las operaciones."
        ),
        tags = listOf("exportar", "excel", "xlsx", "sql", "guardar", "backup", "migracion", "access")
    )
)
