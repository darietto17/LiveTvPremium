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

    private val _allItems = MutableStateFlow<List<M3UItem>>(emptyList())
    val allItems: StateFlow<List<M3UItem>> = _allItems

    val groups = MutableStateFlow<List<String>>(emptyList())
    
    fun syncPlaylist(url: String, proxyUrl: String? = null, token: String? = null) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading(0.1f, "Connecting to GitHub...")
            try {
                val content = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpsURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    
                    if (!token.isNullOrEmpty()) {
                        connection.setRequestProperty("Authorization", "token $token")
                    }
                    
                    // Basic proxy handling could be added here if proxyUrl is valid
                    
                    val inputStream = connection.inputStream
                    _syncState.value = SyncState.Loading(0.5f, "Downloading Playlist...")
                    inputStream.bufferedReader().use { it.readText() }
                }
                
                _syncState.value = SyncState.Loading(0.8f, "Parsing Playlist...")
                val items = m3uParser.parse(content)
                
                _allItems.value = items
                groups.value = items.map { it.groupTitle }.distinct().sorted()
                
                _syncState.value = SyncState.Success(items)
                
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e)
            }
        }
    }
}
