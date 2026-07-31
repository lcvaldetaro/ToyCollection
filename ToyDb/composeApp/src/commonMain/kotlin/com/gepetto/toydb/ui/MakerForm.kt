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
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@Composable
fun MakerForm(
    initialMaker: Maker,
    isEditMode: Boolean,
    onSave: (Maker) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialMaker.name) }
    var country by remember { mutableStateOf(initialMaker.country) }
    var bitmaps by remember { mutableStateOf(initialMaker.bitmaps) }
    var comments by remember { mutableStateOf(initialMaker.comments) }

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

        OutlinedTextField(
            value = bitmaps,
            onValueChange = { bitmaps = it },
            label = { Text(stringResource(Res.string.manufacturer_bitmaps)) },
            placeholder = { Text(stringResource(Res.string.manufacturer_bitmaps_placeholder)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            singleLine = true
        )

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
                        bitmapsSize = initialMaker.bitmapsSize,
                        bitmapsTimeStamp = initialMaker.bitmapsTimeStamp,
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
