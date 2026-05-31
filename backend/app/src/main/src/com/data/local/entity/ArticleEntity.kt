package com.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["sourceId"])
    ]
)
data class ArticleEntity(
    @PrimaryKey
    val url: String,

    val city: String?,
    val sourceId: String?,
    val sourceName: String?,
    val author: String?,
    val title: String?,
    val description: String?,
    val urlToImage: String?,
    val publishedAt: String?,
    val content: String?,
    val categoryId: String,
    val view: Long? = 0,
    val status: String = "PUBLISHED",
    val createdAt: Long = System.currentTimeMillis()
)
