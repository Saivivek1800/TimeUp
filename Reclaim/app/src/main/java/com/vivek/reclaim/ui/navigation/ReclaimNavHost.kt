package com.vivek.reclaim.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vivek.reclaim.ui.articles.ArticlesScreen
import com.vivek.reclaim.ui.home.HomeScreen
import com.vivek.reclaim.ui.onboarding.AppsScreen
import com.vivek.reclaim.ui.onboarding.InterestsScreen
import com.vivek.reclaim.ui.onboarding.LimitScreen
import com.vivek.reclaim.ui.onboarding.PermissionsScreen
import com.vivek.reclaim.ui.onboarding.WelcomeScreen
import com.vivek.reclaim.ui.settings.EditAppsScreen
import com.vivek.reclaim.ui.settings.EditInterestsScreen
import com.vivek.reclaim.ui.settings.SettingsScreen

private data class BottomItem(val route: Route, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Route.Home, "Home", Icons.Outlined.Home),
    BottomItem(Route.Articles, "Articles", Icons.Outlined.AutoStories),
    BottomItem(Route.Settings, "Settings", Icons.Outlined.Settings),
)

@Composable
fun ReclaimNavHost(startViewModel: StartViewModel = hiltViewModel()) {
    val startRoute by startViewModel.startRoute.collectAsStateWithLifecycle()
    val start = startRoute
    if (start == null) {
        // A moment while we read whether onboarding is already done.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = bottomItems.any { item -> destination?.hierarchy?.any { it.route == item.route.path } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == item.route.path } == true,
                            onClick = {
                                navController.navigate(item.route.path) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Welcome.path) {
                WelcomeScreen(onGetStarted = { navController.navigate(Route.Permissions.path) })
            }
            composable(Route.Permissions.path) {
                PermissionsScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate(Route.Interests.path) },
                )
            }
            composable(Route.Interests.path) {
                InterestsScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate(Route.Apps.path) },
                )
            }
            composable(Route.Apps.path) {
                AppsScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate(Route.Limit.path) },
                )
            }
            composable(Route.Limit.path) {
                LimitScreen(
                    onBack = { navController.popBackStack() },
                    onFinished = {
                        // Leave onboarding for good: Back from Home must not return to Welcome.
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Welcome.path) { inclusive = true }
                        }
                    },
                )
            }
            composable(Route.Home.path) { HomeScreen() }
            composable(Route.Articles.path) { ArticlesScreen() }
            composable(Route.Settings.path) {
                SettingsScreen(
                    onEditInterests = { navController.navigate(Route.EditInterests.path) },
                    onEditApps = { navController.navigate(Route.EditApps.path) },
                )
            }
            composable(Route.EditInterests.path) { EditInterestsScreen(onBack = { navController.popBackStack() }) }
            composable(Route.EditApps.path) { EditAppsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
