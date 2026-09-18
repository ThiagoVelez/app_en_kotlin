package com.dbstudio.ui.components

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * File chooser utilities wrapping JFileChooser for database files.
 */
object FileChooser {

    /**
     * Show an "Open" dialog filtered to database files.
     * Returns the selected file path, or null if cancelled.
     */
    fun openDatabase(currentDir: String? = null): String? {
        val chooser = JFileChooser(currentDir ?: System.getProperty("user.home"))
        chooser.dialogTitle = "Abrir Base de Datos"
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        chooser.addChoosableFileFilter(
            FileNameExtensionFilter("Todos los Archivos de Base de Datos (*.db, *.sqlite, *.accdb, *.mdb)", "db", "sqlite", "sqlite3", "accdb", "mdb")
        )
        chooser.addChoosableFileFilter(
            FileNameExtensionFilter("Bases de Datos SQLite (*.db, *.sqlite, *.sqlite3)", "db", "sqlite", "sqlite3")
        )
        chooser.addChoosableFileFilter(
            FileNameExtensionFilter("Bases de Datos Access (*.accdb, *.mdb)", "accdb", "mdb")
        )
        chooser.isAcceptAllFileFilterUsed = false

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else null
    }

    /**
     * Show a "Save" dialog for choosing an export location.
     */
    fun saveFile(
        title: String = "Save",
        defaultName: String = "",
        extension: String = "sql",
        description: String = "SQL Files",
        currentDir: String? = null
    ): String? {
        val chooser = JFileChooser(currentDir ?: System.getProperty("user.home"))
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        chooser.addChoosableFileFilter(FileNameExtensionFilter(description, extension))
        chooser.isAcceptAllFileFilterUsed = false
        if (defaultName.isNotEmpty()) {
            chooser.selectedFile = File(defaultName)
        }

        return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            var path = chooser.selectedFile.absolutePath
            if (!path.endsWith(".$extension")) {
                path += ".$extension"
            }
            path
        } else null
    }

    /**
     * Show a directory chooser for new database location.
     */
    fun chooseDirectory(title: String = "Choose Directory", currentDir: String? = null): String? {
        val chooser = JFileChooser(currentDir ?: System.getProperty("user.home"))
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isAcceptAllFileFilterUsed = false

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else null
    }
}
