package com.viewmodel.admin

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.UserRole
import com.data.domain.UserStatus
import com.data.domain.model.UserModel
import com.data.repository.UserRepository
import com.data.repository.AuthRepository
import kotlinx.coroutines.launch

class UserVM(
    private val repository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var userModelList by mutableStateOf(listOf<UserModel>())
        private set

    var currentUserId by mutableStateOf<String?>(null)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedRole by mutableStateOf("Tất cả")
        private set

    var selectedStatus by mutableStateOf("Tất cả")
        private set

    var errorMessage by mutableStateOf("")
        private set

    var currentUserEmail by mutableStateOf<String?>(null)

    init {
        loadUsersFlow()
        loadCurrentUserId()
    }

    private fun loadUsersFlow() {
        viewModelScope.launch {
            repository.getAllUsersFlow().collect { newUsers ->
                userModelList = newUsers
            }
        }
    }
    fun loadCurrentUserId() {
        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUser()?.id
            currentUserEmail = authRepository.getCurrentUser()?.email
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

    fun getUserById(id: String, onResult: (UserModel?) -> Unit) {
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
        id: String,
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

            val updatedUserModel = existingUser.copy(
                fullname = fullname,
                username = username,
                email = email,
                phone = phone,
                role = role,
                status = status,
                updatedAt = System.currentTimeMillis()
            )

            val result = repository.updateUser(updatedUserModel)

            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { exception -> errorMessage = exception.message ?: "Cập nhật thất bại" }
            )
        }
    }

    fun toggleUserStatus(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val existingUser = repository.getUserById(id)

            if (existingUser == null) {
                errorMessage = "Không tìm thấy người dùng"
                return@launch
            }

            val newStatus = if (existingUser.status == UserStatus.ACTIVE) {
                UserStatus.INACTIVE
            } else {
                UserStatus.ACTIVE
            }

            val updatedUserModel = existingUser.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )

            val result = repository.updateUser(updatedUserModel)

            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { exception -> errorMessage = exception.message ?: "Chuyển đổi thất bại" }
            )
        }
    }
    fun deleteUser(id: String) {
        viewModelScope.launch {
            repository.deleteUser(id)
        }
    }

    fun clearError() {
        errorMessage = ""
    }
}