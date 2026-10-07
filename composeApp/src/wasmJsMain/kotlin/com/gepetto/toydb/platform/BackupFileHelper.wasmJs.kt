package com.gepetto.toydb.platform

import okio.Path
import okio.Sink

actual object BackupFileHelper {
    actual suspend fun saveBackup(
        suggestedName: String,
        dialogTitle: String,
        write: suspend (Sink) -> Unit
    ): BackupSaveResult = BackupSaveResult.Failed("not supported on web")

    actual suspend fun openBackup(dialogTitle: String): BackupOpenResult =
        BackupOpenResult.Failed("not supported on web")

    actual fun release(path: Path) {
        // No-op on web
    }
}
