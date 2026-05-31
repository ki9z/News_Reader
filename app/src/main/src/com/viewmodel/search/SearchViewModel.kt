package com.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.model.Article
import com.data.repository.NewsRepository
import com.data.repository.ProfileRepository
import com.ui.model.NewsUiModel
import com.util.ArticleContentFormatter
import com.util.Constants
import com.util.NetworkResult
import com.util.PaginationHelper
import com.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: NewsRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TRENDING_QUERY_FALLBACK = "latest news"
    }

    private val _uiState = MutableStateFlow<UiState<List<NewsUiModel>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<NewsUiModel>>> = _uiState.asStateFlow()

    private val _trendingState = MutableStateFlow<UiState<List<NewsUiModel>>>(UiState.Idle)
    val trendingState: StateFlow<UiState<List<NewsUiModel>>> = _trendingState.asStateFlow()

    val recentSearchQueries: StateFlow<List<String>> = profileRepository
        .observeSearchHistoryQueries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val paginationHelper = PaginationHelper()
    private var currentQuery: String = ""
    private var currentItems: MutableList<NewsUiModel> = mutableListOf()
    private var searchRequestJob: Job? = null
    private var loadMoreRequestJob: Job? = null
    private var trendingRequestJob: Job? = null

    fun searchNews(query: String) {
        if (query.isBlank()) {
            _uiState.value = UiState.Idle
            return
        }

        searchRequestJob?.cancel()
        loadMoreRequestJob?.cancel()
        trendingRequestJob?.cancel()

        currentQuery = query
        paginationHelper.reset()
        _uiState.value = UiState.Loading

        searchRequestJob = viewModelScope.launch {
            profileRepository.recordSearch(query)

            when (val result = repository.searchNews(
                query = query,
                page = paginationHelper.getCurrentPage(),
                pageSize = Constants.DEFAULT_PAGE_SIZE
            )) {
                is NetworkResult.Success -> {
                    val items = result.data.map { it.toUiModel() }
                    currentItems = items.toMutableList()
                    paginationHelper.onLoadComplete(items.size)

                    _uiState.value = if (items.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(items)
                    }
                }

                is NetworkResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun loadMoreNews() {
        if (loadMoreRequestJob?.isActive == true) return
        val nextPage = paginationHelper.loadNext() ?: return
        if (currentQuery.isBlank()) {
            paginationHelper.onLoadError()
            return
        }

        paginationHelper.setLoading(true)

        loadMoreRequestJob = viewModelScope.launch {
            when (val result = repository.searchNews(
                query = currentQuery,
                page = nextPage,
                pageSize = Constants.DEFAULT_PAGE_SIZE
            )) {
                is NetworkResult.Success -> {
                    val newItems = result.data.map { it.toUiModel() }
                    if (newItems.isEmpty()) {
                        paginationHelper.onLoadComplete(0)
                    } else {
                        currentItems.addAll(newItems)
                        paginationHelper.onLoadComplete(newItems.size)
                        _uiState.value = UiState.Success(currentItems.toList())
                    }
                }

                is NetworkResult.Error -> {
                    paginationHelper.onLoadError()
                }
            }
        }
    }

    fun clearSearch() {
        searchRequestJob?.cancel()
        loadMoreRequestJob?.cancel()
        currentQuery = ""
        currentItems.clear()
        paginationHelper.reset()
        _uiState.value = UiState.Idle
    }

    fun loadTrendingNews() {
        trendingRequestJob?.cancel()
        _trendingState.value = UiState.Loading

        trendingRequestJob = viewModelScope.launch {
            val result = repository.getTopHeadlines(
                category = null,
                page = Constants.INITIAL_PAGE,
                pageSize = Constants.DEFAULT_PAGE_SIZE
            )

            when (result) {
                is NetworkResult.Success -> {
                    val items = result.data.map { it.toUiModel() }
                    if (items.isNotEmpty()) {
                        _trendingState.value = UiState.Success(items)
                    } else {
                        loadTrendingFallback()
                    }
                }
                is NetworkResult.Error -> {
                    loadTrendingFallback()
                }
            }
        }
    }

    private suspend fun loadTrendingFallback() {
        when (val fallback = repository.searchNews(
            query = TRENDING_QUERY_FALLBACK,
            page = Constants.INITIAL_PAGE,
            pageSize = Constants.DEFAULT_PAGE_SIZE
        )) {
            is NetworkResult.Success -> {
                val items = fallback.data.map { it.toUiModel() }
                _trendingState.value = if (items.isEmpty()) UiState.Empty else UiState.Success(items)
            }
            is NetworkResult.Error -> {
                _trendingState.value = UiState.Error(fallback.message)
            }
        }
    }

    private fun Article.toUiModel(): NewsUiModel {
        val sanitizedDescription = ArticleContentFormatter.sanitize(description)
        val sanitizedContent = ArticleContentFormatter.sanitize(content)
        val deduplicatedContent = ArticleContentFormatter.removeDuplicateSummary(
            description = sanitizedDescription,
            content = sanitizedContent
        )

        return NewsUiModel(
            title = ArticleContentFormatter.sanitize(title),
            description = sanitizedDescription.ifBlank { null },
            content = deduplicatedContent.ifBlank { null },
            imageUrl = urlToImage,
            articleUrl = url,
            sourceName = ArticleContentFormatter.sanitize(source?.name),
            author = ArticleContentFormatter.sanitize(author).ifBlank { null },
            publishedAt = publishedAt
        )
    }
}
