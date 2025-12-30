package com.kiosk.jarvis.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun AppNavigationDrawer(
    navController: NavController,
    currentRoute: String?,
    onItemClick: () -> Unit
) {
    val items = listOf(
        NavigationItem("대시보드", Icons.Default.Dashboard, Screen.Dashboard.route),

        NavigationItem("매장 운영", Icons.Default.Router, Screen.FleetOps.route),
        NavigationItem("상품 관리", Icons.Default.ShoppingCart, Screen.Products.route),
        NavigationItem("재고 관리", Icons.Default.Inventory, Screen.Inventory.route),
        NavigationItem("가격/프로모션", Icons.Default.MonetizationOn, Screen.Pricing.route)
    )

    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "무인매장 관리",
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        items.forEach { item ->
            NavigationDrawerItem(
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    safeNavigate(navController, item.route, fallback = Screen.Dashboard.route)
                    onItemClick()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

private fun safeNavigate(
    navController: NavController,
    route: String,
    fallback: String
) {
    val navigateBlock: (String) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    runCatching { navigateBlock(route) }
        .onFailure {
            navigateBlock(fallback)
        }
}
