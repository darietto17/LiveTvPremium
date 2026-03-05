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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
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
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveGroups by viewModel.liveGroups.collectAsState()
    val filmGroups by viewModel.filmGroups.collectAsState()
    val serieGroups by viewModel.serieGroups.collectAsState()
    val filmItems by viewModel.filmItems.collectAsState()
    val serieItems by viewModel.serieItems.collectAsState()
    
    val recentSeries by viewModel.recentSeriesList.collectAsState()

    val recentFilms by viewModel.recentFilms.collectAsState()
    val tmdbApiKey by settingsViewModel.tmdbApiKey.collectAsState()

    // Trigger TMDB-based sorting once items and API key are available
    LaunchedEffect(filmItems, serieItems, tmdbApiKey) {
        if (tmdbApiKey.isNotEmpty()) {
            if (filmItems.isNotEmpty() && recentFilms.isEmpty()) {
                viewModel.fetchRecentFilms(tmdbApiKey)
            }
            if (serieItems.isNotEmpty() && recentSeries.isEmpty()) {
                viewModel.fetchRecentSeries(tmdbApiKey)
            }
        }
    }

    val watchHistory by settingsViewModel.watchHistory.collectAsState()
    
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(0) }
    var liveChannelToPlay by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.livetvpremium.app.model.M3UItem?>(null) }
    
    val bottomBarFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    
    val latestRelease by viewModel.latestRelease.collectAsState()
    
    // Initial focus request for TV and update check
    LaunchedEffect(Unit) {
        bottomBarFocusRequester.requestFocus()
        viewModel.checkForUpdates(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LiveTvPremium", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        modifier = Modifier.focusProperties { down = contentFocusRequester },
                        onClick = onNavigateToSearch
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(
                        modifier = Modifier.focusProperties { down = contentFocusRequester },
                        onClick = onNavigateToSettings
                    ) {
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
                    modifier = Modifier.focusRequester(bottomBarFocusRequester)
                        .focusProperties { up = contentFocusRequester },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    modifier = Modifier.focusProperties { up = contentFocusRequester },
                    icon = { Icon(Icons.Default.Tv, contentDescription = "Live TV") },
                    label = { Text("Live TV") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    modifier = Modifier.focusProperties { up = contentFocusRequester },
                    icon = { Icon(Icons.Default.Movie, contentDescription = "Film") },
                    label = { Text("Film") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    modifier = Modifier.focusProperties { up = contentFocusRequester },
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
                    .padding(horizontal = 16.dp)
                    .focusRequester(contentFocusRequester)
                    .focusProperties { down = bottomBarFocusRequester },
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
                                            if (history.posterUrl != null && history.posterUrl != "null" && history.posterUrl.isNotBlank()) {
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
                        items(recentFilms) { film ->
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
            val isLargeScreen = LocalConfiguration.current.screenWidthDp >= 800
            val categoryType = when (selectedTab) {
                1 -> "live"
                2 -> "film"
                else -> "serie"
            }
            
            if (isLargeScreen && currentGroups.isNotEmpty()) {
                var selectedGroup by androidx.compose.runtime.saveable.rememberSaveable(selectedTab, currentGroups) { 
                    androidx.compose.runtime.mutableStateOf(currentGroups.firstOrNull() ?: "")
                }
                
                Row(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Master List (Left Panel - 30%)
                    Column(modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .focusRequester(contentFocusRequester)
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
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentGroups.size) { index ->
                                val group = currentGroups[index]
                                val isSelected = group == selectedGroup
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedGroup = group }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                                else androidx.compose.ui.graphics.Color.Transparent
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = group,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Detail Grid (Right Panel - 70%)
                    Column(modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                    ) {
                        Text(
                            text = selectedGroup,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        val listToFilter = when (categoryType) {
                            "live" -> viewModel.liveItems.collectAsState().value
                            "film" -> viewModel.filmItems.collectAsState().value
                            else -> viewModel.serieItems.collectAsState().value
                        }
                        
                        val groupItems = remember(listToFilter, selectedGroup, categoryType) {
                            listToFilter.filter { it.groupTitle == selectedGroup }
                        }
                        
                        val displayItems = remember(groupItems, categoryType) {
                            if (categoryType == "serie") {
                                val regex = "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE)
                                val seriesMap = mutableMapOf<String, com.livetvpremium.app.model.M3UItem>()
                                for (item in groupItems) {
                                    val match = regex.find(item.name)
                                    val seriesName = match?.groupValues?.get(1)?.trim() ?: item.name
                                    if (!seriesMap.containsKey(seriesName)) {
                                        seriesMap[seriesName] = item.copy(name = seriesName)
                                    }
                                }
                                seriesMap.values.toList().sortedBy { it.name }
                            } else {
                                groupItems
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                                .focusProperties { down = bottomBarFocusRequester }
                        ) {
                            items(displayItems.size) { index ->
                                val item = displayItems[index]
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f)
                                        .clickable { 
                                            if (categoryType == "serie") {
                                                onNavigateToEpisodeList(item.name, item.groupTitle)
                                            } else if (categoryType == "live") {
                                                liveChannelToPlay = item
                                            } else {
                                                onNavigateToDetails(
                                                    item.tvgId.ifEmpty { "no_id" }, 
                                                    item.groupTitle,
                                                    item.name, 
                                                    item.url
                                                )
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (item.tvgLogo.isNotEmpty()) {
                                            AsyncImage(
                                                model = item.tvgLogo,
                                                contentDescription = item.name,
                                                contentScale = if (categoryType == "live") ContentScale.Fit else ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .padding(if (categoryType == "live") 8.dp else 0.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(item.name.take(2), style = MaterialTheme.typography.headlineLarge)
                                            }
                                        }
                                        
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                                                .fillMaxWidth()
                                        )
                                        if (categoryType == "live") {
                                            val currentProgram = viewModel.getCurrentProgram(item.tvgId)
                                            if (currentProgram != null) {
                                                Text(
                                                    text = "📺 ${currentProgram.title}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                                                        .fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Mobile layout (or fallback if no groups)
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
                            .focusProperties { down = bottomBarFocusRequester }
                    ) {
                        items(currentGroups.size) { index ->
                            val group = currentGroups[index]
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clickable {
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

    liveChannelToPlay?.let { channel ->
        AlertDialog(
            onDismissRequest = { liveChannelToPlay = null },
            title = { Text("Seleziona Provider", fontWeight = FontWeight.Bold) },
            text = { Text("Come vuoi riprodurre questo canale live?") },
            confirmButton = {
                TextButton(onClick = {
                    val encodedUrl = java.net.URLEncoder.encode(channel.url, "UTF-8")
                    onNavigateToPlayer("https://eproxy.rrinformatica.cloud/proxy/manifest.m3u8?url=$encodedUrl", channel.name, channel.groupTitle, channel.tvgLogo)
                    liveChannelToPlay = null
                }) {
                    Text("Tramite Proxy (eProxy)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onNavigateToPlayer(channel.url, channel.name, channel.groupTitle, channel.tvgLogo)
                    liveChannelToPlay = null
                }) {
                    Text("Diretto (ISP)")
                }
            }
        )
    }

    latestRelease?.let { release ->
        AlertDialog(
            onDismissRequest = { /* Don't dismiss update on click outside */ },
            title = { Text("Aggiornamento Disponibile! 🚀", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("È disponibile una nuova versione: ${release.tagName}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(release.body.take(200) + if (release.body.length > 200) "..." else "", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: release.assets.firstOrNull()
                    apkAsset?.let { viewModel.downloadAndInstallUpdate(context, it.downloadUrl) }
                }) {
                    Text("Scarica e Installa")
                }
            },
            dismissButton = {
                TextButton(onClick = { /* Could add a ignore flag later */ }) {
                    Text("Dopo")
                }
            }
        )
    }
}
