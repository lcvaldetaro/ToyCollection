package com.gepetto.toydb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import club.gepetto.composeutils.image.GcImage
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.resolveBitmapUri
import com.gepetto.toydb.utils.scrollHorizontallyWithMouseWheel
import com.gepetto.toydb.utils.selectFileDialog
import androidx.compose.material.icons.filled.Add
import okio.FileSystem
import okio.Path.Companion.toPath
import club.gepetto.GcLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakerDetailScreen(
    repository: ToyRepository,
    makerName: String,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var makerState by remember(makerName) { mutableStateOf(repository.getMaker(makerName)) }
    val maker = makerState
    val toysList = remember { repository.getToysByMaker(makerName) }
    val settingsList = remember { repository.getCategorySettings() }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val bitmapsScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    if (maker == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Manufacturer not found.", color = sysTextColor())
        }
        return
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Manufacturer") },
            text = { Text("Are you sure you want to delete manufacturer '${maker.name}'?") },
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
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = sysBackgroundColor(),
        topBar = {
            TopAppBar(
                title = { Text("Manufacturer Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Destination.EditMaker(makerName)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(GcSpacing.Standard)
        ) {
            // Manufacturer General Info Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = GcSpacing.Standard),
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
                        text = "Country: ${if (maker.country.isEmpty()) "Unknown" else maker.country}",
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

            // Image Files / Bitmaps list
            // Image Files / Bitmaps list
            val makerImages = remember(maker) {
                maker.bitmaps.split(" ").filter { it.trim().isNotEmpty() }
            }

            val allImagePaths = remember(makerImages) {
                val list = mutableListOf<String>()
                makerImages.forEach { filename ->
                    resolveBitmapUri(filename)?.let { list.add(it) }
                }
                list.toTypedArray()
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = GcSpacing.Small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manufacturer Images (${makerImages.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = sysTextColor()
                )
                IconButton(
                    onClick = {
                        val selectedPath = selectFileDialog("Select Image to Upload", listOf("jpg", "jpeg", "png", "gif", "webp", "JPG", "JPEG", "PNG", "GIF"))
                        if (selectedPath != null) {
                            val srcPath = selectedPath.toPath()
                            val filename = srcPath.name
                            
                            // Resolve images directory
                            val customPath = repository.getImagesPathSetting()
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
                                
                                // Get metadata
                                val size = FileSystem.SYSTEM.metadataOrNull(destPath)?.size ?: 0L
                                val timestamp = System.currentTimeMillis()
                                
                                // Update maker fields
                                val currentBitmaps = maker.bitmaps.trim()
                                val currentSizes = maker.bitmapsSize.trim()
                                val currentTimestamps = maker.bitmapsTimeStamp.trim()
                                
                                val newBitmaps = if (currentBitmaps.isEmpty()) filename else "$currentBitmaps $filename"
                                val newSizes = if (currentSizes.isEmpty()) size.toString() else "$currentSizes $size"
                                val newTimestamps = if (currentTimestamps.isEmpty()) timestamp.toString() else "$currentTimestamps $timestamp"
                                
                                val updatedMaker = maker.copy(
                                    bitmaps = newBitmaps,
                                    bitmapsSize = newSizes,
                                    bitmapsTimeStamp = newTimestamps
                                )
                                repository.saveMaker(updatedMaker)
                                
                                // Refresh state
                                makerState = repository.getMaker(makerName)
                                GcLog.d("MakerDetailScreen", "Successfully uploaded and saved manufacturer image $filename")
                            } catch (e: Exception) {
                                GcLog.e("MakerDetailScreen", "Failed to copy and save manufacturer image: ${e.message}", e)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Manufacturer Image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (allImagePaths.isNotEmpty()) {
                LazyRow(
                    state = bitmapsScrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(vertical = GcSpacing.Small)
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
                                modifier = Modifier.size(100.dp),
                                contentScale = ContentScale.Crop,
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
                
                Spacer(modifier = Modifier.height(GcSpacing.Standard))
            }

            // Associated Toys
            Text(
                text = "Associated Toys (${toysList.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = sysTextColor(),
                modifier = Modifier.padding(bottom = GcSpacing.Small)
            )

            if (toysList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No toys found for this manufacturer.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 350.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard),
                    verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                ) {
                    items(toysList) { toy ->
                        val prefix = settingsList.find { it.category == toy.toyType }?.imagePrefix ?: "car"
                        ToyItemCard(toy, prefix) {
                            onNavigate(Destination.ToyDetail(toy.toyType, toy.refNum))
                        }
                    }
                }
            }
        }
    }
}
