package com.gepetto.toydb.platform

import okio.Path
import okio.Sink

const val ANDROID_BACKUP_FILE_NAME = "toy_collection_backup.zip"

sealed interface BackupSaveResult {
    data class Saved(val location: String) : BackupSaveResult   // shown to the user
    data object Cancelled : BackupSaveResult
    data class Failed(val message: String) : BackupSaveResult
}

sealed interface BackupOpenResult {
    /** [isTemporary] = true: call [BackupFileHelper.release] when done. */
    data class Opened(val path: Path, val isTemporary: Boolean) : BackupOpenResult
    data object Cancelled : BackupOpenResult
    data object NotFound : BackupOpenResult      // Android only
    data object NoSpace : BackupOpenResult       // Android only (D10)
    data class Failed(val message: String) : BackupOpenResult
}

expect object BackupFileHelper {
    /**
     * Gets a destination, then calls [write] with a sink to it.
     * [write] is called only after a destination is chosen (so the UI shows its progress dialog only then).
     * [write] runs in the caller's coroutine. It can suspend (it waits for CollectionWriteLock).
     */
    suspend fun saveBackup(suggestedName: String, dialogTitle: String, write: suspend (Sink) -> Unit): BackupSaveResult

    /** Gets a backup file that can be read with random access. */
    suspend fun openBackup(dialogTitle: String): BackupOpenResult

    fun release(path: Path)
}
