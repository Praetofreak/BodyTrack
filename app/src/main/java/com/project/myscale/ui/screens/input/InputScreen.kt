package com.project.myscale.ui.screens.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.myscale.R
import com.project.myscale.ui.components.DatePickerField
import com.project.myscale.ui.components.MeasurementInputField
import com.project.myscale.ui.components.WeightInputField
import com.project.myscale.util.DateUtils

@Composable
fun InputScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateToSettings: () -> Unit,
    viewModel: InputViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InputEvent.SaveSuccess -> {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.save_success,
                            DateUtils.formatShortDate(event.date)
                        )
                    )
                }
                is InputEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.error_saving, event.detail ?: "")
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Weight field - prominent, shown first
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                WeightInputField(
                    value = uiState.weightInput,
                    onValueChange = { viewModel.onWeightChanged(it) },
                    error = uiState.weightError,
                    showDeviationWarning = uiState.showWeightDeviationWarning
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional measurement fields — render everything the form holds, incl.
        // prefilled values of types that are disabled in the settings
        val optionalTypes = uiState.fieldStates.keys.sortedBy { it.sortOrder }
        if (optionalTypes.isEmpty()) {
            TextButton(onClick = onNavigateToSettings) {
                Text(
                    stringResource(R.string.input_enable_more_fields),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            for (type in optionalTypes) {
                val fieldState = uiState.fieldStates[type] ?: continue
                MeasurementInputField(
                    type = type,
                    value = fieldState.value,
                    onValueChange = { viewModel.onFieldValueChanged(type, it) },
                    inputMode = fieldState.inputMode,
                    onInputModeChange = { viewModel.onFieldModeChanged(type, it) },
                    error = fieldState.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Plausibility warning
        AnimatedVisibility(visible = uiState.showPercentSumWarning) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.warning_percent_sum),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Picker - at the bottom before save button
        DatePickerField(
            selectedDate = uiState.selectedDate,
            onDateSelected = { viewModel.onDateSelected(it) }
        )

        // Existing entry banner (values are already prefilled into the form)
        AnimatedVisibility(visible = uiState.existingEntryForDate != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.input_editing_existing_entry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save button
        Button(
            onClick = { viewModel.saveEntry() },
            enabled = uiState.weightInput.isNotBlank() && uiState.weightError == null && !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.action_save_entry), style = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
