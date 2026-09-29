package com.dbstudio.cerebro.advisor

import com.dbstudio.model.Column
import com.dbstudio.model.ColumnType

/**
 * Intelligent type inference engine that analyzes column names, patterns, and contexts
 * to suggest optimal database types, constraints, and defaults.
 */
object TypeInferrer {

    data class InferredColumn(
        val type: ColumnType,
        val isPrimaryKey: Boolean = false,
        val isAutoIncrement: Boolean = false,
        val isNullable: Boolean = true,
        val isUnique: Boolean = false,
        val defaultValue: String? = null,
        val explanation: String
    )

    fun infer(columnName: String, isFirstColumn: Boolean = false): InferredColumn {
        val clean = columnName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")

        // 1. Primary Key / Autoincrement Identification
        if (clean == "id" || (isFirstColumn && (clean == "codigo" || clean == "id_tabla" || clean.endsWith("_id")))) {
            if (clean == "id") {
                return InferredColumn(
                    type = ColumnType.AUTOINCREMENT,
                    isPrimaryKey = true,
                    isAutoIncrement = true,
                    isNullable = false,
                    isUnique = true,
                    explanation = "Identificador principal único y autoincremental de la tabla."
                )
            }
        }

        // 2. Foreign Key columns (e.g. cliente_id, id_cliente, semillero_id)
        if (clean.endsWith("_id") || clean.startsWith("id_") || clean.endsWith("_fk")) {
            return InferredColumn(
                type = ColumnType.INTEGER,
                isPrimaryKey = false,
                isAutoIncrement = false,
                isNullable = true,
                explanation = "Clave foránea sugerida para relacionar con otra tabla."
            )
        }

        // 3. Document / National ID / Large numbers (Cédula, DNI, RUT, NIT, Pasaporte, Teléfono, Celular)
        if (clean in listOf("cedula", "dni", "rut", "nit", "telefono", "celular", "phone", "mobile", "whatsapp") ||
            clean.contains("cedula") || clean.contains("telefono") || clean.contains("celular")
        ) {
            return InferredColumn(
                type = ColumnType.LONG,
                isNullable = false,
                explanation = "Identificador o número de contacto numérico largo."
            )
        }

        // 4. Money / Currency / Financials (Precio, Costo, Saldo, Total, Subtotal, Valor, Tarifa, Sueldo, Salario)
        if (clean in listOf("precio", "costo", "saldo", "total", "subtotal", "valor", "tarifa", "sueldo", "salario", "monto", "importe") ||
            clean.contains("precio") || clean.contains("costo") || clean.contains("total") || clean.contains("salario")
        ) {
            return InferredColumn(
                type = ColumnType.DECIMAL,
                isNullable = false,
                defaultValue = "0.0",
                explanation = "Valor monetario o importe con precisión decimal."
            )
        }

        // 5. Quantity / Stock / Integers (Cantidad, Stock, Unidades, Edad, Dias, Meses, Anio)
        if (clean in listOf("cantidad", "stock", "unidades", "edad", "puntos", "orden", "prioridad", "posicion") ||
            clean.contains("cantidad") || clean.contains("stock")
        ) {
            return InferredColumn(
                type = ColumnType.INTEGER,
                isNullable = false,
                defaultValue = "0",
                explanation = "Cantidad numérica entera."
            )
        }

        // 6. Dates and Timestamps
        if (clean in listOf("fecha_creacion", "creado_en", "fecha_actualizacion", "actualizado_en", "created_at", "updated_at", "timestamp")) {
            return InferredColumn(
                type = ColumnType.DATETIME,
                isNullable = false,
                explanation = "Registro cronológico de fecha y hora del sistema."
            )
        }
        if (clean.startsWith("fecha_") || clean.endsWith("_fecha") || clean in listOf("fecha", "nacimiento", "vencimiento", "entrega", "inicio", "fin")) {
            return InferredColumn(
                type = ColumnType.DATE,
                isNullable = true,
                explanation = "Fecha de calendario (AAAA-MM-DD)."
            )
        }

        // 7. Boolean / Flags (Activo, Habilitado, Estado_Activo, Es_Admin, Pagado, Aprobado)
        if (clean in listOf("activo", "habilitado", "estado", "es_admin", "pagado", "aprobado", "publicado", "vigente") ||
            clean.startsWith("es_") || clean.startsWith("is_") || clean.startsWith("tiene_")
        ) {
            return InferredColumn(
                type = ColumnType.BOOLEAN,
                isNullable = false,
                defaultValue = "1",
                explanation = "Indicador booleano (Verdadero / Falso o Sí / No)."
            )
        }

        // 8. Long Text / Descriptions / Observaciones
        if (clean in listOf("descripcion", "observaciones", "observacion", "nota", "notas", "detalle", "comentario", "comentarios", "contenido", "mensaje") ||
            clean.contains("descripcion") || clean.contains("observacion")
        ) {
            return InferredColumn(
                type = ColumnType.TEXT,
                isNullable = true,
                explanation = "Texto extendido sin límite estricto de longitud."
            )
        }

        // 9. Standard Text (Nombre, Apellido, Correo, Direccion, Ciudad, Cargo, etc.)
        if (clean in listOf("correo", "email", "mail")) {
            return InferredColumn(
                type = ColumnType.VARCHAR,
                isNullable = false,
                isUnique = true,
                explanation = "Dirección de correo electrónico."
            )
        }

        return InferredColumn(
            type = ColumnType.VARCHAR,
            isNullable = false,
            explanation = "Texto corto estándar."
        )
    }

    /**
     * Converts inferred attributes into a ready-to-use Column object.
     */
    fun toColumn(columnName: String, isFirstColumn: Boolean = false): Column {
        val inferred = infer(columnName, isFirstColumn)
        return Column(
            name = columnName.trim(),
            type = inferred.type,
            isNullable = inferred.isNullable,
            isPrimaryKey = inferred.isPrimaryKey,
            isAutoIncrement = inferred.isAutoIncrement,
            isUnique = inferred.isUnique,
            defaultValue = inferred.defaultValue
        )
    }
}
