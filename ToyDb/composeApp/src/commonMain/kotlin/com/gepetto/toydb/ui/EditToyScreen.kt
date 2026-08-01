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
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

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
            Text(stringResource(Res.string.toy_not_found))
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.edit_toy_title, toy.refNum)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        ToyForm(
            repository = repository,
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
