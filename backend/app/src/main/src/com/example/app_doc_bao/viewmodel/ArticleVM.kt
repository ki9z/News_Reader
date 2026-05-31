package com.example.app_doc_bao.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.domain.ArticleStatus
import com.example.app_doc_bao.data.model.ArticleModel
import com.example.app_doc_bao.data.remote.dto.BlockJson
import com.example.app_doc_bao.data.repository.ArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArticleVM(
    private val repository: ArticleRepository
) : ViewModel() {

    var articleModelList by mutableStateOf(listOf<ArticleModel>())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedCategory by mutableStateOf("Tất cả")
        private set

    var selectedSource by mutableStateOf("Tất cả")
        private set

    var selectedStatus by mutableStateOf("Tất cả")
        private set

    var errorMessage by mutableStateOf("")
        private set

    var categories by mutableStateOf(listOf("Tất cả"))
        private set

    init {
        loadArticlesFlow()
        loadCategories()
    }

    private fun loadArticlesFlow() {
        viewModelScope.launch {
            repository.getAllArticlesFlow()
                .flowOn(Dispatchers.IO)
                .collect { newArticles ->
                    articleModelList = newArticles
                }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbCategories = repository.getAllCategoryNames()
            withContext(Dispatchers.Main) {
                categories = listOf("Tất cả") + dbCategories
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateCategory(category: String) {
        selectedCategory = category
    }

    fun updateSource(source: String) {
        selectedSource = source
    }

    fun updateStatus(status: String) {
        selectedStatus = status
    }

    fun getFilteredArticles(): List<ArticleModel> {
        return articleModelList.filter { article ->
            val title = article.title ?: ""
            val author = article.author ?: ""
            val source = article.source ?: ""
            val category = article.category ?: ""

            // 1. Tìm theo Tiêu đề hoặc Tác giả (Search Query)
            val matchSearch = title.contains(searchQuery, ignoreCase = true) ||
                    author.contains(searchQuery, ignoreCase = true)

            // 2. Lọc theo Danh mục
            val matchCategory = selectedCategory == "Tất cả" || category == selectedCategory

            // 3. Lọc theo Nguồn
            val matchSource = selectedSource == "Tất cả" || source == selectedSource

            // 4. Lọc theo Trạng thái
            val matchStatus = selectedStatus == "Tất cả" || article.status.name == selectedStatus

            matchSearch && matchCategory && matchSource && matchStatus
        }
    }

    fun addArticle(
        remoteId: String = "MANUAL_${System.currentTimeMillis()}",
        title: String,
        thumbnail: String?,
        author: String,
        source: String,
        category: String,
        timeString: String,
        url: String,
        blocks: List<BlockJson>,
        status: ArticleStatus,
        onSuccess: () -> Unit
    ) {
        if (title.isBlank() || blocks.isEmpty()) {
            errorMessage = "Vui lòng nhập Tiêu đề và nội dung bài viết"
            return
        }
        errorMessage = ""

        viewModelScope.launch {
            try {
                repository.addArticle(
                    remoteId = remoteId,
                    title = title,
                    thumbnail = thumbnail,
                    author = author,
                    source = source,
                    category = category,
                    timeString = timeString,
                    url = url,
                    blocks = blocks,
                    status = status
                )
                loadCategories()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Lỗi khi thêm bài báo: ${e.message}"
            }
        }
    }

    fun updateArticle(
        id: Long,
        remoteId: String,
        title: String,
        thumbnail: String?,
        author: String,
        source: String,
        category: String,
        timeString: String,
        url: String,
        blocks: List<BlockJson>,
        status: ArticleStatus,
        onSuccess: () -> Unit
    ) {
        if (title.isBlank() || blocks.isEmpty()) {
            errorMessage = "Tiêu đề và nội dung không được để trống"
            return
        }
        errorMessage = ""

        viewModelScope.launch {
            try {
                repository.updateArticle(
                    id = id,
                    remoteId = remoteId,
                    title = title,
                    thumbnail = thumbnail,
                    author = author,
                    source = source,
                    category = category,
                    timeString = timeString,
                    url = url,
                    blocks = blocks,
                    status = status
                )
                loadCategories()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Lỗi khi cập nhật: ${e.message}"
            }
        }
    }

    fun deleteArticle(id: Long) {
        viewModelScope.launch {
            repository.deleteArticle(id)
        }
    }

    fun clearError() {
        errorMessage = ""
    }
}