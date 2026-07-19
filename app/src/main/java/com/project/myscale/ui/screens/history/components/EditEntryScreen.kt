package com.project.myscale.ui.screens.history.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.myscale.BodyTrackApplication
import com.project.myscale.R
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.ui.components.ConfirmDeleteDialog
import com.project.myscale.ui.components.DatePickerField
import com.project.myscale.ui.components.MeasurementInputField
import com.project.myscale.ui.components.MergeConflictField
import com.project.myscale.ui.components.MergeEntriesDialog
import com.project.myscale.ui.components.WeightInputField
import com.project.myscale.ui.screens.input.InputFieldState
import com.project.myscale.util.CalculationUtils
import com.project.myscale.util.DateUtils
import com.project.myscale.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Pending date conflict: the edited values plus the entry already on the target date. */
private data class PendingMerge(
    val editedMeasurements: Map<MeasurementType, MeasurementValue>,
    val existingEntry: BodyEntry,
    val conflicts: List<MergeConflictField>
)

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

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var weightInput by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf<Validators.ValidationError?>(null) }
    val fieldStates = remember { mutableStateMapOf<MeasurementType, InputFieldState>() }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPercentSumWarning by remember { mutableStateOf(false) }
    var pendingMerge by remember { mutableStateOf<PendingMerge?>(null) }

    fun refreshPercentSumWarning() {
        val percentValues = fieldStates
            .filter { it.key.supportsPercent && it.value.inputMode == InputMode.PERCENT }
            .mapNotNull { Validators.parseDecimalInput(it.value.value) }
        showPercentSumWarning = Validators.exceedsPercentSum(percentValues)
    }

    LaunchedEffect(entryId) {
        val defaultMode = app.preferencesManager.defaultInputMode.first()
        val loaded = withContext(Dispatchers.IO) { repository.getEntryById(entryId) }
        if (loaded != null) {
            selectedDate = loaded.date
            val weight = loaded.measurements[MeasurementType.WEIGHT]
            weightInput = weight?.let { Validators.formatDecimalInput(it.valueKg) } ?: ""

            // Load all fields: those with values + those enabled in settings
            val typesWithValues = loaded.measurements.keys.filter { !it.isPrimary }
            val enabledTypes = enabledFields.mapNotNull { name ->
                try { MeasurementType.valueOf(name) } catch (_: IllegalArgumentException) { null }
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
                    fieldStates[type] = InputFieldState(inputMode = defaultMode)
                }
            }
            refreshPercentSumWarning()
        }
    }

    fun buildMeasurements(weightKg: Double): Map<MeasurementType, MeasurementValue> {
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
        return measurements
    }

    suspend fun performSave(measurements: Map<MeasurementType, MeasurementValue>) {
        val existingAtDate = withContext(Dispatchers.IO) { repository.getEntryByDate(selectedDate) }
        if (existingAtDate != null && existingAtDate.id != entryId) {
            // Another entry occupies the target date -> ask the user how to merge
            val conflicts = measurements.mapNotNull { (type, edited) ->
                val other = existingAtDate.measurements[type]
                if (other != null && other != edited) {
                    MergeConflictField(type = type, editedValue = edited, existingValue = other)
                } else null
            }.sortedBy { it.type.sortOrder }
            pendingMerge = PendingMerge(measurements, existingAtDate, conflicts)
            return
        }
        withContext(Dispatchers.IO) {
            repository.updateEntry(
                BodyEntry(id = entryId, date = selectedDate, measurements = measurements)
            )
        }
        onNavigateBack()
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            dateText = DateUtils.formatShortDate(selectedDate),
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) { repository.deleteEntry(entryId) }
                    showDeleteDialog = false
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    pendingMerge?.let { merge ->
        MergeEntriesDialog(
            targetDate = selectedDate,
            conflicts = merge.conflicts,
            onConfirm = { chosenEdited ->
                scope.launch {
                    isSaving = true
                    try {
                        val mergedMeasurements = mutableMapOf<MeasurementType, MeasurementValue>()
                        val allTypes = merge.editedMeasurements.keys + merge.existingEntry.measurements.keys
                        for (type in allTypes) {
                            val edited = merge.editedMeasurements[type]
                            val existing = merge.existingEntry.measurements[type]
                            mergedMeasurements[type] = when {
                                edited != null && existing != null ->
                                    if (merge.conflicts.none { it.type == type } || type in chosenEdited) edited
                                    else existing
                                edited != null -> edited
                                else -> existing!!
                            }
                        }
                        withContext(Dispatchers.IO) {
                            repository.mergeEntries(
                                merged = BodyEntry(
                                    id = entryId,
                                    date = selectedDate,
                                    measurements = mergedMeasurements
                                ),
                                obsoleteEntryId = merge.existingEntry.id
                            )
                        }
                        pendingMerge = null
                        onNavigateBack()
                    } catch (e: Exception) {
                        pendingMerge = null
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.error_saving, e.message ?: "")
                        )
                    } finally {
                        isSaving = false
                    }
                }
            },
            onDismiss = { pendingMerge = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_entry_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
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
                            val validation = if (value.isNotBlank()) {
                                Validators.validateWeight(value)
                            } else {
                                Validators.ValidationResult(true)
                            }
                            weightError = if (!validation.isValid) validation.error else null
                        },
                        error = weightError,
                        showDeviationWarning = false
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
                            InputMode.KG -> Validators.validateOptionalKg(value, weightKg).error
                            InputMode.PERCENT -> Validators.validateOptionalPercent(value).error
                        }
                        fieldStates[type] = fieldState.copy(value = value, error = error)
                        refreshPercentSumWarning()
                    },
                    inputMode = fieldState.inputMode,
                    onInputModeChange = { mode ->
                        fieldStates[type] = fieldState.copy(inputMode = mode, value = "", error = null)
                        refreshPercentSumWarning()
                    },
                    error = fieldState.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            AnimatedVisibility(visible = showPercentSumWarning) {
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val weightValidation = Validators.validateWeight(weightInput)
                    if (!weightValidation.isValid) {
                        weightError = weightValidation.error
                        return@Button
                    }
                    val weightKg = Validators.parseDecimalInput(weightInput) ?: return@Button
                    if (fieldStates.any { it.value.error != null }) return@Button

                    scope.launch {
                        isSaving = true
                        try {
                            performSave(buildMeasurements(weightKg))
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.error_saving, e.message ?: "")
                            )
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
                    Text(
                        stringResource(R.string.action_save_changes),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.action_delete_entry),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
