package com.panda_erkan.zvtclientdemo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.panda_erkan.zvtclientdemo.R

data class BottomNavItem(
    val route: String,
    val labelResId: Int,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(ZvtRoute.Payment.route, R.string.nav_payment, Icons.Default.Payment),
    BottomNavItem(ZvtRoute.Terminal.route, R.string.nav_terminal, Icons.Default.PointOfSale),
    BottomNavItem(ZvtRoute.Journals.route, R.string.nav_journals, Icons.Default.Receipt),
    BottomNavItem(ZvtRoute.Log.route, R.string.nav_log, Icons.Default.Description)
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
                label = { Text(stringResource(item.labelResId)) }
            )
        }
    }
}
