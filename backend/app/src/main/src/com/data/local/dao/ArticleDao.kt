package com.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles")
    suspend fun getAllBookmarks(): List<ArticleEntity>

    @Upsert
    suspend fun insertBookmark(article: ArticleEntity)

    @Query("DELETE FROM articles WHERE url = :url")
    suspend fun deleteBookmark(url: String)

    @Query("SELECT COUNT(*) FROM articles WHERE url = :url")
    suspend fun isBookmarked(url: String): Int

    @Query("SELECT * FROM articles WHERE url IN (:urls)")
    suspend fun getArticlesByUrls(urls: List<String>): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE url = :url LIMIT 1")
    suspend fun getArticleByUrl(url: String): ArticleEntity?

    @Transaction
    suspend fun upsertArticlePreservingStats(article: ArticleEntity) {
        val existing = getArticleByUrl(article.url)
        val entity = if (existing == null) {
            article
        } else {
            article.copy(
                view = existing.view,
                status = existing.status,
                createdAt = existing.createdAt
            )
        }
        insertBookmark(entity)
    }


    // ==========================================
    // --- PHẦN CODE BỔ SUNG CỦA BẠN (ADMIN & STATS) ---
    // ==========================================

    @Upsert
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("SELECT * FROM articles ORDER BY createdAt DESC")
    suspend fun getAllArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE status = 'PUBLISHED' ORDER BY publishedAt DESC")
    fun observePublishedArticles(): Flow<List<ArticleEntity>>

    @Query("DELETE FROM articles WHERE url = :url")
    suspend fun deleteArticleByUrl(url: String)

    // Các hàm phục vụ StatisticUiState và AdminHomeUiState
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getTotalArticlesCount(): Int

    @Query("SELECT SUM(`view`) FROM articles")
    suspend fun getTotalViewsCount(): Long?

    @Query("SELECT COUNT(*) FROM articles WHERE createdAt >= :startOfDayTimestamp")
    suspend fun getTodayArticlesCount(startOfDayTimestamp: Long): Int

    @Query("UPDATE articles SET status = :status WHERE url = :url")
    suspend fun updateArticleStatus(url: String, status: String)

    @Query("UPDATE articles SET `view` = `view` + 1 WHERE url = :url")
    suspend fun incrementViewCount(url: String)

    @androidx.room.Transaction
    @Query("SELECT * FROM articles ORDER BY createdAt DESC")
    fun observeAllArticlesWithDetails(): Flow<List<com.data.local.relation.ArticleWithDetails>>

    @androidx.room.Transaction
    @Query("SELECT * FROM articles")
    suspend fun getAllArticlesWithDetails(): List<com.data.local.relation.ArticleWithDetails>

    @androidx.room.Transaction
    @Query("SELECT * FROM articles WHERE url = :url")
    suspend fun getArticleWithDetails(url: String): com.data.local.relation.ArticleWithDetails?

    @Query("UPDATE articles SET `view` = COALESCE(`view`, 0) + 1 WHERE url = :articleUrl")
    suspend fun incrementArticleView(articleUrl: String)
}
