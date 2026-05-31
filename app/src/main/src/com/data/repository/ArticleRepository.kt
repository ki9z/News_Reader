package com.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.data.domain.ArticleStatus
import com.data.local.db.AppDatabase
import com.data.local.entity.*
import com.data.mapper.ArticleMapper
import com.data.domain.model.ArticleModel
import com.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class ArticleRepository(
    private val db: AppDatabase
) {
    private val categoryDao = db.categoryDao()
    private val articleDao = db.articleDao()
    private val articleBlockDao = db.articleBlockDao()

    // 1. NẠP DỮ LIỆU TỪ JSON (Assets/Admin)
    suspend fun insertFromJson(jsonString: String) = withContext(Dispatchers.IO) {
        if (jsonString.isBlank()) return@withContext

        val articles = try {
            JsonParser.json.decodeFromString<List<ArticleJson>>(jsonString)
        } catch (e: Exception) {
            Log.e("JSON_PARSE_ERROR", e.message ?: "Unknown error")
            return@withContext
        }

        db.withTransaction {
            // Tối ưu: Batch insert thay vì loop từng cái
            val uniqueCategories = articles.map { it.category.trim() }.distinct()
            val categoryEntities = uniqueCategories.map { catName ->
                CategoryEntity(
                    id = "cat_${catName.lowercase().replace(" ", "_")}",
                    name = catName,
                    sortOrder = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            categoryDao.upsertCategories(categoryEntities)

            val articleEntities = mutableListOf<ArticleEntity>()
            val blockEntities = mutableListOf<ArticleBlockEntity>()

            articles.forEach { articleJson ->
                val catId = "cat_${articleJson.category.trim().lowercase().replace(" ", "_")}"
                val sourceId = articleJson.source.lowercase().replace(" ", "")

                articleEntities.add(ArticleMapper.toArticleEntity(articleJson, catId, sourceId))
                blockEntities.addAll(ArticleMapper.toBlockEntities(articleJson.blocks, articleJson.url))
            }

            articleEntities.forEach { articleDao.upsertArticlePreservingStats(it) }
            articleBlockDao.insertAll(blockEntities)
        }
    }

    // 2. ĐỌC DỮ LIỆU
    fun getAllArticlesFlow(): Flow<List<ArticleModel>> {
        return articleDao.observeAllArticlesWithDetails().map { detailsList ->
            detailsList.map { details ->
                ArticleMapper.toArticleModel(
                    article = details.article,
                    blocks = emptyList(),
                    categoryName = details.category?.name
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllArticles(): List<ArticleModel> = withContext(Dispatchers.IO) {
        val articlesWithDetails = articleDao.getAllArticlesWithDetails()
        articlesWithDetails.map { details ->
            ArticleMapper.toArticleModel(
                article = details.article,
                blocks = emptyList(),
                categoryName = details.category?.name ?: "Chưa phân loại"
            )
        }
    }

    suspend fun getArticleByUrl(url: String): ArticleModel? = withContext(Dispatchers.IO) {
        articleDao.getArticleWithDetails(url)?.let { details ->
            ArticleMapper.toArticleModel(
                article = details.article,
                blocks = details.blocks,
                categoryName = details.category?.name
            )
        }
    }

    suspend fun getAllCategoryNames(): List<String> = withContext(Dispatchers.IO) {
        return@withContext categoryDao.getAllCategoryNames()
    }
    // 3. THÊM BÀI BÁO MỚI (Từ Form Admin)
    suspend fun addArticle(
        title: String,
        thumbnail: String?,
        author: String,
        source: String,
        category: String,
        timeString: String,
        url: String,
        blocks: List<BlockJson>,
        status: ArticleStatus = ArticleStatus.PUBLISHED
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val categoryId = "cat_${category.lowercase().replace(" ", "_")}"
            categoryDao.upsertCategories(
                listOf(CategoryEntity(id = categoryId, name = category, sortOrder = 0, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
            )

            // ĐÃ SỬA: Lấy block "sapo" để làm description giống hệt hàm Mapper
            val descriptionText = blocks.firstOrNull { it.type == "sapo" }?.data
            val normalizedUrl = url.trim()
            val existingArticle = articleDao.getArticleByUrl(normalizedUrl)

            val newArticle = ArticleEntity(
                url = normalizedUrl,
                title = title.trim(),
                urlToImage = thumbnail?.trim(),
                author = author.trim().takeIf { it.isNotBlank() } ?: "Ẩn danh",
                sourceId = source.lowercase().replace(" ", ""),
                sourceName = source,
                categoryId = categoryId,
                publishedAt = timeString,
                createdAt = existingArticle?.createdAt ?: System.currentTimeMillis(),
                view = existingArticle?.view ?: 0L,
                status = status.name,
                city = null,
                content = null,
                description = descriptionText // Lưu description để hiển thị ở trang chủ
            )

            // Dùng insertArticles(list) cho đồng bộ
            articleDao.insertArticles(listOf(newArticle))

            if (blocks.isNotEmpty()) {
                val blockEntities = ArticleMapper.toBlockEntities(blocks, newArticle.url)
                articleBlockDao.insertAll(blockEntities)
            }
        }
    }

    // 4. CẬP NHẬT BÀI BÁO
    suspend fun updateArticle(
        title: String,
        thumbnail: String?,
        author: String,
        source: String,
        category: String,
        timeString: String,
        url: String,
        blocks: List<BlockJson>,
        status: ArticleStatus
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val categoryId = "cat_${category.lowercase().replace(" ", "_")}"
            categoryDao.upsertCategories(
                listOf(CategoryEntity(id = categoryId, name = category, sortOrder = 0, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
            )

            val descriptionText = blocks.firstOrNull { it.type == "sapo" }?.data
            val normalizedUrl = url.trim()
            val existingArticle = articleDao.getArticleByUrl(normalizedUrl)

            val updatedArticle = ArticleEntity(
                url = normalizedUrl,
                title = title.trim(),
                urlToImage = thumbnail?.trim(),
                author = author.trim().takeIf { it.isNotBlank() } ?: "Ẩn danh",
                sourceId = source.lowercase().replace(" ", ""),
                sourceName = source,
                categoryId = categoryId,
                publishedAt = timeString,
                createdAt = existingArticle?.createdAt ?: System.currentTimeMillis(),
                view = existingArticle?.view ?: 0L,
                status = status.name,
                city = null,
                content = null,
                description = descriptionText
            )

            // REPLACE ON CONFLICT sẽ ghi đè bài báo cũ
            articleDao.insertArticles(listOf(updatedArticle))

            // ĐÃ SỬA: Xóa block cũ và chèn block mới (Không gọi lại addArticle nữa)
            articleBlockDao.deleteByArticleUrl(url)
            if (blocks.isNotEmpty()) {
                val blockEntities = ArticleMapper.toBlockEntities(blocks, url)
                articleBlockDao.insertAll(blockEntities)
            }
        }
    }

    // 5. XÓA BÀI BÁO
    suspend fun deleteArticle(url: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            articleDao.deleteArticleByUrl(url) // Dùng đúng hàm xóa bài báo
            articleBlockDao.deleteByArticleUrl(url)
        }
    }
}