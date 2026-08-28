package com.gepetto.toycollection.models

import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MakerScales(val maker: Maker) {
    @Serializable
    private class ScaleList {
        var scale = ""
        @SerialName("carList") val toyList: ArrayList<Toy> = ArrayList()
    }

    private val scaleListArray: ArrayList<ScaleList> = ArrayList()
    private val scaleList: ArrayList<String> = ArrayList()

    init {
        listOfScales(maker)
    }

    private fun listOfScales(
        maker: Maker,
    ) {
        maker.toysList.forEach { toy ->
            if (toy.traded.isEmpty()) {
                toy.normalizeData()
                if (!scaleList.contains(toy.normalizedScale)) {
                    scaleList.add(toy.normalizedScale)
                    val singleScale = ScaleList()
                    singleScale.scale = toy.normalizedScale
                    singleScale.toyList.add(toy)
                    scaleListArray.add(singleScale)
                } else {
                    scaleListArray[scaleList.indexOf(toy.normalizedScale)].toyList.add(toy)
                }
            }
        }
    }

    fun getScaleList(scale: String = "All") : List<Toy> {
        if (scale == "All")
            return maker.toysList.toImmutableList()
        return scaleListArray[scaleList.indexOf(scale)].toyList.toImmutableList()
    }

    fun getListOfScales () : List<String> {
        return scaleList.toImmutableList()
    }
}
