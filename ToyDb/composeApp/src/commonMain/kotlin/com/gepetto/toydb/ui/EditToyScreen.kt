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
fun EditToyScreen(
    repository: ToyRepository,
    toyType: String,
    refNum: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toy = remember { repository.getToy(toyType, refNum) }

    if (toy == null) {
        Box(modifier = modifier.fillMaxSize()) {
            Text("Toy not found.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Toy #${toy.refNum}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        ToyForm(
            initialToy = toy,
            onSave = { updatedToy ->
                repository.saveToy(updatedToy)
                onBack()
            },
            onCancel = onBack,
            modifier = modifier.padding(innerPadding)
        )
    }
}
