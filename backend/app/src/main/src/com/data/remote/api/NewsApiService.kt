package com.data.remote.api

import com.data.model.NewsResponse
import com.data.model.SourcesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("category") category: String?,
        @Query("q") query: String? = null,
        @Query("country") country: String? = "us",
        @Query("sources") sources: String? = null,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<NewsResponse>

    @GET("v2/everything")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("sortBy") sortBy: String? = null,
        @Query("sources") sources: String? = null,
        @Query("language") language: String? = null,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<NewsResponse>

    @GET("v2/top-headlines/sources")
    suspend fun getSources(
        @Query("category") category: String? = null,
        @Query("language") language: String? = null,
        @Query("country") country: String? = null
    ): Response<SourcesResponse>
}

