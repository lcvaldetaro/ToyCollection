package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.gepetto.toydb.database.Maker
import com.gepetto.toydb.database.ToyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMakerScreen(
    repository: ToyRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blankMaker = remember {
        Maker(
            name = "",
            country = "",
            bitmaps = "",
            bitmapsSize = "",
            bitmapsTimeStamp = "",
            comments = ""
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Manufacturer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        MakerForm(
            initialMaker = blankMaker,
            isEditMode = false,
            onSave = { newMaker ->
                repository.saveMaker(newMaker)
                onBack()
            },
            onCancel = onBack,
            modifier = modifier.padding(innerPadding)
        )
    }
}
