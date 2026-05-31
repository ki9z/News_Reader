package com.viewmodel.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.data.repository.NewsRepository
import com.data.settings.LocalCityStatsStore

class LocalViewModelFactory(
    private val repository: NewsRepository,
    private val statsStore: LocalCityStatsStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocalViewModel(repository, statsStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

