package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.toArgb
import club.gepetto.composeutils.textAsBitmap
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Business
import androidx.compose.ui.layout.ContentScale
import com.gepetto.toydb.utils.resolveBitmapUri
import club.gepetto.composeutils.image.GcImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.Maker
import com.gepetto.toydb.database.ToyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakerDirectoryScreen(
    repository: ToyRepository,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    // Fetch manufacturers dynamically to reflect additions/deletions immediately
    val makersList = repository.getMakers()

    val filteredMakers = makersList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.country.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = sysBackgroundColor(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Destination.AddMaker) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Manufacturer")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(GcSpacing.Standard)
        ) {
            Text(
                text = "Makers Directory",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = sysTextColor(),
                modifier = Modifier.padding(bottom = GcSpacing.Small)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = GcSpacing.Standard),
                placeholder = { Text("Search manufacturers...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            if (filteredMakers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No manufacturers found.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard),
                    verticalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                ) {
                    items(filteredMakers) { maker ->
                        val toyCount = remember(maker.name) { repository.getToysByMaker(maker.name).size }
                        MakerItemCard(maker, toyCount) {
                            onNavigate(Destination.MakerDetail(maker.name))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MakerItemCard(maker: Maker, toyCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GcSpacing.Standard, vertical = GcSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
        ) {
            val makerImages = remember(maker) {
                maker.bitmaps.split(" ").filter { it.trim().isNotEmpty() }
            }
            val firstImage = makerImages.firstOrNull()
            val bitmapUri = remember(firstImage) { firstImage?.let { resolveBitmapUri(it) } }

            // Round Avatar Container
            Card(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmapUri != null) {
                        GcImage(
                            imageFile = bitmapUri,
                            files = arrayOf(bitmapUri),
                            contentDescription = maker.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fullImageOnClick = false
                        )
                    } else {
                        val textColor = sysTextColor()
                        val textBitmap = remember(maker.name, textColor) {
                            textAsBitmap(text = maker.name, textColor = textColor.toArgb())
                        }
                        GcImage(
                            imageBitmap = textBitmap,
                            contentDescription = maker.name,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Fit,
                            fullImageOnClick = false
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = maker.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = sysTextColor(),
                    maxLines = 1
                )
                Text(
                    text = if (maker.country.isEmpty()) "Unknown" else maker.country,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "$toyCount toys",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
