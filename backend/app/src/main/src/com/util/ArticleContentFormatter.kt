package com.util

import android.text.Html

object ArticleContentFormatter {

    private val trailingCharsPattern = Regex("\\s*\\[\\+\\d+\\s+chars]\\s*$", RegexOption.IGNORE_CASE)
    private val htmlTagPattern = Regex("<[^>]+>")
    private val whitespacePattern = Regex("[\\t\\x0B\\f\\r ]+")
    private val excessiveNewlinePattern = Regex("\\n{3,}")

    fun sanitize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""

        val withoutSuffix = raw.replace(trailingCharsPattern, "")
        val htmlDecoded = Html.fromHtml(withoutSuffix, Html.FROM_HTML_MODE_LEGACY).toString()
        val withoutRawTags = htmlDecoded.replace(htmlTagPattern, " ")

        return withoutRawTags
            .replace("\u00A0", " ")
            .replace(whitespacePattern, " ")
            .replace(" \n", "\n")
            .replace("\n ", "\n")
            .replace(excessiveNewlinePattern, "\n\n")
            .trim()
    }

    fun removeDuplicateSummary(description: String, content: String): String {
        if (description.isBlank() || content.isBlank()) return content

        val descCanonical = canonical(description)
        val contentCanonical = canonical(content)

        if (contentCanonical == descCanonical) return ""

        if (contentCanonical.startsWith(descCanonical)) {
            val prefixRegex = Regex("^\\Q$description\\E[\\s\\n:;,.!\\-]*", RegexOption.IGNORE_CASE)
            val stripped = prefixRegex.replace(content, "").trim()
            if (stripped.isNotBlank()) return stripped
        }

        val containedIndex = content.indexOf(description, ignoreCase = true)
        if (containedIndex in 0..(content.length / 2)) {
            val stripped = content.removeRange(containedIndex, containedIndex + description.length).trim()
            if (stripped.isNotBlank()) return stripped
        }

        return content
    }

    private fun canonical(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }
}

