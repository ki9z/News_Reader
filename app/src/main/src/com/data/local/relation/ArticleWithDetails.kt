package com.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.data.local.entity.ArticleBlockEntity
import com.data.local.entity.ArticleEntity
import com.data.local.entity.CategoryEntity

data class ArticleWithDetails(
    @Embedded
    val article: ArticleEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,

    @Relation(
        parentColumn = "url",
        entityColumn = "articleUrl"  
    )
    val blocks: List<ArticleBlockEntity>
)