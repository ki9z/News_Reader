package com.data.local.source

import com.data.local.dao.ArticleDao
import com.data.local.dao.LocalNewsCacheDao
import com.data.local.entity.LocalNewsCacheEntity
import com.data.mapper.LocalMapper.toArticle
import com.data.mapper.LocalMapper.toEntity
import com.data.model.Article
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class LocalNewsDataSourceImpl(
    private val articleDao: ArticleDao,
    private val localNewsCacheDao: LocalNewsCacheDao
) : LocalNewsDataSource {

    private val gson = Gson()

    private companion object {
        const val CACHE_PREFIX_TOP = "top"
        const val CACHE_PREFIX_SEARCH = "search"
    }

    override suspend fun getHeadlinesCache(
        category: String?,
        country: String?,
        query: String?,
        sources: String?,
        page: Int,
        pageSize: Int,
        maxAgeMillis: Long?
    ): List<Article> {
        val cacheKey = buildHeadlinesCacheKey(category, country, query, sources, page, pageSize)
        val cache = localNewsCacheDao.getByCacheKey(cacheKey) ?: return emptyList()
        if (!isCacheFresh(cache.updatedAt, maxAgeMillis)) return emptyList()
        return decodeArticles(cache.articlesJson)
    }

    override suspend fun saveHeadlinesCache(
        category: String?,
        country: String?,
        query: String?,
        sources: String?,
        page: Int,
        pageSize: Int,
        articles: List<Article>
    ) {
        if (articles.isEmpty()) return
        localNewsCacheDao.upsert(
            LocalNewsCacheEntity(
                cacheKey = buildHeadlinesCacheKey(category, country, query, sources, page, pageSize),
                cityTitle = category?.ifBlank { "Top headlines" } ?: "Top headlines",
                countryCode = "us",
                locationQuery = category,
                articlesJson = gson.toJson(articles),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getSearchCache(
        query: String,
        sortBy: String?,
        sources: String?,
        page: Int,
        pageSize: Int,
        maxAgeMillis: Long?
    ): List<Article> {
        val cacheKey = buildSearchCacheKey(query, sortBy, sources, page, pageSize)
        val cache = localNewsCacheDao.getByCacheKey(cacheKey) ?: return emptyList()
        if (!isCacheFresh(cache.updatedAt, maxAgeMillis)) return emptyList()
        return decodeArticles(cache.articlesJson)
    }

    override suspend fun saveSearchCache(
        query: String,
        sortBy: String?,
        sources: String?,
        page: Int,
        pageSize: Int,
        articles: List<Article>
    ) {
        if (articles.isEmpty()) return
        localNewsCacheDao.upsert(
            LocalNewsCacheEntity(
                cacheKey = buildSearchCacheKey(query, sortBy, sources, page, pageSize),
                cityTitle = query,
                countryCode = "us",
                locationQuery = query,
                articlesJson = gson.toJson(articles),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getCachedLocalNews(
        locationQuery: String?,
        country: String,
        maxAgeMillis: Long?
    ): List<Article> {
        val cacheKey = buildLocalCacheKey(locationQuery, country)
        val cache = localNewsCacheDao.getByCacheKey(cacheKey) ?: return emptyList()
        if (!isCacheFresh(cache.updatedAt, maxAgeMillis)) return emptyList()
        return decodeArticles(cache.articlesJson)
    }

    override suspend fun saveCachedLocalNews(
        locationQuery: String?,
        country: String,
        cityTitle: String,
        articles: List<Article>
    ) {
        if (articles.isEmpty()) return
        localNewsCacheDao.upsert(
            LocalNewsCacheEntity(
                cacheKey = buildLocalCacheKey(locationQuery, country),
                cityTitle = cityTitle,
                countryCode = country,
                locationQuery = locationQuery?.trim().takeUnless { it.isNullOrBlank() },
                articlesJson = gson.toJson(articles),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getBookmarks(): List<Article> {
        return articleDao.getAllBookmarks().map { it.toArticle() }
    }

    override suspend fun saveBookmark(article: Article) {
        val articleUrl = article.url.orEmpty().trim()
        if (articleUrl.isBlank()) return
        articleDao.upsertArticlePreservingStats(article.copy(url = articleUrl).toEntity())
    }

    override suspend fun removeBookmark(articleUrl: String) {
        articleDao.deleteBookmark(articleUrl)
    }

    override suspend fun isBookmarked(articleUrl: String): Boolean {
        return articleDao.isBookmarked(articleUrl) > 0
    }

    private fun isCacheFresh(updatedAt: Long, maxAgeMillis: Long?): Boolean {
        if (maxAgeMillis == null) return true
        if (maxAgeMillis <= 0L) return false
        return System.currentTimeMillis() - updatedAt <= maxAgeMillis
    }

    private fun buildLocalCacheKey(locationQuery: String?, country: String): String {
        val normalizedQuery = locationQuery?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return "${normalizedQuery.ifBlank { "country" }}|$country"
    }

    private fun buildHeadlinesCacheKey(
        category: String?,
        country: String?,
        query: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): String {
        val normalizedCategory = category?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "all" }
        val normalizedCountry = country?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "none" }
        val normalizedQuery = query?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "none" }
        val normalizedSources = sources?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "none" }
        return "$CACHE_PREFIX_TOP|$normalizedCategory|$normalizedCountry|$normalizedQuery|$normalizedSources|$page|$pageSize"
    }

    private fun buildSearchCacheKey(
        query: String,
        sortBy: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): String {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT).ifBlank { "all" }
        val normalizedSortBy = sortBy?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "default" }
        val normalizedSources = sources?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "none" }
        return "$CACHE_PREFIX_SEARCH|$normalizedQuery|$normalizedSortBy|$normalizedSources|$page|$pageSize"
    }

    private fun decodeArticles(json: String): List<Article> {
        val type = object : TypeToken<List<Article>>() {}.type
        return runCatching { gson.fromJson<List<Article>>(json, type) }.getOrDefault(emptyList())
    }
}
