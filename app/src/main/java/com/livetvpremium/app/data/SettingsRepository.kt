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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.livetvpremium.app.model.WatchHistoryItem

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        val GITHUB_TOKEN_KEY = stringPreferencesKey("github_token")
        val TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
        val USE_VLC_PLAYER = booleanPreferencesKey("use_vlc_player")
        val PROXY_URL = stringPreferencesKey("proxy_url")
        val LAST_SYNC_TIME = androidx.datastore.preferences.core.longPreferencesKey("last_sync_time")
        val WATCH_HISTORY = stringPreferencesKey("watch_history")
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
    
    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_SYNC_TIME] ?: 0L
    }

    val watchHistoryFlow: Flow<List<WatchHistoryItem>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[WATCH_HISTORY] ?: "[]"
        try {
            Json.decodeFromString<List<WatchHistoryItem>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
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
    
    suspend fun saveLastSyncTime(time: Long) {
        context.dataStore.edit { preferences -> preferences[LAST_SYNC_TIME] = time }
    }

    suspend fun saveWatchHistory(items: List<WatchHistoryItem>) {
        val jsonStr = Json.encodeToString(items)
        context.dataStore.edit { preferences -> preferences[WATCH_HISTORY] = jsonStr }
    }
}
