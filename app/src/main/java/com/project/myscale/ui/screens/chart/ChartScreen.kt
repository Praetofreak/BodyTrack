package com.project.myscale.ui.screens.chart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.ThemeOption
import com.project.myscale.ui.components.EmptyStateView
import com.project.myscale.ui.screens.chart.components.CategoryFilterChips
import com.project.myscale.ui.screens.chart.components.CombinedChart
import com.project.myscale.ui.screens.chart.components.DevelopmentSummarySection
import com.project.myscale.ui.screens.chart.components.SingleValueChart
import com.project.myscale.ui.screens.chart.components.TimeRangeSelector

@Composable
fun ChartScreen(
    themeOption: ThemeOption,
    viewModel: ChartViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkTheme = themeOption.isDark

    if (uiState.allEntries.isEmpty()) {
        EmptyStateView(
            icon = Icons.AutoMirrored.Rounded.ShowChart,
            message = "Noch keine Daten vorhanden.\nErstelle zuerst einen Eintrag."
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // View mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val leftShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
            val rightShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)

            OutlinedButton(
                onClick = { viewModel.setViewMode(ChartViewMode.COMBINED) },
                shape = leftShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (uiState.viewMode == ChartViewMode.COMBINED)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (uiState.viewMode == ChartViewMode.COMBINED)
                        MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) {
                Text("Gesamt")
            }
            OutlinedButton(
                onClick = { viewModel.setViewMode(ChartViewMode.INDIVIDUAL) },
                shape = rightShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (uiState.viewMode == ChartViewMode.INDIVIDUAL)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (uiState.viewMode == ChartViewMode.INDIVIDUAL)
                        MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) {
                Text("Einzeln")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Unit toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val leftShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
            val rightShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)

            OutlinedButton(
                onClick = { viewModel.setDisplayMode(InputMode.KG) },
                shape = leftShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (uiState.displayMode == InputMode.KG)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (uiState.displayMode == InputMode.KG)
                        MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("kg")
            }
            OutlinedButton(
                onClick = { viewModel.setDisplayMode(InputMode.PERCENT) },
                shape = rightShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (uiState.displayMode == InputMode.PERCENT)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (uiState.displayMode == InputMode.PERCENT)
                        MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("%")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time range
        TimeRangeSelector(
            selectedRange = uiState.timeRange,
            onRangeSelected = { viewModel.setTimeRange(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category filter chips
        CategoryFilterChips(
            availableTypes = uiState.availableTypes,
            activeTypes = uiState.activeTypes,
            onToggleType = { viewModel.toggleType(it) },
            isDarkTheme = isDarkTheme,
            displayMode = uiState.displayMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Charts
        if (uiState.filteredEntries.isEmpty()) {
            Text(
                text = "Keine Daten in diesem Zeitraum",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 32.dp)
            )
        } else {
            Crossfade(targetState = uiState.viewMode, label = "chart_mode") { mode ->
                Column {
                    when (mode) {
                        ChartViewMode.COMBINED -> {
                            CombinedChart(
                                entries = uiState.filteredEntries,
                                activeTypes = uiState.activeTypes,
                                displayMode = uiState.displayMode,
                                isDarkTheme = isDarkTheme
                            )
                        }
                        ChartViewMode.INDIVIDUAL -> {
                            uiState.activeTypes.sortedBy { it.sortOrder }.forEach { type ->
                                if (uiState.displayMode == InputMode.PERCENT && !type.supportsPercent) return@forEach
                                AnimatedVisibility(
                                    visible = true,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    SingleValueChart(
                                        type = type,
                                        entries = uiState.filteredEntries,
                                        displayMode = uiState.displayMode,
                                        isDarkTheme = isDarkTheme
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Development summary
        DevelopmentSummarySection(
            summaries = uiState.developmentSummaries,
            timeRange = uiState.timeRange,
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
