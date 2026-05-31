package com.example.app_doc_bao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
@Entity(
    tableName = "article_user",
    primaryKeys = ["articleId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("articleId"), Index("userId")]
)
data class ArticleUser(
    val articleId: Long,
    val userId: Long,
    val savedAt: Long? = null
)
