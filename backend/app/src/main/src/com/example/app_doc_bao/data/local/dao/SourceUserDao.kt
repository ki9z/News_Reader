package com.example.app_doc_bao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Delete
import com.example.app_doc_bao.data.local.entity.SourceUser
import androidx.room.OnConflictStrategy
import com.example.app_doc_bao.data.local.relation.UserWithSource

@Dao
interface SourceUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sourceUser: SourceUser): Long

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserWithSource(userId: Int): UserWithSource?

    @Delete
    suspend fun delete(sourceUser: SourceUser)
}
