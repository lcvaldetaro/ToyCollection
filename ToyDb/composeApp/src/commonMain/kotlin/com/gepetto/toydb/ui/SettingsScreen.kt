package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.GcLog
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.CategorySetting
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.service.ImportExportService
import okio.FileSystem
import okio.Path.Companion.toPath
import com.gepetto.toydb.utils.selectDirectoryDialog
import com.gepetto.toydb.utils.isDesktopPlatform
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gepetto.toydb.service.SftpService
import com.gepetto.toydb.service.SftpConfig
import com.gepetto.toydb.service.SyncAction
import androidx.compose.foundation.border
import kotlinx.coroutines.CompletableDeferred
import com.gepetto.toydb.utils.selectFileDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    db: ToyDatabase,
    sftpService: SftpService,
    currentTheme: Int = 0,
    onThemeChanged: (Int) -> Unit = {},
    onCategoriesChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { ToyRepository(db) }
    var statusText by remember { mutableStateOf("Ready") }
    val coroutineScope = rememberCoroutineScope()

    // SFTP Config States
    var sftpHost by remember { mutableStateOf(repository.getSftpHostSetting() ?: "") }
    var sftpPort by remember { mutableStateOf(repository.getSftpPortSetting().toString()) }
    var sftpUsername by remember { mutableStateOf(repository.getSftpUsernameSetting() ?: "") }
    var sftpAuthType by remember { mutableStateOf(if (!isDesktopPlatform()) "password" else (repository.getSftpAuthTypeSetting() ?: "password")) }
    var sftpPassword by remember { mutableStateOf(repository.getSftpPasswordSetting() ?: "") }
    var sftpKeyPath by remember { mutableStateOf(repository.getSftpKeyPathSetting() ?: "") }
    var sftpKeyPassphrase by remember { mutableStateOf(repository.getSftpKeyPassphraseSetting() ?: "") }
    var sftpRemoteDir by remember { mutableStateOf(repository.getSftpRemoteDirSetting() ?: "") }
    var sftpApprovedFingerprints by remember { mutableStateOf(repository.getSftpApprovedFingerprintsSetting() ?: "") }

    var isTestingSftp by remember { mutableStateOf(false) }
    var sftpSyncProgress by remember { mutableStateOf(0.0f) }
    var isSftpSyncing by remember { mutableStateOf(false) }
    var showTestSuccessDialog by remember { mutableStateOf(false) }
    var showTestErrorDialog by remember { mutableStateOf(false) }
    var testErrorMsg by remember { mutableStateOf("") }
    var showSyncConfirmDialog by remember { mutableStateOf(false) }
    val proposedSftpActions = remember { mutableStateListOf<SyncAction>() }
    val selectedSftpActions = remember { mutableStateMapOf<String, Boolean>() }
    var syncDirection by remember { mutableStateOf("Upload") }

    // Host Fingerprint verification state
    class HostKeyVerification(
        val hostname: String,
        val port: Int,
        val fingerprint: String,
        val deferred: CompletableDeferred<Boolean>
    )
    var activeVerification by remember { mutableStateOf<HostKeyVerification?>(null) }

    fun buildSftpConfig(): SftpConfig {
        return SftpConfig(
            host = sftpHost.trim(),
            port = sftpPort.toIntOrNull() ?: 22,
            username = sftpUsername.trim(),
            authType = sftpAuthType,
            password = sftpPassword,
            keyPath = sftpKeyPath.trim(),
            keyPassphrase = sftpKeyPassphrase,
            remoteDir = sftpRemoteDir.trim(),
            approvedFingerprints = sftpApprovedFingerprints.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
    }

    val onHostKeyUnverified: suspend (String, Int, String) -> Boolean = { hostname, port, fingerprint ->
        val deferred = CompletableDeferred<Boolean>()
        activeVerification = HostKeyVerification(hostname, port, fingerprint, deferred)
        val accepted = deferred.await()
        if (accepted) {
            withContext(Dispatchers.IO) {
                repository.addSftpApprovedFingerprint(fingerprint)
            }
            sftpApprovedFingerprints = repository.getSftpApprovedFingerprintsSetting() ?: ""
        }
        accepted
    }

    // Dynamic Category List state
    var categoriesList by remember { mutableStateOf(repository.getCategorySettings()) }

    // Category Dialog State
    var showCategoryDialog by remember { mutableStateOf(false) }
    var dialogIsEditMode by remember { mutableStateOf(false) }
    var dialogCategoryKey by remember { mutableStateOf("") }
    var dialogCategoryLabel by remember { mutableStateOf("") }
    var dialogCategoryPrefix by remember { mutableStateOf("") }
    var dialogErrorText by remember { mutableStateOf("") }

    // Delete Category confirmation state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategorySetting?>(null) }

    // Export success dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var exportDirectoryPath by remember { mutableStateOf("") }
    val exportedFiles = remember { mutableStateListOf<String>() }

    // Import success dialog state
    var showImportDialog by remember { mutableStateOf(false) }
    val importCountsMap = remember { mutableStateMapOf<String, Int>() }

    // Paths state
    var customImagesPath by remember { mutableStateOf(repository.getImagesPathSetting()) }
    var customImportExportPath by remember { mutableStateOf(repository.getImportExportPathSetting()) }

    fun readJsonFile(fileName: String, dirPath: String? = null): String? {
        val paths = if (dirPath != null) {
            listOf("$dirPath/$fileName".toPath())
        } else {
            listOf(
                "/Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb/json/$fileName".toPath(),
                "ToyDb/json/$fileName".toPath(),
                "json/$fileName".toPath()
            )
        }
        for (path in paths) {
            try {
                if (FileSystem.SYSTEM.exists(path)) {
                    return FileSystem.SYSTEM.read(path) { readUtf8() }
                }
            } catch (e: Exception) {
                GcLog.e("SettingsScreen", "Error reading $path: ${e.message}", e)
            }
        }
        return null
    }

    fun writeJsonFile(fileName: String, content: String, dirPath: String? = null): String? {
        val paths = if (dirPath != null) {
            listOf("$dirPath/$fileName".toPath())
        } else {
            listOf(
                "/Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb/json/$fileName".toPath(),
                "ToyDb/json/$fileName".toPath(),
                "json/$fileName".toPath()
            )
        }
        for (path in paths) {
            try {
                // Ensure parent directory exists
                path.parent?.let { parent ->
                    if (!FileSystem.SYSTEM.exists(parent)) {
                        FileSystem.SYSTEM.createDirectories(parent)
                    }
                }
                FileSystem.SYSTEM.write(path) { writeUtf8(content) }
                return path.toString()
            } catch (e: Exception) {
                GcLog.e("SettingsScreen", "Error writing $path: ${e.message}", e)
            }
        }
        return null
    }

    // Category Create/Edit Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = if (dialogIsEditMode) "Edit Category" else "Add New Category",
                    color = sysTextColor()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (dialogErrorText.isNotEmpty()) {
                        Text(dialogErrorText, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    OutlinedTextField(
                        value = dialogCategoryKey,
                        onValueChange = { dialogCategoryKey = it.trim().lowercase() },
                        label = { Text("Category ID (lowercase, unique)*") },
                        placeholder = { Text("e.g. lego") },
                        enabled = !dialogIsEditMode, // Key cannot be edited
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor(),
                            disabledTextColor = sysTextColor().copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = dialogCategoryLabel,
                        onValueChange = { dialogCategoryLabel = it },
                        label = { Text("Category Name (Label)*") },
                        placeholder = { Text("e.g. Lego Sets") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                    OutlinedTextField(
                        value = dialogCategoryPrefix,
                        onValueChange = { dialogCategoryPrefix = it.trim() },
                        label = { Text("Filename Prefix (e.g. leg)*") },
                        placeholder = { Text("e.g. leg") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogCategoryKey.trim().isEmpty() || dialogCategoryLabel.trim().isEmpty() || dialogCategoryPrefix.trim().isEmpty()) {
                            dialogErrorText = "All fields marked with * are required."
                            return@Button
                        }
                        
                        val newSetting = CategorySetting(
                            category = dialogCategoryKey.trim(),
                            label = dialogCategoryLabel.trim(),
                            imagePrefix = dialogCategoryPrefix.trim()
                        )
                        
                        if (dialogIsEditMode) {
                            repository.updateCategorySetting(newSetting)
                        } else {
                            if (categoriesList.any { it.category == newSetting.category }) {
                                dialogErrorText = "Category ID '${newSetting.category}' already exists."
                                return@Button
                            }
                            repository.addCategorySetting(newSetting)
                        }
                        
                        // Reload state & trigger callback
                        categoriesList = repository.getCategorySettings()
                        onCategoriesChanged()
                        showCategoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCategoryDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Category Confirmation Dialog
    if (showDeleteConfirmDialog && categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = sysBackgroundColor(),
            title = { Text("Delete Category", color = sysTextColor()) },
            text = {
                Text(
                    "Are you sure you want to delete category '${cat.label}'?\n\n" +
                    "WARNING: This will permanently delete ALL toys belonging to this category from the database!",
                    color = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteCategorySetting(cat.category)
                        categoriesList = repository.getCategorySettings()
                        onCategoriesChanged()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Category")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Success Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = "Export Successful",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = sysTextColor()
                )
            },
            text = {
                Column {
                    Text("The database records were exported to the following directory:", color = sysTextColor())
                    Spacer(modifier = Modifier.height(GcSpacing.Small))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportDirectoryPath,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(GcSpacing.Small),
                            color = sysTextColor()
                        )
                    }
                    Spacer(modifier = Modifier.height(GcSpacing.Standard))
                    Text("Exported Files:", fontWeight = FontWeight.SemiBold, color = sysTextColor())
                    Spacer(modifier = Modifier.height(4.dp))
                    exportedFiles.forEach { file ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(file, fontSize = 14.sp, color = sysTextColor())
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Import Success Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = "Import Successful",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = sysTextColor()
                )
            },
            text = {
                Column {
                    Text("The database records were imported successfully from the following directory:", color = sysTextColor())
                    Spacer(modifier = Modifier.height(GcSpacing.Small))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = customImportExportPath ?: "Default directory",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(GcSpacing.Small),
                            color = sysTextColor()
                        )
                    }
                    Spacer(modifier = Modifier.height(GcSpacing.Standard))
                    Text("Imported Records:", fontWeight = FontWeight.SemiBold, color = sysTextColor())
                    Spacer(modifier = Modifier.height(4.dp))
                    importCountsMap.forEach { (label, count) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("$label: $count", fontSize = 14.sp, color = sysTextColor())
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showImportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (activeVerification != null) {
        val verification = activeVerification!!
        AlertDialog(
            onDismissRequest = {
                verification.deferred.complete(false)
                activeVerification = null
            },
            containerColor = sysBackgroundColor(),
            title = { Text("Verify Remote Host Fingerprint", color = sysTextColor()) },
            text = {
                Column {
                    Text("The authenticity of host '${verification.hostname}:${verification.port}' can't be established.", color = sysTextColor())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fingerprint:", fontWeight = FontWeight.Bold, color = sysTextColor())
                    Text(verification.fingerprint, style = MaterialTheme.typography.bodySmall, color = sysTextColor())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to continue connecting?", color = sysTextColor())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        verification.deferred.complete(true)
                        activeVerification = null
                    }
                ) {
                    Text("Accept & Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        verification.deferred.complete(false)
                        activeVerification = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTestSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showTestSuccessDialog = false },
            containerColor = sysBackgroundColor(),
            title = { Text("Connection Test Successful", color = sysTextColor(), fontWeight = FontWeight.Bold) },
            text = { Text("The connection to the SFTP server was successfully established.", color = sysTextColor()) },
            confirmButton = {
                Button(onClick = { showTestSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showTestErrorDialog) {
        AlertDialog(
            onDismissRequest = { showTestErrorDialog = false },
            containerColor = sysBackgroundColor(),
            title = { Text("Connection Test Failed", color = sysTextColor(), fontWeight = FontWeight.Bold) },
            text = { Text("Could not connect to the SFTP server:\n\n$testErrorMsg", color = sysTextColor()) },
            confirmButton = {
                Button(onClick = { showTestErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showSyncConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSyncConfirmDialog = false },
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = "Confirm Sync ($syncDirection)",
                    fontWeight = FontWeight.Bold,
                    color = sysTextColor()
                )
            },
            text = {
                Column {
                    Text(
                        text = if (proposedSftpActions.isEmpty()) {
                            if (syncDirection == "Upload") {
                                "All files on the server are up-to-date. No changes detected. Do you want to continue with the upload?"
                            } else {
                                "All local files are up-to-date. No changes detected. Do you want to continue with the download?"
                            }
                        } else {
                            "The following actions will be performed to synchronize with the server:"
                        },
                        color = sysTextColor()
                    )
                    
                    if (proposedSftpActions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 13.dp, end = 13.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            val allSelected = proposedSftpActions.all { selectedSftpActions[it.filename] == true }
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { isChecked ->
                                    proposedSftpActions.forEach { action ->
                                        selectedSftpActions[action.filename] = isChecked
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Select All",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = sysTextColor()
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                .padding(4.dp)
                        ) {
                            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                            androidx.compose.foundation.lazy.LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                items(proposedSftpActions.size) { index ->
                                    val action = proposedSftpActions[index]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedSftpActions[action.filename] ?: true,
                                            onCheckedChange = { isChecked ->
                                                selectedSftpActions[action.filename] = isChecked
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = action.filename,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = sysTextColor(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = action.reason,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (action.reason == "New File") {
                                                MaterialTheme.colorScheme.primary
                                            } else if (action.reason.startsWith("Overwrite")) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.tertiary
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            PlatformScrollbar(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val totalCount = proposedSftpActions.size
                        val selectedCount = proposedSftpActions.count { selectedSftpActions[it.filename] == true }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = GcSpacing.Small),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total files: $totalCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = sysTextColor()
                            )
                            Text(
                                text = "Selected: $selectedCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ Warning: Navigating away from this screen or backgrounding the app during transfer will abort the synchronization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSyncConfirmDialog = false
                        coroutineScope.launch {
                            isSftpSyncing = true
                            val selectedSet = selectedSftpActions.filterValues { it }.keys
                            if (syncDirection == "Upload") {
                                statusText = "Uploading data to SFTP server..."
                                sftpSyncProgress = 0.0f
                                val result = sftpService.uploadData(buildSftpConfig(), db, onHostKeyUnverified, selectedSet) { status, progress ->
                                    statusText = status
                                    sftpSyncProgress = progress
                                }
                                if (result.isSuccess) {
                                    statusText = "Data successfully uploaded to server!"
                                } else {
                                    statusText = "Upload failed: ${result.exceptionOrNull()?.message}"
                                }
                            } else {
                                statusText = "Downloading data from SFTP server..."
                                sftpSyncProgress = 0.0f
                                val result = sftpService.downloadData(buildSftpConfig(), db, onHostKeyUnverified, selectedSet) { status, progress ->
                                    statusText = status
                                    sftpSyncProgress = progress
                                }
                                if (result.isSuccess) {
                                    statusText = "Data successfully downloaded from server!"
                                    onCategoriesChanged()
                                } else {
                                    statusText = "Download failed: ${result.exceptionOrNull()?.message}"
                                }
                            }
                            isSftpSyncing = false
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSyncConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val lazyListState = rememberLazyListState()
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .background(sysBackgroundColor())
                .padding(GcSpacing.Standard)
        ) {
            item {
                Text("Database & System Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
                Spacer(modifier = Modifier.height(GcSpacing.Standard))
            }
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth > 850.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                ) {
                    // Left Column: Configurations
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                    ) {
                        StatusBanner(statusText)
                        if (isSftpSyncing) {
                            SyncWarningBanner()
                        }
                        ThemeSelector(currentTheme, onThemeChanged)
                        if (isDesktopPlatform()) {
                            ImagesDirectorySettings(
                                customImagesPath = customImagesPath,
                                onSelectPath = { selectedDir ->
                                    repository.setImagesPathSetting(selectedDir)
                                    com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = selectedDir
                                    customImagesPath = selectedDir
                                },
                                onClearPath = {
                                    repository.setImagesPathSetting(null)
                                    com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = null
                                    customImagesPath = null
                                }
                            )
                            ImportExportDirectorySettings(
                                customImportExportPath = customImportExportPath,
                                onSelectPath = { selectedDir ->
                                    repository.setImportExportPathSetting(selectedDir)
                                    customImportExportPath = selectedDir
                                },
                                onClearPath = {
                                    repository.setImportExportPathSetting(null)
                                    customImportExportPath = null
                                }
                            )
                        }
                        ImportExportActions(
                                customImportExportPath = customImportExportPath,
                                db = db,
                                onImportComplete = { counts ->
                                    importCountsMap.clear()
                                    importCountsMap.putAll(counts)
                                    showImportDialog = true
                                    statusText = "Import Complete!"
                                },
                                onExportComplete = { path, files ->
                                    exportDirectoryPath = path
                                    exportedFiles.clear()
                                    exportedFiles.addAll(files)
                                    showExportDialog = true
                                    statusText = "Export Complete!"
                                },
                                onSetStatus = { statusText = it },
                                readJsonFile = { name, dir -> readJsonFile(name, dir) },
                                writeJsonFile = { name, content, dir -> writeJsonFile(name, content, dir) }
                            )
                            SftpSettingsCard(
                                host = sftpHost, onHostChange = { sftpHost = it },
                                port = sftpPort, onPortChange = { sftpPort = it },
                                username = sftpUsername, onUsernameChange = { sftpUsername = it },
                                authType = sftpAuthType, onAuthTypeChange = { sftpAuthType = it },
                                password = sftpPassword, onPasswordChange = { sftpPassword = it },
                                keyPath = sftpKeyPath, onKeyPathChange = { sftpKeyPath = it },
                                keyPassphrase = sftpKeyPassphrase, onKeyPassphraseChange = { sftpKeyPassphrase = it },
                                remoteDir = sftpRemoteDir, onRemoteDirChange = { sftpRemoteDir = it },
                                onSave = {
                                    repository.setSftpHostSetting(sftpHost)
                                    repository.setSftpPortSetting(sftpPort.toIntOrNull() ?: 22)
                                    repository.setSftpUsernameSetting(sftpUsername)
                                    repository.setSftpAuthTypeSetting(sftpAuthType)
                                    repository.setSftpPasswordSetting(sftpPassword)
                                    repository.setSftpKeyPathSetting(sftpKeyPath)
                                    repository.setSftpKeyPassphraseSetting(sftpKeyPassphrase)
                                    repository.setSftpRemoteDirSetting(sftpRemoteDir)
                                    statusText = "SFTP connection configuration saved."
                                },
                                onTestConnection = {
                                    coroutineScope.launch {
                                        isTestingSftp = true
                                        statusText = "Testing SFTP Connection..."
                                        val result = sftpService.testConnection(buildSftpConfig(), onHostKeyUnverified)
                                        isTestingSftp = false
                                        if (result.isSuccess) {
                                            statusText = "SFTP Connection Successful!"
                                            showTestSuccessDialog = true
                                        } else {
                                            statusText = "SFTP Connection Failed: ${result.exceptionOrNull()?.message}"
                                            testErrorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                            showTestErrorDialog = true
                                        }
                                    }
                                },
                                isTesting = isTestingSftp
                            )
                            SftpSyncActions(
                                onUploadClick = {
                                    coroutineScope.launch {
                                        statusText = "Calculating upload changes..."
                                        isSftpSyncing = true
                                        val result = sftpService.calculateUploadPlan(buildSftpConfig(), db, onHostKeyUnverified)
                                        isSftpSyncing = false
                                        if (result.isSuccess) {
                                            val actions = result.getOrThrow()
                                            proposedSftpActions.clear()
                                            proposedSftpActions.addAll(actions)
                                            selectedSftpActions.clear()
                                            actions.forEach { selectedSftpActions[it.filename] = true }
                                            syncDirection = "Upload"
                                            showSyncConfirmDialog = true
                                        } else {
                                            statusText = "Plan calculation failed: ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                },
                                onDownloadClick = {
                                    coroutineScope.launch {
                                        statusText = "Calculating download changes..."
                                        isSftpSyncing = true
                                        val result = sftpService.calculateDownloadPlan(buildSftpConfig(), db, onHostKeyUnverified)
                                        isSftpSyncing = false
                                        if (result.isSuccess) {
                                            val actions = result.getOrThrow()
                                            proposedSftpActions.clear()
                                            proposedSftpActions.addAll(actions)
                                            selectedSftpActions.clear()
                                            actions.forEach { selectedSftpActions[it.filename] = true }
                                            syncDirection = "Download"
                                            showSyncConfirmDialog = true
                                        } else {
                                            statusText = "Plan calculation failed: ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                },
                                isSyncing = isSftpSyncing || isTestingSftp,
                                syncProgress = sftpSyncProgress
                            )
                        }

                    // Right Column: Categories Manager
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                    ) {
                        CategoriesManager(
                            categoriesList = categoriesList,
                            onAddCategory = {
                                dialogIsEditMode = false
                                dialogCategoryKey = ""
                                dialogCategoryLabel = ""
                                dialogCategoryPrefix = ""
                                dialogErrorText = ""
                                showCategoryDialog = true
                            },
                            onEditCategory = { cat ->
                                dialogIsEditMode = true
                                dialogCategoryKey = cat.category
                                dialogCategoryLabel = cat.label
                                dialogCategoryPrefix = cat.imagePrefix
                                dialogErrorText = ""
                                showCategoryDialog = true
                            },
                            onDeleteCategory = { cat ->
                                categoryToDelete = cat
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            } else {
                // Mobile layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                ) {
                    StatusBanner(statusText)
                    if (isSftpSyncing) {
                        SyncWarningBanner()
                    }
                    ThemeSelector(currentTheme, onThemeChanged)
                    if (isDesktopPlatform()) {
                        ImagesDirectorySettings(
                            customImagesPath = customImagesPath,
                            onSelectPath = { selectedDir ->
                                repository.setImagesPathSetting(selectedDir)
                                com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = selectedDir
                                customImagesPath = selectedDir
                            },
                            onClearPath = {
                                repository.setImagesPathSetting(null)
                                com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = null
                                customImagesPath = null
                            }
                        )
                        ImportExportDirectorySettings(
                            customImportExportPath = customImportExportPath,
                            onSelectPath = { selectedDir ->
                                repository.setImportExportPathSetting(selectedDir)
                                customImportExportPath = selectedDir
                            },
                            onClearPath = {
                                repository.setImportExportPathSetting(null)
                                customImportExportPath = null
                            }
                        )
                    }
                    ImportExportActions(
                            customImportExportPath = customImportExportPath,
                            db = db,
                            onImportComplete = { counts ->
                                importCountsMap.clear()
                                importCountsMap.putAll(counts)
                                showImportDialog = true
                                statusText = "Import Complete!"
                            },
                            onExportComplete = { path, files ->
                                exportDirectoryPath = path
                                exportedFiles.clear()
                                exportedFiles.addAll(files)
                                showExportDialog = true
                                statusText = "Export Complete!"
                            },
                            onSetStatus = { statusText = it },
                            readJsonFile = { name, dir -> readJsonFile(name, dir) },
                            writeJsonFile = { name, content, dir -> writeJsonFile(name, content, dir) }
                        )
                        SftpSettingsCard(
                            host = sftpHost, onHostChange = { sftpHost = it },
                            port = sftpPort, onPortChange = { sftpPort = it },
                            username = sftpUsername, onUsernameChange = { sftpUsername = it },
                            authType = sftpAuthType, onAuthTypeChange = { sftpAuthType = it },
                            password = sftpPassword, onPasswordChange = { sftpPassword = it },
                            keyPath = sftpKeyPath, onKeyPathChange = { sftpKeyPath = it },
                            keyPassphrase = sftpKeyPassphrase, onKeyPassphraseChange = { sftpKeyPassphrase = it },
                            remoteDir = sftpRemoteDir, onRemoteDirChange = { sftpRemoteDir = it },
                            onSave = {
                                repository.setSftpHostSetting(sftpHost)
                                repository.setSftpPortSetting(sftpPort.toIntOrNull() ?: 22)
                                repository.setSftpUsernameSetting(sftpUsername)
                                repository.setSftpAuthTypeSetting(sftpAuthType)
                                repository.setSftpPasswordSetting(sftpPassword)
                                repository.setSftpKeyPathSetting(sftpKeyPath)
                                repository.setSftpKeyPassphraseSetting(sftpKeyPassphrase)
                                repository.setSftpRemoteDirSetting(sftpRemoteDir)
                                statusText = "SFTP connection configuration saved."
                            },
                            onTestConnection = {
                                coroutineScope.launch {
                                    isTestingSftp = true
                                    statusText = "Testing SFTP Connection..."
                                    val result = sftpService.testConnection(buildSftpConfig(), onHostKeyUnverified)
                                    isTestingSftp = false
                                    if (result.isSuccess) {
                                        statusText = "SFTP Connection Successful!"
                                        showTestSuccessDialog = true
                                    } else {
                                        statusText = "SFTP Connection Failed: ${result.exceptionOrNull()?.message}"
                                        testErrorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                        showTestErrorDialog = true
                                    }
                                }
                            },
                            isTesting = isTestingSftp
                        )
                        SftpSyncActions(
                            onUploadClick = {
                                coroutineScope.launch {
                                    statusText = "Calculating upload changes..."
                                    isSftpSyncing = true
                                    val result = sftpService.calculateUploadPlan(buildSftpConfig(), db, onHostKeyUnverified)
                                    isSftpSyncing = false
                                    if (result.isSuccess) {
                                        val actions = result.getOrThrow()
                                        proposedSftpActions.clear()
                                        proposedSftpActions.addAll(actions)
                                        selectedSftpActions.clear()
                                        actions.forEach { selectedSftpActions[it.filename] = true }
                                        syncDirection = "Upload"
                                        showSyncConfirmDialog = true
                                    } else {
                                        statusText = "Plan calculation failed: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            onDownloadClick = {
                                coroutineScope.launch {
                                    statusText = "Calculating download changes..."
                                    isSftpSyncing = true
                                    val result = sftpService.calculateDownloadPlan(buildSftpConfig(), db, onHostKeyUnverified)
                                    isSftpSyncing = false
                                    if (result.isSuccess) {
                                        val actions = result.getOrThrow()
                                        proposedSftpActions.clear()
                                        proposedSftpActions.addAll(actions)
                                        selectedSftpActions.clear()
                                        actions.forEach { selectedSftpActions[it.filename] = true }
                                        syncDirection = "Download"
                                        showSyncConfirmDialog = true
                                    } else {
                                        statusText = "Plan calculation failed: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            isSyncing = isSftpSyncing || isTestingSftp,
                            syncProgress = sftpSyncProgress
                        )
                        CategoriesManager(
                        categoriesList = categoriesList,
                        onAddCategory = {
                            dialogIsEditMode = false
                            dialogCategoryKey = ""
                            dialogCategoryLabel = ""
                            dialogCategoryPrefix = ""
                            dialogErrorText = ""
                            showCategoryDialog = true
                        },
                        onEditCategory = { cat ->
                            dialogIsEditMode = true
                            dialogCategoryKey = cat.category
                            dialogCategoryLabel = cat.label
                            dialogCategoryPrefix = cat.imagePrefix
                            dialogErrorText = ""
                            showCategoryDialog = true
                        },
                        onDeleteCategory = { cat ->
                            categoryToDelete = cat
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        PlatformScrollbar(
            state = lazyListState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

@Composable
fun StatusBanner(statusText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text("Storage Type: SQLite Local Database", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Status: $statusText", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = sysTextColor())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelector(currentTheme: Int, onThemeChanged: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Theme Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val buttonColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                inactiveContainerColor = Color.Transparent,
                inactiveContentColor = sysTextColor(),
                activeBorderColor = MaterialTheme.colorScheme.primary,
                inactiveBorderColor = MaterialTheme.colorScheme.outline
            )
            SegmentedButton(
                selected = currentTheme == 0,
                onClick = { onThemeChanged(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                colors = buttonColors
            ) { Text("System Theme", fontSize = 12.sp) }
            SegmentedButton(
                selected = currentTheme == 1,
                onClick = { onThemeChanged(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                colors = buttonColors
            ) { Text("Light", fontSize = 12.sp) }
            SegmentedButton(
                selected = currentTheme == 2,
                onClick = { onThemeChanged(2) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                colors = buttonColors
            ) { Text("Dark", fontSize = 12.sp) }
        }
    }
}

@Composable
fun ImagesDirectorySettings(
    customImagesPath: String?,
    onSelectPath: (String) -> Unit,
    onClearPath: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Images Directory", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(GcSpacing.Standard)) {
                Text(
                    text = if (customImagesPath.isNullOrEmpty()) {
                        "Using default locations (relative to working directory)"
                    } else {
                        "Custom Path: $customImagesPath"
                    },
                    fontSize = 14.sp,
                    color = sysTextColor()
                )
                Spacer(modifier = Modifier.height(GcSpacing.Small))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                ) {
                    Button(
                        onClick = {
                            val selectedDir = selectDirectoryDialog("Select Images Directory")
                            if (selectedDir != null) {
                                onSelectPath(selectedDir)
                            }
                        }
                    ) {
                        Text("Select Directory")
                    }
                    if (!customImagesPath.isNullOrEmpty()) {
                        OutlinedButton(onClick = onClearPath) {
                            Text("Clear Custom Path")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportDirectorySettings(
    customImportExportPath: String?,
    onSelectPath: (String) -> Unit,
    onClearPath: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Import / Export Directory", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(GcSpacing.Standard)) {
                Text(
                    text = if (customImportExportPath.isNullOrEmpty()) {
                        "Using default locations (relative to working directory)"
                    } else {
                        "Custom Path: $customImportExportPath"
                    },
                    fontSize = 14.sp,
                    color = sysTextColor()
                )
                Spacer(modifier = Modifier.height(GcSpacing.Small))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                ) {
                    Button(
                        onClick = {
                            val selectedDir = selectDirectoryDialog("Select Import/Export Directory")
                            if (selectedDir != null) {
                                onSelectPath(selectedDir)
                            }
                        }
                    ) {
                        Text("Select Directory")
                    }
                    if (!customImportExportPath.isNullOrEmpty()) {
                        OutlinedButton(onClick = onClearPath) {
                            Text("Clear Custom Path")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportActions(
    customImportExportPath: String?,
    db: ToyDatabase,
    onImportComplete: (counts: Map<String, Int>) -> Unit,
    onExportComplete: (path: String, files: List<String>) -> Unit,
    onSetStatus: (String) -> Unit,
    readJsonFile: (fileName: String, dirPath: String?) -> String?,
    writeJsonFile: (fileName: String, content: String, dirPath: String?) -> String?
) {
    val repository = remember(db) { ToyRepository(db) }
    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Import / Export Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
        ) {
            Button(
                enabled = !isImporting && !isExporting,
                onClick = {
                    val selectedDir = customImportExportPath
                    if (selectedDir != null) {
                        coroutineScope.launch {
                            isImporting = true
                            onSetStatus("Importing JSON files from $selectedDir...")
                            try {
                                val importedCounts = mutableMapOf<String, Int>()

                                // Execute SQLite insertions on IO dispatcher
                                withContext(Dispatchers.IO) {
                                    // 1. Import Category Settings
                                    val catSettingsContent = readJsonFile("category_settings.json", selectedDir)
                                    val catSettingsCount = catSettingsContent?.let { ImportExportService.importCategorySettings(db, it) } ?: 0
                                    if (catSettingsCount > 0) {
                                        importedCounts["Category Settings"] = catSettingsCount
                                    }

                                    // 2. Import Makers
                                    var makerCount = 0
                                    val makersContent = readJsonFile("carmaker.json", selectedDir) ?: readJsonFile("makers.json", selectedDir)
                                    makersContent?.let { makerCount = ImportExportService.importMakers(db, it) }
                                    if (makerCount > 0) {
                                        importedCounts["Makers"] = makerCount
                                    }

                                    // 4. Import Toys for all active categories
                                    val activeCategories = repository.getCategorySettings()
                                    activeCategories.forEach { cat ->
                                        val content = readJsonFile("${cat.imagePrefix}list.json", selectedDir)
                                            ?: readJsonFile("${cat.category}s.json", selectedDir)
                                            ?: readJsonFile("${cat.category}list.json", selectedDir)
                                            ?: readJsonFile("${cat.category}.json", selectedDir)
                                        if (content != null) {
                                            val count = ImportExportService.importToys(db, cat.category, content)
                                            importedCounts[cat.label] = count
                                        }
                                    }
                                }

                                isImporting = false
                                onImportComplete(importedCounts)
                            } catch (e: Exception) {
                                isImporting = false
                                onSetStatus("Error during import: ${e.message}")
                                GcLog.e("SettingsScreen", "Import error", e)
                            }
                        }
                    } else {
                        onSetStatus("Import failed: Import/Export directory is not configured.")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importing...")
                } else {
                    Text("Import JSON Data")
                }
            }

            Button(
                enabled = !isImporting && !isExporting,
                onClick = {
                    val selectedDir = customImportExportPath
                    if (selectedDir != null) {
                        coroutineScope.launch {
                            isExporting = true
                            onSetStatus("Exporting JSON files to $selectedDir...")
                            try {
                                val filesToWrite = mutableListOf<Pair<String, String>>()
                                var successCount = 0
                                var lastWrittenPath: String? = null
                                val exported = mutableListOf<String>()

                                withContext(Dispatchers.IO) {
                                    // 1. Export Makers
                                    val makersJson = ImportExportService.exportMakers(db)
                                    filesToWrite.add("carmaker.json" to makersJson)

                                    // 2. Export Category Settings
                                    val categorySettingsJson = ImportExportService.exportCategorySettings(db)
                                    filesToWrite.add("category_settings.json" to categorySettingsJson)

                                    // 4. Export Toys
                                    val activeCategories = repository.getCategorySettings()
                                    activeCategories.forEach { cat ->
                                        val toysJson = ImportExportService.exportToys(db, cat.category)
                                        filesToWrite.add("${cat.imagePrefix}list.json" to toysJson)
                                    }

                                    for ((fileName, jsonContent) in filesToWrite) {
                                        val pathStr = writeJsonFile(fileName, jsonContent, selectedDir)
                                        if (pathStr != null) {
                                            successCount++
                                            exported.add(fileName)
                                            lastWrittenPath = pathStr
                                        }
                                    }
                                }

                                isExporting = false
                                if (successCount == filesToWrite.size && lastWrittenPath != null) {
                                    val dirPath = lastWrittenPath.substringBeforeLast("/")
                                    onExportComplete(dirPath, exported)
                                } else {
                                    onSetStatus("Export Completed with errors ($successCount/${filesToWrite.size} succeeded).")
                                }
                            } catch (e: Exception) {
                                isExporting = false
                                onSetStatus("Error during export: ${e.message}")
                                GcLog.e("SettingsScreen", "Export error", e)
                            }
                        }
                    } else {
                        onSetStatus("Export failed: Import/Export directory is not configured.")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporting...")
                } else {
                    Text("Export JSON Data")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesManager(
    categoriesList: List<CategorySetting>,
    onAddCategory: () -> Unit,
    onEditCategory: (CategorySetting) -> Unit,
    onDeleteCategory: (CategorySetting) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Toy Categories Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
            IconButton(onClick = onAddCategory) {
                Icon(Icons.Default.Add, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(GcSpacing.Small)
        ) {
            categoriesList.forEach { cat ->
                Card(
                    modifier = Modifier.width(320.dp),
                    colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(GcSpacing.Standard),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Key: ${cat.category} | Prefix: ${cat.imagePrefix}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            IconButton(onClick = { onEditCategory(cat) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Category", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpSettingsCard(
    host: String, onHostChange: (String) -> Unit,
    port: String, onPortChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    authType: String, onAuthTypeChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    keyPath: String, onKeyPathChange: (String) -> Unit,
    keyPassphrase: String, onKeyPassphraseChange: (String) -> Unit,
    remoteDir: String, onRemoteDirChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    isTesting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text("SFTP Server Connection", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
            Spacer(modifier = Modifier.height(GcSpacing.Small))

            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("SFTP Host") },
                placeholder = { Text("e.g. sftp.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = sysTextColor(),
                    unfocusedTextColor = sysTextColor()
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    label = { Text("Port") },
                    placeholder = { Text("22") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = sysTextColor(),
                        unfocusedTextColor = sysTextColor()
                    )
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    placeholder = { Text("e.g. gepetto") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = sysTextColor(),
                        unfocusedTextColor = sysTextColor()
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isDesktopPlatform()) {
                Text("Authentication Type", style = MaterialTheme.typography.bodyMedium, color = sysTextColor())
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = authType == "password", onClick = { onAuthTypeChange("password") })
                        Text("Password", color = sysTextColor())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = authType == "key", onClick = { onAuthTypeChange("key") })
                        Text("SSH Key File", color = sysTextColor())
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isDesktopPlatform() || authType == "password") {
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    placeholder = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = sysTextColor(),
                        unfocusedTextColor = sysTextColor()
                    )
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = keyPath,
                        onValueChange = onKeyPathChange,
                        label = { Text("Private Key Path") },
                        placeholder = { Text("/path/to/id_rsa") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                    Button(onClick = {
                        val path = selectFileDialog("Select Private Key File", listOf("pem", "key", "rsa", "pub", ""))
                        if (path != null) {
                            onKeyPathChange(path)
                        }
                    }) {
                        Text("Choose")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyPassphrase,
                    onValueChange = onKeyPassphraseChange,
                    label = { Text("Key Passphrase (optional)") },
                    placeholder = { Text("Passphrase") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = sysTextColor(),
                        unfocusedTextColor = sysTextColor()
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = remoteDir,
                onValueChange = onRemoteDirChange,
                label = { Text("Remote Base Directory") },
                placeholder = { Text("e.g. /home/gepetto/toydb") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = sysTextColor(),
                    unfocusedTextColor = sysTextColor()
                )
            )
            Spacer(modifier = Modifier.height(GcSpacing.Standard))

            Row(horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)) {
                Button(onClick = onSave) {
                    Text("Save Config")
                }
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Text("Test Connection")
                    }
                }
            }
        }
    }
}

@Composable
fun SftpSyncActions(
    onUploadClick: () -> Unit,
    onDownloadClick: () -> Unit,
    isSyncing: Boolean,
    syncProgress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text("SFTP Server Synchronization", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
            Spacer(modifier = Modifier.height(GcSpacing.Small))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
            ) {
                Button(
                    enabled = !isSyncing,
                    onClick = onUploadClick,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...")
                    } else {
                        Text("Upload")
                    }
                }

                Button(
                    enabled = !isSyncing,
                    onClick = onDownloadClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...")
                    } else {
                        Text("Download")
                    }
                }
            }

            if (isSyncing) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "⚠️ Do not navigate away or minimize the app until sync is complete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { syncProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SyncWarningBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text(
                text = "⚠️ Active Transfer in Progress", 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Do not navigate away from this screen or minimize the app. Doing so will abort the active upload or download.",
                fontSize = 12.sp,
                color = sysTextColor()
            )
        }
    }
}
