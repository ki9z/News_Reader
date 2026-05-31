package com.viewmodel.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.model.Article
import com.data.model.Source
import com.data.repository.NewsRepository
import com.ui.model.NewsUiModel
import com.util.ArticleContentFormatter
import com.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<NewsUiModel>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<NewsUiModel>>> = _uiState.asStateFlow()

    fun loadBookmarks() {
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            val bookmarks = repository.getBookmarks()
            val items = bookmarks.map { it.toUiModel(true) }

            _uiState.value = if (items.isEmpty()) {
                UiState.Empty
            } else {
                UiState.Success(items)
            }
        }
    }

    fun removeBookmark(articleUrl: String) {
        val safeUrl = articleUrl.trim()
        if (safeUrl.isBlank()) return

        viewModelScope.launch {
            repository.removeBookmark(safeUrl)
            loadBookmarks()
        }
    }

    fun restoreBookmark(item: NewsUiModel) {
        val articleUrl = item.articleUrl.orEmpty().trim()
        if (articleUrl.isBlank()) return

        viewModelScope.launch {
            repository.saveBookmark(
                Article(
                    source = Source(id = null, name = item.sourceName),
                    author = item.author,
                    title = item.title,
                    description = item.description,
                    url = articleUrl,
                    urlToImage = item.imageUrl,
                    publishedAt = item.publishedAt,
                    content = item.content
                )
            )
            loadBookmarks()
        }
    }

    private fun Article.toUiModel(isBookmarked: Boolean = false): NewsUiModel {
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
            publishedAt = publishedAt,
            isBookmarked = isBookmarked
        )
    }
}