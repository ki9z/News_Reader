package com.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.repository.AuthRepository
import com.data.settings.AuthProvider
import com.data.settings.UserSettingsRepository
import com.util.NetworkResult
import com.util.SecureStorage
import kotlinx.coroutines.launch

class LoginVM(
    private val authRepository: AuthRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var rememberMe by mutableStateOf(false)
    var errorMessage by mutableStateOf("")

    init {
        if (secureStorage.isRememberMe()) {
            email = secureStorage.getEmail()
            password = secureStorage.getPassword()
            rememberMe = true
        }
    }

    fun onLoginClick(onSuccess: (Boolean) -> Unit) {

        viewModelScope.launch {
            when (val result = authRepository.login(email.trim(), password)) {
                is NetworkResult.Success -> {
                    errorMessage = ""

                    userSettingsRepository.signInWithProvider(AuthProvider.EMAIL)

                    if (rememberMe) {
                        secureStorage.saveLoginInfo(email.trim(), password, true)
                    } else {
                        secureStorage.clearLoginInfo()
                    }

                    val isAdmin = authRepository.isAdmin()
                    onSuccess(isAdmin)
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message ?: "Sai tài khoản hoặc mật khẩu"
                }
            }
        }
    }
    fun onGoogleLoginSuccess(idToken: String?, email: String, name: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.loginWithOAuth("google", idToken, null, email, name)) {
                is NetworkResult.Success -> {
                    errorMessage = ""
                    userSettingsRepository.signInWithProvider(AuthProvider.GOOGLE)
                    secureStorage.clearLoginInfo()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message ?: "Đăng nhập Google thất bại"
                }
            }
        }
    }

}