package com.gepetto.toydb.service

import com.gepetto.toydb.database.ToyDatabase

/** SFTP needs raw TCP sockets, which browsers do not allow. */
class WebSftpService : SftpService {
    override val isSupported: Boolean = false

    private fun <T> unsupported(): Result<T> =
        Result.failure(UnsupportedOperationException("SFTP is not available on web"))

    override suspend fun testConnection(
        config: SftpConfig,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean
    ): Result<Unit> = unsupported()

    override suspend fun calculateUploadPlan(
        config: SftpConfig, db: ToyDatabase,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean
    ): Result<List<SyncAction>> = unsupported()

    override suspend fun calculateDownloadPlan(
        config: SftpConfig, db: ToyDatabase,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean
    ): Result<List<SyncAction>> = unsupported()

    override suspend fun uploadData(
        config: SftpConfig, db: ToyDatabase,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean,
        selectedFiles: Set<String>?, onProgress: (String, Float) -> Unit
    ): Result<Unit> = unsupported()

    override suspend fun downloadData(
        config: SftpConfig, db: ToyDatabase,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean,
        selectedFiles: Set<String>?, onProgress: (String, Float) -> Unit
    ): Result<Unit> = unsupported()
}
