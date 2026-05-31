package com.viewmodel.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.data.repository.NewsRepository
import com.data.repository.ProfileRepository
import com.data.settings.UserSettingsRepository

class DetailViewModelFactory(
    private val repository: NewsRepository,
    private val profileRepository: ProfileRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(repository, profileRepository, userSettingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
