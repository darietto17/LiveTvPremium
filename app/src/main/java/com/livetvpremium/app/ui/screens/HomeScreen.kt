package com.livetvpremium.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetvpremium.app.ui.components.GlassCard
import com.livetvpremium.app.ui.viewmodel.MainViewModel

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Subscriptions

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.livetvpremium.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToGroup: (String, String) -> Unit,
    onNavigateToPlayer: (String, String, String, String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetails: (String, String, String, String) -> Unit,
    onNavigateToEpisodeList: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveGroups by viewModel.liveGroups.collectAsState()
    val filmGroups by viewModel.filmGroups.collectAsState()
    val serieGroups by viewModel.serieGroups.collectAsState()
    val filmItems by viewModel.filmItems.collectAsState()
    val serieItems by viewModel.serieItems.collectAsState()
    
    val recentSeries = remember(serieItems) {
        val regex = "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE)
        val uniqueSeries = mutableMapOf<String, com.livetvpremium.app.model.M3UItem>()
        for (item in serieItems) {
            val match = regex.find(item.name)
            val title = match?.groupValues?.get(1)?.trim() ?: item.name
            if (!uniqueSeries.containsKey(title)) {
                uniqueSeries[title] = item
                if (uniqueSeries.size >= 50) break
            }
        }
        uniqueSeries.values.toList()
    }

    val watchHistory by settingsViewModel.watchHistory.collectAsState()
    
    var selectedTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LiveTvPremium", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* Expand Search */ }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Tv, contentDescription = "Live TV") },
                    label = { Text("Live TV") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Movie, contentDescription = "Film") },
                    label = { Text("Film") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Subscriptions, contentDescription = "Serie TV") },
                    label = { Text("Serie") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
    
        val currentGroups = when (selectedTab) {
            1 -> liveGroups
            2 -> filmGroups
            3 -> serieGroups
            else -> emptyList()
        }
        
        if (selectedTab == 0) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (watchHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Continua a guardare",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(watchHistory) { history ->
                                GlassCard(
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(112.dp)
                                        .clickable {
                                            onNavigateToPlayer(
                                                history.originalUrl,
                                                history.title,
                                                history.groupName,
                                                history.posterUrl
                                            )
                                        }
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            if (history.posterUrl != null) {
                                                val imageUrl = if (history.posterUrl.startsWith("http")) {
                                                    history.posterUrl
                                                } else {
                                                    "https://image.tmdb.org/t/p/w500${history.posterUrl}"
                                                }
                                                AsyncImage(
                                                    model = imageUrl,
                                                    contentDescription = history.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                        val progressPercent = if (history.durationMs > 0) {
                                            (history.progressMs.toFloat() / history.durationMs.toFloat()).coerceIn(0f, 1f)
                                        } else {
                                            0f
                                        }
                                        LinearProgressIndicator(
                                            progress = { progressPercent },
                                            modifier = Modifier.fillMaxWidth().height(4.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            text = history.title,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Text(
                        text = "Nuovi Film Aggiunti",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(filmItems.take(50)) { film ->
                           GlassCard(
                               modifier = Modifier.width(150.dp).aspectRatio(2f/3f).clickable {
                                   onNavigateToDetails(
                                       film.tvgId.ifEmpty { "no_id" },
                                       film.groupTitle,
                                       film.name,
                                       film.url
                                   )
                               }
                           ) {
                               Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                   if (film.tvgLogo.isNotEmpty()) {
                                       AsyncImage(
                                           model = film.tvgLogo,
                                           contentDescription = film.name,
                                           contentScale = ContentScale.Crop,
                                           modifier = Modifier.fillMaxSize()
                                       )
                                   } else {
                                       Text(film.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
                                   }
                               }
                           }
                        }
                    }
                }
                
                item {
                    Text(
                        text = "Ultime Serie TV",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(recentSeries) { series ->
                           val regex = "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE)
                           val match = regex.find(series.name)
                           val cleanTitle = match?.groupValues?.get(1)?.trim() ?: series.name
                           
                           GlassCard(
                               modifier = Modifier.width(150.dp).aspectRatio(2f/3f).clickable {
                                   onNavigateToEpisodeList(cleanTitle, series.groupTitle)
                               }
                           ) {
                               Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                   if (series.tvgLogo.isNotEmpty()) {
                                       AsyncImage(
                                           model = series.tvgLogo,
                                           contentDescription = cleanTitle,
                                           contentScale = ContentScale.Crop,
                                           modifier = Modifier.fillMaxSize()
                                       )
                                   } else {
                                       Text(cleanTitle, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
                                   }
                               }
                           }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                val title = when (selectedTab) {
                    1 -> "Canali TV"
                    2 -> "Film On Demand"
                    else -> "Serie TV"
                }
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentGroups) { group ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clickable {
                                    val categoryType = when (selectedTab) {
                                        1 -> "live"
                                        2 -> "film"
                                        else -> "serie"
                                    }
                                    onNavigateToGroup(categoryType, group)
                                }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = group,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
