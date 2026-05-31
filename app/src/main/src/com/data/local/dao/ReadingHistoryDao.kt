package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.local.entity.ReadingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ReadingHistoryEntity): Long

    @Query(
        """
        UPDATE reading_history
        SET closedAt = :closedAt,
            readSeconds = :readSeconds,
            completionPercent = :completionPercent
        WHERE id = :historyId
        """
    )
    suspend fun finish(historyId: Long, closedAt: Long, readSeconds: Int, completionPercent: Int)

    @Query("SELECT * FROM reading_history WHERE userId = :userId ORDER BY openedAt DESC")
    fun observeByUserId(userId: String): Flow<List<ReadingHistoryEntity>>

    @Query("DELETE FROM reading_history WHERE id = :historyId")
    suspend fun deleteById(historyId: Long)

    @Query("DELETE FROM reading_history WHERE userId = :userId")
    suspend fun clearByUserId(userId: String)
}

