package com.livetvpremium.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.livetvpremium.app.ui.viewmodel.SettingsViewModel
import com.livetvpremium.app.ui.viewmodel.MainViewModel
import com.livetvpremium.app.ui.viewmodel.SyncState
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.os.Build
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val githubToken by viewModel.githubToken.collectAsState()
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsState()
    val useVlcPlayer by viewModel.useVlcPlayer.collectAsState()
    val proxyUrl by viewModel.proxyUrl.collectAsState()
    
    val syncState by mainViewModel.syncState.collectAsState()
    val actionState by mainViewModel.actionState.collectAsState()
    val context = LocalContext.current

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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("API e Configurazioni", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                
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
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Gestione Dati Locali", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                
                Button(
                    onClick = {
                        mainViewModel.syncAll(githubToken, 0L, forceSync = true, context = context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Forza Ricaricamento Liste ORA")
                }
                
                when (val state = syncState) {
                    is SyncState.Loading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(state.message, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    is SyncState.Success -> {
                        Text("✅ Sincronizzazione locale completata!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    is SyncState.Error -> {
                        Text("❌ Errore durante il sync: ${state.exception.message}", color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }
                
                if (githubToken.isNotBlank()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Aggiornamenti App", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    
                    val currentVersion = remember { 
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName ?: "0.0.0"
                            } else {
                                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
                            }
                        } catch (e: Exception) { "0.0.0" }
                    }
                    val latestRelease by mainViewModel.latestRelease.collectAsState()
                    val updateLoading by mainViewModel.updateLoading.collectAsState()

                    Text("Versione attuale: v$currentVersion", fontSize = 14.sp)
                    
                    Button(
                        onClick = { mainViewModel.checkForUpdates(context, showFeedback = true) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !updateLoading
                    ) {
                        if (updateLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Verifica Aggiornamenti")
                        }
                    }

                    latestRelease?.let { release ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Nuova versione disponibile: ${release.tagName}", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(release.body, fontSize = 12.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { 
                                        val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: release.assets.firstOrNull()
                                        apkAsset?.let { mainViewModel.downloadAndInstallUpdate(context, it.downloadUrl) }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Scarica e Installa ${release.tagName}")
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Scraper (Richiede GitHub Token)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Attenzione: usa questi comandi solo se vi sono stati cambiamenti evidenti ai cataloghi originari. Generare nuove liste richiede diversi minuti al server.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { mainViewModel.triggerGithubAction(githubToken, "main.yml") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Text("Genera Canali Live")
                        }
                        Button(
                            onClick = { mainViewModel.triggerGithubAction(githubToken, "fs.yml") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Text("Genera Film & Serie")
                        }
                    }
                    
                    if (actionState.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(actionState, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                IconButton(onClick = { mainViewModel.clearActionState() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
    }
}
