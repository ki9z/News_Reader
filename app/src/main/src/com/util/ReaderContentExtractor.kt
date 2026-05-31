package com.util

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Lightweight reader-mode extractor used for offline downloads and the in-app
 * reader view. It intentionally avoids a heavy HTML parser dependency and
 * falls back safely when a publisher blocks the request.
 */
object ReaderContentExtractor {

    private val gson = Gson()

    suspend fun fetchReadableArticle(url: String, fallbackTitle: String = ""): ReaderArticle? = withContext(Dispatchers.IO) {
        val safeUrl = url.trim()
        val isHttps = safeUrl.startsWith("https://", ignoreCase = true)
        val isHttp = safeUrl.startsWith("http://", ignoreCase = true)
        if (!isHttps && !(Constants.USES_BACKEND_PROXY && isHttp)) return@withContext null

        if (Constants.USES_BACKEND_PROXY) {
            fetchFromBackendReader(safeUrl, fallbackTitle)?.let { return@withContext it }
        }

        if (!isHttps) return@withContext null

        runCatching {
            val connection = (URL(safeUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 NewsReaderAndroid/1.0 ReaderMode")
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
            }

            try {
                val code = connection.responseCode
                if (code !in 200..299) return@runCatching null
                val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
                if (contentType.isNotBlank() && !contentType.contains("text/html")) return@runCatching null

                val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    buildString {
                        val buffer = CharArray(4096)
                        var total = 0
                        while (true) {
                            val count = reader.read(buffer)
                            if (count <= 0) break
                            total += count
                            if (total > MAX_HTML_CHARS) break
                            append(buffer, 0, count)
                        }
                    }
                }
                extract(html, fallbackTitle)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    fun extract(html: String, fallbackTitle: String = ""): ReaderArticle? {
        if (html.isBlank()) return null

        val cleanedHtml = html
            .replace(scriptRegex, " ")
            .replace(styleRegex, " ")
            .replace(noscriptRegex, " ")
            .replace(commentRegex, " ")

        val title = firstMatch(cleanedHtml, titleRegex)
            ?.let(::decodeHtmlEntities)
            ?.cleanSpaces()
            ?.takeIf { it.isNotBlank() }
            ?: fallbackTitle.cleanSpaces()

        val candidates = buildList {
            findAllMatches(cleanedHtml, articleRegex).forEach { add(it) }
            findAllMatches(cleanedHtml, mainRegex).forEach { add(it) }
            findAllMatches(cleanedHtml, paragraphGroupRegex).forEach { add(it) }
        }

        val text = candidates
            .map { htmlBlockToText(it) }
            .filter { it.length >= MIN_READER_TEXT_LENGTH }
            .maxByOrNull { it.length }
            ?: htmlBlockToText(cleanedHtml)

        val normalizedText = normalizeArticleText(text)
        if (normalizedText.length < MIN_READER_TEXT_LENGTH) return null

        return ReaderArticle(title = title, text = normalizedText)
    }

    private fun htmlBlockToText(block: String): String {
        return block
            .replace(blockBreakRegex, "\n\n")
            .replace(lineBreakRegex, "\n")
            .replace(tagRegex, " ")
            .let(::decodeHtmlEntities)
            .replace(Regex("[ \t\u000B\u000C\r]+"), " ")
            .replace(Regex(" *\n *"), "\n")
            .cleanSpaces()
    }

    private fun normalizeArticleText(raw: String): String {
        val lines = raw
            .split('\n')
            .map { it.cleanSpaces() }
            .filter { line ->
                line.length >= 35 &&
                    !line.equals("advertisement", ignoreCase = true) &&
                    !line.contains("subscribe", ignoreCase = true) &&
                    !line.contains("enable javascript", ignoreCase = true)
            }
            .distinct()

        return lines.joinToString("\n\n").take(MAX_READER_TEXT_CHARS).trim()
    }

    private fun decodeHtmlEntities(value: String): String {
        return value
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun firstMatch(source: String, regex: Regex): String? = regex.find(source)?.groupValues?.getOrNull(1)

    private fun findAllMatches(source: String, regex: Regex): List<String> = regex.findAll(source)
        .mapNotNull { it.groupValues.getOrNull(1) }
        .toList()

    private fun String.cleanSpaces(): String = trim().replace(Regex("[ \t\u000B\u000C\r]{2,}"), " ")

    data class ReaderArticle(
        val title: String,
        val text: String
    )

    private fun fetchFromBackendReader(articleUrl: String, fallbackTitle: String): ReaderArticle? {
        return runCatching {
            val base = if (Constants.BASE_URL.endsWith("/")) Constants.BASE_URL else "${Constants.BASE_URL}/"
            val encodedUrl = URLEncoder.encode(articleUrl, "UTF-8")
            val endpoint = URL("${base}api/reader?url=$encodedUrl")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                if (Constants.BACKEND_APP_TOKEN.isNotBlank()) {
                    setRequestProperty("X-App-Token", Constants.BACKEND_APP_TOKEN)
                }
            }

            try {
                val code = connection.responseCode
                if (code !in 200..299) return@runCatching null

                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val response = gson.fromJson(json, BackendReaderResponse::class.java)
                val article = response.article ?: return@runCatching null
                val text = listOf(article.content, article.description)
                    .map { it.orEmpty().trim() }
                    .firstOrNull { it.length >= MIN_READER_TEXT_LENGTH }
                    ?: return@runCatching null

                ReaderArticle(
                    title = article.title?.cleanSpaces()?.takeIf { it.isNotBlank() } ?: fallbackTitle.cleanSpaces(),
                    text = normalizeArticleText(text)
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private data class BackendReaderResponse(
        val status: String? = null,
        val article: BackendReaderArticle? = null
    )

    private data class BackendReaderArticle(
        val title: String? = null,
        val description: String? = null,
        val content: String? = null
    )

    private const val MIN_READER_TEXT_LENGTH = 320
    private const val MAX_HTML_CHARS = 1_200_000
    private const val MAX_READER_TEXT_CHARS = 40_000

    private val scriptRegex = Regex("<script\\b[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val styleRegex = Regex("<style\\b[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val noscriptRegex = Regex("<noscript\\b[^>]*>.*?</noscript>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val commentRegex = Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL))
    private val titleRegex = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val articleRegex = Regex("<article\\b[^>]*>(.*?)</article>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val mainRegex = Regex("<main\\b[^>]*>(.*?)</main>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val paragraphGroupRegex = Regex("((?:<p\\b[^>]*>.*?</p>\\s*){3,})", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val blockBreakRegex = Regex("</(p|div|section|article|h[1-6]|li)>|<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val lineBreakRegex = Regex("<(p|div|section|article|h[1-6]|li)\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("<[^>]+>")
}
