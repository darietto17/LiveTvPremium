package com.livetvpremium.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livetvpremium.app.data.SettingsRepository
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

    init {
        viewModelScope.launch {
            _githubToken.value = repository.githubTokenFlow.first()
            _tmdbApiKey.value = repository.tmdbApiKeyFlow.first()
            _useVlcPlayer.value = repository.useVlcPlayerFlow.first()
            _proxyUrl.value = repository.proxyUrlFlow.first()
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
}
