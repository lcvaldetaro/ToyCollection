package com.gepetto.toycollection.intentprocessors

import club.gepetto.circum.CircumEffect
import club.gepetto.circum.CircumIntentProcessor
import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.models.Toy

class ToyIntentProcessor  : CircumIntentProcessor<ToyState, ToyIntent, CircumEffect>() {
    private var toy: Toy? = null
    private var toyState : ToyState = ToyState.PlaceHolder

    fun getCurrentToy() = toy
    fun getCurrentToyState() = toyState

    override fun onIntentCommand(intent: ToyIntent, state: ToyState?) {
        when (intent) {
            is ToyIntent.SaveToyState -> {
                toy = intent.toy
                toyState = intent.ToyState
            }
            is ToyIntent.Tapped -> Unit
            is ToyIntent.ToyAdded -> Unit
            is ToyIntent.ToyEdited -> Unit
        }
    }
}

sealed interface ToyState {
    data class Loaded(val toy : Toy? = null, val ToyState: ToyState? = null) : ToyState
    data object PlaceHolder : ToyState
    data object ShowToy : ToyState
    data object EditToy : ToyState
    data object AddToy: ToyState
    data object WebPage : ToyState
    data object WebSearch : ToyState
    data class ToySearch(val searchString: String = "", val toy: Toy? = null, val ToyState: ToyState? = null) : ToyState
}

sealed interface ToyIntent {
    data class Tapped(val tapAction: ToyTapAction, ): ToyIntent
    data class ToyEdited(val toy: Toy, val maker: Maker) : ToyIntent
    data class ToyAdded(val toy: Toy, val maker: Maker) : ToyIntent
    data class SaveToyState(val toy: Toy? = null, val ToyState: ToyState) : ToyIntent
}

sealed interface ToyTapAction {
    data object TapSave : ToyTapAction
    data class TapWebPage(val toy: Toy? = null): ToyTapAction
    data class TapWebSearch(val toy: Toy? = null): ToyTapAction
    data class TapEditToy(val maker: Maker, val toy: Toy): ToyTapAction
    data object TapBack : ToyTapAction
}
