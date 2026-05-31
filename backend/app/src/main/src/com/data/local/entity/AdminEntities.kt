package com.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Admin User Entity
 * Stores admin credentials
 */
@Entity(tableName = "admin_users")
data class AdminUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val passwordHash: String,
    val fullName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long? = null
)

/**
 * OTP Token Entity
 * Stores OTP for phone verification
 */
@Entity(tableName = "otp_tokens")
data class OtpTokenEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val otpCode: String,
    val expiresAt: Long,
    val attempts: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun isAttemptsExceeded(): Boolean = attempts >= 5
}

/**
 * Admin Article Entity
 * Extended article for admin management
 */
@Entity(tableName = "admin_articles")
data class AdminArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val content: String,
    val imageUrl: String = "",
    val category: String = "",
    val createdBy: Int,
    val isPublished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

