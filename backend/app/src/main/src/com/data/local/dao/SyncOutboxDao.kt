package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncOutboxEntity): Long

    @Query("SELECT * FROM sync_outbox WHERE status = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(status: String = SyncOutboxEntity.STATUS_PENDING, limit: Int = 100): List<SyncOutboxEntity>

    @Query(
        """
        UPDATE sync_outbox
        SET status = :status,
            retryCount = :retryCount,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: String, retryCount: Int, lastError: String?, updatedAt: Long)
}

