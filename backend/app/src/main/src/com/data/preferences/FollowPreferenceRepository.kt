package com.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FollowPreferenceRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _followedSourceIds = MutableStateFlow(readSet(KEY_FOLLOWED_SOURCE_IDS))
    val followedSourceIds: StateFlow<Set<String>> = _followedSourceIds.asStateFlow()

    private val _followedKeywords = MutableStateFlow(readSet(KEY_FOLLOWED_KEYWORDS))
    val followedKeywords: StateFlow<Set<String>> = _followedKeywords.asStateFlow()

    private val _mutedItemIds = MutableStateFlow(readSet(KEY_MUTED_ITEM_IDS))
    val mutedItemIds: StateFlow<Set<String>> = _mutedItemIds.asStateFlow()

    private val _blockedItemIds = MutableStateFlow(readSet(KEY_BLOCKED_ITEM_IDS))
    val blockedItemIds: StateFlow<Set<String>> = _blockedItemIds.asStateFlow()

    fun getFollowedSourceIds(): Set<String> = _followedSourceIds.value

    fun getFollowedKeywords(): Set<String> = _followedKeywords.value

    fun getMutedItemIds(): Set<String> = _mutedItemIds.value

    fun getBlockedItemIds(): Set<String> = _blockedItemIds.value

    fun setSourceFollowed(sourceId: String, followed: Boolean) {
        val normalized = sourceId.trim().lowercase()
        if (normalized.isBlank()) return

        val next = _followedSourceIds.value.toMutableSet().apply {
            if (followed) add(normalized) else remove(normalized)
        }
        writeSet(KEY_FOLLOWED_SOURCE_IDS, next)
        _followedSourceIds.value = next
    }

    fun setKeywordFollowed(keyword: String, followed: Boolean) {
        val normalized = keyword.trim()
        if (normalized.isBlank()) return

        val next = _followedKeywords.value.toMutableSet().apply {
            val existing = firstOrNull { it.equals(normalized, ignoreCase = true) }
            if (existing != null) remove(existing)
            if (followed) add(normalized)
        }
        writeSet(KEY_FOLLOWED_KEYWORDS, next)
        _followedKeywords.value = next
    }

    fun setItemMuted(itemId: String, muted: Boolean) {
        val normalized = normalizeItemId(itemId) ?: return
        val next = _mutedItemIds.value.toMutableSet().apply {
            if (muted) add(normalized) else remove(normalized)
        }
        writeSet(KEY_MUTED_ITEM_IDS, next)
        _mutedItemIds.value = next
    }

    fun setItemBlocked(itemId: String, blocked: Boolean) {
        val normalized = normalizeItemId(itemId) ?: return
        val next = _blockedItemIds.value.toMutableSet().apply {
            if (blocked) add(normalized) else remove(normalized)
        }
        writeSet(KEY_BLOCKED_ITEM_IDS, next)
        _blockedItemIds.value = next
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_FOLLOWED_SOURCE_IDS)
            .remove(KEY_FOLLOWED_KEYWORDS)
            .remove(KEY_MUTED_ITEM_IDS)
            .remove(KEY_BLOCKED_ITEM_IDS)
            .apply()
        _followedSourceIds.value = emptySet()
        _followedKeywords.value = emptySet()
        _mutedItemIds.value = emptySet()
        _blockedItemIds.value = emptySet()
    }

    private fun normalizeItemId(itemId: String): String? {
        return itemId.trim().takeIf { it.isNotBlank() }
    }

    private fun readSet(key: String): Set<String> {
        return prefs.getString(key, null)
            ?.split(SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    private fun writeSet(key: String, values: Set<String>) {
        prefs.edit()
            .putString(key, values.sorted().joinToString(SEPARATOR))
            .apply()
    }

    companion object {
        private const val PREF_NAME = "follow_preferences"
        private const val KEY_FOLLOWED_SOURCE_IDS = "followed_source_ids"
        private const val KEY_FOLLOWED_KEYWORDS = "followed_keywords"
        private const val KEY_MUTED_ITEM_IDS = "muted_item_ids"
        private const val KEY_BLOCKED_ITEM_IDS = "blocked_item_ids"
        private const val SEPARATOR = "|"
    }
}
