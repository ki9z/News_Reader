package com.data.remote.source

import com.data.model.NewsResponse
import com.data.model.SourcesResponse
import com.data.remote.api.NewsApiService
import retrofit2.Response

class RemoteNewsDataSourceImpl(
    private val apiService: NewsApiService
) : RemoteNewsDataSource {

    override suspend fun getTopHeadlines(
        category: String?,
        query: String?,
        country: String?,
        sources: String?,
        page: Int,
        pageSize: Int
    ): Response<NewsResponse> {
        return apiService.getTopHeadlines(
            category = category,
            query = query,
            country = country,
            sources = sources,
            page = page,
            pageSize = pageSize
        )
    }

    override suspend fun searchNews(
        query: String,
        sortBy: String?,
        sources: String?,
        language: String?,
        page: Int,
        pageSize: Int
    ): Response<NewsResponse> {
        return apiService.searchNews(
            query = query,
            sortBy = sortBy,
            sources = sources,
            language = language,
            page = page,
            pageSize = pageSize
        )
    }

    override suspend fun getSources(
        category: String?,
        language: String?,
        country: String?
    ): Response<SourcesResponse> {
        return apiService.getSources(
            category = category,
            language = language,
            country = country
        )
    }
}
