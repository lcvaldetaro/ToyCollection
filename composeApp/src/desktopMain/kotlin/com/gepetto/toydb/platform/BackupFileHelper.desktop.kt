package com.gepetto.toydb.platform

import club.gepetto.GcLog
import com.gepetto.toydb.utils.selectFileDialog
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.SwingUtilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.sink

actual object BackupFileHelper {
    actual suspend fun saveBackup(
        suggestedName: String,
        dialogTitle: String,
        write: suspend (Sink) -> Unit
    ): BackupSaveResult = withContext(Dispatchers.IO) {
        var chosenDir: String? = null
        var chosenFile: String? = null
        val runDialog = {
            val dialog = FileDialog(null as Frame?, dialogTitle, FileDialog.SAVE)
            dialog.file = suggestedName
            dialog.isVisible = true
            chosenDir = dialog.directory
            chosenFile = dialog.file
            dialog.dispose()
        }
        if (SwingUtilities.isEventDispatchThread()) {
            runDialog()
        } else {
            SwingUtilities.invokeAndWait(runDialog)
        }

        val dir = chosenDir
        val file = chosenFile
        if (dir == null || file == null) {
            return@withContext BackupSaveResult.Cancelled
        }

        val finalFileName = if (file.endsWith(".zip", ignoreCase = true)) file else "$file.zip"
        val target = File(dir, finalFileName)
        val partial = File(dir, "$finalFileName.partial")
        if (partial.exists()) {
            partial.delete()
        }

        try {
            val sink = partial.sink()
            try {
                write(sink)
            } finally {
                runCatching { sink.close() }
            }

            try {
                Files.move(
                    partial.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (e: AtomicMoveNotSupportedException) {
                Files.move(
                    partial.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            BackupSaveResult.Saved(target.absolutePath)
        } catch (c: CancellationException) {
            if (partial.exists()) {
                partial.delete()
            }
            throw c
        } catch (e: Throwable) {
            GcLog.e("BackupFileHelper: Error saving backup: ${e.message}")
            if (partial.exists()) {
                partial.delete()
            }
            BackupSaveResult.Failed(e.message ?: "Unknown error")
        }
    }

    actual suspend fun openBackup(dialogTitle: String): BackupOpenResult = withContext(Dispatchers.IO) {
        val selectedPath = selectFileDialog(dialogTitle, listOf("zip", "ZIP"))
            ?: return@withContext BackupOpenResult.Cancelled
        BackupOpenResult.Opened(selectedPath.toPath(), isTemporary = false)
    }

    actual fun release(path: Path) {
        // No-op for non-temporary paths on desktop
    }
}
