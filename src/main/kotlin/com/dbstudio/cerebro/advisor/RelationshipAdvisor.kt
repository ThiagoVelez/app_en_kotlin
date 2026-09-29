package com.dbstudio.cerebro.advisor

import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.*

data class SuggestedRelationship(
    val sourceTable: String,
    val sourceColumn: String,
    val targetTable: String,
    val targetColumn: String,
    val confidence: Float,
    val reason: String
)

data class JunctionTableInfo(
    val tableName: String,
    val tableA: String,
    val columnA: String,
    val tableB: String,
    val columnB: String,
    val extraColumns: List<String>
)

/**
 * Intelligent advisor for foreign keys, 1:N relations, and N:M (Many-to-Many) intermediate tables.
 */
object RelationshipAdvisor {

    /**
     * Finds potential 1:N relationships in the database based on naming conventions and primary keys.
     */
    fun detectPossibleRelationships(
        tables: List<TableInfo>,
        existingRelationships: List<Relationship>
    ): List<SuggestedRelationship> {
        val suggestions = mutableListOf<SuggestedRelationship>()
        val existingPairs = existingRelationships.map {
            "${it.sourceTable.lowercase()}.${it.sourceColumn.lowercase()}->${it.targetTable.lowercase()}.${it.targetColumn.lowercase()}"
        }.toSet()

        val tableMap = tables.associateBy { it.name.lowercase() }

        for (sourceTable in tables) {
            for (sourceCol in sourceTable.columns) {
                // Ignore if it's the source table's own primary key
                if (sourceCol.isPrimaryKey && sourceTable.columns.count { it.isPrimaryKey } == 1) continue

                val colNameLower = sourceCol.name.lowercase()

                // Check pattern: "target_id", "id_target", or "targetid"
                val potentialTargetName = when {
                    colNameLower.endsWith("_id") -> colNameLower.removeSuffix("_id")
                    colNameLower.startsWith("id_") -> colNameLower.removePrefix("id_")
                    colNameLower.endsWith("id") && colNameLower.length > 2 -> colNameLower.removeSuffix("id")
                    else -> null
                } ?: continue

                // Find matching target table (matching singular, plural, or exact)
                val targetTable = tableMap[potentialTargetName]
                    ?: tableMap["${potentialTargetName}s"]
                    ?: tableMap["${potentialTargetName}es"]
                    ?: tableMap.values.find {
                        it.name.lowercase().removeSuffix("s").removeSuffix("es") == potentialTargetName
                    }

                if (targetTable != null && !targetTable.name.equals(sourceTable.name, ignoreCase = true)) {
                    val targetPk = targetTable.columns.firstOrNull { it.isPrimaryKey } ?: continue

                    val pairKey = "${sourceTable.name.lowercase()}.${sourceCol.name.lowercase()}->${targetTable.name.lowercase()}.${targetPk.name.lowercase()}"
                    if (pairKey !in existingPairs) {
                        suggestions.add(
                            SuggestedRelationship(
                                sourceTable = sourceTable.name,
                                sourceColumn = sourceCol.name,
                                targetTable = targetTable.name,
                                targetColumn = targetPk.name,
                                confidence = 0.95f,
                                reason = "La columna '${sourceCol.name}' coincide con la clave primaria '${targetPk.name}' de la tabla '${targetTable.name}'."
                            )
                        )
                    }
                }
            }
        }

        return suggestions
    }

    /**
     * Inspects tables and relationships to identify which tables act as intermediate (junction) tables
     * representing a Many-to-Many (N:M) relationship.
     */
    fun findJunctionTables(
        tables: List<TableInfo>,
        relationships: List<Relationship>
    ): List<JunctionTableInfo> {
        val junctionTables = mutableListOf<JunctionTableInfo>()
        val relsBySource = relationships.groupBy { it.sourceTable.lowercase() }

        for (table in tables) {
            val tableRels = relsBySource[table.name.lowercase()] ?: emptyList()
            // A junction table typically has 2 foreign keys pointing to 2 different tables
            val distinctTargets = tableRels.map { it.targetTable.lowercase() }.distinct()
            if (distinctTargets.size >= 2) {
                val relA = tableRels[0]
                val relB = tableRels[1]
                val extra = table.columns
                    .map { it.name }
                    .filter { col ->
                        !col.equals(relA.sourceColumn, ignoreCase = true) &&
                        !col.equals(relB.sourceColumn, ignoreCase = true) &&
                        !col.equals("id", ignoreCase = true)
                    }

                junctionTables.add(
                    JunctionTableInfo(
                        tableName = table.name,
                        tableA = relA.targetTable,
                        columnA = relA.sourceColumn,
                        tableB = relB.targetTable,
                        columnB = relB.sourceColumn,
                        extraColumns = extra
                    )
                )
            }
        }

        return junctionTables
    }

