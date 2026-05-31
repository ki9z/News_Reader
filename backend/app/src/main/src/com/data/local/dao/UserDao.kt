package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE isSignedIn = 1 LIMIT 1")
    fun observeSignedInUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isSignedIn = 1 LIMIT 1")
    suspend fun getSignedInUser(): UserEntity?

    @Query("UPDATE users SET isSignedIn = CASE WHEN id = :userId THEN 1 ELSE 0 END, updatedAt = :updatedAt")
    suspend fun markSingleSignedIn(userId: String, updatedAt: Long)

    @Query("UPDATE users SET isSignedIn = 0, updatedAt = :updatedAt")
    suspend fun signOutAll(updatedAt: Long)

    @Query("SELECT role FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserRole(userId: String): String?

    // =======================
    // --- (ADMIN & STATS) ---
    // =======================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getTotalUsers(): Int

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users WHERE createdAt >= :startOfDayTimestamp")
    suspend fun getNewUsersTodayCount(startOfDayTimestamp: Long): Int

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsersForAdmin(): Flow<List<UserEntity>>

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)
}

