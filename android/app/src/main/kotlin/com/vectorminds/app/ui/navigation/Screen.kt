package com.vectorminds.app.ui.navigation

/**
 * Screen definitions for VectorMinds navigation.
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Trends : Screen("trends")
    data object TrendDetail : Screen("trends/{trendId}") {
        fun createRoute(trendId: String) = "trends/$trendId"
    }
    data object Blueprints : Screen("blueprints")
    data object Pipelines : Screen("pipelines")
    data object Settings : Screen("settings")
}
