package com.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.repository.AuthRepository
import com.data.settings.UserSettingsRepository
import com.util.NetworkResult
import com.util.UiState
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _authState = MutableLiveData<UiState<Unit>>()
    val authState: LiveData<UiState<Unit>> = _authState

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    fun register(email: String, password: String, name: String = "") {
        if (!validateInput(email, password)) {
            _authState.value = UiState.Error("Vui lòng nhập email và mật khẩu hợp lệ.")
            return
        }

        viewModelScope.launch {
            _authState.value = UiState.Loading

            when (val result = authRepository.register(email, password, name.ifBlank { null })) {
                is NetworkResult.Success -> {
                    updateSignInStatus(email)
                    _authState.value = UiState.Success(Unit)
                    _isLoggedIn.value = true
                }
                is NetworkResult.Error -> {
                    _authState.value = UiState.Error(result.message ?: "Registration failed")
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (!validateInput(email, password)) {
            _authState.value = UiState.Error("Vui lòng nhập email và mật khẩu hợp lệ.")
            return
        }

        viewModelScope.launch {
            _authState.value = UiState.Loading

            when (val result = authRepository.login(email, password)) {
                is NetworkResult.Success -> {
                    updateSignInStatus(email)
                    _authState.value = UiState.Success(Unit)
                    _isLoggedIn.value = true
                }
                is NetworkResult.Error -> {
                    _authState.value = UiState.Error(result.message ?: "Login failed")
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String, email: String, name: String?) {
        viewModelScope.launch {
            _authState.value = UiState.Loading

            when (val result = authRepository.loginWithOAuth("google", idToken, null, email, name)) {
                is NetworkResult.Success -> {
                    updateSignInStatus(email)
                    _authState.value = UiState.Success(Unit)
                    _isLoggedIn.value = true
                }
                is NetworkResult.Error -> {
                    _authState.value = UiState.Error(result.message ?: "Google login failed")
                }
            }
        }
    }

    fun loginWithFacebook(accessToken: String, email: String, name: String?) {
        viewModelScope.launch {
            _authState.value = UiState.Loading

            when (val result = authRepository.loginWithOAuth("facebook", null, accessToken, email, name)) {
                is NetworkResult.Success -> {
                    updateSignInStatus(email)
                    _authState.value = UiState.Success(Unit)
                    _isLoggedIn.value = true
                }
                is NetworkResult.Error -> {
                    _authState.value = UiState.Error(result.message ?: "Facebook login failed")
                }
            }
        }
    }

    fun loginWithApple(idToken: String, email: String?, name: String?) {
        if (email.isNullOrBlank()) {
            _authState.value = UiState.Error("Email is required for Apple Sign-In")
            return
        }

        viewModelScope.launch {
            _authState.value = UiState.Loading

            when (val result = authRepository.loginWithOAuth("apple", idToken, null, email, name)) {
                is NetworkResult.Success -> {
                    updateSignInStatus(email)
                    _authState.value = UiState.Success(Unit)
                    _isLoggedIn.value = true
                }
                is NetworkResult.Error -> {
                    _authState.value = UiState.Error(result.message ?: "Apple login failed")
                }
            }
        }
    }

    fun requestPhoneOtp(phone: String) {
        if (phone.length < 10) {
            _authState.value = UiState.Error("Please enter a valid phone number")
            return
        }

        viewModelScope.launch {
            _authState.value = UiState.Loading

            when (val result = authRepository.requestPhoneOtp(phone)) {
                is NetworkResult.Success -> {
                    _authState.value = UiState.Success(Unit)
                }
                is NetworkResult.Error -> {
                    _authState.value = UiState.Error(result.message ?: "Failed to send OTP")
                }
            }
        }
    }

    fun verifyPhoneOtp(phone: String, otp: String, name: String = "") {
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            _authState.value = UiState.Error("Please enter a valid 6-digit OTP")
            return
        }

        viewModelScope.launch {
            _authState.value = UiState.Loading

            when (val result = authRepository.verifyPhoneOtp(phone, otp, name.ifBlank { null })) {
                is NetworkResult.Success -> {
                    updateSignInStatusPhone(phone)
                    _authState.value = UiState.Success(Unit)
                    _isLoggedIn.value = true
                }
                is NetworkResult.Error -> {
                    _authState.value = UiState.Error(result.message ?: "OTP verification failed")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            userSettingsRepository.logout()
            _isLoggedIn.value = false
        }
    }

    fun checkLoginStatus() {
        _isLoggedIn.value = authRepository.hasValidToken()
    }

    private fun updateSignInStatus(email: String) {
        viewModelScope.launch {
            userSettingsRepository.signInWithProvider(
                com.data.settings.AuthProvider.EMAIL
            )
        }
    }

    private fun updateSignInStatusPhone(phone: String) {
        viewModelScope.launch {
            userSettingsRepository.signInWithProvider(
                com.data.settings.AuthProvider.PHONE,
                phone
            )
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) return false
        if (!email.contains("@")) return false
        if (password.length < 6) return false
        return true
    }
}
