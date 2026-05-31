package com.data.local.source

import com.data.model.Article

interface LocalNewsDataSource {
    suspend fun getHeadlinesCache(
        category: String?,
        country: String?,
        query: String?,
        sources: String?,
        page: Int,
        pageSize: Int,
        maxAgeMillis: Long? = null
    ): List<Article>

    suspend fun saveHeadlinesCache(
        category: String?,
        country: String?,
        query: String?,
        sources: String?,
        page: Int,
        pageSize: Int,
        articles: List<Article>
    )

    suspend fun getSearchCache(
        query: String,
        sortBy: String?,
        sources: String?,
        page: Int,
        pageSize: Int,
        maxAgeMillis: Long? = null
    ): List<Article>

    suspend fun saveSearchCache(
        query: String,
        sortBy: String?,
        sources: String?,
        page: Int,
        pageSize: Int,
        articles: List<Article>
    )

    suspend fun getCachedLocalNews(
        locationQuery: String?,
        country: String,
        maxAgeMillis: Long? = null
    ): List<Article>

    suspend fun saveCachedLocalNews(
        locationQuery: String?,
        country: String,
        cityTitle: String,
        articles: List<Article>
    )

    suspend fun getBookmarks(): List<Article>
    suspend fun saveBookmark(article: Article)
    suspend fun removeBookmark(articleUrl: String)
    suspend fun isBookmarked(articleUrl: String): Boolean
}
