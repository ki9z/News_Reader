package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.ArticleBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleBlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ArticleBlockEntity>)

    @Query("DELETE FROM article_blocks WHERE articleUrl = :articleUrl")
    suspend fun deleteByArticleUrl(articleUrl: String)

    @Query("SELECT * FROM article_blocks WHERE articleUrl = :articleUrl ORDER BY blockOrder ASC, id ASC")
    fun observeByArticleUrl(articleUrl: String): Flow<List<ArticleBlockEntity>>
}


