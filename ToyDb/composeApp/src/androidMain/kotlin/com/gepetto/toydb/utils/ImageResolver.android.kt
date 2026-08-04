package com.gepetto.toydb.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

actual fun resolveImageUri(prefix: String, refNum: Int): String? {
    val extensions = listOf("jpg", "jpeg", "png", "gif", "webp", "JPG", "PNG", "GIF")
    val possibleDirs = mutableListOf<File>()
    
    ImageResolverConfig.imagesPath?.let { customPath ->
        val customDir = File(customPath)
        if (customDir.exists() && customDir.isDirectory) {
            possibleDirs.add(customDir)
        }
    }
    
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
    
    val dir = possibleDirs.find { it.exists() && it.isDirectory } ?: return null
    val file = File(dir, filename.trim())
    return if (file.exists()) file.absolutePath else null
}

actual fun selectDirectoryDialog(title: String): String? = null

actual fun selectFileDialog(title: String, allowedExtensions: List<String>): String? = null

actual fun isDesktopPlatform(): Boolean = false

fun copyUriToTempPicker(context: Context, uri: Uri): String? {
    try {
        val contentResolver = context.contentResolver
        var fileName = "temp_picker_image"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val name = it.getString(nameIndex)
                    if (!name.isNullOrEmpty()) {
                        fileName = name
                    }
                }
            }
        }
        
        if (!fileName.contains(".")) {
            fileName += ".jpg"
        }
        
        val tempDir = File(context.filesDir, "tmp_picker")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        
        val tempFile = File(tempDir, fileName)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile.absolutePath
    } catch (e: Exception) {
        club.gepetto.GcLog.e("AndroidImageResolver", "Failed to copy URI to persistent temp storage: ${e.message}", e)
    }
    return null
}

@Composable
actual fun rememberImagePicker(onImagePicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val copiedPath = copyUriToTempPicker(context, uri)
            if (copiedPath != null) {
                onImagePicked(copiedPath)
            }
        }
    }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

actual fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return formatter.format(date)
}

