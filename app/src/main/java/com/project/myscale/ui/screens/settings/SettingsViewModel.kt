package com.project.myscale.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.myscale.BodyTrackApplication
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.ThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val enabledFields: Set<String> = setOf("WEIGHT"),
    val defaultInputMode: InputMode = InputMode.PERCENT,
    val selectedTheme: ThemeOption = ThemeOption.FOREST
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodyTrackApplication
    private val preferencesManager = app.preferencesManager
    val repository = app.repository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.enabledInputFields.collect { fields ->
                _uiState.value = _uiState.value.copy(enabledFields = fields)
            }
        }
        viewModelScope.launch {
            preferencesManager.defaultInputMode.collect { mode ->
                _uiState.value = _uiState.value.copy(defaultInputMode = mode)
            }
        }
        viewModelScope.launch {
            preferencesManager.selectedTheme.collect { theme ->
                _uiState.value = _uiState.value.copy(selectedTheme = theme)
            }
        }
    }

    fun toggleField(type: MeasurementType) {
        if (type.isPrimary) return // Can't disable weight
        val current = _uiState.value.enabledFields.toMutableSet()
        if (current.contains(type.name)) {
            current.remove(type.name)
        } else {
            current.add(type.name)
        }
        viewModelScope.launch {
            preferencesManager.setEnabledInputFields(current)
        }
    }

    fun setDefaultInputMode(mode: InputMode) {
        viewModelScope.launch {
            preferencesManager.setDefaultInputMode(mode)
        }
    }

    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            preferencesManager.setSelectedTheme(theme)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(completed)
        }
    }
}
