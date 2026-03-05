package com.livetvpremium.app.model

data class TmdbDetails(
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val trailerUrl: String? = null
)
