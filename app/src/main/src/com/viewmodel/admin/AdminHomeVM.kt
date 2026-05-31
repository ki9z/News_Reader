package com.viewmodel.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.model.AdminHomeUiState
import com.data.repository.ArticleRepository
import com.data.repository.UserRepository
import com.data.repository.AuthRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminHomeVM(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(AdminHomeUiState())
    val uiState: State<AdminHomeUiState> = _uiState

    init {
        loadAdminInfo()
    }

    fun loadAdminInfo() {
        viewModelScope.launch {
            val articles = articleRepository.getAllArticles()
            val users = userRepository.getAllUsers()
            val currentUser = authRepository.getCurrentUser()

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val todayString = dateFormat.format(Date())

            val totalArticleCount = articles.size
            val totalViewCount = articles.sumOf { it.views }
            val todayArticleCount = articles.count { it.publishDate == todayString }

            val totalUserCount = users.size
            val newUsersCount = users.count { user ->
                dateFormat.format(Date(user.createdAt)) == todayString
            }

            _uiState.value = AdminHomeUiState(
                fullname = currentUser?.fullName ?: "", // Chú ý: Entity dùng fullName
                email = currentUser?.email ?: "",
                totalArticles = totalArticleCount.toString(),
                totalUsers = totalUserCount.toString(),
                totalViews = totalViewCount.toString(),
                todayArticles = todayArticleCount.toString(),
                newUsers = newUsersCount.toString()
            )
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()

            onLogoutSuccess()
        }
    }
}