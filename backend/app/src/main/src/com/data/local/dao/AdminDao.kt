package com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.data.local.entity.AdminArticleEntity
import com.data.local.entity.AdminUserEntity
import com.data.local.entity.OtpTokenEntity

/**
 * Admin User DAO
 */
@Dao
interface AdminUserDao {
    @Insert
    suspend fun insert(user: AdminUserEntity): Long

    @Update
    suspend fun update(user: AdminUserEntity)

    @Query("SELECT * FROM admin_users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getByPhoneNumber(phoneNumber: String): AdminUserEntity?

    @Query("SELECT * FROM admin_users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): AdminUserEntity?

    @Query("SELECT * FROM admin_users WHERE isActive = 1")
    suspend fun getAllActive(): List<AdminUserEntity>
}

/**
 * OTP Token DAO
 */
@Dao
interface OtpTokenDao {
    @Insert
    suspend fun insert(token: OtpTokenEntity): Long

    @Query("SELECT * FROM otp_tokens WHERE phoneNumber = :phoneNumber ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByPhoneNumber(phoneNumber: String): OtpTokenEntity?

    @Query("DELETE FROM otp_tokens WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())

    @Query("UPDATE otp_tokens SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Int)
}

/**
 * Admin Article DAO
 */
@Dao
interface AdminArticleDao {
    @Insert
    suspend fun insert(article: AdminArticleEntity): Long

    @Update
    suspend fun update(article: AdminArticleEntity)

    @Query("DELETE FROM admin_articles WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM admin_articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): AdminArticleEntity?

    @Query("SELECT * FROM admin_articles ORDER BY createdAt DESC")
    suspend fun getAllArticles(): List<AdminArticleEntity>

    @Query("SELECT * FROM admin_articles WHERE isPublished = 1 ORDER BY createdAt DESC")
    suspend fun getPublishedArticles(): List<AdminArticleEntity>

    @Query("SELECT * FROM admin_articles WHERE createdBy = :adminId ORDER BY createdAt DESC")
    suspend fun getArticlesByAdmin(adminId: Int): List<AdminArticleEntity>

    @Query("SELECT COUNT(*) FROM admin_articles")
    suspend fun getTotalCount(): Int
}

