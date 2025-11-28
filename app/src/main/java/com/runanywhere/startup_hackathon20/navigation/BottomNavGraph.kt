package com.runanywhere.startup_hackathon20.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.runanywhere.startup_hackathon20.ui.screens.ChatScreen
import com.runanywhere.startup_hackathon20.ui.screens.FinanceScreen
import com.runanywhere.startup_hackathon20.ui.screens.HealthScreen
import com.runanywhere.startup_hackathon20.ui.screens.HomeScreen

@Composable
fun BottomNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.Home.route
    ) {
        composable(route = AppDestinations.Home.route) {
            HomeScreen()
        }
        composable(route = AppDestinations.Finance.route) {
            FinanceScreen()
        }
        composable(route = AppDestinations.Health.route) {
            HealthScreen()
        }
        composable(route = AppDestinations.Chat.route) {
            ChatScreen()
        }
    }
}
