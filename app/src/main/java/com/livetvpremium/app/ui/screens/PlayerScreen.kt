package com.livetvpremium.app.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.livetvpremium.app.ui.viewmodel.SettingsViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    url: String,
    title: String,
    groupName: String,
    posterUrl: String?,
    settingsViewModel: SettingsViewModel,
    mainViewModel: com.livetvpremium.app.ui.viewmodel.MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val useVlcPlayer by settingsViewModel.useVlcPlayer.collectAsState()
    val isVod = groupName.contains("Film", ignoreCase = true) || groupName.contains("Serie", ignoreCase = true)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (useVlcPlayer) {
            // Placeholder for VLC Player implementation
            // Need to setup LibVLC and org.videolan.libvlc.util.VLCVideoLayout
            // We use a simple view for now
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // ExoPlayer Implementation
            var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
            val watchHistory by settingsViewModel.watchHistory.collectAsState()
            
            val saveHistory: (Long, Long) -> Unit = remember(url, title, groupName, posterUrl) {
                { currentPos, duration -> 
                    if (isVod && currentPos > 5000 && duration > 0) {
                        val isCompleted = duration > 0 && currentPos >= duration - 15000L
                        if (isCompleted && groupName.contains("Serie", ignoreCase = true)) {
                            val episodes = mainViewModel.serieItems.value.filter { it.groupTitle == groupName }
                            val regex = "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE)
                            val currentMatch = regex.find(title)
                            val currentSeriesName = currentMatch?.groupValues?.get(1)?.trim() ?: title
                            
                            val sameSeriesEpisodes = episodes.filter { 
                                (regex.find(it.name)?.groupValues?.get(1)?.trim() ?: it.name) == currentSeriesName
                            }.sortedBy { it.name }
                            
                            val currentIndex = sameSeriesEpisodes.indexOfFirst { it.name == title }
                            if (currentIndex != -1 && currentIndex < sameSeriesEpisodes.size - 1) {
                                val nextEpisode = sameSeriesEpisodes[currentIndex + 1]
                                val historyItem = com.livetvpremium.app.model.WatchHistoryItem(
                                    title = nextEpisode.name,
                                    originalUrl = nextEpisode.url,
                                    groupName = nextEpisode.groupTitle,
                                    posterUrl = posterUrl,
                                    progressMs = 0L,
                                    durationMs = 0L,
                                    timestamp = System.currentTimeMillis()
                                )
                                settingsViewModel.addOrUpdateWatchHistoryItem(historyItem)
                            } else {
                                settingsViewModel.removeWatchHistoryItem(url)
                            }
                        } else if (!isCompleted || !groupName.contains("Serie", ignoreCase = true)) {
                            val historyItem = com.livetvpremium.app.model.WatchHistoryItem(
                                title = title,
                                originalUrl = url,
                                groupName = groupName,
                                posterUrl = posterUrl,
                                progressMs = currentPos,
                                durationMs = duration,
                                timestamp = System.currentTimeMillis()
                            )
                            settingsViewModel.addOrUpdateWatchHistoryItem(historyItem)
                        }
                    }
                }
            }
            
            DisposableEffect(url) {
                val trackSelector = DefaultTrackSelector(context).apply {
                    setParameters(buildUponParameters().setPreferredAudioLanguage("it"))
                }
                val player = ExoPlayer.Builder(context).setTrackSelector(trackSelector).build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(url))
                    setMediaItem(mediaItem)
                    
                    if (isVod) {
                        val existingHistory = watchHistory.find { it.originalUrl == url }
                        if (existingHistory != null && existingHistory.progressMs > 0) {
                            val startPosition = (existingHistory.progressMs - 5000L).coerceAtLeast(0L)
                            seekTo(startPosition)
                        }
                    }
                    
                    prepare()
                    playWhenReady = true
                }
                exoPlayer = player
                
                onDispose {
                    val currentPos = player.currentPosition
                    val duration = player.duration
                    saveHistory(currentPos, duration)
                    player.release()
                }
            }
            
            // Poll progress every 10 seconds to save state proactively
            LaunchedEffect(exoPlayer) {
                while (true) {
                    kotlinx.coroutines.delay(10000)
                    exoPlayer?.let { player ->
                        if (player.isPlaying) {
                            saveHistory(player.currentPosition, player.duration)
                        }
                    }
                }
            }
            
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> {
                            exoPlayer?.pause()
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            exoPlayer?.play()
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            exoPlayer?.let { player ->
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            setShowSubtitleButton(true)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            // D-pad support requires deeper customization of controller layout
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
