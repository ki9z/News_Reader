package com.example.app_doc_bao.data.local.initializer

import android.content.Context
import android.util.Log
import com.example.app_doc_bao.data.local.AppDatabase
import com.example.app_doc_bao.data.local.entity.User
import com.example.app_doc_bao.data.domain.UserRole
import com.example.app_doc_bao.data.domain.UserStatus
import com.example.app_doc_bao.data.remote.JsonReader
import com.example.app_doc_bao.data.repository.ArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseInitializer {
    suspend fun init(context: Context, db: AppDatabase) {
        withContext(Dispatchers.IO) {
            try {
                val repo = ArticleRepository(db)
                val userDao = db.userDao()

                // 1. Kiểm tra và nạp bài báo
                val currentArticlesCount = repo.getAllArticles().size
                if (currentArticlesCount == 0) {
                    val jsonFiles = listOf("admin_news1.json", "admin_news2.json", "admin_news3.json", "admin_news4.json")
                    jsonFiles.forEach { fileName ->
                        val json = JsonReader.readFromAssets(context, fileName)
                        if (json.isNotBlank()) {
                            repo.insertFromJson(json)
                            Log.d("DB_INIT", "Đã nạp thành công bài báo từ: $fileName")
                        }
                    }
                } else {
                    Log.d("DB_INIT", "Đã có $currentArticlesCount bài báo, bỏ qua nạp JSON.")
                }

                // 2. Xử lý tài khoản
                val totalUsers = userDao.getTotalUsers()
                if (totalUsers == 0) {
                    val adminAccount = User(
                        email = "huytran27t092005@gmail.com",
                        fullname = "Quản Trị Viên",
                        username = "admin",
                        password = "123",
                        role = UserRole.ADMIN.name,
                        status = UserStatus.ACTIVE.name,
                        createdAt = System.currentTimeMillis(),
                        phone = null
                    )
                    userDao.insertUser(adminAccount)

                    val userAccount = User(
                        email = "user1@gmail.com",
                        fullname = "Nguyễn Văn A",
                        username = "user1",
                        password = "123",
                        role = UserRole.USER.name,
                        status = UserStatus.ACTIVE.name,
                        createdAt = System.currentTimeMillis(),
                        phone = null
                    )
                    userDao.insertUser(userAccount)
                    Log.d("DB_INIT", "Đã tạo tài khoản Admin và User mặc định lần đầu.")
                } else {
                    Log.d("DB_INIT", "Đã có $totalUsers người dùng, không tạo thêm tài khoản mặc định.")
                }

            } catch (e: Exception) {
                Log.e("DB_INIT_ERROR", "Lỗi khởi tạo Database: ${e.message}")
            }
        }
    }
}