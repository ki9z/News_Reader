package com.example.app_doc_bao.data.remote

import kotlinx.serialization.json.Json
object JsonParser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }
}