    /**
     * Creates a fully functional Many-to-Many intermediate table (tabla asociativa/intermedia)
     * between two tables with automatic foreign keys, primary key, and metadata columns.
     */
    fun createManytoManyIntermediateTable(
        engine: DatabaseEngine,
        tableA: String,
        tableB: String,
        customTableName: String? = null,
        includeAuditDates: Boolean = true
    ): IntermediateTableCreationResult {
        val cleanA = tableA.trim()
        val cleanB = tableB.trim()

        val infoA = engine.getTableInfo(cleanA)
        val infoB = engine.getTableInfo(cleanB)

        val pkA = infoA.columns.firstOrNull { it.isPrimaryKey }
            ?: throw IllegalStateException("La tabla '$cleanA' no tiene una clave primaria definida.")
        val pkB = infoB.columns.firstOrNull { it.isPrimaryKey }
            ?: throw IllegalStateException("La tabla '$cleanB' no tiene una clave primaria definida.")

        val tableName = if (!customTableName.isNullOrBlank()) {
            customTableName.trim()
        } else {
            "${cleanA.lowercase()}_${cleanB.lowercase()}"
        }

        // Verify table doesn't already exist
        if (engine.getTableNames().any { it.equals(tableName, ignoreCase = true) }) {
            throw IllegalArgumentException("La tabla intermedia '$tableName' ya existe en la base de datos.")
        }

        val fkColA = "${cleanA.lowercase()}_${pkA.name.lowercase()}"
        val fkColB = "${cleanB.lowercase()}_${pkB.name.lowercase()}"

        val fkTypeA = when (pkA.type) {
            ColumnType.AUTOINCREMENT, ColumnType.INTEGER -> ColumnType.INTEGER
            ColumnType.LONG -> ColumnType.LONG
            else -> pkA.type
        }
        val fkTypeB = when (pkB.type) {
            ColumnType.AUTOINCREMENT, ColumnType.INTEGER -> ColumnType.INTEGER
            ColumnType.LONG -> ColumnType.LONG
            else -> pkB.type
        }

        val columns = mutableListOf(
            Column(
                name = "id",
                type = ColumnType.AUTOINCREMENT,
                isPrimaryKey = true,
                isAutoIncrement = true,
                isNullable = false
            ),
            Column(
                name = fkColA,
                type = fkTypeA,
                isPrimaryKey = false,
                isAutoIncrement = false,
                isNullable = false
            ),
            Column(
                name = fkColB,
                type = fkTypeB,
                isPrimaryKey = false,
                isAutoIncrement = false,
                isNullable = false
            )
        )

        if (includeAuditDates) {
            columns.add(
                Column(
                    name = "fecha_asignacion",
                    type = ColumnType.DATETIME,
                    isNullable = false,
                    defaultValue = null
                )
            )
            columns.add(
                Column(
                    name = "estado",
                    type = ColumnType.VARCHAR,
                    isNullable = true,
                    defaultValue = "ACTIVO"
                )
            )
        }

        val intermediateTable = TableInfo(
            name = tableName,
            columns = columns
        )

        // 1. Create intermediate table in database
        engine.createTable(intermediateTable)

        // 2. Create the two foreign key relationships (1:N from each parent to intermediate table)
        val relA = Relationship(
            name = "fk_${tableName}_${fkColA}",
            sourceTable = tableName,
            sourceColumn = fkColA,
            targetTable = cleanA,
            targetColumn = pkA.name,
            type = RelationType.ONE_TO_MANY,
            onDelete = ReferentialAction.CASCADE,
            onUpdate = ReferentialAction.NO_ACTION
        )

        val relB = Relationship(
            name = "fk_${tableName}_${fkColB}",
            sourceTable = tableName,
            sourceColumn = fkColB,
            targetTable = cleanB,
            targetColumn = pkB.name,
            type = RelationType.ONE_TO_MANY,
            onDelete = ReferentialAction.CASCADE,
            onUpdate = ReferentialAction.NO_ACTION
        )

        try {
            engine.addRelationship(relA)
        } catch (e: Exception) {
            // Non-fatal if DB engine does not support inline FK alter
        }

        try {
            engine.addRelationship(relB)
        } catch (e: Exception) {
            // Non-fatal
        }

        return IntermediateTableCreationResult(
            intermediateTable = intermediateTable,
            relationshipToA = relA,
            relationshipToB = relB
        )
    }
}

data class IntermediateTableCreationResult(
    val intermediateTable: TableInfo,
    val relationshipToA: Relationship,
    val relationshipToB: Relationship
)
