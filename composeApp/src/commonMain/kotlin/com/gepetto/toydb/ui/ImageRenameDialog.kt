package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysTextColor
import club.gepetto.composeutils.image.GcImage
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.resolveBitmapUri
import com.gepetto.toydb.utils.formatTimestamp
import okio.FileSystem
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

data class ImageDetail(
    val index: Int,
    val originalName: String,
    var currentName: String,
    val size: Long,
    val timestamp: Long,
    val exists: Boolean,
    val absolutePath: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageRenameDialog(
    bitmapsStr: String,
    bitmapsSizeStr: String,
    bitmapsTimeStampStr: String,
    repository: ToyRepository,
    onDismiss: () -> Unit,
    onSave: (newBitmaps: String, newSizes: String, newTimestamps: String) -> Unit
) {
    // Parse files
    val fileList = remember(bitmapsStr) {
        val names = bitmapsStr.split(" ").filter { it.trim().isNotEmpty() }
        val sizes = bitmapsSizeStr.split(" ").filter { it.trim().isNotEmpty() }
        val timestamps = bitmapsTimeStampStr.split(" ").filter { it.trim().isNotEmpty() }
        
        names.mapIndexed { idx, name ->
            val size = sizes.getOrNull(idx)?.toLongOrNull() ?: 0L
            val timestamp = timestamps.getOrNull(idx)?.toLongOrNull() ?: 0L
            val absolutePath = resolveBitmapUri(name)
            val exists = absolutePath != null && FileSystem.SYSTEM.exists(absolutePath.toPath())
            
            ImageDetail(
                index = idx,
                originalName = name,
                currentName = name,
                size = size,
                timestamp = timestamp,
                exists = exists,
                absolutePath = absolutePath
            )
        }
    }

    // Local mutable state for editing names
    val editedNames = remember { mutableStateMapOf<Int, String>().apply {
        fileList.forEach { put(it.index, it.currentName) }
    } }

    // Validation helper
    fun isValidFilename(name: String): Boolean {
        if (name.isBlank()) return false
        if (name.contains(" ")) return false
        val invalidChars = listOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
        return !name.any { it in invalidChars }
    }

    val isAllValid = editedNames.values.all { isValidFilename(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_image_filenames_title)) },
        text = {
            if (fileList.isEmpty()) {
                Text(stringResource(Res.string.file_status_missing))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                ) {
                    itemsIndexed(fileList) { idx, item ->
                        val currentEditName = editedNames[item.index] ?: ""
                        val isValid = isValidFilename(currentEditName)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(GcSpacing.Small).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail / Placeholder
                                if (item.exists && item.absolutePath != null) {
                                    GcImage(
                                        imageFile = item.absolutePath,
                                        modifier = Modifier.size(60.dp),
                                        contentDescription = item.originalName
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(60.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = stringResource(Res.string.file_status_missing),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(GcSpacing.Standard))

                                // Filename TextField and Details
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = currentEditName,
                                        onValueChange = { editedNames[item.index] = it },
                                        label = { Text(stringResource(Res.string.filename_label)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        isError = !isValid,
                                        singleLine = true
                                    )
                                    if (!isValid) {
                                        Text(
                                            text = stringResource(Res.string.invalid_filename_error),
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Details
                                    val sizeKb = item.size / 1024.0
                                    val sizeText = if (item.size > 0) "${(sizeKb * 10).toLong() / 10.0} KB" else "0 B"
                                    val dateText = if (item.timestamp > 0) formatTimestamp(item.timestamp) else ""
                                    
                                    Text(
                                        text = "${stringResource(Res.string.file_size_label)}: $sizeText | ${stringResource(Res.string.file_date_label)}: $dateText",
                                        fontSize = 11.sp,
                                        color = sysTextColor().copy(alpha = 0.6f)
                                    )
                                    if (!item.exists) {
                                        Text(
                                            text = stringResource(Res.string.file_status_missing),
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isAllValid) return@Button
                    
                    val updatedNames = mutableListOf<String>()
                    val updatedSizes = mutableListOf<String>()
                    val updatedTimestamps = mutableListOf<String>()
                    
                    fileList.forEach { item ->
                        val newName = editedNames[item.index] ?: item.originalName
                        
                        // Handle physical rename if exists
                        if (newName != item.originalName && item.exists && item.absolutePath != null) {
                            try {
                                val oldPath = item.absolutePath.toPath()
                                val newPath = oldPath.parent!!.div(newName)
                                FileSystem.SYSTEM.atomicMove(oldPath, newPath)
                            } catch (e: Exception) {
                                club.gepetto.GcLog.e("ImageRenameDialog", "Failed to rename ${item.originalName} to $newName: ${e.message}", e)
                            }
                        }
                        
                        updatedNames.add(newName)
                        updatedSizes.add(item.size.toString())
                        updatedTimestamps.add(item.timestamp.toString())
                    }
                    
                    onSave(
                        updatedNames.joinToString(" "),
                        updatedSizes.joinToString(" "),
                        updatedTimestamps.joinToString(" ")
                    )
                },
                enabled = isAllValid && fileList.isNotEmpty()
            ) {
                Text(stringResource(Res.string.save_changes_btn))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
