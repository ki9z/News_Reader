package com.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.data.repository.ProfileRepository
import com.data.preferences.OfflineDownloadPreferences
import com.data.work.OfflineWorkScheduler

class DownloadsViewModelFactory(
    private val profileRepository: ProfileRepository,
    private val offlineDownloadPreferences: OfflineDownloadPreferences,
    private val offlineWorkScheduler: OfflineWorkScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DownloadsViewModel(profileRepository, offlineDownloadPreferences, offlineWorkScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

