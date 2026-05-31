package com.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.model.Article
import com.data.model.Source
import com.data.preferences.FollowPreferenceRepository
import com.data.repository.NewsRepository
import com.data.repository.ProfileRepository
import com.ui.model.NewsUiModel
import com.util.ArticleContentFormatter
import com.util.Constants
import com.util.NetworkResult
import com.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: NewsRepository,
    private val profileRepository: ProfileRepository,
    private val followPreferenceRepository: FollowPreferenceRepository
) : ViewModel() {

    enum class HomeFeedTab {
        FEATURED,
        LOCAL,
        FOLLOWING
    }

    data class HomeFilterState(
        val countryCode: String = "us",
        val category: String = Constants.CATEGORY_GENERAL,
        val sortBy: String = "publishedAt",
        val sourceId: String? = null
    )

    private companion object {
        const val REFRESH_PAGE_START = 2
        const val REFRESH_PAGE_MAX = 3
        const val REFRESH_DEBOUNCE_MS = 1200L
        const val REFRESH_COOLDOWN_MS = 30_000L
        const val REFRESH_CACHE_TTL_MS = 30_000L
        const val MAX_REFRESH_PAGES_PER_TAP = 1
    }

    private data class RefreshCacheEntry(
        val page: Int,
        val items: List<NewsUiModel>,
        val fetchedAtMillis: Long
    )

    private val _uiState = MutableStateFlow<UiState<List<NewsUiModel>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<NewsUiModel>>> = _uiState.asStateFlow()

    private val _refreshNotice = MutableStateFlow<RefreshNotice>(RefreshNotice.None)
    val refreshNotice: StateFlow<RefreshNotice> = _refreshNotice.asStateFlow()

    private val _loadMoreError = MutableStateFlow<String?>(null)
    val loadMoreError: StateFlow<String?> = _loadMoreError.asStateFlow()

    private val _filterState = MutableStateFlow(HomeFilterState())
    val filterState: StateFlow<HomeFilterState> = _filterState.asStateFlow()

    private var currentPage = Constants.INITIAL_PAGE
    private var refreshProbePage = REFRESH_PAGE_START
    private var selectedTab: HomeFeedTab = HomeFeedTab.FEATURED
    private var currentItems: MutableList<NewsUiModel> = mutableListOf()
    private var isLoadingMore = false
    private var canLoadMore = true
    private var isRefreshing = false
    private var lastRefreshTapAt = 0L
    private var lastNetworkRefreshAt = 0L
    private var refreshCache: RefreshCacheEntry? = null

    enum class RefreshNotice {
        None,
        Updated,
        NoNew,
        CoolingDown,
        Debounced
    }

    fun loadInitialNews() {
        currentPage = Constants.INITIAL_PAGE
        canLoadMore = true
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            when (val result = fetchNews(currentPage)) {
                is NetworkResult.Success -> {
                    val items = decorateWithBookmarks(result.data.map { it.toUiModel() })
                    currentItems = items.toMutableList()

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

    fun refreshNews() {
        val now = System.currentTimeMillis()

        if (now - lastRefreshTapAt < REFRESH_DEBOUNCE_MS) {
            _refreshNotice.value = RefreshNotice.Debounced
            return
        }

        lastRefreshTapAt = now

        if (isRefreshing) {
            _refreshNotice.value = RefreshNotice.Debounced
            return
        }

        val cached = refreshCache
        val cacheAlive = cached != null && now - cached.fetchedAtMillis <= REFRESH_CACHE_TTL_MS
        val coolingDown = now - lastNetworkRefreshAt < REFRESH_COOLDOWN_MS

        if (coolingDown && cacheAlive) {
            applyRefreshedItems(cached!!.items, cached.page)
            _refreshNotice.value = RefreshNotice.CoolingDown
            return
        }

        _refreshNotice.value = RefreshNotice.None
        _uiState.value = UiState.Loading
        isRefreshing = true

        viewModelScope.launch {
            val existingKeys = currentItems.map { it.stableKey() }.toSet()
            val candidatePages = buildRefreshCandidatePages()
                .take(MAX_REFRESH_PAGES_PER_TAP)

            var firstNonEmpty: Pair<Int, List<NewsUiModel>>? = null
            var updatedSelection: Pair<Int, List<NewsUiModel>>? = null

            for (page in candidatePages) {
                when (val result = fetchNews(page)) {
                    is NetworkResult.Success -> {
                        val items = decorateWithBookmarks(result.data.map { it.toUiModel() })
                        if (items.isEmpty()) continue

                        if (firstNonEmpty == null) {
                            firstNonEmpty = page to items
                        }

                        val hasUnseen = items.any { it.stableKey() !in existingKeys }

                        if (existingKeys.isEmpty() || hasUnseen) {
                            updatedSelection = page to items
                            break
                        }
                    }

                    is NetworkResult.Error -> {
                        if (firstNonEmpty == null) {
                            if (cacheAlive) {
                                applyRefreshedItems(cached!!.items, cached.page)
                                _refreshNotice.value = RefreshNotice.CoolingDown
                                isRefreshing = false
                                return@launch
                            }

                            _uiState.value = UiState.Error(result.message)
                            isRefreshing = false
                            return@launch
                        }
                    }
                }
            }

            val chosen = updatedSelection ?: firstNonEmpty

            if (chosen == null) {
                if (cacheAlive) {
                    applyRefreshedItems(cached!!.items, cached.page)
                    _refreshNotice.value = RefreshNotice.NoNew
                } else {
                    _uiState.value = UiState.Empty
                }

                isRefreshing = false
                return@launch
            }

            applyRefreshedItems(chosen.second, chosen.first)

            refreshCache = RefreshCacheEntry(
                page = chosen.first,
                items = chosen.second,
                fetchedAtMillis = System.currentTimeMillis()
            )

            lastNetworkRefreshAt = System.currentTimeMillis()

            _refreshNotice.value = if (updatedSelection != null) {
                RefreshNotice.Updated
            } else {
                RefreshNotice.NoNew
            }

            refreshProbePage = if (chosen.first >= REFRESH_PAGE_MAX) {
                REFRESH_PAGE_START
            } else {
                chosen.first + 1
            }

            isRefreshing = false
        }
    }

    private fun applyRefreshedItems(items: List<NewsUiModel>, page: Int) {
        if (items.isEmpty()) {
            _uiState.value = UiState.Empty
            return
        }

        currentPage = page
        currentItems = items.toMutableList()
        canLoadMore = items.size >= Constants.DEFAULT_PAGE_SIZE
        _uiState.value = UiState.Success(items)
    }

    private fun buildRefreshCandidatePages(): List<Int> {
        val pages = mutableListOf<Int>()
        var cursor = refreshProbePage

        repeat(REFRESH_PAGE_MAX - REFRESH_PAGE_START + 1) {
            pages += cursor
            cursor = if (cursor >= REFRESH_PAGE_MAX) {
                REFRESH_PAGE_START
            } else {
                cursor + 1
            }
        }

        if (!pages.contains(Constants.INITIAL_PAGE)) {
            pages += Constants.INITIAL_PAGE
        }

        return pages.distinct()
    }

    fun selectTab(tab: HomeFeedTab) {
        if (selectedTab == tab) return

        selectedTab = tab
        refreshCache = null
        refreshProbePage = REFRESH_PAGE_START
        loadInitialNews()
    }

    fun applyFilters(filterState: HomeFilterState) {
        if (_filterState.value == filterState) return

        _filterState.value = filterState
        refreshCache = null
        refreshProbePage = REFRESH_PAGE_START
        loadInitialNews()
    }

    fun currentFilterState(): HomeFilterState = _filterState.value

    fun toggleBookmark(item: NewsUiModel) {
        val articleUrl = item.articleUrl?.trim().orEmpty()
        if (articleUrl.isBlank()) return

        viewModelScope.launch {
            if (item.isBookmarked) {
                repository.removeBookmark(articleUrl)
            } else {
                repository.saveBookmark(item.toArticle())
            }

            currentItems = currentItems.map { current ->
                if (current.articleUrl == articleUrl) {
                    current.copy(isBookmarked = !item.isBookmarked)
                } else {
                    current
                }
            }.toMutableList()

            _uiState.value = UiState.Success(currentItems.toList())
        }
    }

    fun loadMoreNews() {
        if (isLoadingMore || !canLoadMore) return

        isLoadingMore = true
        currentPage++

        viewModelScope.launch {
            when (val result = fetchNews(currentPage)) {
                is NetworkResult.Success -> {
                    val newItems = decorateWithBookmarks(result.data.map { it.toUiModel() })

                    if (newItems.isEmpty()) {
                        canLoadMore = false
                    } else {
                        currentItems.addAll(newItems)
                        _uiState.value = UiState.Success(currentItems.toList())
                    }

                    if (result.data.size < Constants.DEFAULT_PAGE_SIZE) {
                        canLoadMore = false
                    }
                }

                is NetworkResult.Error -> {
                    currentPage--
                    _loadMoreError.value = result.message
                }
            }

            isLoadingMore = false
        }
    }

    private suspend fun fetchNews(page: Int): NetworkResult<List<Article>> {
        val filters = _filterState.value

        return when (selectedTab) {
            HomeFeedTab.FEATURED -> fetchFeaturedNews(filters, page)

            HomeFeedTab.LOCAL -> repository.searchNews(
                query = cityQueryForCountry(filters.countryCode),
                sortBy = filters.sortBy,
                sources = filters.sourceId,
                page = page,
                pageSize = Constants.DEFAULT_PAGE_SIZE
            )

            HomeFeedTab.FOLLOWING -> fetchForYouNews(filters, page)
        }
    }

    private suspend fun fetchForYouNews(
        filters: HomeFilterState,
        page: Int
    ): NetworkResult<List<Article>> {
        val selectedSource = filters.sourceId?.trim().takeUnless { it.isNullOrBlank() }
        if (selectedSource != null) {
            return repository.getTopHeadlines(
                category = null,
                country = filters.countryCode,
                sources = selectedSource,
                page = page,
                pageSize = Constants.DEFAULT_PAGE_SIZE
            )
        }

        val followedSources = followPreferenceRepository.getFollowedSourceIds()
            .filter { it.isNotBlank() }
            .take(20)

        if (followedSources.isNotEmpty()) {
            return repository.getTopHeadlines(
                category = null,
                country = filters.countryCode,
                sources = followedSources.joinToString(","),
                page = page,
                pageSize = Constants.DEFAULT_PAGE_SIZE
            )
        }

        val followedKeywordQuery = followPreferenceRepository.getFollowedKeywords()
            .filter { it.isNotBlank() }
            .take(8)
            .joinToString(" OR ") { keyword ->
                val safe = keyword.replace("\"", "")
                if (safe.contains(" ")) "\"$safe\"" else safe
            }

        val topicQuery = profileRepository.getFollowingQuery()
        val query = listOf(followedKeywordQuery, topicQuery)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" OR ")
            .ifBlank { "technology OR business OR science OR health OR sports" }

        return repository.searchNews(
            query = query,
            sortBy = filters.sortBy,
            sources = null,
            page = page,
            pageSize = Constants.DEFAULT_PAGE_SIZE
        )
    }

    private suspend fun fetchFeaturedNews(
        filters: HomeFilterState,
        page: Int
    ): NetworkResult<List<Article>> {
        val sourceScoped = filters.sourceId?.trim().takeUnless { it.isNullOrBlank() }

        return if (sourceScoped != null || filters.sortBy == "publishedAt") {
            repository.getTopHeadlines(
                category = if (sourceScoped != null) null else filters.category,
                country = filters.countryCode,
                sources = sourceScoped,
                page = page,
                pageSize = Constants.DEFAULT_PAGE_SIZE
            )
        } else {
            repository.searchNews(
                query = filters.category,
                sortBy = filters.sortBy,
                sources = filters.sourceId,
                page = page,
                pageSize = Constants.DEFAULT_PAGE_SIZE
            )
        }
    }

    private fun cityQueryForCountry(countryCode: String): String {
        return when (countryCode.lowercase()) {
            "vn" -> "Hanoi"
            "jp" -> "Tokyo"
            "kr" -> "Seoul"
            "gb" -> "London"
            "fr" -> "Paris"
            "de" -> "Berlin"
            "au" -> "Sydney"
            else -> "New York"
        }
    }

    fun clearLoadMoreError() {
        _loadMoreError.value = null
    }

    private suspend fun decorateWithBookmarks(items: List<NewsUiModel>): List<NewsUiModel> {
        if (items.isEmpty()) return items

        val bookmarkedUrls = repository.getBookmarks()
            .mapNotNull { it.url }
            .toSet()

        return items.map { item ->
            val isBookmarked = item.articleUrl != null && bookmarkedUrls.contains(item.articleUrl)
            item.copy(isBookmarked = isBookmarked)
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

    private fun NewsUiModel.stableKey(): String {
        return articleUrl?.trim().orEmpty().ifBlank {
            "$title|${publishedAt.orEmpty()}"
        }
    }

    private fun NewsUiModel.toArticle(): Article {
        return Article(
            source = Source(id = null, name = sourceName),
            author = author,
            title = title,
            description = description,
            url = articleUrl,
            urlToImage = imageUrl,
            publishedAt = publishedAt,
            content = content
        )
    }
}