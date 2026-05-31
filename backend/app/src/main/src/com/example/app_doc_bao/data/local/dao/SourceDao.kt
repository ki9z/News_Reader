package com.example.app_doc_bao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.app_doc_bao.data.local.entity.Source
@Dao
interface SourceDao {

    @Insert
    suspend fun insert(source: Source) : Long

    @Query("SELECT * FROM sources")
    suspend fun getAll(): List<Source>

    @Query("SELECT * FROM sources WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Source?

    @Delete
    suspend fun delete(source: Source)
}
