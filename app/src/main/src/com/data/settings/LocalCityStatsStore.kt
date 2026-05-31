package com.data.settings

import android.content.Context
import android.content.SharedPreferences

class LocalCityStatsStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getHitCount(cityTitle: String): Int {
        return prefs.getInt(keyFor(cityTitle), 0)
    }

    fun incrementHitCount(cityTitle: String, delta: Int) {
        if (delta <= 0) return
        val key = keyFor(cityTitle)
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + delta).apply()
    }

    private fun keyFor(cityTitle: String): String {
        return cityTitle.trim().lowercase().replace(" ", "_")
    }

    private companion object {
        const val PREF_NAME = "local_city_stats"
    }
}

