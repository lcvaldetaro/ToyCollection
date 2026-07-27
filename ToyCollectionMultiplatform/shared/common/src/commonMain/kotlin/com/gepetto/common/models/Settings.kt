package com.gepetto.common.models

import com.gepetto.common.Common
import com.gepetto.common.SETTINGS_FILENAME
import com.gepetto.common.GcFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class Settings(
    var caching: Boolean = false,
    var forceLightMode: Boolean = false,
    var forceDarkMode: Boolean = false,
    var baseUrl: String = "",
) {
    fun toJson() : String {
        return Json.encodeToString(serializer(), this)
    }

    fun save(filename: String = SETTINGS_FILENAME) {
        try {
            val savedDataFile = GcFile(Common.packageFolder, filename)
            val jsonStr = this.toJson()
            savedDataFile.writeText(jsonStr)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun fromJson (jsonStr: String) : Settings? {
            var result : Settings? = null
            try {
                val newSettings = Json.decodeFromString(serializer(), jsonStr)
                result = newSettings
            }
            catch (e: Exception) {
               e.printStackTrace()
            }
            return result
        }

        fun restore(filename: String = SETTINGS_FILENAME): Settings? {
            var result: Settings? = null
            val settingsFile = GcFile(Common.packageFolder, filename)

            if (settingsFile.exists()) {
                try {
                    result = fromJson(settingsFile.readText())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return result
        }
    }
}