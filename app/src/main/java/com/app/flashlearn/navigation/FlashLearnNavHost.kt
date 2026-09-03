package com.app.flashlearn.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.flashlearn.presentation.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val ONBOARDING = "onboarding"
}

@Composable
fun FlashLearnNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen()
        }
    }
}
