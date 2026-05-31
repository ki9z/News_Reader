package com.viewmodel.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.model.Article
import com.data.repository.NewsRepository
import com.data.repository.ProfileRepository
import com.data.settings.UserSettings
import com.data.settings.UserSettingsRepository
import com.ui.model.NewsUiModel
import com.util.ArticleContentFormatter
import com.util.Constants
import com.util.NetworkResult
import com.util.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailViewModel(
    private val repository: NewsRepository,
    private val profileRepository: ProfileRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _readingHistoryId = MutableStateFlow<Long?>(null)
    val readingHistoryId: StateFlow<Long?> = _readingHistoryId.asStateFlow()

    private val _relatedArticles = MutableStateFlow<UiState<List<NewsUiModel>>>(UiState.Idle)
    val relatedArticles: StateFlow<UiState<List<NewsUiModel>>> = _relatedArticles.asStateFlow()

    private val _downloadState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val downloadState: StateFlow<UiState<String>> = _downloadState.asStateFlow()

    val settings: StateFlow<UserSettings> = userSettingsRepository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    private var hasIncrementedView = false
    private var loadedRelatedForUrl: String? = null
    private var pendingFinish: ReadingFinish? = null
    private var hasSubmittedFinish = false

    fun incrementArticleView(articleUrl: String) {
        if (hasIncrementedView) return

        val safeUrl = articleUrl.trim()
        if (safeUrl.isBlank()) return

        hasIncrementedView = true

        viewModelScope.launch {
            repository.incrementArticleView(safeUrl)
        }
    }

    fun recordOriginalArticleOpen(article: Article) {
        if (hasIncrementedView) return

        val safeUrl = article.url.orEmpty().trim()
        if (safeUrl.isBlank()) return

        hasIncrementedView = true

        viewModelScope.launch {
            repository.recordOriginalArticleOpen(article.copy(url = safeUrl))
        }
    }

    fun checkBookmarkStatus(articleUrl: String) {
        val safeUrl = articleUrl.trim()
        if (safeUrl.isBlank()) return

        viewModelScope.launch {
            _isBookmarked.value = repository.isBookmarked(safeUrl)
        }
    }

    fun toggleBookmark(articleUrl: String, article: Article) {
        val safeUrl = articleUrl.trim()
        if (safeUrl.isBlank()) return

        viewModelScope.launch {
            if (_isBookmarked.value) {
                repository.removeBookmark(safeUrl)
                _isBookmarked.value = false
            } else {
                repository.saveBookmark(article)
                _isBookmarked.value = true
            }
        }
    }

    fun recordReading(article: Article, fromScreen: String) {
        viewModelScope.launch {
            val settings = userSettingsRepository.userSettingsFlow.first()
            if (!settings.trackReadingHistory) return@launch

            val id = profileRepository.recordReading(
                article = article,
                fromScreen = fromScreen.takeIf { it.isNotBlank() }
            )

            _readingHistoryId.value = id

            val finish = pendingFinish
            if (id != null && finish != null && !hasSubmittedFinish) {
                finishReadingInternal(id, finish.readSeconds, finish.completionPercent)
                pendingFinish = null
            }
        }
    }

    fun finishReading(readSeconds: Int, completionPercent: Int) {
        val finish = ReadingFinish(
            readSeconds = readSeconds.coerceAtLeast(1),
            completionPercent = completionPercent.coerceIn(1, 100)
        )
        val historyId = _readingHistoryId.value

        if (historyId == null) {
            pendingFinish = finish
            return
        }

        if (hasSubmittedFinish) return
        finishReadingInternal(historyId, finish.readSeconds, finish.completionPercent)
    }

    fun downloadArticle(article: Article) {
        val articleUrl = article.url.orEmpty().trim()
        if (articleUrl.isBlank()) {
            _downloadState.value = UiState.Error("invalid_download")
            return
        }

        _downloadState.value = UiState.Loading

        viewModelScope.launch {
            runCatching {
                profileRepository.addDownload(article)
            }.onSuccess {
                _downloadState.value = UiState.Success("saved")
            }.onFailure { error ->
                _downloadState.value = UiState.Error(error.message ?: "download_failed")
            }
        }
    }


    fun consumeDownloadMessage() {
        if (_downloadState.value is UiState.Success || _downloadState.value is UiState.Error) {
            _downloadState.value = UiState.Idle
        }
    }

    fun loadRelatedArticles(article: Article) {
        val articleUrl = article.url.orEmpty().trim()
        if (loadedRelatedForUrl == articleUrl) return
        loadedRelatedForUrl = articleUrl

        val query = buildRelatedQuery(article)
        if (query.isBlank()) {
            _relatedArticles.value = UiState.Empty
            return
        }

        _relatedArticles.value = UiState.Loading

        viewModelScope.launch {
            val localRelated = profileRepository.getLocalRelatedArticles(article, limit = 6)
            if (localRelated.isNotEmpty()) {
                _relatedArticles.value = UiState.Success(localRelated)
                return@launch
            }

            when (val result = repository.searchNews(
                query = query,
                sortBy = "publishedAt",
                page = Constants.INITIAL_PAGE,
                pageSize = 8
            )) {
                is NetworkResult.Success -> {
                    val currentUrl = article.url.orEmpty().trim()
                    val items = result.data
                        .filter { candidate -> candidate.url.orEmpty().trim() != currentUrl }
                        .distinctBy { candidate ->
                            candidate.url.orEmpty().ifBlank { candidate.title.orEmpty() }
                        }
                        .take(6)
                        .map { candidate -> candidate.toUiModel() }

                    _relatedArticles.value = if (items.isEmpty()) UiState.Empty else UiState.Success(items)
                }

                is NetworkResult.Error -> {
                    _relatedArticles.value = UiState.Error(result.message)
                }
            }
        }
    }

    private fun finishReadingInternal(historyId: Long, readSeconds: Int, completionPercent: Int) {
        if (historyId <= 0L) return
        hasSubmittedFinish = true

        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                profileRepository.finishReading(
                    historyId = historyId,
                    readSeconds = readSeconds,
                    completionPercent = completionPercent
                )
            }
        }
    }

    private fun buildRelatedQuery(article: Article): String {
        article.source?.name
            ?.trim()
            ?.takeIf { it.length in 3..80 }
            ?.let { sourceName -> return "\"$sourceName\"" }

        return ArticleContentFormatter.sanitize(article.title)
            .split(Regex("\\s+"))
            .map { it.trim(' ', ',', '.', ':', ';', '!', '?', '\"', '\'', '(', ')') }
            .filter { token -> token.length >= 4 }
            .take(6)
            .joinToString(" ")
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

    private data class ReadingFinish(
        val readSeconds: Int,
        val completionPercent: Int
    )
}
