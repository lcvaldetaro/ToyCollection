package com.gepetto.toydb.service

import com.gepetto.toydb.database.ToyDatabase

data class SftpConfig(
    val host: String,
    val port: Int,
    val username: String,
    val authType: String, // "password" or "key"
    val password: String,
    val keyPath: String,
    val keyPassphrase: String,
    val remoteDir: String,
    val approvedFingerprints: List<String>
)

data class SyncAction(
    val filename: String,
    val type: String, // "Upload" or "Download"
    val reason: String // "New File", "Size Changed", "Newer Timestamp", "Overwrite (JSON)"
)

interface SftpService {
    val isSupported: Boolean
    
    suspend fun testConnection(
        config: SftpConfig,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean
    ): Result<Unit>

    suspend fun calculateUploadPlan(
        config: SftpConfig,
        db: ToyDatabase,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean
    ): Result<List<SyncAction>>

    suspend fun calculateDownloadPlan(
        config: SftpConfig,
        db: ToyDatabase,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean
    ): Result<List<SyncAction>>
    
    suspend fun uploadData(
        config: SftpConfig, 
        db: ToyDatabase, 
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean,
        onProgress: (status: String, progress: Float) -> Unit
    ): Result<Unit>
    
    suspend fun downloadData(
        config: SftpConfig, 
        db: ToyDatabase, 
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean,
        onProgress: (status: String, progress: Float) -> Unit
    ): Result<Unit>
}
