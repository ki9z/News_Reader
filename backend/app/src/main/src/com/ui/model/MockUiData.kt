package com.ui.model

object MockUiData {
    val categories = listOf("Top", "Local", "Following", "Entertainment", "Lifestyle", "U.S.")

    val sources = listOf("Top News", "The Guardian", "ESPN", "ABC News")

    val exploreTopics = listOf(
        ExploreTopicItem("us-politics", "U.S. Politics", "https://picsum.photos/seed/explore_1/220/220"),
        ExploreTopicItem("foreign-policy", "Foreign policy", "https://picsum.photos/seed/explore_2/220/220"),
        ExploreTopicItem("middle-east", "Middle East", "https://picsum.photos/seed/explore_3/220/220"),
        ExploreTopicItem("entertainment", "Entertainment", "https://picsum.photos/seed/explore_4/220/220"),
        ExploreTopicItem("movies", "Movies", "https://picsum.photos/seed/explore_5/220/220"),
        ExploreTopicItem("technology", "Technology", "https://picsum.photos/seed/explore_6/220/220"),
        ExploreTopicItem("science", "Science", "https://picsum.photos/seed/explore_7/220/220"),
        ExploreTopicItem("business", "Business", "https://picsum.photos/seed/explore_8/220/220"),
        ExploreTopicItem("health", "Health", "https://picsum.photos/seed/explore_9/220/220"),
        ExploreTopicItem("sports", "Sports", "https://picsum.photos/seed/explore_10/220/220")
    )

    val homeNews = listOf(
        NewsItem(
            id = "1",
            title = "Morning briefing: global markets, weather alerts, and top headlines to watch",
            source = "Top News",
            thumbnailUrl = "https://picsum.photos/seed/news_1/300/220",
            category = "Top"
        ),
        NewsItem(
            id = "2",
            title = "City officials outline new transport plan for the downtown corridor",
            source = "Local Desk",
            thumbnailUrl = "https://picsum.photos/seed/news_2/300/220",
            category = "Local"
        ),
        NewsItem(
            id = "3",
            title = "Technology companies expand AI safety teams as regulation evolves",
            source = "Tech Review",
            thumbnailUrl = "https://picsum.photos/seed/news_3/300/220",
            category = "Following"
        ),
        NewsItem(
            id = "4",
            title = "Independent film festival announces its full weekend lineup",
            source = "Culture Wire",
            thumbnailUrl = "https://picsum.photos/seed/news_4/300/220",
            category = "Entertainment"
        ),
        NewsItem(
            id = "5",
            title = "How readers are building healthier daily news routines",
            source = "Lifestyle Today",
            thumbnailUrl = "https://picsum.photos/seed/news_5/300/220",
            category = "Lifestyle"
        ),
        NewsItem(
            id = "6",
            title = "Policy roundup: key state-level decisions from the week",
            source = "Civic Journal",
            thumbnailUrl = "https://picsum.photos/seed/news_6/300/220",
            category = "U.S."
        ),
        NewsItem(
            id = "7",
            title = "Researchers publish new climate resilience report for coastal cities",
            source = "Science Daily",
            thumbnailUrl = "https://picsum.photos/seed/news_7/300/220",
            category = "Top"
        ),
        NewsItem(
            id = "8",
            title = "Championship preview: what to expect from tonight's matchup",
            source = "ESPN",
            thumbnailUrl = "https://picsum.photos/seed/news_8/300/220",
            category = "Following"
        )
    )

    val trending = listOf(
        TrendingItem(1, "Local elections", "Key dates, voter resources, and candidate explainers are drawing reader interest.", "https://picsum.photos/seed/trend_1/200/140"),
        TrendingItem(2, "AI regulation", "Governments and companies are debating new guardrails for generative AI tools.", "https://picsum.photos/seed/trend_2/200/140"),
        TrendingItem(3, "Climate resilience", "Cities are preparing infrastructure plans for extreme weather and rising temperatures.", "https://picsum.photos/seed/trend_3/200/140"),
        TrendingItem(4, "Space exploration", "New missions and launch milestones continue to attract broad public interest.", "https://picsum.photos/seed/trend_4/200/140"),
        TrendingItem(5, "Health research", "Fresh studies explore prevention, nutrition, and public health policy.", "https://picsum.photos/seed/trend_5/200/140"),
        TrendingItem(6, "Market watch", "Investors track earnings, inflation signals, and central bank commentary.", "https://picsum.photos/seed/trend_6/200/140"),
        TrendingItem(7, "Streaming releases", "New films and series are competing for attention across major platforms.", "https://picsum.photos/seed/trend_7/200/140"),
        TrendingItem(8, "Sports finals", "Analysts break down momentum, injuries, and coaching decisions.", "https://picsum.photos/seed/trend_8/200/140")
    )
}
