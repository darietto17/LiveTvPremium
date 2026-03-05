package com.livetvpremium.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.livetvpremium.app.model.TmdbDetails
import com.livetvpremium.app.repository.TmdbRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    title: String,
    originalUrl: String,
    groupName: String,
    tmdbApiKey: String,
    onNavigateBack: () -> Unit,
    onPlayClicked: (String, String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProxyDialog by remember { mutableStateOf(false) }
    
    // Auto-Proxy rule: if it's Film or Serie, force proxy.
    val isVod = groupName.contains("Film", ignoreCase = true) || groupName.contains("Serie", ignoreCase = true)

    var tmdbDetails by remember { mutableStateOf<TmdbDetails?>(null) }
    var isLoadingTmdb by remember { mutableStateOf(isVod) }
    
    LaunchedEffect(title, tmdbApiKey) {
        if (isVod && tmdbApiKey.isNotEmpty()) {
            val repo = TmdbRepository()
            tmdbDetails = repo.fetchDetails(title, tmdbApiKey)
        }
        isLoadingTmdb = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettagli") },
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
        ) {
            tmdbDetails?.let { details ->
                val imagePath = details.backdropPath ?: details.posterPath
                if (imagePath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w780$imagePath",
                        contentDescription = details.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = tmdbDetails?.title ?: title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoadingTmdb) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (isVod) {
                    Text(
                        text = tmdbDetails?.overview ?: "Trama non disponibile.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "Canale Live TV",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val finalTitle = tmdbDetails?.title ?: title
                        val finalPoster = tmdbDetails?.backdropPath ?: tmdbDetails?.posterPath
                        
                        if (isVod) {
                            val encodedUrl = java.net.URLEncoder.encode(originalUrl, "UTF-8")
                            onPlayClicked("https://eproxy.rrinformatica.cloud/proxy/manifest.m3u8?url=$encodedUrl", finalTitle, groupName, finalPoster)
                        } else {
                            showProxyDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Riproduci")
                }
                
                if (tmdbDetails?.trailerUrl != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(tmdbDetails!!.trailerUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Trailer")
                    }
                }
            }
        }
    }

    if (showProxyDialog) {
        AlertDialog(
            onDismissRequest = { showProxyDialog = false },
            title = { Text("Utilizzo Proxy") },
            text = { Text("Vuoi utilizzare il proxy eproxy.rrinformatica.cloud per riprodurre questo canale Live? Può aiutare se il canale si blocca.") },
            confirmButton = {
                TextButton(onClick = {
                    showProxyDialog = false
                    val encodedUrl = java.net.URLEncoder.encode(originalUrl, "UTF-8")
                    val finalTitle = tmdbDetails?.title ?: title
                    val finalPoster = tmdbDetails?.backdropPath ?: tmdbDetails?.posterPath
                    onPlayClicked("https://eproxy.rrinformatica.cloud/proxy/manifest.m3u8?url=$encodedUrl", finalTitle, groupName, finalPoster)
                }) {
                    Text("Sì, usa Proxy")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProxyDialog = false
                    val finalTitle = tmdbDetails?.title ?: title
                    val finalPoster = tmdbDetails?.backdropPath ?: tmdbDetails?.posterPath
                    onPlayClicked(originalUrl, finalTitle, groupName, finalPoster) // Play directly
                }) {
                    Text("No, riproduzione diretta")
                }
            }
        )
    }
}
