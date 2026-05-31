package com.example.app_doc_bao.data.model

import com.example.app_doc_bao.data.domain.BlockType

data class BlockModel(
    val id: Int,
    val type: BlockType,
    val content: String,
    val imageUrl: String,
    val caption: String
)