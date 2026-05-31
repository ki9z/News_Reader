package com.data.local.seed

import android.content.Context
import com.data.model.Article
import com.data.model.Source
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PreCrawledNewsDataSource(
    private val context: Context
) {
    private val gson = Gson()

    @Volatile
    private var cachedArticles: List<Article>? = null

    fun getAllArticles(): List<Article> {
        cachedArticles?.let { return it }
        return synchronized(this) {
            cachedArticles ?: loadArticlesFromAssets().also { cachedArticles = it }
        }
    }

    private fun loadArticlesFromAssets(): List<Article> {
        val assetFiles = listOf("news1.json", "news2.json", "news3.json", "news4.json")
        val listType = object : TypeToken<List<SeedArticleJson>>() {}.type

        return assetFiles.flatMap { fileName ->
            runCatching {
                context.assets.open(fileName).bufferedReader().use { reader ->
                    val payload = reader.readText()
                    val items: List<SeedArticleJson> = gson.fromJson(payload, listType) ?: emptyList()
                    items.map { it.toArticle() }
                }
            }.getOrDefault(emptyList())
        }
    }

    private fun SeedArticleJson.toArticle(): Article {
        val description = blocks.orEmpty()
            .firstOrNull { block ->
                val type = block.type?.lowercase(Locale.ROOT)
                (type == "sapo" || type == "text") && !block.data.isNullOrBlank()
            }
            ?.data
            ?.trim()

        val content = blocks.orEmpty()
            .asSequence()
            .mapNotNull { block ->
                val text = block.data ?: block.caption
                text?.trim()?.takeIf { it.isNotBlank() }
            }
            .take(6)
            .joinToString("\n\n")
            .ifBlank { null }

        return Article(
            source = Source(
                id = source.orEmpty().trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-"),
                name = source?.trim()
            ),
            author = author?.trim()?.ifBlank { null },
            title = title?.trim(),
            description = description,
            url = url?.trim(),
            urlToImage = thumbnail?.trim(),
            publishedAt = parsePublishedAt(time),
            content = content
        )
    }

    private fun parsePublishedAt(value: String?): String? {
        if (value.isNullOrBlank()) return null

        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            isLenient = false
        }
        val output = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return runCatching { output.format(input.parse(value.trim())!!) }.getOrNull()
    }

    private data class SeedArticleJson(
        val source: String?,
        val author: String?,
        val title: String?,
        val thumbnail: String?,
        val time: String?,
        val blocks: List<SeedBlockJson>?,
        val url: String?
    )

    private data class SeedBlockJson(
        val type: String?,
        val data: String?,
        val caption: String?
    )
}

