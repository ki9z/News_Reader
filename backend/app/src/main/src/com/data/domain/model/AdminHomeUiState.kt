package com.data.domain.model

data class AdminHomeUiState(
    val fullname: String = "",
    val email: String = "",
    val totalArticles: String = "0",
    val totalUsers: String = "0",
    val totalViews: String = "0",
    val todayArticles: String = "0",
    val newUsers: String = "0"
)