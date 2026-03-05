package com.livetvpremium.app.model

import kotlinx.serialization.Serializable

@Serializable
data class M3UItem(
    val name: String,
    val groupTitle: String,
    val tvgId: String,
    val tvgLogo: String,
    val url: String
)
