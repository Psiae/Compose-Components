package dev.flammky.compose_components.presentation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.flammky.compose_components.presentation.MainActivity
import dev.flammky.compose_components.presentation.reordering.Ordering

@Composable
fun MainActivity.RootNavigation() {
    val hostController = rememberNavController()
    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = hostController,
        startDestination = "home"
    ) {
        composable("home") {
            Home(hostController = hostController)
        }
        composable("ordering") {
            Ordering()
        }
    }
}