package com.livetvpremium.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.livetvpremium.app.R
import com.livetvpremium.app.ui.viewmodel.MainViewModel
import com.livetvpremium.app.ui.viewmodel.SyncState

@Composable
fun StartupSyncScreen(
    viewModel: MainViewModel,
    playlistUrl: String,
    githubToken: String? = null,
    onSyncComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syncState by viewModel.syncState.collectAsState()

    LaunchedEffect(githubToken) {
        if (syncState is SyncState.Idle) {
             viewModel.syncPlaylist(playlistUrl, null, githubToken)
        }
    }

    LaunchedEffect(syncState) {
        if (syncState is SyncState.Success) {
            onSyncComplete()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "LiveTvPremium",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            when (val state = syncState) {
                is SyncState.Loading -> {
                    CircularProgressIndicator(
                        progress = { state.progress },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = state.message, color = MaterialTheme.colorScheme.onBackground)
                }
                is SyncState.Error -> {
                    Text(
                        text = "Error: ${state.exception.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
