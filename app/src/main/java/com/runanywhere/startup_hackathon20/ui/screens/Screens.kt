package com.runanywhere.startup_hackathon20.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Home Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun FinanceScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Finance Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun HealthScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Health Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun ChatScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Chat Screen", style = MaterialTheme.typography.headlineMedium)
    }
}
