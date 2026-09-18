package com.dbstudio.model

/**
 * Type of relationship between tables.
 */
enum class RelationType(val displayName: String, val symbol: String) {
    ONE_TO_ONE("Uno a Uno (1:1)", "1:1"),
    ONE_TO_MANY("Uno a Muchos (1:N)", "1:N"),
    MANY_TO_MANY("Muchos a Muchos (N:M)", "N:M");

    companion object {
        fun fromDisplayName(name: String): RelationType {
            return entries.find { it.displayName == name } ?: ONE_TO_MANY
        }
    }
}

/**
 * Referential action for foreign key constraints.
 */
enum class ReferentialAction(val displayName: String, val sql: String) {
    CASCADE("En Cascada (CASCADE)", "CASCADE"),
    SET_NULL("Establecer Nulo (SET NULL)", "SET NULL"),
    SET_DEFAULT("Establecer Predeterminado (SET DEFAULT)", "SET DEFAULT"),
    RESTRICT("Restringir (RESTRICT)", "RESTRICT"),
    NO_ACTION("Sin Acción (Por Defecto)", "NO ACTION");

    companion object {
        fun fromDisplayName(name: String): ReferentialAction {
            return entries.find { it.displayName == name } ?: NO_ACTION
        }
    }
}

/**
 * Represents a foreign key relationship between two tables.
 */
data class Relationship(
    val name: String,
    val sourceTable: String,
    val sourceColumn: String,
    val targetTable: String,
    val targetColumn: String,
    val type: RelationType = RelationType.ONE_TO_MANY,
    val onDelete: ReferentialAction = ReferentialAction.NO_ACTION,
    val onUpdate: ReferentialAction = ReferentialAction.NO_ACTION
) {
    val displaySummary: String
        get() = "$sourceTable.$sourceColumn → $targetTable.$targetColumn"

    /**
     * Generate the ALTER TABLE statement for SQLite (added after table creation via PRAGMA).
     * Note: SQLite doesn't support ALTER TABLE ADD CONSTRAINT, so FK must be
     * defined at table creation or via recreation. This generates the FK clause for CREATE TABLE.
     */
    fun toSqliteFkClause(): String {
        return "FOREIGN KEY (\"$sourceColumn\") REFERENCES \"$targetTable\"(\"$targetColumn\")" +
                " ON DELETE ${onDelete.sql} ON UPDATE ${onUpdate.sql}"
    }

    /**
     * Generate the ALTER TABLE statement for Access via UCanAccess.
     * Note: Access only supports CASCADE for ON DELETE/UPDATE. If NO ACTION or RESTRICT,
     * the clause is omitted as it is the default behavior in Access.
     */
    fun toAccessAlterSql(): String {
        val actions = mutableListOf<String>()
        if (onDelete == ReferentialAction.CASCADE) actions.add("ON DELETE CASCADE")
        if (onUpdate == ReferentialAction.CASCADE) actions.add("ON UPDATE CASCADE")
        val actionClause = if (actions.isNotEmpty()) " " + actions.joinToString(" ") else ""
        return "ALTER TABLE [$sourceTable] ADD CONSTRAINT [$name] " +
                "FOREIGN KEY ([$sourceColumn]) REFERENCES [$targetTable]([$targetColumn])$actionClause"
    }
}
