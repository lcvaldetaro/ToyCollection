package com.gepetto.toydb.platform

import club.gepetto.GcLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

actual object SettingsStorage {
    private val properties = Properties()
    private val configFile: File by lazy {
        val dir = club.gepetto.utils.getAppDataDir("ToyDatabaseManager")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        File(dir, "toydatabasemanager_setup.properties")
    }

    private fun load() {
        try {
            if (configFile.exists()) {
                FileInputStream(configFile).use { properties.load(it) }
            }
        } catch (e: Exception) {
            GcLog.e("SettingsStorage", "Failed to load desktop properties", e)
        }
    }

    private fun save() {
        try {
            FileOutputStream(configFile).use { properties.store(it, "Gepetto Toy Database Manager Properties") }
        } catch (e: Exception) {
            GcLog.e("SettingsStorage", "Failed to save desktop properties", e)
        }
    }

    actual fun initContext(context: Any?) {
        load()
    }

    actual fun getString(key: String, defaultValue: String): String {
        load()
        return properties.getProperty(key, defaultValue)
    }

    actual fun putString(key: String, value: String) {
        properties.setProperty(key, value)
        save()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        load()
        return properties.getProperty(key)?.toIntOrNull() ?: defaultValue
    }

    actual fun putInt(key: String, value: Int) {
        properties.setProperty(key, value.toString())
        save()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        load()
        return properties.getProperty(key)?.toBoolean() ?: defaultValue
    }

    actual fun putBoolean(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        save()
    }

    actual fun remove(key: String) {
        load()
        properties.remove(key)
        save()
    }
}
