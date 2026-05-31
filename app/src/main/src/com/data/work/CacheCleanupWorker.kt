package com.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.data.local.db.AppDatabase
import com.data.preferences.OfflineDownloadPreferences
import com.data.repository.ProfileRepository

class CacheCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val preferences = OfflineDownloadPreferences(applicationContext)
            val database = AppDatabase.getInstance(applicationContext)
            val repository = ProfileRepository(database, applicationContext)
            repository.cleanupExpiredDownloads(preferences.current().autoDeleteDays)
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
