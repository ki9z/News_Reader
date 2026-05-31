package com.util

import android.os.Bundle
import androidx.core.os.bundleOf
import com.data.model.Article
import com.ui.model.NewsUiModel

private const val DETAIL_BUNDLE_TEXT_LIMIT = 12_000

private fun safeDetailText(value: String?): String {
    val text = value.orEmpty()
    return if (text.length <= DETAIL_BUNDLE_TEXT_LIMIT) {
        text
    } else {
        text.take(DETAIL_BUNDLE_TEXT_LIMIT).trimEnd() + "…"
    }
}

fun NewsUiModel.toDetailBundle(fromScreen: String): Bundle {
    return bundleOf(
        "title" to title,
        "description" to safeDetailText(description),
        "content" to safeDetailText(content),
        "imageUrl" to imageUrl.orEmpty(),
        "articleUrl" to articleUrl.orEmpty(),
        "sourceName" to sourceName,
        "author" to author.orEmpty(),
        "publishedAt" to publishedAt.orEmpty(),
        "fromScreen" to fromScreen
    )
}

fun Article.toDetailBundle(fromScreen: String): Bundle {
    return bundleOf(
        "title" to title.orEmpty(),
        "description" to safeDetailText(description),
        "content" to safeDetailText(content),
        "imageUrl" to urlToImage.orEmpty(),
        "articleUrl" to url.orEmpty(),
        "sourceName" to source?.name.orEmpty(),
        "author" to author.orEmpty(),
        "publishedAt" to publishedAt.orEmpty(),
        "fromScreen" to fromScreen
    )
}
