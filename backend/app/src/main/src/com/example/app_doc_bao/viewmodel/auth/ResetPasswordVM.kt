package com.example.app_doc_bao.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.repository.UserRepository
import kotlinx.coroutines.launch

class ResetPasswordVM(
    private val userRepository: UserRepository
) : ViewModel() {
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var message by mutableStateOf("")

    fun onResetPasswordClick(email: String?, onSuccess: () -> Unit) {
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            message = "Vui lòng nhập đầy đủ mật khẩu"
            return
        }

        if (newPassword != confirmPassword) {
            message = "Mật khẩu không khớp"
            return
        }

        if (email.isNullOrBlank()) {
            message = "Lỗi dữ liệu email, vui lòng thử lại quá trình"
            return
        }

        viewModelScope.launch {
            val user = userRepository.getUserByEmail(email)

            if (user != null) {
                val updatedUser = user.copy(password = newPassword)

                val result = userRepository.updateUser(updatedUser)

                result.fold(
                    onSuccess = {
                        message = ""
                        onSuccess()
                    },
                    onFailure = { exception ->
                        message = exception.message ?: "Cập nhật mật khẩu thất bại"
                    }
                )
            } else {
                message = "Không tìm thấy tài khoản"
            }
        }
    }
}