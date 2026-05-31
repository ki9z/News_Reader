package com.example.app_doc_bao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.app_doc_bao.data.local.entity.*
@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: Category) : Long

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Category?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Category?
    @Delete
    suspend fun delete(category: Category)
}
