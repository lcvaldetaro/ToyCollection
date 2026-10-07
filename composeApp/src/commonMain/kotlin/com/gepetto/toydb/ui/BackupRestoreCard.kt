package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import club.gepetto.utils.ioDispatcher
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.platform.BackupFileHelper
import com.gepetto.toydb.platform.BackupOpenResult
import com.gepetto.toydb.platform.BackupSaveResult
import com.gepetto.toydb.platform.rememberStoragePermissionRequest
import com.gepetto.toydb.service.BackupManifest
import com.gepetto.toydb.service.BackupRestoreService
import com.gepetto.toydb.utils.isDesktopPlatform
import com.gepetto.toydb.utils.systemFileSystem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

private enum class BackupPhase {
    Idle,
    Saving,
    Saved,
    SaveFailed,
    Opening,
    Confirm,
    Restoring,
    Restored,
    RestoreFailed,
    NotFound,
    Invalid,
    NoDataDir,
    NoSpace,
    PermissionDenied
}

@Composable
fun BackupRestoreCard(
    db: ToyDatabase,
    dataPath: String?,
    onCollectionRestored: () -> Unit,
    onSetStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val platformContext = coil3.compose.LocalPlatformContext.current
    val requestPermission = rememberStoragePermissionRequest()

    var isWorking by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(BackupPhase.Idle) }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var resultMessage by remember { mutableStateOf("") }
    var savedLocation by remember { mutableStateOf("") }
    var summaryToys by remember { mutableIntStateOf(0) }
    var summaryMakers by remember { mutableIntStateOf(0) }
    var summaryPhotos by remember { mutableIntStateOf(0) }
    var summaryMissingPhotos by remember { mutableIntStateOf(0) }
    var pendingBackup by remember { mutableStateOf<BackupRestoreService.ValidatedBackup?>(null) }
    var pendingOpen by remember { mutableStateOf<BackupOpenResult.Opened?>(null) }

    fun startBackup() {
        coroutineScope.launch {
            if (isDesktopPlatform()) {
                if (dataPath.isNullOrBlank() || systemFileSystem.metadataOrNull(dataPath.toPath())?.isDirectory != true) {
                    phase = BackupPhase.NoDataDir
                    return@launch
                }
            }
            val granted = requestPermission()
            if (!granted) {
                phase = BackupPhase.PermissionDenied
                return@launch
            }
            isWorking = true
            progressDone = 0
            progressTotal = 0
            try {
                var summary: BackupRestoreService.BackupSummary? = null
                val saveDialogTitle = getString(Res.string.backup_save_dialog_title)
                val result = withContext(ioDispatcher) {
                    BackupFileHelper.saveBackup("toy_collection_backup.zip", saveDialogTitle) { sink ->
                        phase = BackupPhase.Saving
                        val content = BackupRestoreService.prepareBackup(db)
                        summary = BackupRestoreService.writeBackup(content, sink) { done, total ->
                            progressDone = done
                            progressTotal = total
                        }
                    }
                }
                when (result) {
                    is BackupSaveResult.Saved -> {
                        savedLocation = result.location
                        summaryToys = summary?.toys ?: 0
                        summaryPhotos = summary?.photos ?: 0
                        summaryMissingPhotos = summary?.missingPhotos ?: 0
                        phase = BackupPhase.Saved
                        onSetStatus(getString(Res.string.backup_done_title))
                    }
                    is BackupSaveResult.Cancelled -> {
                        phase = BackupPhase.Idle
                    }
                    is BackupSaveResult.Failed -> {
                        resultMessage = result.message
                        phase = BackupPhase.SaveFailed
                        onSetStatus(getString(Res.string.backup_failed, result.message))
                    }
                }
            } catch (c: CancellationException) {
                phase = BackupPhase.Idle
                throw c
            } catch (e: Throwable) {
                val msg = e.message ?: "Unknown error"
                resultMessage = msg
                phase = BackupPhase.SaveFailed
                onSetStatus(getString(Res.string.backup_failed, msg))
            } finally {
                isWorking = false
            }
        }
    }

    fun startRestore() {
        coroutineScope.launch {
            if (isDesktopPlatform()) {
                if (dataPath.isNullOrBlank()) {
                    phase = BackupPhase.NoDataDir
                    return@launch
                }
            }
            val granted = requestPermission()
            if (!granted) {
                phase = BackupPhase.PermissionDenied
                return@launch
            }
            isWorking = true
            var openedResult: BackupOpenResult.Opened? = null
            try {
                if (!isDesktopPlatform()) {
                    phase = BackupPhase.Opening
                }
                val openDialogTitle = getString(Res.string.restore_open_dialog_title)
                val openResult = withContext(ioDispatcher) {
                    BackupFileHelper.openBackup(openDialogTitle)
                }
                when (openResult) {
                    is BackupOpenResult.Cancelled -> {
                        phase = BackupPhase.Idle
                        isWorking = false
                        return@launch
                    }
                    is BackupOpenResult.NotFound -> {
                        phase = BackupPhase.NotFound
                        isWorking = false
                        return@launch
                    }
                    is BackupOpenResult.NoSpace -> {
                        phase = BackupPhase.NoSpace
                        isWorking = false
                        return@launch
                    }
                    is BackupOpenResult.Failed -> {
                        resultMessage = openResult.message
                        phase = BackupPhase.RestoreFailed
                        onSetStatus(getString(Res.string.restore_failed, openResult.message))
                        isWorking = false
                        return@launch
                    }
                    is BackupOpenResult.Opened -> {
                        openedResult = openResult
                        pendingOpen = openResult
                    }
                }

                phase = BackupPhase.Opening
                val validated = try {
                    withContext(ioDispatcher) {
                        BackupRestoreService.readBackup(openResult.path)
                    }
                } catch (e: BackupRestoreService.InvalidBackupException) {
                    phase = BackupPhase.Invalid
                    if (openResult.isTemporary) {
                        BackupFileHelper.release(openResult.path)
                        pendingOpen = null
                    }
                    isWorking = false
                    return@launch
                }

                pendingBackup = validated
                phase = BackupPhase.Confirm
            } catch (c: CancellationException) {
                if (openedResult?.isTemporary == true) {
                    BackupFileHelper.release(openedResult.path)
                    pendingOpen = null
                }
                phase = BackupPhase.Idle
                isWorking = false
                throw c
            } catch (e: Throwable) {
                if (openedResult?.isTemporary == true) {
                    BackupFileHelper.release(openedResult.path)
                    pendingOpen = null
                }
                val msg = e.message ?: "Unknown error"
                resultMessage = msg
                phase = BackupPhase.RestoreFailed
                onSetStatus(getString(Res.string.restore_failed, msg))
                isWorking = false
            }
        }
    }

    fun confirmRestore() {
        val validated = pendingBackup ?: return
        val openResult = pendingOpen ?: return
        coroutineScope.launch {
            progressDone = 0
            progressTotal = 0
            phase = BackupPhase.Restoring
            try {
                val targetDir = dataPath!!.toPath()
                val summary = BackupRestoreService.restoreBackup(
                    db = db,
                    backup = validated,
                    imagesDir = targetDir
                ) { done, total ->
                    progressDone = done
                    progressTotal = total
                }
                val loader = coil3.SingletonImageLoader.get(platformContext)
                loader.memoryCache?.clear()
                loader.diskCache?.clear()

                onCollectionRestored()

                summaryToys = summary.toys
                summaryMakers = summary.makers
                summaryPhotos = summary.photos
                phase = BackupPhase.Restored
                onSetStatus(getString(Res.string.restore_done_title))
            } catch (c: CancellationException) {
                phase = BackupPhase.Idle
                throw c
            } catch (e: Throwable) {
                val msg = e.message ?: "Unknown error"
                resultMessage = msg
                phase = BackupPhase.RestoreFailed
                onSetStatus(getString(Res.string.restore_failed, msg))
            } finally {
                if (openResult.isTemporary) {
                    BackupFileHelper.release(openResult.path)
                }
                pendingOpen = null
                pendingBackup = null
                isWorking = false
            }
        }
    }

    fun dismissConfirm() {
        if (pendingOpen?.isTemporary == true) {
            BackupFileHelper.release(pendingOpen!!.path)
        }
        pendingOpen = null
        pendingBackup = null
        phase = BackupPhase.Idle
        isWorking = false
    }

    BackupRestoreCardContent(
        isWorking = isWorking,
        onBackUp = ::startBackup,
        onRestore = ::startRestore,
        modifier = modifier
    )

    BackupRestoreDialogs(
        phase = phase,
        progressDone = progressDone,
        progressTotal = progressTotal,
        resultMessage = resultMessage,
        savedLocation = savedLocation,
        summaryToys = summaryToys,
        summaryMakers = summaryMakers,
        summaryPhotos = summaryPhotos,
        summaryMissingPhotos = summaryMissingPhotos,
        pendingManifest = pendingBackup?.manifest,
        onDismiss = { phase = BackupPhase.Idle },
        onConfirmRestore = ::confirmRestore,
        onDismissConfirm = ::dismissConfirm
    )
}

