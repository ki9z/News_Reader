package com.example.app_doc_bao.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.app_doc_bao.data.domain.ArticleStatus
import com.example.app_doc_bao.data.local.AppDatabase
import com.example.app_doc_bao.data.local.entity.Article
import com.example.app_doc_bao.data.local.entity.Category
import com.example.app_doc_bao.data.local.entity.Source
import com.example.app_doc_bao.data.mapper.ArticleMapper
import com.example.app_doc_bao.data.model.ArticleModel
import com.example.app_doc_bao.data.remote.JsonParser
import com.example.app_doc_bao.data.remote.dto.ArticleJson
import com.example.app_doc_bao.data.remote.dto.BlockJson
import com.util.toTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ArticleRepository(
    private val db: AppDatabase
) {
    private val categoryDao = db.categoryDao()
    private val articleDao = db.articleDao()
    private val blockDao = db.blockDao()
    private val sourceDao = db.sourceDao()

    // 1. NẠP DỮ LIỆU TỪ JSON
    suspend fun insertFromJson(jsonString: String) = withContext(Dispatchers.IO) {
        if (jsonString.isBlank()) return@withContext

        val articles = try {
            JsonParser.json.decodeFromString<List<ArticleJson>>(jsonString)
        } catch (e: Exception) {
            Log.e("JSON_PARSE_ERROR", e.message ?: "Unknown error")
            return@withContext
        }

        db.withTransaction {
            articles.forEach { articleJson ->
                val categoryId = categoryDao.findByName(articleJson.category)?.id
                    ?: categoryDao.insert(Category(name = articleJson.category))

                val sourceId = sourceDao.findByName(articleJson.source)?.id
                    ?: sourceDao.insert(Source(name = articleJson.source))

                val articleId = articleDao.insert(
                    ArticleMapper.toArticleEntity(articleJson, categoryId, sourceId)
                )

                if (articleId != -1L) {
                    val blocks = ArticleMapper.toBlockEntities(articleJson.blocks, articleId)
                    blockDao.insertAll(blocks)
                }
            }
        }
    }

    // 2. ĐỌC DỮ LIỆU
    fun getAllArticlesFlow(): Flow<List<ArticleModel>> {
        return articleDao.getAllArticlesWithDetailsFlow()
            .map { details ->
                details.map { ArticleMapper.toArticleModel(it) }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getAllArticles(): List<ArticleModel> = withContext(Dispatchers.IO) {
        articleDao.getAllArticlesWithDetails().map { ArticleMapper.toArticleModel(it) }
    }

    suspend fun getArticleById(id: Long): ArticleModel? = withContext(Dispatchers.IO) {
        articleDao.getArticleWithDetails(id)?.let { ArticleMapper.toArticleModel(it) }
    }

    suspend fun getAllCategoryNames(): List<String> = withContext(Dispatchers.IO) {
        categoryDao.getAll().map { it.name }
    }

    // 3. THÊM BÀI BÁO
    suspend fun addArticle(
        remoteId: String,
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
            val categoryId = categoryDao.findByName(category)?.id ?: categoryDao.insert(Category(name = category))
            val sourceId = sourceDao.findByName(source)?.id ?: sourceDao.insert(Source(name = source))

            val newArticle = Article(
                remoteId = remoteId.trim(),
                title = title.trim(),
                thumbnail = thumbnail?.trim(),
                author = author.trim().takeIf { it.isNotBlank() },
                sourceId = sourceId,
                categoryId = categoryId,
                publishAt = timeString.toTimestamp(),
                createdAt = System.currentTimeMillis(),
                view = 0L,
                status = status,
                url = url.trim()
            )

            val articleId = articleDao.insert(newArticle)
            if (articleId != -1L) {
                val blockEntities = ArticleMapper.toBlockEntities(blocks, articleId)
                blockDao.insertAll(blockEntities)
            }
        }
    }

    // 4. CẬP NHẬT
    suspend fun updateArticle(
        id: Long,
        remoteId: String,
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
            val existingArticle = articleDao.getById(id) ?: return@withTransaction

            val categoryId = categoryDao.findByName(category)?.id ?: categoryDao.insert(Category(name = category))
            val sourceId = sourceDao.findByName(source)?.id ?: sourceDao.insert(Source(name = source))

            val updatedArticle = existingArticle.copy(
                remoteId = remoteId.trim(),
                title = title.trim(),
                thumbnail = thumbnail?.trim(),
                author = author.trim().takeIf { it.isNotBlank() },
                sourceId = sourceId,
                categoryId = categoryId,
                publishAt = timeString.toTimestamp(),
                status = status,
                url = url.trim()
            )

            articleDao.update(updatedArticle)
            blockDao.deleteByArticleId(id)
            blockDao.insertAll(ArticleMapper.toBlockEntities(blocks, id))
        }
    }

    suspend fun deleteArticle(id: Long) = withContext(Dispatchers.IO) {
        articleDao.deleteById(id)
    }
}