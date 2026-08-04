package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.Maker
import com.gepetto.toydb.database.ToyRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@Composable
fun MakerForm(
    repository: ToyRepository,
    initialMaker: Maker,
    isEditMode: Boolean,
    onSave: (Maker) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialMaker.name) }
    var country by remember { mutableStateOf(initialMaker.country) }
    var bitmaps by remember { mutableStateOf(initialMaker.bitmaps) }
    var bitmapsSize by remember { mutableStateOf(initialMaker.bitmapsSize) }
    var bitmapsTimeStamp by remember { mutableStateOf(initialMaker.bitmapsTimeStamp) }
    var comments by remember { mutableStateOf(initialMaker.comments) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(GcSpacing.Standard)
    ) {
        Text(
            text = if (isEditMode) stringResource(Res.string.edit_manufacturer) else stringResource(Res.string.add_manufacturer),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = sysTextColor()
        )
        
        Spacer(modifier = Modifier.height(GcSpacing.Standard))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(Res.string.manufacturer_name_required)) },
            enabled = !isEditMode, // Name is primary key, cannot edit
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = country,
            onValueChange = { country = it },
            label = { Text(stringResource(Res.string.manufacturer_country)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = bitmaps,
                    onValueChange = { bitmaps = it },
                    label = { Text(stringResource(Res.string.manufacturer_bitmaps)) },
                    placeholder = { Text(stringResource(Res.string.manufacturer_bitmaps_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.width(GcSpacing.Small))
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Filenames")
            }
        }

        if (showRenameDialog) {
            ImageRenameDialog(
                bitmapsStr = bitmaps,
                bitmapsSizeStr = bitmapsSize,
                bitmapsTimeStampStr = bitmapsTimeStamp,
                repository = repository,
                onDismiss = { showRenameDialog = false },
                onSave = { newBitmaps, newSizes, newTimestamps ->
                    bitmaps = newBitmaps
                    bitmapsSize = newSizes
                    bitmapsTimeStamp = newTimestamps
                    
                    if (isEditMode) {
                        val updatedMaker = initialMaker.copy(
                            name = name.trim(),
                            country = country.trim(),
                            bitmaps = newBitmaps,
                            bitmapsSize = newSizes,
                            bitmapsTimeStamp = newTimestamps,
                            comments = comments.trim()
                        )
                        repository.saveMaker(updatedMaker)
                    }
                    showRenameDialog = false
                }
            )
        }

        OutlinedTextField(
            value = comments,
            onValueChange = { comments = it },
            label = { Text(stringResource(Res.string.manufacturer_comments)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(GcSpacing.Standard))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(end = GcSpacing.Small)
            ) {
                Text(stringResource(Res.string.cancel))
            }
            Button(
                onClick = {
                    if (name.trim().isEmpty()) return@Button
                    val maker = Maker(
                        name = name.trim(),
                        country = country.trim(),
                        bitmaps = bitmaps.trim(),
                        bitmapsSize = bitmapsSize,
                        bitmapsTimeStamp = bitmapsTimeStamp,
                        comments = comments.trim()
                    )
                    onSave(maker)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = name.trim().isNotEmpty()
            ) {
                Text(stringResource(Res.string.save))
            }
        }
    }
}
