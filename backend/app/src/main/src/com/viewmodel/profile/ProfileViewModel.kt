package com.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.local.dao.UserDao
import com.data.local.entity.UserEntity
import com.data.security.TokenManager
import com.data.settings.AuthProvider
import com.data.settings.UserSettings
import com.data.settings.UserSettingsRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userSettingsRepository: UserSettingsRepository,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : ViewModel() {

    val settings: StateFlow<UserSettings> = userSettingsRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

    val currentUser: StateFlow<UserEntity?> = userDao.observeSignedInUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setDarkModeEnabled(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setNotificationsEnabled(enabled)
            updateBreakingNewsTopic(enabled && settings.value.breakingNewsEnabled)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            userSettingsRepository.setLanguage(languageCode)
        }
    }

    fun setTextSize(textSize: String) {
        viewModelScope.launch {
            userSettingsRepository.setTextSize(textSize)
        }
    }

    fun setDataSaverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setDataSaverEnabled(enabled)
        }
    }

    fun setTrackReadingHistory(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setTrackReadingHistory(enabled)
        }
    }

    fun setPersonalizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setPersonalizationEnabled(enabled)
        }
    }

    fun setRegionCountry(countryCode: String) {
        viewModelScope.launch {
            userSettingsRepository.setRegionCountry(countryCode)
        }
    }

    fun setDefaultStartTab(tab: String) {
        viewModelScope.launch {
            userSettingsRepository.setDefaultStartTab(tab)
        }
    }

    fun setArticleStyle(style: String) {
        viewModelScope.launch {
            userSettingsRepository.setArticleStyle(style)
        }
    }

    fun setBreakingNewsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setBreakingNewsEnabled(enabled)
            updateBreakingNewsTopic(enabled && settings.value.notificationsEnabled)
        }
    }

    fun setDailyDigestEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setDailyDigestEnabled(enabled)
        }
    }

    fun setSyncHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setSyncHistoryEnabled(enabled)
        }
    }

    private fun updateBreakingNewsTopic(enabled: Boolean) {
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            if (enabled) {
                messaging.subscribeToTopic("breaking-news")
            } else {
                messaging.unsubscribeFromTopic("breaking-news")
            }
        }
    }

    fun clearLocalAccountData() {
        viewModelScope.launch(Dispatchers.IO) {
            userSettingsRepository.clearLocalAccountData()
            tokenManager.clearTokens()

            val currentUserEntity = userDao.getSignedInUser()
            if (currentUserEntity != null) {
                userDao.updateUser(
                    currentUserEntity.copy(
                        isSignedIn = false,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateProfile(
        displayName: String,
        email: String,
        avatarUrl: String,
        occupation: String,
        location: String,
        birthday: String,
        bio: String,
        interests: String
    ) {
        viewModelScope.launch {
            // 1. Lưu vào Cài đặt chung (Giữ nguyên của bạn)
            userSettingsRepository.updateProfile(
                displayName = displayName,
                email = email,
                avatarUrl = avatarUrl,
                occupation = occupation,
                location = location,
                birthday = birthday,
                bio = bio,
                interests = interests
            )

            val userEntity = currentUser.value

            if (userEntity != null) {
                val updatedUser = userEntity.copy(
                    fullName = displayName,
                    email = email,
                    avatarUrl = avatarUrl,
                    occupation = occupation,
                    location = location,
                    birthday = birthday,
                    bio = bio,
                    interests = interests,
                    updatedAt = System.currentTimeMillis()
                )

                userDao.updateUser(updatedUser)
            }
        }
    }

    fun signInWithProvider(provider: AuthProvider, phone: String? = null) {
        viewModelScope.launch {
            userSettingsRepository.signInWithProvider(provider, phone)
        }
    }

    fun linkProvider(provider: AuthProvider) {
        viewModelScope.launch {
            userSettingsRepository.linkProvider(provider)
        }
    }

    fun unlinkProvider(provider: AuthProvider) {
        viewModelScope.launch {
            userSettingsRepository.unlinkProvider(provider)
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Xóa cài đặt giao diện
            userSettingsRepository.logout()

            // 2. Xóa Token bảo mật
            tokenManager.clearTokens()

            // 3. Xóa trạng thái đăng nhập trong CSDL
            val currentUserEntity = userDao.getSignedInUser()
            if (currentUserEntity != null) {
                val loggedOutUser = currentUserEntity.copy(isSignedIn = false)
                userDao.updateUser(loggedOutUser)
            }
        }
    }

    fun setAuthType(authType: String) {
        viewModelScope.launch {
            userSettingsRepository.setAuthType(authType)
        }
    }
}