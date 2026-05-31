package com.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_bookmarks",
    primaryKeys = ["userId", "articleUrl"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["url"],
            childColumns = ["articleUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["articleUrl"])
    ]
)
data class UserBookmarkEntity(
    val userId: String,
    val articleUrl: String,
    val savedAt: Long,
    val note: String? = null,
    val tags: String? = null
)

