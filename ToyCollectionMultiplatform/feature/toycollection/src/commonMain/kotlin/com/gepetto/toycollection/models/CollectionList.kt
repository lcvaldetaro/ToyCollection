package com.gepetto.toycollection.models

import com.gepetto.common.Common
import com.gepetto.common.SAVED_FILENAME
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import club.gepetto.GcLog
import com.gepetto.common.GcFile

@Serializable
data class ToyCollection(
    val title: String,
    val typeQuery: String,
    val image: String,
    var loaded: Boolean = false,
) {
    @Transient var collectionData: CollectionData? = null
}

@Serializable
data class CollectionList(
    val title: String,
    val toyCollections: List<ToyCollection>,
    var totals: String? = null,
) {
    fun toJson() : String {
        return Json.encodeToString(this)
    }

    fun save(
        filename: String = SAVED_FILENAME,
    ) {
        try {
            val filenameComplete = "${filename}-collectionlist"
            val savedDataFile = GcFile(Common.packageFolder, filenameComplete)
            val jsonStr = this.toJson()
            savedDataFile.writeText(jsonStr)
            GcLog.d("collection list saved on ${Common.packageFolder}/${filenameComplete}")
        }
        catch (e: Exception) {
            GcLog.e("save Exception ${e}")
            e.printStackTrace()
        }
    }

    fun findCollection(collectionName: String) : ToyCollection? {
        this.toyCollections.forEach { collection ->
            if (collection.title == collectionName) return collection
        }
        return null
    }

    companion object {
        fun fromJson (jsonStr: String) : CollectionList? {
            var result : CollectionList? = null
            try {
                val newCollectionListSaved = Json.decodeFromString<CollectionList>(jsonStr)
                result = newCollectionListSaved
            }
            catch (e: Exception) {
                GcLog.e("Exception ${e}")
                e.printStackTrace()
            }
            return result
        }

        fun restore(
            filename: String = SAVED_FILENAME,
        ) : CollectionList? {
            val filenameComplete = "${filename}-collectionlist"
            var resultCollectionList: CollectionList? = null
            val savedCollectionListFile = GcFile( Common.packageFolder, filenameComplete)

            if (savedCollectionListFile.exists()) {
                GcLog.d("Saved collection list exists on '${Common.packageFolder}${filenameComplete}'")
                try {
                    resultCollectionList = fromJson(savedCollectionListFile.readText())
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    resultCollectionList = null
                }
            }

            if (resultCollectionList == null)
                GcLog.d("Saved collection list do not exist on '${Common.packageFolder}${filenameComplete}'. returning $resultCollectionList")

            return resultCollectionList
        }
    }
}
