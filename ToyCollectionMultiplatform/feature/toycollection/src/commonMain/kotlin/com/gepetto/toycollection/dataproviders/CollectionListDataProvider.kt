package com.gepetto.toycollection.dataproviders

import com.gepetto.common.TOYS_QUERY_STRING
import com.gepetto.common.MISC_QUERY_STRING
import com.gepetto.common.PLASTIC_QUERY_STRING
import com.gepetto.common.STATIC_QUERY_STRING
import com.gepetto.common.TRAINS_QUERY_STRING
import com.gepetto.toycollection.models.CollectionList
import com.gepetto.toycollection.models.ToyCollection

val defaultProcessorCollectionList = CollectionList(
    title = "Gepetto Club Toy Collection",
    toyCollections = listOf(
        ToyCollection("Slot Cars", TOYS_QUERY_STRING, "slotcaricon.png"),
        ToyCollection("Trains", TRAINS_QUERY_STRING, "train.png"),
        ToyCollection("Static Models", STATIC_QUERY_STRING, "staticmodel.png"),
        ToyCollection("Model Kits", PLASTIC_QUERY_STRING, "plastickits.png"),
        ToyCollection("Others", MISC_QUERY_STRING, "others.png"),
    )
)

fun updateLoadedStatus(name: String, status: Boolean) {
    val collection = defaultProcessorCollectionList.findCollection(name)!!
    collection.loaded = status
}