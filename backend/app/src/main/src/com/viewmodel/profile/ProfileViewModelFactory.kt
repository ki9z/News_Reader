package com.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.data.local.dao.UserDao
import com.data.security.TokenManager
import com.data.settings.UserSettingsRepository

class ProfileViewModelFactory(
    private val userSettingsRepository: UserSettingsRepository,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // TRUYỀN THÊM tokenManager VÀO ĐÂY
            return ProfileViewModel(userSettingsRepository, userDao, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}