package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.ArticleEntity
import com.data.local.entity.UserBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: UserBookmarkEntity)

    @Query("DELETE FROM user_bookmarks WHERE userId = :userId AND articleUrl = :articleUrl")
    suspend fun delete(userId: String, articleUrl: String)

    @Query("DELETE FROM user_bookmarks WHERE userId = :userId")
    suspend fun clearByUserId(userId: String)

    @Query("SELECT COUNT(*) FROM user_bookmarks WHERE userId = :userId AND articleUrl = :articleUrl")
    suspend fun count(userId: String, articleUrl: String): Int

    @Query(
        """
        SELECT a.*
        FROM articles a
        INNER JOIN user_bookmarks b ON b.articleUrl = a.url
        WHERE b.userId = :userId
        ORDER BY b.savedAt DESC
        """
    )
    suspend fun getBookmarkedArticles(userId: String): List<ArticleEntity>

    @Query(
        """
        SELECT a.*
        FROM articles a
        INNER JOIN user_bookmarks b ON b.articleUrl = a.url
        WHERE b.userId = :userId
        ORDER BY b.savedAt DESC
        """
    )
    fun observeBookmarkedArticles(userId: String): Flow<List<ArticleEntity>>
}

