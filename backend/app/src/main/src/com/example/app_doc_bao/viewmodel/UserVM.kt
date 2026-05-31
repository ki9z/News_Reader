package com.example.app_doc_bao.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.model.UserModel
import com.example.app_doc_bao.data.domain.*
import com.example.app_doc_bao.data.repository.UserRepository
import kotlinx.coroutines.launch

class UserVM(
    private val repository: UserRepository
) : ViewModel() {

    var userModelList by mutableStateOf(listOf<UserModel>())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedRole by mutableStateOf("Tất cả")
        private set

    var selectedStatus by mutableStateOf("Tất cả")
        private set

    var errorMessage by mutableStateOf("")
        private set

    init {
        loadUsersFlow()
    }

    private fun loadUsersFlow() {
        viewModelScope.launch {
            repository.getAllUsersFlow().collect { newUsers ->
                userModelList = newUsers
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateRole(role: String) {
        selectedRole = role
    }

    fun updateStatus(status: String) {
        selectedStatus = status
    }

    fun getFilteredUsers(): List<UserModel> {
        return userModelList.filter { user ->
            val matchSearch =
                user.username.contains(searchQuery, ignoreCase = true) ||
                        user.email.contains(searchQuery, ignoreCase = true)

            val matchRole =
                selectedRole == "Tất cả" || user.role.name == selectedRole

            val matchStatus =
                selectedStatus == "Tất cả" || user.status.name == selectedStatus

            matchSearch && matchRole && matchStatus
        }
    }

    fun getUserById(id: Long, onResult: (UserModel?) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserById(id)
            onResult(user)
        }
    }

    fun addUser(
        fullname: String,
        username: String,
        email: String,
        password: String,
        phone: String?,
        role: UserRole,
        status: UserStatus,
        onSuccess: () -> Unit
    ) {
        if (username.isBlank() || email.isBlank() || password.isBlank() || fullname.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin bắt buộc"
            return
        }

        errorMessage = ""

        viewModelScope.launch {
            val result = repository.addUser(
                email = email,
                fullname = fullname,
                username = username,
                password = password,
                phone = phone,
                role = role,
                status = status
            )

            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { exception -> errorMessage = exception.message ?: "Thêm thất bại" }
            )
        }
    }

    fun updateUser(
        id: Long,
        fullname: String,
        username: String,
        email: String,
        phone: String?,
        role: UserRole,
        status: UserStatus,
        onSuccess: () -> Unit
    ) {
        if (username.isBlank() || email.isBlank() || fullname.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin bắt buộc"
            return
        }

        errorMessage = ""

        viewModelScope.launch {
            val existingUser = repository.getUserById(id)

            if (existingUser == null) {
                errorMessage = "Không tìm thấy người dùng"
                return@launch
            }

            val updatedUserModel = UserModel(
                id = id,
                email = email,
                fullname = fullname,
                username = username,
                password = existingUser.password,
                phone = phone,
                role = role,
                status = status,
                createdAt = existingUser.createdAt
            )

            val result = repository.updateUser(updatedUserModel)

            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { exception -> errorMessage = exception.message ?: "Cập nhật thất bại" }
            )
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.deleteUser(id)
        }
    }

    fun clearError() {
        errorMessage = ""
    }
}