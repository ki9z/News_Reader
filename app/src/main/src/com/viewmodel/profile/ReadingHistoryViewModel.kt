package com.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.repository.ProfileRepository
import com.ui.model.NewsUiModel
import com.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReadingHistoryViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    enum class RangeFilter { TODAY, DAYS_7, DAYS_30 }

    private val allItems = MutableStateFlow<List<NewsUiModel>>(emptyList())
    private val query = MutableStateFlow("")
    private val range = MutableStateFlow(RangeFilter.DAYS_7)

    val uiState: StateFlow<UiState<List<NewsUiModel>>> = combine(allItems, query, range) { items, keyword, selected ->
        val now = System.currentTimeMillis()
        val startTime = when (selected) {
            RangeFilter.TODAY -> now - 24L * 60 * 60 * 1000
            RangeFilter.DAYS_7 -> now - 7L * 24 * 60 * 60 * 1000
            RangeFilter.DAYS_30 -> now - 30L * 24 * 60 * 60 * 1000
        }

        val filtered = items
            .filter { (it.eventTimeMillis ?: 0L) >= startTime }
            .filter {
                keyword.isBlank() ||
                    it.title.contains(keyword, ignoreCase = true) ||
                    it.sourceName.contains(keyword, ignoreCase = true)
            }

        if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )

    val continueReading: StateFlow<List<NewsUiModel>> = allItems
        .combine(range) { items, selected ->
            val now = System.currentTimeMillis()
            val startTime = when (selected) {
                RangeFilter.TODAY -> now - 24L * 60 * 60 * 1000
                RangeFilter.DAYS_7 -> now - 7L * 24 * 60 * 60 * 1000
                RangeFilter.DAYS_30 -> now - 30L * 24 * 60 * 60 * 1000
            }
            items.filter { it.canResume && (it.eventTimeMillis ?: 0L) >= startTime }.take(5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        observeHistory()
    }

    fun setQuery(value: String) {
        query.value = value.trim()
    }

    fun setRange(value: RangeFilter) {
        range.value = value
    }

    fun removeItem(item: NewsUiModel) {
        val id = item.itemId ?: return
        viewModelScope.launch {
            profileRepository.removeHistoryItem(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            profileRepository.clearReadingHistory()
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            profileRepository.bootstrap()
            profileRepository.observeReadingHistoryItems().collect { items ->
                allItems.value = items
            }
        }
    }
}

