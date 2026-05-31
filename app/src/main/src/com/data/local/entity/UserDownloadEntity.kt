package com.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_downloads",
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
        Index(value = ["userId", "articleUrl"], unique = true),
        Index(value = ["userId", "downloadedAt"]),
        Index(value = ["status"]),
        Index(value = ["articleUrl"])
    ]
)
data class UserDownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val articleUrl: String,
    val downloadedAt: Long,
    val localPath: String? = null,
    val fileSizeBytes: Long? = null,
    val status: String = STATUS_QUEUED,
    val expiresAt: Long? = null
) {
    companion object {
        const val STATUS_QUEUED = "queued"
        const val STATUS_DOWNLOADING = "downloading"
        const val STATUS_DONE = "done"
        const val STATUS_FAILED = "failed"
    }
}
