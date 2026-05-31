package com.example.app_doc_bao.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.app_doc_bao.data.domain.*
@Entity(
    "articles",
    foreignKeys = [
        ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Source::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value=["remoteId"], unique=true),
        Index(value=["categoryId"]),
        Index(value = ["sourceId"])
    ]
)
data class Article(
    @PrimaryKey(autoGenerate= true) val id:Long = 0,
    val remoteId: String,
    val title: String,
    val thumbnail: String?,
    val author: String?,
    val sourceId: Long?,
    val categoryId: Long,
    val publishAt: Long,
    val createdAt: Long,
    val view: Long?,
    val status: ArticleStatus = ArticleStatus.PUBLISHED,
    val url: String
)
