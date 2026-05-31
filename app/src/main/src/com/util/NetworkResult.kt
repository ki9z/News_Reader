package com.util

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val type: ErrorType = ErrorType.UNKNOWN
    ) : NetworkResult<Nothing>()

    enum class ErrorType {
        UNAUTHORIZED,
        RATE_LIMITED,
        NETWORK,
        SERVER,
        CLIENT,
        UNKNOWN
    }
}