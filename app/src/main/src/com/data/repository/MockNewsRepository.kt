package com.data.repository

import com.data.model.Article
import com.data.model.NewsSource
import com.data.model.Source
import com.util.NetworkResult

class MockNewsRepository : NewsRepository {

    private val bookmarks = mutableListOf<Article>()
    private val localCache = mutableMapOf<String, List<Article>>()

    private fun mockArticles(category: String?): List<Article> = listOf(
        Article(
            source = Source(id = "bbc-news", name = "BBC News"),
            author = "BBC Reporter",
            title = "[$category] Breaking: Major Tech Conference Announces AI Advancements",
            description = "Tech giants gather to showcase the latest AI advancements that could reshape industries worldwide.",
            url = "https://example.com/article/1",
            urlToImage = null,
            publishedAt = "2026-03-14T08:00:00Z",
            content = "Tech giants gather worldwide to showcase the latest in artificial intelligence..."
        ),
        Article(
            source = Source(id = "cnn", name = "CNN"),
            author = "CNN Staff",
            title = "[$category] Global Economy Shows Signs of Recovery",
            description = "New data indicates global markets are recovering with strong growth forecasts for next quarter.",
            url = "https://example.com/article/2",
            urlToImage = null,
            publishedAt = "2026-03-14T07:30:00Z",
            content = "Global economic indicators show positive trends for the coming quarter..."
        ),
        Article(
            source = Source(id = "reuters", name = "Reuters"),
            author = "Reuters Staff",
            title = "[$category] Space Agency Confirms New Planetary Discovery",
            description = "Scientists confirm the discovery of a new planet in a nearby solar system.",
            url = "https://example.com/article/3",
            urlToImage = null,
            publishedAt = "2026-03-14T06:00:00Z",
            content = "Scientists at the national space agency confirmed today the discovery of..."
        ),
        Article(
            source = Source(id = "techcrunch", name = "TechCrunch"),
            author = "Tech Writer",
            title = "[$category] Startup Raises \$500M in Record Funding Round",
            description = "A Silicon Valley startup has raised \$500 million in a record-breaking Series C round.",
            url = "https://example.com/article/4",
            urlToImage = null,
            publishedAt = "2026-03-13T20:00:00Z",
            content = "In one of the largest Series C rounds in recent history, the startup announced..."
        ),
        Article(
            source = Source(id = "nytimes", name = "New York Times"),
            author = "NYT Correspondent",
            title = "[$category] Climate Summit Reaches Landmark Agreement",
            description = "World leaders reached a historic climate agreement setting ambitious carbon reduction targets.",
            url = "https://example.com/article/5",
            urlToImage = null,
            publishedAt = "2026-03-13T18:00:00Z",
            content = "In a landmark achievement for global climate policy, world leaders..."
        ),
        Article(
            source = Source(id = "the-guardian", name = "The Guardian"),
            author = "Guardian Writer",
            title = "[$category] New Study Links Exercise to Improved Mental Health",
            description = "Research shows consistent exercise can significantly reduce anxiety and depression symptoms.",
            url = "https://example.com/article/6",
            urlToImage = null,
            publishedAt = "2026-03-13T15:00:00Z",
            content = "A comprehensive study involving over 10,000 participants found that regular exercise..."
        )
    )

    override suspend fun getTopHeadlines(
        category: String?,
        country: String,
        query: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): NetworkResult<List<Article>> {
        val seed = query ?: category ?: country
        val articles = mockArticles(seed)
        return NetworkResult.Success(articles)
    }

    override suspend fun searchNews(
        query: String,
        sortBy: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): NetworkResult<List<Article>> {
        val suffix = listOfNotNull(sortBy, sources).joinToString("-").ifBlank { "search" }
        val keyword = "${query.trim().ifEmpty { "search" }}-$suffix"
        return NetworkResult.Success(mockArticles(keyword))
    }

    override suspend fun getLocalNews(
        locationQuery: String?,
        country: String,
        page: Int,
        pageSize: Int
    ): NetworkResult<List<Article>> {
        val keyword = locationQuery?.trim().takeUnless { it.isNullOrBlank() } ?: "local"
        val articles = mockArticles("$country-$keyword")
        localCache[buildCacheKey(locationQuery, country)] = articles
        return NetworkResult.Success(articles)
    }

    override suspend fun getCachedLocalNews(locationQuery: String?, country: String): List<Article> {
        return localCache[buildCacheKey(locationQuery, country)].orEmpty()
    }

    override suspend fun saveCachedLocalNews(
        locationQuery: String?,
        country: String,
        cityTitle: String,
        articles: List<Article>
    ) {
        localCache[buildCacheKey(locationQuery, country)] = articles.toList()
    }

    override suspend fun getBookmarks(): List<Article> = bookmarks.toList()

    override suspend fun saveBookmark(article: Article) {
        if (bookmarks.none { it.url == article.url }) {
            bookmarks.add(article)
        }
    }

    override suspend fun removeBookmark(articleUrl: String) {
        bookmarks.removeAll { it.url == articleUrl }
    }

    override suspend fun isBookmarked(articleUrl: String): Boolean {
        return bookmarks.any { it.url == articleUrl }
    }

    private fun buildCacheKey(locationQuery: String?, country: String): String {
        return "${locationQuery?.trim()?.lowercase().orEmpty().ifBlank { "country" }}|${country.lowercase()}"
    }
    override suspend fun incrementArticleView(articleUrl: String) {
    }

    override suspend fun recordOriginalArticleOpen(article: Article) {
        val articleUrl = article.url?.trim().orEmpty()
        if (articleUrl.isBlank()) return
        if (bookmarks.none { it.url == articleUrl }) {
            bookmarks.add(article.copy(url = articleUrl))
        }
    }

    override suspend fun getSources(
        category: String?,
        language: String?,
        country: String?
    ): NetworkResult<List<NewsSource>> {
        val items = listOf(
            NewsSource("bbc-news", "BBC News", "International news from BBC.", "https://www.bbc.co.uk/news", "general", "en", "gb"),
            NewsSource("cnn", "CNN", "Breaking news and live coverage.", "https://www.cnn.com", "general", "en", "us"),
            NewsSource("reuters", "Reuters", "Global business and world news.", "https://www.reuters.com", "general", "en", "us"),
            NewsSource("techcrunch", "TechCrunch", "Startup and technology news.", "https://techcrunch.com", "technology", "en", "us")
        )
        return NetworkResult.Success(items)
    }
}

