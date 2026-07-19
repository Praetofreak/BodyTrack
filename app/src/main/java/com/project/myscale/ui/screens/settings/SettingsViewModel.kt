package com.project.myscale.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.myscale.BodyTrackApplication
import com.project.myscale.backup.BackupWorker
import com.project.myscale.data.model.BackupInterval
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.ThemeOption
import com.project.myscale.data.repository.BodyTrackRepository
import com.project.myscale.util.CsvExporter
import com.project.myscale.util.CsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Parsed CSV waiting for the user to pick an import strategy. */
data class PendingImport(
    val entries: List<BodyEntry>,
    val newCount: Int,
    val conflictCount: Int,
    val errors: List<CsvImporter.RowError>
)

data class SettingsUiState(
    val enabledFields: Set<String> = setOf("WEIGHT"),
    val defaultInputMode: InputMode = InputMode.PERCENT,
    val selectedTheme: ThemeOption = ThemeOption.FOREST,
    val backupFolderUri: String? = null,
    val backupInterval: BackupInterval = BackupInterval.OFF,
    val pendingImport: PendingImport? = null
)

sealed class SettingsEvent {
    data object ExportSuccess : SettingsEvent()
    data class ExportFailed(val detail: String?) : SettingsEvent()
    data class ImportFinished(val stats: BodyTrackRepository.ImportStats, val errorCount: Int) : SettingsEvent()
    data class ImportFailed(val detail: String?) : SettingsEvent()
    data class BackupFinished(val result: BackupWorker.BackupResult) : SettingsEvent()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodyTrackApplication
    private val preferencesManager = app.preferencesManager
    private val repository = app.repository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

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
        viewModelScope.launch {
            preferencesManager.backupFolderUri.collect { uri ->
                _uiState.value = _uiState.value.copy(backupFolderUri = uri)
            }
        }
        viewModelScope.launch {
            preferencesManager.backupInterval.collect { interval ->
                _uiState.value = _uiState.value.copy(backupInterval = interval)
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

    // --- CSV export/import ---

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val entries = repository.getAllEntriesForExport()
                    app.contentResolver.openOutputStream(uri)?.use { os ->
                        CsvExporter.export(entries, os)
                    } ?: throw IllegalStateException("stream unavailable")
                }
                _events.emit(SettingsEvent.ExportSuccess)
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ExportFailed(e.message))
            }
        }
    }

    fun startImport(uri: Uri) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { CsvImporter.parse(it) }
                        ?: throw IllegalStateException("stream unavailable")
                }
                if (!result.isValid) {
                    _events.emit(SettingsEvent.ImportFailed(null))
                    return@launch
                }
                val existingDates = withContext(Dispatchers.IO) { repository.getExistingDates() }
                val conflictCount = result.entries.count { it.date in existingDates }
                _uiState.value = _uiState.value.copy(
                    pendingImport = PendingImport(
                        entries = result.entries,
                        newCount = result.entries.size - conflictCount,
                        conflictCount = conflictCount,
                        errors = result.errors
                    )
                )
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ImportFailed(e.message))
            }
        }
    }

    fun confirmImport(overwriteExisting: Boolean) {
        val pending = _uiState.value.pendingImport ?: return
        _uiState.value = _uiState.value.copy(pendingImport = null)
        viewModelScope.launch {
            try {
                val stats = withContext(Dispatchers.IO) {
                    repository.importEntries(pending.entries, overwriteExisting)
                }
                _events.emit(SettingsEvent.ImportFinished(stats, pending.errors.size))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ImportFailed(e.message))
            }
        }
    }

    fun cancelImport() {
        _uiState.value = _uiState.value.copy(pendingImport = null)
    }

    // --- Local backup ---

    fun setBackupFolder(uri: Uri) {
        app.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModelScope.launch {
            preferencesManager.setBackupFolderUri(uri.toString())
        }
    }

    fun setBackupInterval(interval: BackupInterval) {
        viewModelScope.launch {
            preferencesManager.setBackupInterval(interval)
            BackupWorker.schedule(app, interval)
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            val result = BackupWorker.runBackup(app)
            _events.emit(SettingsEvent.BackupFinished(result))
        }
    }
}
