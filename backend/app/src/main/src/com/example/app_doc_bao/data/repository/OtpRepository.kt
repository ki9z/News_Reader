package com.example.app_doc_bao.data.repository

import java.net.URL

class OtpRepository {

    fun sendOTP(email: String): Boolean {
        return try {
            val url = URL("https://news-reader-android-backend.onrender.com/auth/send-otp?email=$email")
            url.readText()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun verifyOTP(email: String, otp: String): Boolean {
        return try {
            val url = URL("https://news-reader-android-backend.onrender.com/auth/verify-otp?email=$email&otp=$otp")
            val result = url.readText()
            result.trim() == "Success"
        } catch (e: Exception) {
            false
        }
    }
}