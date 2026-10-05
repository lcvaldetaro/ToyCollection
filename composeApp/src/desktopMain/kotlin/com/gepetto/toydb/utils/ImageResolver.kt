package com.gepetto.toydb.utils

import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import java.awt.FileDialog
import java.awt.Frame

actual fun resolveImageUri(prefix: String, refNum: Int): String? {
    val extensions = listOf("jpg", "jpeg", "png", "gif", "webp", "JPG", "PNG", "GIF")
    val possibleDirs = mutableListOf<File>()
    
    ImageResolverConfig.imagesPath?.let { customPath ->
        val customDir = File(customPath)
        if (customDir.exists() && customDir.isDirectory) {
            possibleDirs.add(customDir)
        }
    }
    
    val homeDir = System.getProperty("user.home")
    if (homeDir != null) {
        possibleDirs.add(File(homeDir, "valdetaro/ToyCollection/ToyDb/images"))
    }
    possibleDirs.addAll(listOf(
        File("images"),
        File("../images"),
        File("ToyDb/images"),
        File("../ToyDb/images")
    ))
    
    val dir = possibleDirs.find { it.exists() && it.isDirectory } ?: return null
    
    for (ext in extensions) {
        val file = File(dir, "$prefix$refNum.$ext")
        if (file.exists()) {
            return file.absolutePath
        }
    }
    return null
}

actual fun resolveBitmapUri(filename: String): String? {
    val possibleDirs = mutableListOf<File>()
    
    ImageResolverConfig.imagesPath?.let { customPath ->
        val customDir = File(customPath)
        if (customDir.exists() && customDir.isDirectory) {
            possibleDirs.add(customDir)
        }
    }
    
    val homeDir = System.getProperty("user.home")
    if (homeDir != null) {
        possibleDirs.add(File(homeDir, "valdetaro/ToyCollection/ToyDb/images"))
    }
    possibleDirs.addAll(listOf(
        File("images"),
        File("../images"),
        File("ToyDb/images"),
        File("../ToyDb/images")
    ))
    
    val dir = possibleDirs.find { it.exists() && it.isDirectory } ?: return null
    val file = File(dir, filename.trim())
    return if (file.exists()) file.absolutePath else null
}


actual fun selectDirectoryDialog(title: String): String? {
    var selectedPath: String? = null
    try {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = title
            isAcceptAllFileFilterUsed = false
        }
        val runChooser = {
            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedPath = chooser.selectedFile.absolutePath
            }
        }
        if (SwingUtilities.isEventDispatchThread()) {
            runChooser()
        } else {
            SwingUtilities.invokeAndWait(runChooser)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return selectedPath
}

actual fun selectFileDialog(title: String, allowedExtensions: List<String>): String? {
    var selectedPath: String? = null
    try {
        val runDialog = {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            dialog.filenameFilter = java.io.FilenameFilter { _, name ->
                allowedExtensions.any { ext -> name.lowercase().endsWith(".$ext") }
            }
            dialog.isVisible = true
            val directory = dialog.directory
            val file = dialog.file
            if (file != null && directory != null) {
                selectedPath = File(directory, file).absolutePath
            }
        }
        if (SwingUtilities.isEventDispatchThread()) {
            runDialog()
        } else {
            SwingUtilities.invokeAndWait(runDialog)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return selectedPath
}

actual fun isDesktopPlatform(): Boolean = true

@androidx.compose.runtime.Composable
actual fun rememberImagePicker(onImagePicked: (String) -> Unit): () -> Unit {
    return {
        val selectedPath = selectFileDialog(
            "Select Image to Upload",
            listOf("jpg", "jpeg", "png", "gif", "webp", "JPG", "JPEG", "PNG", "GIF")
        )
        if (selectedPath != null) {
            onImagePicked(selectedPath)
        }
    }
}

actual fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return formatter.format(date)
}

