package com.vectorminds.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.vectorminds.app.ui.dashboard.DashboardScreen
import com.vectorminds.app.ui.trends.TrendListScreen
import com.vectorminds.app.ui.trends.TrendDetailScreen
import com.vectorminds.app.ui.blueprints.BlueprintScreen
import com.vectorminds.app.ui.pipelines.PipelineScreen
import com.vectorminds.app.ui.settings.SettingsScreen

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Default.Dashboard),
    BottomNavItem(Screen.Trends, "Trends", Icons.Default.TrendingUp),
    BottomNavItem(Screen.Blueprints, "Blueprints", Icons.Default.Description),
    BottomNavItem(Screen.Pipelines, "Pipelines", Icons.Default.Terminal),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun VectorMindsNavGraph(
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            if (currentRoute != item.screen.route) {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToTrends = { navController.navigate(Screen.Trends.route) }
                )
            }
            composable(Screen.Trends.route) {
                TrendListScreen(
                    onTrendClick = { trendId ->
                        navController.navigate(Screen.TrendDetail.createRoute(trendId))
                    }
                )
            }
            composable(
                route = Screen.TrendDetail.route,
                arguments = listOf(navArgument("trendId") { type = NavType.StringType })
            ) { backStackEntry ->
                val trendId = backStackEntry.arguments?.getString("trendId") ?: ""
                TrendDetailScreen(
                    trendId = trendId,
                    onBack = { navController.popBackStack() },
                    onShowBlueprints = {
                        navController.navigate(Screen.Blueprints.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onShowPipelines = {
                        navController.navigate(Screen.Pipelines.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Screen.Blueprints.route) {
                BlueprintScreen()
            }
            composable(Screen.Pipelines.route) {
                PipelineScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
