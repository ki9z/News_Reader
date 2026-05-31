package com.example.app_doc_bao.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val fullname: String,
    val username: String,
    val password: String,
    val role: String,
    val status: String,
    val createdAt: Long,
    val phone: String?
)
