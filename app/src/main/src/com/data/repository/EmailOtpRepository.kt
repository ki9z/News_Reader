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

    data class OtpApiResult(
        val success: Boolean,
        val message: String? = null
    )

    suspend fun sendOTP(email: String): OtpApiResult = withContext(Dispatchers.IO) {
        return@withContext try {
            postOtpRequest(email.trim(), "email")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gửi OTP: ${e.message}")
            OtpApiResult(false, "Không kết nối được server")
        }
    }

    suspend fun verifyOTP(email: String, otp: String): OtpApiResult = withContext(Dispatchers.IO) {
        return@withContext try {
            postOtpVerify(email.trim(), "email", otp.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xác thực OTP: ${e.message}")
            OtpApiResult(false, "Không kết nối được server")
        }
    }

    private fun postOtpRequest(identifier: String, channel: String): OtpApiResult {
        if (!Constants.USES_BACKEND_PROXY) return OtpApiResult(false, "Backend OTP chưa được cấu hình")
        val body = JSONObject()
            .put("identifier", identifier)
            .put("channel", channel)
        return postJson("api/auth/otp/request", body)
    }

    private fun postOtpVerify(identifier: String, channel: String, code: String): OtpApiResult {
        if (!Constants.USES_BACKEND_PROXY) return OtpApiResult(false, "Backend OTP chưa được cấu hình")
        val body = JSONObject()
            .put("identifier", identifier)
            .put("channel", channel)
            .put("code", code)
        return postJson("api/auth/otp/verify", body)
    }

    private fun postJson(path: String, body: JSONObject): OtpApiResult {
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

        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = runCatching {
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrDefault("")

        connection.disconnect()

        if (responseCode in 200..299) {
            return OtpApiResult(true)
        }

        val backendMessage = runCatching {
            if (responseText.isBlank()) "" else JSONObject(responseText).optString("message")
        }.getOrDefault("")

        val message = when {
            responseCode == 429 -> "Bạn vừa yêu cầu OTP, vui lòng đợi khoảng 60 giây rồi thử lại"
            backendMessage.isNotBlank() -> backendMessage
            else -> "Yêu cầu OTP thất bại (HTTP $responseCode)"
        }
        return OtpApiResult(false, message)
    }

    companion object {
        private const val TAG = "EmailOtpRepository"
    }
}
