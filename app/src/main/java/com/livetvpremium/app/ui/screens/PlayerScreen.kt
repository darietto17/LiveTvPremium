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
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.livetvpremium.app.ui.viewmodel.SettingsViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    url: String,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val useVlcPlayer by settingsViewModel.useVlcPlayer.collectAsState()

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
            
            DisposableEffect(url) {
                val player = ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(url))
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
                exoPlayer = player
                
                onDispose {
                    player.release()
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
                            // D-pad support requires deeper customization of controller layout
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
