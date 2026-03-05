package com.livetvpremium.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livetvpremium.app.model.M3UItem
import com.livetvpremium.app.repository.M3UParser
import com.livetvpremium.app.repository.TmdbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val progress: Float, val message: String) : SyncState()
    data class Success(val items: List<M3UItem>) : SyncState()
    data class Error(val exception: Throwable) : SyncState()
}

class MainViewModel(private val m3uParser: M3UParser) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _liveItems = MutableStateFlow<List<M3UItem>>(emptyList())
    val liveItems: StateFlow<List<M3UItem>> = _liveItems

    private val _filmItems = MutableStateFlow<List<M3UItem>>(emptyList())
    val filmItems: StateFlow<List<M3UItem>> = _filmItems

    private val _serieItems = MutableStateFlow<List<M3UItem>>(emptyList())
    val serieItems: StateFlow<List<M3UItem>> = _serieItems

    val liveGroups = MutableStateFlow<List<String>>(emptyList())
    val filmGroups = MutableStateFlow<List<String>>(emptyList())
    val serieGroups = MutableStateFlow<List<String>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<M3UItem>>(emptyList())
    val searchResults: StateFlow<List<M3UItem>> = _searchResults

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val q = query.trim()
            val results = mutableListOf<M3UItem>()
            val limit = 100
            for (item in _liveItems.value) {
                if (item.name.contains(q, ignoreCase = true)) {
                    results.add(item)
                    if (results.size >= limit) break
                }
            }
            if (results.size < limit) {
                for (item in _filmItems.value) {
                    if (item.name.contains(q, ignoreCase = true)) {
                        results.add(item)
                        if (results.size >= limit) break
                    }
                }
            }
            if (results.size < limit) {
                for (item in _serieItems.value) {
                    if (item.name.contains(q, ignoreCase = true)) {
                        results.add(item)
                        if (results.size >= limit) break
                    }
                }
            }
            _searchResults.value = results
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    private val _recentFilms = MutableStateFlow<List<M3UItem>>(emptyList())
    val recentFilms: StateFlow<List<M3UItem>> = _recentFilms

    fun fetchRecentFilms(tmdbApiKey: String) {
        if (tmdbApiKey.isBlank()) return
        viewModelScope.launch(Dispatchers.Default) {
            val films = _filmItems.value
            if (films.isEmpty()) return@launch

            val yearRegex = "\\((\\d{4})\\)".toRegex()
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

            // Pre-filter: candidates that have a year >= (currentYear - 3) in their title
            val candidates = films.filter { item ->
                val year = yearRegex.find(item.name)?.groupValues?.get(1)?.toIntOrNull()
                year != null && year >= currentYear - 3
            }.take(80) // limit to avoid excessive API calls

            // Immediately show year-sorted candidates while TMDB lookup runs
            val yearSorted = candidates.sortedByDescending { item ->
                yearRegex.find(item.name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }.take(50)
            _recentFilms.value = yearSorted

            // Now enrich with exact TMDB release dates
            val repo = TmdbRepository()
            val semaphore = Semaphore(5)
            val datedFilms = candidates.map { item ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val date = repo.fetchReleaseDate(item.name, tmdbApiKey)
                        item to (date ?: "0000-00-00")
                    }
                }
            }.awaitAll()

            val sorted = datedFilms
                .sortedByDescending { it.second }
                .take(50)
                .map { it.first }

            _recentFilms.value = sorted
        }
    }

    private val _recentSeriesList = MutableStateFlow<List<M3UItem>>(emptyList())
    val recentSeriesList: StateFlow<List<M3UItem>> = _recentSeriesList

    fun fetchRecentSeries(tmdbApiKey: String) {
        if (tmdbApiKey.isBlank()) return
        viewModelScope.launch(Dispatchers.Default) {
            val series = _serieItems.value
            if (series.isEmpty()) return@launch

            val serieRegex = "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE)

            // Deduplicate by title, keep first occurrence as representative
            val uniqueSeries = mutableMapOf<String, M3UItem>()
            for (item in series) {
                val match = serieRegex.find(item.name)
                val title = match?.groupValues?.get(1)?.trim() ?: item.name
                if (!uniqueSeries.containsKey(title)) {
                    uniqueSeries[title] = item.copy(name = title)
                    if (uniqueSeries.size >= 60) break
                }
            }

            val candidates = uniqueSeries.values.toList()
            // Show immediately while TMDB lookup runs
            _recentSeriesList.value = candidates.take(50)

            // Enrich with TMDB last_air_date
            val repo = TmdbRepository()
            val semaphore = Semaphore(5)
            val datedSeries = candidates.map { item ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val date = repo.fetchSeriesLastAirDate(item.name, tmdbApiKey)
                        item to (date ?: "0000-00-00")
                    }
                }
            }.awaitAll()

            val sorted = datedSeries
                .sortedByDescending { it.second }
                .take(50)
                .map { it.first }

            _recentSeriesList.value = sorted
        }
    }
    fun syncAll(githubToken: String? = null, lastSyncTime: Long = 0L, forceSync: Boolean = false, context: android.content.Context) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading(0.1f, "Vefication sync status...")
            try {
                val isCacheValid = !forceSync && isToday(lastSyncTime)
                
                val liveFile = getPlaylistFile("lista.m3u", isCacheValid, githubToken, context)
                _syncState.value = SyncState.Loading(0.4f, "Parsing Live TV...")
                val parsedLive = liveFile?.inputStream()?.use { m3uParser.parse(it) } ?: emptyList()
                _liveItems.value = parsedLive
                val orderedCategories = fetchOrderedCategories(githubToken, context)
                liveGroups.value = parsedLive.map { it.groupTitle }.distinct().sortedBy { groupName ->
                    val index = orderedCategories.indexOf(groupName)
                    if (index == -1) Int.MAX_VALUE else index
                }

                val filmFile = getPlaylistFile("film.m3u", isCacheValid, githubToken, context)
                _syncState.value = SyncState.Loading(0.7f, "Parsing Movies...")
                val parsedFilm = filmFile?.inputStream()?.use { m3uParser.parse(it) } ?: emptyList()
                _filmItems.value = parsedFilm
                filmGroups.value = parsedFilm.map { it.groupTitle }.distinct().sorted()

                val serieFile = getPlaylistFile("serie.m3u", isCacheValid, githubToken, context)
                _syncState.value = SyncState.Loading(0.9f, "Parsing Series...")
                val parsedSerie = serieFile?.inputStream()?.use { m3uParser.parse(it) } ?: emptyList()
                _serieItems.value = parsedSerie
                serieGroups.value = parsedSerie.map { it.groupTitle }.distinct().sorted()

                // Mark success
                _syncState.value = SyncState.Success(parsedLive + parsedFilm + parsedSerie) // Returning all just to satisfy the old Success signature, will refactor UI later.
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e)
            }
        }
    }

    private suspend fun getPlaylistFile(filename: String, useCache: Boolean, token: String?, context: android.content.Context): java.io.File? {
        return withContext(Dispatchers.IO) {
            val file = java.io.File(context.cacheDir, filename)
            if (useCache && file.exists() && file.length() > 0) {
                return@withContext file
            }
            
            // Need to download
            val url = "https://raw.githubusercontent.com/darietto17/LiveTvPremium/refs/heads/master/$filename"
            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (!token.isNullOrEmpty()) {
                connection.setRequestProperty("Authorization", "token $token")
            }
            
            if (connection.responseCode == 404) {
               return@withContext null // File might not exist yet on repo
            }
            
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val now = java.util.Calendar.getInstance()
        val syncDate = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(java.util.Calendar.YEAR) == syncDate.get(java.util.Calendar.YEAR) &&
               now.get(java.util.Calendar.DAY_OF_YEAR) == syncDate.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private suspend fun fetchOrderedCategories(token: String?, context: android.content.Context): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val file = java.io.File(context.cacheDir, "categories.json")
                val urlString = "https://raw.githubusercontent.com/darietto17/LiveTvPremium/refs/heads/master/scripts/categories.json"
                val connection = URL(urlString).openConnection() as HttpsURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (!token.isNullOrEmpty()) {
                    connection.setRequestProperty("Authorization", "token $token")
                }
                
                val jsonString = if (connection.responseCode == 200) {
                    val content = connection.inputStream.bufferedReader().use { it.readText() }
                    file.outputStream().use { it.write(content.toByteArray()) }
                    content
                } else if (file.exists()) {
                    file.readText()
                } else {
                    return@withContext defaultCategories()
                }
                
                val regex = "\"([^\"]+)\"\\s*:\\s*\\[".toRegex()
                val keys = regex.findAll(jsonString).map { it.groupValues[1] }.toList()
                
                if (keys.isNotEmpty()) {
                    if ("Altri" !in keys) keys + "Altri" else keys
                } else {
                    defaultCategories()
                }
            } catch (e: Exception) {
                defaultCategories()
            }
        }
    }

    private fun defaultCategories() = listOf(
        "Intrattenimento", "Sport", "Cinema", "Documentari", "Bambini", "Musica", "News", "Eventi Live", "Altri"
    )
}
