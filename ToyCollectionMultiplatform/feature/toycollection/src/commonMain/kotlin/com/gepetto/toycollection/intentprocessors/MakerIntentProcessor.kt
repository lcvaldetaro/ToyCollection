package com.gepetto.toycollection.intentprocessors

import club.gepetto.circum.CircumEffect
import club.gepetto.circum.CircumIntentProcessor

class MakerIntentProcessor  : CircumIntentProcessor<ListDetailsState, ListDetailsIntent, CircumEffect>() {

    override fun onIntentCommand(intent: ListDetailsIntent, state: ListDetailsState?) {
        when (intent) {
            is ListDetailsIntent.SaveListDetailsState -> {
                if (state is ListDetailsState.Loaded)
                    setState(state.copy(toy = intent.toy, listDetailsState = intent.listDetailsState))
            }
            is ListDetailsIntent.Tapped -> {
                when (intent.tapAction) {
                    is ListDetailsTapAction.TapToy -> sendEffect { GoToViewToyEffect(intent.tapAction.toy) }
                    is ListDetailsTapAction.TapAddToy -> {
                        if (state is ListDetailsState.Loaded && state.maker != null)
                            sendEffect { GoToAddToyEffect(maker = state.maker) }
                    }
                    else -> Unit
                }
            }
            else -> Unit
        }
    }
}
