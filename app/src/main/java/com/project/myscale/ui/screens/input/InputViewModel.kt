package com.project.myscale.ui.screens.input

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.myscale.BodyTrackApplication
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.util.CalculationUtils
import com.project.myscale.util.Validators
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class InputFieldState(
    val value: String = "",
    val inputMode: InputMode = InputMode.PERCENT,
    val error: String? = null
)

data class InputUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val weightInput: String = "",
    val weightError: String? = null,
    val weightWarning: String? = null,
    val fieldStates: Map<MeasurementType, InputFieldState> = emptyMap(),
    val enabledFields: Set<MeasurementType> = emptySet(),
    val existingEntryForDate: BodyEntry? = null,
    val isSaving: Boolean = false,
    val plausibilityWarning: String? = null
)

sealed class InputEvent {
    data class SaveSuccess(val dateText: String) : InputEvent()
    data class Error(val message: String) : InputEvent()
}

class InputViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodyTrackApplication
    private val repository = app.repository
    private val preferencesManager = app.preferencesManager

    private val _uiState = MutableStateFlow(InputUiState())
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InputEvent>()
    val events = _events.asSharedFlow()

    val enabledInputFields = preferencesManager.enabledInputFields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("WEIGHT"))

    val defaultInputMode = preferencesManager.defaultInputMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InputMode.PERCENT)

    private var lastWeight: Double? = null

    init {
        viewModelScope.launch {
            val latest = repository.getLatestEntry()
            lastWeight = latest?.measurements?.get(MeasurementType.WEIGHT)?.valueKg
        }

        viewModelScope.launch {
            enabledInputFields.collect { fieldNames ->
                val types = fieldNames.mapNotNull { name ->
                    try { MeasurementType.valueOf(name) } catch (_: Exception) { null }
                }.filter { !it.isPrimary }.sortedBy { it.sortOrder }

                val currentStates = _uiState.value.fieldStates.toMutableMap()
                val defaultMode = defaultInputMode.value
                for (type in types) {
                    if (!currentStates.containsKey(type)) {
                        currentStates[type] = InputFieldState(inputMode = defaultMode)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    enabledFields = types.toSet(),
                    fieldStates = currentStates
                )
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        viewModelScope.launch {
            val existing = repository.getEntryByDate(date)
            _uiState.value = _uiState.value.copy(
                selectedDate = date,
                existingEntryForDate = existing
            )
        }
    }

    fun loadExistingEntry() {
        val existing = _uiState.value.existingEntryForDate ?: return
        val weightVal = existing.measurements[MeasurementType.WEIGHT]
        val newFieldStates = mutableMapOf<MeasurementType, InputFieldState>()

        for ((type, value) in existing.measurements) {
            if (type == MeasurementType.WEIGHT) continue
            val displayValue = when (value.inputMode) {
                InputMode.PERCENT -> value.valuePercent?.let { Validators.formatDecimalInput(it) } ?: ""
                InputMode.KG -> Validators.formatDecimalInput(value.valueKg)
            }
            newFieldStates[type] = InputFieldState(
                value = displayValue,
                inputMode = value.inputMode
            )
        }

        // Also include enabled fields that don't have values
        for (type in _uiState.value.enabledFields) {
            if (!newFieldStates.containsKey(type)) {
                newFieldStates[type] = InputFieldState(inputMode = defaultInputMode.value)
            }
        }

        _uiState.value = _uiState.value.copy(
            weightInput = weightVal?.let { Validators.formatDecimalInput(it.valueKg) } ?: "",
            fieldStates = newFieldStates
        )
    }

    fun onWeightChanged(value: String) {
        val validation = if (value.isNotBlank()) Validators.validateWeight(value) else Validators.ValidationResult(true)
        val parsedWeight = Validators.parseDecimalInput(value)
        val warning = if (parsedWeight != null) {
            Validators.checkWeightDeviation(parsedWeight, lastWeight)
        } else null

        _uiState.value = _uiState.value.copy(
            weightInput = value,
            weightError = if (!validation.isValid) validation.errorMessage else null,
            weightWarning = warning
        )
        checkPlausibility()
    }

    fun onFieldValueChanged(type: MeasurementType, value: String) {
        val currentState = _uiState.value.fieldStates[type] ?: InputFieldState()
        val weightKg = Validators.parseDecimalInput(_uiState.value.weightInput)

        val error = when (currentState.inputMode) {
            InputMode.KG -> {
                val result = Validators.validateOptionalKg(value, weightKg)
                if (!result.isValid) result.errorMessage else null
            }
            InputMode.PERCENT -> {
                val result = Validators.validateOptionalPercent(value)
                if (!result.isValid) result.errorMessage else null
            }
        }

        val updatedStates = _uiState.value.fieldStates.toMutableMap()
        updatedStates[type] = currentState.copy(value = value, error = error)
        _uiState.value = _uiState.value.copy(fieldStates = updatedStates)
        checkPlausibility()
    }

    fun onFieldModeChanged(type: MeasurementType, mode: InputMode) {
        val currentState = _uiState.value.fieldStates[type] ?: InputFieldState()
        val updatedStates = _uiState.value.fieldStates.toMutableMap()
        updatedStates[type] = currentState.copy(inputMode = mode, value = "", error = null)
        _uiState.value = _uiState.value.copy(fieldStates = updatedStates)
    }

    private fun checkPlausibility() {
        val percentValues = _uiState.value.fieldStates
            .filter { it.key.supportsPercent && it.value.inputMode == InputMode.PERCENT }
            .mapNotNull { Validators.parseDecimalInput(it.value.value) }
        val warning = Validators.checkPercentSum(percentValues)
        _uiState.value = _uiState.value.copy(plausibilityWarning = warning)
    }

    fun saveEntry() {
        val state = _uiState.value
        val weightValidation = Validators.validateWeight(state.weightInput)
        if (!weightValidation.isValid) {
            _uiState.value = state.copy(weightError = weightValidation.errorMessage)
            return
        }

        val weightKg = Validators.parseDecimalInput(state.weightInput) ?: return

        // Check for field errors
        if (state.fieldStates.any { it.value.error != null }) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val measurements = mutableMapOf<MeasurementType, MeasurementValue>()

                // Weight
                measurements[MeasurementType.WEIGHT] = MeasurementValue(
                    valueKg = weightKg,
                    valuePercent = null,
                    inputMode = InputMode.KG
                )

                // Other measurements
                for ((type, fieldState) in state.fieldStates) {
                    if (fieldState.value.isBlank()) continue
                    val parsedValue = Validators.parseDecimalInput(fieldState.value) ?: continue
                    measurements[type] = CalculationUtils.calculateMeasurementValue(
                        type = type,
                        inputValue = parsedValue,
                        inputMode = fieldState.inputMode,
                        weightKg = weightKg
                    )
                }

                val entry = BodyEntry(
                    date = state.selectedDate,
                    measurements = measurements
                )

                repository.saveEntry(entry)
                lastWeight = weightKg

                val dateText = com.project.myscale.util.DateUtils.formatShortDate(state.selectedDate)
                _events.emit(InputEvent.SaveSuccess(dateText))

                // Reset form
                val defaultMode = defaultInputMode.value
                val resetFields = state.enabledFields.associateWith {
                    InputFieldState(inputMode = defaultMode)
                }
                _uiState.value = InputUiState(
                    enabledFields = state.enabledFields,
                    fieldStates = resetFields
                )
            } catch (e: Exception) {
                _events.emit(InputEvent.Error("Fehler beim Speichern: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
