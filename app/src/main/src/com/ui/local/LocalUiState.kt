package com.ui.local

import com.ui.model.NewsUiModel

sealed class LocalUiState {
    object Loading : LocalUiState()
    data class Refreshing(val articles: List<NewsUiModel>, val isOffline: Boolean = false) : LocalUiState()
    data class Success(val articles: List<NewsUiModel>, val isOffline: Boolean = false) : LocalUiState()
    object Empty : LocalUiState()
    data class Error(val message: String) : LocalUiState()
}

