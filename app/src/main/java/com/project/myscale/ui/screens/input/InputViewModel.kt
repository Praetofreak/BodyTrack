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
import com.project.myscale.util.Validators.ValidationError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class InputFieldState(
    val value: String = "",
    val inputMode: InputMode = InputMode.PERCENT,
    val error: ValidationError? = null
)

data class InputUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val weightInput: String = "",
    val weightError: ValidationError? = null,
    val showWeightDeviationWarning: Boolean = false,
    val fieldStates: Map<MeasurementType, InputFieldState> = emptyMap(),
    val enabledFields: Set<MeasurementType> = emptySet(),
    val existingEntryForDate: BodyEntry? = null,
    val isSaving: Boolean = false,
    val showPercentSumWarning: Boolean = false
)

sealed class InputEvent {
    data class SaveSuccess(val date: LocalDate) : InputEvent()
    data class Error(val detail: String?) : InputEvent()
}

class InputViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodyTrackApplication
    private val repository = app.repository
    private val preferencesManager = app.preferencesManager

    private val _uiState = MutableStateFlow(InputUiState())
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InputEvent>()
    val events = _events.asSharedFlow()

    private var defaultMode = InputMode.PERCENT

    /** Weight of the entry chronologically before the selected date, for the deviation warning. */
    private var previousWeight: Double? = null

    init {
        viewModelScope.launch {
            preferencesManager.defaultInputMode.collect { mode ->
                defaultMode = mode
                // Untouched (empty) fields follow the default unit setting
                val updated = _uiState.value.fieldStates.mapValues { (_, fs) ->
                    if (fs.value.isBlank()) fs.copy(inputMode = mode, error = null) else fs
                }
                _uiState.value = _uiState.value.copy(fieldStates = updated)
            }
        }

        viewModelScope.launch {
            preferencesManager.enabledInputFields.collect { fieldNames ->
                val types = fieldNames.mapNotNull { name ->
                    try { MeasurementType.valueOf(name) } catch (_: IllegalArgumentException) { null }
                }.filter { !it.isPrimary }.sortedBy { it.sortOrder }

                val currentStates = _uiState.value.fieldStates
                val newStates = mutableMapOf<MeasurementType, InputFieldState>()
                // Never drop entered or prefilled data, even for disabled types —
                // otherwise saving would silently delete those measurements
                for ((type, fs) in currentStates) {
                    if (fs.value.isNotBlank()) newStates[type] = fs
                }
                for (type in types) {
                    if (type !in newStates) {
                        newStates[type] = currentStates[type] ?: InputFieldState(inputMode = defaultMode)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    enabledFields = types.toSet(),
                    fieldStates = newStates
                )
            }
        }

        // Prefill today's entry if one already exists
        onDateSelected(_uiState.value.selectedDate)
    }

    fun onDateSelected(date: LocalDate) {
        viewModelScope.launch {
            val existing = repository.getEntryByDate(date)
            previousWeight = repository.getEntryBefore(date)
                ?.measurements?.get(MeasurementType.WEIGHT)?.valueKg

            if (existing != null) {
                applyEntryToForm(date, existing)
            } else if (_uiState.value.existingEntryForDate != null) {
                // Leaving a prefilled date for an empty one: prefilled values
                // belong to their date, so start with a blank form
                _uiState.value = _uiState.value.copy(
                    selectedDate = date,
                    existingEntryForDate = null,
                    weightInput = "",
                    weightError = null,
                    fieldStates = _uiState.value.enabledFields.associateWith {
                        InputFieldState(inputMode = defaultMode)
                    },
                    showPercentSumWarning = false
                )
                refreshWeightDeviation()
            } else {
                // Manually typed values carry over to the newly picked date
                _uiState.value = _uiState.value.copy(
                    selectedDate = date,
                    existingEntryForDate = null
                )
                refreshWeightDeviation()
            }
        }
    }

    /** Loads an existing entry's values into the form so saving never silently drops data. */
    private fun applyEntryToForm(date: LocalDate, entry: BodyEntry) {
        val state = _uiState.value
        val newFieldStates = mutableMapOf<MeasurementType, InputFieldState>()

        for (type in state.enabledFields) {
            newFieldStates[type] = InputFieldState(inputMode = defaultMode)
        }
        for ((type, value) in entry.measurements) {
            if (type == MeasurementType.WEIGHT) continue
            val displayValue = when (value.inputMode) {
                InputMode.PERCENT -> value.valuePercent?.let { Validators.formatDecimalInput(it) } ?: ""
                InputMode.KG -> Validators.formatDecimalInput(value.valueKg)
            }
            newFieldStates[type] = InputFieldState(value = displayValue, inputMode = value.inputMode)
        }

        val weightValue = entry.measurements[MeasurementType.WEIGHT]
        _uiState.value = state.copy(
            selectedDate = date,
            existingEntryForDate = entry,
            weightInput = weightValue?.let { Validators.formatDecimalInput(it.valueKg) } ?: "",
            weightError = null,
            fieldStates = newFieldStates
        )
        refreshWeightDeviation()
        refreshPercentSumWarning()
    }

    fun onWeightChanged(value: String) {
        val validation = if (value.isNotBlank()) {
            Validators.validateWeight(value)
        } else {
            Validators.ValidationResult(true)
        }
        _uiState.value = _uiState.value.copy(
            weightInput = value,
            weightError = if (!validation.isValid) validation.error else null
        )
        refreshWeightDeviation()
        refreshPercentSumWarning()
    }

    fun onFieldValueChanged(type: MeasurementType, value: String) {
        val currentState = _uiState.value.fieldStates[type] ?: InputFieldState(inputMode = defaultMode)
        val weightKg = Validators.parseDecimalInput(_uiState.value.weightInput)

        val error = when (currentState.inputMode) {
            InputMode.KG -> Validators.validateOptionalKg(value, weightKg).error
            InputMode.PERCENT -> Validators.validateOptionalPercent(value).error
        }

        val updatedStates = _uiState.value.fieldStates.toMutableMap()
        updatedStates[type] = currentState.copy(value = value, error = error)
        _uiState.value = _uiState.value.copy(fieldStates = updatedStates)
        refreshPercentSumWarning()
    }

    fun onFieldModeChanged(type: MeasurementType, mode: InputMode) {
        val currentState = _uiState.value.fieldStates[type] ?: InputFieldState(inputMode = defaultMode)
        val updatedStates = _uiState.value.fieldStates.toMutableMap()
        updatedStates[type] = currentState.copy(inputMode = mode, value = "", error = null)
        _uiState.value = _uiState.value.copy(fieldStates = updatedStates)
        refreshPercentSumWarning()
    }

    private fun refreshWeightDeviation() {
        val parsed = Validators.parseDecimalInput(_uiState.value.weightInput)
        val warn = parsed != null && Validators.isLargeWeightDeviation(parsed, previousWeight)
        _uiState.value = _uiState.value.copy(showWeightDeviationWarning = warn)
    }

    private fun refreshPercentSumWarning() {
        val percentValues = _uiState.value.fieldStates
            .filter { it.key.supportsPercent && it.value.inputMode == InputMode.PERCENT }
            .mapNotNull { Validators.parseDecimalInput(it.value.value) }
        _uiState.value = _uiState.value.copy(
            showPercentSumWarning = Validators.exceedsPercentSum(percentValues)
        )
    }

    fun saveEntry() {
        val state = _uiState.value
        val weightValidation = Validators.validateWeight(state.weightInput)
        if (!weightValidation.isValid) {
            _uiState.value = state.copy(weightError = weightValidation.error)
            return
        }

        val weightKg = Validators.parseDecimalInput(state.weightInput) ?: return
        if (state.fieldStates.any { it.value.error != null }) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val measurements = mutableMapOf<MeasurementType, MeasurementValue>()
                measurements[MeasurementType.WEIGHT] = MeasurementValue(
                    valueKg = weightKg,
                    valuePercent = null,
                    inputMode = InputMode.KG
                )

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

                repository.saveEntry(
                    BodyEntry(date = state.selectedDate, measurements = measurements)
                )
                _events.emit(InputEvent.SaveSuccess(state.selectedDate))

                // Re-sync the form with what is now stored for this date
                onDateSelected(state.selectedDate)
            } catch (e: Exception) {
                _events.emit(InputEvent.Error(e.message))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
