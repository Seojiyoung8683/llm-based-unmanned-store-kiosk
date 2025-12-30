// app/src/main/java/com/kiosk/jarvis/navigation/AppNavigation.kt
package com.kiosk.jarvis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kiosk.jarvis.ui.screens.Store3DScreen
import com.kiosk.jarvis.ui.screens.admin.DashboardScreen
import com.kiosk.jarvis.ui.screens.admin.FleetOperationsScreen
import com.kiosk.jarvis.ui.screens.admin.ProductsScreen
import com.kiosk.jarvis.ui.screens.admin.InventoryScreen
import com.kiosk.jarvis.ui.screens.admin.PricingScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object FleetOps : Screen("fleet_ops")
    object Products : Screen("products")
    object Inventory : Screen("inventory")
    object Pricing : Screen("pricing")

    object Store3D : Screen("store3d")
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    onNavigateToKiosk: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                onNavigateToKiosk = onNavigateToKiosk
            )
        }
        composable(Screen.FleetOps.route) {
            FleetOperationsScreen(navController = navController)
        }
        composable(Screen.Products.route) {
            ProductsScreen(navController = navController)
        }
        composable(Screen.Inventory.route) {
            InventoryScreen(navController = navController)
        }
        composable(Screen.Pricing.route) {
            PricingScreen(navController = navController)
        }

        composable(
            route = "${Screen.Store3D.route}/{command}",
            arguments = listOf(
                navArgument("command") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val command = backStackEntry.arguments?.getString("command")
            Store3DScreen(
                initialCommand = command,
                onClose = {
                    navController.popBackStack()
                }
            )
        }
    }
}
