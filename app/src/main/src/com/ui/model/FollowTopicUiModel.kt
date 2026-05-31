package com.ui.model

data class FollowTopicUiModel(
    val id: String,
    val name: String,
    val isFollowed: Boolean,
    val newTodayCount: Int = 0,
    val notificationsEnabled: Boolean = true,
    val muted: Boolean = false,
    val blocked: Boolean = false,
    val type: String = "topic",
    val apiValue: String? = null
)

