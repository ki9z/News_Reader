package com.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_news_cache")
data class LocalNewsCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val cityTitle: String,
    val countryCode: String,
    val locationQuery: String?,
    val articlesJson: String,
    val updatedAt: Long
)

