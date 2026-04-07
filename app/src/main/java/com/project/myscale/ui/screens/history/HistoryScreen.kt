package com.project.myscale.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.ThemeOption
import com.project.myscale.ui.components.ConfirmDeleteDialog
import com.project.myscale.ui.components.EmptyStateView
import com.project.myscale.ui.screens.chart.components.CategoryFilterChips
import com.project.myscale.ui.screens.chart.components.TimeRangeSelector
import com.project.myscale.ui.screens.history.components.EntryCard
import com.project.myscale.util.DateUtils

@Composable
fun HistoryScreen(
    themeOption: ThemeOption,
    snackbarHostState: SnackbarHostState,
    onNavigateToInput: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkTheme = themeOption.isDark

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.EntryDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Eintrag gelöscht",
                        actionLabel = "Rückgängig",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
                is HistoryEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    if (uiState.showDeleteDialog && uiState.entryToDelete != null) {
        ConfirmDeleteDialog(
            dateText = DateUtils.formatShortDate(uiState.entryToDelete!!.date),
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDeleteConfirmation() }
        )
    }

    if (uiState.allEntries.isEmpty()) {
        EmptyStateView(
            icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
            message = "Noch keine Einträge vorhanden",
            actionText = "Ersten Eintrag erstellen",
            onAction = onNavigateToInput
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Time range filter
        TimeRangeSelector(
            selectedRange = uiState.timeRange,
            onRangeSelected = { viewModel.setTimeRange(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category filter
        CategoryFilterChips(
            availableTypes = uiState.availableTypes,
            activeTypes = uiState.activeTypes,
            onToggleType = { viewModel.toggleType(it) },
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Unit toggle
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
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

        Spacer(modifier = Modifier.height(8.dp))

        // Entry list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = uiState.filteredEntries,
                key = { it.id }
            ) { entry ->
                EntryCard(
                    entry = entry,
                    activeTypes = uiState.activeTypes,
                    displayMode = uiState.displayMode,
                    isDarkTheme = isDarkTheme,
                    onEdit = { onNavigateToEdit(entry.id) },
                    onDelete = { viewModel.showDeleteConfirmation(entry) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
