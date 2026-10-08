package com.gepetto.toydb.platform

import android.content.Context
import android.content.SharedPreferences

actual object SettingsStorage {
    private var prefs: SharedPreferences? = null

    actual fun initContext(context: Any?) {
        if (context is Context) {
            prefs = context.getSharedPreferences("gepetto_toydb_prefs", Context.MODE_PRIVATE)
        }
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs?.getString(key, defaultValue) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return prefs?.getInt(key, defaultValue) ?: defaultValue
    }

    actual fun putInt(key: String, value: Int) {
        prefs?.edit()?.putInt(key, value)?.apply()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs?.getBoolean(key, defaultValue) ?: defaultValue
    }

    actual fun putBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    actual fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }
}
