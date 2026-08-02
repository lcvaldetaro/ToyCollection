package com.gepetto.toydb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcGenericDialog
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor
import club.gepetto.composeutils.sysTextColor
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.rememberLazyListState
import club.gepetto.composeutils.image.GcImage
import com.gepetto.toydb.database.Toy
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.resolveBitmapUri
import com.gepetto.toydb.utils.resolveImageUri
import com.gepetto.toydb.utils.scrollHorizontallyWithMouseWheel
import com.gepetto.toydb.utils.rememberImagePicker
import androidx.compose.material.icons.filled.Add
import okio.FileSystem
import okio.Path.Companion.toPath
import club.gepetto.GcLog
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToyDetailScreen(
    repository: ToyRepository,
    toyType: String,
    refNum: Int,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var toyState by remember(toyType, refNum) { mutableStateOf(repository.getToy(toyType, refNum)) }
    val toy = toyState
    val settingsList = remember { repository.getCategorySettings() }
    val prefix = remember { settingsList.find { it.category == toyType }?.imagePrefix ?: "car" }
    val allImagePaths = remember(toy) {
        val list = mutableListOf<String>()
        if (toy != null) {
            resolveImageUri(prefix, toy.refNum)?.let { list.add(it) }
            toy.getSecondaryImages().forEach { img ->
                resolveBitmapUri(img.filename)?.let { list.add(it) }
            }
        }
        list.toTypedArray()
    }
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val bitmapsScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberImagePicker { selectedPath ->
        val currentToy = toyState ?: return@rememberImagePicker
        val srcPath = selectedPath.toPath()
        val filename = srcPath.name
        
        val customPath = repository.getDataPathSetting()
        val targetDir = if (!customPath.isNullOrEmpty()) {
            customPath.toPath()
        } else {
            val possibleDirs = listOf("images", "../images", "ToyDb/images", "../ToyDb/images")
            possibleDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) } ?: "images".toPath()
        }
        
        val destPath = targetDir.div(filename)
        try {
            if (!FileSystem.SYSTEM.exists(targetDir)) {
                FileSystem.SYSTEM.createDirectories(targetDir)
            }
            FileSystem.SYSTEM.copy(srcPath, destPath)
            
            val size = FileSystem.SYSTEM.metadataOrNull(destPath)?.size ?: 0L
            val timestamp = System.currentTimeMillis()
            
            val currentBitmaps = currentToy.bitmaps.trim()
            val currentSizes = currentToy.bitmapsSize.trim()
            val currentTimestamps = currentToy.bitmapsTimeStamp.trim()
            
            val newBitmaps = if (currentBitmaps.isEmpty()) filename else "$currentBitmaps $filename"
            val newSizes = if (currentSizes.isEmpty()) size.toString() else "$currentSizes $size"
            val newTimestamps = if (currentTimestamps.isEmpty()) timestamp.toString() else "$currentTimestamps $timestamp"
            
            val updatedToy = currentToy.copy(
                bitmaps = newBitmaps,
                bitmapsSize = newSizes,
                bitmapsTimeStamp = newTimestamps
            )
            repository.saveToy(updatedToy)
            
            toyState = repository.getToy(toyType, refNum)
            GcLog.d("ToyDetailScreen", "Successfully uploaded and saved secondary image $filename")
        } catch (e: Exception) {
            GcLog.e("ToyDetailScreen", "Error uploading secondary image: ${e.message}", e)
        }
    }

    if (toy == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.toy_not_found))
        }
        return
    }

    if (showDeleteConfirmation) {
        GcGenericDialog(
            title = stringResource(Res.string.delete_toy_title),
            message = stringResource(Res.string.delete_toy_confirm, toy.refNum),
            buttonText = stringResource(Res.string.delete),
            onClick = {
                repository.deleteToy(toyType, refNum)
                showDeleteConfirmation = false
                onBack()
            }
        )
        // Note: The gepetto-utils GcGenericDialog does not have a dismiss/cancel button natively in some versions.
        // But we can let clicking on it trigger confirmation, or we can use a standard AlertDialog with cancel option.
        // Let's use a standard AlertDialog to have a clean cancel option for a premium UI experience!
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.toy_details_title, toy.refNum)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Destination.EditToy(toyType, refNum)) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = GcSpacing.Standard)
                .verticalScroll(rememberScrollState())
        ) {
            // Main Image
            val mainImg = remember(toy.refNum) { resolveImageUri(prefix, toy.refNum) }
            if (mainImg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = GcSpacing.Small),
                    contentAlignment = Alignment.Center
                ) {
                    GcImage(
                        imageFile = mainImg,
                        files = allImagePaths,
                        contentDescription = toy.description,
                        modifier = Modifier
                            .height(250.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                        fullImageOnClick = true
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.no_main_picture), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(GcSpacing.Small))

            // Details list
            Text(
                text = toy.description,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = sysTextColor()
            )

            Spacer(modifier = Modifier.height(GcSpacing.Small))

            // Primary stats grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard)) {
                DetailCard(label = stringResource(Res.string.form_field_scale), value = toy.scale, modifier = Modifier.weight(1f))
                DetailCard(label = stringResource(Res.string.form_field_condition), value = toy.condition, modifier = Modifier.weight(1f))
                DetailCard(label = stringResource(Res.string.value), value = if (toy.value > 0) "$${toy.value}" else stringResource(Res.string.not_set), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(GcSpacing.Small))

            DetailField(stringResource(Res.string.manufacturer), toy.makerCombo)
            DetailField(stringResource(Res.string.form_field_body_maker), toy.bodyMaker)
            DetailField(stringResource(Res.string.form_field_chassis_maker), toy.chassisMaker)
            DetailField(stringResource(Res.string.form_field_chassis_type), toy.chassisType)
            DetailField(stringResource(Res.string.motor), "${toy.motorMaker} ${toy.motorDetails}")
            DetailField(stringResource(Res.string.form_field_color), toy.color)
            DetailField(stringResource(Res.string.form_field_catalog_number), toy.catalogNumber)
            DetailField(stringResource(Res.string.form_field_acquisition_details), toy.acquired)
            DetailField(stringResource(Res.string.amount_paid), if (toy.amountPaid > 0) "$${toy.amountPaid}" else "")
            DetailField(stringResource(Res.string.amount_sold), toy.amountSold)
            DetailField(stringResource(Res.string.form_field_boxed), if (toy.boxed == "y") stringResource(Res.string.yes) else stringResource(Res.string.no))
            DetailField(stringResource(Res.string.form_field_factory_car), if (toy.factoryCar == "y") stringResource(Res.string.yes) else stringResource(Res.string.no))
            DetailField(stringResource(Res.string.repro_parts), toy.repro)
            DetailField(stringResource(Res.string.form_field_traded_info), toy.traded)
            DetailField(stringResource(Res.string.form_field_buy_info), toy.buy)
            DetailField(stringResource(Res.string.form_field_maintenance_log), toy.maintenance)
            DetailField(stringResource(Res.string.form_field_to_build_info), toy.toMake)
            DetailField(stringResource(Res.string.form_field_detail_decal_work), toy.detail)
            DetailField(stringResource(Res.string.form_field_major_work), toy.majorWork)
            DetailField(stringResource(Res.string.form_field_minor_work), toy.minorWork)
            DetailField(stringResource(Res.string.comments), toy.comments)

            // Secondary Images Gallery
            val secondaryImages = remember(toy) { toy.getSecondaryImages() }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.secondary_images_title, secondaryImages.size),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = sysTextColor()
                )
                IconButton(
                    onClick = { imagePicker() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_secondary_image_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (secondaryImages.isNotEmpty()) {
                LazyRow(
                    state = bitmapsScrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(vertical = GcSpacing.Small)
                        .scrollHorizontallyWithMouseWheel(bitmapsScrollState, coroutineScope),
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                ) {
                    items(secondaryImages) { img ->
                        val bitmapUri = remember(img.filename) { resolveBitmapUri(img.filename) }
                        if (bitmapUri != null) {
                            GcImage(
                                imageFile = bitmapUri,
                                files = allImagePaths,
                                contentDescription = img.filename,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                fullImageOnClick = true
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(img.filename, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DetailCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(GcSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        }
    }
}

@Composable
fun DetailField(label: String, value: String) {
    if (value.trim().isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 15.sp, color = sysTextColor())
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 4.dp))
    }
}
