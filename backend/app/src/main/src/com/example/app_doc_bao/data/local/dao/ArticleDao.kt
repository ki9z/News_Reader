package com.example.app_doc_bao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import com.example.app_doc_bao.data.local.entity.*
import com.example.app_doc_bao.data.local.relation.ArticleWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(article: Article) : Long

    @Query("SELECT * FROM articles WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): Article?

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Article?

    @Query("SELECT * FROM articles WHERE categoryId = :categoryId")
    suspend fun getByCategory(categoryId: Long): List<Article>

    @Transaction
    @Query("SELECT * FROM articles WHERE id = :articleId")
    suspend fun getArticleWithDetails(articleId: Long): ArticleWithDetails?

    @Transaction
    @Query("SELECT * FROM articles")
    suspend fun getAllArticlesWithDetails(): List<ArticleWithDetails>

    @Query("SELECT * FROM articles")
    suspend fun getAll(): List<Article>

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getTotalArticles(): Int

    @Query("SELECT SUM(`view`) FROM articles")
    suspend fun getTotalViews(): Long?
    @Update
    suspend fun update(article: Article)

    @Transaction
    @Query("SELECT * FROM articles")
    fun getAllArticlesWithDetailsFlow(): Flow<List<ArticleWithDetails>>
}
