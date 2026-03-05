package com.livetvpremium.app.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.viewinterop.AndroidView
import android.app.Activity
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
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
    val view = LocalView.current
    
    // Manage Screen On and Fullscreen Immersive Mode
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            // Keep Screen On
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Immersive Fullscreen
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, view) }
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    
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
            val proxyUrl by settingsViewModel.proxyUrl.collectAsState()
            
            val finalUrl = remember(url, proxyUrl) {
                if (proxyUrl.isNotBlank()) {
                    // Prepend proxy if configured, handle trailing slash
                    val base = if (proxyUrl.endsWith("/")) proxyUrl else "$proxyUrl/"
                    "$base$url"
                } else {
                    url
                }
            }

            val saveHistory: (Long, Long) -> Unit = remember(finalUrl, title, groupName, posterUrl) {
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
                                settingsViewModel.removeWatchHistoryItem(finalUrl)
                            }
                        } else if (!isCompleted || !groupName.contains("Serie", ignoreCase = true)) {
                            val historyItem = com.livetvpremium.app.model.WatchHistoryItem(
                                title = title,
                                originalUrl = finalUrl,
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
            
            DisposableEffect(finalUrl) {
                val trackSelector = DefaultTrackSelector(context).apply {
                    setParameters(buildUponParameters().setPreferredAudioLanguage("it"))
                }
                
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setAllowCrossProtocolRedirects(true)
                    
                val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
                
                val extractorsFactory = DefaultExtractorsFactory()
                    .setConstantBitrateSeekingEnabled(true)
                    .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)
                    
                val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
                    .setDataSourceFactory(dataSourceFactory)
                
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        30_000, // Min buffer (increased from default 15s to 30s)
                        60_000, // Max buffer (60s)
                        5_000,  // Buffer for playback start (5s)
                        5_000   // Buffer for playback after re-buffer (5s)
                    )
                    .setBackBuffer(10_000, true) // Keep 10s of back buffer for seeking
                    .build()

                val player = ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setLoadControl(loadControl)
                    .build()
                    .apply {
                        val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(finalUrl))
                        
                        // Refined MIME type detection
                        val hasM3u8Ext = finalUrl.contains(".m3u8", ignoreCase = true)
                        val hasMp4Ext = finalUrl.contains(".mp4", ignoreCase = true)
                        val hasTsExt = finalUrl.contains(".ts", ignoreCase = true)
                        val hasMkvExt = finalUrl.contains(".mkv", ignoreCase = true)
                        
                        when {
                            hasM3u8Ext -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                            hasMp4Ext -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
                            hasTsExt -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
                            hasMkvExt -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MATROSKA)
                            groupName.contains("live", ignoreCase = true) -> {
                                // For Live channels without clear extension, assume HLS (most common)
                                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                            }
                            else -> {
                                // For VOD (Movies/Series) without clear extension, DO NOT force MimeType
                                // and let ExoPlayer attempt auto-detection by sniffing the content.
                                // This prevents "Input does not start with #EXTM3U" errors on progressive streams.
                            }
                        }
 
                        setMediaItem(mediaItemBuilder.build())
                        
                        if (isVod) {
                            val existingHistory = watchHistory.find { it.originalUrl == finalUrl }
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
                        object : PlayerView(ctx) {
                            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                                // BACK button handling: if controller is shown, hide it first.
                                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                                    if (isControllerFullyVisible) {
                                        hideController()
                                        return true
                                    }
                                    // If controller is hidden, let the default behavior (exit screen) handle it
                                }

                                // If the panel is NOT visible, show it on any D-PAD or ENTER press
                                if (!isControllerFullyVisible) {
                                    val actionCode = event.keyCode
                                    if (actionCode == KeyEvent.KEYCODE_DPAD_UP ||
                                        actionCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                                        actionCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                        actionCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                                        actionCode == KeyEvent.KEYCODE_ENTER ||
                                        actionCode == KeyEvent.KEYCODE_DPAD_CENTER
                                    ) {
                                        if (event.action == KeyEvent.ACTION_DOWN) {
                                            showController()
                                            // Request focus for the player view to ensure D-PAD navigation starts within the controller
                                            requestFocus()
                                        }
                                        return true
                                    }
                                }

                                // Explicit Media Keys handling (PLAY/PAUSE/TOGGLE)
                                if (event.action == KeyEvent.ACTION_DOWN) {
                                    when (event.keyCode) {
                                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 
                                        KeyEvent.KEYCODE_DPAD_CENTER, 
                                        KeyEvent.KEYCODE_ENTER -> {
                                            // If controller is visible, we let the focused button handle the click
                                            // But if we're just playing/pausing via center button on the video itself:
                                            if (isControllerFullyVisible) {
                                                return super.dispatchKeyEvent(event)
                                            } else {
                                                if (player.isPlaying) player.pause() else player.play()
                                                showController()
                                                return true
                                            }
                                        }
                                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                            if (!player.isPlaying) player.play()
                                            showController()
                                            return true
                                        }
                                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                            if (player.isPlaying) player.pause()
                                            showController()
                                            return true
                                        }
                                    }
                                }
                                
                                return super.dispatchKeyEvent(event)
                            }
                        }.apply {
                            this.player = player
                            useController = true
                            setShowSubtitleButton(true)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
