package com.util
import java.text.SimpleDateFormat
import java.util.Locale

fun String.toTimestamp(): Long {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.parse(this)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}
