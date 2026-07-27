package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.gepetto.toydb.database.ToyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMakerScreen(
    repository: ToyRepository,
    makerName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maker = remember { repository.getMaker(makerName) }

    if (maker == null) {
        Box(modifier = modifier.fillMaxSize()) {
            Text("Manufacturer not found.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Manufacturer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        MakerForm(
            initialMaker = maker,
            isEditMode = true,
            onSave = { updatedMaker ->
                repository.saveMaker(updatedMaker)
                onBack()
            },
            onCancel = onBack,
            modifier = modifier.padding(innerPadding)
        )
    }
}
