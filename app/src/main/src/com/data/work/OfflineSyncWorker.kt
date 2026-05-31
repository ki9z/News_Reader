package com.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.data.local.db.AppDatabase
import com.data.repository.ProfileRepository

class OfflineSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val database = AppDatabase.getInstance(applicationContext)
            val repository = ProfileRepository(database, applicationContext)
            repository.bootstrap()
            repository.autoDownloadBookmarkedArticles(limit = 20)
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
