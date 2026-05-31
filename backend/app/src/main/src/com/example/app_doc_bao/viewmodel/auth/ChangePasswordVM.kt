package com.example.app_doc_bao.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.repository.UserRepository
import com.example.app_doc_bao.util.SessionManager
import kotlinx.coroutines.launch

class ChangePasswordVM(
    private val userRepository: UserRepository
) : ViewModel() {
    var oldPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var message by mutableStateOf("")

    fun onChangePasswordClick(onSuccess: () -> Unit) {

        val currentUser = SessionManager.currentUserModel

        when {
            currentUser == null -> {
                message = "Lỗi phiên làm việc, vui lòng đăng nhập lại"
            }
            oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                message = "Vui lòng nhập đầy đủ thông tin"
            }
            currentUser.password != oldPassword -> {
                message = "Mật khẩu cũ không đúng"
            }
            newPassword != confirmPassword -> {
                message = "Mật khẩu mới không khớp"
            }
            else -> {
                viewModelScope.launch {
                    val updatedUser = currentUser.copy(password = newPassword)

                    val result = userRepository.updateUser(updatedUser)

                    // Xử lý kết quả trả về
                    result.fold(
                        onSuccess = {
                            SessionManager.currentUserModel = updatedUser
                            message = ""
                            onSuccess()
                        },
                        onFailure = { exception ->
                            message = exception.message ?: "Cập nhật thất bại!"
                        }
                    )
                }
            }
        }
    }
}