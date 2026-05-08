package com.vectorminds.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.vectorminds.app.ui.blueprints.BlueprintScreen
import com.vectorminds.app.ui.components.VmDockItem
import com.vectorminds.app.ui.components.VmNavDock
import com.vectorminds.app.ui.dashboard.DashboardScreen
import com.vectorminds.app.ui.pipelines.PipelineScreen
import com.vectorminds.app.ui.settings.SettingsScreen
import com.vectorminds.app.ui.theme.Vm
import com.vectorminds.app.ui.trends.TrendDetailScreen
import com.vectorminds.app.ui.trends.TrendListScreen

private val DockItems = listOf(
    VmDockItem(Screen.Dashboard.route,  "Command",   Icons.Filled.Hub),
    VmDockItem(Screen.Trends.route,     "Trends",    Icons.Filled.Insights),
    VmDockItem(Screen.Blueprints.route, "Blueprints",Icons.Filled.AutoGraph),
    VmDockItem(Screen.Pipelines.route,  "Pipelines", Icons.Filled.Terminal),
    VmDockItem(Screen.Settings.route,   "Settings",  Icons.Filled.Settings),
)

@Composable
fun VectorMindsNavGraph(
    navController: NavHostController,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val dockSelected = when {
        currentRoute == null -> Screen.Dashboard.route
        currentRoute.startsWith("trends") -> Screen.Trends.route
        else -> currentRoute
    }
    val showDock = currentRoute != Screen.TrendDetail.route

    Box(
        Modifier
            .fillMaxSize()
            .background(Vm.colors.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.fillMaxSize(),
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
            composable(Screen.Blueprints.route) { BlueprintScreen() }
            composable(Screen.Pipelines.route)  { PipelineScreen() }
            composable(Screen.Settings.route)   { SettingsScreen() }
        }

        if (showDock) {
            VmNavDock(
                items = DockItems,
                selectedKey = dockSelected,
                onSelect = { key ->
                    if (currentRoute != key) {
                        navController.navigate(key) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
