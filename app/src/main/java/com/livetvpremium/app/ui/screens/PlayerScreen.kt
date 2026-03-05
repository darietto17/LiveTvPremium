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
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.URLEncoder
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import okhttp3.JavaNetCookieJar
import okhttp3.Protocol
import java.nio.charset.StandardCharsets
import com.livetvpremium.app.ui.viewmodel.SettingsViewModel
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

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
    val isVod = groupName.contains("Film", ignoreCase = true) || 
                 groupName.contains("Serie", ignoreCase = true) ||
                 groupName.contains("Cinema", ignoreCase = true) ||
                 groupName.contains("Movies", ignoreCase = true) ||
                 groupName.contains("VOD", ignoreCase = true)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (useVlcPlayer) {
            val proxyUrl by settingsViewModel.proxyUrl.collectAsState()
            val finalUrl = remember(url, proxyUrl) {
                if (proxyUrl.isNotBlank()) {
                    val base = if (proxyUrl.endsWith("/")) proxyUrl else "$proxyUrl/"
                    try {
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        "${base}manifest.m3u8?url=$encodedUrl"
                    } catch (e: Exception) { url }
                } else { url }
            }

            val libVlc = remember { 
                val args = ArrayList<String>().apply {
                    add("--http-reconnect")
                    add("--network-caching=3000")
                    add("--rtsp-tcp")
                    add("--avcodec-hw=any")
                }
                LibVLC(context, args) 
            }
            val mediaPlayer = remember { MediaPlayer(libVlc) }
            
            DisposableEffect(finalUrl) {
                val media = Media(libVlc, Uri.parse(finalUrl))
                media.setHWDecoderEnabled(true, false)
                media.addOption(":network-caching=3000")
                media.addOption(":clock-jitter=0")
                media.addOption(":clock-synchro=0")
                
                mediaPlayer.media = media
                media.release()
                mediaPlayer.play()
                
                onDispose {
                    mediaPlayer.stop()
                    mediaPlayer.release()
                    libVlc.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    object : VLCVideoLayout(ctx) {
                        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                                (context as? Activity)?.onBackPressed()
                                return true
                            }
                            return super.dispatchKeyEvent(event)
                        }
                    }.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        mediaPlayer.attachViews(this, null, true, false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // ExoPlayer Implementation
            var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
            val watchHistory by settingsViewModel.watchHistory.collectAsState()
            val proxyUrl by settingsViewModel.proxyUrl.collectAsState()
            val dnsUrl by settingsViewModel.dnsUrl.collectAsState()
            
            val finalUrl = remember(url, proxyUrl) {
                if (proxyUrl.isNotBlank()) {
                    val base = if (proxyUrl.endsWith("/")) proxyUrl else "$proxyUrl/"
                    try {
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        "${base}manifest.m3u8?url=$encodedUrl"
                    } catch (e: Exception) {
                        url
                    }
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

                val okHttpClient = OkHttpClient.Builder().apply {
                    val customDns = if (dnsUrl.isNotBlank()) {
                        try {
                            val bootstrapClient = OkHttpClient.Builder().build()
                            when {
                                dnsUrl == "1.1.1.1" || dnsUrl == "1.0.0.1" -> {
                                    DnsOverHttps.Builder().client(bootstrapClient)
                                        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                                        .bootstrapDnsHosts(listOf(InetAddress.getByName("1.1.1.1")))
                                        .build()
                                }
                                dnsUrl == "8.8.8.8" || dnsUrl == "8.8.4.4" -> {
                                    DnsOverHttps.Builder().client(bootstrapClient)
                                        .url("https://dns.google/dns-query".toHttpUrl())
                                        .bootstrapDnsHosts(listOf(InetAddress.getByName("8.8.8.8")))
                                        .build()
                                }
                                dnsUrl.startsWith("http") -> {
                                    DnsOverHttps.Builder().client(bootstrapClient)
                                        .url(dnsUrl.toHttpUrl())
                                        .build()
                                }
                                else -> Dns.SYSTEM
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Dns.SYSTEM
                        }
                    } else {
                        Dns.SYSTEM
                    }
                    dns(customDns)
                }
                .protocols(listOf(Protocol.HTTP_1_1))
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectionPool(okhttp3.ConnectionPool(15, 5, TimeUnit.MINUTES))
                .cookieJar(JavaNetCookieJar(CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) }))
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
                
                val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    
                val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
                
                // Enhanced Extractors Factory for better IPTV compatibility
                val extractorsFactory = DefaultExtractorsFactory()
                    .setConstantBitrateSeekingEnabled(true)
                    .setTsExtractorFlags(
                        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                        DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                        DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM or
                        DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                        (1 shl 6) or // FLAG_IGNORE_PCR: Critical for IPTV to avoid BufferQueue/AudioTrack stalls
                        (1 shl 3)    // FLAG_ALLOW_NON_CONSECUTIVE_PIDS
                    )
                    .setAdtsExtractorFlags(androidx.media3.extractor.ts.AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
                    
                val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
                    .setDataSourceFactory(dataSourceFactory)
                
                val isLive = !isVod || 
                             groupName.contains("live", ignoreCase = true) || 
                             groupName.contains("diretta", ignoreCase = true) ||
                             groupName.contains("tv", ignoreCase = true) ||
                             groupName.contains("channels", ignoreCase = true)
                             
                val bufferForPlaybackMs = if (isLive) 2_000 else 2_500
                val bufferAfterRebufferMs = if (isLive) 4_000 else 5_000
                
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        if (isLive) 15_000 else 15_000, 
                        if (isLive) 60_000 else 50_000, 
                        bufferForPlaybackMs, 
                        bufferAfterRebufferMs
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .setBackBuffer(10_000, true)
                    .build()

                val renderersFactory = DefaultRenderersFactory(context)
                    .setEnableDecoderFallback(true) // Crucial: switch to sw decoder if MediaCodec/BufferQueue stalls

                val player = ExoPlayer.Builder(context, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setLoadControl(loadControl)
                    .build()
                    .apply {
                        addListener(object : androidx.media3.common.Player.Listener {
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                // Auto-retry on network or timeout errors
                                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                                ) {
                                    val currentPos = currentPosition
                                    prepare()
                                    seekTo(currentPos)
                                    play()
                                }
                            }
                        })
                        
                        val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(finalUrl))
                        
                        // Improved MIME type detection for IPTV streams
                        val lowerUrl = finalUrl.lowercase()
                        val isHls = lowerUrl.contains(".m3u8") || lowerUrl.contains("m3u8") || 
                                    lowerUrl.contains("type=m3u8") || lowerUrl.contains("output=m3u8") || 
                                    lowerUrl.contains("output=hls") || lowerUrl.contains("protocol=hls")
                        val isTs = lowerUrl.contains(".ts") || lowerUrl.contains("ts") || 
                                   lowerUrl.contains("type=ts") || lowerUrl.contains("output=ts") || 
                                   lowerUrl.contains("output=mpegts") || lowerUrl.contains("mpegts") ||
                                   (isLive && lowerUrl.contains("/ts"))
                        val isMp4 = lowerUrl.contains(".mp4") || lowerUrl.contains("type=mp4") || lowerUrl.contains("output=mp4")
                        val isMkv = lowerUrl.contains(".mkv") || lowerUrl.contains("type=mkv") || lowerUrl.contains("output=mkv")
                        
                        when {
                            isHls -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                            isTs -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
                            isMp4 -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
                            isMkv -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MATROSKA)
                            isLive -> {
                                // Default for Live channels if no clear format, prefer HLS
                                if (lowerUrl.contains("output=ts") || lowerUrl.contains("type=ts") || lowerUrl.endsWith(".ts")) {
                                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
                                } else {
                                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                                }
                            }
                            else -> {
                                // For VOD let it sniff
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
                                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                                    if (isControllerFullyVisible) {
                                        hideController()
                                        return true
                                    }
                                }

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
                                            requestFocus()
                                        }
                                        return true
                                    }
                                }

                                if (event.action == KeyEvent.ACTION_DOWN) {
                                    when (event.keyCode) {
                                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 
                                        KeyEvent.KEYCODE_DPAD_CENTER, 
                                        KeyEvent.KEYCODE_ENTER -> {
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
