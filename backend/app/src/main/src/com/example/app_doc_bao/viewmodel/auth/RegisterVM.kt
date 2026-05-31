package com.example.app_doc_bao.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.domain.*
import com.example.app_doc_bao.data.repository.UserRepository
import kotlinx.coroutines.launch

class RegisterVM(
    private val userRepository: UserRepository
): ViewModel() {

    var fullname by mutableStateOf("")
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var errorMessage by mutableStateOf("")

    fun onRegisterClick(onSuccess: () -> Unit) {

        viewModelScope.launch {
            val result = userRepository.addUser(
                fullname = fullname.ifBlank { username },
                username = username,
                email = email,
                password = password,
                phone = null,
                role = UserRole.USER,
                status = UserStatus.ACTIVE
            )

            result.fold(
                onSuccess = {
                    errorMessage = ""
                    onSuccess()
                },
                onFailure = { exception ->
                    errorMessage = exception.message ?: "Đăng ký thất bại, vui lòng thử lại!"
                }
            )
        }
    }
}