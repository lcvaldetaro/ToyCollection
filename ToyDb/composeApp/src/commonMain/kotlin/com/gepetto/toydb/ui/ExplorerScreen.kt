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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.composeutils.GcTheme
import toydb.composeapp.generated.resources.*

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

    ExplorerContent(
        category = category,
        categoryLabel = categoryLabel,
        prefix = prefix,
        distinctScales = distinctScales,
        distinctMakers = distinctMakers,
        toysList = toysList,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedScale = selectedScale,
        onScaleChange = { selectedScale = it },
        selectedMaker = selectedMaker,
        onMakerChange = { selectedMaker = it },
        onNavigate = onNavigate,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerContent(
    category: String,
    categoryLabel: String,
    prefix: String,
    distinctScales: List<String>,
    distinctMakers: List<String>,
    toysList: List<Toy>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedScale: String,
    onScaleChange: (String) -> Unit,
    selectedMaker: String,
    onMakerChange: (String) -> Unit,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val makersScrollState = rememberLazyListState()
    val scalesScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small),
                placeholder = { Text(stringResource(Res.string.search_placeholder_toy)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.search)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
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
                            onMakerChange("")
                        }
                    }
                    lazyItems(distinctMakers) { maker ->
                        GcFilterButton(label = maker, selection = selectedMaker) {
                            onMakerChange(if (selectedMaker == maker) "" else maker)
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
                                onScaleChange("")
                            }
                        }
                        lazyItems(distinctScales) { scale ->
                            GcFilterButton(label = scale, selection = selectedScale) {
                                onScaleChange(if (selectedScale == scale) "" else scale)
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
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val columnsCount = (maxWidth / 160.dp).toInt().coerceIn(2, 5)
                    val rows = remember(toysList, columnsCount) { toysList.chunked(columnsCount) }
                    val listState = rememberLazyListState()
                    Row(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                        ) {
                            items(rows) { rowToys ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                                ) {
                                    for (i in 0 until columnsCount) {
                                        val toy = rowToys.getOrNull(i)
                                        if (toy != null) {
                                            ToyItemCard(
                                                toy = toy,
                                                prefix = prefix,
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            ) {
                                                onNavigate(Destination.ToyDetail(category, toy.refNum))
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                        PlatformScrollbar(
                            state = listState,
                            modifier = Modifier.fillMaxHeight().width(8.dp)
                        )
                    }
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

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun ExplorerContentPreview() {
    GcTheme {
        val mockToys = listOf(
            Toy(refNum = 1, toyType = "slots", description = "Porsche 917", scale = "1:32", makerCombo = "Scalextric"),
            Toy(refNum = 2, toyType = "slots", description = "Ferrari 512", scale = "1:32", makerCombo = "Fly", traded = "Traded")
        )
        ExplorerContent(
            category = "slots",
            categoryLabel = "Slot Cars",
            prefix = "slots_",
            distinctScales = listOf("1:32", "1:24"),
            distinctMakers = listOf("Scalextric", "Fly", "Carrera"),
            toysList = mockToys,
            searchQuery = "",
            onSearchQueryChange = {},
            selectedScale = "1:32",
            onScaleChange = {},
            selectedMaker = "Scalextric",
            onMakerChange = {},
            onNavigate = {}
        )
    }
}
