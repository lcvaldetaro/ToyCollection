package com.gepetto.toydb.service

import club.gepetto.GcLog
import com.gepetto.toydb.database.Maker
import com.gepetto.toydb.database.Toy
import com.gepetto.toydb.database.ToyDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

private const val TAG = "ImportExportService"

@Serializable
data class JsonMaker(
    val name: String,
    val country: String = "",
    val bitmaps: String = "",
    val bitmapsSize: String = "",
    val bitmapsTimeStamp: String = "",
    val comments: String = ""
)

@Serializable
data class JsonToy(
    val refNum: String,
    val makerCombo: String = "",
    val condition: String = "",
    val scale: String = "",
    val description: String,
    val comments: String = "",
    val bodyMaker: String = "",
    val chassisMaker: String = "",
    val chassisType: String = "",
    val motorMaker: String = "",
    val motorDetails: String = "",
    val repro: String = "",
    val boxed: String = "",
    val color: String = "",
    val catalogNumber: String = "",
    val majorWork: String = "",
    val minorWork: String = "",
    val maintenance: String = "",
    val detail: String = "",
    val toMake: String = "",
    val buy: String = "",
    val traded: String = "",
    val value: String = "",
    val acquired: String = "",
    val amountPaid: String = "",
    val amountSold: String = "",
    val bitmaps: String = "",
    val bitmapsTimeStamp: String = "",
    val bitmapsSize: String = "",
    val picture: String = "",
    val pictureTimeStamp: String = "",
    val pictureSize: String = "",
    val hasPicture: String = "",
    val factoryCar: String = ""
)

@Serializable
data class JsonMakersFile(
    val date: String = "July 17, 2026",
    val buildNumber: String = "1052",
    val makers: List<JsonMaker>
)

@Serializable
data class JsonToysFile(
    val date: String = "July 17, 2026",
    val buildNumber: String = "1052",
    val cars: List<JsonToy>
)

@Serializable
data class JsonCategorySetting(
    val category: String,
    val imagePrefix: String,
    val label: String
)

@Serializable
data class JsonCategorySettingsFile(
    val date: String = "July 17, 2026",
    val buildNumber: String = "1052",
    val settings: List<JsonCategorySetting>
)

@Serializable
data class JsonAppSetting(
    val key: String,
    val value: String
)

@Serializable
data class JsonAppSettingsFile(
    val date: String = "July 17, 2026",
    val buildNumber: String = "1052",
    val settings: List<JsonAppSetting>
)

object ImportExportService {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private fun getImagesPath(db: ToyDatabase): String? {
        var path: String? = null
        try {
            val cursor = db.query("SELECT value FROM app_settings WHERE key = 'images_path'")
            if (cursor.next()) {
                path = cursor.getString("value")
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e(TAG, "Error reading images_path: ${e.message}", e)
        }
        return path
    }

    private fun processBitmaps(
        db: ToyDatabase,
        incomingBitmaps: String,
        incomingSizes: String,
        incomingTimestamps: String
    ): Triple<String, String, String> {
        val customPath = getImagesPath(db)
        val targetDir = if (!customPath.isNullOrEmpty()) {
            customPath.toPath()
        } else {
            val possibleDirs = listOf("images", "../images", "ToyDb/images", "../ToyDb/images")
            possibleDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) } ?: "images".toPath()
        }

        val filenames = incomingBitmaps.split(" ").filter { it.trim().isNotEmpty() }
        val sizes = incomingSizes.split(" ").filter { it.trim().isNotEmpty() }
        val timestamps = incomingTimestamps.split(" ").filter { it.trim().isNotEmpty() }

        val finalBitmaps = mutableListOf<String>()
        val finalSizes = mutableListOf<String>()
        val finalTimestamps = mutableListOf<String>()

