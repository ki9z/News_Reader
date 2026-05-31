package com.data.repository

import com.data.local.dao.ArticleDao
import com.data.local.dao.BookmarkDao
import com.data.local.dao.UserDao
import com.data.local.db.DbMigrations
import com.data.local.entity.UserBookmarkEntity
import com.data.local.entity.UserEntity
import com.data.local.source.LocalNewsDataSource
import com.data.local.source.OfflineNewsDataSource
import com.data.mapper.LocalMapper.toArticle
import com.data.mapper.LocalMapper.toEntity
import com.data.local.seed.PreCrawledNewsDataSource
import com.data.model.Article
import com.data.model.LocalCityCatalog
import com.data.model.NewsSource
import com.data.model.SourcesResponse
import com.data.remote.client.NewsApiRequestCoordinator
import com.data.remote.client.NewsApiRequestGuard
import com.data.remote.source.RemoteNewsDataSource
import com.util.NetworkResult
import com.util.Constants
import com.util.NewsApiErrorParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.Normalizer
import java.util.Locale

class NewsRepositoryImpl(
    private val remoteDataSource: RemoteNewsDataSource,
    private val localDataSource: LocalNewsDataSource,
    private val offlineDataSource: OfflineNewsDataSource,
    private val articleDao: ArticleDao,
    private val bookmarkDao: BookmarkDao,
    private val userDao: UserDao,
    private val preCrawledNewsDataSource: PreCrawledNewsDataSource
) : NewsRepository {

    private companion object {
        const val CACHE_PREFIX_TOP = "top"
        const val CACHE_PREFIX_SEARCH = "search"
        const val DEFAULT_USER_ID: String = DbMigrations.DEFAULT_USER_ID

        const val TOP_HEADLINES_CACHE_TTL_MS = 15 * 60 * 1000L
        const val SEARCH_CACHE_TTL_MS = 60 * 60 * 1000L
        const val LOCAL_CACHE_TTL_MS = 3 * 60 * 60 * 1000L
        const val SOURCES_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        const val FALLBACK_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        const val NO_API_KEY_CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }

    private data class LocalProfile(
        val cityName: String,
        val aliases: Set<String>,
        val countryHints: Set<String>
    )

    private data class SourcesMemoryCache(
        val updatedAt: Long,
        val sources: List<NewsSource>
    )

    private val sourcesMemoryCache = mutableMapOf<String, SourcesMemoryCache>()

    private val localProfiles = LocalCityCatalog.majorCities.map { city ->
        LocalProfile(
            cityName = city.query,
            aliases = (city.aliases + city.query + city.title).map { it.trim() }.filter { it.isNotBlank() }.toSet(),
            countryHints = (city.countryHints + city.countryName + city.countryCode)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        )
    }

    private fun mapArticles(response: Response<com.data.model.NewsResponse>): NetworkResult<List<Article>> {
        if (response.isSuccessful) {
            val articles = response.body()?.articles
                ?.filterNot { article ->
                    article.title.isNullOrBlank() || article.title == "[Removed]"
                }
                ?.distinctBy { article ->
                    article.url?.trim().orEmpty().ifBlank {
                        "${article.title.orEmpty()}|${article.publishedAt.orEmpty()}"
                    }
                }
                ?: emptyList()

            return NetworkResult.Success(articles)
        }

        val code = response.code()
        val parsedMessage = NewsApiErrorParser.parseMessage(response)

        return when (code) {
            401 -> NetworkResult.Error(
                message = parsedMessage ?: "NEWS_API_KEY không hợp lệ hoặc chưa được cấp quyền.",
                code = code,
                type = NetworkResult.ErrorType.UNAUTHORIZED
            )

            426 -> NetworkResult.Error(
                message = parsedMessage ?: "Gói NewsAPI hiện tại không hỗ trợ yêu cầu này.",
                code = code,
                type = NetworkResult.ErrorType.CLIENT
            )

            429 -> NetworkResult.Error(
                message = parsedMessage ?: "Đã vượt giới hạn NewsAPI. Vui lòng thử lại sau.",
                code = code,
                type = NetworkResult.ErrorType.RATE_LIMITED
            )

            in 500..599 -> NetworkResult.Error(
                message = parsedMessage ?: "Máy chủ NewsAPI đang bận. Vui lòng thử lại sau.",
                code = code,
                type = NetworkResult.ErrorType.SERVER
            )

            in 400..499 -> NetworkResult.Error(
                message = parsedMessage ?: "Yêu cầu tin tức chưa hợp lệ. Vui lòng đổi bộ lọc hoặc từ khóa.",
                code = code,
                type = NetworkResult.ErrorType.CLIENT
            )

            else -> NetworkResult.Error(
                message = parsedMessage ?: "Không thể tải tin tức lúc này.",
                code = code,
                type = NetworkResult.ErrorType.UNKNOWN
            )
        }
    }

    private fun mapSources(response: Response<SourcesResponse>): NetworkResult<List<NewsSource>> {
        if (response.isSuccessful) {
            val sources = response.body()?.sources
                ?.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }
                ?.distinctBy { it.id!!.trim().lowercase(Locale.ROOT) }
                ?.sortedWith(compareBy<NewsSource> { it.country.orEmpty() }.thenBy { it.name.orEmpty() })
                ?: emptyList()

            return NetworkResult.Success(sources)
        }

        val code = response.code()
        val parsedMessage = NewsApiErrorParser.parseMessage(response)

        return when (code) {
            401 -> NetworkResult.Error(
                message = parsedMessage ?: "NEWS_API_KEY không hợp lệ hoặc chưa được cấp quyền.",
                code = code,
                type = NetworkResult.ErrorType.UNAUTHORIZED
            )

            429 -> NetworkResult.Error(
                message = parsedMessage ?: "Đã vượt giới hạn NewsAPI. Vui lòng thử lại sau.",
                code = code,
                type = NetworkResult.ErrorType.RATE_LIMITED
            )

            in 500..599 -> NetworkResult.Error(
                message = parsedMessage ?: "Máy chủ NewsAPI đang bận. Vui lòng thử lại sau.",
                code = code,
                type = NetworkResult.ErrorType.SERVER
            )

            else -> NetworkResult.Error(
                message = parsedMessage ?: "Không thể tải danh sách nguồn tin lúc này.",
                code = code,
                type = NetworkResult.ErrorType.UNKNOWN
            )
        }
    }

    private fun mapException(exception: Exception): NetworkResult.Error {
        return when (exception) {
            is SocketTimeoutException -> NetworkResult.Error(
                message = "Kết nối quá thời gian. Vui lòng thử lại.",
                type = NetworkResult.ErrorType.NETWORK
            )

            is IOException -> NetworkResult.Error(
                message = "Không có kết nối mạng. Đang thử dùng dữ liệu đã lưu nếu có.",
                type = NetworkResult.ErrorType.NETWORK
            )

            else -> NetworkResult.Error(
                message = "Không thể tải tin tức. Vui lòng thử lại sau.",
                type = NetworkResult.ErrorType.UNKNOWN
            )
        }
    }

    private suspend fun <T> guardedNewsApiRequest(
        requestKey: String,
        block: suspend () -> Response<T>
    ): NetworkResult<Response<T>> {
        return NewsApiRequestCoordinator.runDeduplicated(requestKey) {
            val permitError = NewsApiRequestGuard.acquirePermit(requestKey)
            if (permitError != null) {
                return@runDeduplicated permitError
            }

            try {
                val response = block()
                NewsApiRequestGuard.recordHttpResponse(response.code())
                NetworkResult.Success(response)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                NewsApiRequestGuard.recordException(e)
                mapException(e)
            }
        }
    }

    private suspend fun guardedArticleRequest(
        requestKey: String,
        block: suspend () -> Response<com.data.model.NewsResponse>
    ): NetworkResult<List<Article>> {
        return when (val responseResult = guardedNewsApiRequest(requestKey, block)) {
            is NetworkResult.Success -> mapArticles(responseResult.data)
            is NetworkResult.Error -> responseResult
        }
    }


    private fun requireNewsApiKey(): NetworkResult.Error? {
        return if (!Constants.CAN_CALL_REMOTE_NEWS) {
            NetworkResult.Error(
                message = "Thiếu cấu hình nguồn tin. Hãy thêm NEWS_API_KEY để gọi NewsAPI trực tiếp hoặc đặt NEWS_BASE_URL tới backend proxy.",
                type = NetworkResult.ErrorType.CLIENT
            )
        } else {
            null
        }
    }

    override suspend fun getTopHeadlines(
        category: String?,
        country: String,
        query: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): NetworkResult<List<Article>> {
        return try {
            val normalizedSources = sources?.trim().takeUnless { it.isNullOrBlank() }
            val isSourceScoped = normalizedSources != null
            val isVietnamFeed = normalizedSources == null && country.equals("vn", ignoreCase = true)
            val requestCategory = if (normalizedSources != null) null else category
            val requestCountry = when {
                normalizedSources != null -> null
                isVietnamFeed -> null
                else -> normalizeCountryForTopHeadlines(country)
            }
            val requestQuery = if (isVietnamFeed && query.isNullOrBlank()) {
                "Vietnam OR \"Viet Nam\" OR \"Việt Nam\""
            } else {
                query
            }

            if (!Constants.CAN_CALL_REMOTE_NEWS) {
                val cached = localDataSource.getHeadlinesCache(
                    category = requestCategory,
                    country = requestCountry,
                    query = requestQuery,
                    sources = normalizedSources,
                    page = page,
                    pageSize = pageSize,
                    maxAgeMillis = NO_API_KEY_CACHE_TTL_MS
                )
                if (cached.isNotEmpty()) return NetworkResult.Success(cached)

                return if (isSourceScoped) {
                    requireNewsApiKey()!!
                } else {
                    val offlinePool = pickVietnameseCandidatesForTopHeadlines(category, page, pageSize)
                    if (offlinePool.isNotEmpty()) {
                        NetworkResult.Success(offlinePool.take(pageSize))
                    } else {
                        requireNewsApiKey()!!
                    }
                }
            }

            val freshCache = localDataSource.getHeadlinesCache(
                category = requestCategory,
                country = requestCountry,
                query = requestQuery,
                sources = normalizedSources,
                page = page,
                pageSize = pageSize,
                maxAgeMillis = TOP_HEADLINES_CACHE_TTL_MS
            )
            if (freshCache.isNotEmpty()) return NetworkResult.Success(freshCache)

            val requestKey = buildTopRequestKey(
                category = requestCategory,
                country = requestCountry,
                query = requestQuery,
                sources = normalizedSources,
                page = page,
                pageSize = pageSize
            )

            when (val result = guardedArticleRequest(requestKey) {
                remoteDataSource.getTopHeadlines(
                    category = requestCategory,
                    query = requestQuery,
                    country = requestCountry,
                    sources = normalizedSources,
                    page = page,
                    pageSize = pageSize
                )
            }) {
                is NetworkResult.Success -> {
                    val items = if (isSourceScoped) {
                        dedupeArticles(result.data).take(pageSize)
                    } else {
                        mixArticlesWithVietnamese(
                            primaryArticles = result.data,
                            vietnameseCandidates = pickVietnameseCandidatesForTopHeadlines(category, page, pageSize),
                            pageSize = pageSize,
                            vietnameseRatio = 0.4f
                        )
                    }
                    localDataSource.saveHeadlinesCache(
                        category = requestCategory,
                        country = requestCountry,
                        query = requestQuery,
                        sources = normalizedSources,
                        page = page,
                        pageSize = pageSize,
                        articles = items
                    )
                    NetworkResult.Success(items)
                }
                is NetworkResult.Error -> {
                    val cached = localDataSource.getHeadlinesCache(
                        category = requestCategory,
                        country = requestCountry,
                        query = requestQuery,
                        sources = normalizedSources,
                        page = page,
                        pageSize = pageSize,
                        maxAgeMillis = FALLBACK_CACHE_TTL_MS
                    )
                    if (cached.isNotEmpty()) {
                        NetworkResult.Success(cached)
                    } else {
                        if (isSourceScoped) {
                            result
                        } else {
                            val offlinePool = pickVietnameseCandidatesForTopHeadlines(category, page, pageSize)
                            if (offlinePool.isNotEmpty()) NetworkResult.Success(offlinePool.take(pageSize)) else result
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val normalizedSources = sources?.trim().takeUnless { it.isNullOrBlank() }
            val isVietnamFeed = normalizedSources == null && country.equals("vn", ignoreCase = true)
            val requestCategory = if (normalizedSources != null) null else category
            val requestCountry = when {
                normalizedSources != null -> null
                isVietnamFeed -> null
                else -> normalizeCountryForTopHeadlines(country)
            }
            val requestQuery = if (isVietnamFeed && query.isNullOrBlank()) {
                "Vietnam OR \"Viet Nam\" OR \"Việt Nam\""
            } else {
                query
            }
            val fallback = localDataSource.getHeadlinesCache(
                category = requestCategory,
                country = requestCountry,
                query = requestQuery,
                sources = normalizedSources,
                page = page,
                pageSize = pageSize,
                maxAgeMillis = FALLBACK_CACHE_TTL_MS
            )
            if (fallback.isNotEmpty()) {
                NetworkResult.Success(fallback)
            } else {
                if (normalizedSources != null) {
                    mapException(e)
                } else {
                    val offlinePool = pickVietnameseCandidatesForTopHeadlines(category, page, pageSize)
                    if (offlinePool.isNotEmpty()) NetworkResult.Success(offlinePool.take(pageSize)) else mapException(e)
                }
            }
        }
    }

    override suspend fun searchNews(
        query: String,
        sortBy: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): NetworkResult<List<Article>> {
        return try {
            val normalizedSources = sources?.trim().takeUnless { it.isNullOrBlank() }
            val normalizedSortBy = sortBy?.trim().takeUnless { it.isNullOrBlank() }

            if (!Constants.CAN_CALL_REMOTE_NEWS) {
                val cached = localDataSource.getSearchCache(query, normalizedSortBy, normalizedSources, page, pageSize, NO_API_KEY_CACHE_TTL_MS)
                if (cached.isNotEmpty()) return NetworkResult.Success(cached)

                return if (normalizedSources != null) {
                    requireNewsApiKey()!!
                } else {
                    val offlinePool = pickVietnameseCandidatesForSearch(query, page, pageSize)
                    if (offlinePool.isNotEmpty()) {
                        NetworkResult.Success(offlinePool.take(pageSize))
                    } else {
                        requireNewsApiKey()!!
                    }
                }
            }

            val freshCache = localDataSource.getSearchCache(
                query = query,
                sortBy = normalizedSortBy,
                sources = normalizedSources,
                page = page,
                pageSize = pageSize,
                maxAgeMillis = SEARCH_CACHE_TTL_MS
            )
            if (freshCache.isNotEmpty()) return NetworkResult.Success(freshCache)

            val requestKey = buildSearchRequestKey(
                query = query,
                sortBy = normalizedSortBy,
                sources = normalizedSources,
                page = page,
                pageSize = pageSize
            )

            when (val result = guardedArticleRequest(requestKey) {
                remoteDataSource.searchNews(
                    query = query,
                    sortBy = normalizedSortBy,
                    sources = normalizedSources,
                    page = page,
                    pageSize = pageSize
                )
            }) {
                is NetworkResult.Success -> {
                    val items = if (normalizedSources != null) {
                        dedupeArticles(result.data).take(pageSize)
                    } else {
                        mixArticlesWithVietnamese(
                            primaryArticles = result.data,
                            vietnameseCandidates = pickVietnameseCandidatesForSearch(query, page, pageSize),
                            pageSize = pageSize,
                            vietnameseRatio = 0.35f
                        )
                    }
                    localDataSource.saveSearchCache(query, normalizedSortBy, normalizedSources, page, pageSize, items)
                    NetworkResult.Success(items)
                }
                is NetworkResult.Error -> {
                    val cached = localDataSource.getSearchCache(query, normalizedSortBy, normalizedSources, page, pageSize, FALLBACK_CACHE_TTL_MS)
                    if (cached.isNotEmpty()) {
                        NetworkResult.Success(cached)
                    } else {
                        if (normalizedSources != null) {
                            result
                        } else {
                            val offlinePool = pickVietnameseCandidatesForSearch(query, page, pageSize)
                            if (offlinePool.isNotEmpty()) NetworkResult.Success(offlinePool.take(pageSize)) else result
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val normalizedSources = sources?.trim().takeUnless { it.isNullOrBlank() }
            val normalizedSortBy = sortBy?.trim().takeUnless { it.isNullOrBlank() }
            val fallback = localDataSource.getSearchCache(query, normalizedSortBy, normalizedSources, page, pageSize, FALLBACK_CACHE_TTL_MS)
            if (fallback.isNotEmpty()) {
                NetworkResult.Success(fallback)
            } else {
                if (normalizedSources != null) {
                    mapException(e)
                } else {
                    val offlinePool = pickVietnameseCandidatesForSearch(query, page, pageSize)
                    if (offlinePool.isNotEmpty()) NetworkResult.Success(offlinePool.take(pageSize)) else mapException(e)
                }
            }
        }
    }

    override suspend fun getLocalNews(
        locationQuery: String?,
        country: String,
        page: Int,
        pageSize: Int
    ): NetworkResult<List<Article>> {
        return try {
            val normalizedQuery = locationQuery?.trim().takeUnless { it.isNullOrBlank() }

            if (normalizedQuery != null) {
                val localProfile = resolveLocalProfile(normalizedQuery)
                if (!Constants.CAN_CALL_REMOTE_NEWS) {
                    return NetworkResult.Success(
                        pickVietnameseCandidatesForLocal(normalizedQuery, page, pageSize)
                            .filter { article -> matchesLocalProfile(article, localProfile) }
                            .take(pageSize)
                    )
                }

                when (val strictLocal = fetchStrictLocalNews(normalizedQuery, page, pageSize)) {
                    is NetworkResult.Success -> {
                        val mixed = mixArticlesWithVietnamese(
                            primaryArticles = strictLocal.data,
                            vietnameseCandidates = pickVietnameseCandidatesForLocal(normalizedQuery, page, pageSize),
                            pageSize = pageSize,
                            vietnameseRatio = 0.4f
                        )
                        if (mixed.isNotEmpty()) {
                            if (page == Constants.INITIAL_PAGE) {
                                saveCachedLocalNews(normalizedQuery, country, normalizedQuery, mixed)
                            }
                            return NetworkResult.Success(mixed)
                        }
                    }
                    is NetworkResult.Error -> {
                        if (isCriticalError(strictLocal)) return strictLocal
                    }
                }

                return NetworkResult.Success(
                    pickVietnameseCandidatesForLocal(normalizedQuery, page, pageSize)
                        .filter { article -> matchesLocalProfile(article, localProfile) }
                        .take(pageSize)
                )
            }

            if (!Constants.CAN_CALL_REMOTE_NEWS) {
                val offlinePool = pickVietnameseCandidatesForTopHeadlines("local", page, pageSize)
                return if (offlinePool.isNotEmpty()) {
                    NetworkResult.Success(offlinePool.take(pageSize))
                } else {
                    requireNewsApiKey()!!
                }
            }

            val normalizedCountry = normalizeCountryForTopHeadlines(country)
            val requestKey = buildTopRequestKey(
                category = null,
                country = normalizedCountry,
                query = null,
                sources = null,
                page = page,
                pageSize = pageSize
            )

            when (val result = guardedArticleRequest(requestKey) {
                remoteDataSource.getTopHeadlines(
                    category = null,
                    query = null,
                    country = normalizedCountry,
                    page = page,
                    pageSize = pageSize
                )
            }) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(
                        mixArticlesWithVietnamese(
                            primaryArticles = result.data,
                            vietnameseCandidates = pickVietnameseCandidatesForTopHeadlines("local", page, pageSize),
                            pageSize = pageSize,
                            vietnameseRatio = 0.5f
                        )
                    )
                }
                is NetworkResult.Error -> {
                    val offlinePool = pickVietnameseCandidatesForTopHeadlines("local", page, pageSize)
                    if (offlinePool.isNotEmpty()) NetworkResult.Success(offlinePool.take(pageSize)) else result
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val offlinePool = pickVietnameseCandidatesForTopHeadlines("local", page, pageSize)
            if (offlinePool.isNotEmpty()) NetworkResult.Success(offlinePool.take(pageSize)) else mapException(e)
        }
    }

    override suspend fun getCachedLocalNews(
        locationQuery: String?,
        country: String
    ): List<Article> {
        val cached = localDataSource.getCachedLocalNews(locationQuery, country, LOCAL_CACHE_TTL_MS)
        val normalizedQuery = locationQuery?.trim().takeUnless { it.isNullOrBlank() } ?: return cached
        val profile = resolveLocalProfile(normalizedQuery)
        return cached.filter { article ->
            matchesLocalProfile(article, profile) && isLanguageSuitableForLocal(article, profile)
        }
    }

    override suspend fun saveCachedLocalNews(
        locationQuery: String?,
        country: String,
        cityTitle: String,
        articles: List<Article>
    ) {
        localDataSource.saveCachedLocalNews(locationQuery, country, cityTitle, articles)
    }

    override suspend fun getBookmarks(): List<Article> {
        ensureDefaultUser()
        return bookmarkDao.observeBookmarkedArticles(DEFAULT_USER_ID).first().map { it.toArticle() }
    }

    override suspend fun saveBookmark(article: Article) {
        val articleUrl = article.url?.trim().orEmpty()
        if (articleUrl.isBlank()) return

        ensureDefaultUser()

        articleDao.upsertArticlePreservingStats(article.copy(url = articleUrl).toEntity())

        bookmarkDao.upsert(
            UserBookmarkEntity(
                userId = DEFAULT_USER_ID,
                articleUrl = articleUrl,
                savedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeBookmark(articleUrl: String) {
        val normalizedUrl = articleUrl.trim()
        if (normalizedUrl.isBlank()) return

        ensureDefaultUser()
        bookmarkDao.delete(DEFAULT_USER_ID, normalizedUrl)
    }

    override suspend fun isBookmarked(articleUrl: String): Boolean {
        val normalizedUrl = articleUrl.trim()
        if (normalizedUrl.isBlank()) return false

        ensureDefaultUser()
        return bookmarkDao.count(DEFAULT_USER_ID, normalizedUrl) > 0
    }

    private suspend fun ensureDefaultUser() {
        if (userDao.getById(DEFAULT_USER_ID) != null) return

        val now = System.currentTimeMillis()

        userDao.upsert(
            UserEntity(
                id = DEFAULT_USER_ID,
                email = "guest@newsreader.app",
                fullName = "Guest User",
                role = "guest",
                isSignedIn = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun normalizeCountryForTopHeadlines(country: String): String {
        val normalized = country.lowercase()
        val supportedCountries = setOf(
            "ar", "au", "at", "be", "br", "bg", "ca", "cn", "co", "cu", "cz", "eg",
            "fr", "de", "gr", "hk", "hu", "in", "id", "ie", "il", "it", "jp", "lv",
            "lt", "my", "mx", "ma", "nl", "nz", "ng", "no", "ph", "pl", "pt", "ro",
            "ru", "sa", "rs", "sg", "sk", "si", "za", "kr", "se", "ch", "tw", "th",
            "tr", "ae", "ua", "gb", "us", "ve"
        )

        return if (normalized in supportedCountries) normalized else "us"
    }

    private suspend fun fetchStrictLocalNews(
        locationQuery: String,
        page: Int,
        pageSize: Int
    ): NetworkResult<List<Article>> {
        val profile = resolveLocalProfile(locationQuery)
        val queries = buildLocalQueries(profile)
        val collected = mutableListOf<Article>()
        val dedupeKeys = mutableSetOf<String>()

        for (query in queries) {
            val requestKey = buildSearchRequestKey(
                query = query,
                sortBy = null,
                sources = null,
                page = page,
                pageSize = pageSize
            )
            when (val result = guardedArticleRequest(requestKey) {
                remoteDataSource.searchNews(query = query, page = page, pageSize = pageSize)
            }) {
                is NetworkResult.Success -> {
                    val filtered = result.data.filter { article ->
                        matchesLocalProfile(article, profile) && isLanguageSuitableForLocal(article, profile)
                    }
                    filtered.forEach { article ->
                        val key = article.url.orEmpty().ifBlank {
                            "${article.title.orEmpty()}|${article.publishedAt.orEmpty()}"
                        }
                        if (dedupeKeys.add(key)) {
                            collected.add(article)
                        }
                    }
                    if (collected.size >= pageSize) break
                }
                is NetworkResult.Error -> {
                    if (isCriticalError(result)) return result
                }
            }
        }

        if (collected.isEmpty()) {
            val fallbackRequestKey = buildSearchRequestKey(
                query = profile.cityName,
                sortBy = null,
                sources = null,
                page = page,
                pageSize = pageSize
            )
            when (val fallback = guardedArticleRequest(fallbackRequestKey) {
                remoteDataSource.searchNews(query = profile.cityName, page = page, pageSize = pageSize)
            }) {
                is NetworkResult.Success -> {
                    val fallbackFiltered = fallback.data.filter { article ->
                        matchesCityOnly(article, profile) && isLanguageSuitableForLocal(article, profile)
                    }
                    return NetworkResult.Success(fallbackFiltered.take(pageSize))
                }
                is NetworkResult.Error -> {
                    if (isCriticalError(fallback)) return fallback
                }
            }
        }

        return NetworkResult.Success(collected.take(pageSize))
    }

    private fun resolveLocalProfile(locationQuery: String): LocalProfile {
        val normalized = normalizeText(locationQuery)
        return localProfiles.firstOrNull { profile ->
            profile.aliases.any { alias -> normalized.contains(normalizeText(alias)) }
        } ?: LocalProfile(
            cityName = locationQuery,
            aliases = setOf(locationQuery),
            countryHints = emptySet()
        )
    }

    private fun buildLocalQueries(profile: LocalProfile): List<String> {
        val aliases = profile.aliases.toList().sortedByDescending { it.length }.take(3)
        val countryPart = profile.countryHints
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" OR ") { "\"$it\"" }

        val countryHint = profile.countryHints.firstOrNull().orEmpty()
        val strictQueries = aliases.flatMap { alias ->
            val list = mutableListOf<String>()
            list += "\"$alias\""
            if (!countryPart.isNullOrBlank()) {
                list += "\"$alias\" AND ($countryPart)"
            }
            if (countryHint.isNotBlank()) {
                list += "$alias $countryHint"
            }
            list
        }

        val broadQueries = mutableListOf(profile.cityName)
        if (countryHint.isNotBlank()) {
            broadQueries += "${profile.cityName} $countryHint"
        }

        return (strictQueries + broadQueries).distinct()
    }

    private fun pickVietnameseCandidatesForTopHeadlines(
        category: String?,
        page: Int,
        pageSize: Int
    ): List<Article> {
        val pool = preCrawledNewsDataSource.getAllArticles()
        val filtered = category?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { categoryValue -> filterVietnameseArticlesByKeyword(pool, categoryValue) }
            ?.takeIf { it.isNotEmpty() }
            ?: pool
        return rotateForPage(filtered, key = "top:${category.orEmpty()}", page = page, pageSize = pageSize)
    }

    private fun pickVietnameseCandidatesForSearch(
        query: String,
        page: Int,
        pageSize: Int
    ): List<Article> {
        val pool = preCrawledNewsDataSource.getAllArticles()
        val filtered = filterVietnameseArticlesByKeyword(pool, query)
            .takeIf { it.isNotEmpty() }
            ?: pool
        return rotateForPage(filtered, key = "search:$query", page = page, pageSize = pageSize)
    }

    private fun pickVietnameseCandidatesForLocal(
        query: String,
        page: Int,
        pageSize: Int
    ): List<Article> {
        val pool = preCrawledNewsDataSource.getAllArticles()
        val filtered = filterVietnameseArticlesByKeyword(pool, query)
        if (filtered.isEmpty()) return emptyList()
        return rotateForPage(filtered, key = "local:$query", page = page, pageSize = pageSize)
    }

    private fun filterVietnameseArticlesByKeyword(articles: List<Article>, keyword: String): List<Article> {
        val normalizedKeyword = normalizeText(keyword)
        if (normalizedKeyword.isBlank()) return articles

        val expandedKeywords = buildSet {
            add(normalizedKeyword)
            when (normalizedKeyword) {
                "business" -> addAll(listOf("kinh te", "doanh nghiep", "tai chinh"))
                "sports" -> addAll(listOf("the thao", "bong da"))
                "technology" -> addAll(listOf("cong nghe", "tri tue nhan tao"))
                "health" -> addAll(listOf("suc khoe", "y te"))
                "science" -> addAll(listOf("khoa hoc", "vu tru"))
                "entertainment" -> addAll(listOf("giai tri", "phim", "am nhac"))
                "general" -> addAll(listOf("thoi su", "the gioi"))
            }
        }

        return articles.filter { article ->
            val haystack = normalizeText(
                listOf(
                    article.title,
                    article.description,
                    article.content,
                    article.source?.name,
                    article.author
                ).joinToString(" ") { it.orEmpty() }
            )
            expandedKeywords.any { key -> key.isNotBlank() && haystack.contains(key) }
        }
    }

    private fun rotateForPage(
        articles: List<Article>,
        key: String,
        page: Int,
        pageSize: Int
    ): List<Article> {
        if (articles.isEmpty()) return emptyList()

        val deduped = dedupeArticles(articles)
        if (deduped.isEmpty()) return emptyList()

        val safePage = page.coerceAtLeast(Constants.INITIAL_PAGE)
        val offset = ((key.hashCode() and Int.MAX_VALUE) + (safePage - 1) * pageSize) % deduped.size
        return deduped.drop(offset) + deduped.take(offset)
    }

    private fun mixArticlesWithVietnamese(
        primaryArticles: List<Article>,
        vietnameseCandidates: List<Article>,
        pageSize: Int,
        vietnameseRatio: Float
    ): List<Article> {
        if (pageSize <= 0) return emptyList()

        val uniquePrimary = dedupeArticles(primaryArticles)
        val uniqueVietnamese = dedupeArticles(vietnameseCandidates)

        if (uniquePrimary.isEmpty()) return uniqueVietnamese.take(pageSize)
        if (uniqueVietnamese.isEmpty()) return uniquePrimary.take(pageSize)

        val targetVietnamese = (pageSize * vietnameseRatio).toInt().coerceAtLeast(1).coerceAtMost(pageSize)
        val result = mutableListOf<Article>()
        val seenKeys = mutableSetOf<String>()

        uniquePrimary.take(pageSize).forEach { article ->
            if (result.size >= pageSize - targetVietnamese) return@forEach
            if (seenKeys.add(articleDedupKey(article))) result += article
        }

        uniqueVietnamese.forEach { article ->
            if (result.size >= pageSize) return@forEach
            if (seenKeys.add(articleDedupKey(article))) result += article
        }

        uniquePrimary.forEach { article ->
            if (result.size >= pageSize) return@forEach
            if (seenKeys.add(articleDedupKey(article))) result += article
        }

        return result.take(pageSize)
    }

    private fun dedupeArticles(articles: List<Article>): List<Article> {
        val deduped = mutableListOf<Article>()
        val seen = mutableSetOf<String>()

        articles.forEach { article ->
            val key = articleDedupKey(article)
            if (seen.add(key)) deduped += article
        }

        return deduped
    }

    private fun articleDedupKey(article: Article): String {
        val url = article.url?.trim().orEmpty()
        if (url.isNotEmpty()) return url
        return normalizeText("${article.title.orEmpty()}|${article.publishedAt.orEmpty()}")
    }

    private fun matchesLocalProfile(article: Article, profile: LocalProfile): Boolean {
        val fullText = listOf(
            article.title,
            article.description,
            article.content,
            article.source?.name,
            article.author
        ).joinToString(" ") { it.orEmpty() }
        val normalizedText = normalizeText(fullText)

        val cityMatched = profile.aliases.any { alias ->
            val normalizedAlias = normalizeText(alias)
            normalizedAlias.isNotBlank() && normalizedText.contains(normalizedAlias)
        }
        if (!cityMatched) return false

        if (profile.countryHints.isEmpty()) return true

        val countryMatched = profile.countryHints.any { hint ->
            val normalizedHint = normalizeText(hint)
            normalizedHint.isNotBlank() && normalizedText.contains(normalizedHint)
        }

        return countryMatched || profile.aliases.any { it.length >= 8 && normalizedText.contains(normalizeText(it)) }
    }

    private fun matchesCityOnly(article: Article, profile: LocalProfile): Boolean {
        val fullText = listOf(
            article.title,
            article.description,
            article.content,
            article.source?.name,
            article.author
        ).joinToString(" ") { it.orEmpty() }
        val normalizedText = normalizeText(fullText)

        return profile.aliases.any { alias ->
            val normalizedAlias = normalizeText(alias)
            normalizedAlias.isNotBlank() && normalizedText.contains(normalizedAlias)
        }
    }

    private fun isLanguageSuitableForLocal(article: Article, profile: LocalProfile): Boolean {
        if (!profile.countryHints.any { hint -> hint in setOf("vietnam", "viet nam", "vn") }) {
            return true
        }

        val text = listOf(article.title, article.description, article.content)
            .joinToString(" ") { it.orEmpty() }
        if (text.isBlank()) return true

        val cjkCount = text.count { ch ->
            when (Character.UnicodeScript.of(ch.code)) {
                Character.UnicodeScript.HAN,
                Character.UnicodeScript.HIRAGANA,
                Character.UnicodeScript.KATAKANA -> true
                else -> false
            }
        }
        if (cjkCount == 0) return true

        val latinCount = text.count { ch -> Character.UnicodeScript.of(ch.code) == Character.UnicodeScript.LATIN }
        return cjkCount <= latinCount
    }

    private fun isCriticalError(error: NetworkResult.Error): Boolean {
        return error.type in setOf(
            NetworkResult.ErrorType.UNAUTHORIZED,
            NetworkResult.ErrorType.RATE_LIMITED,
            NetworkResult.ErrorType.SERVER,
            NetworkResult.ErrorType.NETWORK
        )
    }

    private fun normalizeText(value: String): String {
        val strippedAccents = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

        return strippedAccents
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun buildLocalCacheKey(locationQuery: String?, country: String): String {
        val normalizedQuery = locationQuery?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return "${normalizedQuery.ifBlank { "country" }}|${normalizeCountryForTopHeadlines(country)}"
    }
    override suspend fun incrementArticleView(articleUrl: String) {
        articleDao.incrementArticleView(articleUrl.trim())
    }

    override suspend fun recordOriginalArticleOpen(article: Article) {
        val articleUrl = article.url?.trim().orEmpty()
        if (articleUrl.isBlank()) return

        articleDao.upsertArticlePreservingStats(article.copy(url = articleUrl).toEntity())
        articleDao.incrementArticleView(articleUrl)
    }

    override suspend fun getSources(
        category: String?,
        language: String?,
        country: String?
    ): NetworkResult<List<NewsSource>> {
        val fallback = defaultNewsSources()
        if (!Constants.CAN_CALL_REMOTE_NEWS) {
            return NetworkResult.Success(filterSources(fallback, category, language, country))
        }

        return try {
            val requestCategory = category?.trim().takeUnless { it.isNullOrBlank() }
            val requestLanguage = language?.trim().takeUnless { it.isNullOrBlank() }
            val requestCountry = country?.trim().takeUnless { it.isNullOrBlank() }
            val requestKey = buildSourcesRequestKey(requestCategory, requestLanguage, requestCountry)

            getFreshSourcesFromMemory(requestKey)?.let { cachedSources ->
                return NetworkResult.Success(filterSources(cachedSources, category, language, country))
            }

            when (val result = when (val responseResult = guardedNewsApiRequest(requestKey) {
                remoteDataSource.getSources(
                    category = requestCategory,
                    language = requestLanguage,
                    country = requestCountry
                )
            }) {
                is NetworkResult.Success -> mapSources(responseResult.data)
                is NetworkResult.Error -> responseResult
            }) {
                is NetworkResult.Success -> {
                    if (result.data.isNotEmpty()) {
                        saveSourcesToMemory(requestKey, result.data)
                    }
                    val liveSources = result.data.takeIf { it.isNotEmpty() } ?: fallback
                    NetworkResult.Success(filterSources(liveSources, category, language, country))
                }
                is NetworkResult.Error -> {
                    val filteredFallback = filterSources(fallback, category, language, country)
                    if (filteredFallback.isNotEmpty()) NetworkResult.Success(filteredFallback) else result
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val filteredFallback = filterSources(fallback, category, language, country)
            if (filteredFallback.isNotEmpty()) NetworkResult.Success(filteredFallback) else mapException(e)
        }
    }

    @Synchronized
    private fun getFreshSourcesFromMemory(requestKey: String): List<NewsSource>? {
        val cached = sourcesMemoryCache[requestKey] ?: return null
        val isFresh = System.currentTimeMillis() - cached.updatedAt <= SOURCES_CACHE_TTL_MS
        return if (isFresh) cached.sources else null
    }

    @Synchronized
    private fun saveSourcesToMemory(requestKey: String, sources: List<NewsSource>) {
        sourcesMemoryCache[requestKey] = SourcesMemoryCache(
            updatedAt = System.currentTimeMillis(),
            sources = sources
        )
    }

    private fun filterSources(
        sources: List<NewsSource>,
        category: String?,
        language: String?,
        country: String?
    ): List<NewsSource> {
        val categoryValue = category?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val languageValue = language?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val countryValue = country?.trim()?.lowercase(Locale.ROOT).orEmpty()

        return sources.filter { source ->
            val categoryMatches = categoryValue.isBlank() || source.category.orEmpty().equals(categoryValue, ignoreCase = true)
            val languageMatches = languageValue.isBlank() || source.language.orEmpty().equals(languageValue, ignoreCase = true)
            val countryMatches = countryValue.isBlank() || source.country.orEmpty().equals(countryValue, ignoreCase = true)
            categoryMatches && languageMatches && countryMatches
        }
    }

    private fun buildTopRequestKey(
        category: String?,
        country: String?,
        query: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): String {
        return listOf(
            CACHE_PREFIX_TOP,
            category?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "all" },
            country?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "none" },
            query?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "none" },
            sources?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "none" },
            page.toString(),
            pageSize.toString()
        ).joinToString("|")
    }

    private fun buildSearchRequestKey(
        query: String,
        sortBy: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): String {
        return listOf(
            CACHE_PREFIX_SEARCH,
            query.trim().lowercase(Locale.ROOT).ifBlank { "all" },
            sortBy?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "default" },
            sources?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "none" },
            page.toString(),
            pageSize.toString()
        ).joinToString("|")
    }

    private fun buildSourcesRequestKey(
        category: String?,
        language: String?,
        country: String?
    ): String {
        return listOf(
            "sources",
            category?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "all" },
            language?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "all" },
            country?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "all" }
        ).joinToString("|")
    }

    private fun defaultNewsSources(): List<NewsSource> {
        return listOf(
            NewsSource("bbc-news", "BBC News", "International news from BBC.", "https://www.bbc.co.uk/news", "general", "en", "gb"),
            NewsSource("cnn", "CNN", "Breaking news and live coverage.", "https://www.cnn.com", "general", "en", "us"),
            NewsSource("reuters", "Reuters", "Global business and world news.", "https://www.reuters.com", "general", "en", "us"),
            NewsSource("associated-press", "Associated Press", "Independent global news reporting.", "https://apnews.com", "general", "en", "us"),
            NewsSource("techcrunch", "TechCrunch", "Startup and technology news.", "https://techcrunch.com", "technology", "en", "us"),
            NewsSource("the-verge", "The Verge", "Technology, science, art, and culture.", "https://www.theverge.com", "technology", "en", "us"),
            NewsSource("business-insider", "Business Insider", "Business and markets news.", "https://www.businessinsider.com", "business", "en", "us"),
            NewsSource("espn", "ESPN", "Sports news and analysis.", "https://www.espn.com", "sports", "en", "us"),
            NewsSource("medical-news-today", "Medical News Today", "Health and medical news.", "https://www.medicalnewstoday.com", "health", "en", "us"),
            NewsSource("entertainment-weekly", "Entertainment Weekly", "Entertainment news.", "https://ew.com", "entertainment", "en", "us")
        )
    }
}

