package com.livetvpremium.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.livetvpremium.app.ui.viewmodel.MainViewModel
import com.livetvpremium.app.ui.viewmodel.SettingsViewModel
import com.livetvpremium.app.ui.screens.StartupSyncScreen
import com.livetvpremium.app.ui.screens.HomeScreen
import com.livetvpremium.app.ui.screens.GroupListScreen
import com.livetvpremium.app.ui.screens.DetailsScreen
import com.livetvpremium.app.ui.screens.PlayerScreen
import com.livetvpremium.app.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    object StartupSync : Screen("startup_sync")
    object Home : Screen("home")
    object GroupList : Screen("group_list/{groupName}") {
        fun createRoute(groupName: String) = "group_list/$groupName"
    }
    object Details : Screen("details/{tvgId}") {
        fun createRoute(tvgId: String) = "details/$tvgId"
    }
    object Player : Screen("player/{url}") {
        fun createRoute(url: String) = "player/$url"
    }
    object Settings : Screen("settings")
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
            StartupSyncScreen(
                viewModel = mainViewModel,
                playlistUrl = "https://raw.githubusercontent.com/darietto17/LiveTvPremium/refs/heads/master/scripts/film.m3u", // Usa la m3u di default o prendila dalle settings
                onSyncComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.StartupSync.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = mainViewModel,
                onNavigateToGroup = { groupName ->
                    navController.navigate(Screen.GroupList.createRoute(groupName))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.GroupList.route) { backStackEntry ->
            val groupName = backStackEntry.arguments?.getString("groupName") ?: ""
            GroupListScreen(
                groupName = groupName,
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { tvgId ->
                    navController.navigate(Screen.Details.createRoute(tvgId))
                }
            )
        }
        composable(Screen.Details.route) { backStackEntry ->
            val tvgId = backStackEntry.arguments?.getString("tvgId") ?: ""
            DetailsScreen(
                tvgId = tvgId,
                onNavigateBack = { navController.popBackStack() },
                onPlayClicked = { url ->
                    navController.navigate(Screen.Player.createRoute(url))
                }
            )
        }
        composable(Screen.Player.route) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            PlayerScreen(
                url = url,
                settingsViewModel = settingsViewModel
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
