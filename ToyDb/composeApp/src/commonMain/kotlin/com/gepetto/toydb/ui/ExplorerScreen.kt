package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcFilterButton
import club.gepetto.composeutils.textAsBitmap
import androidx.compose.ui.graphics.toArgb
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysBackgroundColor
import androidx.compose.ui.graphics.Color
import club.gepetto.composeutils.sysTextColor
import club.gepetto.composeutils.image.GcImage
import com.gepetto.toydb.database.Toy
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.resolveImageUri
import com.gepetto.toydb.utils.scrollHorizontallyWithMouseWheel
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    repository: ToyRepository,
    category: String,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable(category) { mutableStateOf("") }
    var selectedScale by rememberSaveable(category) { mutableStateOf("") }
    val selectedCondition = ""
    var selectedMaker by rememberSaveable(category) { mutableStateOf("") }

    val makersScrollState = rememberLazyListState()
    val scalesScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val distinctScales = remember { repository.getDistinctScales(category) }
    val distinctMakers = remember { repository.getDistinctMakers(category) }
    
    val categorySetting = remember(category) {
        repository.getCategorySettings().find { it.category == category }
    }
    val prefix = remember(categorySetting) {
        categorySetting?.imagePrefix ?: "car"
    }

    val toysList = repository.getToys(
        toyType = category,
        searchQuery = searchQuery,
        scaleFilter = selectedScale,
        conditionFilter = selectedCondition,
        makerFilter = selectedMaker
    )

    val categoryLabel = remember(categorySetting) {
        categorySetting?.label ?: category.replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Destination.AddToy(category)) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_toy_desc))
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
                text = stringResource(Res.string.explorer_title, categoryLabel),
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
                placeholder = { Text(stringResource(Res.string.search_placeholder_toy)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.search)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.clear))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = sysBackgroundColor(),
                    unfocusedContainerColor = sysBackgroundColor()
                )
            )

            // Filter Chips Rows
            if (distinctMakers.isNotEmpty()) {
                Text(stringResource(Res.string.maker_filter), fontSize = 12.sp, color = sysTextColor(), modifier = Modifier.padding(top = 4.dp))
                LazyRow(
                    state = makersScrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .scrollHorizontallyWithMouseWheel(makersScrollState, coroutineScope),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        GcFilterButton(label = stringResource(Res.string.all_makers), selection = if (selectedMaker.isEmpty()) stringResource(Res.string.all_makers) else "") {
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
                    Text(stringResource(Res.string.scale_filter), fontSize = 12.sp, color = sysTextColor())
                    LazyRow(
                        state = scalesScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .scrollHorizontallyWithMouseWheel(scalesScrollState, coroutineScope),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                             GcFilterButton(label = stringResource(Res.string.all_scales), selection = if (selectedScale.isEmpty()) stringResource(Res.string.all_scales) else "") {
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
                    Text(stringResource(Res.string.no_toys_matching), color = MaterialTheme.colorScheme.outline)
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (toy.traded.isNotBlank()) 0.5f else 1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .fillMaxWidth()
                .padding(8.dp)
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
                    text = stringResource(Res.string.toy_scale_ref_format, toy.scale, toy.refNum.toString()),
                    fontSize = 12.sp,
                    color = textColor,
                )
                if (toy.traded.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.traded_gone_label),
                        fontSize = 12.sp,
                        color = textColor,
                    )
                }
            }
        }
    }
}
