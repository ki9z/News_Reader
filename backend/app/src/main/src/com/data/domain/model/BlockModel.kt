package com.data.domain.model

import com.data.domain.BlockType

data class BlockModel(
    val id: Long,
    val type: BlockType,
    val content: String,
    val imageUrl: String,
    val caption: String
)