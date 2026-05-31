package com.util

class PaginationHelper(
    initialPage: Int = Constants.INITIAL_PAGE,
    val pageSize: Int = Constants.DEFAULT_PAGE_SIZE
) {
    private var currentPage = initialPage
    private var isLoadingMore = false
    private var canLoadMore = true

    /**
     * Get current pagination state as a data class
     */
    fun getState(): PaginationState {
        return PaginationState(
            currentPage = currentPage,
            isLoadingMore = isLoadingMore,
            canLoadMore = canLoadMore
        )
    }

    /**
     * Check if we should load more and return next page number, or null if we shouldn't load
     */
    fun loadNext(): Int? {
        return if (shouldLoadMore()) {
            currentPage++
            currentPage
        } else {
            null
        }
    }

    /**
     * Call this after a successful load to update pagination state
     * @param itemCount Number of items returned from the load
     */
    fun onLoadComplete(itemCount: Int) {
        canLoadMore = itemCount >= pageSize
        isLoadingMore = false
    }

    /**
     * Call this when a load error occurs
     */
    fun onLoadError() {
        isLoadingMore = false
    }

    /**
     * Reset pagination to initial state
     */
    fun reset(initialPage: Int = Constants.INITIAL_PAGE) {
        currentPage = initialPage
        isLoadingMore = false
        canLoadMore = true
    }

    /**
     * Manually set loading state (for UI feedback)
     */
    fun setLoading(loading: Boolean) {
        isLoadingMore = loading
    }

    /**
     * Check if we can load more
     */
    fun canLoadMore(): Boolean = canLoadMore && !isLoadingMore

    /**
     * Check if currently loading
     */
    fun isLoadingMore(): Boolean = isLoadingMore

    /**
     * Get current page
     */
    fun getCurrentPage(): Int = currentPage

    private fun shouldLoadMore(): Boolean {
        return !isLoadingMore && canLoadMore
    }
}
