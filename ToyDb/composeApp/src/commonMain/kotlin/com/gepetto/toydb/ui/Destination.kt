package com.gepetto.toydb.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable data object Dashboard : Destination
    @Serializable data class CategoryExplorer(val category: String) : Destination
    @Serializable data object MakerDirectory : Destination
    @Serializable data class MakerDetail(val makerName: String) : Destination
    @Serializable data class ToyDetail(val toyType: String, val refNum: Int) : Destination
    @Serializable data class EditToy(val toyType: String, val refNum: Int) : Destination
    @Serializable data class AddToy(val category: String) : Destination
    @Serializable data object AddMaker : Destination
    @Serializable data class EditMaker(val makerName: String) : Destination
    @Serializable data object Settings : Destination
}
