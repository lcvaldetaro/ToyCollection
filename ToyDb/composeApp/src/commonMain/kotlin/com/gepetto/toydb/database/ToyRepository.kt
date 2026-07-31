package com.gepetto.toydb.database

import club.gepetto.GcLog

class ToyRepository(private val db: ToyDatabase) {

    fun getDashboardStats(): DashboardStats {
        var totalToys = 0
        var totalValue = 0.0
        var totalSpent = 0.0
        val categoryStats = mutableMapOf<String, CategoryStat>()

        // Initialize with zeros
        listOf("slot", "train", "static", "kit", "misc").forEach { cat ->
            categoryStats[cat] = CategoryStat(cat, 0, 0.0, 0.0)
        }

        try {
            val cursor = db.query(
                """
                SELECT toy_type, COUNT(*), SUM(value), SUM(amount_paid) 
                FROM toys 
                WHERE traded IS NULL OR traded = ''
                GROUP BY toy_type
                """.trimIndent()
            )
            while (cursor.next()) {
                val type = cursor.getString("toy_type") ?: ""
                val count = cursor.getInt("COUNT(*)") ?: 0
                val value = cursor.getDouble("SUM(value)") ?: 0.0
                val spent = cursor.getDouble("SUM(amount_paid)") ?: 0.0
                
                totalToys += count
                totalValue += value
                totalSpent += spent
                
                if (type.isNotEmpty()) {
                    categoryStats[type] = CategoryStat(type, count, value, spent)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error getting dashboard stats: ${e.message}", e)
        }

        return DashboardStats(totalToys, totalValue, totalSpent, categoryStats.values.toList())
    }

    fun getToys(
        toyType: String,
        searchQuery: String = "",
        scaleFilter: String = "",
        conditionFilter: String = "",
        makerFilter: String = ""
    ): List<Toy> {
        val toysList = mutableListOf<Toy>()
        val sql = StringBuilder("SELECT * FROM toys WHERE toy_type = ?")
        val bindArgs = mutableListOf<String>()
        bindArgs.add(toyType)

        if (searchQuery.isNotEmpty()) {
            val words = searchQuery.split(" ").filter { it.trim().isNotEmpty() }
            for (word in words) {
                if (word.lowercase() == "repro") {
                    sql.append(" AND (repro = 'y' OR repro = 'r')")
                } else if (word.lowercase() == "boxed") {
                    sql.append(" AND (boxed = 'y' OR boxed = '1')")
                } else if (word.lowercase() == "traded" || word.lowercase() == "gone") {
                    sql.append(" AND (traded IS NOT NULL AND traded != '')")
                } else {
                    sql.append("""
                        AND (
                            description LIKE ? OR 
                            maker_combo LIKE ? OR 
                            body_maker LIKE ? OR 
                            chassis_maker LIKE ? OR 
                            chassis_type LIKE ? OR 
                            motor_maker LIKE ? OR 
                            motor_details LIKE ? OR 
                            comments LIKE ? OR 
                            scale LIKE ? OR 
                            color LIKE ? OR 
                            catalog_number LIKE ? OR 
                            detail LIKE ? OR 
                            condition LIKE ? OR 
                            bitmaps LIKE ? OR 
                            major_work LIKE ? OR 
                            minor_work LIKE ? OR 
                            CAST(ref_num AS TEXT) LIKE ?
                        )
                    """.trimIndent())
                    val likePattern = "%$word%"
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                    bindArgs.add(likePattern)
                }
            }
        }

        if (scaleFilter.isNotEmpty()) {
            sql.append(" AND scale = ?")
            bindArgs.add(scaleFilter)
        }

        if (conditionFilter.isNotEmpty()) {
            sql.append(" AND condition = ?")
            bindArgs.add(conditionFilter)
        }

        if (makerFilter.isNotEmpty()) {
            sql.append(" AND body_maker = ?")
            bindArgs.add(makerFilter)
        }

        sql.append(" ORDER BY description ASC")

        try {
            val cursor = db.query(sql.toString(), bindArgs)
            while (cursor.next()) {
                toysList.add(parseToyFromCursor(cursor, toyType))
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error loading toys for $toyType: ${e.message}", e)
        }
        return toysList
    }

    fun getToy(toyType: String, refNum: Int): Toy? {
        var toy: Toy? = null
        try {
            val cursor = db.query(
                "SELECT * FROM toys WHERE toy_type = ? AND ref_num = ?",
                listOf(toyType, refNum.toString())
            )
            if (cursor.next()) {
                toy = parseToyFromCursor(cursor, toyType)
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error loading toy $toyType:$refNum: ${e.message}", e)
        }
        return toy
    }

    fun getMakers(): List<Maker> {
        val makersList = mutableListOf<Maker>()
        try {
            val cursor = db.query("SELECT * FROM makers ORDER BY name ASC")
            while (cursor.next()) {
                makersList.add(
                    Maker(
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
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error loading makers: ${e.message}", e)
        }
        return makersList
    }

    fun getMaker(name: String): Maker? {
        var maker: Maker? = null
        try {
            val cursor = db.query("SELECT * FROM makers WHERE name = ?", listOf(name))
            if (cursor.next()) {
                maker = Maker(
                    name = cursor.getString("name") ?: "",
                    country = cursor.getString("country") ?: "",
                    bitmaps = cursor.getString("bitmaps") ?: "",
                    bitmapsSize = cursor.getString("bitmaps_size") ?: "",
                    bitmapsTimeStamp = cursor.getString("bitmaps_timestamp") ?: "",
                    comments = cursor.getString("comments") ?: ""
                )
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error loading maker $name: ${e.message}", e)
        }
        return maker
    }

    fun saveMaker(maker: Maker) {
        try {
            db.execute(
                """
                INSERT OR REPLACE INTO makers (
                    name, country, bitmaps, bitmaps_size, bitmaps_timestamp, comments
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                listOf(maker.name, maker.country, maker.bitmaps, maker.bitmapsSize, maker.bitmapsTimeStamp, maker.comments)
            )
            GcLog.d("ToyRepository", "Saved maker ${maker.name}")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error saving maker: ${e.message}", e)
        }
    }

    fun deleteMaker(name: String) {
        try {
            db.execute("DELETE FROM makers WHERE name = ?", listOf(name))
            GcLog.d("ToyRepository", "Deleted maker $name")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error deleting maker: ${e.message}", e)
        }
    }

    fun getToysByMaker(makerName: String): List<Toy> {
        val toysList = mutableListOf<Toy>()
        try {
            val cursor = db.query(
                "SELECT * FROM toys WHERE body_maker = ? ORDER BY description ASC",
                listOf(makerName)
            )
            while (cursor.next()) {
                val toyType = cursor.getString("toy_type") ?: "slot"
                toysList.add(parseToyFromCursor(cursor, toyType))
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error loading toys by maker $makerName: ${e.message}", e)
        }
        return toysList
    }

    fun getMakerCategoryCounts(): Map<String, Map<String, Int>> {
        val counts = mutableMapOf<String, MutableMap<String, Int>>()
        try {
            val cursor = db.query(
                "SELECT body_maker, toy_type, COUNT(*) FROM toys WHERE body_maker IS NOT NULL AND body_maker != '' GROUP BY body_maker, toy_type"
            )
            while (cursor.next()) {
                val maker = cursor.getString("body_maker") ?: ""
                val type = cursor.getString("toy_type") ?: ""
                val count = cursor.getInt("COUNT(*)") ?: 0
                if (maker.isNotEmpty()) {
                    val makerMap = counts.getOrPut(maker) { mutableMapOf() }
                    makerMap[type] = count
                }
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error getting maker category counts: ${e.message}", e)
        }
        return counts
    }

    fun deleteToy(toyType: String, refNum: Int) {
        try {
            db.execute("DELETE FROM toys WHERE toy_type = ? AND ref_num = ?", listOf(toyType, refNum))
            GcLog.d("ToyRepository", "Deleted toy $toyType:$refNum")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error deleting toy: ${e.message}", e)
        }
    }

    fun saveToy(toy: Toy) {
        try {
            val body = toy.bodyMaker.trim()
            val chassis = toy.chassisMaker.trim()
            val calculatedMakerCombo = if (body == chassis) {
                body
            } else if (body.isEmpty()) {
                chassis
            } else if (chassis.isEmpty()) {
                body
            } else {
                "$chassis/$body"
            }
            db.execute(
                """
                INSERT OR REPLACE INTO toys (
                    ref_num, toy_type, description, maker_combo, scale, factory_car, 
                    body_maker, acquired, chassis_type, chassis_maker, condition, color, 
                    motor_maker, motor_details, catalog_number, comments, major_work, 
                    minor_work, repro, value, amount_paid, amount_sold, traded, buy, 
                    maintenance, to_make, detail, boxed, picture, picture_size, 
                    picture_timestamp, has_picture, bitmaps, bitmaps_size, bitmaps_timestamp,
                    year_made, number, my_comments
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                listOf(
                    toy.refNum, toy.toyType, toy.description, calculatedMakerCombo, toy.scale, toy.factoryCar,
                    toy.bodyMaker, toy.acquired, toy.chassisType, toy.chassisMaker, toy.condition, toy.color,
                    toy.motorMaker, toy.motorDetails, toy.catalogNumber, toy.comments, toy.majorWork,
                    toy.minorWork, toy.repro, toy.value, toy.amountPaid, toy.amountSold, toy.traded, toy.buy,
                    toy.maintenance, toy.toMake, toy.detail, toy.boxed, toy.picture, toy.pictureSize,
                    toy.pictureTimeStamp, toy.hasPicture, toy.bitmaps, toy.bitmapsSize, toy.bitmapsTimeStamp,
                    toy.yearMade, toy.number, toy.myComments
                )
            )
            GcLog.d("ToyRepository", "Saved toy ${toy.toyType}:${toy.refNum}")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error saving toy: ${e.message}", e)
        }
    }

    fun getCategorySettings(): List<CategorySetting> {
        val list = mutableListOf<CategorySetting>()
        try {
            val cursor = db.query("SELECT * FROM category_settings")
            while (cursor.next()) {
                list.add(
                    CategorySetting(
                        category = cursor.getString("category") ?: "",
                        imagePrefix = cursor.getString("image_prefix") ?: "",
                        label = cursor.getString("label") ?: "",
                        title = cursor.getString("title") ?: ""
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error getting category settings: ${e.message}", e)
        }
        return list
    }

    fun addCategorySetting(setting: CategorySetting) {
        try {
            db.execute(
                "INSERT INTO category_settings (category, image_prefix, label, title) VALUES (?, ?, ?, ?)",
                listOf(setting.category.trim().lowercase(), setting.imagePrefix.trim(), setting.label.trim(), setting.title.trim())
            )
            GcLog.d("ToyRepository", "Added category ${setting.category}")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error adding category: ${e.message}", e)
        }
    }

    fun updateCategorySetting(setting: CategorySetting) {
        try {
            db.execute(
                "UPDATE category_settings SET image_prefix = ?, label = ?, title = ? WHERE category = ?",
                listOf(setting.imagePrefix.trim(), setting.label.trim(), setting.title.trim(), setting.category)
            )
            GcLog.d("ToyRepository", "Updated category ${setting.category}")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error updating category: ${e.message}", e)
        }
    }

    fun deleteCategorySetting(category: String) {
        try {
            db.execute("DELETE FROM toys WHERE toy_type = ?", listOf(category))
            db.execute("DELETE FROM category_settings WHERE category = ?", listOf(category))
            GcLog.d("ToyRepository", "Deleted category $category and associated toys")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error deleting category: ${e.message}", e)
        }
    }

    fun getThemeSetting(): Int {
        var theme = 0
        try {
            val cursor = db.query("SELECT value FROM app_settings WHERE key = 'theme'")
            if (cursor.next()) {
                val strVal = cursor.getString("value")
                theme = strVal?.toIntOrNull() ?: 0
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error reading theme setting: ${e.message}", e)
        }
        return theme
    }

    fun setThemeSetting(theme: Int) {
        try {
            db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('theme', ?)", listOf(theme.toString()))
            GcLog.d("ToyRepository", "Saved theme setting: $theme")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error saving theme setting: ${e.message}", e)
        }
    }

    fun getImagesPathSetting(): String? {
        var path: String? = null
        try {
            val cursor = db.query("SELECT value FROM app_settings WHERE key = 'images_path'")
            if (cursor.next()) {
                path = cursor.getString("value")
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error reading images_path setting: ${e.message}", e)
        }
        return path
    }

    fun setImagesPathSetting(path: String?) {
        try {
            if (path == null) {
                db.execute("DELETE FROM app_settings WHERE key = 'images_path'")
            } else {
                db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('images_path', ?)", listOf(path))
            }
            GcLog.d("ToyRepository", "Saved images_path setting: $path")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error saving images_path setting: ${e.message}", e)
        }
    }

    fun getImportExportPathSetting(): String? {
        var path: String? = null
        try {
            val cursor = db.query("SELECT value FROM app_settings WHERE key = 'import_export_path'")
            if (cursor.next()) {
                path = cursor.getString("value")
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error reading import_export_path setting: ${e.message}", e)
        }
        return path
    }

    fun setImportExportPathSetting(path: String?) {
        try {
            if (path == null) {
                db.execute("DELETE FROM app_settings WHERE key = 'import_export_path'")
            } else {
                db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('import_export_path', ?)", listOf(path))
            }
            GcLog.d("ToyRepository", "Saved import_export_path setting: $path")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error saving import_export_path setting: ${e.message}", e)
        }
    }

    private fun getAppSetting(key: String): String? {
        var value: String? = null
        try {
            val cursor = db.query("SELECT value FROM app_settings WHERE key = ?", listOf(key))
            if (cursor.next()) {
                value = cursor.getString("value")
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error reading setting $key: ${e.message}", e)
        }
        return value
    }

    private fun setAppSetting(key: String, value: String?) {
        try {
            if (value == null) {
                db.execute("DELETE FROM app_settings WHERE key = ?", listOf(key))
            } else {
                db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)", listOf(key, value))
            }
            GcLog.d("ToyRepository", "Saved setting $key")
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error saving setting $key: ${e.message}", e)
        }
    }

    fun getAppTitleSetting(): String = getAppSetting("app_title") ?: "Gepetto Toy Database Manager"
    fun setAppTitleSetting(title: String) = setAppSetting("app_title", title)

    fun getSftpHostSetting(): String? = getAppSetting("sftp_host")
    fun setSftpHostSetting(host: String?) = setAppSetting("sftp_host", host)

    fun getSftpPortSetting(): Int = getAppSetting("sftp_port")?.toIntOrNull() ?: 22
    fun setSftpPortSetting(port: Int) = setAppSetting("sftp_port", port.toString())

    fun getSftpUsernameSetting(): String? = getAppSetting("sftp_username")
    fun setSftpUsernameSetting(username: String?) = setAppSetting("sftp_username", username)

    fun getSftpAuthTypeSetting(): String = getAppSetting("sftp_auth_type") ?: "password"
    fun setSftpAuthTypeSetting(authType: String) = setAppSetting("sftp_auth_type", authType)

    fun getSftpPasswordSetting(): String? = getAppSetting("sftp_password")
    fun setSftpPasswordSetting(password: String?) = setAppSetting("sftp_password", password)

    fun getSftpKeyPathSetting(): String? = getAppSetting("sftp_key_path")
    fun setSftpKeyPathSetting(path: String?) = setAppSetting("sftp_key_path", path)

    fun getSftpKeyPassphraseSetting(): String? = getAppSetting("sftp_key_passphrase")
    fun setSftpKeyPassphraseSetting(passphrase: String?) = setAppSetting("sftp_key_passphrase", passphrase)

    fun getSftpRemoteDirSetting(): String? = getAppSetting("sftp_remote_dir")
    fun setSftpRemoteDirSetting(dir: String?) = setAppSetting("sftp_remote_dir", dir)

    fun getSftpApprovedFingerprintsSetting(): String? = getAppSetting("sftp_approved_fingerprints")
    fun setSftpApprovedFingerprintsSetting(fingerprints: String?) = setAppSetting("sftp_approved_fingerprints", fingerprints)

    fun addSftpApprovedFingerprint(fingerprint: String) {
        val current = getSftpApprovedFingerprintsSetting()
        if (current.isNullOrEmpty()) {
            setSftpApprovedFingerprintsSetting(fingerprint)
        } else {
            val list = current.split(",").map { it.trim() }.toMutableList()
            if (!list.contains(fingerprint)) {
                list.add(fingerprint)
                setSftpApprovedFingerprintsSetting(list.joinToString(","))
            }
        }
    }

    fun getDistinctScales(toyType: String): List<String> = getDistinctField(toyType, "scale")
    fun getDistinctConditions(toyType: String): List<String> = getDistinctField(toyType, "condition")
    fun getDistinctMakers(toyType: String): List<String> = getDistinctField(toyType, "body_maker")

    private fun getDistinctField(toyType: String, field: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val cursor = db.query(
                "SELECT DISTINCT $field FROM toys WHERE toy_type = ? AND $field IS NOT NULL AND $field != '' ORDER BY $field ASC",
                listOf(toyType)
            )
            while (cursor.next()) {
                cursor.getString(field)?.let { list.add(it) }
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error getting distinct $field: ${e.message}", e)
        }
        return list
    }

    fun getNextRefNum(toyType: String): Int {
        var maxRef = 0
        try {
            val cursor = db.query("SELECT MAX(ref_num) FROM toys WHERE toy_type = ?", listOf(toyType))
            if (cursor.next()) {
                maxRef = cursor.getInt("MAX(ref_num)") ?: 0
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e("ToyRepository", "Error getting max refNum: ${e.message}", e)
        }
        return maxRef + 1
    }

    private fun parseToyFromCursor(cursor: SqlCursor, toyType: String): Toy {
        return Toy(
            refNum = cursor.getInt("ref_num") ?: 0,
            toyType = toyType,
            description = cursor.getString("description") ?: "",
            makerCombo = cursor.getString("maker_combo") ?: "",
            scale = cursor.getString("scale") ?: "",
            factoryCar = cursor.getString("factory_car") ?: "n",
            bodyMaker = cursor.getString("body_maker") ?: "",
            acquired = cursor.getString("acquired") ?: "",
            chassisType = cursor.getString("chassis_type") ?: "",
            chassisMaker = cursor.getString("chassis_maker") ?: "",
            condition = cursor.getString("condition") ?: "",
            color = cursor.getString("color") ?: "",
            motorMaker = cursor.getString("motor_maker") ?: "",
            motorDetails = cursor.getString("motor_details") ?: "",
            catalogNumber = cursor.getString("catalog_number") ?: "",
            comments = cursor.getString("comments") ?: "",
            majorWork = cursor.getString("major_work") ?: "",
            minorWork = cursor.getString("minor_work") ?: "",
            repro = cursor.getString("repro") ?: "",
            value = cursor.getDouble("value") ?: 0.0,
            amountPaid = cursor.getDouble("amount_paid") ?: 0.0,
            amountSold = cursor.getString("amount_sold") ?: "",
            traded = cursor.getString("traded") ?: "",
            buy = cursor.getString("buy") ?: "",
            maintenance = cursor.getString("maintenance") ?: "",
            toMake = cursor.getString("to_make") ?: "",
            detail = cursor.getString("detail") ?: "",
            boxed = cursor.getString("boxed") ?: "n",
            picture = cursor.getString("picture") ?: "",
            pictureSize = cursor.getInt("picture_size") ?: 0,
            pictureTimeStamp = cursor.getDouble("picture_timestamp")?.toLong() ?: 0L,
            hasPicture = cursor.getString("has_picture") ?: "n",
            bitmaps = cursor.getString("bitmaps") ?: "",
            bitmapsSize = cursor.getString("bitmaps_size") ?: "",
            bitmapsTimeStamp = cursor.getString("bitmaps_timestamp") ?: "",
            yearMade = cursor.getString("year_made") ?: "",
            number = cursor.getString("number") ?: "",
            myComments = cursor.getString("my_comments") ?: ""
        )
    }
}

data class DashboardStats(
    val totalToys: Int,
    val totalValue: Double,
    val totalSpent: Double,
    val categories: List<CategoryStat>
)

data class CategoryStat(
    val category: String,
    val count: Int,
    val totalValue: Double,
    val totalSpent: Double
)
