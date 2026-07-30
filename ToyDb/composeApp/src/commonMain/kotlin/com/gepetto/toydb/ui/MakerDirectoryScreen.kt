package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import club.gepetto.composeutils.GcCard
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val numOfColumns = (this.maxWidth / 180).value.toInt()
                    if (numOfColumns > 0) {
                        val columns = GridCells.Fixed(numOfColumns)
                        val localDensity = LocalDensity.current
                        var columnHeight by remember { mutableStateOf(0) }
                        var columnHeightDp by remember { mutableStateOf(0.dp) }

                        LazyVerticalGrid(
                            columns = columns,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            items(filteredMakers.size) { index ->
                                val maker = filteredMakers[index]
                                val toyCount = remember(maker.name) { repository.getToysByMaker(maker.name).size }
                                val localModifier =
                                    if (columnHeight > 0) Modifier.height(columnHeightDp) else Modifier

                                MakerItemCard(
                                    maker = maker,
                                    toyCount = toyCount,
                                    modifier = localModifier
                                        .onGloballyPositioned {
                                            if (it.size.height > columnHeight) {
                                                columnHeight = it.size.height
                                                columnHeightDp = with(localDensity) { columnHeight.toDp() }
                                            }
                                        }
                                ) {
                                    onNavigate(Destination.MakerDetail(maker.name))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MakerItemCard(
    maker: Maker,
    toyCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor = sysTextColor()
    val makerImages = remember(maker) {
        maker.bitmaps.split(" ").filter { it.trim().isNotEmpty() }
    }
    val firstImage = makerImages.firstOrNull()
    val bitmapUri = remember(firstImage) { firstImage?.let { resolveBitmapUri(it) } }

    GcCard(
        modifier = modifier.clickable { onClick() },
    ) {
        Row {
            if (bitmapUri == null) {
                val textBitmap = remember(maker.name, textColor) {
                    textAsBitmap(text = maker.name, textColor = textColor.toArgb())
                }
                GcImage(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    imageBitmap = textBitmap,
                    contentDescription = maker.name,
                    fullImageOnClick = false,
                    size = 48.dp,
                    cornerSize = 16.dp,
                    paddingSize = 4.dp,
                    onClick = onClick
                )
            } else {
                GcImage(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    imageFile = bitmapUri,
                    contentDescription = maker.name,
                    fullImageOnClick = false,
                    size = 48.dp,
                    cornerSize = 16.dp,
                    paddingSize = 4.dp,
                    onClick = onClick
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = maker.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor,
                )
                Text(
                    text = maker.country,
                    fontSize = 12.sp,
                    color = textColor,
                )
                Text(
                    text = "$toyCount models",
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }
    }
}
