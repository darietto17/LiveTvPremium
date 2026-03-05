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
                    if (isVod && currentPos > 5000 && duration > 0) {
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
                    player.release()
                }
            }
            
            // Poll progress every 10 seconds to save state proactively
            LaunchedEffect(exoPlayer) {
                while (true) {
                    kotlinx.coroutines.delay(10000)
                    exoPlayer?.let { player ->
                        val currentPos = player.currentPosition
                        val duration = player.duration
                        if (isVod && currentPos > 5000 && duration > 0 && player.isPlaying) {
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
