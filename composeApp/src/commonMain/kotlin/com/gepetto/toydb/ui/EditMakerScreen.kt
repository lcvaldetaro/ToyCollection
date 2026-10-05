package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.RenameMakerResult
import com.gepetto.toydb.database.ToyRepository
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMakerScreen(
    repository: ToyRepository,
    makerName: String,
    onBack: () -> Unit,
    onRenamed: ((oldName: String, newName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val maker = remember { repository.getMaker(makerName) }
    var errorDuplicateName by remember { mutableStateOf<String?>(null) }

    if (maker == null) {
        Box(modifier = modifier.fillMaxSize()) {
            Text(stringResource(Res.string.manufacturer_not_found))
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.edit_manufacturer)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        MakerForm(
            repository = repository,
            initialMaker = maker,
            isEditMode = true,
            onSave = { updatedMaker ->
                if (updatedMaker.name.trim() != maker.name.trim()) {
                    when (repository.renameMaker(maker.name, updatedMaker)) {
                        is RenameMakerResult.Success -> {
                            if (onRenamed != null) {
                                onRenamed(maker.name, updatedMaker.name.trim())
                            } else {
                                onBack()
                            }
                        }
                        is RenameMakerResult.NameAlreadyExists -> {
                            errorDuplicateName = updatedMaker.name.trim()
                        }
                        is RenameMakerResult.InvalidName -> {}
                        is RenameMakerResult.Error -> {}
                    }
                } else {
                    repository.saveMaker(updatedMaker)
                    onBack()
                }
            },
            onCancel = onBack,
            modifier = modifier.padding(innerPadding)
        )

        if (errorDuplicateName != null) {
            AlertDialog(
                onDismissRequest = { errorDuplicateName = null },
                containerColor = sysBackgroundColor(),
                titleContentColor = sysTextColor(),
                textContentColor = sysTextColor(),
                title = {
                    Text(
                        text = stringResource(Res.string.edit_manufacturer),
                        fontWeight = FontWeight.Bold,
                        color = sysTextColor()
                    )
                },
                text = {
                    Text(
                        text = stringResource(Res.string.maker_already_exists, errorDuplicateName!!),
                        color = sysTextColor()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { errorDuplicateName = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }
    }
}