@Composable
fun BackupRestoreCardContent(
    isWorking: Boolean,
    onBackUp: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GcSpacing.Small),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text(
                text = stringResource(Res.string.backup_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = sysTextColor()
            )
            Spacer(modifier = Modifier.height(GcSpacing.Small))
            Text(
                text = stringResource(Res.string.backup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = sysTextColor()
            )
            Spacer(modifier = Modifier.height(GcSpacing.Standard))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
            ) {
                Button(
                    onClick = onBackUp,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.backup_btn))
                }
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.restore_btn))
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreDialogs(
    phase: BackupPhase,
    progressDone: Int,
    progressTotal: Int,
    resultMessage: String,
    savedLocation: String,
    summaryToys: Int,
    summaryMakers: Int,
    summaryPhotos: Int,
    summaryMissingPhotos: Int,
    pendingManifest: BackupManifest?,
    onDismiss: () -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissConfirm: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogModifier = if (isDark) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), AlertDialogDefaults.shape)
    } else {
        Modifier
    }

    when (phase) {
        BackupPhase.Idle -> {}

        BackupPhase.Saving -> {
            AlertDialog(
                onDismissRequest = {},
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.backup_saving), color = sysTextColor()) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        if (progressTotal > 0) {
                            Spacer(modifier = Modifier.height(GcSpacing.Standard))
                            LinearProgressIndicator(
                                progress = { progressDone.toFloat() / progressTotal.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(GcSpacing.Small))
                            Text(
                                text = stringResource(Res.string.backup_progress, progressDone, progressTotal),
                                color = sysTextColor()
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        BackupPhase.Opening -> {
            AlertDialog(
                onDismissRequest = {},
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.restore_running), color = sysTextColor()) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {}
            )
        }

        BackupPhase.Confirm -> {
            AlertDialog(
                onDismissRequest = onDismissConfirm,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.restore_confirm_title), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(
                            Res.string.restore_confirm_msg,
                            pendingManifest?.toys ?: 0,
                            pendingManifest?.makers ?: 0,
                            pendingManifest?.photos ?: 0
                        ),
                        color = sysTextColor()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmRestore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(Res.string.restore_confirm_btn))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismissConfirm) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }

        BackupPhase.Restoring -> {
            AlertDialog(
                onDismissRequest = {},
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.restore_running), color = sysTextColor()) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        if (progressTotal > 0) {
                            Spacer(modifier = Modifier.height(GcSpacing.Standard))
                            LinearProgressIndicator(
                                progress = { progressDone.toFloat() / progressTotal.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(GcSpacing.Small))
                            Text(
                                text = stringResource(Res.string.backup_progress, progressDone, progressTotal),
                                color = sysTextColor()
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        BackupPhase.Saved -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.backup_done_title), color = sysTextColor()) },
                text = {
                    Column {
                        Text(
                            text = stringResource(Res.string.backup_done_msg, summaryToys, summaryPhotos, savedLocation),
                            color = sysTextColor()
                        )
                        if (summaryMissingPhotos > 0) {
                            Spacer(modifier = Modifier.height(GcSpacing.Small))
                            Text(
                                text = stringResource(Res.string.backup_missing_photos, summaryMissingPhotos),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        BackupPhase.SaveFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.backup_title), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(Res.string.backup_failed, resultMessage),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        BackupPhase.Restored -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.restore_done_title), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(Res.string.restore_done_msg, summaryToys, summaryMakers, summaryPhotos),
                        color = sysTextColor()
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        BackupPhase.RestoreFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.restore_btn), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(Res.string.restore_failed, resultMessage),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        BackupPhase.NotFound -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.restore_missing_title), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(Res.string.restore_missing_msg),
                        color = sysTextColor()
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        BackupPhase.Invalid -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.restore_btn), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(Res.string.restore_invalid_file),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        BackupPhase.NoDataDir -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.backup_title), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(Res.string.backup_no_data_dir),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        BackupPhase.NoSpace -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.restore_btn), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(Res.string.restore_no_space),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        BackupPhase.PermissionDenied -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = dialogModifier,
                containerColor = sysBackgroundColor(),
                title = { Text(stringResource(Res.string.backup_title), color = sysTextColor()) },
                text = {
                    Text(
                        text = stringResource(Res.string.backup_permission_denied),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BackupRestoreCardPreview() {
    GcTheme {
        Surface {
            BackupRestoreCardContent(
                isWorking = false,
                onBackUp = {},
                onRestore = {}
            )
        }
    }
}

@Preview(name = "Landscape", widthDp = 800, heightDp = 400)
@Composable
private fun BackupRestoreCardLandscapePreview() {
    GcTheme {
        Surface {
            BackupRestoreCardContent(
                isWorking = false,
                onBackUp = {},
                onRestore = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BackupRestoreConfirmDialogPreview() {
    GcTheme {
        BackupRestoreDialogs(
            phase = BackupPhase.Confirm,
            progressDone = 0,
            progressTotal = 0,
            resultMessage = "",
            savedLocation = "",
            summaryToys = 100,
            summaryMakers = 20,
            summaryPhotos = 150,
            summaryMissingPhotos = 0,
            pendingManifest = BackupManifest(
                format = "gepetto-toy-collection-backup",
                formatVersion = 1,
                createdAt = "2026-10-07",
                appVersionCode = "1",
                categories = 5,
                makers = 20,
                toys = 100,
                photos = 150
            ),
            onDismiss = {},
            onConfirmRestore = {},
            onDismissConfirm = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun BackupRestoreSavingDialogPreview() {
    GcTheme {
        BackupRestoreDialogs(
            phase = BackupPhase.Saving,
            progressDone = 45,
            progressTotal = 150,
            resultMessage = "",
            savedLocation = "",
            summaryToys = 0,
            summaryMakers = 0,
            summaryPhotos = 0,
            summaryMissingPhotos = 0,
            pendingManifest = null,
            onDismiss = {},
            onConfirmRestore = {},
            onDismissConfirm = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun BackupRestoreSavedWithMissingPhotosPreview() {
    GcTheme {
        BackupRestoreDialogs(
            phase = BackupPhase.Saved,
            progressDone = 0,
            progressTotal = 0,
            resultMessage = "",
            savedLocation = "/Downloads/toy_collection_backup.zip",
            summaryToys = 100,
            summaryMakers = 20,
            summaryPhotos = 145,
            summaryMissingPhotos = 5,
            pendingManifest = null,
            onDismiss = {},
            onConfirmRestore = {},
            onDismissConfirm = {}
        )
    }
}
