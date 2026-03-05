package com.livetvpremium.app.repository

import com.livetvpremium.app.model.TmdbDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class TmdbRepository {
    
    suspend fun fetchDetails(query: String, apiKey: String): TmdbDetails? = withContext(Dispatchers.IO) {
        if (query.isBlank() || apiKey.isBlank()) return@withContext null
        try {
            var cleanQuery = query.replace("""(?i)^\s*[A-Z]{2}\s*:\s*""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\(\d{4}\)""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\[.*?\]""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\|.*""".toRegex(), "")
            cleanQuery = cleanQuery.trim()
            if (cleanQuery.isBlank()) cleanQuery = query

            val encodedQuery = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val urlStr = "https://api.themoviedb.org/3/search/multi?query=$encodedQuery&language=it-IT&api_key=$apiKey"
            val connection = URL(urlStr).openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val resultsArray = jsonObject.optJSONArray("results")
                
                if (resultsArray != null && resultsArray.length() > 0) {
                    val firstResult = resultsArray.getJSONObject(0)
                    
                    val title = firstResult.optString("title", firstResult.optString("name", cleanQuery))
                    var overview = firstResult.optString("overview", "Trama non disponibile in italiano.")
                    if (overview.isBlank()) overview = "Trama non disponibile in italiano."
                    val posterPath = firstResult.optString("poster_path", null)
                    val backdropPath = firstResult.optString("backdrop_path", null)
                    val voteAverage = firstResult.optDouble("vote_average", 0.0)
                    val id = firstResult.optInt("id", -1)
                    val mediaType = firstResult.optString("media_type", "movie")
                    
                    var trailerUrl: String? = null
                    if (id != -1) {
                        try {
                            val vidUrlStr = "https://api.themoviedb.org/3/$mediaType/$id/videos?language=it-IT&api_key=$apiKey"
                            val vidConn = URL(vidUrlStr).openConnection() as HttpsURLConnection
                            vidConn.requestMethod = "GET"
                            vidConn.connectTimeout = 3000
                            vidConn.readTimeout = 3000
                            if (vidConn.responseCode == 200) {
                                val vidResponse = vidConn.inputStream.bufferedReader().use { it.readText() }
                                val vidJson = JSONObject(vidResponse)
                                val vidResults = vidJson.optJSONArray("results")
                                if (vidResults != null) {
                                    for (i in 0 until vidResults.length()) {
                                        val video = vidResults.getJSONObject(i)
                                        if (video.optString("site") == "YouTube" && video.optString("type") == "Trailer") {
                                            trailerUrl = "https://www.youtube.com/watch?v=" + video.optString("key")
                                            break
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    return@withContext TmdbDetails(
                        title = title,
                        overview = overview,
                        posterPath = posterPath,
                        backdropPath = backdropPath,
                        voteAverage = voteAverage,
                        trailerUrl = trailerUrl
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
