package com.example.app_doc_bao.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.model.AdminHomeUiState
import com.example.app_doc_bao.data.repository.ArticleRepository
import com.example.app_doc_bao.data.repository.UserRepository
import com.example.app_doc_bao.util.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminHomeVM(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(AdminHomeUiState())
    val uiState: State<AdminHomeUiState> = _uiState

    init {
        loadAdminInfo()
    }

    fun loadAdminInfo() {
        viewModelScope.launch {

            // 1. LẤY TOÀN BỘ DỮ LIỆU TỪ DB THẬT
            val articles = articleRepository.getAllArticles()
            val users = userRepository.getAllUsers()
            val currentUser = SessionManager.currentUserModel

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val todayString = dateFormat.format(Date())

            // 2. TÍNH TOÁN BÀI BÁO
            val totalArticleCount = articles.size
            val totalViewCount = articles.sumOf { it.views }
            val todayArticleCount = articles.count { it.publishDate == todayString }

            // 3. TÍNH TOÁN NGƯỜI DÙNG
            val totalUserCount = users.size
            val newUsersCount = users.count { user ->
                dateFormat.format(Date(user.createdAt)) == todayString
            }

            // 4. CẬP NHẬT GIAO DIỆN
            _uiState.value = AdminHomeUiState(
                fullname = currentUser?.fullname ?: "",
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
        SessionManager.logout()
        onLogoutSuccess()
    }
}