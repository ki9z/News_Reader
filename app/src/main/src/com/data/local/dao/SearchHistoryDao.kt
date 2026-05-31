package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.UserSearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UserSearchHistoryEntity)

    @Query("DELETE FROM user_search_history WHERE userId = :userId")
    suspend fun clearByUserId(userId: String)

    @Query("SELECT * FROM user_search_history WHERE userId = :userId ORDER BY searchedAt DESC LIMIT :limit")
    fun observeByUserId(userId: String, limit: Int = 20): Flow<List<UserSearchHistoryEntity>>
}

