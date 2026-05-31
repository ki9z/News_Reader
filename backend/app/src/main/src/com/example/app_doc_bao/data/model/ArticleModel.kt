package com.example.app_doc_bao.data.model

import com.example.app_doc_bao.data.domain.ArticleStatus

data class ArticleModel(
    val id: Long,
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