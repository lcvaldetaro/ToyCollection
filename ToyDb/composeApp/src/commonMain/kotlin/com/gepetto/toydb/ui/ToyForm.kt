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
import com.gepetto.toydb.database.Toy
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*
import club.gepetto.GcLog
import club.gepetto.composeutils.image.GcImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.rememberImagePicker
import okio.FileSystem
import okio.Path.Companion.toPath
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit

@Composable
fun ToyForm(
    repository: ToyRepository,
    initialToy: Toy,
    onSave: (Toy) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(Res.string.tab_general),
        stringResource(Res.string.tab_makers_parts),
        stringResource(Res.string.tab_financials),
        stringResource(Res.string.tab_restoration),
        stringResource(Res.string.tab_images)
    )

    // General state
    var description by remember { mutableStateOf(initialToy.description) }
    var scale by remember { mutableStateOf(initialToy.scale) }
    var condition by remember { mutableStateOf(initialToy.condition) }
    var color by remember { mutableStateOf(initialToy.color) }
    var boxed by remember { mutableStateOf(initialToy.boxed == "y") }
    var factoryCar by remember { mutableStateOf(initialToy.factoryCar == "y") }

    // Makers state
    var bodyMaker by remember { mutableStateOf(initialToy.bodyMaker) }
    var chassisMaker by remember { mutableStateOf(initialToy.chassisMaker) }
    var chassisType by remember { mutableStateOf(initialToy.chassisType) }
    var motorMaker by remember { mutableStateOf(initialToy.motorMaker) }
    var motorDetails by remember { mutableStateOf(initialToy.motorDetails) }
    var catalogNumber by remember { mutableStateOf(initialToy.catalogNumber) }

    // Financials state
    var acquired by remember { mutableStateOf(initialToy.acquired) }
    var amountPaid by remember { mutableStateOf(if (initialToy.amountPaid > 0) initialToy.amountPaid.toString() else "") }
    var value by remember { mutableStateOf(if (initialToy.value > 0) initialToy.value.toString() else "") }
    var amountSold by remember { mutableStateOf(initialToy.amountSold) }
    var traded by remember { mutableStateOf(initialToy.traded) }
    var buy by remember { mutableStateOf(initialToy.buy) }

    // Restoration state
    var majorWork by remember { mutableStateOf(initialToy.majorWork) }
    var minorWork by remember { mutableStateOf(initialToy.minorWork) }
    var repro by remember { mutableStateOf(initialToy.repro) }
    var maintenance by remember { mutableStateOf(initialToy.maintenance) }
    var toMake by remember { mutableStateOf(initialToy.toMake) }
    var detail by remember { mutableStateOf(initialToy.detail) }
    var comments by remember { mutableStateOf(initialToy.comments) }

    // Images state
    var picture by remember { mutableStateOf(initialToy.picture) }
    var pictureSize by remember { mutableStateOf(if (initialToy.pictureSize > 0) initialToy.pictureSize.toString() else "") }
    var pictureTimeStamp by remember { mutableStateOf(if (initialToy.pictureTimeStamp > 0) initialToy.pictureTimeStamp.toString() else "") }
    var hasPicture by remember { mutableStateOf(initialToy.hasPicture == "y") }
    var bitmaps by remember { mutableStateOf(initialToy.bitmaps) }
    var bitmapsSize by remember { mutableStateOf(initialToy.bitmapsSize) }
    var bitmapsTimeStamp by remember { mutableStateOf(initialToy.bitmapsTimeStamp) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val categorySettings = remember { repository.getCategorySettings() }
    val prefix = remember(initialToy.toyType, categorySettings) {
        categorySettings.find { it.category == initialToy.toyType }?.imagePrefix ?: "car"
    }

    var pendingImagePath by remember { mutableStateOf<String?>(null) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var overwriteDestPath by remember { mutableStateOf("") }

    fun performCopyImage(selectedPath: String) {
        val srcPath = selectedPath.toPath()
        val extension = srcPath.name.substringAfterLast('.', "jpg").lowercase()
        val destFilename = "$prefix${initialToy.refNum}.$extension"
        
        val customPath = repository.getDataPathSetting()
        val targetDir = if (!customPath.isNullOrEmpty()) {
            customPath.toPath()
        } else {
            val possibleDirs = listOf("images", "../images", "ToyDb/images", "../ToyDb/images")
            possibleDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) } ?: "images".toPath()
        }
        
        val destPath = targetDir.div(destFilename)
        try {
            if (!FileSystem.SYSTEM.exists(targetDir)) {
                FileSystem.SYSTEM.createDirectories(targetDir)
            }
            if (FileSystem.SYSTEM.exists(destPath)) {
                FileSystem.SYSTEM.delete(destPath)
            }
            FileSystem.SYSTEM.copy(srcPath, destPath)
            
            val size = FileSystem.SYSTEM.metadataOrNull(destPath)?.size ?: 0L
            val timestamp = System.currentTimeMillis()
            
            picture = destFilename
            pictureSize = size.toString()
            pictureTimeStamp = timestamp.toString()
            hasPicture = true
            
            GcLog.d("ToyForm", "Successfully saved main picture $destFilename")
        } catch (e: Exception) {
            GcLog.e("ToyForm", "Error saving main picture: ${e.message}", e)
        }
    }

    val imagePicker = rememberImagePicker { selectedPath ->
        val srcPath = selectedPath.toPath()
        val extension = srcPath.name.substringAfterLast('.', "jpg").lowercase()
        val destFilename = "$prefix${initialToy.refNum}.$extension"
        
        val customPath = repository.getDataPathSetting()
        val targetDir = if (!customPath.isNullOrEmpty()) {
            customPath.toPath()
        } else {
            val possibleDirs = listOf("images", "../images", "ToyDb/images", "../ToyDb/images")
            possibleDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) } ?: "images".toPath()
        }
        
        val destPath = targetDir.div(destFilename)
        if (FileSystem.SYSTEM.exists(destPath)) {
            pendingImagePath = selectedPath
            overwriteDestPath = destPath.toString()
            showOverwriteDialog = true
        } else {
            performCopyImage(selectedPath)
        }
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(GcSpacing.Standard)
        ) {
            when (tabIndex) {
                0 -> { // General
                    FormField(label = stringResource(Res.string.form_field_description_required), value = description, onValueChange = { description = it })
                    FormField(label = stringResource(Res.string.form_field_scale), value = scale, onValueChange = { scale = it })
                    FormField(label = stringResource(Res.string.form_field_condition), value = condition, onValueChange = { condition = it })
                    FormField(label = stringResource(Res.string.form_field_color), value = color, onValueChange = { color = it })
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = boxed, onCheckedChange = { boxed = it })
                        Text(stringResource(Res.string.form_field_boxed), color = sysTextColor())
                        Spacer(modifier = Modifier.width(GcSpacing.Large))
                        Checkbox(checked = factoryCar, onCheckedChange = { factoryCar = it })
                        Text(stringResource(Res.string.form_field_factory_car), color = sysTextColor())
                    }
                    FormField(label = stringResource(Res.string.form_field_general_comments), value = comments, onValueChange = { comments = it })
                }
                1 -> { // Makers
                    FormField(label = stringResource(Res.string.form_field_body_maker), value = bodyMaker, onValueChange = { bodyMaker = it })
                    FormField(label = stringResource(Res.string.form_field_chassis_maker), value = chassisMaker, onValueChange = { chassisMaker = it })
                    FormField(label = stringResource(Res.string.form_field_chassis_type), value = chassisType, onValueChange = { chassisType = it })
                    FormField(label = stringResource(Res.string.form_field_motor_maker), value = motorMaker, onValueChange = { motorMaker = it })
                    FormField(label = stringResource(Res.string.form_field_motor_details), value = motorDetails, onValueChange = { motorDetails = it })
                    FormField(label = stringResource(Res.string.form_field_catalog_number), value = catalogNumber, onValueChange = { catalogNumber = it })
                }
                2 -> { // Financials
                    FormField(label = stringResource(Res.string.form_field_acquisition_details), value = acquired, onValueChange = { acquired = it })
                    FormField(label = stringResource(Res.string.form_field_amount_paid), value = amountPaid, onValueChange = { amountPaid = it })
                    FormField(label = stringResource(Res.string.form_field_estimated_value), value = value, onValueChange = { value = it })
                    FormField(label = stringResource(Res.string.form_field_amount_sold), value = amountSold, onValueChange = { amountSold = it })
                    FormField(label = stringResource(Res.string.form_field_traded_info), value = traded, onValueChange = { traded = it })
                    FormField(label = stringResource(Res.string.form_field_buy_info), value = buy, onValueChange = { buy = it })
                }
                3 -> { // Restoration
                    FormField(label = stringResource(Res.string.form_field_major_work), value = majorWork, onValueChange = { majorWork = it })
                    FormField(label = stringResource(Res.string.form_field_minor_work), value = minorWork, onValueChange = { minorWork = it })
                    FormField(label = stringResource(Res.string.form_field_repro_details), value = repro, onValueChange = { repro = it })
                    FormField(label = stringResource(Res.string.form_field_maintenance_log), value = maintenance, onValueChange = { maintenance = it })
                    FormField(label = stringResource(Res.string.form_field_to_build_info), value = toMake, onValueChange = { toMake = it })
                    FormField(label = stringResource(Res.string.form_field_detail_decal_work), value = detail, onValueChange = { detail = it })
                }
                4 -> { // Images
                    FormField(label = stringResource(Res.string.form_field_main_picture), value = picture, onValueChange = { picture = it })
                    
                    Spacer(modifier = Modifier.height(GcSpacing.Small))
                    Button(
                        onClick = imagePicker,
                        modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small)
                    ) {
                        Text(stringResource(Res.string.add_picture_btn))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = hasPicture, onCheckedChange = { hasPicture = it })
                        Text(stringResource(Res.string.form_field_has_picture), color = sysTextColor())
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            FormField(label = stringResource(Res.string.form_field_secondary_bitmaps), value = bitmaps, onValueChange = { bitmaps = it })
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
                                
                                if (initialToy.refNum > 0) {
                                    val updatedToy = initialToy.copy(
                                        bitmaps = newBitmaps,
                                        bitmapsSize = newSizes,
                                        bitmapsTimeStamp = newTimestamps
                                    )
                                    repository.saveToy(updatedToy)
                                }
                                showRenameDialog = false
                            }
                        )
                    }
                }
            }
        }

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GcSpacing.Standard),
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
                    if (description.trim().isEmpty()) return@Button
                    val body = bodyMaker.trim()
                    val chassis = chassisMaker.trim()
                    val calculatedMakerCombo = if (body == chassis) {
                        body
                    } else if (body.isEmpty()) {
                        chassis
                    } else if (chassis.isEmpty()) {
                        body
                    } else {
                        "$chassis/$body"
                    }
                    val updated = Toy(
                        refNum = initialToy.refNum,
                        toyType = initialToy.toyType,
                        description = description,
                        makerCombo = calculatedMakerCombo,
                        scale = scale,
                        factoryCar = if (factoryCar) "y" else "n",
                        bodyMaker = bodyMaker,
                        acquired = acquired,
                        chassisType = chassisType,
                        chassisMaker = chassisMaker,
                        condition = condition,
                        color = color,
                        motorMaker = motorMaker,
                        motorDetails = motorDetails,
                        catalogNumber = catalogNumber,
                        comments = comments,
                        majorWork = majorWork,
                        minorWork = minorWork,
                        repro = repro,
                        value = value.toDoubleOrNull() ?: 0.0,
                        amountPaid = amountPaid.toDoubleOrNull() ?: 0.0,
                        amountSold = amountSold,
                        traded = traded,
                        buy = buy,
                        maintenance = maintenance,
                        toMake = toMake,
                        detail = detail,
                        boxed = if (boxed) "y" else "n",
                        picture = picture,
                        pictureSize = pictureSize.toIntOrNull() ?: 0,
                        pictureTimeStamp = pictureTimeStamp.toLongOrNull() ?: 0L,
                        hasPicture = if (hasPicture) "y" else "n",
                        bitmaps = bitmaps,
                        bitmapsSize = bitmapsSize,
                        bitmapsTimeStamp = bitmapsTimeStamp
                    )
                    onSave(updated)
                },
                enabled = description.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(Res.string.save))
            }
        }
    }

    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showOverwriteDialog = false
                pendingImagePath = null
            },
            title = { Text(stringResource(Res.string.overwrite_image_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.overwrite_image_confirm))
                    Spacer(modifier = Modifier.height(GcSpacing.Small))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(Res.string.current_image_label), style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            GcImage(
                                imageFile = overwriteDestPath,
                                contentDescription = "Current image",
                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(Res.string.new_image_label), style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            pendingImagePath?.let { path ->
                                GcImage(
                                    imageFile = path,
                                    contentDescription = "New image",
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverwriteDialog = false
                        pendingImagePath?.let { performCopyImage(it) }
                        pendingImagePath = null
                    }
                ) {
                    Text(stringResource(Res.string.overwrite))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showOverwriteDialog = false
                        pendingImagePath = null
                    }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = label != "General Comments" && !label.contains("Work")
    )
}
