package com.gepetto.toycollection.intentprocessors

import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.models.Toy

sealed interface ListDetailsState {
    data class Loaded(val toy : Toy? = null, val maker: Maker? = null, val listDetailsState: ListDetailsState? = null) : ListDetailsState
    data object PlaceHolder : ListDetailsState
    data object ShowToy : ListDetailsState
    data object EditToy : ListDetailsState
    data object AddToy: ListDetailsState
    data object WebPage : ListDetailsState
    data object WebSearch : ListDetailsState
    data class ToySearch(val searchString: String = "", val toy: Toy? = null, val listDetailsState: ListDetailsState? = null) : ListDetailsState
}

sealed interface ListDetailsIntent {
    data class Tapped(val tapAction: ListDetailsTapAction, ): ListDetailsIntent
    data class ToyEdited(val toy: Toy, val maker: Maker) : ListDetailsIntent
    data class ToyAdded(val toy: Toy, val maker: Maker) : ListDetailsIntent
    data class SaveListDetailsState(val toy: Toy? = null, val listDetailsState: ListDetailsState) : ListDetailsIntent
}

sealed interface ListDetailsTapAction {
    data class TapToy(val toy: Toy) : ListDetailsTapAction
    data object TapSave : ListDetailsTapAction
    data class TapWebPage(val toy: Toy? = null): ListDetailsTapAction
    data class TapWebSearch(val toy: Toy? = null): ListDetailsTapAction
    data class TapEditToy(val maker: Maker, val toy: Toy): ListDetailsTapAction
    data class TapAddToy(val maker: Maker): ListDetailsTapAction
    data object TapBack : ListDetailsTapAction
    data class UpdateSearchString(val searchString: String) : ListDetailsTapAction
}
