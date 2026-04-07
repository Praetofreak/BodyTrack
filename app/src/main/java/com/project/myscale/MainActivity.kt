package com.project.myscale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.myscale.data.model.ThemeOption
import com.project.myscale.ui.navigation.NavGraph
import com.project.myscale.ui.navigation.Screen
import com.project.myscale.ui.navigation.bottomNavItems
import com.project.myscale.ui.theme.BodyTrackTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BodyTrackApplication
        val preferencesManager = app.preferencesManager

        setContent {
            val themeOption by preferencesManager.selectedTheme
                .collectAsState(initial = ThemeOption.FOREST)
            val onboardingCompleted by preferencesManager.onboardingCompleted
                .collectAsState(initial = true)
            val enabledFields by preferencesManager.enabledInputFields
                .collectAsState(initial = setOf("WEIGHT"))

            Crossfade(targetState = themeOption, label = "theme_crossfade") { theme ->
                BodyTrackTheme(themeOption = theme) {
                    val navController = rememberNavController()
                    val snackbarHostState = remember { SnackbarHostState() }
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val showBottomBar = currentRoute in listOf(
                        Screen.Input.route,
                        Screen.Chart.route,
                        Screen.History.route
                    )
                    val showTopBar = currentRoute !in listOf(
                        Screen.Onboarding.route,
                        Screen.Settings.route,
                        Screen.EditEntry.route
                    )

                    val startDestination = if (onboardingCompleted) Screen.Input.route
                    else Screen.Onboarding.route

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (showTopBar) {
                                TopAppBar(
                                    title = {
                                        Text(
                                            "BodyTrack",
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = {
                                            navController.navigate(Screen.Settings.route)
                                        }) {
                                            Icon(
                                                Icons.Rounded.Settings,
                                                contentDescription = "Einstellungen"
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar {
                                    bottomNavItems.forEach { item ->
                                        val selected = navBackStackEntry?.destination?.hierarchy
                                            ?.any { it.route == item.route } == true

                                        NavigationBarItem(
                                            icon = {
                                                Icon(item.icon, contentDescription = item.label)
                                            },
                                            label = { Text(item.label) },
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination,
                            themeOption = theme,
                            enabledFields = enabledFields,
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
