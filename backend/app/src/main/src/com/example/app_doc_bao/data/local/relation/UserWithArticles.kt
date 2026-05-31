package com.example.app_doc_bao.data.local.relation

import androidx.room.*
import com.example.app_doc_bao.data.local.entity.*

data class UserWithArticles(
    @Embedded val user: User,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ArticleUser::class,
            parentColumn = "userId",
            entityColumn = "articleId"
        )
    )
    val articles: List<Article>
)