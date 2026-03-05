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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.livetvpremium.app.model.M3UItem
import com.livetvpremium.app.ui.components.GlassCard
import com.livetvpremium.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    categoryType: String,
    groupName: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String, String, String) -> Unit,
    onNavigateToEpisodeList: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val liveItems by viewModel.liveItems.collectAsState()
    val filmItems by viewModel.filmItems.collectAsState()
    val serieItems by viewModel.serieItems.collectAsState()
    
    val groupItems = remember(liveItems, filmItems, serieItems, groupName, categoryType) {
        val listToFilter = when (categoryType) {
            "live" -> liveItems
            "film" -> filmItems
            "serie" -> serieItems
            else -> liveItems + filmItems + serieItems
        }
        listToFilter.filter { it.groupTitle == groupName }
    }
    
    val displayItems = remember(groupItems, categoryType) {
        if (categoryType == "serie") {
            val regex = "^(.*?)(?:\\s+S\\d{1,2}\\s*E\\d{1,3})".toRegex(RegexOption.IGNORE_CASE)
            val seriesMap = mutableMapOf<String, M3UItem>()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(displayItems) { item ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clickable { 
                            if (categoryType == "serie") {
                                onNavigateToEpisodeList(item.name)
                            } else {
                                onNavigateToDetails(
                                    item.tvgId.ifEmpty { "no_id" }, 
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
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
