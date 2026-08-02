package com.gepetto.toydb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcFilterButton
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import androidx.compose.ui.graphics.Color
import club.gepetto.composeutils.image.GcImage
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.resolveBitmapUri
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
fun MakerDetailScreen(
    repository: ToyRepository,
    makerName: String,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backStack: List<Destination> = emptyList()
) {
    var makerState by remember(makerName) { mutableStateOf(repository.getMaker(makerName)) }
    val maker = makerState
    var toysState by remember(makerName) { mutableStateOf(repository.getToysByMaker(makerName)) }
    val toysList = toysState

    LaunchedEffect(makerName, backStack.lastOrNull()) {
        makerState = repository.getMaker(makerName)
        toysState = repository.getToysByMaker(makerName)
    }
    val settingsList = remember { repository.getCategorySettings() }

    var selectedCategory by rememberSaveable(makerName) { mutableStateOf("all") }
    
    val filteredToys = remember(toysList, selectedCategory) {
        if (selectedCategory == "all") {
            toysList
        } else {
            toysList.filter { it.toyType == selectedCategory }
        }
    }

    val allLabel = stringResource(Res.string.all)
    val categories = remember(toysList, settingsList, allLabel) {
        val list = mutableListOf<CategoryFilter>()
        list.add(CategoryFilter("all", allLabel, toysList.size))
        settingsList.forEach { catSetting ->
            val count = toysList.count { it.toyType == catSetting.category }
            list.add(CategoryFilter(catSetting.category, catSetting.label, count))
        }
        list.filter { it.key == "all" || it.count > 0 }
    }

    val showFilters = remember(categories) {
        categories.count { it.key != "all" && it.count > 0 } > 1
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val bitmapsScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val makerImages = remember(maker) {
        maker?.bitmaps?.split(" ")?.filter { it.trim().isNotEmpty() } ?: emptyList()
    }

    val allImagePaths = remember(makerImages) {
        val list = mutableListOf<String>()
        makerImages.forEach { filename ->
            resolveBitmapUri(filename)?.let { list.add(it) }
        }
        list.toTypedArray()
    }

    val imagePicker = rememberImagePicker { selectedPath ->
        val currentMaker = makerState ?: return@rememberImagePicker
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
            
            val currentBitmaps = currentMaker.bitmaps.trim()
            val currentSizes = currentMaker.bitmapsSize.trim()
            val currentTimestamps = currentMaker.bitmapsTimeStamp.trim()
            
            val newBitmaps = if (currentBitmaps.isEmpty()) filename else "$currentBitmaps $filename"
            val newSizes = if (currentSizes.isEmpty()) size.toString() else "$currentSizes $size"
            val newTimestamps = if (currentTimestamps.isEmpty()) timestamp.toString() else "$currentTimestamps $timestamp"
            
            val updatedMaker = currentMaker.copy(
                bitmaps = newBitmaps,
                bitmapsSize = newSizes,
                bitmapsTimeStamp = newTimestamps
            )
            repository.saveMaker(updatedMaker)
            
            makerState = repository.getMaker(makerName)
            GcLog.d("MakerDetailScreen", "Successfully uploaded and saved manufacturer image $filename")
        } catch (e: Exception) {
            GcLog.e("MakerDetailScreen", "Error uploading manufacturer image: ${e.message}", e)
        }
    }

    if (maker == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.manufacturer_not_found), color = sysTextColor())
        }
        return
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(Res.string.delete_manufacturer_title)) },
            text = { Text(stringResource(Res.string.delete_manufacturer_confirm, maker.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteMaker(makerName)
                        showDeleteConfirmation = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.manufacturer_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Destination.EditMaker(makerName)) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 350.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = GcSpacing.Standard),
            horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard),
            verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(GcSpacing.Small))
            }

            // Manufacturer General Info Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(GcSpacing.Standard)) {
                        Text(
                            text = maker.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = sysTextColor()
                        )
                        Spacer(modifier = Modifier.height(GcSpacing.Small))
                        Text(
                            text = stringResource(Res.string.country_label, if (maker.country.isEmpty()) stringResource(Res.string.unknown) else maker.country),
                            fontWeight = FontWeight.SemiBold,
                            color = sysTextColor()
                        )
                        if (maker.comments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(GcSpacing.Small))
                            Text(
                                text = maker.comments,
                                fontSize = 14.sp,
                                color = sysTextColor()
                            )
                        }
                    }
                }
            }

            // Image Files / Bitmaps list Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.manufacturer_images_title, makerImages.size),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = sysTextColor()
                    )
                    IconButton(
                        onClick = { imagePicker() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.add_manufacturer_image_desc),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Image Files / Bitmaps list Row
            if (allImagePaths.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        state = bitmapsScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .scrollHorizontallyWithMouseWheel(bitmapsScrollState, coroutineScope),
                        horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                    ) {
                        lazyItems(makerImages) { filename ->
                            val bitmapUri = remember(filename) { resolveBitmapUri(filename) }
                            if (bitmapUri != null) {
                                GcImage(
                                    imageFile = bitmapUri,
                                    files = allImagePaths,
                                    contentDescription = filename,
                                    size = 100.dp,
                                    cornerSize = 16.dp,
                                    contentScale = ContentScale.Fit,
                                    fullImageOnClick = true
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(filename, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            // Associated Toys Header & Filter Buttons
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = stringResource(Res.string.associated_toys_title, filteredToys.size),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = sysTextColor(),
                        modifier = Modifier.padding(top = GcSpacing.Small, bottom = GcSpacing.Small)
                    )
                    
                    if (showFilters) {
                        val selectedLabel = categories.find { it.key == selectedCategory }?.let { "${it.label} (${it.count})" } ?: ""
                        val filtersScrollState = rememberLazyListState()
                        
                        LazyRow(
                            state = filtersScrollState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = GcSpacing.Standard)
                                .scrollHorizontallyWithMouseWheel(filtersScrollState, coroutineScope),
                            horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                        ) {
                            lazyItems(categories) { cat ->
                                val labelText = "${cat.label} (${cat.count})"
                                GcFilterButton(
                                    label = labelText,
                                    selection = selectedLabel
                                ) {
                                    selectedCategory = cat.key
                                }
                            }
                        }
                    }
                }
            }

            // Associated Toys items / empty state
            if (filteredToys.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(Res.string.no_toys_for_category), color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(filteredToys) { toy ->
                    val prefix = settingsList.find { it.category == toy.toyType }?.imagePrefix ?: "car"
                    ToyItemCard(toy, prefix) {
                        onNavigate(Destination.ToyDetail(toy.toyType, toy.refNum))
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

private data class CategoryFilter(val key: String, val label: String, val count: Int)
