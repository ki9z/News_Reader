package com.example.app_doc_bao.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.app_doc_bao.data.local.entity.Article
import com.example.app_doc_bao.data.local.entity.Block
import com.example.app_doc_bao.data.local.entity.Category
import com.example.app_doc_bao.data.local.entity.Source // THÊM IMPORT NÀY

data class ArticleWithDetails(
    @Embedded
    val article: Article,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?,

    @Relation(
        parentColumn = "sourceId",
        entityColumn = "id"
    )
    val source: Source?,

    @Relation(
        parentColumn = "id",
        entityColumn = "articleId"
    )
    val blocks: List<Block>
)