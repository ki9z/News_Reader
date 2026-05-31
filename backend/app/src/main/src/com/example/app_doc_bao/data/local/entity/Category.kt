package com.example.app_doc_bao.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
