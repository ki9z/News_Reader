package com.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "article_blocks",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["url"],
            childColumns = ["articleUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["articleUrl", "blockOrder"])
    ]
)
data class ArticleBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val articleUrl: String, // Đổi từ articleId (Long) sang articleUrl (String)
    val type: String,       // "text", "image", v.v.
    val content: String? = null,
    val imageUrl: String? = null,
    val caption: String? = null,
    val blockOrder: Int = 0, // Thay thế cho 'position' của bạn
    val metadataJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

