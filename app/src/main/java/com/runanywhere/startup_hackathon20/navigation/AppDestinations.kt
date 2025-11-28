package com.runanywhere.startup_hackathon20.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestinations(val route: String, val title: String, val icon: ImageVector) {
    object Home : AppDestinations("home", "Home", Icons.Default.Home)
    object Finance : AppDestinations("finance", "Finance", Icons.Default.Star)
    object Health : AppDestinations("health", "Health", Icons.Default.Favorite)
    object Chat : AppDestinations("chat", "Chat", Icons.Default.Email)
}
