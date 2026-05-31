package com.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BlockJson(
    val type: String,
    val data: String? = null,
    val url: String? = null,
    val caption: String? = null
)
