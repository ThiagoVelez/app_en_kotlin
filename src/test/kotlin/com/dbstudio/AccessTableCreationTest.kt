package com.dbstudio

import com.dbstudio.data.AccessEngine
import com.dbstudio.model.ColumnPresets
import com.dbstudio.model.TableInfo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AccessTableCreationTest {

    @Test
    fun testCreateAccessTableWithFechaCreacion() {
        val table = TableInfo(
            name = "ola",
            columns = listOf(
                ColumnPresets.id(),
                ColumnPresets.fechaCreacion()
            )
        )
        val sql = table.toCreateAccessSql()
        println("Generated SQL: $sql")
        assertTrue(sql.contains("DEFAULT CURRENT_TIMESTAMP NOT NULL") || sql.contains("DEFAULT CURRENT_TIMESTAMP"))
        assertTrue(!sql.contains("NOT NULL DEFAULT"))

        // Now test actual execution in an Access database
        val tempDb = File.createTempFile("test_db_", ".accdb")
        tempDb.delete()
        try {
            val engine = AccessEngine()
            engine.connect(tempDb.absolutePath, create = true)
            engine.createTable(table)
            val info = engine.getTableInfo("ola")
            assertTrue(info.columns.any { it.name == "fecha_creacion" })
            engine.disconnect()
        } finally {
            tempDb.delete()
        }
    }
}
