package com.example.app_doc_bao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.app_doc_bao.data.local.entity.*
@Dao
interface BlockDao {

    @Insert
    suspend fun insert(block: Block) : Long

    @Insert
    suspend fun insertAll(blocks: List<Block>) : List<Long>

    @Query("SELECT * FROM blocks WHERE articleId = :articleId ORDER BY position ASC")
    suspend fun getByArticleId(articleId: Long): List<Block>

    @Query("DELETE FROM blocks WHERE articleId = :articleId")
    suspend fun deleteByArticleId(articleId: Long)
}
