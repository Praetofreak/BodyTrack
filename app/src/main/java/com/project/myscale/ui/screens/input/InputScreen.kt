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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.ui.components.DatePickerField
import com.project.myscale.ui.components.MeasurementInputField
import com.project.myscale.ui.components.WeightInputField

@Composable
fun InputScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateToSettings: () -> Unit,
    viewModel: InputViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val enabledFields by viewModel.enabledInputFields.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InputEvent.SaveSuccess -> {
                    snackbarHostState.showSnackbar("Eintrag für ${event.dateText} gespeichert")
                }
                is InputEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
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
                    errorMessage = uiState.weightError,
                    warningMessage = uiState.weightWarning
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional measurement fields
        val optionalTypes = uiState.enabledFields.sortedBy { it.sortOrder }
        if (optionalTypes.isEmpty()) {
            TextButton(onClick = onNavigateToSettings) {
                Text(
                    "Du kannst in den Einstellungen weitere Messwerte aktivieren \u2192",
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
                    errorMessage = fieldState.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Plausibility warning
        AnimatedVisibility(visible = uiState.plausibilityWarning != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.plausibilityWarning ?: "",
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

        // Existing entry banner
        AnimatedVisibility(visible = uiState.existingEntryForDate != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Für dieses Datum existiert bereits ein Eintrag. Die Werte werden überschrieben.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    TextButton(onClick = { viewModel.loadExistingEntry() }) {
                        Text("Bestehenden Eintrag laden")
                    }
                }
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
                Text("Eintrag speichern", style = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
