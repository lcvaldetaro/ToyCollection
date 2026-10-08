package com.gepetto.toydb.platform

import kotlinx.browser.localStorage

actual object SettingsStorage {
    actual fun initContext(context: Any?) {
        // No-op on Web
    }

    actual fun getString(key: String, defaultValue: String): String {
        return try {
            localStorage.getItem(key) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    actual fun putString(key: String, value: String) {
        try {
            localStorage.setItem(key, value)
        } catch (_: Exception) {}
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return try {
            localStorage.getItem(key)?.toIntOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    actual fun putInt(key: String, value: Int) {
        try {
            localStorage.setItem(key, value.toString())
        } catch (_: Exception) {}
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            localStorage.getItem(key)?.toBoolean() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        try {
            localStorage.setItem(key, value.toString())
        } catch (_: Exception) {}
    }

    actual fun remove(key: String) {
        try {
            localStorage.removeItem(key)
        } catch (_: Exception) {}
    }
}
