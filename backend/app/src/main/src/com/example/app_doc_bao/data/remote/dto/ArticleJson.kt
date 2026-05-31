package com.example.app_doc_bao.data.remote.dto

import kotlinx.serialization.Serializable
@Serializable
data class ArticleJson(
    val category: String,
    val source: String,
    val author: String,
    val title: String,
    val thumbnail: String,
    val time: String,
    val id: String,
    val url: String,
    val blocks: List<BlockJson> = emptyList()
)
