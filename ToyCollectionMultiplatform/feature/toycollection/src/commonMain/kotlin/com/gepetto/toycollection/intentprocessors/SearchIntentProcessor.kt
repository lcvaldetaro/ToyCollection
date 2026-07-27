package com.gepetto.toycollection.intentprocessors

import club.gepetto.circum.CircumEffect
import club.gepetto.circum.CircumIntentProcessor

class SearchIntentProcessor  : CircumIntentProcessor<ListDetailsState, ListDetailsIntent, CircumEffect>() {
    override fun onIntentCommand(intent: ListDetailsIntent, state: ListDetailsState?) {
        when (intent) {
            is ListDetailsIntent.SaveListDetailsState -> {
                if (state is ListDetailsState.ToySearch)
                    setState(state.copy(toy = intent.toy, listDetailsState = intent.listDetailsState))
            }
            is ListDetailsIntent.Tapped -> {
                when (intent.tapAction) {
                    is ListDetailsTapAction.UpdateSearchString -> {
                        if (state is ListDetailsState.ToySearch)
                            setState(state.copy( searchString = intent.tapAction.searchString))
                    }
                    is ListDetailsTapAction.TapToy -> sendEffect { GoToViewToyEffect(intent.tapAction.toy) }
                    else -> Unit
                }
            }
            else -> Unit
        }
    }
}