package com.example.app_doc_bao.data.model

data class StatisticUiState(
    val totalUsers: Int = 0,
    val totalArticles: Int = 0,
    val totalViews: Long = 0,

    val topArticleModels: List<ArticleModel> = emptyList(),
    val articlesByCategory: Map<String, Int> = emptyMap(),
    val articlesByStatus: Map<String, Int> = emptyMap(),
    val usersByRole: Map<String, Int> = emptyMap(),
    val usersByStatus: Map<String, Int> = emptyMap()
)