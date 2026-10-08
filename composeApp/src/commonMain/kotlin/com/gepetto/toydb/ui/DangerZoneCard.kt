package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.service.DatabaseResetService
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@Composable
fun DangerZoneCard(
    db: ToyDatabase,
    onCollectionReset: () -> Unit,
    onSetStatus: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val platformContext = coil3.compose.LocalPlatformContext.current

    var showConfirmDialog by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun executeReset() {
        coroutineScope.launch {
            isResetting = true
            errorMessage = null
            successMessage = null
            val result = DatabaseResetService.resetDatabaseToDefaults(db, platformContext)
            isResetting = false
            result.onSuccess {
                val doneMsg = "Database reset complete. You can now maintain your own collection."
                successMessage = doneMsg
                onSetStatus(doneMsg)
                onCollectionReset()
            }.onFailure { err ->
                errorMessage = err.message ?: "Unknown error"
            }
        }
    }

    DangerZoneCardContent(
        isResetting = isResetting,
        errorMessage = errorMessage,
        successMessage = successMessage,
        onResetClick = { showConfirmDialog = true },
        modifier = modifier
    )

    if (showConfirmDialog) {
        val isDark = isSystemInDarkTheme()
        val dialogShape = RoundedCornerShape(16.dp)
        AlertDialog(
            onDismissRequest = { if (!isResetting) showConfirmDialog = false },
            modifier = Modifier.then(
                if (isDark) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, dialogShape) else Modifier
            ),
            shape = dialogShape,
            containerColor = sysBackgroundColor(),
            titleContentColor = MaterialTheme.colorScheme.error,
            textContentColor = sysTextColor(),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(Res.string.danger_zone_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.danger_zone_confirm_msg),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        executeReset()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(Res.string.danger_zone_confirm_btn))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
fun DangerZoneCardContent(
    isResetting: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = sysBackgroundColor()
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GcSpacing.Standard)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(Res.string.danger_zone_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.danger_zone_desc),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = sysTextColor()
            )

            if (!errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.danger_zone_failed, errorMessage),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!successMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.danger_zone_done),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(GcSpacing.Standard))

            if (isResetting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(Res.string.danger_zone_resetting),
                        fontSize = 14.sp,
                        color = sysTextColor()
                    )
                }
            } else {
                Button(
                    onClick = onResetClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.danger_zone_reset_btn),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun DangerZoneCardPreview() {
    GcTheme {
        DangerZoneCardContent(
            isResetting = false,
            errorMessage = null,
            successMessage = null,
            onResetClick = {}
        )
    }
}
