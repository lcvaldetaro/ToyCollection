package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import club.gepetto.GcLog
import club.gepetto.composeutils.GcMarkdown
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import toydb.composeapp.generated.resources.Res

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpSetupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var setupMarkdown by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            setupMarkdown = Res.readBytes("files/sftp_setup.md").decodeToString()
        } catch (e: Exception) {
            GcLog.e("SftpSetupScreen", "Failed to load sftp_setup.md: ${e.message}", e)
            setupMarkdown = "Failed to load SFTP server setup guide."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SFTP Setup Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = sysBackgroundColor(),
        modifier = modifier.fillMaxSize().imePadding()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = sysBackgroundColor()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                if (setupMarkdown.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        GcMarkdown(
                            content = setupMarkdown,
                            textColor = sysTextColor()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
