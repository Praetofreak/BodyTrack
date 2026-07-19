package com.project.myscale.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.myscale.BuildConfig
import com.project.myscale.R
import com.project.myscale.backup.BackupWorker
import com.project.myscale.data.model.BackupInterval
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.ThemeOption
import com.project.myscale.ui.components.messageRes
import com.project.myscale.ui.theme.*

private const val LANGUAGE_SYSTEM = ""

private val languageOptions = listOf(
    LANGUAGE_SYSTEM to R.string.language_system,
    "en" to R.string.language_english,
    "de" to R.string.language_german,
    "fr" to R.string.language_french
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onRestartOnboarding: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is SettingsEvent.ExportSuccess ->
                    context.getString(R.string.export_success)
                is SettingsEvent.ExportFailed ->
                    context.getString(R.string.export_failed, event.detail ?: "")
                is SettingsEvent.ImportFailed ->
                    context.getString(R.string.import_failed, event.detail ?: "")
                is SettingsEvent.ImportFinished ->
                    context.getString(
                        R.string.import_finished,
                        event.stats.inserted,
                        event.stats.overwritten,
                        event.stats.skipped
                    ) + if (event.errorCount > 0) {
                        " " + context.getString(R.string.import_finished_errors, event.errorCount)
                    } else ""
                is SettingsEvent.BackupFinished -> when (event.result) {
                    BackupWorker.BackupResult.SUCCESS ->
                        context.getString(R.string.backup_success)
                    BackupWorker.BackupResult.NOT_CONFIGURED ->
                        context.getString(R.string.backup_not_configured)
                    BackupWorker.BackupResult.FOLDER_INACCESSIBLE ->
                        context.getString(R.string.backup_folder_inaccessible)
                    BackupWorker.BackupResult.WRITE_FAILED ->
                        context.getString(R.string.backup_failed)
                }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportTo(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.startImport(it) }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setBackupFolder(it) }
    }

    uiState.pendingImport?.let { pending ->
        ImportDialog(
            pending = pending,
            onConfirm = { overwrite -> viewModel.confirmImport(overwrite) },
            onDismiss = { viewModel.cancelImport() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            // Section: Input Fields
            Text(
                stringResource(R.string.settings_input_fields),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.settings_input_fields_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            MeasurementType.entries.sortedBy { it.sortOrder }.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(type.chartColor(uiState.selectedTheme.isDark))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(type.labelRes), style = MaterialTheme.typography.bodyLarge)
                            if (type.isPrimary) {
                                Text(
                                    stringResource(R.string.settings_required_field),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Switch(
                        checked = type.isPrimary || uiState.enabledFields.contains(type.name),
                        onCheckedChange = {
                            if (!type.isPrimary) viewModel.toggleField(type)
                        },
                        enabled = !type.isPrimary
                    )
                }
                if (type != MeasurementType.entries.last()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Default input unit
            Text(
                stringResource(R.string.settings_default_unit),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                val leftShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                val rightShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)

                OutlinedButton(
                    onClick = { viewModel.setDefaultInputMode(InputMode.PERCENT) },
                    shape = leftShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.defaultInputMode == InputMode.PERCENT)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (uiState.defaultInputMode == InputMode.PERCENT)
                            MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(stringResource(R.string.unit_percent_full))
                }
                OutlinedButton(
                    onClick = { viewModel.setDefaultInputMode(InputMode.KG) },
                    shape = rightShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.defaultInputMode == InputMode.KG)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (uiState.defaultInputMode == InputMode.KG)
                            MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(stringResource(R.string.unit_kg_full))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Theme
            Text(
                stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeOption.entries.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = uiState.selectedTheme == theme,
                        onClick = { viewModel.setTheme(theme) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Language
            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            val currentLanguageTag = AppCompatDelegate.getApplicationLocales()
                .toLanguageTags().substringBefore('-')
            languageOptions.forEach { (tag, labelRes) ->
                val selected = currentLanguageTag == tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, onClick = {
                            AppCompatDelegate.setApplicationLocales(
                                if (tag == LANGUAGE_SYSTEM) LocaleListCompat.getEmptyLocaleList()
                                else LocaleListCompat.forLanguageTags(tag)
                            )
                        })
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(
                                if (tag == LANGUAGE_SYSTEM) LocaleListCompat.getEmptyLocaleList()
                                else LocaleListCompat.forLanguageTags(tag)
                            )
                        }
                    )
                    Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Data
            Text(
                stringResource(R.string.settings_data),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { exportLauncher.launch("dailyscale_export.csv") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_export_csv))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("text/*", "text/csv", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_import_csv))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Local backup
            Text(
                stringResource(R.string.settings_backup),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.settings_backup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { backupFolderLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.backup_choose_folder))
            }
            Text(
                text = uiState.backupFolderUri
                    ?.let { readableFolderName(it) }
                    ?.let { stringResource(R.string.backup_folder_current, it) }
                    ?: stringResource(R.string.backup_folder_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.backup_interval_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackupInterval.entries.forEach { interval ->
                    FilterChip(
                        selected = uiState.backupInterval == interval,
                        onClick = { viewModel.setBackupInterval(interval) },
                        label = { Text(stringResource(interval.labelRes())) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.backupNow() },
                enabled = uiState.backupFolderUri != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.backup_now))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Info
            Text(
                stringResource(R.string.settings_info),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = {
                viewModel.setOnboardingCompleted(false)
                onRestartOnboarding()
            }) {
                Text(stringResource(R.string.settings_show_onboarding))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ImportDialog(
    pending: PendingImport,
    onConfirm: (overwriteExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var overwrite by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(
                        R.string.import_dialog_summary,
                        pending.newCount,
                        pending.conflictCount
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (pending.conflictCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.import_conflict_question),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = !overwrite, onClick = { overwrite = false })
                    ) {
                        RadioButton(selected = !overwrite, onClick = { overwrite = false })
                        Text(stringResource(R.string.import_keep_existing))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = overwrite, onClick = { overwrite = true })
                    ) {
                        RadioButton(selected = overwrite, onClick = { overwrite = true })
                        Text(stringResource(R.string.import_overwrite))
                    }
                }
                if (pending.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.import_errors_title, pending.errors.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    for (error in pending.errors) {
                        Text(
                            stringResource(
                                R.string.import_error_line,
                                error.lineNumber,
                                stringResource(error.reason.messageRes())
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(overwrite) }) {
                Text(stringResource(R.string.action_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun BackupInterval.labelRes(): Int = when (this) {
    BackupInterval.OFF -> R.string.backup_interval_off
    BackupInterval.DAILY -> R.string.backup_interval_daily
    BackupInterval.WEEKLY -> R.string.backup_interval_weekly
    BackupInterval.MONTHLY -> R.string.backup_interval_monthly
}

/** "content://…/tree/primary%3ABackups" -> "primary:Backups" */
private fun readableFolderName(uriString: String): String {
    return try {
        android.net.Uri.parse(uriString).lastPathSegment ?: uriString
    } catch (_: Exception) {
        uriString
    }
}

@Composable
private fun ThemeCard(
    theme: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = when (theme) {
        ThemeOption.FOREST -> listOf(ForestPrimary, ForestSecondary, ForestBackground, ForestTertiary)
        ThemeOption.MIDNIGHT -> listOf(MidnightPrimary, MidnightSecondary, MidnightBackground, MidnightTertiary)
        ThemeOption.SUNSET -> listOf(SunsetPrimary, SunsetSecondary, SunsetBackground, SunsetTertiary)
    }

    Card(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
            Text(
                text = theme.label,
                style = MaterialTheme.typography.labelLarge
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.settings_theme_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
