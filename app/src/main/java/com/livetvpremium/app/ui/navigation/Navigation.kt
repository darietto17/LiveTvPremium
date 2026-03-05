package com.livetvpremium.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.livetvpremium.app.ui.viewmodel.MainViewModel
import com.livetvpremium.app.ui.viewmodel.SettingsViewModel
import com.livetvpremium.app.ui.screens.StartupSyncScreen
import com.livetvpremium.app.ui.screens.HomeScreen
import com.livetvpremium.app.ui.screens.GroupListScreen
import com.livetvpremium.app.ui.screens.EpisodeListScreen
import com.livetvpremium.app.ui.screens.DetailsScreen
import com.livetvpremium.app.ui.screens.PlayerScreen
import com.livetvpremium.app.ui.screens.SettingsScreen
import com.livetvpremium.app.ui.screens.SearchScreen

sealed class Screen(val route: String) {
    object StartupSync : Screen("startup_sync")
    object Home : Screen("home")
    object GroupList : Screen("group_list/{categoryType}/{groupName}") {
        fun createRoute(categoryType: String, groupName: String) = "group_list/$categoryType/$groupName"
    }
    object EpisodeList : Screen("episode_list/{seriesTitle}/{groupName}") {
        fun createRoute(seriesTitle: String, groupName: String): String {
            val encodedTitle = java.net.URLEncoder.encode(seriesTitle, java.nio.charset.StandardCharsets.UTF_8.toString())
            val encodedGroup = java.net.URLEncoder.encode(groupName, java.nio.charset.StandardCharsets.UTF_8.toString())
            return "episode_list/$encodedTitle/$encodedGroup"
        }
    }
    object Details : Screen("details/{tvgId}/{groupName}/{title}/{url}") {
        fun createRoute(tvgId: String, groupName: String, title: String, url: String): String {
            val encodedUrl = java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8.toString())
            val encodedTitle = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8.toString())
            return "details/$tvgId/$groupName/$encodedTitle/$encodedUrl"
        }
    }
    object Player : Screen("player/{url}/{title}/{groupName}/{posterUrl}") {
        fun createRoute(url: String, title: String, groupName: String, posterUrl: String): String {
            val encodedUrl = java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8.toString())
            val encodedTitle = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8.toString())
            val encodedGroup = java.net.URLEncoder.encode(groupName, java.nio.charset.StandardCharsets.UTF_8.toString())
            val safePosterUrl = if (posterUrl.isEmpty()) "empty" else posterUrl
            val encodedPoster = java.net.URLEncoder.encode(safePosterUrl, java.nio.charset.StandardCharsets.UTF_8.toString())
            return "player/$encodedUrl/$encodedTitle/$encodedGroup/$encodedPoster"
        }
    }
    object Settings : Screen("settings")
    object Search : Screen("search")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.StartupSync.route,
        modifier = modifier
    ) {
        composable(Screen.StartupSync.route) {
            val githubToken by settingsViewModel.githubToken.collectAsState()
            val lastSyncTime by settingsViewModel.lastSyncTime.collectAsState()
            
            StartupSyncScreen(
                viewModel = mainViewModel,
                githubToken = githubToken,
                lastSyncTime = lastSyncTime,
                onSyncComplete = {
                    settingsViewModel.saveLastSyncTime(System.currentTimeMillis())
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.StartupSync.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = mainViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateToGroup = { categoryType, groupName ->
                    navController.navigate(Screen.GroupList.createRoute(categoryType, groupName))
                },
                onNavigateToPlayer = { url, title, groupName, posterUrl ->
                    navController.navigate(Screen.Player.createRoute(url, title, groupName, posterUrl ?: ""))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToDetails = { tvgId, groupName, title, url ->
                    navController.navigate(Screen.Details.createRoute(tvgId, groupName, title, url))
                },
                onNavigateToEpisodeList = { seriesTitle, groupName ->
                    navController.navigate(Screen.EpisodeList.createRoute(seriesTitle, groupName))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { tvgId, groupName, title, url ->
                    navController.navigate(Screen.Details.createRoute(tvgId, groupName, title, url))
                },
                onNavigateToPlayer = { url, title, selectedGroup, posterUrl ->
                    navController.navigate(Screen.Player.createRoute(url, title, selectedGroup, posterUrl ?: ""))
                },
                onNavigateToEpisodeList = { seriesTitle, groupName ->
                    navController.navigate(Screen.EpisodeList.createRoute(seriesTitle, groupName))
                }
            )
        }
        composable(Screen.GroupList.route) { backStackEntry ->
            val categoryType = backStackEntry.arguments?.getString("categoryType") ?: "live"
            val groupName = backStackEntry.arguments?.getString("groupName") ?: ""
            GroupListScreen(
                categoryType = categoryType,
                groupName = groupName,
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { tvgId, title, url ->
                    navController.navigate(Screen.Details.createRoute(tvgId, groupName, title, url))
                },
                onNavigateToPlayer = { url, title, selectedGroup, posterUrl ->
                    navController.navigate(Screen.Player.createRoute(url, title, selectedGroup, posterUrl ?: ""))
                },
                onNavigateToEpisodeList = { seriesTitle ->
                    navController.navigate(Screen.EpisodeList.createRoute(seriesTitle, groupName))
                }
            )
        }
        composable(Screen.EpisodeList.route) { backStackEntry ->
            val encodedTitle = backStackEntry.arguments?.getString("seriesTitle") ?: ""
            val encodedGroup = backStackEntry.arguments?.getString("groupName") ?: ""
            val seriesTitle = java.net.URLDecoder.decode(encodedTitle, java.nio.charset.StandardCharsets.UTF_8.toString())
            val groupName = java.net.URLDecoder.decode(encodedGroup, java.nio.charset.StandardCharsets.UTF_8.toString())
            val tmdbApiKey by settingsViewModel.tmdbApiKey.collectAsState(initial = "")
            
            EpisodeListScreen(
                seriesTitle = seriesTitle,
                groupName = groupName,
                tmdbApiKey = tmdbApiKey,
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { tvgId, title, url ->
                    navController.navigate(Screen.Details.createRoute(tvgId, groupName, title, url))
                },
                onNavigateToPlayer = { url, title, selectedGroup, posterUrl ->
                    navController.navigate(Screen.Player.createRoute(url, title, selectedGroup, posterUrl ?: ""))
                }
            )
        }
        composable(Screen.Details.route) { backStackEntry ->
            val tvgId = backStackEntry.arguments?.getString("tvgId") ?: ""
            val groupName = backStackEntry.arguments?.getString("groupName") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            
            val title = java.net.URLDecoder.decode(encodedTitle, java.nio.charset.StandardCharsets.UTF_8.toString())
            val url = java.net.URLDecoder.decode(encodedUrl, java.nio.charset.StandardCharsets.UTF_8.toString())
            val tmdbApiKey by settingsViewModel.tmdbApiKey.collectAsState(initial = "")
            
            DetailsScreen(
                title = title,
                originalUrl = url,
                groupName = groupName,
                tmdbApiKey = tmdbApiKey,
                onNavigateBack = { navController.popBackStack() },
                onPlayClicked = { url, playTitle, playGroup, playPoster ->
                    navController.navigate(Screen.Player.createRoute(url, playTitle, playGroup, playPoster ?: ""))
                }
            )
        }
        composable(Screen.Player.route) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
            val encodedGroup = backStackEntry.arguments?.getString("groupName") ?: ""
            val encodedPoster = backStackEntry.arguments?.getString("posterUrl") ?: ""
            
            val url = java.net.URLDecoder.decode(encodedUrl, java.nio.charset.StandardCharsets.UTF_8.toString())
            val title = java.net.URLDecoder.decode(encodedTitle, java.nio.charset.StandardCharsets.UTF_8.toString())
            val groupName = java.net.URLDecoder.decode(encodedGroup, java.nio.charset.StandardCharsets.UTF_8.toString())
            val posterUrlRaw = java.net.URLDecoder.decode(encodedPoster, java.nio.charset.StandardCharsets.UTF_8.toString())
            val posterUrl = if (posterUrlRaw == "empty") null else posterUrlRaw
            
            PlayerScreen(
                url = url,
                title = title,
                groupName = groupName,
                posterUrl = posterUrl,
                settingsViewModel = settingsViewModel,
                mainViewModel = mainViewModel
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
