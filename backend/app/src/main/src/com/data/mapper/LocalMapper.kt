package com.data.mapper

import com.data.local.entity.ArticleEntity
import com.data.model.Article
import com.data.model.Source

object LocalMapper {
    /**
     * Chuyển từ Model API (Article) sang Entity Database (ArticleEntity)
     */
    fun Article.toEntity(categoryId: String = "general"): ArticleEntity = ArticleEntity(
        url = url.orEmpty(),
        city = null,
        sourceId = source?.id,
        sourceName = source?.name,
        author = author,
        title = title,
        description = description,
        urlToImage = urlToImage,
        publishedAt = publishedAt,
        content = content,
        // Bổ sung các trường từ ArticleEntity đã gộp
        categoryId = categoryId, // Mặc định là "general" nếu không có
        view = 0L,
        status = "PUBLISHED",
        createdAt = System.currentTimeMillis()
    )

    /**
     * Chuyển từ Entity Database sang Model Article để hiển thị lên UI
     */
    fun ArticleEntity.toArticle(): Article = Article(
        source = Source(id = sourceId, name = sourceName),
        author = author,
        title = title,
        description = description,
        url = url,
        urlToImage = urlToImage,
        publishedAt = publishedAt,
        content = content
    )
}