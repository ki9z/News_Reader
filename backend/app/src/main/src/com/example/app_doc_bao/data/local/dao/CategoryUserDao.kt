package com.example.app_doc_bao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Delete
import com.example.app_doc_bao.data.local.entity.*
import androidx.room.OnConflictStrategy
import com.example.app_doc_bao.data.local.relation.UserWithCategories
@Dao
interface CategoryUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoryUser: CategoryUser): Long

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserWithCategories(userId: Long): UserWithCategories?

    @Delete
    suspend fun delete(categoryUser: CategoryUser)
}
