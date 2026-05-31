package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.UserDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: UserDownloadEntity)

    @Query("DELETE FROM user_downloads WHERE userId = :userId AND articleUrl = :articleUrl")
    suspend fun delete(userId: String, articleUrl: String)

    @Query("SELECT * FROM user_downloads WHERE userId = :userId ORDER BY downloadedAt DESC")
    fun observeByUserId(userId: String): Flow<List<UserDownloadEntity>>

    @Query("DELETE FROM user_downloads WHERE userId = :userId")
    suspend fun clearByUserId(userId: String)

    @Query("SELECT * FROM user_downloads WHERE status = :status ORDER BY downloadedAt ASC")
    suspend fun getByStatus(status: String): List<UserDownloadEntity>

    @Query("SELECT * FROM user_downloads WHERE userId = :userId AND status = :status ORDER BY downloadedAt DESC")
    suspend fun getByUserAndStatus(userId: String, status: String): List<UserDownloadEntity>

    @Query("SELECT * FROM user_downloads WHERE userId = :userId AND articleUrl = :articleUrl LIMIT 1")
    suspend fun getByUserAndArticle(userId: String, articleUrl: String): UserDownloadEntity?

    @Query("SELECT * FROM user_downloads WHERE userId = :userId AND downloadedAt < :cutoffMillis")
    suspend fun getOlderThan(userId: String, cutoffMillis: Long): List<UserDownloadEntity>
}

