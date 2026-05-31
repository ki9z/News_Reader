package com.viewmodel.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.model.Article
import com.data.model.LocalCity
import com.data.model.LocalCityCatalog
import com.data.model.Source
import com.data.repository.NewsRepository
import com.data.settings.LocalCityStatsStore
import com.ui.local.LocalUiState
import com.ui.model.NewsUiModel
import com.util.ArticleContentFormatter
import com.util.Constants
import com.util.NetworkResult
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocalViewModel(
    private val repository: NewsRepository,
    private val statsStore: LocalCityStatsStore
) : ViewModel() {

    private companion object {
        const val MIN_INITIAL_ARTICLE_COUNT = 20
        const val MAX_INITIAL_FETCH_PAGES = 4
    }

    data class LocalOption(
        val title: String,
        val query: String?,
        val country: String,
        val countryName: String,
        val continent: String,
        val searchTokens: Set<String> = emptySet()
    )

    private val defaultOptions = LocalCityCatalog.defaultCities().map { it.toLocalOption() }
    private val availableAdditionalOptions = LocalCityCatalog.additionalCities().map { it.toLocalOption() }

    private var selectedAdditionalTitles: Set<String> = emptySet()

    private val _locationOptions = MutableStateFlow(defaultOptions)
    val locationOptions: StateFlow<List<LocalOption>> = _locationOptions.asStateFlow()

    private var selectedOption: LocalOption = defaultOptions.first()
    private var currentPage = Constants.INITIAL_PAGE
    private var currentRawArticles: MutableList<Article> = mutableListOf()
    private var currentUiItems: MutableList<NewsUiModel> = mutableListOf()
    private var isLoadingMore = false
    private var canLoadMore = true
    private var currentOffline = false
    private var loadGeneration = 0

    private val _selectedTitle = MutableStateFlow(selectedOption.title)
    val selectedTitle: StateFlow<String> = _selectedTitle.asStateFlow()

    private val _uiState = MutableStateFlow<LocalUiState>(LocalUiState.Loading)
    val uiState: StateFlow<LocalUiState> = _uiState.asStateFlow()

    fun loadInitialNews() {
        loadNews(isRefresh = false)
    }

    fun refreshNews() {
        loadNews(isRefresh = true)
    }

    fun selectLocation(title: String) {
        val next = _locationOptions.value.firstOrNull { it.title == title } ?: return
        if (selectedOption.title == next.title) return
        selectedOption = next
        _selectedTitle.value = next.title
        loadInitialNews()
    }

    fun setCurrentLocation(city: String, countryCode: String) {
        val normalizedCity = city.trim()
        if (normalizedCity.isBlank()) return

        val normalizedCountry = countryCode.trim().ifBlank { "us" }.lowercase()
        val option = LocalOption(
            title = normalizedCity,
            query = normalizedCity,
            country = normalizedCountry,
            countryName = normalizedCountry.uppercase(Locale.ROOT),
            continent = "Custom",
            searchTokens = setOf(normalizedCity.lowercase(Locale.ROOT), normalizedCountry)
        )

        val remaining = _locationOptions.value.filterNot { it.title.equals(normalizedCity, ignoreCase = true) }
        _locationOptions.value = listOf(option) + remaining

        selectedOption = option
        _selectedTitle.value = option.title
        loadInitialNews()
    }

    fun getAvailableAdditionalCityOptions(): List<LocalOption> {
        return availableAdditionalOptions
    }

    fun searchAdditionalCityOptions(query: String): List<LocalOption> {
        val keyword = query.trim().lowercase(Locale.ROOT)
        val filtered = if (keyword.isBlank()) {
            availableAdditionalOptions
        } else {
            availableAdditionalOptions.filter { option ->
            option.searchTokens.any { token -> token.contains(keyword) }
            }
        }

        return filtered.sortedWith(
            compareByDescending<LocalOption> { statsStore.getHitCount(it.title) }
                .thenBy { it.title }
        )
    }

    fun getCityHitCount(cityTitle: String): Int = statsStore.getHitCount(cityTitle)

    fun getSelectedAdditionalCityTitles(): Set<String> {
        return selectedAdditionalTitles
    }

    fun updateAdditionalCities(titles: Set<String>) {
        val normalizedTitles = titles.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (normalizedTitles == selectedAdditionalTitles) return

        selectedAdditionalTitles = normalizedTitles
        val additionalSelected = availableAdditionalOptions.filter { option ->
            option.title in selectedAdditionalTitles
        }

        _locationOptions.value = (defaultOptions + additionalSelected)
            .distinctBy { it.title.lowercase(Locale.ROOT) }

        if (_locationOptions.value.none { it.title == selectedOption.title }) {
            selectedOption = _locationOptions.value.first()
            _selectedTitle.value = selectedOption.title
            loadInitialNews()
        }
    }

    fun loadMoreNews() {
        if (isLoadingMore || !canLoadMore || currentUiItems.isEmpty()) return

        isLoadingMore = true
        val nextPage = currentPage + 1

        viewModelScope.launch {
            when (val result = fetchNews(nextPage)) {
                is NetworkResult.Success -> {
                    val existingKeys = currentRawArticles.map { it.stableKey() }.toMutableSet()
                    val newArticles = result.data.filter { existingKeys.add(it.stableKey()) }

                    if (newArticles.isEmpty()) {
                        canLoadMore = false
                    } else {
                        currentPage = nextPage
                        currentRawArticles.addAll(newArticles)
                        currentUiItems.addAll(enrichArticles(newArticles))
                        canLoadMore = result.data.size >= Constants.DEFAULT_PAGE_SIZE
                        persistCache()
                        _uiState.value = LocalUiState.Success(currentUiItems.toList(), currentOffline)
                    }
                }
                is NetworkResult.Error -> {
                    canLoadMore = false
                }
            }
            isLoadingMore = false
        }
    }

    fun toggleBookmark(item: NewsUiModel) {
        val articleUrl = item.articleUrl.orEmpty()
        if (articleUrl.isBlank()) return

        viewModelScope.launch {
            if (item.isBookmarked) {
                repository.removeBookmark(articleUrl)
            } else {
                repository.saveBookmark(item.toArticle())
            }

            val updated = currentUiItems.map { existing ->
                if (existing.articleUrl == articleUrl) existing.copy(isBookmarked = !item.isBookmarked) else existing
            }
            currentUiItems = updated.toMutableList()
            if (currentRawArticles.isNotEmpty()) {
                _uiState.value = LocalUiState.Success(updated, currentOffline)
            }
        }
    }

    private fun loadNews(isRefresh: Boolean) {
        val requestId = ++loadGeneration
        currentPage = Constants.INITIAL_PAGE
        canLoadMore = true
        isLoadingMore = false

        val previousItems = currentUiItems.toList()
        if (isRefresh && previousItems.isNotEmpty()) {
            _uiState.value = LocalUiState.Refreshing(previousItems, currentOffline)
        } else {
            _uiState.value = LocalUiState.Loading
        }

        viewModelScope.launch {
            val dedupeKeys = mutableSetOf<String>()
            val collected = mutableListOf<Article>()
            var page = Constants.INITIAL_PAGE
            var lastLoadedPage = Constants.INITIAL_PAGE
            var reachedEnd = false
            var errorState: NetworkResult.Error? = null

            repeat(MAX_INITIAL_FETCH_PAGES) {
                if (requestId != loadGeneration || collected.size >= MIN_INITIAL_ARTICLE_COUNT || reachedEnd || errorState != null) {
                    return@repeat
                }

                when (val result = fetchNews(page)) {
                    is NetworkResult.Success -> {
                        val uniqueArticles = result.data.filter { article ->
                            dedupeKeys.add(article.stableKey())
                        }
                        if (uniqueArticles.isNotEmpty()) {
                            collected.addAll(uniqueArticles)
                        }

                        lastLoadedPage = page
                        reachedEnd = result.data.isEmpty() || result.data.size < Constants.DEFAULT_PAGE_SIZE
                        page++
                    }
                    is NetworkResult.Error -> {
                        errorState = result
                    }
                }
            }

            if (requestId != loadGeneration) return@launch

            if (collected.isNotEmpty()) {
                currentRawArticles = collected.toMutableList()
                currentUiItems = enrichArticles(collected).toMutableList()
                currentPage = lastLoadedPage
                canLoadMore = !reachedEnd
                currentOffline = false
                persistCache()
                statsStore.incrementHitCount(selectedOption.title, currentUiItems.size)
                _uiState.value = LocalUiState.Success(currentUiItems.toList(), false)
                return@launch
            }

            val cachedArticles = repository.getCachedLocalNews(selectedOption.query, selectedOption.country)
            if (cachedArticles.isNotEmpty()) {
                currentRawArticles = cachedArticles.toMutableList()
                currentUiItems = enrichArticles(cachedArticles).toMutableList()
                currentPage = Constants.INITIAL_PAGE
                canLoadMore = false
                currentOffline = true
                statsStore.incrementHitCount(selectedOption.title, currentUiItems.size)
                _uiState.value = LocalUiState.Success(currentUiItems.toList(), true)
                return@launch
            }

            currentRawArticles.clear()
            currentUiItems.clear()
            canLoadMore = false
            currentOffline = false

            if (errorState != null) {
                _uiState.value = LocalUiState.Error(mapErrorMessage(errorState))
            } else {
                _uiState.value = LocalUiState.Empty
            }
        }
    }

    private suspend fun fetchNews(page: Int): NetworkResult<List<Article>> {
        return repository.getLocalNews(
            locationQuery = selectedOption.query,
            country = selectedOption.country,
            page = page,
            pageSize = Constants.DEFAULT_PAGE_SIZE
        )
    }

    private suspend fun enrichArticles(articles: List<Article>): List<NewsUiModel> {
        return articles.map { article -> article.toUiModel() }
    }

    private suspend fun persistCache() {
        repository.saveCachedLocalNews(
            locationQuery = selectedOption.query,
            country = selectedOption.country,
            cityTitle = selectedOption.title,
            articles = currentRawArticles.toList()
        )
    }

    private fun mapErrorMessage(error: NetworkResult.Error?): String {
        return when (error?.type) {
            NetworkResult.ErrorType.NETWORK -> "Không có kết nối mạng"
            NetworkResult.ErrorType.RATE_LIMITED -> "Máy chủ đang bận, vui lòng thử lại sau"
            NetworkResult.ErrorType.SERVER -> "Không thể tải tin tức, vui lòng thử lại sau"
            NetworkResult.ErrorType.UNAUTHORIZED -> "Không thể tải tin tức, vui lòng thử lại sau"
            NetworkResult.ErrorType.CLIENT,
            NetworkResult.ErrorType.UNKNOWN,
            null -> "Không thể tải tin tức, vui lòng thử lại sau"
        }
    }

    private suspend fun Article.toUiModel(): NewsUiModel {
        val sanitizedDescription = ArticleContentFormatter.sanitize(description)
        val sanitizedContent = ArticleContentFormatter.sanitize(content)
        val deduplicatedContent = ArticleContentFormatter.removeDuplicateSummary(
            description = sanitizedDescription,
            content = sanitizedContent
        )

        val articleUrl = url.orEmpty()
        return NewsUiModel(
            title = ArticleContentFormatter.sanitize(title),
            description = sanitizedDescription.ifBlank { null },
            content = deduplicatedContent.ifBlank { null },
            imageUrl = urlToImage,
            articleUrl = articleUrl.ifBlank { null },
            sourceName = ArticleContentFormatter.sanitize(source?.name),
            author = ArticleContentFormatter.sanitize(author).ifBlank { null },
            publishedAt = publishedAt,
            isBookmarked = if (articleUrl.isBlank()) false else repository.isBookmarked(articleUrl)
        )
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

    private fun Article.stableKey(): String {
        return url?.trim().orEmpty().ifBlank { "${title.orEmpty()}|${publishedAt.orEmpty()}" }
    }

    private fun LocalCity.toLocalOption(): LocalOption {
        return LocalOption(
            title = title,
            query = query,
            country = countryCode,
            countryName = countryName,
            continent = continent,
            searchTokens = buildSet {
                add(title.lowercase(Locale.ROOT))
                add(query.lowercase(Locale.ROOT))
                add(countryCode.lowercase(Locale.ROOT))
                add(countryName.lowercase(Locale.ROOT))
                add(continent.lowercase(Locale.ROOT))
                aliases.forEach { add(it.lowercase(Locale.ROOT)) }
                countryHints.forEach { add(it.lowercase(Locale.ROOT)) }
            }
        )
    }
}

