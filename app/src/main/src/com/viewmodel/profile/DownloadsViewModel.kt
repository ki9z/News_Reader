package com.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.repository.ProfileRepository
import com.data.preferences.DownloadQuality
import com.data.preferences.OfflineDownloadPreferences
import com.data.preferences.OfflineDownloadSettings
import com.data.work.OfflineWorkScheduler
import com.ui.model.NewsUiModel
import com.util.UiState
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val profileRepository: ProfileRepository,
    private val offlineDownloadPreferences: OfflineDownloadPreferences,
    private val offlineWorkScheduler: OfflineWorkScheduler
) : ViewModel() {

    enum class StatusFilter { ALL, DOWNLOADED, FAILED, EXPIRED }

    data class DownloadHeader(
        val totalDownloaded: Int = 0,
        val storageUsedText: String = "0 B"
    )

    private val allItems = MutableStateFlow<List<NewsUiModel>>(emptyList())
    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow(StatusFilter.ALL)

    val offlineSettings: StateFlow<OfflineDownloadSettings> = offlineDownloadPreferences.state

    val uiState: StateFlow<UiState<List<NewsUiModel>>> = combine(allItems, query, statusFilter) { items, keyword, filter ->
        val filteredByStatus = when (filter) {
            StatusFilter.ALL -> items
            StatusFilter.DOWNLOADED -> items.filter { it.status.equals("done", ignoreCase = true) }
            StatusFilter.FAILED -> items.filter { it.status.equals("failed", ignoreCase = true) }
            StatusFilter.EXPIRED -> items.filter { it.status.equals("expired", ignoreCase = true) }
        }

        val filtered = filteredByStatus.filter {
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

    val headerState: StateFlow<DownloadHeader> = allItems
        .combine(statusFilter) { items, _ ->
            val totalDone = items.count { it.status.equals("done", ignoreCase = true) }
            val bytes = items.sumOf { it.fileSizeBytes ?: 0L }
            DownloadHeader(totalDownloaded = totalDone, storageUsedText = formatSize(bytes))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DownloadHeader()
        )

    init {
        observeDownloads()
    }

    fun setQuery(value: String) {
        query.value = value.trim()
    }

    fun setStatusFilter(value: StatusFilter) {
        statusFilter.value = value
    }

    fun setWifiOnly(value: Boolean) {
        offlineDownloadPreferences.updateWifiOnly(value)
        offlineWorkScheduler.schedule()
    }

    fun setAutoDownloadBookmarks(value: Boolean) {
        offlineDownloadPreferences.updateAutoDownloadBookmarks(value)
        if (value) offlineWorkScheduler.schedule() else offlineWorkScheduler.cancelAutoDownload()
    }

    fun setDownloadQuality(value: DownloadQuality) {
        offlineDownloadPreferences.updateQuality(value)
    }

    fun setAutoDeleteDays(value: Int) {
        offlineDownloadPreferences.updateAutoDeleteDays(value)
        offlineWorkScheduler.schedule()
    }

    fun syncBookmarksNow() {
        viewModelScope.launch {
            profileRepository.autoDownloadBookmarkedArticles(limit = 20)
        }
    }

    fun removeDownload(articleUrl: String) {
        viewModelScope.launch {
            profileRepository.removeDownload(articleUrl)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            profileRepository.clearDownloads()
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            profileRepository.bootstrap()
            profileRepository.observeDownloadedItems().collect { items ->
                allItems.value = items
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }
}

