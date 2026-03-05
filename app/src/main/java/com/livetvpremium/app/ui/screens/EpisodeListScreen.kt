package com.livetvpremium.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.livetvpremium.app.ui.components.GlassCard
import com.livetvpremium.app.ui.viewmodel.MainViewModel

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    seriesTitle: String,
    groupName: String,
    tmdbApiKey: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String, String, String) -> Unit,
    onNavigateToPlayer: (String, String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val serieItems by viewModel.serieItems.collectAsState()
    
    val episodeItems = remember(serieItems, seriesTitle, groupName) {
        val regex = "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE)
        serieItems.filter { it.groupTitle == groupName }.filter { item ->
            val match = regex.find(item.name)
            val extractedName = match?.groupValues?.get(1)?.trim() ?: item.name
            extractedName == seriesTitle || item.name.startsWith(seriesTitle)
        }
    }

    val episodesBySeason = remember(episodeItems) {
        val seasonRegex = "S(\\d{1,2})\\s*E\\d{1,3}".toRegex(RegexOption.IGNORE_CASE)
        val grouped = mutableMapOf<String, MutableList<com.livetvpremium.app.model.M3UItem>>()
        
        for (item in episodeItems) {
            val match = seasonRegex.find(item.name)
            val seasonLabel = if (match != null) {
                val num = match.groupValues[1].toIntOrNull() ?: 1
                "Stagione $num"
            } else {
                "Extra / Altro"
            }
            if (!grouped.containsKey(seasonLabel)) {
                grouped[seasonLabel] = mutableListOf()
            }
            grouped[seasonLabel]!!.add(item)
        }
        grouped
    }

    val seasons = episodesBySeason.keys.toList().sorted()
    var selectedSeason by remember(seasons) { mutableStateOf(seasons.firstOrNull() ?: "") }
    var expanded by remember { mutableStateOf(false) }

    var tmdbDetails by remember { mutableStateOf<com.livetvpremium.app.model.TmdbDetails?>(null) }
    var isLoadingTmdb by remember { mutableStateOf(true) }

    LaunchedEffect(seriesTitle, tmdbApiKey) {
        if (tmdbApiKey.isNotEmpty()) {
            val repo = com.livetvpremium.app.repository.TmdbRepository()
            tmdbDetails = repo.fetchDetails(seriesTitle, tmdbApiKey)
        }
        isLoadingTmdb = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(seriesTitle) },
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
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
                        text = tmdbDetails?.title ?: seriesTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoadingTmdb) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Text(
                            text = tmdbDetails?.overview ?: "Trama non disponibile.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (tmdbDetails?.trailerUrl != null) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(tmdbDetails!!.trailerUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guarda Trailer Ufficiale")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    Text(
                        text = "Episodi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (seasons.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedSeason,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                seasons.forEach { season ->
                                    DropdownMenuItem(
                                        text = { Text(season) },
                                        onClick = {
                                            selectedSeason = season
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            val displayedEpisodes = episodesBySeason[selectedSeason] ?: emptyList()
            
            items(displayedEpisodes.size) { index ->
                val item = displayedEpisodes[index]
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            val posterPath = tmdbDetails?.backdropPath ?: tmdbDetails?.posterPath
                            val posterUrl = if (posterPath != null) "https://image.tmdb.org/t/p/w780$posterPath" else null
                            onNavigateToPlayer(item.url, item.name, item.groupTitle, posterUrl)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "Play",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
