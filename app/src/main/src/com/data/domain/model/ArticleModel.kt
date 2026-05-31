package com.data.domain.model

import com.data.domain.ArticleStatus

data class ArticleModel(
    val id: String,
    val remoteId: String,
    val title: String,
    val thumbnail: String?,
    val category: String,
    val author: String?,
    val source: String?,
    val publishDate: String,
    val status: ArticleStatus,
    val views: Long,
    val url: String,
    val contentBlocks: List<BlockModel>
)