package com.gepetto.toycollection.ui.search

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import club.gepetto.circum.CircumEffect
import club.gepetto.circum.circumIntentProcessor
import com.gepetto.toycollection.intentprocessors.CollectionIntentProcessor
import com.gepetto.toycollection.intentprocessors.GoBackEffect
import com.gepetto.toycollection.intentprocessors.ListDetailsState
import com.gepetto.toycollection.intentprocessors.SearchIntentProcessor

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SearchView(
    collectionIp: CollectionIntentProcessor,
    modifier: Modifier = Modifier,
    iproc: SearchIntentProcessor = circumIntentProcessor<SearchIntentProcessor>(),
    onEffect: (CircumEffect) -> Unit = {},
) {
    val state by iproc.collectState(initialState = ListDetailsState.ToySearch())

    iproc.onEffectIssued { effect -> onEffect(effect) }

    when (state) {
        is ListDetailsState.ToySearch -> {
            val searchState = state as ListDetailsState.ToySearch

            SearchContent(
                searchString = searchState.searchString,
                collection = collectionIp.getCurrentCollectionData()
                    ?: collectionIp.getActiveCollectionData()!!,
                currentSelectedToy = searchState.toy,
                modifier = modifier,
                goBack = { onEffect(GoBackEffect) },
                onIntentCommand = { intent -> iproc.onIntentCommand(intent, state) }
            )
        }
        else -> Unit
    }
}