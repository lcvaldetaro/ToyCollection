package com.gepetto.toycollection.models

import com.gepetto.common.Common
import com.gepetto.toycollection.utils.stripExcessSpaces
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import com.gepetto.common.GcFile

@Serializable
data class Maker (
    var name: String,
    @SerialName("hasCars")var hasToys: Boolean = false,
    var comments: String = "",
    var bitmaps : String = "",
    var bitmapsTimeStamp: String = "",
    var bitmapsSize: String = "",
    var country: String = "",
    @SerialName("carsList")var toysList: List<Toy> = emptyList(),
) {
    var bitmapFiles          : List<String>? = null
    var bitmapFilesTimeStamp : List<String>? = null
    var bitmapFilesSize : List<String>? = null
    var totalFactoryToys = 0
    var picture : String = ""
    @Transient lateinit var scales: MakerScales

    fun add (toy: Toy, collection: CollectionData?) {
        val newToysList = toysList.toMutableList()
        if (collection != null) {
            toy.refNum = collection.getNextRefnum()
            toy.normalizeData()

            newToysList.add(toy)
            toysList = newToysList.toImmutableList()
            sortToysList()
            collection.countAllToys()
            normalizeData()
        }
    }

    fun delete (toy: Toy, collection: CollectionData?) : Boolean {
        if (collection != null) {
            var i = 0
            val newToysList = toysList.toMutableList()
            while (i < newToysList.size) {
                if (toy.refNum == newToysList[i].refNum) {
                    newToysList.removeAt(i)
                    toysList = newToysList.toImmutableList()
                    collection.countAllToys()
                    normalizeData()
                    return true
                }
                i++
            }
        }
        return false
    }

    fun replace (toy: Toy, collection: CollectionData?) {
        if (collection != null) {
            var i = 0
            val newToysList = toysList.toMutableList()
            while (i < newToysList.size) {
                if (toy.refNum == newToysList[i].refNum) {
                    newToysList[i] = toy
                    toysList = newToysList.toImmutableList()
                    sortToysList()
                    collection.countAllToys()
                    normalizeData()
                }
                i++
            }
        }
    }

    fun normalizeData() {
        splitBitmaps()
        setHasToys()
        cleanHtmlFromComments ()
        findMakerPicture()
        countFactoryToys()
        scales = MakerScales(this)
    }

    fun countFactoryToys() {
        totalFactoryToys = 0
        for (toy in toysList)
            if (toy.isFactoryToy())
                totalFactoryToys++
    }

    fun sortToysList () {
        toysList = toysList.sortedBy { it.bodyMaker.lowercase() + it.description.lowercase() }
    }

    fun splitBitmaps () {
        bitmapFiles = bitmaps.stripExcessSpaces().split(" ")
        bitmapFilesTimeStamp = bitmapsTimeStamp.stripExcessSpaces().split(" ")
        bitmapFilesSize = bitmapsSize.stripExcessSpaces().split(" ")
    }

    fun setHasToys() {
        var hasToys = false
        for (toy in toysList) {
            if (!toy.isTraded()) {
                hasToys = true
                break
            }
        }
        this.hasToys = hasToys
    }

    fun cleanHtmlFromComments () {
        comments = comments.stripExcessSpaces().replace("<p>", "\n")
    }

    fun clearCache (directory: GcFile? = Common.directoryFile) {
        GcFile(directory, picture).delete()

        bitmapFiles?.forEach { pic ->
            GcFile(directory, pic).delete()
            Common.clearGcImageCache(pic)
        }

        toysList.forEach { toy -> toy.clearCache(directory) }
    }

    private fun findMakerPicture () : String {
        val maker = this
        var result = ""
        maker.bitmapFiles?.let { if (it[0].isNotEmpty()) result = maker.bitmapFiles!![0] }
        return result
    }

    fun initPicture () {
        this.picture = findMakerPicture()
    }

    fun containsToys (searchString: String) : List<Toy> {
        val returnList = mutableListOf<Toy>()

        if (searchString.isNotEmpty())
            toysList.forEach { toy ->
                if (toy.contains(searchString))
                    returnList.add(toy)
            }
        return returnList
    }

    fun toToysListJson() : String {
        return Json.encodeToString(this.toysList)
    }
}
