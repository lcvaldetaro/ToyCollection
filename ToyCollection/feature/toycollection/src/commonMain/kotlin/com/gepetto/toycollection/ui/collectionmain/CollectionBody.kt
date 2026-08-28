package com.gepetto.toycollection.ui.collectionmain


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import com.gepetto.toycollection.intentprocessors.CollectionTapAction
import com.gepetto.toycollection.intentprocessors.CollectionIntent
import com.gepetto.toycollection.ui.common.AboutSheet
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.models.CollectionData
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.scaffold.GcFlipModalSheet
import club.gepetto.composeutils.scaffold.GcScaffold
import club.gepetto.composeutils.scaffold.gcModalBottomSheetState
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.common.Common
import com.gepetto.toycollection.ui.collection.main.CollectionDrawer
import com.gepetto.toycollection.ui.collection.main.MakersList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionBody(
    timeStamp: Long,
    aboutTitle: String,
    aboutString: String,
    title: String,
    collection: CollectionData,
    collectionImage: String,
    modifier: Modifier = Modifier,
    cached: Boolean = false,
    onIntentCommand: (CollectionIntent) -> Unit,
)  {
    val modalBottomSheetState = gcModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }



    GcScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = sysBackgroundColor(),
        modalBottomSheetState = modalBottomSheetState,
        topBar = {
            Row (Modifier.padding(bottom = 4.dp)) {
                CollectionTitle(title = if (cached) "$title (cached)" else title) {
                    if (it is CollectionIntent.Tapped && it.tapAction is CollectionTapAction.TapMenu) {
                        coroutineScope.launch {
                            showMenu = true
                            GcFlipModalSheet(modalBottomSheetState)
                        }
                    } else
                    if (it is CollectionIntent.Tapped && it.tapAction is CollectionTapAction.TapAbout) {
                        coroutineScope.launch {
                            showMenu = false
                            GcFlipModalSheet(modalBottomSheetState)
                        }
                    } else {
                        onIntentCommand(it)
                    }
                }
            }
        },
        content = {
            MakersList(
                modifier = Modifier.padding(paddingValues = it),
                makers = collection.makers,
                timeStamp = timeStamp,
                onIntentCommand = onIntentCommand
            )
        },
        sheetContent = {
            if (showMenu) {
                CollectionDrawer(
                    aboutTitle = aboutTitle,
                    onIntentCommand = {
                        showMenu = false
                        if (it is CollectionIntent.Tapped && it.tapAction != CollectionTapAction.TapAbout) {
                            coroutineScope.launch {
                                GcFlipModalSheet(modalBottomSheetState)
                                onIntentCommand(it)
                            }
                        }
                    },
                    collectionImage = collectionImage
                )
            }
            else {
                AboutSheet(
                    modifier = Modifier.background(sysBackgroundColor()),
                    about = aboutTitle,
                    aboutString = aboutString,
                    collectionImage = collectionImage,
                    onCloseClick = {
                        coroutineScope.launch {
                            GcFlipModalSheet(modalBottomSheetState)
                        }
                    }
                )
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    Common.testInit()
    MakerDataProvider.init()

    GcTheme {
        Surface {
            CollectionBody(
                collection = MakerDataProvider.collectionDataDb,
                title = "Collection Title",
                aboutTitle = "About Title",
                aboutString = "About String",
                collectionImage = "",
                timeStamp = 0L,
            ) {}
        }
    }
}


