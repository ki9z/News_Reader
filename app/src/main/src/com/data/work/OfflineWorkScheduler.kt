package com.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.data.preferences.OfflineDownloadPreferences
import java.util.concurrent.TimeUnit

class OfflineWorkScheduler(
    context: Context,
    private val preferences: OfflineDownloadPreferences
) {
    private val appContext = context.applicationContext

    fun schedule() {
        val settings = preferences.current()
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(syncConstraints)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_AUTO_DOWNLOAD_BOOKMARKS,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_CLEANUP_DOWNLOADS,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest
        )
    }

    fun cancelAutoDownload() {
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_AUTO_DOWNLOAD_BOOKMARKS)
    }

    companion object {
        const val WORK_AUTO_DOWNLOAD_BOOKMARKS = "auto_download_bookmarked_articles"
        const val WORK_CLEANUP_DOWNLOADS = "cleanup_old_offline_articles"
    }
}
