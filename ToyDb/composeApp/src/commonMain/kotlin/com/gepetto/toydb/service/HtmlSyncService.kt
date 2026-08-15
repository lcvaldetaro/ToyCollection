package com.gepetto.toydb.service

import club.gepetto.GcLog
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object HtmlSyncService {
    private const val TAG = "HtmlSyncService"
    
    private val client = HttpClient(OkHttp)
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun parseJsonDate(dateStr: String): Long {
        return try {
            val formatter = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            formatter.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun calculateHash(content: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(content.encodeToByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            content.hashCode().toString()
        }
    }
    
    suspend fun syncIfNewer(db: ToyDatabase, repository: ToyRepository): Boolean {
        val baseUrl = repository.getBaseUrlSetting()
        if (baseUrl.isNullOrBlank()) {
            GcLog.d(TAG, "Base URL not set, skipping HTML synchronization.")
            return false
        }
        
        return withContext(club.gepetto.utils.ioDispatcher) {
            try {
                // 1. Download category_settings.json
                val catSettingsUrl = if (baseUrl.endsWith("/")) "${baseUrl}category_settings.json" else "$baseUrl/category_settings.json"
                val catResponse = client.get(catSettingsUrl)
                if (catResponse.status.value != 200) {
                    GcLog.e(TAG, "Failed to download category_settings.json from server: HTTP ${catResponse.status.value}")
                    return@withContext false
                }
                val catContent = catResponse.bodyAsText()
                val catServerHash = calculateHash(catContent)
                
                // Parse date from category_settings.json
                val catJson = json.parseToJsonElement(catContent) as? JsonObject
                val catDateStr = catJson?.get("date")?.jsonPrimitive?.content ?: ""
                val catServerTime = parseJsonDate(catDateStr)
                
                val cursor = db.query("SELECT value FROM app_settings WHERE key = 'html_sync_imported_date_category_settings.json'")
                val catStoredDateStr = if (cursor.next()) cursor.getString("value") ?: "" else ""
                cursor.close()
                val catStoredTime = parseJsonDate(catStoredDateStr)

                val cursorHash = db.query("SELECT value FROM app_settings WHERE key = 'html_sync_imported_hash_category_settings.json'")
                val catStoredHash = if (cursorHash.next()) cursorHash.getString("value") ?: "" else ""
                cursorHash.close()
                
                var isNewer = catServerTime > catStoredTime || catStoredDateStr.isEmpty() || (catServerTime == catStoredTime && catServerHash != catStoredHash)
                
                // Parse category settings to get dynamic categories
                val parsedSettings = json.decodeFromString<JsonCategorySettingsFile>(catContent)
                val categories = parsedSettings.settings
                
                // 2. Resolve makers file on server (carmaker.json or makers.json)
                var makersFilename = "carmaker.json"
                var makersUrl = if (baseUrl.endsWith("/")) "$baseUrl$makersFilename" else "$baseUrl/$makersFilename"
                var makersResponse = client.get(makersUrl)
                if (makersResponse.status.value != 200) {
                    makersFilename = "makers.json"
                    makersUrl = if (baseUrl.endsWith("/")) "$baseUrl$makersFilename" else "$baseUrl/$makersFilename"
                    makersResponse = client.get(makersUrl)
                }
                if (makersResponse.status.value != 200) {
                    GcLog.e(TAG, "Failed to locate makers JSON file on server.")
                    return@withContext false
                }
                val makersContent = makersResponse.bodyAsText()
                val makersServerHash = calculateHash(makersContent)

                val makersJson = json.parseToJsonElement(makersContent) as? JsonObject
                val makersDateStr = makersJson?.get("date")?.jsonPrimitive?.content ?: ""
                val makersServerTime = parseJsonDate(makersDateStr)
                
                val cursorMakers = db.query("SELECT value FROM app_settings WHERE key = 'html_sync_imported_date_makers'")
                val makersStoredDateStr = if (cursorMakers.next()) cursorMakers.getString("value") ?: "" else ""
                cursorMakers.close()
                val makersStoredTime = parseJsonDate(makersStoredDateStr)

                val cursorMakersHash = db.query("SELECT value FROM app_settings WHERE key = 'html_sync_imported_hash_makers'")
                val makersStoredHash = if (cursorMakersHash.next()) cursorMakersHash.getString("value") ?: "" else ""
                cursorMakersHash.close()

                if (makersServerTime > makersStoredTime || makersStoredDateStr.isEmpty() || (makersServerTime == makersStoredTime && makersServerHash != makersStoredHash)) {
                    isNewer = true
                }
                
                // 3. For each category, dynamically find the list JSON file on the server
                val categoryFilesToDownload = mutableListOf<Triple<String, String, String>>() // Category, Filename, Content
                for (cat in categories) {
                    val potentialNames = listOf(
                        "${cat.imagePrefix}list.json",
                        "${cat.category}s.json",
                        "${cat.category}list.json",
                        "${cat.category}.json"
                    )
                    var foundContent: String? = null
                    var foundFilename: String? = null
                    for (name in potentialNames) {
                        val url = if (baseUrl.endsWith("/")) "$baseUrl$name" else "$baseUrl/$name"
                        val response = client.get(url)
                        if (response.status.value == 200) {
                            foundContent = response.bodyAsText()
                            foundFilename = name
                            break
                        }
                    }
                    if (foundContent == null || foundFilename == null) {
                        GcLog.e(TAG, "Failed to find JSON list file for category '${cat.category}' on server.")
                        return@withContext false
                    }
                    
                    val listServerHash = calculateHash(foundContent)
                    val listJson = json.parseToJsonElement(foundContent) as? JsonObject
                    val listDateStr = listJson?.get("date")?.jsonPrimitive?.content ?: ""
                    val listServerTime = parseJsonDate(listDateStr)
                    
                    val cursorList = db.query("SELECT value FROM app_settings WHERE key = 'html_sync_imported_date_${cat.category}'")
                    val listStoredDateStr = if (cursorList.next()) cursorList.getString("value") ?: "" else ""
                    cursorList.close()
                    val listStoredTime = parseJsonDate(listStoredDateStr)

                    val cursorListHash = db.query("SELECT value FROM app_settings WHERE key = 'html_sync_imported_hash_${cat.category}'")
                    val listStoredHash = if (cursorListHash.next()) cursorListHash.getString("value") ?: "" else ""
                    cursorListHash.close()

                    if (listServerTime > listStoredTime || listStoredDateStr.isEmpty() || (listServerTime == listStoredTime && listServerHash != listStoredHash)) {
                        isNewer = true
                    }
                    
                    categoryFilesToDownload.add(Triple(cat.category, foundFilename, foundContent))
                }
                
                // If nothing is newer, skip sync
                if (!isNewer) {
                    GcLog.d(TAG, "Local database is already up to date with HTML server.")
                    return@withContext false
                }
                
                GcLog.i(TAG, "HTML Server has newer backups. Performing clean import...")
                
                // 4. Perform clean import in a transaction / thread-safe manner
                db.execute("DELETE FROM toys")
                db.execute("DELETE FROM makers")
                db.execute("DELETE FROM category_settings")
                
                // Import category settings
                ImportExportService.importCategorySettings(db, catContent)
                
                // Import makers
                ImportExportService.importMakers(db, makersContent)
                
                // Import toys
                for (item in categoryFilesToDownload) {
                    ImportExportService.importToys(db, item.first, item.third)
                }
                
                // 5. Save the new imported date strings and hashes to metadata
                saveMetadataSetting(db, "html_sync_imported_date_category_settings.json", catDateStr)
                saveMetadataSetting(db, "html_sync_imported_hash_category_settings.json", catServerHash)

                saveMetadataSetting(db, "html_sync_imported_date_makers", makersDateStr)
                saveMetadataSetting(db, "html_sync_imported_hash_makers", makersServerHash)

                for (item in categoryFilesToDownload) {
                    val listJson = json.parseToJsonElement(item.third) as? JsonObject
                    val listDateStr = listJson?.get("date")?.jsonPrimitive?.content ?: ""
                    val listServerHash = calculateHash(item.third)
                    saveMetadataSetting(db, "html_sync_imported_date_${item.first}", listDateStr)
                    saveMetadataSetting(db, "html_sync_imported_hash_${item.first}", listServerHash)
                }
                
                GcLog.i(TAG, "HTML Startup Sync completed successfully.")
                return@withContext true
            } catch (e: Exception) {
                GcLog.e(TAG, "Error performing HTML startup sync: ${e.message}", e)
                return@withContext false
            }
        }
    }
    
    private fun saveMetadataSetting(db: ToyDatabase, key: String, value: String) {
        try {
            db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)", listOf(key, value))
        } catch (e: Exception) {
            GcLog.e(TAG, "Error saving sync metadata $key: ${e.message}", e)
        }
    }
}
