package com.ui.model

data class NewsItem(
    val id: String,
    val title: String,
    val source: String,
    val thumbnailUrl: String,
    val category: String,
    val description: String? = null,
    val content: String? = null,
    val articleUrl: String? = null,
    val author: String? = null,
    val publishedAt: String? = null
)

