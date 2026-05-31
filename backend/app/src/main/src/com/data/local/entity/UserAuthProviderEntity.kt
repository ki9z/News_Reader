package com.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_auth_providers",
    primaryKeys = ["userId", "providerCode"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class UserAuthProviderEntity(
    val userId: String,
    val providerCode: String,
    val providerUserId: String? = null,
    val maskedPhone: String? = null,
    val isPrimary: Boolean = false,
    val linkedAt: Long
)

