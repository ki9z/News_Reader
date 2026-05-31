package com.example.app_doc_bao.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.domain.ArticleStatus
import com.example.app_doc_bao.data.model.StatisticUiState
import com.example.app_doc_bao.data.repository.ArticleRepository
import com.example.app_doc_bao.data.repository.UserRepository
import kotlinx.coroutines.launch

class StatisticVM(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(StatisticUiState())
    val uiState: State<StatisticUiState> = _uiState

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {

            // Lấy toàn bộ dữ liệu từ Database thật
            val articles = articleRepository.getAllArticles()
            val users = userRepository.getAllUsers()

            // 1. Tính toán các con số tổng
            val totalUserCount = users.size
            val totalArticleCount = articles.size
            val totalViewCount = articles.sumOf { it.views }

            // 2. Tính toán các danh sách chi tiết
            val topArticlesList = articles
                .filter { it.status == ArticleStatus.PUBLISHED }
                .sortedByDescending { it.views }
                .take(3)

            val categoryMap = articles.groupingBy { it.category }.eachCount()
            val articleStatusMap = articles.groupingBy { it.status.name }.eachCount()

            val roleMap = users.groupingBy { it.role.name }.eachCount()
            val userStatusMap = users.groupingBy { it.status.name }.eachCount()

            // 3. Đẩy TẤT CẢ vào trong UiState
            _uiState.value = StatisticUiState(
                totalUsers = totalUserCount,
                totalArticles = totalArticleCount,
                totalViews = totalViewCount,
                topArticleModels = topArticlesList,
                articlesByCategory = categoryMap,
                articlesByStatus = articleStatusMap,
                usersByRole = roleMap,
                usersByStatus = userStatusMap
            )
        }
    }
}