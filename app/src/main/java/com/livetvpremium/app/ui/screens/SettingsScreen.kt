package com.livetvpremium.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livetvpremium.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val githubToken by viewModel.githubToken.collectAsState()
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsState()
    val useVlcPlayer by viewModel.useVlcPlayer.collectAsState()
    val proxyUrl by viewModel.proxyUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = githubToken,
                onValueChange = { viewModel.saveGithubToken(it) },
                label = { Text("GitHub Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = tmdbApiKey,
                onValueChange = { viewModel.saveTmdbApiKey(it) },
                label = { Text("TMDB API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = proxyUrl,
                onValueChange = { viewModel.saveProxyUrl(it) },
                label = { Text("Proxy URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Usa VLC Player come predefinito", color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = useVlcPlayer,
                    onCheckedChange = { viewModel.saveUseVlcPlayer(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    viewModel.saveLastSyncTime(0L) // Force expiration
                    onNavigateBack() // Go back to Home
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Forza Sincronizzazione Ora")
            }
        }
    }
}
