package com.project.myscale.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.graphics.vector.ImageVector
import com.project.myscale.R

data class BottomNavItem(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        labelRes = R.string.nav_input,
        icon = Icons.Rounded.Add,
        route = Screen.Input.route
    ),
    BottomNavItem(
        labelRes = R.string.nav_chart,
        icon = Icons.AutoMirrored.Rounded.ShowChart,
        route = Screen.Chart.route
    ),
    BottomNavItem(
        labelRes = R.string.nav_history,
        icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
        route = Screen.History.route
    )
)
