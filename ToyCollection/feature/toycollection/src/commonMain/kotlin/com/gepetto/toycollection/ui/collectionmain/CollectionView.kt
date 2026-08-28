package com.gepetto.toycollection.ui.collection.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import club.gepetto.circum.CircumEffect
import com.gepetto.toycollection.intentprocessors.CollectionIntent
import com.gepetto.toycollection.intentprocessors.CollectionIntentProcessor
import com.gepetto.toycollection.intentprocessors.CollectionState
import com.gepetto.toycollection.models.ToyCollection
import com.gepetto.toycollection.ui.collectionmain.CollectionContent

@Composable
fun CollectionView(
    collectionChoice: ToyCollection,
    collectionIp: CollectionIntentProcessor,
    modifier: Modifier = Modifier,
    onEffect: (CircumEffect) -> Unit = {}
) {
    val state by collectionIp.collectState(initialState = CollectionState.Initial)

    collectionIp.onEffectIssued { effect -> onEffect(effect) }

    if (collectionChoice != collectionIp.getCurrentCollection())
        collectionIp.sendIntentCommand(CollectionIntent.LoadCollection(collectionChoice))

    CollectionContent(
        modifier = modifier,
        state = state,
        onIntentCommand = { collectionIp.sendIntentCommand(it) }
    )
}