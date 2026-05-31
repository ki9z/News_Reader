package com.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.data.preferences.FollowPreferenceRepository
import com.data.repository.NewsRepository
import com.data.repository.ProfileRepository

class FollowingTopicsViewModelFactory(
    private val profileRepository: ProfileRepository,
    private val newsRepository: NewsRepository,
    private val followPreferenceRepository: FollowPreferenceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FollowingTopicsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FollowingTopicsViewModel(
                profileRepository = profileRepository,
                newsRepository = newsRepository,
                followPreferenceRepository = followPreferenceRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
