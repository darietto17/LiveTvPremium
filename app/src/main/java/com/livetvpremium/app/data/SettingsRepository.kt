package com.livetvpremium.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        val GITHUB_TOKEN_KEY = stringPreferencesKey("github_token")
        val TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
        val USE_VLC_PLAYER = booleanPreferencesKey("use_vlc_player")
        val PROXY_URL = stringPreferencesKey("proxy_url")
    }

    val githubTokenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GITHUB_TOKEN_KEY] ?: ""
    }

    val tmdbApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val saved = preferences[TMDB_API_KEY]
        if (saved.isNullOrEmpty()) "2b45f26a5e7788a16d6f59136f2635d9" else saved
    }

    val useVlcPlayerFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_VLC_PLAYER] ?: false
    }

    val proxyUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROXY_URL] ?: ""
    }

    suspend fun saveGithubToken(token: String) {
        context.dataStore.edit { preferences -> preferences[GITHUB_TOKEN_KEY] = token }
    }

    suspend fun saveTmdbApiKey(key: String) {
        context.dataStore.edit { preferences -> preferences[TMDB_API_KEY] = key }
    }

    suspend fun saveUseVlcPlayer(useVlc: Boolean) {
        context.dataStore.edit { preferences -> preferences[USE_VLC_PLAYER] = useVlc }
    }

    suspend fun saveProxyUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[PROXY_URL] = url }
    }
}
