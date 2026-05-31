package com.example.app_doc_bao.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.repository.UserRepository
import kotlinx.coroutines.launch

class ForgotPasswordVM(
    private val userRepository: UserRepository
) : ViewModel() {

    var email by mutableStateOf("")
    var errorMessage by mutableStateOf("")

    fun onSendRequestClick(onSuccess: (String) -> Unit) {
        if (email.isBlank()) {
            errorMessage = "Vui lòng nhập email"
            return
        }

        viewModelScope.launch {
            val user = userRepository.getUserByEmail(email)

            if (user != null) {
                errorMessage = ""
                onSuccess(user.email)
            } else {
                errorMessage = "Email không tồn tại trong hệ thống"
            }
        }
    }
}