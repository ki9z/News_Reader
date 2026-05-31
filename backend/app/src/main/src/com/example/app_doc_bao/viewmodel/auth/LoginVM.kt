package com.example.app_doc_bao.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.model.UserModel
import com.example.app_doc_bao.data.repository.UserRepository
import com.example.app_doc_bao.util.SessionManager
import kotlinx.coroutines.launch

class LoginVM(
    private val userRepository: UserRepository
) : ViewModel() {

    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var rememberMe by mutableStateOf(false)
    var errorMessage by mutableStateOf("")

    fun onLoginClick(onSuccess: (UserModel) -> Unit) {
        viewModelScope.launch {
            val user = userRepository.login(username, password)

            if (user != null) {
                SessionManager.currentUserModel = user
                errorMessage = ""
                onSuccess(user)
            } else {
                errorMessage = "Sai tài khoản, mật khẩu hoặc tài khoản bị khóa"
            }
        }
    }
}