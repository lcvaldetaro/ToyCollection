package com.gepetto.toycollection.ui.search

import club.gepetto.composeutils.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.scaffold.GcScaffold
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent.*
import com.gepetto.toycollection.intentprocessors.ListDetailsState
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.models.Toy
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SearchContent(
    collection: CollectionData,
    modifier: Modifier = Modifier,
    searchString: String = "",
    index: Int = 0,
    offset: Int = 0,
    currentSelectedToy: Toy? = null,
    timeStamp: Long = 0L,
    goBack: () -> Unit = {},
    onIntentCommand: (ListDetailsIntent) -> Unit,
) {
    val background = TopAppBarDefaults.topAppBarColors().containerColor
    val tint = sysTextColor()
    var selectedToy: Toy? by remember { mutableStateOf(currentSelectedToy) }
    var refresh: Long by remember { mutableStateOf(0L)}

    BackHandler(true) { goBack() }

    GcScaffold(
        modifier = modifier.background(sysBackgroundColor()),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = background),
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.backarrow),
                            contentDescription = "Back",
                            tint = tint,
                            modifier = Modifier.size(24.dp).clickable { goBack() }.weight(1f),
                        )
                        Text(
                            text = "Search for toy",
                            textAlign = TextAlign.Center,
                            color = tint,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(12f).padding(end = 8.dp)
                        )
                    }
                },
            )
        },
        sheetContent = {},
        content = { paddingValues ->
            SearchScreenBody(
                modifier = Modifier.padding(paddingValues),
                collection = collection,
                searchString = searchString,
                multipleColumns = true,
                timeStamp = timeStamp + refresh,
                onIntentCommand = {
                    when (it) {
                        is Tapped if it.tapAction is ListDetailsTapAction.TapToy -> {
                            val toy = it.tapAction.toy
                            selectedToy = toy
                            onIntentCommand(SaveListDetailsState(toy, ListDetailsState.ShowToy))
                            onIntentCommand(Tapped(ListDetailsTapAction.TapToy(selectedToy!!)))
                        }

                        is Tapped if it.tapAction is ListDetailsTapAction.TapBack -> goBack()

                        else -> onIntentCommand(it)
                    }
                },
                index = index,
                offset = offset,
            )
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    com.gepetto.common.Common.testInit()
    club.gepetto.composeutils.GcTheme {
        androidx.compose.material3.Surface {
            SearchContent(
                collection = com.gepetto.toycollection.dataproviders.MakerDataProvider.collectionDataDb,
                searchString = "1966"
            ) {}
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun PreviewLandscape() {
    com.gepetto.common.Common.testInit()
    club.gepetto.composeutils.GcTheme {
        androidx.compose.material3.Surface {
            SearchContent(
                collection = com.gepetto.toycollection.dataproviders.MakerDataProvider.collectionDataDb,
                searchString = "19"
            ) {}
        }
    }
}
