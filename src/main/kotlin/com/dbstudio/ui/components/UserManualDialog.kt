@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
                                    Icons.AutoMirrored.Filled.MenuBook,
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
                    SearchBarField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Buscar en el manual (ej. relaciones, primary key, access, fecha)...",
                        modifier = Modifier.fillMaxWidth()
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
                            "DataBase Studio v3.0 — SQLite & Microsoft Access Manager",
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
    RIBBON("Cinta Office", Icons.Default.ViewStream, Color(0xFF16A34A)),
    BASICS("General y Métricas", Icons.Default.Dashboard, Color(0xFF3B82F6)),
    CEREBRO("Cerebro Asistente", Icons.Default.Psychology, Color(0xFF9333EA)),
    TABLE_DESIGNER("Diseñador de Tablas", Icons.Default.TableChart, Color(0xFF8B5CF6)),
    DATA_VIEW("Vista de Datos", Icons.AutoMirrored.Filled.ViewList, Color(0xFF10B981)),
    SQL_CONSOLE("Consola SQL", Icons.Default.Terminal, Color(0xFF0EA5E9)),
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
        title = "Cinta de Opciones Superior (Estilo Excel / Access / Word)",
        category = ManualCategory.RIBBON,
        content = "DB Studio adopta una barra de herramientas horizontal superior inspirada en Microsoft Office, eliminando los paneles laterales invasivos y otorgando el 100% del ancho de la pantalla a tus datos y diagramas.",
        keyPoints = listOf(
            "Pestañas Temáticas: 'Archivo' (gestión de archivos y copias), 'Inicio' (modos de trabajo y tabla activa), 'Insertar' (nuevas entidades, campos y registros), 'Datos' (consultas avanzadas e intercambio) y 'Vista' (diagramas y aspecto).",
            "Selector Rápido de Tabla: Menú desplegable en la barra superior que muestra la tabla en uso, su cantidad de registros y permite cambiar entre tablas al instante desde cualquier pantalla.",
            "Contraer / Expandir Cinta: Haz clic en la flecha (^ / v) en la esquina superior derecha para ocultar o mostrar los botones de la cinta y maximizar la altura de trabajo.",
            "Pantalla 100% Libre: Al no haber barras laterales ocupando espacio, las tablas y cuadrículas se despliegan con máxima amplitud sin recortar ni deformar botones."
        ),
        tags = listOf("cinta", "ribbon", "office", "excel", "access", "barra", "pestanas", "inicio", "archivo", "insertar", "datos", "vista", "contraer")
    ),
    ManualTopic(
        title = "Herramienta Insertar: Campo Personalizado en la Cinta",
        category = ManualCategory.RIBBON,
        content = "Agrega nuevas columnas a la estructura de tu tabla directamente desde la barra de herramientas superior:",
        keyPoints = listOf(
            "Ubicación en la Cinta: Dirígete a la pestaña 'Insertar' > grupo 'Estructura' > botón 'Campo Personalizado'.",
            "Navegación Automática: Si estás en otra vista, al hacer clic en 'Campo Personalizado' la aplicación salta automáticamente al Diseñador de Tablas y despliega el formulario de creación.",
            "Configuración Completa: Define el nombre del campo, tipo de dato, Clave Primaria (PK), No Nulo (NN), Único (UQ), Autoincremental (AI) y valor predeterminado.",
            "Acceso en el Diseñador: También disponible mediante el botón 'Añadir Campo' en el encabezado del diseñador."
        ),
        tags = listOf("insertar", "campo personalizado", "nueva columna", "columna", "estructura", "disenador", "ribbon")
    ),
    ManualTopic(
        title = "Inicio Rápido: Crear, Abrir y Bases de Datos Recientes",
        category = ManualCategory.BASICS,
        content = "Gestiona bases de datos relacionales locales en formato SQLite (.db, .sqlite) o Microsoft Access (.accdb, .mdb) sin requerir instalaciones pesadas de servidores.",
        keyPoints = listOf(
            "Archivo > Nueva BD: Especifica el nombre de la base de datos, el tipo de motor (SQLite o Access) y el directorio de destino.",
            "Archivo > Abrir BD: Abre cualquier archivo de base de datos existente en tu equipo.",
            "Archivo > Recientes: Historial persistente que recuerda las bases de datos abiertas anteriormente para saltar entre proyectos en 1 solo clic.",
            "Píldora de Estado: El encabezado superior muestra el nombre del archivo activo con un indicador verde y el tipo de motor."
        ),
        tags = listOf("conectar", "crear", "abrir", "sqlite", "access", "accdb", "db", "recientes", "historial")
    ),
    ManualTopic(
        title = "Copias de Respaldo Inmediatas (Snapshots con Fecha y Hora)",
        category = ManualCategory.BASICS,
        content = "Genera copias de seguridad de tu base de datos con un solo clic para proteger tu trabajo antes de realizar cambios estructurales masivos:",
        keyPoints = listOf(
            "1 Clic en 'Copia Respaldo': Disponible en la pestaña 'Archivo' y en 'Inicio > Operaciones Rápidas'.",
            "Nomenclatura Automática: Se genera un archivo snapshot nombrado con el patrón '[Nombre]_backup_YYYYMMDD_HHmmss.[ext]' en el mismo directorio.",
            "No Interrumpe la Sesión: La copia se crea en milisegundos en segundo plano manteniendo la base de datos activa y en línea.",
            "Seguridad Total: Ideal para respaldar antes de eliminar tablas, ejecutar consultas UPDATE/DELETE masivas o alterar relaciones."
        ),
        tags = listOf("backup", "respaldo", "copia", "seguridad", "snapshot", "archivo", "recuperacion")
    ),
    ManualTopic(
        title = "Cerebro: Asistente Local y Tablas Intermedias Muchos a Muchos (N:M)",
        category = ManualCategory.CEREBRO,
        content = "Cerebro es el motor de inteligencia local de DB Studio (100% offline, sin APIs externas) que te asiste en el diseño relacional y previene errores:",
        keyPoints = listOf(
            "Tablas Intermedias N:M: Al relacionar entidades de muchos a muchos (ej. Estudiantes y Semilleros), la herramienta 'Insertar > Tabla N:M (Intermedia)' crea automáticamente la tabla asociativa con ambas claves foráneas compuestas.",
            "Doctor Cerebro: Auditoría relacional que analiza tu base de datos detectando tablas sin PK, índices faltantes y claves huérfanas con sugerencias de remediación.",
            "Diagnóstico Inteligente de Errores: Intercepta fallos crípticos de SQLite y Access (claves duplicadas, violaciones de FK, bloqueos de archivos) y muestra explicaciones claras en lenguaje natural.",
            "Inferencia de Tipos en Tiempo Real: Sugiere automáticamente el tipo de columna ideal al tipear su nombre (ej. 'correo' -> VARCHAR, 'precio' -> DECIMAL, 'activo' -> BOOLEAN)."
        ),
        tags = listOf("cerebro", "asistente", "muchos a muchos", "intermedia", "n:m", "doctor", "diagnostico", "heuristica", "offline")
    ),
    ManualTopic(
        title = "Diseñador de Tablas: Cuadrícula MySQL y Previsualización SQL en Vivo",
        category = ManualCategory.TABLE_DESIGNER,
        content = "Estructura las columnas de tus tablas con precisión visual profesional y previsualiza el código DDL generado en tiempo real:",
        keyPoints = listOf(
            "Cuadrícula Estilo MySQL: Modifica Nombre, Tipo de Dato, PK, NN, UQ, AI y Valor Predeterminado en una tabla interactiva.",
            "Botón 'Vista SQL': Activa una consola inferior que muestra en vivo la sentencia CREATE TABLE o ALTER TABLE correspondiente a tu diseño, con botón para copiar al portapapeles.",
            "Plantillas Rápidas de Columnas: Inserta con 1 clic campos estándar (+ID, +Cédula, +Nombre, +Apellido, +Correo, +Celular, +Teléfono, +Fecha Creación, +Observaciones, +Descripción).",
            "Preservación de Datos: Al guardar cambios de esquema, DB Studio migra y preserva el 100% de los datos preexistentes en la tabla.",
            "Deshacer y Rehacer (Ctrl+Z / Ctrl+Y): Historial completo de hasta 50 pasos para revertir cambios accidentales."
        ),
        tags = listOf("disenador", "columnas", "mysql", "pk", "nn", "uq", "ai", "vista sql", "ddl", "plantillas", "deshacer", "preservacion")
    ),
    ManualTopic(
        title = "Vista de Datos: Edición de Celdas en Línea (Doble Clic estilo Excel)",
        category = ManualCategory.DATA_VIEW,
        content = "Explora y modifica los registros de cada tabla con la familiaridad de una hoja de cálculo moderna:",
        keyPoints = listOf(
            "Edición Rápida con Doble Clic: Haz doble clic sobre cualquier celda para editar su contenido directamente en la cuadrícula.",
            "Confirmación con Teclado: Presiona 'Enter' para guardar el valor editado inmediatamente en la base de datos, o 'Escape' para cancelar.",
            "Nuevo Registro: Abre el modal de inserción con validación de tipos y formatos.",
            "Llenar Datos (Mock): Poblador sintético local guiado por Cerebro para generar 5, 10, 50 o 100 registros realistas en español respetando claves foráneas.",
            "Exportación Individual: Menú desplegable para descargar la tabla activa en formato CSV (.csv) o JSON (.json)."
        ),
        tags = listOf("datos", "edicion", "celda", "doble clic", "excel", "nuevo registro", "llenar datos", "mock", "csv", "json")
    ),
    ManualTopic(
        title = "Importación y Exportación Completa a Microsoft Excel (.xlsx)",
        category = ManualCategory.DATA_VIEW,
        content = "Interoperabilidad nativa bidireccional con libros de cálculo de Microsoft Excel (.xlsx):",
        keyPoints = listOf(
            "Exportar a Excel (.xlsx): Disponible en 'Archivo > Exportar Excel' o en la pestaña 'Datos'. Genera un libro profesional con una pestaña por cada tabla y encabezados estilizados.",
            "Importar desde Excel (.xlsx): Disponible en 'Archivo > Importar Excel'. Lee cualquier libro .xlsx, analiza sus hojas, crea automáticamente las tablas correspondientes e inserta sus filas detectando tipos de datos.",
            "Compatibilidad Universal: Los archivos generados son 100% compatibles con Microsoft Excel, Google Sheets, LibreOffice Calc y Apple Numbers."
        ),
        tags = listOf("excel", "xlsx", "exportar", "importar", "hoja de calculo", "sheets", "intercambio")
    ),
    ManualTopic(
        title = "Diagrama Entidad-Relación: Auto-Organización Masonry y Relaciones",
        category = ManualCategory.DIAGRAM,
        content = "Mapa interactivo gráfico de tu modelo relacional con tecnología de disposición inteligente:",
        keyPoints = listOf(
            "Botón 'Auto-Organizar': Ubicado en la pestaña 'Vista > Diagramas'. Aplica un algoritmo masonry que calcula la altura real de cada tabla según sus columnas y las empaqueta limpiamente sin solaparse.",
            "Movimiento 100% Libre: Haz clic y arrastra cualquier tabla por el lienzo; las líneas conectoras de relaciones se adaptan dinámicamente en tiempo real.",
            "Gestión Visual de Relaciones: Haz clic sobre cualquier conector para ver su información y acceder a botones de editar reglas (CASCADE, RESTRICT, SET NULL) o eliminar.",
            "Controles de Zoom y Centrado: Botones flotantes para acercar (+), alejar (-) o centrar la panorámica de tu diagrama."
        ),
        tags = listOf("diagrama", "er", "auto-organizar", "masonry", "relaciones", "foreign key", "fk", "lienzo", "zoom")
    ),
    ManualTopic(
        title = "Consultas Avanzadas: Juntar Tablas (JOIN) y Pipeline ETL",
        category = ManualCategory.SQL_CONSOLE,
        content = "Herramientas visuales integradas en la pestaña 'Datos' para cruzar información entre múltiples tablas:",
        keyPoints = listOf(
            "Juntar Tablas (JOIN): Interfaz gráfica para seleccionar dos tablas, definir los campos de enlace (ej. clientes.id = pedidos.cliente_id) y elegir el tipo de JOIN (INNER, LEFT, RIGHT). Muestra la vista previa del resultado combinado.",
            "Consulta ETL: Permite extraer columnas de tablas relacionadas, transformarlas y persistirlas como una nueva tabla estructurada en tu base de datos.",
            "Sin Escribir SQL: Ideal para estudiantes o analistas que necesitan reportes cruzados sin necesidad de escribir sentencias complejas manualmente."
        ),
        tags = listOf("join", "juntar tablas", "etl", "consultas avanzadas", "datos", "inner join", "cruce")
    ),
    ManualTopic(
        title = "Consola SQL: Editor Monospace, Historial y Snippets",
        category = ManualCategory.SQL_CONSOLE,
        content = "Entorno de ejecución de sentencias SQL puras para desarrolladores y administradores de bases de datos:",
        keyPoints = listOf(
            "Atajo Ctrl + Enter: Ejecuta la consulta seleccionada al instante sin levantar las manos del teclado.",
            "Historial de Consultas: Panel lateral que registra automáticamente las sentencias ejecutadas para reutilizarlas con un clic.",
            "Snippets / Plantillas SQL: Chips para insertar rápidamente esqueletos de sentencias frecuentes (SELECT *, COUNT(*), INNER JOIN, CREATE TABLE).",
            "Métricas de Rendimiento: Visualiza el tiempo exacto en milisegundos (ms) y el total de filas afectadas o devueltas."
        ),
        tags = listOf("sql", "consola", "query", "historial", "snippets", "ctrl+enter", "bigquery", "tiempo")
    ),
    ManualTopic(
        title = "Paleta de Comandos Universal (Ctrl + K) y Tema Oscuro / Claro",
        category = ManualCategory.COMMANDS_THEME,
        content = "Control total y ergonomía visual con Material 3:",
        keyPoints = listOf(
            "Paleta de Comandos (Ctrl + K): Busca tablas, pantallas o funciones globales y navega rápidamente con las flechas del teclado y Enter.",
            "Modo Oscuro / Claro: Alterna en cualquier momento con el botón del sol/luna en la barra superior.",
            "Manual de Usuario (F1): Accede a esta guía completa en cualquier momento presionando F1 o desde el botón de libro en la barra superior."
        ),
        tags = listOf("comandos", "ctrl+k", "f1", "atajos", "tema", "dark mode", "modo oscuro", "material3")
    )
)
