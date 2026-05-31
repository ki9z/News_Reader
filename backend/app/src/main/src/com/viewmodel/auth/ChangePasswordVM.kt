package com.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.repository.AuthRepository
import com.data.repository.UserRepository
import com.data.mapper.UserMapper
import kotlinx.coroutines.launch

class ChangePasswordVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    var oldPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var message by mutableStateOf("")

    fun onChangePasswordClick(onSuccess: () -> Unit) {
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            message = "Vui lòng nhập đầy đủ thông tin"
            return
        }
        if (newPassword != confirmPassword) {
            message = "Mật khẩu mới không khớp"
            return
        }
        if (oldPassword == newPassword) {
            message = "Mật khẩu mới không được trùng với mật khẩu cũ"
            return
        }

        viewModelScope.launch {
            val currentUserEntity = authRepository.getCurrentUser()

            if (currentUserEntity == null) {
                message = "Lỗi phiên làm việc, vui lòng đăng nhập lại"
                return@launch
            }

            // Kiểm tra mật khẩu cũ
            val oldHash = oldPassword.hashCode().toString()
            if (currentUserEntity.passwordHash != oldHash) {
                message = "Mật khẩu cũ không đúng"
                return@launch
            }

            // Cập nhật mật khẩu mới
            val newHash = newPassword.hashCode().toString()
            val updatedEntity = currentUserEntity.copy(passwordHash = newHash)

            val result = userRepository.updateUser(UserMapper.toUserModel(updatedEntity))

            result.fold(
                onSuccess = {
                    message = ""
                    oldPassword = ""
                    newPassword = ""
                    confirmPassword = ""

                    onSuccess()
                },
                onFailure = { exception ->
                    message = exception.message ?: "Cập nhật thất bại!"
                }
            )
        }
    }
}