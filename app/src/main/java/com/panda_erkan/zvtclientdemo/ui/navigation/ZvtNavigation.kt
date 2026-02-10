package com.panda_erkan.zvtclientdemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.panda_erkan.zvtclientdemo.ui.main.MainViewModel
import com.panda_erkan.zvtclientdemo.ui.screen.JournalsScreen
import com.panda_erkan.zvtclientdemo.ui.screen.LogScreen
import com.panda_erkan.zvtclientdemo.ui.screen.PaymentScreen
import com.panda_erkan.zvtclientdemo.ui.screen.TerminalScreen

sealed class ZvtRoute(val route: String) {
    data object Payment : ZvtRoute("payment")
    data object Terminal : ZvtRoute("terminal")
    data object Journals : ZvtRoute("journals")
    data object Log : ZvtRoute("log")
}

@Composable
fun ZvtNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ZvtRoute.Payment.route,
        modifier = modifier
    ) {
        composable(ZvtRoute.Payment.route) {
            PaymentScreen()
        }
        composable(ZvtRoute.Terminal.route) {
            TerminalScreen()
        }
        composable(ZvtRoute.Journals.route) {
            JournalsScreen()
        }
        composable(ZvtRoute.Log.route) {
            LogScreen(mainViewModel = mainViewModel)
        }
    }
}
