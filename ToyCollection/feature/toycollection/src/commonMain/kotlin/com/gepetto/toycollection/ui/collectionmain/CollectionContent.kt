package com.gepetto.toycollection.ui.collectionmain

import club.gepetto.composeutils.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gepetto.toycollection.intentprocessors.CollectionTapAction
import com.gepetto.toycollection.intentprocessors.CollectionIntent
import com.gepetto.toycollection.intentprocessors.CollectionState
import com.gepetto.toycollection.ui.collection.main.Error
import com.gepetto.toycollection.ui.collection.main.Loading

@Composable
fun CollectionContent(
    state: Any,
    modifier: Modifier = Modifier,
    onIntentCommand: (CollectionIntent) -> Unit
) {
    BackHandler(true) { onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapBack)) }

    when (state) {
        is CollectionState.Error -> {
            Error(modifier) { onIntentCommand(CollectionIntent.GoHome)}
        }
        is CollectionState.Loading -> {
            Loading(onIntentCommand = onIntentCommand)
        }
        is CollectionState.Loaded -> {
            CollectionBody(
                modifier = modifier,
                timeStamp = state.timeStamp,
                collection = state.collection,
                title = state.title,
                aboutTitle = state.about,
                aboutString = state.aboutString,
                collectionImage = state.toyCollection.image,
                onIntentCommand = onIntentCommand,
                cached = state.fromCache,
            )
        }
        else -> Unit
    }
}
