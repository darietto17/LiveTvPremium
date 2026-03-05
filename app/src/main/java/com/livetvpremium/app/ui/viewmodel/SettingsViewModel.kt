package com.livetvpremium.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livetvpremium.app.data.SettingsRepository
import com.livetvpremium.app.model.WatchHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _githubToken = MutableStateFlow("")
    val githubToken: StateFlow<String> = _githubToken

    private val _tmdbApiKey = MutableStateFlow("")
    val tmdbApiKey: StateFlow<String> = _tmdbApiKey

    private val _useVlcPlayer = MutableStateFlow(false)
    val useVlcPlayer: StateFlow<Boolean> = _useVlcPlayer

    private val _proxyUrl = MutableStateFlow("")
    val proxyUrl: StateFlow<String> = _proxyUrl

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime

    private val _watchHistory = MutableStateFlow<List<WatchHistoryItem>>(emptyList())
    val watchHistory: StateFlow<List<WatchHistoryItem>> = _watchHistory

    init {
        viewModelScope.launch {
            _githubToken.value = repository.githubTokenFlow.first()
            _tmdbApiKey.value = repository.tmdbApiKeyFlow.first()
            _useVlcPlayer.value = repository.useVlcPlayerFlow.first()
            _proxyUrl.value = repository.proxyUrlFlow.first()
            _lastSyncTime.value = repository.lastSyncTimeFlow.first()
            
            repository.watchHistoryFlow.collect { history ->
                _watchHistory.value = history
            }
        }
    }

    fun saveGithubToken(token: String) {
        _githubToken.value = token
        viewModelScope.launch { repository.saveGithubToken(token) }
    }

    fun saveTmdbApiKey(key: String) {
        _tmdbApiKey.value = key
        viewModelScope.launch { repository.saveTmdbApiKey(key) }
    }

    fun saveUseVlcPlayer(useVlc: Boolean) {
        _useVlcPlayer.value = useVlc
        viewModelScope.launch { repository.saveUseVlcPlayer(useVlc) }
    }

    fun saveProxyUrl(url: String) {
        _proxyUrl.value = url
        viewModelScope.launch { repository.saveProxyUrl(url) }
    }

    fun saveLastSyncTime(time: Long) {
        _lastSyncTime.value = time
        viewModelScope.launch { repository.saveLastSyncTime(time) }
    }

    fun addOrUpdateWatchHistoryItem(item: WatchHistoryItem) {
        val currentList = _watchHistory.value.toMutableList()
        val index = if (item.groupName.contains("Serie", ignoreCase = true)) {
            val regex = "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE)
            val newMatch = regex.find(item.title)
            val newSeriesName = newMatch?.groupValues?.get(1)?.trim() ?: item.title
            
            currentList.indexOfFirst { 
                it.groupName == item.groupName && 
                ((regex.find(it.title)?.groupValues?.get(1)?.trim() ?: it.title) == newSeriesName)
            }
        } else {
            currentList.indexOfFirst { it.title == item.title && it.groupName == item.groupName }
        }
        
        if (index != -1) {
            currentList[index] = item
        } else {
            currentList.add(0, item) // Add to top
        }
        
        // Keep only the last 50 items to prevent massive JSONs
        val trimmedList = currentList.sortedByDescending { it.timestamp }.take(50)
        
        _watchHistory.value = trimmedList
        viewModelScope.launch { repository.saveWatchHistory(trimmedList) }
    }

    fun removeWatchHistoryItem(url: String) {
        val currentList = _watchHistory.value.toMutableList()
        val index = currentList.indexOfFirst { it.originalUrl == url }
        if (index != -1) {
            currentList.removeAt(index)
            _watchHistory.value = currentList
            viewModelScope.launch { repository.saveWatchHistory(currentList) }
        }
    }
}
