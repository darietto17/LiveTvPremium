package com.livetvpremium.app.model

import kotlinx.serialization.Serializable

@Serializable
data class WatchHistoryItem(
    val title: String,
    val originalUrl: String,
    val groupName: String,
    val posterUrl: String?,
    val progressMs: Long,
    val durationMs: Long,
    val timestamp: Long
)
