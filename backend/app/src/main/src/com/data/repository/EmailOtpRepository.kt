package com.data.repository

import android.util.Log
import com.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class EmailOtpRepository {

    suspend fun sendOTP(email: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            postOtpRequest(email.trim(), "email")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gửi OTP: ${e.message}")
            false
        }
    }

    suspend fun verifyOTP(email: String, otp: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            postOtpVerify(email.trim(), "email", otp.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xác thực OTP: ${e.message}")
            false
        }
    }

    private fun postOtpRequest(identifier: String, channel: String): Boolean {
        if (!Constants.USES_BACKEND_PROXY) return false
        val body = JSONObject()
            .put("identifier", identifier)
            .put("channel", channel)
        return postJson("api/auth/otp/request", body)
    }

    private fun postOtpVerify(identifier: String, channel: String, code: String): Boolean {
        if (!Constants.USES_BACKEND_PROXY) return false
        val body = JSONObject()
            .put("identifier", identifier)
            .put("channel", channel)
            .put("code", code)
        return postJson("api/auth/otp/verify", body)
    }

    private fun postJson(path: String, body: JSONObject): Boolean {
        val url = URL(Constants.BASE_URL.trimEnd('/') + "/" + path.trimStart('/'))
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
        return responseCode in 200..299
    }

    companion object {
        private const val TAG = "EmailOtpRepository"
    }
}
