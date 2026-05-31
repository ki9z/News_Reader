package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.LocalNewsCacheEntity

@Dao
interface LocalNewsCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalNewsCacheEntity)

    @Query("SELECT * FROM local_news_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByCacheKey(cacheKey: String): LocalNewsCacheEntity?

    @Query("DELETE FROM local_news_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)

    @Query("DELETE FROM local_news_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM local_news_cache")
    suspend fun countAll(): Int
}

