package com.util

data class PaginationState(
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true
)
