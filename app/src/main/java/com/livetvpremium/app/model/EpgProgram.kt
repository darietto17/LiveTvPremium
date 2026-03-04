package com.livetvpremium.app.model

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String,
    val startTime: String, // Ideally should be parsed to Long or Date
    val stopTime: String
)
