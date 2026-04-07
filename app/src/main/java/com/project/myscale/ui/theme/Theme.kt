package com.project.myscale.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.project.myscale.data.model.ThemeOption

private val ForestColorScheme = lightColorScheme(
    primary = ForestPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimary = ForestOnPrimary,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary,
    secondaryContainer = ForestSecondaryContainer,
    onSecondary = ForestOnSecondary,
    onSecondaryContainer = ForestOnSecondaryContainer,
    tertiary = ForestTertiary,
    onTertiary = ForestOnTertiary,
    background = ForestBackground,
    surface = ForestSurface,
    surfaceVariant = ForestSurfaceVariant,
    onBackground = ForestOnBackground,
    onSurface = ForestOnSurface,
    outline = ForestOutline,
    outlineVariant = ForestOutlineVariant,
    surfaceTint = ForestSurfaceTint,
    error = ForestError,
    onError = ForestOnError,
    errorContainer = ForestErrorContainer,
    onErrorContainer = ForestOnErrorContainer
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimary = MidnightOnPrimary,
    onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = MidnightSecondary,
    secondaryContainer = MidnightSecondaryContainer,
    onSecondary = MidnightOnSecondary,
    onSecondaryContainer = MidnightOnSecondaryContainer,
    tertiary = MidnightTertiary,
    onTertiary = MidnightOnTertiary,
    background = MidnightBackground,
    surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onBackground = MidnightOnBackground,
    onSurface = MidnightOnSurface,
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineVariant,
    surfaceTint = MidnightSurfaceTint,
    error = MidnightError,
    onError = MidnightOnError,
    errorContainer = MidnightErrorContainer,
    onErrorContainer = MidnightOnErrorContainer
)

private val SunsetColorScheme = lightColorScheme(
    primary = SunsetPrimary,
    primaryContainer = SunsetPrimaryContainer,
    onPrimary = SunsetOnPrimary,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary,
    secondaryContainer = SunsetSecondaryContainer,
    onSecondary = SunsetOnSecondary,
    onSecondaryContainer = SunsetOnSecondaryContainer,
    tertiary = SunsetTertiary,
    onTertiary = SunsetOnTertiary,
    background = SunsetBackground,
    surface = SunsetSurface,
    surfaceVariant = SunsetSurfaceVariant,
    onBackground = SunsetOnBackground,
    onSurface = SunsetOnSurface,
    outline = SunsetOutline,
    outlineVariant = SunsetOutlineVariant,
    surfaceTint = SunsetSurfaceTint,
    error = SunsetError,
    onError = SunsetOnError,
    errorContainer = SunsetErrorContainer,
    onErrorContainer = SunsetOnErrorContainer
)

@Composable
fun BodyTrackTheme(
    themeOption: ThemeOption = ThemeOption.FOREST,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeOption) {
        ThemeOption.FOREST -> ForestColorScheme
        ThemeOption.MIDNIGHT -> MidnightColorScheme
        ThemeOption.SUNSET -> SunsetColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BodyTrackTypography,
        shapes = BodyTrackShapes,
        content = content
    )
}
