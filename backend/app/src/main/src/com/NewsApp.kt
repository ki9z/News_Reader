package com

import android.app.Application
import com.data.local.db.AppDatabase
import com.data.local.seed.PreCrawledNewsDataSource
import com.data.local.source.LocalNewsDataSource
import com.data.local.source.LocalNewsDataSourceImpl
import com.data.local.source.OfflineNewsDataSource
import com.data.local.source.OfflineNewsDataSourceImpl
import com.data.preferences.FollowPreferenceRepository
import com.data.preferences.OfflineDownloadPreferences
import com.data.work.OfflineWorkScheduler
import com.data.remote.client.RetrofitClient
import com.data.remote.source.RemoteNewsDataSource
import com.data.remote.source.RemoteNewsDataSourceImpl
import com.data.repository.ProfileRepository
import com.data.repository.NewsRepository
import com.data.repository.NewsRepositoryImpl
import com.data.security.TokenManager
import com.data.settings.LocalCityStatsStore
import com.data.settings.UserSettingsRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.notifications.DeviceTokenRegistrar
import kotlinx.coroutines.launch

class NewsApp : Application() {
    val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    private val preCrawledNewsDataSource: PreCrawledNewsDataSource by lazy {
        PreCrawledNewsDataSource(this)
    }

    val tokenManager: TokenManager by lazy {
        TokenManager(this)
    }

    private val remoteNewsDataSource: RemoteNewsDataSource by lazy {
        RetrofitClient.initialize(this)
        RemoteNewsDataSourceImpl(RetrofitClient.newsApiService)
    }

    private val localNewsDataSource: LocalNewsDataSource by lazy {
        LocalNewsDataSourceImpl(
            articleDao = appDatabase.articleDao(),
            localNewsCacheDao = appDatabase.localNewsCacheDao()
        )
    }

    private val offlineNewsDataSource: OfflineNewsDataSource by lazy {
        OfflineNewsDataSourceImpl(preCrawledNewsDataSource)
    }

    val repository: NewsRepository by lazy {
        NewsRepositoryImpl(
            remoteDataSource = remoteNewsDataSource,
            localDataSource = localNewsDataSource,
            offlineDataSource = offlineNewsDataSource,
            articleDao = appDatabase.articleDao(),
            bookmarkDao = appDatabase.bookmarkDao(),
            userDao = appDatabase.userDao(),
            preCrawledNewsDataSource = preCrawledNewsDataSource
        )
    }

    val offlineDownloadPreferences: OfflineDownloadPreferences by lazy {
        OfflineDownloadPreferences(this)
    }

    val offlineWorkScheduler: OfflineWorkScheduler by lazy {
        OfflineWorkScheduler(this, offlineDownloadPreferences)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(appDatabase, this)
    }

    val followPreferenceRepository: FollowPreferenceRepository by lazy {
        FollowPreferenceRepository(this)
    }

    val userSettingsRepository: UserSettingsRepository by lazy {
        UserSettingsRepository(this)
    }

    val localCityStatsStore: LocalCityStatsStore by lazy {
        LocalCityStatsStore(this)
    }

    override fun onCreate() {
        super.onCreate()
        com.util.CrashLogger.install(this)

        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            com.data.initializer.DatabaseInitializer.init(this@NewsApp, appDatabase)
        }
        offlineWorkScheduler.schedule()
        registerCurrentFcmToken()
    }

    private fun registerCurrentFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            DeviceTokenRegistrar.registerAsync(this, token)
        }
    }
}
