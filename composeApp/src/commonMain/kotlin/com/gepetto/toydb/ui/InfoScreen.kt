package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import club.gepetto.GcLog
import club.gepetto.composeutils.GcMarkdown
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import androidx.compose.ui.graphics.Color
import club.gepetto.composeutils.sysLinkColor
import com.gepetto.toydb.CommonConfig
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import toydb.composeapp.generated.resources.*

@Composable
fun InfoScreen(
    onNavigateToSftpSetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var aboutMarkdown by remember { mutableStateOf("") }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var privacyPolicyMarkdown by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            aboutMarkdown = Res.readBytes("files/about.md").decodeToString()
        } catch (e: Exception) {
            GcLog.e("InfoScreen", "Failed to load about.md: ${e.message}", e)
            aboutMarkdown = getString(Res.string.failed_load_about)
        }
    }

    LaunchedEffect(showPrivacyPolicyDialog) {
        if (showPrivacyPolicyDialog && privacyPolicyMarkdown.isEmpty()) {
            try {
                privacyPolicyMarkdown = Res.readBytes("files/en_privacypolicy.md").decodeToString()
            } catch (e: Exception) {
                GcLog.e("InfoScreen", "Failed to load en_privacypolicy.md: ${e.message}", e)
                privacyPolicyMarkdown = getString(Res.string.failed_load_privacy)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = sysBackgroundColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = sysTextColor(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.version_format, CommonConfig.versionName, CommonConfig.versionCode),
                style = MaterialTheme.typography.bodyMedium,
                color = sysTextColor().copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.copyright) + stringResource(Res.string.rights_reserved),
                style = MaterialTheme.typography.bodySmall,
                color = sysTextColor().copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            if (aboutMarkdown.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    GcMarkdown(
                        content = aboutMarkdown,
                        textColor = sysTextColor()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.backup_sync_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = sysTextColor()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.backup_sync_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = sysTextColor().copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onNavigateToSftpSetup,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = stringResource(Res.string.sftp_setup_guide_btn),
                            style = MaterialTheme.typography.labelLarge,
                            color = sysLinkColor()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { showPrivacyPolicyDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = stringResource(Res.string.privacy_policy_btn),
                            style = MaterialTheme.typography.labelLarge,
                            color = sysLinkColor()
                        )
                    }
                }
            } else {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text(stringResource(Res.string.close), color = sysTextColor())
                }
            },
            title = {
                Text(
                    text = stringResource(Res.string.privacy_policy_btn),
                    fontWeight = FontWeight.Bold,
                    color = sysTextColor()
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (privacyPolicyMarkdown.isNotEmpty()) {
                        GcMarkdown(
                            content = privacyPolicyMarkdown,
                            textColor = sysTextColor()
                        )
                    } else {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            },
            containerColor = sysBackgroundColor()
        )
    }
}
