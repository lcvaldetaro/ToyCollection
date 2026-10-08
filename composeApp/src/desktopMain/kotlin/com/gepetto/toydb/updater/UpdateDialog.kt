package com.gepetto.toydb.updater

import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
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
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = sysForegroundColor()
    AlertDialog(
        modifier = modifier.border(width = 2.dp, color = textColor, shape = MaterialTheme.shapes.medium),
        containerColor = sysBackgroundColor(),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.update_available_title), color = textColor)
        },
        text = {
            Text(stringResource(Res.string.update_available_message, newVersionName), color = textColor)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.update_btn_update), color = textColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.update_btn_later), color = textColor)
            }
        }
    )
}
