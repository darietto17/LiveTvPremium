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
                    val posterPath = if (firstResult.isNull("poster_path")) null else firstResult.optString("poster_path")
                    val backdropPath = if (firstResult.isNull("backdrop_path")) null else firstResult.optString("backdrop_path")
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

    suspend fun fetchReleaseDate(query: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        if (query.isBlank() || apiKey.isBlank()) return@withContext null
        try {
            var cleanQuery = query.replace("""(?i)^\s*[A-Z]{2}\s*:\s*""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\(\d{4}\)""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\[.*?\]""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\|.*""".toRegex(), "")
            cleanQuery = cleanQuery.trim()
            if (cleanQuery.isBlank()) cleanQuery = query

            val encodedQuery = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val urlStr = "https://api.themoviedb.org/3/search/movie?query=$encodedQuery&language=it-IT&api_key=$apiKey"
            val connection = URL(urlStr).openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val results = jsonObject.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val date = first.optString("release_date", "")
                    if (date.isNotBlank()) return@withContext date
                }
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun fetchSeriesLastAirDate(query: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        if (query.isBlank() || apiKey.isBlank()) return@withContext null
        try {
            var cleanQuery = query.replace("""(?i)^\s*[A-Z]{2}\s*:\s*""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\(\d{4}\)""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\[.*?\]""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""\|.*""".toRegex(), "")
            cleanQuery = cleanQuery.replace("""(?i)\s*S\d{1,2}\s*E\d{1,3}.*""".toRegex(), "")
            cleanQuery = cleanQuery.trim()
            if (cleanQuery.isBlank()) cleanQuery = query

            val encodedQuery = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val searchUrl = "https://api.themoviedb.org/3/search/tv?query=$encodedQuery&language=it-IT&api_key=$apiKey"
            val conn = URL(searchUrl).openConnection() as HttpsURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val results = JSONObject(response).optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val tvId = results.getJSONObject(0).optInt("id", -1)
                    if (tvId != -1) {
                        // Fetch detail to get last_air_date
                        val detailUrl = "https://api.themoviedb.org/3/tv/$tvId?api_key=$apiKey"
                        val detailConn = URL(detailUrl).openConnection() as HttpsURLConnection
                        detailConn.connectTimeout = 3000
                        detailConn.readTimeout = 3000
                        if (detailConn.responseCode == 200) {
                            val detailJson = JSONObject(detailConn.inputStream.bufferedReader().use { it.readText() })
                            val lastAir = detailJson.optString("last_air_date", "")
                            if (lastAir.isNotBlank()) return@withContext lastAir
                        }
                    }
                    // Fallback to first_air_date from search
                    val firstAir = results.getJSONObject(0).optString("first_air_date", "")
                    if (firstAir.isNotBlank()) return@withContext firstAir
                }
            }
        } catch (_: Exception) {}
        null
    }
}
