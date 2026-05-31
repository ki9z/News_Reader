package com.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.data.local.dao.UserDao
import com.data.repository.AuthRepository
import com.data.security.TokenManager
import com.data.settings.UserSettingsRepository

class AuthViewModelFactory(
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val authRepository = AuthRepository(tokenManager, userDao)

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository, userSettingsRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}