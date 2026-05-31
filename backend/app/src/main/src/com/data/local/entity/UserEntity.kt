package com.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String? = null,
    val fullName: String? = null,
    val username: String? = null,
    val passwordHash: String? = null,
    val role: String = "user",

    val status: String = "ACTIVE",

    val phone: String? = null,
    val avatarUrl: String? = null,
    val occupation: String? = null,
    val location: String? = null,
    val birthday: String? = null,
    val bio: String? = null,
    val interests: String? = null,
    val isSignedIn: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)