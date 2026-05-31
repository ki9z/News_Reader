package com.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleJson(
    val category: String = "",
    val source: String = "",
    val author: String? = null,
    val title: String = "",
    val thumbnail: String? = null,
    val time: String? = null,
    val id: String? = null,
    val url: String = "",
    val blocks: List<BlockJson> = emptyList()
)