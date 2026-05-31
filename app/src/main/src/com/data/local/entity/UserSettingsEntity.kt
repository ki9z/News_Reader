package com.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_settings",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserSettingsEntity(
    @PrimaryKey
    val userId: String,
    val darkModeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
    val languageCode: String = "en",
    val textSize: String = "M",
    val updatedAt: Long
)

