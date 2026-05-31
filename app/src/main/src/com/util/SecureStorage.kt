package com.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_login_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveLoginInfo(email: String, password: String, rememberMe: Boolean) {
        sharedPreferences.edit().apply {
            putString("EMAIL", email)
            putString("PASSWORD", password)
            putBoolean("REMEMBER_ME", rememberMe)
            apply()
        }
    }

    fun clearLoginInfo() {
        sharedPreferences.edit().apply {
            remove("EMAIL")
            remove("PASSWORD")
            putBoolean("REMEMBER_ME", false)
            apply()
        }
    }

    fun getEmail(): String = sharedPreferences.getString("EMAIL", "") ?: ""

    fun getPassword(): String = sharedPreferences.getString("PASSWORD", "") ?: ""

    fun isRememberMe(): Boolean = sharedPreferences.getBoolean("REMEMBER_ME", false)
}