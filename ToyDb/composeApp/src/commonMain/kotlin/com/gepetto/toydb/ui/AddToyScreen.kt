package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.gepetto.toydb.database.Toy
import com.gepetto.toydb.database.ToyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToyScreen(
    repository: ToyRepository,
    category: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextRef = remember { repository.getNextRefNum(category) }
    val blankToy = remember {
        Toy(
            refNum = nextRef,
            toyType = category,
            description = ""
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Toy (#$nextRef)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        ToyForm(
            initialToy = blankToy,
            onSave = { newToy ->
                repository.saveToy(newToy)
                onBack()
            },
            onCancel = onBack,
            modifier = modifier.padding(innerPadding)
        )
    }
}
