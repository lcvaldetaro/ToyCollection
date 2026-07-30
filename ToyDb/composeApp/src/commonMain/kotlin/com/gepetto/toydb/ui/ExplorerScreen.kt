package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcFilterButton
import club.gepetto.composeutils.textAsBitmap
import androidx.compose.ui.graphics.toArgb
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor
import club.gepetto.composeutils.sysTextColor
import club.gepetto.composeutils.image.GcImage
import com.gepetto.toydb.database.Toy
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.resolveImageUri
import com.gepetto.toydb.utils.scrollHorizontallyWithMouseWheel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    repository: ToyRepository,
    category: String,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedScale by remember { mutableStateOf("") }
    val selectedCondition = ""
    var selectedMaker by remember { mutableStateOf("") }

    val makersScrollState = rememberLazyListState()
    val scalesScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val distinctScales = remember { repository.getDistinctScales(category) }
    val distinctMakers = remember { repository.getDistinctMakers(category) }
    
    val prefix = remember {
        repository.getCategorySettings().find { it.category == category }?.imagePrefix ?: "car"
    }

    val toysList = repository.getToys(
        toyType = category,
        searchQuery = searchQuery,
        scaleFilter = selectedScale,
        conditionFilter = selectedCondition,
        makerFilter = selectedMaker
    )

    val categoryLabel = when (category) {
        "slot" -> "Slot Cars"
        "train" -> "Model Trains"
        "static" -> "Static Models"
        "kit" -> "Model Kits"
        "misc" -> "Others"
        else -> category.replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        containerColor = sysBackgroundColor(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Destination.AddToy(category)) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Toy")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = GcSpacing.Standard)
        ) {
            Text(
                text = "$categoryLabel Explorer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = sysTextColor(),
                modifier = Modifier.padding(vertical = GcSpacing.Small)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small),
                placeholder = { Text("Search by description, maker, or refNum...") },
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

            // Filter Chips Rows
            if (distinctMakers.isNotEmpty()) {
                Text("Maker Filter:", fontSize = 12.sp, color = sysTextColor(), modifier = Modifier.padding(top = 4.dp))
                LazyRow(
                    state = makersScrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .scrollHorizontallyWithMouseWheel(makersScrollState, coroutineScope),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        GcFilterButton(label = "All Makers", selection = if (selectedMaker.isEmpty()) "All Makers" else "") {
                            selectedMaker = ""
                        }
                    }
                    lazyItems(distinctMakers) { maker ->
                        GcFilterButton(label = maker, selection = selectedMaker) {
                            selectedMaker = if (selectedMaker == maker) "" else maker
                        }
                    }
                }
            }

            if (distinctScales.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Scale Filter:", fontSize = 12.sp, color = sysTextColor())
                    LazyRow(
                        state = scalesScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .scrollHorizontallyWithMouseWheel(scalesScrollState, coroutineScope),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            GcFilterButton(label = "All Scales", selection = if (selectedScale.isEmpty()) "All Scales" else "") {
                                selectedScale = ""
                            }
                        }
                        lazyItems(distinctScales) { scale ->
                            GcFilterButton(label = scale, selection = selectedScale) {
                                selectedScale = if (selectedScale == scale) "" else scale
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(GcSpacing.Small))

            // Toys List
            if (toysList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No toys found matching filters.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Row(modifier = Modifier.weight(1f)) {
                    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 350.dp),
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard),
                        verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                    ) {
                        items(toysList) { toy ->
                            ToyItemCard(toy, prefix) {
                                onNavigate(Destination.ToyDetail(category, toy.refNum))
                            }
                        }
                    }
                    PlatformGridScrollbar(
                        state = gridState,
                        modifier = Modifier.fillMaxHeight().width(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToyItemCard(
    toy: Toy,
    prefix: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor = sysTextColor()
    val imgUri = remember(toy.refNum) { resolveImageUri(prefix, toy.refNum) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (toy.traded.isNotBlank()) 0.5f else 1f)
            .background(sysBackgroundColor())
    ) {
        HorizontalDivider()

        Row(
            modifier = Modifier
                .clickable { onClick() }
                .fillMaxWidth()
        ) {
            if (imgUri == null) {
                val textBitmap = remember(toy.refNum, textColor) {
                    textAsBitmap(text = toy.refNum.toString(), textColor = textColor.toArgb())
                }
                GcImage(
                    modifier = Modifier.align(Alignment.Top),
                    imageBitmap = textBitmap,
                    contentDescription = toy.description,
                    fullImageOnClick = false,
                    size = 48.dp,
                    cornerSize = 16.dp,
                    paddingSize = 4.dp,
                    onClick = onClick
                )
            } else {
                GcImage(
                    modifier = Modifier.align(Alignment.Top),
                    imageFile = imgUri,
                    contentDescription = toy.description,
                    fullImageOnClick = false,
                    size = 48.dp,
                    cornerSize = 16.dp,
                    paddingSize = 4.dp,
                    onClick = onClick
                )
            }

            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = toy.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor,
                )
                Text(
                    text = "${toy.scale} (${toy.refNum})",
                    fontSize = 12.sp,
                    color = textColor,
                )
                if (toy.traded.isNotEmpty()) {
                    Text(
                        text = "* gone *",
                        fontSize = 12.sp,
                        color = textColor,
                    )
                }
            }
        }
    }
}