        filenames.forEachIndexed { index, filename ->
            val trimmedFilename = filename.trim()
            val file = targetDir.div(trimmedFilename)
            val metadata = FileSystem.SYSTEM.metadataOrNull(file)
            if (metadata != null && metadata.isRegularFile) {
                val actualSize = metadata.size ?: 0L
                val actualTimestamp = metadata.lastModifiedAtMillis ?: 0L
                finalBitmaps.add(trimmedFilename)
                finalSizes.add(actualSize.toString())
                finalTimestamps.add(actualTimestamp.toString())
            } else {
                val sizeVal = sizes.getOrNull(index)?.trim() ?: ""
                val timeVal = timestamps.getOrNull(index)?.trim() ?: ""
                if (sizeVal.isNotEmpty() && timeVal.isNotEmpty()) {
                    finalBitmaps.add(trimmedFilename)
                    finalSizes.add(sizeVal)
                    finalTimestamps.add(timeVal)
                }
            }
        }

        return Triple(
            finalBitmaps.joinToString(" "),
            finalSizes.joinToString(" "),
            finalTimestamps.joinToString(" ")
        )
    }

    /**
     * Imports makers from JSON string content into the database.
     */
    fun importMakers(db: ToyDatabase, jsonContent: String): Int {
        GcLog.d(TAG, "Importing makers...")
        val parsed = json.decodeFromString<JsonMakersFile>(jsonContent)
        var count = 0
        parsed.makers.forEach { m ->
            val (bitmaps, bitmapsSize, bitmapsTimeStamp) = processBitmaps(
                db,
                m.bitmaps,
                m.bitmapsSize,
                m.bitmapsTimeStamp
            )
            db.execute(
                """
                INSERT OR REPLACE INTO makers 
                (name, country, bitmaps, bitmaps_size, bitmaps_timestamp, comments) 
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                listOf(m.name, m.country, bitmaps, bitmapsSize, bitmapsTimeStamp, m.comments)
            )
            count++
        }
        GcLog.d(TAG, "Imported $count makers.")
        return count
    }

    /**
     * Imports toys of a specific category from JSON string content into the database.
     */
    fun importToys(db: ToyDatabase, toyType: String, jsonContent: String): Int {
        GcLog.d(TAG, "Importing toys for type '$toyType'...")
        val parsed = json.decodeFromString<JsonToysFile>(jsonContent)
        var count = 0
        parsed.cars.forEach { c ->
            val ref = c.refNum.toIntOrNull() ?: 0
            val valDouble = c.value.trim().toDoubleOrNull() ?: 0.0
            val paidDouble = c.amountPaid.trim().toDoubleOrNull() ?: 0.0

            val customPath = getImagesPath(db)
            val targetDir = if (!customPath.isNullOrEmpty()) {
                customPath.toPath()
            } else {
                val possibleDirs = listOf("images", "../images", "ToyDb/images", "../ToyDb/images")
                possibleDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) } ?: "images".toPath()
            }

            var finalPicture = ""
            var finalPicSize = 0
            var finalPicTime = 0L
            var finalHasPicture = "n"

            val incomingPicture = c.picture.trim()
            if (incomingPicture.isNotEmpty()) {
                val file = targetDir.div(incomingPicture)
                val metadata = FileSystem.SYSTEM.metadataOrNull(file)
                if (metadata != null && metadata.isRegularFile) {
                    finalPicture = incomingPicture
                    finalPicSize = (metadata.size ?: 0L).toInt()
                    finalPicTime = metadata.lastModifiedAtMillis ?: 0L
                    finalHasPicture = "y"
                } else {
                    val incomingSize = c.pictureSize.trim().toIntOrNull() ?: 0
                    val incomingTime = c.pictureTimeStamp.trim().toLongOrNull() ?: 0L
                    if (incomingSize > 0 && incomingTime > 0L) {
                        finalPicture = incomingPicture
                        finalPicSize = incomingSize
                        finalPicTime = incomingTime
                        finalHasPicture = if (c.hasPicture.trim().isNotEmpty()) c.hasPicture.trim() else "y"
                    }
                }
            }
            
            val (bitmaps, bitmapsSize, bitmapsTimeStamp) = processBitmaps(
                db,
                c.bitmaps,
                c.bitmapsSize,
                c.bitmapsTimeStamp
            )

            db.execute(
                """
                INSERT OR REPLACE INTO toys (
                    ref_num, toy_type, description, maker_combo, scale, factory_car, 
                    body_maker, acquired, chassis_type, chassis_maker, condition, color, 
                    motor_maker, motor_details, catalog_number, comments, major_work, 
                    minor_work, repro, value, amount_paid, amount_sold, traded, buy, 
                    maintenance, to_make, detail, boxed, picture, picture_size, 
                    picture_timestamp, has_picture, bitmaps, bitmaps_size, bitmaps_timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                listOf(
                    ref, toyType, c.description, c.makerCombo, c.scale, c.factoryCar,
                    c.bodyMaker, c.acquired, c.chassisType, c.chassisMaker, c.condition, c.color,
                    c.motorMaker, c.motorDetails, c.catalogNumber, c.comments, c.majorWork,
                    c.minorWork, c.repro, valDouble, paidDouble, c.amountSold, c.traded, c.buy,
                    c.maintenance, c.toMake, c.detail, c.boxed, finalPicture, finalPicSize,
                    finalPicTime, finalHasPicture, bitmaps, bitmapsSize, bitmapsTimeStamp
                )
            )
            count++
        }
        GcLog.d(TAG, "Imported $count toys for type '$toyType'.")
        return count
    }

    /**
     * Exports all makers from the database into JSON format.
     */
    fun exportMakers(db: ToyDatabase): String {
        GcLog.d(TAG, "Exporting makers...")
        val cursor = db.query("SELECT * FROM makers ORDER BY name ASC")
        val list = mutableListOf<JsonMaker>()
        while (cursor.next()) {
            list.add(
                JsonMaker(
                    name = cursor.getString("name") ?: "",
                    country = cursor.getString("country") ?: "",
                    bitmaps = cursor.getString("bitmaps") ?: "",
                    bitmapsSize = cursor.getString("bitmaps_size") ?: "",
                    bitmapsTimeStamp = cursor.getString("bitmaps_timestamp") ?: "",
                    comments = cursor.getString("comments") ?: ""
                )
            )
        }
        cursor.close()
        val fileObj = JsonMakersFile(makers = list)
        return json.encodeToString(JsonMakersFile.serializer(), fileObj)
    }

    /**
     * Exports toys of a specific category from the database into JSON format.
     */
    fun exportToys(db: ToyDatabase, toyType: String): String {
        GcLog.d(TAG, "Exporting toys for type '$toyType'...")
        val cursor = db.query("SELECT * FROM toys WHERE toy_type = ? ORDER BY ref_num ASC", listOf(toyType))
        val list = mutableListOf<JsonToy>()
        while (cursor.next()) {
            val ref = cursor.getInt("ref_num") ?: 0
            val valDouble = cursor.getDouble("value") ?: 0.0
            val paidDouble = cursor.getDouble("amount_paid") ?: 0.0
            val picSize = cursor.getInt("picture_size") ?: 0
            val picTime = cursor.getDouble("picture_timestamp")?.toLong() ?: 0L // JDBC double to long fallback helper

            list.add(
                JsonToy(
                    refNum = ref.toString(),
                    makerCombo = cursor.getString("maker_combo") ?: "",
                    condition = cursor.getString("condition") ?: "",
                    scale = cursor.getString("scale") ?: "",
                    description = cursor.getString("description") ?: "",
                    comments = cursor.getString("comments") ?: "",
                    bodyMaker = cursor.getString("body_maker") ?: "",
                    chassisMaker = cursor.getString("chassis_maker") ?: "",
                    chassisType = cursor.getString("chassis_type") ?: "",
                    motorMaker = cursor.getString("motor_maker") ?: "",
                    motorDetails = cursor.getString("motor_details") ?: "",
                    repro = cursor.getString("repro") ?: "",
                    boxed = cursor.getString("boxed") ?: "",
                    color = cursor.getString("color") ?: "",
                    catalogNumber = cursor.getString("catalog_number") ?: "",
                    majorWork = cursor.getString("major_work") ?: "",
                    minorWork = cursor.getString("minor_work") ?: "",
                    maintenance = cursor.getString("maintenance") ?: "",
                    detail = cursor.getString("detail") ?: "",
                    toMake = cursor.getString("to_make") ?: "",
                    buy = cursor.getString("buy") ?: "",
                    traded = cursor.getString("traded") ?: "",
                    value = formatDouble(valDouble),
                    acquired = cursor.getString("acquired") ?: "",
                    amountPaid = formatDouble(paidDouble),
                    amountSold = cursor.getString("amount_sold") ?: "",
                    bitmaps = cursor.getString("bitmaps") ?: "",
                    bitmapsTimeStamp = cursor.getString("bitmaps_timestamp") ?: "",
                    bitmapsSize = cursor.getString("bitmaps_size") ?: "",
                    picture = cursor.getString("picture") ?: "",
                    pictureTimeStamp = if (picTime > 0) picTime.toString() else "",
                    pictureSize = if (picSize > 0) picSize.toString() else "",
                    hasPicture = cursor.getString("has_picture") ?: "",
                    factoryCar = cursor.getString("factory_car") ?: ""
                )
            )
        }
        cursor.close()
        val fileObj = JsonToysFile(cars = list)
        return json.encodeToString(JsonToysFile.serializer(), fileObj)
    }

    fun importCategorySettings(db: ToyDatabase, jsonContent: String): Int {
        GcLog.d(TAG, "Importing category settings...")
        val parsed = json.decodeFromString<JsonCategorySettingsFile>(jsonContent)
        var count = 0
        parsed.settings.forEach { s ->
            db.execute(
                """
                INSERT OR REPLACE INTO category_settings (category, image_prefix, label)
                VALUES (?, ?, ?)
                """.trimIndent(),
                listOf(s.category, s.imagePrefix, s.label)
            )
            count++
        }
        GcLog.d(TAG, "Imported $count category settings.")
        return count
    }

    fun exportCategorySettings(db: ToyDatabase): String {
        GcLog.d(TAG, "Exporting category settings...")
        val cursor = db.query("SELECT * FROM category_settings ORDER BY category ASC")
        val list = mutableListOf<JsonCategorySetting>()
        while (cursor.next()) {
            list.add(
                JsonCategorySetting(
                    category = cursor.getString("category") ?: "",
                    imagePrefix = cursor.getString("image_prefix") ?: "",
                    label = cursor.getString("label") ?: ""
                )
            )
        }
        cursor.close()
        val fileObj = JsonCategorySettingsFile(settings = list)
        return json.encodeToString(JsonCategorySettingsFile.serializer(), fileObj)
    }

    fun importAppSettings(db: ToyDatabase, jsonContent: String): Int {
        GcLog.d(TAG, "Importing app settings...")
        val parsed = json.decodeFromString<JsonAppSettingsFile>(jsonContent)
        var count = 0
        parsed.settings.forEach { s ->
            db.execute(
                """
                INSERT OR REPLACE INTO app_settings (key, value)
                VALUES (?, ?)
                """.trimIndent(),
                listOf(s.key, s.value)
            )
            count++
        }
        GcLog.d(TAG, "Imported $count app settings.")
        return count
    }

    fun exportAppSettings(db: ToyDatabase): String {
        GcLog.d(TAG, "Exporting app settings...")
        val cursor = db.query("SELECT * FROM app_settings ORDER BY key ASC")
        val list = mutableListOf<JsonAppSetting>()
        while (cursor.next()) {
            list.add(
                JsonAppSetting(
                    key = cursor.getString("key") ?: "",
                    value = cursor.getString("value") ?: ""
                )
            )
        }
        cursor.close()
        val fileObj = JsonAppSettingsFile(settings = list)
        return json.encodeToString(JsonAppSettingsFile.serializer(), fileObj)
    }

    private fun formatDouble(value: Double): String {
        if (value <= 0.0) return ""
        val rounded = (value * 100 + 0.5).toLong() / 100.0
        val parts = rounded.toString().split(".")
        val integerPart = parts[0]
        val decimalPart = parts.getOrNull(1) ?: "00"
        val paddedDecimal = decimalPart.padEnd(2, '0').take(2)
        return "$integerPart.$paddedDecimal"
    }
}
