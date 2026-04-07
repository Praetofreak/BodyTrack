package com.project.myscale.ui.screens.history.components

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.myscale.BodyTrackApplication
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.ui.components.ConfirmDeleteDialog
import com.project.myscale.ui.components.DatePickerField
import com.project.myscale.ui.components.MeasurementInputField
import com.project.myscale.ui.components.WeightInputField
import com.project.myscale.ui.screens.input.InputFieldState
import com.project.myscale.util.CalculationUtils
import com.project.myscale.util.DateUtils
import com.project.myscale.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    entryId: Long,
    enabledFields: Set<String>,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BodyTrackApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var entry by remember { mutableStateOf<BodyEntry?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var weightInput by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf<String?>(null) }
    var weightWarning by remember { mutableStateOf<String?>(null) }
    val fieldStates = remember { mutableStateMapOf<MeasurementType, InputFieldState>() }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var plausibilityWarning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(entryId) {
        val loaded = withContext(Dispatchers.IO) { repository.getEntryById(entryId) }
        if (loaded != null) {
            entry = loaded
            selectedDate = loaded.date
            val weight = loaded.measurements[MeasurementType.WEIGHT]
            weightInput = weight?.let { Validators.formatDecimalInput(it.valueKg) } ?: ""

            // Load all fields: those with values + those enabled in settings
            val typesWithValues = loaded.measurements.keys.filter { !it.isPrimary }
            val enabledTypes = enabledFields.mapNotNull { name ->
                try { MeasurementType.valueOf(name) } catch (_: Exception) { null }
            }.filter { !it.isPrimary }

            val allTypes = (typesWithValues + enabledTypes).distinct().sortedBy { it.sortOrder }
            for (type in allTypes) {
                val value = loaded.measurements[type]
                if (value != null) {
                    val displayValue = when (value.inputMode) {
                        InputMode.PERCENT -> value.valuePercent?.let { Validators.formatDecimalInput(it) } ?: ""
                        InputMode.KG -> Validators.formatDecimalInput(value.valueKg)
                    }
                    fieldStates[type] = InputFieldState(
                        value = displayValue,
                        inputMode = value.inputMode
                    )
                } else {
                    fieldStates[type] = InputFieldState(inputMode = InputMode.PERCENT)
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            dateText = DateUtils.formatShortDate(selectedDate),
            onConfirm = {
                scope.launch {
                    repository.deleteEntry(entryId)
                    showDeleteDialog = false
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eintrag bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DatePickerField(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    WeightInputField(
                        value = weightInput,
                        onValueChange = { value ->
                            weightInput = value
                            val validation = if (value.isNotBlank()) Validators.validateWeight(value) else Validators.ValidationResult(true)
                            weightError = if (!validation.isValid) validation.errorMessage else null
                        },
                        errorMessage = weightError,
                        warningMessage = weightWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            for ((type, fieldState) in fieldStates.entries.sortedBy { it.key.sortOrder }) {
                MeasurementInputField(
                    type = type,
                    value = fieldState.value,
                    onValueChange = { value ->
                        val weightKg = Validators.parseDecimalInput(weightInput)
                        val error = when (fieldState.inputMode) {
                            InputMode.KG -> {
                                val result = Validators.validateOptionalKg(value, weightKg)
                                if (!result.isValid) result.errorMessage else null
                            }
                            InputMode.PERCENT -> {
                                val result = Validators.validateOptionalPercent(value)
                                if (!result.isValid) result.errorMessage else null
                            }
                        }
                        fieldStates[type] = fieldState.copy(value = value, error = error)
                    },
                    inputMode = fieldState.inputMode,
                    onInputModeChange = { mode ->
                        fieldStates[type] = fieldState.copy(inputMode = mode, value = "", error = null)
                    },
                    errorMessage = fieldState.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            AnimatedVisibility(visible = plausibilityWarning != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = plausibilityWarning ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val weightValidation = Validators.validateWeight(weightInput)
                    if (!weightValidation.isValid) {
                        weightError = weightValidation.errorMessage
                        return@Button
                    }
                    val weightKg = Validators.parseDecimalInput(weightInput) ?: return@Button
                    if (fieldStates.any { it.value.error != null }) return@Button

                    scope.launch {
                        isSaving = true
                        try {
                            val measurements = mutableMapOf<MeasurementType, MeasurementValue>()
                            measurements[MeasurementType.WEIGHT] = MeasurementValue(
                                valueKg = weightKg,
                                valuePercent = null,
                                inputMode = InputMode.KG
                            )
                            for ((type, fs) in fieldStates) {
                                if (fs.value.isBlank()) continue
                                val parsedValue = Validators.parseDecimalInput(fs.value) ?: continue
                                measurements[type] = CalculationUtils.calculateMeasurementValue(
                                    type = type,
                                    inputValue = parsedValue,
                                    inputMode = fs.inputMode,
                                    weightKg = weightKg
                                )
                            }
                            val updatedEntry = BodyEntry(
                                id = entryId,
                                date = selectedDate,
                                measurements = measurements
                            )
                            repository.updateEntry(updatedEntry)
                            onNavigateBack()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Fehler: ${e.message}")
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = weightInput.isNotBlank() && weightError == null && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Änderungen speichern", style = MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Eintrag löschen",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
