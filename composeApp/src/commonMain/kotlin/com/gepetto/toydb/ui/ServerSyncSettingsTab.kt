package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.GcLog
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import club.gepetto.utils.ioDispatcher
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.service.*
import com.gepetto.toydb.utils.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

class HostKeyVerification(
    val hostname: String,
    val port: Int,
    val fingerprint: String,
    val deferred: CompletableDeferred<Boolean>
)

@Composable
fun ServerSyncSettingsTab(
    db: ToyDatabase,
    sftpService: SftpService,
    dataPath: String?,
    onCategoriesChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val repository = remember(db) { ToyRepository(db) }
    val coroutineScope = rememberCoroutineScope()

    // Status strings
    val htmlExportCompleteText = stringResource(Res.string.html_export_complete)
    val sftpConfigSavedText = stringResource(Res.string.sftp_config_saved)
    val webSyncRunningText = stringResource(Res.string.web_sync_running)
    val webSyncSuccessText = stringResource(Res.string.web_sync_success)
    val webSyncNoUpdatesText = stringResource(Res.string.web_sync_no_updates)
    val webSyncErrorText = stringResource(Res.string.web_sync_error)
    var statusText by remember { mutableStateOf("") }

    // SFTP Config States
    var sftpHost by remember { mutableStateOf(repository.getSftpHostSetting() ?: "") }
    var sftpPort by remember { mutableStateOf(repository.getSftpPortSetting().toString()) }
    var sftpUsername by remember { mutableStateOf(repository.getSftpUsernameSetting() ?: "") }
    var sftpAuthType by remember { mutableStateOf(if (!isDesktopPlatform()) "password" else repository.getSftpAuthTypeSetting()) }
    var sftpPassword by remember { mutableStateOf(repository.getSftpPasswordSetting() ?: "") }
    var sftpKeyPath by remember { mutableStateOf(repository.getSftpKeyPathSetting() ?: "") }
    var sftpKeyPassphrase by remember { mutableStateOf(repository.getSftpKeyPassphraseSetting() ?: "") }
    var sftpRemoteDir by remember { mutableStateOf(repository.getSftpRemoteDirSetting() ?: "") }
    var sftpApprovedFingerprints by remember { mutableStateOf(repository.getSftpApprovedFingerprintsSetting() ?: "") }
    var htmlBaseUrl by remember { mutableStateOf(repository.getBaseUrlSetting()) }

    var isTestingSftp by remember { mutableStateOf(false) }
    var sftpSyncProgress by remember { mutableStateOf(0.0f) }
    var isSftpSyncing by remember { mutableStateOf(false) }
    var showTestSuccessDialog by remember { mutableStateOf(false) }
    var showTestErrorDialog by remember { mutableStateOf(false) }
    var testErrorMsg by remember { mutableStateOf("") }
    var showSyncErrorDialog by remember { mutableStateOf(false) }
    var syncErrorMsg by remember { mutableStateOf("") }
    var showSyncConfirmDialog by remember { mutableStateOf(false) }
    var syncDialogPhase by remember { mutableStateOf("Confirm") } // "Confirm", "Progress", "Success", "Error"
    var syncErrorMessage by remember { mutableStateOf("") }
    val proposedSftpActions = remember { mutableStateListOf<SyncAction>() }
    val selectedSftpActions = remember { mutableStateMapOf<String, Boolean>() }
    var syncDirection by remember { mutableStateOf("Upload") }
    var excludeHtmlFiles by remember { mutableStateOf(false) }
    var showWebSyncDialog by remember { mutableStateOf(false) }
    var webSyncDialogPhase by remember { mutableStateOf("Progress") }
    var webSyncErrorMessage by remember { mutableStateOf("") }

    // HTML export success dialog state
    var showHtmlExportDialog by remember { mutableStateOf(false) }
    var htmlExportPath by remember { mutableStateOf("") }
    var htmlExportCount by remember { mutableStateOf(0) }

    var activeVerification by remember { mutableStateOf<HostKeyVerification?>(null) }

    val isDark = isSystemInDarkTheme()
    val dialogModifier = if (isDark) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), AlertDialogDefaults.shape)
    } else {
        Modifier
    }

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
            withContext(ioDispatcher) {
                repository.addSftpApprovedFingerprint(fingerprint)
            }
            sftpApprovedFingerprints = repository.getSftpApprovedFingerprintsSetting() ?: ""
        }
        accepted
    }

    // HTML Export Success Dialog
    if (showHtmlExportDialog) {
        AlertDialog(
            onDismissRequest = { showHtmlExportDialog = false },
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = stringResource(Res.string.html_export_success_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = sysTextColor()
                )
            },
            text = {
                Column {
                    Text(stringResource(Res.string.html_export_success_desc), color = sysTextColor())
                    Spacer(modifier = Modifier.height(GcSpacing.Small))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = htmlExportPath,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(GcSpacing.Small),
                            color = sysTextColor()
                        )
                    }
                    Spacer(modifier = Modifier.height(GcSpacing.Standard))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(stringResource(Res.string.html_export_success_count, htmlExportCount), fontSize = 14.sp, color = sysTextColor())
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHtmlExportDialog = false }) {
                    Text(stringResource(Res.string.ok))
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
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = { Text(stringResource(Res.string.sftp_verify_host_title), color = sysTextColor()) },
            text = {
                Column {
                    Text(stringResource(Res.string.sftp_verify_host_desc, verification.hostname, verification.port.toString()), color = sysTextColor())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(Res.string.sftp_verify_host_fingerprint), fontWeight = FontWeight.Bold, color = sysTextColor())
                    Text(verification.fingerprint, style = MaterialTheme.typography.bodySmall, color = sysTextColor())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(Res.string.sftp_verify_host_confirm), color = sysTextColor())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        verification.deferred.complete(true)
                        activeVerification = null
                    }
                ) {
                    Text(stringResource(Res.string.sftp_verify_host_accept))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        verification.deferred.complete(false)
                        activeVerification = null
                    }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showTestSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showTestSuccessDialog = false },
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = { Text(stringResource(Res.string.sftp_test_success_title), color = sysTextColor(), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.sftp_test_success_desc), color = sysTextColor()) },
            confirmButton = {
                Button(onClick = { showTestSuccessDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    if (showTestErrorDialog) {
        AlertDialog(
            onDismissRequest = { showTestErrorDialog = false },
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = { Text(stringResource(Res.string.sftp_test_failed_title), color = sysTextColor(), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.sftp_test_failed_desc, testErrorMsg), color = sysTextColor()) },
            confirmButton = {
                Button(onClick = { showTestErrorDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    if (showSyncErrorDialog) {
        AlertDialog(
            onDismissRequest = { showSyncErrorDialog = false },
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = { Text(stringResource(Res.string.sftp_status_plan_failed, "").trim().removeSuffix(":").removeSuffix("："), color = sysTextColor(), fontWeight = FontWeight.Bold) },
            text = { Text(syncErrorMsg, color = sysTextColor()) },
            confirmButton = {
                Button(onClick = { showSyncErrorDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    if (showSyncConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (syncDialogPhase != "Progress") {
                    showSyncConfirmDialog = false
                }
            },
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = {
                val titleText = when (syncDialogPhase) {
                    "Confirm" -> stringResource(Res.string.sftp_sync_direction_label, syncDirection)
                    "Progress" -> stringResource(Res.string.sftp_status_syncing)
                    "Success" -> {
                        if (syncDirection == "Upload") {
                            stringResource(Res.string.sftp_status_upload_completed)
                        } else {
                            stringResource(Res.string.sftp_status_completed)
                        }
                    }
                    "Error" -> stringResource(Res.string.sftp_test_failed_title)
                    else -> ""
                }
                Text(text = titleText, fontWeight = FontWeight.Bold, color = sysTextColor())
            },
            text = {
                Column {
                    when (syncDialogPhase) {
                        "Confirm" -> {
                            Text(
                                text = if (proposedSftpActions.isEmpty()) {
                                    if (syncDirection == "Upload") {
                                        stringResource(Res.string.sftp_sync_ready_upload)
                                    } else {
                                        stringResource(Res.string.sftp_sync_ready_download)
                                    }
                                } else {
                                    stringResource(Res.string.sftp_sync_actions_needed)
                                },
                                color = sysTextColor()
                            )
                            
                            if (proposedSftpActions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                if (syncDirection == "Download") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 13.dp, end = 13.dp, top = 2.dp, bottom = 2.dp)
                                    ) {
                                        Checkbox(
                                            checked = excludeHtmlFiles,
                                            onCheckedChange = { isChecked ->
                                                excludeHtmlFiles = isChecked
                                                if (isChecked) {
                                                    proposedSftpActions.forEach { action ->
                                                        val isHtml = action.filename.endsWith(".html", ignoreCase = true) || action.filename.endsWith(".htm", ignoreCase = true)
                                                        if (isHtml) {
                                                            selectedSftpActions[action.filename] = false
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = stringResource(Res.string.sftp_sync_exclude_html_files),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = sysTextColor()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 13.dp, end = 13.dp, top = 2.dp, bottom = 2.dp)
                                    ) {
                                    val allSelected = proposedSftpActions
                                        .filter { !excludeHtmlFiles || !(it.filename.endsWith(".html", ignoreCase = true) || it.filename.endsWith(".htm", ignoreCase = true)) }
                                        .all { selectedSftpActions[it.filename] == true }
                                    Checkbox(
                                        checked = allSelected,
                                        onCheckedChange = { isChecked ->
                                            proposedSftpActions.forEach { action ->
                                                val isHtml = action.filename.endsWith(".html", ignoreCase = true) || action.filename.endsWith(".htm", ignoreCase = true)
                                                if (!(excludeHtmlFiles && isHtml)) {
                                                    selectedSftpActions[action.filename] = isChecked
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.select_all),
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
                                            val isHtml = action.filename.endsWith(".html", ignoreCase = true) || action.filename.endsWith(".htm", ignoreCase = true)
                                            val isItemChecked = if (excludeHtmlFiles && isHtml) false else (selectedSftpActions[action.filename] ?: true)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isItemChecked,
                                                    enabled = !(excludeHtmlFiles && isHtml),
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
                                                    text = if (action.reason == "New File") stringResource(Res.string.sftp_sync_new_file) else if (action.reason.startsWith("Overwrite")) stringResource(Res.string.sftp_sync_overwrite) else action.reason,
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
                                val selectedCount = proposedSftpActions.count { action ->
                                    val isHtml = action.filename.endsWith(".html", ignoreCase = true) || action.filename.endsWith(".htm", ignoreCase = true)
                                    if (excludeHtmlFiles && isHtml) false else selectedSftpActions[action.filename] == true
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = GcSpacing.Small),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(Res.string.sftp_sync_total_files, totalCount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = sysTextColor()
                                    )
                                    Text(
                                        text = stringResource(Res.string.selected_count, selectedCount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(Res.string.sftp_sync_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        "Progress" -> {
                            Text(text = statusText, color = sysTextColor())
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { sftpSyncProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(Res.string.sftp_transfer_warning_dialog_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        "Success" -> {
                            val descText = if (syncDirection == "Upload") {
                                stringResource(Res.string.sftp_status_upload_completed_desc)
                            } else {
                                stringResource(Res.string.sftp_status_download_completed_desc)
                            }
                            Text(text = descText, color = sysTextColor())
                        }
                        "Error" -> {
                            val errorDesc = if (syncDirection == "Upload") {
                                stringResource(Res.string.sftp_status_upload_failed, syncErrorMessage)
                            } else {
                                stringResource(Res.string.sftp_status_download_failed, syncErrorMessage)
                            }
                            Text(text = errorDesc, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                if (syncDialogPhase != "Progress") {
                    Button(
                        onClick = {
                            if (syncDialogPhase == "Confirm") {
                                syncDialogPhase = "Progress"
                                coroutineScope.launch {
                                    isSftpSyncing = true
                                    val selectedSet = selectedSftpActions.filterValues { it }.keys.filter { filename ->
                                        val isHtml = filename.endsWith(".html", ignoreCase = true) || filename.endsWith(".htm", ignoreCase = true)
                                        !(excludeHtmlFiles && isHtml)
                                    }.toSet()
                                    if (syncDirection == "Upload") {
                                        statusText = getString(Res.string.sftp_status_uploading)
                                        sftpSyncProgress = 0.0f
                                        val result = sftpService.uploadData(buildSftpConfig(), db, onHostKeyUnverified, selectedSet) { status, progress ->
                                            statusText = status
                                            sftpSyncProgress = progress
                                        }
                                        if (result.isSuccess) {
                                            statusText = getString(Res.string.sftp_status_upload_success)
                                            syncDialogPhase = "Success"
                                        } else {
                                            syncErrorMessage = result.exceptionOrNull()?.message ?: ""
                                            statusText = getString(Res.string.sftp_status_upload_failed, syncErrorMessage)
                                            syncDialogPhase = "Error"
                                        }
                                    } else {
                                        statusText = getString(Res.string.sftp_status_downloading)
                                        sftpSyncProgress = 0.0f
                                        val result = sftpService.downloadData(buildSftpConfig(), db, onHostKeyUnverified, selectedSet) { status, progress ->
                                            statusText = status
                                            sftpSyncProgress = progress
                                        }
                                        if (result.isSuccess) {
                                            statusText = getString(Res.string.sftp_status_download_success)
                                            syncDialogPhase = "Success"
                                        } else {
                                            syncErrorMessage = result.exceptionOrNull()?.message ?: ""
                                            statusText = getString(Res.string.sftp_status_download_failed, syncErrorMessage)
                                            syncDialogPhase = "Error"
                                        }
                                    }
                                    isSftpSyncing = false
                                }
                            } else {
                                showSyncConfirmDialog = false
                            }
                        }
                    ) {
                        Text(if (syncDialogPhase == "Confirm") stringResource(Res.string.continue_btn) else stringResource(Res.string.ok))
                    }
                }
            },
            dismissButton = {
                if (syncDialogPhase == "Confirm") {
                    OutlinedButton(onClick = { showSyncConfirmDialog = false }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        )
    }

    if (showWebSyncDialog) {
        AlertDialog(
            onDismissRequest = {
                if (webSyncDialogPhase != "Progress") {
                    showWebSyncDialog = false
                }
            },
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = {
                val titleText = when (webSyncDialogPhase) {
                    "Progress" -> stringResource(Res.string.html_sync_title)
                    "Success" -> stringResource(Res.string.web_sync_success)
                    "NoUpdates" -> stringResource(Res.string.web_sync_no_updates)
                    else -> stringResource(Res.string.web_sync_error)
                }
                Text(titleText, color = sysTextColor(), fontWeight = FontWeight.Bold)
            },
            text = {
                val descText = when (webSyncDialogPhase) {
                    "Progress" -> stringResource(Res.string.web_sync_running)
                    "Success" -> stringResource(Res.string.web_sync_success)
                    "NoUpdates" -> stringResource(Res.string.web_sync_no_updates)
                    else -> stringResource(Res.string.web_sync_error)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (webSyncDialogPhase == "Progress") {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(descText, color = sysTextColor())
                    if (webSyncDialogPhase == "Error" && webSyncErrorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = webSyncErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (webSyncDialogPhase != "Progress") {
                    Button(onClick = { showWebSyncDialog = false }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            }
        )
    }

    KeepScreenOn(enabled = isSftpSyncing && syncDirection == "Download")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 850.dp),
        verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
    ) {
        BaseUrlSettingsCard(
            baseUrl = htmlBaseUrl,
            onBaseUrlChange = { htmlBaseUrl = it },
            onSave = {
                repository.setBaseUrlSetting(htmlBaseUrl)
                statusText = webSyncRunningText
                webSyncDialogPhase = "Progress"
                webSyncErrorMessage = ""
                showWebSyncDialog = true
                coroutineScope.launch {
                    try {
                        val syncSuccess = HtmlSyncService.syncIfNewer(db, repository)
                        if (syncSuccess) {
                            onCategoriesChanged()
                            statusText = webSyncSuccessText
                            webSyncDialogPhase = "Success"
                        } else {
                            statusText = webSyncNoUpdatesText
                            webSyncDialogPhase = "NoUpdates"
                        }
                    } catch (e: Exception) {
                        webSyncErrorMessage = e.message ?: ""
                        statusText = webSyncErrorText
                        webSyncDialogPhase = "Error"
                    }
                }
            }
        )
        if (!isWebPlatform()) {
            WebsitePagesActions(
                customImportExportPath = dataPath,
                db = db,
                onHtmlExportComplete = { path, count ->
                    htmlExportPath = path
                    htmlExportCount = count
                    showHtmlExportDialog = true
                    statusText = htmlExportCompleteText
                },
                onSetStatus = { statusText = it }
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
                    statusText = sftpConfigSavedText
                },
                onTestConnection = {
                    coroutineScope.launch {
                        if (sftpHost.trim().isEmpty() || sftpUsername.trim().isEmpty()) {
                            testErrorMsg = getString(Res.string.all_fields_required)
                            showTestErrorDialog = true
                            return@launch
                        }
                        isTestingSftp = true
                        statusText = getString(Res.string.sftp_testing_connection)
                        val result = sftpService.testConnection(buildSftpConfig(), onHostKeyUnverified)
                        isTestingSftp = false
                        if (result.isSuccess) {
                            statusText = getString(Res.string.sftp_status_test_success)
                            showTestSuccessDialog = true
                        } else {
                            statusText = getString(Res.string.sftp_status_test_failed, result.exceptionOrNull()?.message ?: "")
                            testErrorMsg = result.exceptionOrNull()?.message ?: getString(Res.string.unknown)
                            showTestErrorDialog = true
                        }
                    }
                },
                isTesting = isTestingSftp
            )
            SftpSyncActions(
                onUploadClick = {
                    coroutineScope.launch {
                        if (sftpHost.trim().isEmpty() || sftpUsername.trim().isEmpty()) {
                            testErrorMsg = getString(Res.string.all_fields_required)
                            showTestErrorDialog = true
                            return@launch
                        }
                        statusText = getString(Res.string.sftp_status_calculating_upload)
                        val result = sftpService.calculateUploadPlan(buildSftpConfig(), db, onHostKeyUnverified)
                        if (result.isSuccess) {
                            proposedSftpActions.clear()
                            selectedSftpActions.clear()
                            val actions = result.getOrNull() ?: emptyList()
                            proposedSftpActions.addAll(actions)
                            actions.forEach { action ->
                                selectedSftpActions[action.filename] = true
                            }
                            syncDirection = "Upload"
                            syncDialogPhase = "Confirm"
                            showSyncConfirmDialog = true
                        } else {
                            val errorMsg = result.exceptionOrNull()?.message ?: getString(Res.string.unknown)
                            statusText = getString(Res.string.sftp_status_plan_failed, errorMsg)
                            syncErrorMsg = errorMsg
                            showSyncErrorDialog = true
                        }
                    }
                },
                onDownloadClick = {
                    coroutineScope.launch {
                        if (sftpHost.trim().isEmpty() || sftpUsername.trim().isEmpty()) {
                            testErrorMsg = getString(Res.string.all_fields_required)
                            showTestErrorDialog = true
                            return@launch
                        }
                        statusText = getString(Res.string.sftp_status_calculating_download)
                        val result = sftpService.calculateDownloadPlan(buildSftpConfig(), db, onHostKeyUnverified)
                        if (result.isSuccess) {
                            proposedSftpActions.clear()
                            selectedSftpActions.clear()
                            val actions = result.getOrNull() ?: emptyList()
                            proposedSftpActions.addAll(actions)
                            actions.forEach { action ->
                                val isHtml = action.filename.endsWith(".html", ignoreCase = true) || action.filename.endsWith(".htm", ignoreCase = true)
                                selectedSftpActions[action.filename] = !(excludeHtmlFiles && isHtml)
                            }
                            syncDirection = "Download"
                            syncDialogPhase = "Confirm"
                            showSyncConfirmDialog = true
                        } else {
                            val errorMsg = result.exceptionOrNull()?.message ?: getString(Res.string.unknown)
                            statusText = getString(Res.string.sftp_status_plan_failed, errorMsg)
                            syncErrorMsg = errorMsg
                            showSyncErrorDialog = true
                        }
                    }
                },
                isSyncing = isSftpSyncing,
                syncProgress = sftpSyncProgress
            )
            if (isSftpSyncing) {
                Spacer(modifier = Modifier.height(GcSpacing.Standard))
                SyncWarningBanner()
            }
        }
    }
}

@Composable
fun BaseUrlSettingsCard(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text(
                text = stringResource(Res.string.html_sync_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = sysTextColor()
            )
            Spacer(modifier = Modifier.height(GcSpacing.Small))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text(stringResource(Res.string.base_url_label)) },
                placeholder = { Text(stringResource(Res.string.base_url_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = sysTextColor(),
                    unfocusedTextColor = sysTextColor()
                )
            )
            Spacer(modifier = Modifier.height(GcSpacing.Standard))

            Button(
                onClick = onSave,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(Res.string.save_changes_btn))
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun BaseUrlSettingsCardPreview() {
    GcTheme {
        BaseUrlSettingsCard(
            baseUrl = "https://example.com/collection",
            onBaseUrlChange = {},
            onSave = {}
        )
    }
}

@Composable
fun WebsitePagesActions(
    customImportExportPath: String?,
    db: ToyDatabase,
    onHtmlExportComplete: (path: String, count: Int) -> Unit,
    onSetStatus: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val errorHtmlNoDirText = stringResource(Res.string.error_html_no_dir)
    var isExportingHtml by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.import_export_actions_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        
        Button(
            enabled = !isExportingHtml,
            onClick = {
                val selectedDir = customImportExportPath
                if (selectedDir != null) {
                    coroutineScope.launch {
                        isExportingHtml = true
                        onSetStatus(getString(Res.string.website_status_creating, selectedDir))
                        try {
                            val generatedCount = withContext(ioDispatcher) {
                                ImportExportService.exportHtml(db, selectedDir)
                            }
                            isExportingHtml = false
                            onHtmlExportComplete(selectedDir, generatedCount)
                            onSetStatus(getString(Res.string.website_status_done, generatedCount, selectedDir))
                        } catch (e: Exception) {
                            isExportingHtml = false
                            onSetStatus(getString(Res.string.error_html, e.message ?: ""))
                            GcLog.e("ServerSyncSettingsTab", "HTML export error", e)
                        }
                    }
                } else {
                    onSetStatus(errorHtmlNoDirText)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            if (isExportingHtml) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onTertiary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.status_exporting_html))
            } else {
                Text(stringResource(Res.string.export_html_btn))
            }
        }
    }
}

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
            Text(stringResource(Res.string.sftp_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
            Spacer(modifier = Modifier.height(GcSpacing.Small))

            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text(stringResource(Res.string.sftp_host) + "*") },
                placeholder = { Text(stringResource(Res.string.sftp_host_placeholder)) },
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
                    label = { Text(stringResource(Res.string.sftp_port)) },
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
                    label = { Text(stringResource(Res.string.sftp_username) + "*") },
                    placeholder = { Text(stringResource(Res.string.sftp_username_placeholder)) },
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
                Text(stringResource(Res.string.sftp_auth_type), style = MaterialTheme.typography.bodyMedium, color = sysTextColor())
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = authType == "password", onClick = { onAuthTypeChange("password") })
                        Text(stringResource(Res.string.sftp_password_label), color = sysTextColor())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = authType == "key", onClick = { onAuthTypeChange("key") })
                        Text(stringResource(Res.string.sftp_ssh_key_label), color = sysTextColor())
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            var passwordVisible by remember { mutableStateOf(false) }

            if (!isDesktopPlatform() || authType == "password") {
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(Res.string.sftp_password_label)) },
                    placeholder = { Text(stringResource(Res.string.sftp_password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
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
                        label = { Text(stringResource(Res.string.sftp_key_path)) },
                        placeholder = { Text(stringResource(Res.string.sftp_key_path_placeholder)) },
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
                        Text(stringResource(Res.string.choose))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyPassphrase,
                    onValueChange = onKeyPassphraseChange,
                    label = { Text(stringResource(Res.string.sftp_passphrase)) },
                    placeholder = { Text(stringResource(Res.string.sftp_passphrase_placeholder)) },
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
                label = { Text(stringResource(Res.string.sftp_remote_dir)) },
                placeholder = { Text(stringResource(Res.string.sftp_remote_dir_placeholder)) },
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
                    Text(stringResource(Res.string.sftp_save_config))
                }
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.sftp_testing_connection))
                    } else {
                        Text(stringResource(Res.string.sftp_test_connection))
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun SftpSettingsCardPreview() {
    GcTheme {
        SftpSettingsCard(
            host = "example.com",
            onHostChange = {},
            port = "22",
            onPortChange = {},
            username = "user",
            onUsernameChange = {},
            authType = "password",
            onAuthTypeChange = {},
            password = "secret",
            onPasswordChange = {},
            keyPath = "",
            onKeyPathChange = {},
            keyPassphrase = "",
            onKeyPassphraseChange = {},
            remoteDir = "/var/www/collection",
            onRemoteDirChange = {},
            onSave = {},
            onTestConnection = {},
            isTesting = false
        )
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
            Text(stringResource(Res.string.sftp_sync_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
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
                        Text(stringResource(Res.string.sftp_status_syncing))
                    } else {
                        Text(stringResource(Res.string.upload))
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
                        Text(stringResource(Res.string.sftp_status_syncing))
                    } else {
                        Text(stringResource(Res.string.download))
                    }
                }
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
                text = stringResource(Res.string.sftp_transfer_warning_dialog_title), 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.sftp_transfer_warning_dialog_desc),
                fontSize = 12.sp,
                color = sysTextColor()
            )
        }
    }
}
