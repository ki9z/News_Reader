package com.util

import org.json.JSONObject
import retrofit2.Response

object NewsApiErrorParser {

    fun parseMessage(response: Response<*>): String? {
        return runCatching {
            val raw = response.errorBody()?.string().orEmpty()
            if (raw.isBlank()) return@runCatching null

            val json = JSONObject(raw)
            val code = json.optString("code").orEmpty()
            val message = json.optString("message").orEmpty()

            when {
                code.equals("invalidClientToken", ignoreCase = true) ->
                    "BACKEND_APP_TOKEN không đúng. Kiểm tra lại local.properties và backend/.env."

                code.equals("missingBackendNewsApiKey", ignoreCase = true) ->
                    "Backend chưa cấu hình NEWS_API_KEY. Kiểm tra file backend/.env."

                code.equals("apiKeyMissing", ignoreCase = true) ->
                    "Thiếu NEWS_API_KEY. Nếu dùng backend, hãy cấu hình NEWS_API_KEY trong backend/.env."

                code.equals("apiKeyInvalid", ignoreCase = true) ->
                    "NEWS_API_KEY không hợp lệ. Kiểm tra lại key NewsAPI."

                code.equals("apiKeyDisabled", ignoreCase = true) ->
                    "NEWS_API_KEY đã bị vô hiệu hóa."

                code.equals("rateLimited", ignoreCase = true) ->
                    "Đã vượt giới hạn NewsAPI/backend. Vui lòng thử lại sau."

                code.equals("sourcesTooMany", ignoreCase = true) ->
                    "Bạn đang chọn quá nhiều nguồn tin cùng lúc."

                code.equals("sourceDoesNotExist", ignoreCase = true) ->
                    "Nguồn tin này không tồn tại trên NewsAPI."

                message.isNotBlank() ->
                    message

                else -> null
            }
        }.getOrNull()
    }
}

