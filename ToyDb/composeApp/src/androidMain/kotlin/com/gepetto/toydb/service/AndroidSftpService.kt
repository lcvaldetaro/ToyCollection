package com.gepetto.toydb.service

import club.gepetto.GcLog
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.security.PublicKey

class AndroidSftpService(private val context: android.content.Context) : SftpService {
    override val isSupported: Boolean = true

    private val ALLOWED_SYNC_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp",
        "wav", "mp3", "ogg", "m4a", "flac"
    )

    private fun getFileNameFromPath(path: String): String {
        val lastSlash = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return if (lastSlash >= 0) {
            path.substring(lastSlash + 1)
        } else {
            path
        }
    }

    private fun isAllowedFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ALLOWED_SYNC_EXTENSIONS.contains(ext)
    }

    private fun connectClient(
        config: SftpConfig,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean
    ): SSHClient {
        val client = SSHClient()
        client.connectTimeout = 15000
        client.timeout = 30000
        client.addHostKeyVerifier(object : HostKeyVerifier {
            override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                val fingerprint = SecurityUtils.getFingerprint(key)
                if (config.approvedFingerprints.contains(fingerprint)) {
                    return true
                }
                GcLog.d("AndroidSftpService", "Host key unverified for $hostname. Prompting user...")
                return kotlinx.coroutines.runBlocking {
                    onHostKeyUnverified(hostname, port, fingerprint)
                }
            }

            override fun findExistingAlgorithms(hostname: String, port: Int): List<String> {
                return emptyList()
            }
        })
        client.connect(config.host, config.port)
        if (config.authType == "password") {
            client.authPassword(config.username, config.password)
        } else {
            val keyFile = File(config.keyPath)
            val keyProvider = if (config.keyPassphrase.isNotEmpty()) {
                client.loadKeys(keyFile.absolutePath, config.keyPassphrase)
            } else {
                client.loadKeys(keyFile.absolutePath)
            }
            client.authPublickey(config.username, keyProvider)
        }
        return client
    }

    private fun safeMkdirs(sftp: SFTPClient, path: String) {
        try {
            sftp.stat(path)
        } catch (e: Exception) {
            try {
                sftp.mkdirs(path)
            } catch (ex: Exception) {
                GcLog.e("AndroidSftpService", "Could not create directory $path: ${ex.message}")
            }
        }
    }

    override suspend fun testConnection(
        config: SftpConfig,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            GcLog.d("AndroidSftpService", "Testing connection to ${config.host}:${config.port}...")
            val client = connectClient(config, onHostKeyUnverified)
            try {
                val sftp = client.newSFTPClient()
                sftp.close()
            } finally {
                client.disconnect()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            GcLog.e("AndroidSftpService", "Connection test failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun calculateUploadPlan(
        config: SftpConfig,
        db: ToyDatabase,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean
    ): Result<List<SyncAction>> = withContext(Dispatchers.IO) {
        try {
            val plan = mutableListOf<SyncAction>()
            val repository = ToyRepository(db)
            val localDataDir = repository.getDataPathSetting()
                ?: return@withContext Result.failure(Exception("Data directory is not configured."))

            // JSON files to upload
            plan.add(SyncAction("carmaker.json", "Upload", "Overwrite (JSON)"))
            plan.add(SyncAction("category_settings.json", "Upload", "Overwrite (JSON)"))
            val categories = repository.getCategorySettings()
            categories.forEach { cat ->
                plan.add(SyncAction("${cat.imagePrefix}list.json", "Upload", "Overwrite (JSON)"))
            }

            val client = connectClient(config, onHostKeyUnverified)
            try {
                val sftp = client.newSFTPClient()
                try {
                    val allLocalFiles = mutableListOf<okio.Path>()
                    val localDataPath = localDataDir.toPath()
                    if (FileSystem.SYSTEM.exists(localDataPath)) {
                        allLocalFiles.addAll(
                            FileSystem.SYSTEM.list(localDataPath).filter { 
                                val isRegular = FileSystem.SYSTEM.metadata(it).isRegularFile
                                isRegular && (isAllowedFile(it.name) || it.name.lowercase().endsWith(".html"))
                            }
                        )
                    }

                    val remoteFiles = try {
                        sftp.ls(config.remoteDir)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val remoteFileMap = remoteFiles.associateBy { getFileNameFromPath(it.path) }

                    allLocalFiles.forEach { localFilePath ->
                        val fileName = localFilePath.name
                        if (fileName.endsWith(".json", ignoreCase = true)) return@forEach // Skip JSONs handled above
                        
                        val localMeta = FileSystem.SYSTEM.metadata(localFilePath)
                        val localSize = localMeta.size ?: 0L
                        val localMtimeSec = (localMeta.lastModifiedAtMillis ?: 0L) / 1000L

                        val remoteInfo = remoteFileMap[fileName]

                        if (remoteInfo == null) {
                            plan.add(SyncAction(fileName, "Upload", "New File"))
                        } else {
                            val remoteSize = remoteInfo.attributes.size
                            val remoteMtimeSec = remoteInfo.attributes.mtime
                            if (localSize != remoteSize) {
                                plan.add(SyncAction(fileName, "Upload", "Size Changed"))
                            } else if (localMtimeSec > remoteMtimeSec) {
                                plan.add(SyncAction(fileName, "Upload", "Newer Timestamp"))
                            }
                        }
                    }
                } finally {
                    sftp.close()
                }
            } finally {
                client.disconnect()
            }
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateDownloadPlan(
        config: SftpConfig,
        db: ToyDatabase,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean
    ): Result<List<SyncAction>> = withContext(Dispatchers.IO) {
        try {
            val plan = mutableListOf<SyncAction>()
            val repository = ToyRepository(db)
            val localDataDir = repository.getDataPathSetting()
                ?: return@withContext Result.failure(Exception("Data directory is not configured."))

            val client = connectClient(config, onHostKeyUnverified)
            try {
                val sftp = client.newSFTPClient()
                try {
                    val remoteFiles = sftp.ls(config.remoteDir)
                    val localDataPath = localDataDir.toPath()
                    
                    remoteFiles.forEach { remoteFile ->
                        if (!remoteFile.isRegularFile) return@forEach
                        
                        val fileName = getFileNameFromPath(remoteFile.path)
                        val remoteSize = remoteFile.attributes.size
                        val remoteMtimeSec = remoteFile.attributes.mtime
                        val localFilePath = localDataPath.div(fileName)

                        if (fileName.endsWith(".json", ignoreCase = true)) {
                            plan.add(SyncAction(fileName, "Download", "Overwrite (JSON)"))
                        } else if (isAllowedFile(fileName) || fileName.lowercase().endsWith(".html")) {
                            if (!FileSystem.SYSTEM.exists(localFilePath)) {
                                plan.add(SyncAction(fileName, "Download", "New File"))
                            } else {
                                val localMeta = FileSystem.SYSTEM.metadata(localFilePath)
                                val localSize = localMeta.size ?: 0L
                                val localMtimeSec = (localMeta.lastModifiedAtMillis ?: 0L) / 1000L
                                if (localSize != remoteSize) {
                                    plan.add(SyncAction(fileName, "Download", "Size Changed"))
                                } else if (remoteMtimeSec > localMtimeSec) {
                                    plan.add(SyncAction(fileName, "Download", "Newer Timestamp"))
                                }
                            }
                        }
                    }
                } finally {
                    sftp.close()
                }
            } finally {
                client.disconnect()
            }
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadData(
        config: SftpConfig,
        db: ToyDatabase,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean,
        selectedFiles: Set<String>?,
        onProgress: (status: String, progress: Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "ToyDb:SftpUpload")
        wakeLock.acquire(15 * 60 * 1000L) // 15 mins max timeout
        val activity = context as? android.app.Activity
        activity?.runOnUiThread {
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        try {
            val repository = ToyRepository(db)
            val localDataDir = repository.getDataPathSetting()
                ?: return@withContext Result.failure(Exception("Data directory is not configured."))

            onProgress("Exporting database to local files...", 0.1f)
            val filesToWrite = mutableListOf<Pair<String, String>>()
            if (selectedFiles == null || selectedFiles.contains("carmaker.json")) {
                filesToWrite.add("carmaker.json" to ImportExportService.exportMakers(db))
            }
            if (selectedFiles == null || selectedFiles.contains("category_settings.json")) {
                filesToWrite.add("category_settings.json" to ImportExportService.exportCategorySettings(db))
            }
            val categories = repository.getCategorySettings()
            categories.forEach { cat ->
                val fileName = "${cat.imagePrefix}list.json"
                if (selectedFiles == null || selectedFiles.contains(fileName)) {
                    filesToWrite.add(fileName to ImportExportService.exportToys(db, cat.category))
                }
            }

            val localDataPath = localDataDir.toPath()
            if (!FileSystem.SYSTEM.exists(localDataPath)) {
                FileSystem.SYSTEM.createDirectories(localDataPath)
            }
            for ((fileName, jsonContent) in filesToWrite) {
                val path = localDataPath.div(fileName)
                FileSystem.SYSTEM.write(path) { writeUtf8(jsonContent) }
            }

            onProgress("Connecting to SFTP server...", 0.3f)
            val client = connectClient(config, onHostKeyUnverified)
            try {
                val sftp = client.newSFTPClient()
                try {
                    onProgress("Creating remote directories...", 0.4f)
                    safeMkdirs(sftp, config.remoteDir)

                    onProgress("Uploading JSON configuration files...", 0.5f)
                    for ((fileName, _) in filesToWrite) {
                        val localFile = localDataPath.div(fileName).toNioPath().toFile()
                        val remoteFile = if (config.remoteDir.endsWith("/")) "${config.remoteDir}$fileName" else "${config.remoteDir}/$fileName"
                        sftp.put(net.schmizz.sshj.xfer.FileSystemFile(localFile), remoteFile)
                    }

                    onProgress("Uploading media and HTML files...", 0.7f)
                    val allLocalFiles = mutableListOf<okio.Path>()
                    if (FileSystem.SYSTEM.exists(localDataPath)) {
                        allLocalFiles.addAll(
                            FileSystem.SYSTEM.list(localDataPath).filter { 
                                val name = it.name
                                val isRegular = FileSystem.SYSTEM.metadata(it).isRegularFile
                                isRegular && !name.endsWith(".json", ignoreCase = true) && (isAllowedFile(name) || name.lowercase().endsWith(".html"))
                            }
                        )
                    }

                    val remoteFiles = try {
                        sftp.ls(config.remoteDir)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val remoteFileMap = remoteFiles.associateBy { getFileNameFromPath(it.path) }

                    val filesToUpload = if (selectedFiles != null) {
                        allLocalFiles.filter { selectedFiles.contains(it.name) }
                    } else {
                        allLocalFiles
                    }

                    filesToUpload.forEachIndexed { index, localFilePath ->
                        val fileName = localFilePath.name
                        val localFile = localFilePath.toNioPath().toFile()
                        val localMeta = FileSystem.SYSTEM.metadata(localFilePath)
                        val localSize = localMeta.size ?: 0L
                        val localMtimeSec = (localMeta.lastModifiedAtMillis ?: 0L) / 1000L

                        val remoteInfo = remoteFileMap[fileName]
                        val remoteFile = if (config.remoteDir.endsWith("/")) "${config.remoteDir}$fileName" else "${config.remoteDir}/$fileName"

                        val shouldUpload = if (remoteInfo == null) {
                            true
                        } else {
                            val remoteSize = remoteInfo.attributes.size
                            val remoteMtimeSec = remoteInfo.attributes.mtime
                            if (localSize != remoteSize) {
                                true
                            } else {
                                localMtimeSec > remoteMtimeSec
                            }
                        }

                        if (shouldUpload) {
                            sftp.put(net.schmizz.sshj.xfer.FileSystemFile(localFile), remoteFile)
                        }
                        
                        val progressVal = 0.7f + (0.3f * (index + 1) / filesToUpload.size.coerceAtLeast(1))
                        onProgress("Uploading media and HTML files (${index + 1}/${filesToUpload.size})...", progressVal)
                    }

                } finally {
                    sftp.close()
                }
            } finally {
                client.disconnect()
            }
            onProgress("Upload Complete!", 1.0f)
            Result.success(Unit)
        } catch (e: Exception) {
            GcLog.e("AndroidSftpService", "Upload failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            activity?.runOnUiThread {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override suspend fun downloadData(
        config: SftpConfig,
        db: ToyDatabase,
        onHostKeyUnverified: suspend (hostname: String, port: Int, fingerprint: String) -> Boolean,
        selectedFiles: Set<String>?,
        onProgress: (status: String, progress: Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "ToyDb:SftpDownload")
        wakeLock.acquire(15 * 60 * 1000L) // 15 mins max timeout
        val activity = context as? android.app.Activity
        activity?.runOnUiThread {
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        try {
            val repository = ToyRepository(db)
            val localDataDir = repository.getDataPathSetting()
                ?: return@withContext Result.failure(Exception("Data directory is not configured."))

            val localDataPath = localDataDir.toPath()
            if (!FileSystem.SYSTEM.exists(localDataPath)) {
                FileSystem.SYSTEM.createDirectories(localDataPath)
            }

            onProgress("Connecting to SFTP server...", 0.1f)
            val client = connectClient(config, onHostKeyUnverified)
            try {
                val sftp = client.newSFTPClient()
                try {
                    onProgress("Downloading configurations and media...", 0.3f)
                    val remoteFiles = sftp.ls(config.remoteDir)

                    // 1. Download JSON configurations
                    val jsonFiles = remoteFiles.filter { getFileNameFromPath(it.path).endsWith(".json", ignoreCase = true) }
                    jsonFiles.forEach { rf ->
                        val fileName = getFileNameFromPath(rf.path)
                        if (selectedFiles == null || selectedFiles.contains(fileName)) {
                            val localFile = localDataPath.div(fileName).toNioPath().toFile()
                            val remoteFile = if (config.remoteDir.endsWith("/")) "${config.remoteDir}$fileName" else "${config.remoteDir}/$fileName"
                            sftp.get(remoteFile, net.schmizz.sshj.xfer.FileSystemFile(localFile))
                        }
                    }

                    // 2. Download media files
                    val allowedRemoteImages = remoteFiles.filter { it.isRegularFile && (isAllowedFile(getFileNameFromPath(it.path)) || getFileNameFromPath(it.path).lowercase().endsWith(".html")) }

                    val imagesToDownload = if (selectedFiles != null) {
                        allowedRemoteImages.filter { selectedFiles.contains(getFileNameFromPath(it.path)) }
                    } else {
                        allowedRemoteImages
                    }

                    imagesToDownload.forEachIndexed { index, remoteImg ->
                        val fileName = getFileNameFromPath(remoteImg.path)
                        val remoteSize = remoteImg.attributes.size
                        val remoteMtimeSec = remoteImg.attributes.mtime
                        val remoteFile = if (config.remoteDir.endsWith("/")) "${config.remoteDir}$fileName" else "${config.remoteDir}/$fileName"
                        val localFilePath = localDataPath.div(fileName)
                        val localFile = localFilePath.toNioPath().toFile()

                        val shouldDownload = if (!FileSystem.SYSTEM.exists(localFilePath)) {
                            true
                        } else {
                            val localMeta = FileSystem.SYSTEM.metadata(localFilePath)
                            val localSize = localMeta.size ?: 0L
                            val localMtimeSec = (localMeta.lastModifiedAtMillis ?: 0L) / 1000L
                            if (localSize != remoteSize) {
                                true
                            } else {
                                remoteMtimeSec > localMtimeSec
                            }
                        }

                        if (shouldDownload) {
                            sftp.get(remoteFile, net.schmizz.sshj.xfer.FileSystemFile(localFile))
                        }
                        
                        val progressVal = 0.5f + (0.3f * (index + 1) / imagesToDownload.size.coerceAtLeast(1))
                        onProgress("Downloading media (${index + 1}/${imagesToDownload.size})...", progressVal)
                    }

                    onProgress("Importing downloaded data to database...", 0.8f)

                    fun readLocalJson(fileName: String): String? {
                        val file = localDataPath.div(fileName)
                        return if (FileSystem.SYSTEM.exists(file)) {
                            FileSystem.SYSTEM.read(file) { readUtf8() }
                        } else null
                    }

                    // 1. Category Settings
                    if (selectedFiles == null || selectedFiles.contains("category_settings.json")) {
                        val catSettingsContent = readLocalJson("category_settings.json")
                        if (catSettingsContent != null) {
                            ImportExportService.importCategorySettings(db, catSettingsContent)
                        }
                    }

                    // 2. Makers
                    if (selectedFiles == null || selectedFiles.contains("carmaker.json") || selectedFiles.contains("makers.json")) {
                        val makersContent = readLocalJson("carmaker.json") ?: readLocalJson("makers.json")
                        if (makersContent != null) {
                            ImportExportService.importMakers(db, makersContent)
                        }
                    }

                    // 3. Toys
                    val activeCategories = repository.getCategorySettings()
                    activeCategories.forEach { cat ->
                        val fileName = "${cat.imagePrefix}list.json"
                        if (selectedFiles == null || selectedFiles.contains(fileName) || selectedFiles.contains("${cat.category}s.json") || selectedFiles.contains("${cat.category}list.json") || selectedFiles.contains("${cat.category}.json")) {
                            val content = readLocalJson(fileName)
                                ?: readLocalJson("${cat.category}s.json")
                                ?: readLocalJson("${cat.category}list.json")
                                ?: readLocalJson("${cat.category}.json")
                            if (content != null) {
                                ImportExportService.importToys(db, cat.category, content)
                            }
                        }
                    }

                } finally {
                    sftp.close()
                }
            } finally {
                client.disconnect()
            }
            onProgress("Download Complete!", 1.0f)
            Result.success(Unit)
        } catch (e: Exception) {
            GcLog.e("AndroidSftpService", "Download failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            activity?.runOnUiThread {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
