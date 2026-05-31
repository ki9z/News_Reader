package com.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.repository.AuthRepository
import com.util.NetworkResult
import kotlinx.coroutines.launch

class RegisterVM(
    private val authRepository: AuthRepository
): ViewModel() {

    var fullname by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf("")

    fun onRegisterClick(onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.register(email, password, fullname)) {
                is NetworkResult.Success -> {
                    errorMessage = ""
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message ?: "Đăng ký thất bại, vui lòng thử lại!"
                }
            }
        }
    }
}