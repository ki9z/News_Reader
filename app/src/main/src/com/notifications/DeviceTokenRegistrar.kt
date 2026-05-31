package com.notifications

import android.content.Context
import android.util.Log
import com.BuildConfig
import com.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object DeviceTokenRegistrar {
    private const val TAG = "DeviceTokenRegistrar"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerAsync(context: Context, token: String, topics: List<String> = listOf("breaking-news")) {
        if (!Constants.USES_BACKEND_PROXY || token.isBlank()) return
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                registerToken(appContext, token, topics)
            }.onFailure { error ->
                Log.w(TAG, "Không đăng ký được FCM token lên backend: ${error.message}")
            }
        }
    }

    private fun registerToken(context: Context, token: String, topics: List<String>) {
        val body = JSONObject()
            .put("token", token)
            .put("platform", "android")
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("language", Locale.getDefault().language)
            .put("topics", JSONArray(topics))

        val url = URL(Constants.BASE_URL.trimEnd('/') + "/api/devices/register")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (Constants.BACKEND_APP_TOKEN.isNotBlank()) {
                setRequestProperty("X-App-Token", Constants.BACKEND_APP_TOKEN)
            }
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        connection.disconnect()
        if (responseCode !in 200..299) {
            error("Backend returned HTTP $responseCode")
        }
    }
}
