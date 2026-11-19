package com.gepetto.toydb.updater

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor

@Composable
fun UpdateDialog(
    newVersionName: String,
    isDownloading: Boolean = false,
    downloadProgress: Float? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = sysForegroundColor()
    AlertDialog(
        modifier = modifier.border(width = 2.dp, color = textColor, shape = MaterialTheme.shapes.medium),
        containerColor = sysBackgroundColor(),
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Text(stringResource(Res.string.update_available_title), color = textColor)
        },
        text = {
            Column {
                if (isDownloading) {
                    Text(stringResource(Res.string.update_downloading_message, newVersionName), color = textColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (downloadProgress != null) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(stringResource(Res.string.update_available_message, newVersionName), color = textColor)
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(Res.string.update_btn_update), color = textColor)
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.update_btn_later), color = textColor)
                }
            }
        }
    )
}
