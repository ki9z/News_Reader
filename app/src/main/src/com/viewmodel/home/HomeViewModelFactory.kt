package com.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.data.preferences.FollowPreferenceRepository
import com.data.repository.NewsRepository
import com.data.repository.ProfileRepository

class HomeViewModelFactory(
    private val repository: NewsRepository,
    private val profileRepository: ProfileRepository,
    private val followPreferenceRepository: FollowPreferenceRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, profileRepository, followPreferenceRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}