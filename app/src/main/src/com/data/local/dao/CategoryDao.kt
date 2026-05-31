package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.CategoryEntity
import com.data.local.entity.UserFollowedCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT name FROM categories ORDER BY name ASC")
    suspend fun getAllCategoryNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(items: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeAllActiveCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun follow(entity: UserFollowedCategoryEntity)

    @Query("DELETE FROM user_followed_categories WHERE userId = :userId AND categoryId = :categoryId")
    suspend fun unfollow(userId: String, categoryId: String)

    @Query(
        """
        SELECT c.*
        FROM categories c
        INNER JOIN user_followed_categories f ON f.categoryId = c.id
        WHERE f.userId = :userId
        ORDER BY c.sortOrder ASC, c.name ASC
        """
    )
    fun observeFollowedCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT categoryId FROM user_followed_categories WHERE userId = :userId")
    fun observeFollowedCategoryIds(userId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("DELETE FROM user_followed_categories WHERE userId = :userId")
    suspend fun clearFollowedByUserId(userId: String)

    @Query(
        """
        SELECT c.*
        FROM categories c
        INNER JOIN user_followed_categories f ON f.categoryId = c.id
        WHERE f.userId = :userId
        ORDER BY c.sortOrder ASC, c.name ASC
        """
    )
    suspend fun getFollowedCategories(userId: String): List<CategoryEntity>

}

