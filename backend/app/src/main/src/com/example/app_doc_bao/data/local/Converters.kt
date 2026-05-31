package com.example.app_doc_bao.data.local

import com.example.app_doc_bao.data.local.entity.*
import androidx.room.TypeConverter
import com.example.app_doc_bao.data.domain.*
class Converters {
    @TypeConverter
    fun fromStatus(status: ArticleStatus): String {
        return status.name.lowercase()
    }

    @TypeConverter
    fun toStatus(value: String): ArticleStatus {
        return ArticleStatus.valueOf(value.uppercase())
    }
}
