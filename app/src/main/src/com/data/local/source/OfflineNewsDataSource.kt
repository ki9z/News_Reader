package com.data.local.source

import com.data.model.Article

interface OfflineNewsDataSource {
    fun getPreCrawledArticles(): List<Article>

    fun filterByCategory(articles: List<Article>, category: String?): List<Article>
    fun filterByQuery(articles: List<Article>, query: String): List<Article>
    fun filterByLocation(articles: List<Article>, location: String): List<Article>

    fun rotateForPage(
        articles: List<Article>,
        key: String,
        page: Int,
        pageSize: Int
    ): List<Article>
}
