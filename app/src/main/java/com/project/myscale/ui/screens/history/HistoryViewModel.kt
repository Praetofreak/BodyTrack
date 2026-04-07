package com.project.myscale.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.myscale.BodyTrackApplication
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.ui.screens.chart.TimeRange
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HistoryUiState(
    val timeRange: TimeRange = TimeRange.ALL,
    val displayMode: InputMode = InputMode.KG,
    val availableTypes: List<MeasurementType> = emptyList(),
    val activeTypes: Set<MeasurementType> = emptySet(),
    val allEntries: List<BodyEntry> = emptyList(),
    val filteredEntries: List<BodyEntry> = emptyList(),
    val showDeleteDialog: Boolean = false,
    val entryToDelete: BodyEntry? = null
)

sealed class HistoryEvent {
    data class EntryDeleted(val entry: BodyEntry) : HistoryEvent()
    data class Error(val message: String) : HistoryEvent()
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodyTrackApplication
    private val repository = app.repository

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events = _events.asSharedFlow()

    private var lastDeletedEntry: BodyEntry? = null

    init {
        viewModelScope.launch {
            combine(
                repository.getAllEntriesFlow(),
                repository.getDistinctMeasurementTypes()
            ) { entries, types ->
                Pair(entries, types)
            }.collect { (entries, types) ->
                val currentActive = _uiState.value.activeTypes
                val newActive = if (currentActive.isEmpty()) types.toSet() else {
                    currentActive.intersect(types.toSet()).ifEmpty { types.toSet() }
                }
                _uiState.value = _uiState.value.copy(
                    allEntries = entries,
                    availableTypes = types,
                    activeTypes = newActive
                )
                recalculate()
            }
        }
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(timeRange = range)
        recalculate()
    }

    fun setDisplayMode(mode: InputMode) {
        _uiState.value = _uiState.value.copy(displayMode = mode)
    }

    fun toggleType(type: MeasurementType) {
        val current = _uiState.value.activeTypes
        if (current.contains(type) && current.size <= 1) return
        val newActive = if (current.contains(type)) current - type else current + type
        _uiState.value = _uiState.value.copy(activeTypes = newActive)
    }

    fun showDeleteConfirmation(entry: BodyEntry) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true, entryToDelete = entry)
    }

    fun dismissDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false, entryToDelete = null)
    }

    fun confirmDelete() {
        val entry = _uiState.value.entryToDelete ?: return
        lastDeletedEntry = entry
        viewModelScope.launch {
            try {
                repository.deleteEntry(entry.id)
                _uiState.value = _uiState.value.copy(showDeleteDialog = false, entryToDelete = null)
                _events.emit(HistoryEvent.EntryDeleted(entry))
            } catch (e: Exception) {
                _events.emit(HistoryEvent.Error("Fehler beim Löschen: ${e.message}"))
            }
        }
    }

    fun undoDelete() {
        val entry = lastDeletedEntry ?: return
        viewModelScope.launch {
            try {
                repository.restoreEntry(entry)
                lastDeletedEntry = null
            } catch (e: Exception) {
                _events.emit(HistoryEvent.Error("Fehler beim Wiederherstellen: ${e.message}"))
            }
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val startDate = state.timeRange.getStartDate()
        val filtered = if (startDate != null) {
            state.allEntries.filter { !it.date.isBefore(startDate) }
        } else {
            state.allEntries
        }
        _uiState.value = state.copy(filteredEntries = filtered)
    }
}
