package com.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.model.NewsSource
import com.data.preferences.FollowPreferenceRepository
import com.data.repository.NewsRepository
import com.data.repository.ProfileRepository
import com.ui.model.FollowTopicUiModel
import com.util.NetworkResult
import com.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue

class FollowingTopicsViewModel(
    private val profileRepository: ProfileRepository,
    private val newsRepository: NewsRepository,
    private val followPreferenceRepository: FollowPreferenceRepository
) : ViewModel() {

    enum class FollowTab { TOPICS, SOURCES, KEYWORDS }

    private val allTopics = MutableStateFlow<List<FollowTopicUiModel>>(emptyList())
    private val query = MutableStateFlow("")
    private val mutedIds = MutableStateFlow(followPreferenceRepository.getMutedItemIds())
    private val blockedIds = MutableStateFlow(followPreferenceRepository.getBlockedItemIds())
    private val followTab = MutableStateFlow(FollowTab.TOPICS)
    private val followedSourceIds = MutableStateFlow(followPreferenceRepository.getFollowedSourceIds())
    private val followedKeywords = MutableStateFlow(followPreferenceRepository.getFollowedKeywords())

    private val sourceItems = MutableStateFlow(defaultSourceItems())

    private val keywordItems = MutableStateFlow(
        listOf(
            keywordItem("AI"),
            keywordItem("Startup"),
            keywordItem("Education"),
            keywordItem("Vietnam"),
            keywordItem("Football"),
            keywordItem("Health"),
            keywordItem("Climate"),
            keywordItem("Business")
        )
    )

    private val sourceItemsWithFollowState = combine(sourceItems, followedSourceIds) { sources, followed ->
        sources.map { item ->
            val value = item.apiValue ?: sourceIdFromUiId(item.id)
            item.copy(isFollowed = !value.isNullOrBlank() && followed.contains(value.lowercase(Locale.ROOT)))
        }
    }

    private val keywordItemsWithFollowState = combine(keywordItems, followedKeywords) { keywords, followed ->
        keywords.map { item ->
            val value = item.apiValue ?: item.name
            item.copy(isFollowed = followed.any { it.equals(value, ignoreCase = true) })
        }
    }

    private val mergedTopicData = combine(allTopics, sourceItemsWithFollowState) { topics, sources ->
        topics to sources
    }

    private val groupedTopicData = combine(mergedTopicData, keywordItemsWithFollowState) { topicAndSource, keywords ->
        Triple(topicAndSource.first, topicAndSource.second, keywords)
    }

    private val baseItems = combine(groupedTopicData, followTab) { grouped, tab ->
        when (tab) {
            FollowTab.TOPICS -> grouped.first
            FollowTab.SOURCES -> grouped.second
            FollowTab.KEYWORDS -> grouped.third
        }
    }

    private val queriedItems = combine(baseItems, query) { base, keyword ->
        base.filter { keyword.isBlank() || it.name.contains(keyword, ignoreCase = true) }
    }

    private val mutedDecorated = combine(queriedItems, mutedIds) { items, muted ->
        items.map { it.copy(muted = muted.contains(it.id)) }
    }

    val uiState: StateFlow<UiState<List<FollowTopicUiModel>>> = combine(mutedDecorated, blockedIds) { items, blocked ->
        val decorated = items
            .map { it.copy(blocked = blocked.contains(it.id)) }
            .filterNot { it.blocked }

        if (decorated.isEmpty()) UiState.Empty else UiState.Success(decorated)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )

    init {
        observeTopics()
        observeFollowPreferences()
        loadLiveSources()
    }

    fun toggleTopic(item: FollowTopicUiModel) {
        viewModelScope.launch {
            when (item.type) {
                "topic" -> profileRepository.toggleFollowTopic(item)
                "source" -> toggleSourceFollow(item)
                "keyword" -> toggleKeywordFollow(item)
                else -> Unit
            }
        }
    }

    fun setTab(tab: FollowTab) {
        followTab.value = tab
        if (tab == FollowTab.SOURCES && sourceItems.value.isEmpty()) {
            loadLiveSources()
        }
    }

    fun setQuery(value: String) {
        query.value = value.trim()
    }

    fun toggleMute(item: FollowTopicUiModel) {
        followPreferenceRepository.setItemMuted(item.id, muted = !mutedIds.value.contains(item.id))
    }

    fun blockTopic(item: FollowTopicUiModel) {
        followPreferenceRepository.setItemBlocked(item.id, blocked = true)
    }

    fun resetRecommendations() {
        viewModelScope.launch {
            profileRepository.resetFollowingTopics()
            followPreferenceRepository.clear()
            followedSourceIds.value = emptySet()
            followedKeywords.value = emptySet()
            mutedIds.value = emptySet()
            blockedIds.value = emptySet()
        }
    }

    private fun observeTopics() {
        viewModelScope.launch {
            profileRepository.bootstrap()
            profileRepository.observeFollowTopics().collect { topics ->
                allTopics.value = topics
            }
        }
    }

    private fun observeFollowPreferences() {
        viewModelScope.launch {
            followPreferenceRepository.followedSourceIds.collect { followedSourceIds.value = it }
        }
        viewModelScope.launch {
            followPreferenceRepository.followedKeywords.collect { followedKeywords.value = it }
        }
        viewModelScope.launch {
            followPreferenceRepository.mutedItemIds.collect { mutedIds.value = it }
        }
        viewModelScope.launch {
            followPreferenceRepository.blockedItemIds.collect { blockedIds.value = it }
        }
    }

    private fun loadLiveSources() {
        viewModelScope.launch {
            when (val result = newsRepository.getSources(language = "en")) {
                is NetworkResult.Success -> {
                    val mapped = result.data
                        .filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }
                        .sortedWith(compareBy<NewsSource> { it.category.orEmpty() }.thenBy { it.name.orEmpty() })
                        .take(40)
                        .map { source -> source.toFollowItem() }

                    if (mapped.isNotEmpty()) {
                        sourceItems.value = mapped
                    }
                }
                is NetworkResult.Error -> Unit
            }
        }
    }

    private fun toggleSourceFollow(item: FollowTopicUiModel) {
        val sourceId = item.apiValue ?: sourceIdFromUiId(item.id) ?: return
        followPreferenceRepository.setSourceFollowed(sourceId, followed = !item.isFollowed)
    }

    private fun toggleKeywordFollow(item: FollowTopicUiModel) {
        val keyword = item.apiValue ?: item.name
        followPreferenceRepository.setKeywordFollowed(keyword, followed = !item.isFollowed)
    }

    private fun NewsSource.toFollowItem(): FollowTopicUiModel {
        val sourceId = id!!.trim().lowercase(Locale.ROOT)
        val countSeed = (sourceId.hashCode().absoluteValue % 12) + 1
        val metaBoost = when (category?.lowercase(Locale.ROOT)) {
            "technology", "business", "sports" -> 2
            else -> 0
        }

        return FollowTopicUiModel(
            id = "source_$sourceId",
            name = name!!.trim(),
            isFollowed = followedSourceIds.value.contains(sourceId),
            type = "source",
            apiValue = sourceId,
            newTodayCount = countSeed + metaBoost
        )
    }

    private fun keywordItem(keyword: String): FollowTopicUiModel {
        return FollowTopicUiModel(
            id = "kw_${keyword.lowercase(Locale.ROOT).replace(" ", "_")}",
            name = keyword,
            isFollowed = followedKeywords.value.any { it.equals(keyword, ignoreCase = true) },
            type = "keyword",
            apiValue = keyword,
            newTodayCount = ((keyword.length * 5) % 10) + 2
        )
    }

    private fun defaultSourceItems(): List<FollowTopicUiModel> {
        return listOf(
            FollowTopicUiModel(id = "source_bbc-news", name = "BBC News", isFollowed = false, type = "source", apiValue = "bbc-news", newTodayCount = 8),
            FollowTopicUiModel(id = "source_cnn", name = "CNN", isFollowed = false, type = "source", apiValue = "cnn", newTodayCount = 5),
            FollowTopicUiModel(id = "source_reuters", name = "Reuters", isFollowed = false, type = "source", apiValue = "reuters", newTodayCount = 10),
            FollowTopicUiModel(id = "source_techcrunch", name = "TechCrunch", isFollowed = false, type = "source", apiValue = "techcrunch", newTodayCount = 6),
            FollowTopicUiModel(id = "source_the-verge", name = "The Verge", isFollowed = false, type = "source", apiValue = "the-verge", newTodayCount = 4)
        )
    }

    private fun sourceIdFromUiId(id: String): String? {
        return id.removePrefix("source_").takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
    }
}
