package com.data.remote.client

import android.content.Context
import android.content.SharedPreferences
import com.util.Constants
import com.util.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

/**
 * Centralized protection layer for NewsAPI calls.
 *
 * It keeps request pacing, a soft daily quota, and temporary backoff windows after
 * rate-limit/server/network failures. This prevents several screens from burning
 * through a free NewsAPI quota by repeatedly refreshing, searching, or paginating.
 */
object NewsApiRequestGuard {
    private const val PREFS_NAME = "news_api_request_guard"
    private const val KEY_DAY = "day"
    private const val KEY_DAILY_COUNT = "daily_count"
    private const val KEY_PAUSE_UNTIL = "pause_until"
    private const val KEY_LAST_REQUEST_AT = "last_request_at"

    private val softDailyLimit: Int
        get() = Constants.NEWS_API_DAILY_LIMIT.coerceAtLeast(1)
    private const val MIN_INTERVAL_BETWEEN_REQUESTS_MS = 450L
    private const val RATE_LIMIT_BACKOFF_MS = 15 * 60 * 1000L
    private const val SERVER_BACKOFF_MS = 60 * 1000L
    private const val NETWORK_BACKOFF_MS = 20 * 1000L

    private val mutex = Mutex()
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        resetDailyCountIfNeeded(System.currentTimeMillis())
    }

    suspend fun acquirePermit(requestKey: String): NetworkResult.Error? {
        while (true) {
            var waitMs = 0L

            mutex.lock()
            try {
                val now = System.currentTimeMillis()
                resetDailyCountIfNeeded(now)

                val pauseUntil = prefs?.getLong(KEY_PAUSE_UNTIL, 0L) ?: 0L
                if (pauseUntil > now) {
                    return rateLimitedError(
                        "NewsAPI đang tạm nghỉ để tránh vượt giới hạn. Thử lại sau khoảng ${formatRemaining(pauseUntil - now)}."
                    )
                }

                val dailyCount = prefs?.getInt(KEY_DAILY_COUNT, 0) ?: 0
                if (dailyCount >= softDailyLimit) {
                    return rateLimitedError(
                        "Đã chạm ngưỡng bảo vệ quota NewsAPI hôm nay ($softDailyLimit request). App sẽ ưu tiên cache/offline."
                    )
                }

                val lastRequestAt = prefs?.getLong(KEY_LAST_REQUEST_AT, 0L) ?: 0L
                waitMs = MIN_INTERVAL_BETWEEN_REQUESTS_MS - (now - lastRequestAt)
                if (waitMs <= 0) {
                    val countAfterIncrement = dailyCount + 1
                    prefs?.edit()
                        ?.putInt(KEY_DAILY_COUNT, countAfterIncrement)
                        ?.putLong(KEY_LAST_REQUEST_AT, now)
                        ?.putString("last_request_key", requestKey)
                        ?.apply()
                    return null
                }
            } finally {
                mutex.unlock()
            }

            delay(waitMs)
        }
    }

    fun recordHttpResponse(code: Int) {
        when (code) {
            429 -> pauseFor(RATE_LIMIT_BACKOFF_MS)
            in 500..599 -> pauseFor(SERVER_BACKOFF_MS)
        }
    }

    fun recordException(exception: Exception) {
        when (exception) {
            is SocketTimeoutException,
            is IOException -> pauseFor(NETWORK_BACKOFF_MS)
        }
    }

    fun remainingDailyQuota(): Int {
        resetDailyCountIfNeeded(System.currentTimeMillis())
        val count = prefs?.getInt(KEY_DAILY_COUNT, 0) ?: 0
        return (softDailyLimit - count).coerceAtLeast(0)
    }

    private fun pauseFor(durationMs: Long) {
        val now = System.currentTimeMillis()
        val currentPauseUntil = prefs?.getLong(KEY_PAUSE_UNTIL, 0L) ?: 0L
        val nextPauseUntil = maxOf(currentPauseUntil, now + durationMs)
        prefs?.edit()?.putLong(KEY_PAUSE_UNTIL, nextPauseUntil)?.apply()
    }

    private fun resetDailyCountIfNeeded(now: Long) {
        val currentDay = dayKey(now)
        val savedDay = prefs?.getString(KEY_DAY, null)
        if (savedDay != currentDay) {
            prefs?.edit()
                ?.putString(KEY_DAY, currentDay)
                ?.putInt(KEY_DAILY_COUNT, 0)
                ?.apply()
        }
    }

    private fun dayKey(timestamp: Long): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(timestamp))
    }

    private fun rateLimitedError(message: String): NetworkResult.Error {
        return NetworkResult.Error(
            message = message,
            code = 429,
            type = NetworkResult.ErrorType.RATE_LIMITED
        )
    }

    private fun formatRemaining(ms: Long): String {
        val minutes = ceil(ms / 60_000.0).toInt().coerceAtLeast(1)
        return if (minutes >= 60) {
            val hours = minutes / 60
            val remainMinutes = minutes % 60
            if (remainMinutes == 0) "${hours} giờ" else "${hours} giờ ${remainMinutes} phút"
        } else {
            "$minutes phút"
        }
    }
}
