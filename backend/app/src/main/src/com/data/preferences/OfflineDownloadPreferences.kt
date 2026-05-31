package com.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfflineDownloadPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("offline_download_preferences", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<OfflineDownloadSettings> = _state.asStateFlow()

    fun updateWifiOnly(value: Boolean) = update { it.copy(wifiOnly = value) }
    fun updateAutoDownloadBookmarks(value: Boolean) = update { it.copy(autoDownloadBookmarks = value) }
    fun updateQuality(value: DownloadQuality) = update { it.copy(quality = value) }
    fun updateAutoDeleteDays(value: Int) = update { it.copy(autoDeleteDays = value.coerceAtLeast(1)) }

    fun current(): OfflineDownloadSettings = readState()

    private fun update(transform: (OfflineDownloadSettings) -> OfflineDownloadSettings) {
        val next = transform(readState())
        prefs.edit()
            .putBoolean(KEY_WIFI_ONLY, next.wifiOnly)
            .putBoolean(KEY_AUTO_DOWNLOAD_BOOKMARKS, next.autoDownloadBookmarks)
            .putString(KEY_QUALITY, next.quality.name)
            .putInt(KEY_AUTO_DELETE_DAYS, next.autoDeleteDays)
            .apply()
        _state.value = next
    }

    private fun readState(): OfflineDownloadSettings {
        return OfflineDownloadSettings(
            wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, true),
            autoDownloadBookmarks = prefs.getBoolean(KEY_AUTO_DOWNLOAD_BOOKMARKS, false),
            quality = runCatching {
                DownloadQuality.valueOf(prefs.getString(KEY_QUALITY, DownloadQuality.FULL.name).orEmpty())
            }.getOrDefault(DownloadQuality.FULL),
            autoDeleteDays = prefs.getInt(KEY_AUTO_DELETE_DAYS, 7).coerceAtLeast(1)
        )
    }

    companion object {
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_AUTO_DOWNLOAD_BOOKMARKS = "auto_download_bookmarks"
        private const val KEY_QUALITY = "quality"
        private const val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
    }
}

data class OfflineDownloadSettings(
    val wifiOnly: Boolean = true,
    val autoDownloadBookmarks: Boolean = false,
    val quality: DownloadQuality = DownloadQuality.FULL,
    val autoDeleteDays: Int = 7
)

enum class DownloadQuality { FULL, LITE, TEXT_ONLY }
