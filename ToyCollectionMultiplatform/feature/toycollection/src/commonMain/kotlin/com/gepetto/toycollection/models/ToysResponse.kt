package com.gepetto.toycollection.models

import com.gepetto.common.Common
import com.gepetto.common.SAVED_FILENAME
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import club.gepetto.GcLog
import com.gepetto.common.GcFile

@Serializable
data class ToysResponse (
    val date: String,
    val buildNumber: String,
    @SerialName("cars") var toys: List<Toy>
) {
    val size: Int get() = toys.size
    operator fun get(position: Int): Toy = toys[position]

    @Transient var totalToys = 0
    @Transient var valueCollection  = 0.0F
    @Transient var costCollection   = 0.0F

    fun normalizeData () : ToysResponse {
        for (toy in toys)
            toy.normalizeData()

        toys = toys.sortedBy { it.bodyMaker.lowercase() + it.description.lowercase() }

        totalToys = toys.size

        return this
    }

    fun contains (searchString: String) : List<Toy> {
        val returnList: ArrayList<Toy> = ArrayList(0)
        for (toy in toys) {
            if (toy.contains(searchString))
                returnList.add(toy)
        }
        return returnList
    }

    fun toJson() : String {
        return Json.encodeToString(this)
    }

    fun save(
        typeQuery: String,
        filename: String = "${SAVED_FILENAME}-toys-",
    ) {
        val filenameComplete = "${filename}${typeQuery}"
        val savedDataFile = GcFile(Common.packageFolder, filenameComplete)
        savedDataFile.writeText(this.toJson())
        GcLog.d("save saved json file ${Common.packageFolder}/${filenameComplete}")
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(jsonStr: String) : ToysResponse? {
            var result : ToysResponse? = null
            try {
               result = json.decodeFromString<ToysResponse>(jsonStr)
            }
            catch (e: Exception) {
                GcLog.e("Exception ${e}")
            }
            return result
        }

        fun restore(
            typeQuery: String,
            filename: String = "${SAVED_FILENAME}-toys-",
        ) : ToysResponse? {
            var result: ToysResponse? = null
            try {
                val filenameComplete = "${filename}${typeQuery}"
                val savedDataFile = GcFile(Common.packageFolder, filenameComplete)
                result = fromJson(savedDataFile.readText())
            }
            catch (e: Exception) {
                GcLog.e("exception ${e}")
            }
            return result
        }
    }
}