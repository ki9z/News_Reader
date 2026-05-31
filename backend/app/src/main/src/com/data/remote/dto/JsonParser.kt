package com.data.remote.dto

import kotlinx.serialization.json.Json
object JsonParser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }
}
