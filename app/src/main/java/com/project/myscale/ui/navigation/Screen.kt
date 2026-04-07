package com.project.myscale.ui.navigation

sealed class Screen(val route: String) {
    data object Input : Screen("input")
    data object Chart : Screen("chart")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
    data object EditEntry : Screen("edit_entry/{entryId}") {
        fun createRoute(entryId: Long) = "edit_entry/$entryId"
    }
}
