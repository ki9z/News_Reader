package com.ui.model

data class NewsUiModel(
    val title: String,
    val description: String?,
    val content: String?,
    val imageUrl: String?,
    val articleUrl: String?,
    val sourceName: String,
    val author: String?,
    val publishedAt: String?,
    val isBookmarked: Boolean = false,
    val itemId: Long? = null,
    val eventTimeMillis: Long? = null,
    val completionPercent: Int? = null,
    val readSeconds: Int? = null,
    val status: String? = null,
    val fileSizeBytes: Long? = null,
    val canResume: Boolean = false
) {
    val hasValidUrl: Boolean
        get() = articleUrl?.trim()?.startsWith("http", ignoreCase = true) == true
}