package com.dbstudio.cerebro

import com.dbstudio.cerebro.advisor.*
import com.dbstudio.cerebro.diagnostics.DiagnosticReport
import com.dbstudio.cerebro.diagnostics.ErrorDoctor
import com.dbstudio.cerebro.generator.MockDataGenerator
import com.dbstudio.cerebro.generator.MockGenerationResult
import com.dbstudio.cerebro.templates.DatabaseTemplate
import com.dbstudio.cerebro.templates.SchemaTemplates
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.Column
import com.dbstudio.model.Relationship
import com.dbstudio.model.TableInfo

/**
 * Cerebro - The Offline Intelligent Database Assistant.
 * Provides unified access to diagnostics, schema auditing, relationship guidance,
 * Many-to-Many intermediate table creation, and smart data population.
 */
object Cerebro {

    /** Diagnoses any technical exception and returns a human-friendly Spanish explanation and fix. */
    fun diagnoseError(throwable: Throwable, context: String = ""): DiagnosticReport {
        return ErrorDoctor.diagnose(throwable, context)
    }

    /** Suggests optimal type, primary key, and nullability based on column name. */
    fun inferColumn(columnName: String, isFirstColumn: Boolean = false): TypeInferrer.InferredColumn {
        return TypeInferrer.infer(columnName, isFirstColumn)
    }

    /** Creates a ready-to-use Column object inferred from the given name. */
    fun toColumn(columnName: String, isFirstColumn: Boolean = false): Column {
        return TypeInferrer.toColumn(columnName, isFirstColumn)
    }

    /** Audits an individual table for common design flaws or engine incompatibilities. */
    fun auditTable(table: TableInfo): List<SchemaAuditIssue> {
        return SchemaAdvisor.auditTable(table)
    }

    /** Audits the entire database schema. */
    fun auditDatabase(tables: List<TableInfo>): List<SchemaAuditIssue> {
        return SchemaAdvisor.auditDatabase(tables)
    }

    /** Finds implicit 1:N relationships between tables. */
    fun detectPossibleRelationships(
        tables: List<TableInfo>,
        existingRelationships: List<Relationship>
    ): List<SuggestedRelationship> {
        return RelationshipAdvisor.detectPossibleRelationships(tables, existingRelationships)
    }

    /** Detects which tables act as intermediate (junction) tables for Many-to-Many relationships. */
    fun findJunctionTables(
        tables: List<TableInfo>,
        relationships: List<Relationship>
    ): List<JunctionTableInfo> {
        return RelationshipAdvisor.findJunctionTables(tables, relationships)
    }

    /**
     * Creates a Many-to-Many intermediate table (tabla intermedia) with foreign keys to both tables.
     */
    fun createManytoManyIntermediateTable(
        engine: DatabaseEngine,
        tableA: String,
        tableB: String,
        customTableName: String? = null,
        includeAuditDates: Boolean = true
    ): IntermediateTableCreationResult {
        return RelationshipAdvisor.createManytoManyIntermediateTable(
            engine = engine,
            tableA = tableA,
            tableB = tableB,
            customTableName = customTableName,
            includeAuditDates = includeAuditDates
        )
    }

    /** Populates a table with realistic, Spanish, constraint-compliant test data. */
    fun generateMockData(
        engine: DatabaseEngine,
        tableName: String,
        rowCount: Int = 10
    ): MockGenerationResult {
        return MockDataGenerator.generateRows(engine, tableName, rowCount)
    }

    /** Returns verified database templates. */
    fun getTemplates(): List<DatabaseTemplate> = SchemaTemplates.getAllTemplates()

    /** Applies a predefined database template. */
    fun applyTemplate(engine: DatabaseEngine, template: DatabaseTemplate) {
        SchemaTemplates.applyTemplate(engine, template)
    }
}
