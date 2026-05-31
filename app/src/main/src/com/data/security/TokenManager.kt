package com.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAccessToken(token: String) {
        encryptedSharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? {
        return encryptedSharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        encryptedSharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? {
        return encryptedSharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveTokenExpiry(expiresIn: Long) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        encryptedSharedPreferences.edit().putLong(KEY_TOKEN_EXPIRY, expiryTime).apply()
    }

    fun getTokenExpiry(): Long {
        return encryptedSharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0)
    }

    fun isAccessTokenExpired(): Boolean {
        val expiry = getTokenExpiry()
        return System.currentTimeMillis() > expiry
    }

    fun saveUserId(userId: String) {
        encryptedSharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return encryptedSharedPreferences.getString(KEY_USER_ID, null)
    }

    fun clearTokens() {
        encryptedSharedPreferences.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRY)
            remove(KEY_USER_ID)
            apply()
        }
    }

    fun hasToken(): Boolean {
        return !getAccessToken().isNullOrBlank()
    }

    companion object {
        private const val PREFS_NAME = "encrypted_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
    }
}
