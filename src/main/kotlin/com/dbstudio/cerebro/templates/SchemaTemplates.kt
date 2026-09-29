package com.dbstudio.cerebro.templates

import com.dbstudio.data.DatabaseEngine
import com.dbstudio.model.*

data class DatabaseTemplate(
    val name: String,
    val description: String,
    val tables: List<TableInfo>,
    val relationships: List<Relationship>
)

/**
 * Predefined, verified database schema templates showcasing clean relational architectures
 * including Many-to-Many intermediate tables (junction tables).
 */
object SchemaTemplates {

    val TEMPLATE_RESEARCH_GROUPS = DatabaseTemplate(
        name = "Gestión de Semilleros y Estudiantes (N:M)",
        description = "Esquema completo para grupos de investigación académica con tabla intermedia para membresías muchos a muchos.",
        tables = listOf(
            TableInfo(
                name = "semilleros",
                columns = listOf(
                    Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                    Column("nombre", ColumnType.VARCHAR, isNullable = false),
                    Column("linea_investigacion", ColumnType.VARCHAR, isNullable = true),
                    Column("lider", ColumnType.VARCHAR, isNullable = true),
                    Column("estado", ColumnType.VARCHAR, isNullable = false, defaultValue = "ACTIVO")
                )
            ),
            TableInfo(
                name = "estudiantes",
                columns = listOf(
                    Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                    Column("nombre", ColumnType.VARCHAR, isNullable = false),
                    Column("apellido", ColumnType.VARCHAR, isNullable = false),
                    Column("cedula", ColumnType.LONG, isNullable = false, isUnique = true),
                    Column("correo", ColumnType.VARCHAR, isNullable = false, isUnique = true),
                    Column("carrera", ColumnType.VARCHAR, isNullable = true)
                )
            ),
            TableInfo(
                name = "estudiantes_semilleros",
                columns = listOf(
                    Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                    Column("estudiante_id", ColumnType.INTEGER, isNullable = false),
                    Column("semillero_id", ColumnType.INTEGER, isNullable = false),
                    Column("rol", ColumnType.VARCHAR, isNullable = false, defaultValue = "INVESTIGADOR"),
                    Column("fecha_vinculacion", ColumnType.DATETIME, isNullable = false)
                )
            )
        ),
        relationships = listOf(
            Relationship(
                name = "fk_est_sem_estudiante",
                sourceTable = "estudiantes_semilleros",
                sourceColumn = "estudiante_id",
                targetTable = "estudiantes",
                targetColumn = "id",
                type = RelationType.ONE_TO_MANY,
                onDelete = ReferentialAction.CASCADE
            ),
            Relationship(
                name = "fk_est_sem_semillero",
                sourceTable = "estudiantes_semilleros",
                sourceColumn = "semillero_id",
                targetTable = "semilleros",
                targetColumn = "id",
                type = RelationType.ONE_TO_MANY,
                onDelete = ReferentialAction.CASCADE
            )
        )
    )

    val TEMPLATE_STORE_ORDERS = DatabaseTemplate(
        name = "Tienda, Productos y Pedidos (N:M)",
        description = "Esquema comercial con catálogo de productos, clientes, pedidos y detalle de ítems muchos a muchos.",
        tables = listOf(
            TableInfo(
                name = "clientes",
                columns = listOf(
                    Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                    Column("nombre", ColumnType.VARCHAR, isNullable = false),
                    Column("correo", ColumnType.VARCHAR, isNullable = false, isUnique = true),
                    Column("telefono", ColumnType.VARCHAR, isNullable = true),
                    Column("ciudad", ColumnType.VARCHAR, isNullable = true)
                )
            ),
            TableInfo(
                name = "productos",
                columns = listOf(
                    Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                    Column("nombre", ColumnType.VARCHAR, isNullable = false),
                    Column("precio", ColumnType.DECIMAL, isNullable = false, defaultValue = "0.0"),
                    Column("stock", ColumnType.INTEGER, isNullable = false, defaultValue = "0"),
                    Column("categoria", ColumnType.VARCHAR, isNullable = true)
                )
            ),
            TableInfo(
                name = "pedidos",
                columns = listOf(
                    Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                    Column("cliente_id", ColumnType.INTEGER, isNullable = false),
                    Column("fecha", ColumnType.DATETIME, isNullable = false),
                    Column("total", ColumnType.DECIMAL, isNullable = false, defaultValue = "0.0"),
                    Column("estado", ColumnType.VARCHAR, isNullable = false, defaultValue = "PENDIENTE")
                )
            ),
            TableInfo(
                name = "pedidos_productos",
                columns = listOf(
                    Column("id", ColumnType.AUTOINCREMENT, isPrimaryKey = true, isAutoIncrement = true, isNullable = false),
                    Column("pedido_id", ColumnType.INTEGER, isNullable = false),
                    Column("producto_id", ColumnType.INTEGER, isNullable = false),
                    Column("cantidad", ColumnType.INTEGER, isNullable = false, defaultValue = "1"),
                    Column("precio_unitario", ColumnType.DECIMAL, isNullable = false)
                )
            )
        ),
        relationships = listOf(
            Relationship(
                name = "fk_pedidos_cliente",
                sourceTable = "pedidos",
                sourceColumn = "cliente_id",
                targetTable = "clientes",
                targetColumn = "id",
                type = RelationType.ONE_TO_MANY,
                onDelete = ReferentialAction.RESTRICT
            ),
            Relationship(
                name = "fk_ped_prod_pedido",
                sourceTable = "pedidos_productos",
                sourceColumn = "pedido_id",
                targetTable = "pedidos",
                targetColumn = "id",
                type = RelationType.ONE_TO_MANY,
                onDelete = ReferentialAction.CASCADE
            ),
            Relationship(
                name = "fk_ped_prod_producto",
                sourceTable = "pedidos_productos",
                sourceColumn = "producto_id",
                targetTable = "productos",
                targetColumn = "id",
                type = RelationType.ONE_TO_MANY,
                onDelete = ReferentialAction.RESTRICT
            )
        )
    )

    fun getAllTemplates(): List<DatabaseTemplate> = listOf(
        TEMPLATE_RESEARCH_GROUPS,
        TEMPLATE_STORE_ORDERS
    )

    /**
     * Applies a complete template schema and its relationships to the database engine.
     */
    fun applyTemplate(engine: DatabaseEngine, template: DatabaseTemplate) {
        // 1. Create all tables
        for (table in template.tables) {
            if (!engine.getTableNames().any { it.equals(table.name, ignoreCase = true) }) {
                engine.createTable(table)
            }
        }

        // 2. Add relationships
        for (rel in template.relationships) {
            try {
                engine.addRelationship(rel)
            } catch (_: Exception) {
                // Non-fatal if engine requires custom recreation
            }
        }
    }
}
