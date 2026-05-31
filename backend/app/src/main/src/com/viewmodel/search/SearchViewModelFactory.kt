package com.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.data.repository.NewsRepository
import com.data.repository.ProfileRepository

class SearchViewModelFactory(
    private val repository: NewsRepository,
    private val profileRepository: ProfileRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository, profileRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}