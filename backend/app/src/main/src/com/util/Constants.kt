package com.util

import com.BuildConfig

object Constants {
    const val BASE_URL = BuildConfig.NEWS_BASE_URL
    const val NEWS_API_KEY = BuildConfig.NEWS_API_KEY
    const val NEWS_API_DAILY_LIMIT = BuildConfig.NEWS_API_DAILY_LIMIT
    const val BACKEND_APP_TOKEN = BuildConfig.BACKEND_APP_TOKEN

    /**
     * Backend proxy mode is enabled when NEWS_BASE_URL does not point to NewsAPI.
     * In this mode Android can leave NEWS_API_KEY blank because the backend owns it.
     */
    val USES_BACKEND_PROXY: Boolean = !BASE_URL.contains("newsapi.org", ignoreCase = true)
    val CAN_CALL_REMOTE_NEWS: Boolean = NEWS_API_KEY.isNotBlank() || USES_BACKEND_PROXY

    const val DEFAULT_PAGE_SIZE = 50
    const val INITIAL_PAGE = 1

    const val CATEGORY_GENERAL = "general"
    const val CATEGORY_BUSINESS = "business"
    const val CATEGORY_ENTERTAINMENT = "entertainment"
    const val CATEGORY_HEALTH = "health"
    const val CATEGORY_SPORTS = "sports"
    const val CATEGORY_TECHNOLOGY = "technology"

    data class SubCategory(
        val title: String,
        val apiCategory: String?,
        val query: String?
    )

    data class CategoryGroup(
        val title: String,
        val subCategories: List<SubCategory>
    )

    val CATEGORY_TREE = listOf(
        CategoryGroup(
            title = "Thời sự",
            subCategories = listOf(
                SubCategory("Chính trị", CATEGORY_GENERAL, "politics"),
                SubCategory("Xã hội", CATEGORY_GENERAL, "society"),
                SubCategory("Pháp luật", CATEGORY_GENERAL, "law")
            )
        ),
        CategoryGroup(
            title = "Thế giới",
            subCategories = listOf(
                SubCategory("Quốc tế", CATEGORY_GENERAL, "international"),
                SubCategory("Chiến sự", CATEGORY_GENERAL, "war"),
                SubCategory("Ngoại giao", CATEGORY_GENERAL, "diplomacy")
            )
        ),
        CategoryGroup(
            title = "Kinh tế",
            subCategories = listOf(
                SubCategory("Tài chính", CATEGORY_BUSINESS, "finance"),
                SubCategory("Chứng khoán", CATEGORY_BUSINESS, "stock market"),
                SubCategory("Bất động sản", CATEGORY_BUSINESS, "real estate")
            )
        ),
        CategoryGroup(
            title = "Công nghệ",
            subCategories = listOf(
                SubCategory("AI", CATEGORY_TECHNOLOGY, "artificial intelligence"),
                SubCategory("Startup", CATEGORY_BUSINESS, "startup"),
                SubCategory("Gadget", CATEGORY_TECHNOLOGY, "gadget")
            )
        ),
        CategoryGroup(
            title = "Thể thao",
            subCategories = listOf(
                SubCategory("Bóng đá", CATEGORY_SPORTS, "football"),
                SubCategory("Bóng rổ", CATEGORY_SPORTS, "basketball"),
                SubCategory("Esport", CATEGORY_SPORTS, "esports")
            )
        ),
        CategoryGroup(
            title = "Giải trí",
            subCategories = listOf(
                SubCategory("Showbiz", CATEGORY_ENTERTAINMENT, "showbiz"),
                SubCategory("Phim ảnh", CATEGORY_ENTERTAINMENT, "movie"),
                SubCategory("Âm nhạc", CATEGORY_ENTERTAINMENT, "music")
            )
        ),
        CategoryGroup(
            title = "Sức khỏe",
            subCategories = listOf(
                SubCategory("Y tế", CATEGORY_HEALTH, "healthcare"),
                SubCategory("Dinh dưỡng", CATEGORY_HEALTH, "nutrition"),
                SubCategory("Làm đẹp", CATEGORY_HEALTH, "beauty")
            )
        ),
        CategoryGroup(
            title = "Giáo dục",
            subCategories = listOf(
                SubCategory("Tuyển sinh", CATEGORY_GENERAL, "education admission"),
                SubCategory("Du học", CATEGORY_GENERAL, "study abroad"),
                SubCategory("Kỹ năng", CATEGORY_GENERAL, "skills")
            )
        ),
        CategoryGroup(
            title = "Du lịch",
            subCategories = listOf(
                SubCategory("Điểm đến", CATEGORY_GENERAL, "travel destination"),
                SubCategory("Kinh nghiệm", CATEGORY_GENERAL, "travel tips"),
                SubCategory("Ẩm thực", CATEGORY_GENERAL, "food")
            )
        ),
        CategoryGroup(
            title = "Xe",
            subCategories = listOf(
                SubCategory("Ô tô", CATEGORY_GENERAL, "car"),
                SubCategory("Xe máy", CATEGORY_GENERAL, "motorbike"),
                SubCategory("Thị trường xe", CATEGORY_GENERAL, "automotive market")
            )
        )
    )

    val DEFAULT_SUB_CATEGORY: SubCategory = CATEGORY_TREE.first().subCategories.first()
}
