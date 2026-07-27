package com.gepetto.toycollection.ui.home


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

import club.gepetto.composeutils.GcBanner
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*
import com.gepetto.toycollection.intentprocessors.HomeIntent
import com.gepetto.toycollection.intentprocessors.HomeTapAction
import com.gepetto.toycollection.ui.common.AboutSheet
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.dataproviders.defaultProcessorCollectionList
import com.gepetto.toycollection.models.CollectionList
import com.gepetto.toycollection.ui.common.images.LargeImage
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.isDark
import club.gepetto.composeutils.isSystemAtablet
import club.gepetto.composeutils.isSystemInLandscape
import club.gepetto.composeutils.scaffold.GcFlipModalSheet
import club.gepetto.composeutils.scaffold.GcScaffold
import club.gepetto.composeutils.scaffold.gcModalBottomSheetState
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.common.BANNER_CONTENT
import com.gepetto.common.Common
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    collectionList: CollectionList,
    modifier: Modifier = Modifier,
    timeStamp: Long = 0L,
    onIntentCommand: (HomeIntent) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = gcModalBottomSheetState()
    val rememberedTimeStamp = remember { mutableStateOf(0L) }
    val nullCl: CollectionList? = null
    val collectionWithTotals = remember { mutableStateOf(nullCl) }
    var showMenu by remember { mutableStateOf(false) }

    if (rememberedTimeStamp.value != timeStamp)
        rememberedTimeStamp.value = timeStamp

    Box (modifier.fillMaxSize().background(sysBackgroundColor())) {
        val imageResource = if (isDark()) Res.drawable.invertedgepetto else Res.drawable.gepetto
        LargeImage(
            imageResourceRes = imageResource,
            fullImageOnClick = false,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (isSystemInLandscape()) ContentScale.FillHeight else ContentScale.Crop,
            paddingSize = 0.dp,
            cornerSize = 0.dp,
        )

        val fontSize = if (isSystemAtablet()) 16.sp else 12.sp
        val startTextPadding = if (isSystemInLandscape() && isSystemAtablet()) 48.dp else 24.dp
        val endtextPadding = if (isSystemInLandscape() && isSystemAtablet()) 24.dp else if (isSystemInLandscape()) 12.dp else 24.dp

        GcBanner(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = if (isSystemInLandscape()) 96.dp else 0.dp,
                    bottom = if (isSystemInLandscape()) 0.dp else 48.dp
                ),
            text = BANNER_CONTENT,
            fontSize = fontSize,
            imageInnerVerticalPadding = 48.dp,
            startTextPadding = startTextPadding,
            endTextPadding = endtextPadding,
        )

        GcScaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            /*topBar = {
                HomeTitle(title = collectionList.title) {
                    if (it is HomeIntent.Tapped && it.tapAction is HomeTapAction.TapMenu) {
                        coroutineScope.launch {
                            showMenu = true
                            GcFlipModalSheet(modalBottomSheetState)
                        }
                    }
                    else
                    if (it is HomeIntent.Tapped && it.tapAction is HomeTapAction.TapTitle)
                        /*coroutineScope.launch {
                            collectionWithTotals.value = collectionList;
                            showMenu = false
                            GcFlipModalSheet(modalBottomSheetState)
                        }*/
                    else
                        onIntentCommand(it)
                }
            },*/
            modalBottomSheetState = modalBottomSheetState,
            sheetContent = {
                if (showMenu) {
                    HomeDrawer(
                        onIntentCommand = {
                            coroutineScope.launch {
                                showMenu = false
                                GcFlipModalSheet(modalBottomSheetState)
                                onIntentCommand(it)
                            }
                        },
                        onAboutClicked = {
                            coroutineScope.launch {
                                collectionWithTotals.value = collectionList
                                showMenu = false
                            }
                        }
                    )
                }
                else {
                    AboutSheet(
                        modifier = Modifier.background(sysBackgroundColor()),
                        showVersion = true,
                        collectionList = collectionWithTotals.value,
                        onCloseClick = {
                            coroutineScope.launch {
                                GcFlipModalSheet(modalBottomSheetState)
                            }
                        }
                    )
                }
            },
            content = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    Common.testInit()
    MakerDataProvider.init()

    GcTheme {
        Surface {
            HomeContent(collectionList = defaultProcessorCollectionList) {}
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun PreviewLandscape() {
    Common.testInit()
    MakerDataProvider.init()

    GcTheme {
        Surface {
            HomeContent(collectionList = defaultProcessorCollectionList) {}
        }
    }
}


