package com.data.repository

import android.content.Context
import android.os.Build
import androidx.room.withTransaction
import com.data.local.db.AppDatabase
import com.data.offline.OfflineAssetStore
import com.data.preferences.DownloadQuality
import com.data.preferences.OfflineDownloadPreferences
import com.data.local.db.DbMigrations
import com.data.local.entity.CategoryEntity
import com.data.local.entity.ReadingHistoryEntity
import com.data.local.entity.UserDownloadEntity
import com.data.local.entity.UserEntity
import com.data.local.entity.UserFollowedCategoryEntity
import com.data.local.entity.UserSearchHistoryEntity
import com.data.mapper.LocalMapper.toArticle
import com.data.mapper.LocalMapper.toEntity
import com.data.model.Article
import com.data.model.Source
import com.ui.model.FollowTopicUiModel
import com.ui.model.NewsUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.util.ReaderContentExtractor

class ProfileRepository(
    private val database: AppDatabase,
    context: Context
) {

    private val offlineAssetStore = OfflineAssetStore(context)
    private val offlineDownloadPreferences = OfflineDownloadPreferences(context)

    private val articleDao = database.articleDao()
    private val bookmarkDao = database.bookmarkDao()
    private val readingHistoryDao = database.readingHistoryDao()
    private val downloadDao = database.downloadDao()
    private val categoryDao = database.categoryDao()
    private val searchHistoryDao = database.searchHistoryDao()
    private val localNewsCacheDao = database.localNewsCacheDao()
    private val userDao = database.userDao()

    suspend fun bootstrap() {
        ensureDefaultUser()
        seedCategoriesIfNeeded()
    }

    fun observeReadingHistoryItems(): Flow<List<NewsUiModel>> {
        return readingHistoryDao.observeByUserId(DEFAULT_USER_ID).map { historyItems ->
            val orderedUrls = historyItems.map { it.articleUrl }.distinct()
            if (orderedUrls.isEmpty()) return@map emptyList<NewsUiModel>()

            val articleMap = articleDao
                .getArticlesByUrls(orderedUrls)
                .associateBy { it.url }

            historyItems.mapNotNull { entry ->
                val article = articleMap[entry.articleUrl]?.toArticle() ?: return@mapNotNull null

                NewsUiModel(
                    title = article.title.orEmpty(),
                    description = article.description,
                    content = article.content,
                    imageUrl = article.urlToImage,
                    articleUrl = article.url,
                    sourceName = article.source?.name.orEmpty(),
                    author = article.author,
                    publishedAt = article.publishedAt,
                    itemId = entry.id,
                    eventTimeMillis = entry.openedAt,
                    completionPercent = entry.completionPercent,
                    readSeconds = entry.readSeconds,
                    canResume = entry.completionPercent in 1..99
                )
            }
        }
    }

    fun observeDownloadedItems(): Flow<List<NewsUiModel>> {
        return downloadDao.observeByUserId(DEFAULT_USER_ID).map { downloads ->
            val orderedUrls = downloads.map { it.articleUrl }.distinct()
            if (orderedUrls.isEmpty()) return@map emptyList<NewsUiModel>()

            val articleMap = articleDao
                .getArticlesByUrls(orderedUrls)
                .associateBy { it.url }

            downloads.mapNotNull { entry ->
                val article = articleMap[entry.articleUrl]?.toArticle() ?: return@mapNotNull null

                NewsUiModel(
                    title = article.title.orEmpty(),
                    description = article.description,
                    content = article.content,
                    imageUrl = article.urlToImage,
                    articleUrl = article.url,
                    sourceName = article.source?.name.orEmpty(),
                    author = article.author,
                    publishedAt = article.publishedAt,
                    itemId = entry.id,
                    eventTimeMillis = entry.downloadedAt,
                    status = entry.status,
                    fileSizeBytes = entry.fileSizeBytes
                )
            }
        }
    }

    fun observeFollowTopics(): Flow<List<FollowTopicUiModel>> {
        return combine(
            categoryDao.observeAllActiveCategories(),
            categoryDao.observeFollowedCategoryIds(DEFAULT_USER_ID)
        ) { categories, followedIds ->
            val followedSet = followedIds.toSet()

            categories.map { category ->
                FollowTopicUiModel(
                    id = category.id,
                    name = category.name,
                    isFollowed = followedSet.contains(category.id),
                    newTodayCount = ((category.name.length * 7) % 12) + 1,
                    notificationsEnabled = true
                )
            }
        }
    }

    suspend fun toggleFollowTopic(item: FollowTopicUiModel) {
        ensureDefaultUser()

        if (item.isFollowed) {
            categoryDao.unfollow(DEFAULT_USER_ID, item.id)
        } else {
            categoryDao.follow(
                UserFollowedCategoryEntity(
                    userId = DEFAULT_USER_ID,
                    categoryId = item.id,
                    followedAt = System.currentTimeMillis(),
                    notificationsEnabled = true
                )
            )
        }
    }

    suspend fun recordReading(article: Article, fromScreen: String?): Long? {
        val articleUrl = article.url.orEmpty()
        if (articleUrl.isBlank()) return null

        ensureDefaultUser()

        articleDao.upsertArticlePreservingStats(article.copy(url = articleUrl).toEntity())

        return readingHistoryDao.insert(
            ReadingHistoryEntity(
                userId = DEFAULT_USER_ID,
                articleUrl = articleUrl,
                openedAt = System.currentTimeMillis(),
                readSeconds = 0,
                completionPercent = 1,
                fromScreen = fromScreen,
                deviceInfo = Build.MODEL
            )
        )
    }

    suspend fun finishReading(
        historyId: Long,
        readSeconds: Int,
        completionPercent: Int
    ) {
        if (historyId <= 0L) return

        readingHistoryDao.finish(
            historyId = historyId,
            closedAt = System.currentTimeMillis(),
            readSeconds = readSeconds.coerceAtLeast(1),
            completionPercent = completionPercent.coerceIn(1, 100)
        )
    }

    suspend fun recordSearch(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return

        ensureDefaultUser()

        searchHistoryDao.insert(
            UserSearchHistoryEntity(
                userId = DEFAULT_USER_ID,
                query = normalizedQuery,
                searchedAt = System.currentTimeMillis()
            )
        )
    }

    fun observeSearchHistoryQueries(limit: Int = 8): Flow<List<String>> {
        return searchHistoryDao.observeByUserId(DEFAULT_USER_ID, limit)
            .map { historyItems ->
                historyItems
                    .map { it.query.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(limit)
            }
    }

    suspend fun getFollowingQuery(): String {
        bootstrap()

        val followedNames = categoryDao
            .getFollowedCategories(DEFAULT_USER_ID)
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.equals("Top News", ignoreCase = true) }

        val fallback = listOf(
            "technology",
            "business",
            "science",
            "health",
            "sports"
        )

        val selected = followedNames.ifEmpty { fallback }

        return selected.joinToString(" OR ") { name ->
            val safeName = name.replace("\"", "")
            if (safeName.contains(" ")) "\"$safeName\"" else safeName
        }
    }

    suspend fun addDownload(article: Article) = withContext(Dispatchers.IO) {
        val articleUrl = article.url.orEmpty().trim()
        if (articleUrl.isBlank()) return@withContext

        ensureDefaultUser()

        val settings = offlineDownloadPreferences.current()
        val imageQuality = when (settings.quality) {
            DownloadQuality.FULL -> OfflineAssetStore.ImageQuality.FULL
            DownloadQuality.LITE -> OfflineAssetStore.ImageQuality.LITE
            DownloadQuality.TEXT_ONLY -> OfflineAssetStore.ImageQuality.TEXT_ONLY
        }

        val baseOfflineArticle = buildOfflineArticle(article)
        val storedImage = offlineAssetStore.saveCoverImage(
            articleUrl = articleUrl,
            imageUrl = baseOfflineArticle.urlToImage ?: article.urlToImage,
            quality = imageQuality
        )
        val offlineArticle = baseOfflineArticle.copy(
            urlToImage = storedImage?.uri ?: baseOfflineArticle.urlToImage
        )
        val articleBytes = estimateArticleSizeBytes(offlineArticle)
        val totalBytes = articleBytes + (storedImage?.sizeBytes ?: 0L)

        database.withTransaction {
            articleDao.upsertArticlePreservingStats(offlineArticle.copy(url = articleUrl).toEntity())

            downloadDao.upsert(
                UserDownloadEntity(
                    userId = DEFAULT_USER_ID,
                    articleUrl = articleUrl,
                    downloadedAt = System.currentTimeMillis(),
                    localPath = "offline://article/${articleUrl.hashCode()}",
                    fileSizeBytes = totalBytes,
                    status = UserDownloadEntity.STATUS_DONE,
                    expiresAt = null
                )
            )
        }
    }

    suspend fun removeDownload(articleUrl: String) {
        val safeUrl = articleUrl.trim()
        if (safeUrl.isBlank()) return
        downloadDao.delete(DEFAULT_USER_ID, safeUrl)
        offlineAssetStore.deleteCoverForArticle(safeUrl)
    }

    suspend fun removeHistoryItem(historyId: Long) {
        readingHistoryDao.deleteById(historyId)
    }

    suspend fun clearReadingHistory() {
        readingHistoryDao.clearByUserId(DEFAULT_USER_ID)
    }

    suspend fun clearDownloads() {
        downloadDao.clearByUserId(DEFAULT_USER_ID)
        offlineAssetStore.deleteAllOfflineImages()
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearByUserId(DEFAULT_USER_ID)
    }

    suspend fun clearBookmarks() {
        bookmarkDao.clearByUserId(DEFAULT_USER_ID)
    }

    suspend fun clearNewsCache(): Int {
        val removed = localNewsCacheDao.countAll()
        localNewsCacheDao.clearAll()
        return removed
    }

    suspend fun refreshLocalAccount() {
        bootstrap()
    }

    suspend fun resetFollowingTopics() {
        categoryDao.clearFollowedByUserId(DEFAULT_USER_ID)
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


    private suspend fun buildOfflineArticle(article: Article): Article {
        val articleUrl = article.url.orEmpty().trim()
        val storedDetails = articleDao.getArticleWithDetails(articleUrl)
        val storedArticle = storedDetails?.article?.toArticle()
        val blockContent = storedDetails
            ?.blocks
            .orEmpty()
            .sortedWith(compareBy({ it.blockOrder }, { it.id }))
            .mapNotNull { block ->
                val text = block.content?.trim().orEmpty()
                val caption = block.caption?.trim().orEmpty()
                when {
                    text.isNotBlank() -> text
                    caption.isNotBlank() -> caption
                    else -> null
                }
            }
            .distinct()
            .joinToString("\n\n")
            .trim()

        val localBestContent = longestNonBlank(
            blockContent,
            article.content,
            storedArticle?.content,
            article.description,
            storedArticle?.description
        )
        val extractedContent = if (localBestContent.length < MIN_FULL_ARTICLE_LENGTH && articleUrl.startsWith("https://", ignoreCase = true)) {
            ReaderContentExtractor.fetchReadableArticle(
                url = articleUrl,
                fallbackTitle = firstNonBlank(article.title, storedArticle?.title).orEmpty()
            )?.text.orEmpty()
        } else {
            ""
        }
        val fullContent = longestNonBlank(
            extractedContent,
            blockContent,
            article.content,
            storedArticle?.content,
            article.description,
            storedArticle?.description
        )

        return Article(
            source = Source(
                id = storedArticle?.source?.id ?: article.source?.id,
                name = firstNonBlank(storedArticle?.source?.name, article.source?.name)
            ),
            author = firstNonBlank(article.author, storedArticle?.author),
            title = firstNonBlank(article.title, storedArticle?.title),
            description = firstNonBlank(article.description, storedArticle?.description, fullContent.take(240)),
            url = articleUrl,
            urlToImage = firstNonBlank(article.urlToImage, storedArticle?.urlToImage),
            publishedAt = firstNonBlank(article.publishedAt, storedArticle?.publishedAt),
            content = fullContent.ifBlank { firstNonBlank(article.description, storedArticle?.description) }
        )
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun longestNonBlank(vararg values: String?): String {
        return values
            .map { it.orEmpty().trim() }
            .filter { it.isNotBlank() }
            .maxByOrNull { it.length }
            .orEmpty()
    }

    private fun estimateArticleSizeBytes(article: Article): Long {
        val text = buildString {
            append(article.title.orEmpty())
            append(article.description.orEmpty())
            append(article.content.orEmpty())
            append(article.url.orEmpty())
            append(article.urlToImage.orEmpty())
            append(article.source?.name.orEmpty())
            append(article.author.orEmpty())
            append(article.publishedAt.orEmpty())
        }

        return text.toByteArray(Charsets.UTF_8).size.toLong()
    }


    suspend fun getLocalRelatedArticles(article: Article, limit: Int = 6): List<NewsUiModel> = withContext(Dispatchers.IO) {
        val currentUrl = article.url.orEmpty().trim()
        val tokens = relatedTokens(article)
        if (tokens.isEmpty()) return@withContext emptyList()

        articleDao.getAllArticles()
            .asSequence()
            .filter { it.url != currentUrl }
            .map { it.toArticle() }
            .map { candidate -> candidate to relatedScore(candidate, tokens) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<Article, Int>> { it.second }.thenByDescending { it.first.publishedAt.orEmpty() })
            .take(limit)
            .map { (candidate, _) ->
                NewsUiModel(
                    title = candidate.title.orEmpty(),
                    description = candidate.description,
                    content = candidate.content,
                    imageUrl = candidate.urlToImage,
                    articleUrl = candidate.url,
                    sourceName = candidate.source?.name.orEmpty(),
                    author = candidate.author,
                    publishedAt = candidate.publishedAt
                )
            }
            .toList()
    }

    suspend fun autoDownloadBookmarkedArticles(limit: Int = 20): Int = withContext(Dispatchers.IO) {
        ensureDefaultUser()
        val settings = offlineDownloadPreferences.current()
        if (!settings.autoDownloadBookmarks) return@withContext 0

        val bookmarked = database.bookmarkDao().getBookmarkedArticles(DEFAULT_USER_ID).take(limit)
        var saved = 0
        bookmarked.forEach { entity ->
            val alreadyDownloaded = downloadDao.getByUserAndArticle(DEFAULT_USER_ID, entity.url)?.status == UserDownloadEntity.STATUS_DONE
            if (!alreadyDownloaded) {
                addDownload(entity.toArticle())
                saved++
            }
        }
        saved
    }

    suspend fun cleanupExpiredDownloads(olderThanDays: Int): Int = withContext(Dispatchers.IO) {
        ensureDefaultUser()
        val cutoff = System.currentTimeMillis() - olderThanDays.coerceAtLeast(1) * 24L * 60L * 60L * 1000L
        val expired = downloadDao.getOlderThan(DEFAULT_USER_ID, cutoff)
        expired.forEach { entry ->
            downloadDao.delete(DEFAULT_USER_ID, entry.articleUrl)
            offlineAssetStore.deleteCoverForArticle(entry.articleUrl)
        }
        expired.size
    }

    fun offlineImageBytes(): Long = offlineAssetStore.offlineImageBytes()

    private fun relatedTokens(article: Article): Set<String> {
        val text = listOf(article.title, article.description, article.content, article.source?.name)
            .joinToString(" ") { it.orEmpty() }
            .lowercase()
        return text
            .split(Regex("[^a-zA-Z0-9À-ỹ]+"))
            .map { it.trim() }
            .filter { it.length >= 4 }
            .filterNot { it in stopWords }
            .take(18)
            .toSet()
    }

    private fun relatedScore(article: Article, tokens: Set<String>): Int {
        val haystack = listOf(article.title, article.description, article.content, article.source?.name)
            .joinToString(" ") { it.orEmpty() }
            .lowercase()
        return tokens.count { token -> haystack.contains(token) }
    }

    private suspend fun seedCategoriesIfNeeded() {
        if (categoryDao.getCategoryCount() > 0) return

        val now = System.currentTimeMillis()

        categoryDao.upsertCategories(
            defaultCategories.mapIndexed { index, name ->
                CategoryEntity(
                    id = name.lowercase().replace(" ", "_"),
                    name = name,
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now
                )
            }
        )
    }

    companion object {
        private val stopWords = setOf(
            "with", "from", "that", "this", "have", "will", "news", "article", "the", "and",
            "của", "cho", "với", "này", "đang", "trong", "người", "việt", "nam"
        )

        private val defaultCategories = listOf(
            "Top News",
            "Politics",
            "Business",
            "Technology",
            "Sports",
            "Entertainment",
            "Health",
            "Science",
            "Travel",
            "Lifestyle"
        )

        private const val MIN_FULL_ARTICLE_LENGTH = 1_200

        const val DEFAULT_USER_ID: String = DbMigrations.DEFAULT_USER_ID
    }
}