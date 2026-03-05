package com.livetvpremium.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livetvpremium.app.model.M3UItem
import com.livetvpremium.app.repository.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    fun syncAll(githubToken: String? = null, lastSyncTime: Long = 0L, forceSync: Boolean = false, context: android.content.Context) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading(0.1f, "Vefication sync status...")
            try {
                val isCacheValid = !forceSync && isToday(lastSyncTime)
                
                val liveFile = getPlaylistFile("lista.m3u", isCacheValid, githubToken, context)
                _syncState.value = SyncState.Loading(0.4f, "Parsing Live TV...")
                val parsedLive = liveFile?.inputStream()?.use { m3uParser.parse(it) } ?: emptyList()
                _liveItems.value = parsedLive
                liveGroups.value = parsedLive.map { it.groupTitle }.distinct().sorted()

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
}
