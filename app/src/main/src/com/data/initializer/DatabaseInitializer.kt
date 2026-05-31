package com.data.initializer

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.data.local.db.AppDatabase
import com.data.local.entity.UserEntity
import com.data.remote.dto.JsonReader
import com.data.repository.ArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object DatabaseInitializer {
    suspend fun init(context: Context, db: AppDatabase) {
        withContext(Dispatchers.IO) {
            try {
                val repo = ArticleRepository(db)
                val userDao = db.userDao()

                val totalUsers = userDao.getTotalUsers()
                if (totalUsers == 0) {
                    val currentTime = System.currentTimeMillis()

                    // Tài khoản Admin
                    val adminAccount = UserEntity(
                        id = UUID.randomUUID().toString(),
                        email = "huytran27t092005@gmail.com",
                        fullName = "Quản Trị Viên",
                        username = "admin",
                        passwordHash = "123".hashCode().toString(),
                        role = "admin",
                        status = "ACTIVE",
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        isSignedIn = false
                    )
                    userDao.insertUser(adminAccount)

                    // Tài khoản khách mặc định
                    val userAccount = UserEntity(
                        id = UUID.randomUUID().toString(),
                        email = "user1@gmail.com",
                        fullName = "Nguyễn Văn A",
                        username = "user1",
                        passwordHash = "123".hashCode().toString(),
                        role = "user",
                        status = "ACTIVE",
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        isSignedIn = false
                    )
                    userDao.insertUser(userAccount)
                    Log.d("DB_INIT", "Đã tạo tài khoản Admin và User mặc định.")
                }

                val currentArticlesCount = repo.getAllArticles().size
                if (currentArticlesCount == 0) {
                    val jsonFiles = listOf(
                        "news1.json",
                        "news2.json",
                        "news3.json",
                        "news4.json"
                    )

                    val allArticlesDto = mutableListOf<com.data.remote.dto.ArticleJson>()

                    jsonFiles.forEach { fileName ->
                        val jsonString = JsonReader.readFromAssets(context, fileName)
                        if (jsonString.isNotBlank()) {
                            val dtoList = com.data.remote.dto.JsonParser.json.decodeFromString<List<com.data.remote.dto.ArticleJson>>(jsonString)
                            allArticlesDto.addAll(dtoList)
                        }
                    }

                    val uniqueCategories = allArticlesDto.map { it.category.trim() }.distinct()
                    val categoryEntities = uniqueCategories.map { catName ->
                        val catId = "cat_" + catName.lowercase().replace(" ", "_")
                        com.data.local.entity.CategoryEntity(id = catId, name = catName)
                    }

                    val articleEntities = mutableListOf<com.data.local.entity.ArticleEntity>()
                    val blockEntities = mutableListOf<com.data.local.entity.ArticleBlockEntity>()

                    allArticlesDto.forEach { dto ->
                        val catId = "cat_" + dto.category.trim().lowercase().replace(" ", "_")

                        val article = com.data.mapper.ArticleMapper.toArticleEntity(
                            json = dto,
                            categoryId = catId,
                            sourceId = dto.source.lowercase().replace(" ", "")
                        )
                        articleEntities.add(article)

                        val blocks = com.data.mapper.ArticleMapper.toBlockEntities(
                            blocks = dto.blocks,
                            articleUrl = dto.url
                        )
                        blockEntities.addAll(blocks)
                    }

                    db.withTransaction {
                        categoryEntities.forEach { cat ->
                            db.categoryDao().insertCategory(cat)
                        }
                        db.articleDao().insertArticles(articleEntities)
                        db.articleBlockDao().insertAll(blockEntities)
                    }

                    Log.d("DB_INIT", "Đã nạp thành công ${articleEntities.size} bài báo và ${blockEntities.size} blocks vào DB!")
                }

            } catch (e: Exception) {
                Log.e("DB_INIT_ERROR", "Lỗi khởi tạo Database: ${e.message}")
            }
        }
    }
}