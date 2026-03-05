package com.livetvpremium.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livetvpremium.app.model.M3UItem
import com.livetvpremium.app.ui.components.GlassCard
import com.livetvpremium.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String, String, String, String) -> Unit,
    onNavigateToPlayer: (String, String, String, String?) -> Unit,
    onNavigateToEpisodeList: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val liveItems by viewModel.liveItems.collectAsState()
    val serieItems by viewModel.serieItems.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus the text field when the screen opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Debounce: wait 300ms after last keystroke before searching
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            debouncedQuery = ""
            return@LaunchedEffect
        }
        delay(300L)
        debouncedQuery = searchQuery
    }
    LaunchedEffect(debouncedQuery) {
        viewModel.search(debouncedQuery)
    }

    // Detect item type for navigation
    val liveSet = remember(liveItems) { liveItems.map { it.url }.toHashSet() }
    val serieRegex = remember { "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE) }

    var liveChannelToPlay by remember { mutableStateOf<M3UItem?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearSearch() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.search(it) },
                        placeholder = {
                            Text(
                                "Cerca canali, film, serie...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Empty State
            AnimatedVisibility(
                visible = searchQuery.isBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cerca canali, film e serie...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // No results
            AnimatedVisibility(
                visible = searchQuery.isNotBlank() && searchResults.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nessun risultato per",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "\"$searchQuery\"",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Results Grid
            AnimatedVisibility(
                visible = searchResults.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Text(
                        text = "${searchResults.size} risultati",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { item ->
                            val isSerie = serieRegex.containsMatchIn(item.name)

                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f)
                                    .clickable {
                                        if (isSerie) {
                                            val seriesTitle = serieRegex.find(item.name)
                                                ?.groupValues?.get(1)?.trim() ?: item.name
                                            onNavigateToEpisodeList(seriesTitle, item.groupTitle)
                                        } else if (liveSet.contains(item.url)) {
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
                                Column(modifier = Modifier.fillMaxSize()) {
                                        val isLive = liveSet.contains(item.url)
                                        if (item.tvgLogo.isNotEmpty()) {
                                            AsyncImage(
                                                model = item.tvgLogo,
                                                contentDescription = item.name,
                                                contentScale = if (isLive) ContentScale.Fit else ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .padding(if (isLive) 8.dp else 0.dp)
                                            )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                item.name.take(2),
                                                style = MaterialTheme.typography.headlineLarge
                                            )
                                        }
                                    }

                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .fillMaxWidth()
                                    )
                                    Text(
                                        text = item.groupTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
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
}
