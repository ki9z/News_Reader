package com.data.local.source

import com.data.local.seed.PreCrawledNewsDataSource
import com.data.model.Article
import com.util.Constants
import java.text.Normalizer
import java.util.Locale

class OfflineNewsDataSourceImpl(
    private val preCrawledNewsDataSource: PreCrawledNewsDataSource
) : OfflineNewsDataSource {

    override fun getPreCrawledArticles(): List<Article> {
        return preCrawledNewsDataSource.getAllArticles()
    }

    override fun filterByCategory(articles: List<Article>, category: String?): List<Article> {
        if (category.isNullOrBlank()) return articles
        val normalizedKeyword = normalizeText(category)
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

    override fun filterByQuery(articles: List<Article>, query: String): List<Article> {
        return filterByCategory(articles, query)
    }

    override fun filterByLocation(articles: List<Article>, location: String): List<Article> {
        return filterByCategory(articles, location)
    }

    override fun rotateForPage(
        articles: List<Article>,
        key: String,
        page: Int,
        pageSize: Int
    ): List<Article> {
        if (articles.isEmpty()) return emptyList()

        val safePage = page.coerceAtLeast(Constants.INITIAL_PAGE)
        val offset = ((key.hashCode() and Int.MAX_VALUE) + (safePage - 1) * pageSize) % articles.size
        return articles.drop(offset) + articles.take(offset)
    }

    private fun normalizeText(value: String): String {
        val strippedAccents = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

        return strippedAccents
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
