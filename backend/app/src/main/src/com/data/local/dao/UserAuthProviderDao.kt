package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.UserAuthProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAuthProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserAuthProviderEntity)

    @Query("DELETE FROM user_auth_providers WHERE userId = :userId AND providerCode = :providerCode")
    suspend fun delete(userId: String, providerCode: String)

    @Query("SELECT * FROM user_auth_providers WHERE userId = :userId")
    fun observeByUserId(userId: String): Flow<List<UserAuthProviderEntity>>
}

