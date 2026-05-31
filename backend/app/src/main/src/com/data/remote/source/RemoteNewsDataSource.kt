package com.data.remote.source

import com.data.model.NewsResponse
import com.data.model.SourcesResponse
import retrofit2.Response

interface RemoteNewsDataSource {
    suspend fun getTopHeadlines(
        category: String? = null,
        query: String? = null,
        country: String? = null,
        sources: String? = null,
        page: Int,
        pageSize: Int
    ): Response<NewsResponse>

    suspend fun searchNews(
        query: String,
        sortBy: String? = null,
        sources: String? = null,
        language: String? = null,
        page: Int,
        pageSize: Int
    ): Response<NewsResponse>

    suspend fun getSources(
        category: String? = null,
        language: String? = null,
        country: String? = null
    ): Response<SourcesResponse>
}
