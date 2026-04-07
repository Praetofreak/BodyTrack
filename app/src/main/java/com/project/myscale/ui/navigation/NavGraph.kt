package com.project.myscale.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.project.myscale.data.model.ThemeOption
import com.project.myscale.ui.screens.chart.ChartScreen
import com.project.myscale.ui.screens.history.HistoryScreen
import com.project.myscale.ui.screens.history.components.EditEntryScreen
import com.project.myscale.ui.screens.input.InputScreen
import com.project.myscale.ui.screens.onboarding.OnboardingScreen
import com.project.myscale.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    themeOption: ThemeOption,
    enabledFields: Set<String>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Input.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Input.route) {
            InputScreen(
                snackbarHostState = snackbarHostState,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Chart.route) {
            ChartScreen(themeOption = themeOption)
        }

        composable(Screen.History.route) {
            HistoryScreen(
                themeOption = themeOption,
                snackbarHostState = snackbarHostState,
                onNavigateToInput = {
                    navController.navigate(Screen.Input.route) {
                        popUpTo(Screen.History.route) { inclusive = false }
                    }
                },
                onNavigateToEdit = { entryId ->
                    navController.navigate(Screen.EditEntry.createRoute(entryId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onRestartOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.EditEntry.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: return@composable
            EditEntryScreen(
                entryId = entryId,
                enabledFields = enabledFields,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
