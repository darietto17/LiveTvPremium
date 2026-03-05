package com.livetvpremium.app.model

data class EpgProgram(
    val channelId: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val startTime: Long,  // epoch millis
    val stopTime: Long    // epoch millis
)
