package com.project.myscale.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        label = "Eintrag",
        icon = Icons.Rounded.Add,
        route = Screen.Input.route
    ),
    BottomNavItem(
        label = "Verlauf",
        icon = Icons.AutoMirrored.Rounded.ShowChart,
        route = Screen.Chart.route
    ),
    BottomNavItem(
        label = "Historie",
        icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
        route = Screen.History.route
    )
)
