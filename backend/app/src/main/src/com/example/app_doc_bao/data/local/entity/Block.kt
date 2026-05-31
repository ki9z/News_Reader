package com.example.app_doc_bao.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
@Entity(
    tableName = "blocks",
    foreignKeys = [
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("articleId")]
)
data class Block(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val articleId: Long,
    val type: String,
    val content: String?,
    val imageUrl: String?,
    val caption: String?,
    val position: Long?
)
