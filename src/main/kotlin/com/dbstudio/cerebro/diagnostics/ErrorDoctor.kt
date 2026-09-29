package com.dbstudio.cerebro.diagnostics

/**
 * Intelligent local diagnostics engine that translates cryptic SQLite and UCanAccess/MS Access
 * exceptions into clear, friendly Spanish explanations with actionable solutions.
 */
object ErrorDoctor {

    fun diagnose(throwable: Throwable, context: String = ""): DiagnosticReport {
        val rawMessage = throwable.message ?: throwable.localizedMessage ?: throwable.toString()
        val cause = throwable.cause?.message ?: ""
        val fullText = "$rawMessage $cause $context".lowercase()

        // 1. Foreign Key / Integrity Constraint Violation
        if (fullText.contains("foreign key constraint failed") ||
            fullText.contains("no parent key") ||
            fullText.contains("integrity constraint violation") ||
            fullText.contains("foreign key") && fullText.contains("violat")
        ) {
            return DiagnosticReport(
                title = "Violación de Clave Foránea (Integridad Referencial)",
                friendlyExplanation = "Se intentó guardar o vincular un registro con un ID que no existe en la tabla principal referenciada.",
                technicalSummary = rawMessage,
                possibleCauses = listOf(
                    "El registro padre (por ejemplo, el curso, cliente o semillero) aún no ha sido creado en su tabla correspondiente.",
                    "El número o código ingresado en el campo foráneo no coincide exactamente con ninguna clave primaria existente.",
                    "Se intentó borrar un registro padre que todavía tiene registros hijos asociados."
                ),
                suggestedActions = listOf(
                    SuggestedAction(
                        title = "Verificar registros existentes",
                        description = "Asegúrate de que el valor foráneo ingresado exista primero en la tabla principal.",
                        actionType = ActionType.CREATE_PARENT_RECORD
                    ),
                    SuggestedAction(
                        title = "Dejar en blanco si es opcional",
                        description = "Si la relación permite valores nulos (NULL), puedes dejar el campo vacío temporalmente.",
                        actionType = ActionType.GENERAL_TIP
                    )
                ),
                severity = DiagnosticSeverity.ERROR
            )
        }

        // 2. Unique Constraint / Primary Key Duplicate
        if (fullText.contains("unique constraint failed") ||
            fullText.contains("duplicate key") ||
            fullText.contains("primary key constraint") ||
            fullText.contains("already exists in unique index")
        ) {
            return DiagnosticReport(
                title = "Clave Primaria o Valor Único Duplicado",
                friendlyExplanation = "Se intentó guardar un registro con un valor que ya existe en una columna configurada como Única o Clave Primaria (PK).",
                technicalSummary = rawMessage,
                possibleCauses = listOf(
                    "La columna 'id' u otra columna única (como cédula o correo) ya tiene una fila con ese mismo valor exacto.",
                    "Si la clave es autoincremental, el valor manual introducido colisionó con la secuencia automática."
                ),
                suggestedActions = listOf(
                    SuggestedAction(
                        title = "Usar un valor identificador diferente",
                        description = "Cambia el valor por uno que no se haya usado previamente.",
                        actionType = ActionType.REMOVE_DUPLICATE_KEY
                    ),
                    SuggestedAction(
                        title = "Dejar que el sistema autoincremente el ID",
                        description = "Si es una columna con autoincremento, déjala vacía para que la base de datos asigne el siguiente número disponible.",
                        actionType = ActionType.GENERAL_TIP
                    )
                ),
                severity = DiagnosticSeverity.ERROR
            )
        }

        // 3. NOT NULL constraint failed
        if (fullText.contains("not null constraint failed") ||
            fullText.contains("cannot be null") ||
            fullText.contains("may not be null") ||
            fullText.contains("null value in column")
        ) {
            return DiagnosticReport(
                title = "Campo Obligatorio Vacío (NOT NULL)",
                friendlyExplanation = "Uno o más campos configurados como obligatorios quedaron en blanco.",
                technicalSummary = rawMessage,
                possibleCauses = listOf(
                    "Se omitió un valor en una columna que tiene la regla 'No Nulo' (NOT NULL) activada.",
                    "Se envió una cadena vacía o nula desde el formulario o archivo importado."
                ),
                suggestedActions = listOf(
                    SuggestedAction(
                        title = "Completar los campos requeridos",
                        description = "Rellena todos los campos marcados como obligatorios antes de guardar.",
                        actionType = ActionType.FILL_REQUIRED_FIELD
                    ),
                    SuggestedAction(
                        title = "Permitir valores nulos si no es indispensable",
                        description = "En el Diseñador de Tablas, puedes desmarcar 'NOT NULL' si ese campo puede ser opcional.",
                        actionType = ActionType.GENERAL_TIP
                    )
                ),
                severity = DiagnosticSeverity.WARNING
            )
        }

        // 4. Database locked / file busy
        if (fullText.contains("database is locked") ||
            fullText.contains("resource temporarily unavailable") ||
            fullText.contains("locked") ||
            fullText.contains("busy") ||
            fullText.contains("used by another process")
        ) {
            return DiagnosticReport(
                title = "Base de Datos Bloqueada por Otro Proceso",
                friendlyExplanation = "El archivo de base de datos está siendo utilizado simultáneamente por otro programa o conexión.",
                technicalSummary = rawMessage,
                possibleCauses = listOf(
                    "Microsoft Access, Excel, DB Browser u otro software tiene el archivo abierto en segundo plano.",
                    "Una consulta previa muy pesada quedó ejecutándose y retiene el bloqueo de escritura."
                ),
                suggestedActions = listOf(
                    SuggestedAction(
                        title = "Cerrar programas externos",
                        description = "Cierra Access, Excel o cualquier visor que tenga abierto el archivo de la base de datos.",
                        actionType = ActionType.CLOSE_EXTERNAL_APP
                    ),
                    SuggestedAction(
                        title = "Reintentar la operación",
                        description = "Espera 2 segundos e intenta guardar nuevamente.",
                        actionType = ActionType.GENERAL_TIP
                    )
                ),
                severity = DiagnosticSeverity.WARNING
            )
        }

        // 5. Data type mismatch
        if (fullText.contains("datatype mismatch") ||
            fullText.contains("data type mismatch") ||
            fullText.contains("incompatible data types") ||
            fullText.contains("numberformatexception") ||
            fullText.contains("cannot convert")
        ) {
            return DiagnosticReport(
                title = "Tipo de Dato Incompatible",
                friendlyExplanation = "El valor ingresado no corresponde al tipo de dato esperado por la columna.",
                technicalSummary = rawMessage,
                possibleCauses = listOf(
                    "Se ingresaron letras o símbolos en una columna numérica (INTEGER, DECIMAL).",
                    "El formato de fecha no es válido (se recomienda 'YYYY-MM-DD' o 'YYYY-MM-DD HH:mm:ss').",
                    "Se intentó guardar un texto excesivamente largo en un campo limitado."
                ),
                suggestedActions = listOf(
                    SuggestedAction(
                        title = "Corregir formato del valor",
                        description = "Verifica que los números no contengan letras y que las fechas usen el formato ISO (AAAA-MM-DD).",
                        actionType = ActionType.FIX_DATA_TYPE
                    )
                ),
                severity = DiagnosticSeverity.ERROR
            )
        }

        // 6. Table already exists / does not exist
        if (fullText.contains("table") && (fullText.contains("already exists") || fullText.contains("ya existe"))) {
            return DiagnosticReport(
                title = "La Tabla Ya Existe",
                friendlyExplanation = "Ya existe una tabla con ese mismo nombre en la base de datos.",
                technicalSummary = rawMessage,
                possibleCauses = listOf(
                    "Se intentó crear o renombrar una tabla con un nombre que ya está en uso."
                ),
                suggestedActions = listOf(
                    SuggestedAction(
                        title = "Elegir un nombre distintivo",
                        description = "Asigna un nombre diferente a la tabla para evitar colisiones.",
                        actionType = ActionType.GENERAL_TIP
                    )
                ),
                severity = DiagnosticSeverity.WARNING
            )
        }

        // Default generic helpful diagnostic
        return DiagnosticReport(
            title = "Aviso del Asistente de Base de Datos",
            friendlyExplanation = "Ocurrió una incidencia durante la ejecución de la operación en el motor de base de datos.",
            technicalSummary = rawMessage.ifBlank { throwable.javaClass.simpleName },
            possibleCauses = listOf(
                "La consulta o cambio de esquema no es compatible con el motor de base de datos actual.",
                "Los datos proporcionados no cumplen con las restricciones de la tabla."
            ),
            suggestedActions = listOf(
                SuggestedAction(
                    title = "Verificar esquema y datos",
                    description = "Comprueba que los nombres de columnas y tipos de datos coincidan con la estructura esperada.",
                    actionType = ActionType.GENERAL_TIP
                )
            ),
            severity = DiagnosticSeverity.ERROR
        )
    }
}
