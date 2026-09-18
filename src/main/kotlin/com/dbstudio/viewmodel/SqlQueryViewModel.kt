package com.dbstudio.viewmodel

import androidx.compose.runtime.*
import com.dbstudio.data.DatabaseEngine
import com.dbstudio.data.QueryResult
import kotlinx.coroutines.*
import java.io.File
import java.io.PrintWriter

/**
 * ViewModel for the interactive BigQuery/Cloud SQL style Query Studio.
 */
class SqlQueryViewModel {

    var queryText by mutableStateOf("")
    var queryResult by mutableStateOf<QueryResult?>(null)
        private set
    var isExecuting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var queryHistory by mutableStateOf<List<String>>(emptyList())
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun setQuery(sql: String) {
        queryText = sql
    }

    fun execute(engine: DatabaseEngine?) {
        val trimmed = queryText.trim()
        if (trimmed.isBlank() || engine == null) return

        scope.launch {
            try {
                isExecuting = true
                errorMessage = null
                val res = engine.executeQuery(trimmed)
                withContext(Dispatchers.Main) {
                    queryResult = res
                    if (!queryHistory.contains(trimmed)) {
                        queryHistory = (listOf(trimmed) + queryHistory).take(20)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "Error al ejecutar la consulta"
                    queryResult = null
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isExecuting = false
                }
            }
        }
    }

    fun clearResult() {
        queryResult = null
        errorMessage = null
    }

    /**
     * Export current query results to a CSV file.
     */
    fun exportResultToCsv(file: File): Boolean {
        val res = queryResult ?: return false
        return try {
            PrintWriter(file.bufferedWriter(Charsets.UTF_8)).use { writer ->
                // Write header
                writer.println(res.columns.joinToString(",") { escapeCsv(it) })
                // Write data rows
                for (row in res.rows) {
                    writer.println(row.joinToString(",") { escapeCsv(it?.toString() ?: "") })
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
