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
import com.gepetto.toydb.CommonConfig
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.Res
import toydb.composeapp.generated.resources.app_name
import toydb.composeapp.generated.resources.copyright

@Composable
fun InfoScreen(
    onNavigateToSftpSetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var aboutMarkdown by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            aboutMarkdown = Res.readBytes("files/about.md").decodeToString()
        } catch (e: Exception) {
            GcLog.e("InfoScreen", "Failed to load about.md: ${e.message}", e)
            aboutMarkdown = "Failed to load application information."
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
                text = "Version ${CommonConfig.versionName} (${CommonConfig.versionCode})",
                style = MaterialTheme.typography.bodyMedium,
                color = sysTextColor().copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.copyright) + "\nAll rights reserved.",
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
                        text = "Backup & Synchronization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = sysTextColor()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "If you want to back up your collection data safely to the cloud, or synchronize your data across several devices (such as your phone, tablet, and computer), you will need to set up a private SFTP server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = sysTextColor().copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToSftpSetup,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SFTP Server Setup Guide")
                    }
                }
            } else {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
