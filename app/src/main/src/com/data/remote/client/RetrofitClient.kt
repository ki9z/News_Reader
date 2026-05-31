package com.data.remote.client

import android.content.Context
import com.BuildConfig
import com.data.remote.api.NewsApiService
import com.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    fun initialize(context: Context) {
        NewsApiRequestGuard.initialize(context)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        redactHeader("X-Api-Key")
        redactHeader("Authorization")
        redactHeader("X-App-Token")
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val newsApiKeyInterceptor = okhttp3.Interceptor { chain ->
        val originalRequest = chain.request()
        val hasApiKeyHeader = originalRequest.header("X-Api-Key").isNullOrBlank().not()
        val hasAppTokenHeader = originalRequest.header("X-App-Token").isNullOrBlank().not()
        val builder = originalRequest.newBuilder()

        // Direct NewsAPI mode: app attaches X-Api-Key.
        // Backend proxy mode: leave NEWS_API_KEY blank and set BACKEND_APP_TOKEN.
        if (Constants.NEWS_API_KEY.isNotBlank() && !hasApiKeyHeader) {
            builder.header("X-Api-Key", Constants.NEWS_API_KEY)
        }

        if (Constants.BACKEND_APP_TOKEN.isNotBlank() && !hasAppTokenHeader) {
            builder.header("X-App-Token", Constants.BACKEND_APP_TOKEN)
        }

        chain.proceed(builder.build())
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(newsApiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val newsApiService: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }
}