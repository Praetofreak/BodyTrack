package com.project.myscale.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.myscale.BuildConfig
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.ThemeOption
import com.project.myscale.ui.theme.*
import com.project.myscale.util.CsvExporter
import com.project.myscale.util.CsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onRestartOnboarding: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val entries = withContext(Dispatchers.IO) {
                        viewModel.repository.getAllEntriesForExport()
                    }
                    context.contentResolver.openOutputStream(it)?.let { os ->
                        CsvExporter.export(entries, os)
                    }
                    snackbarHostState.showSnackbar("Daten exportiert")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export fehlgeschlagen: ${e.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    if (inputStream != null) {
                        val result = CsvImporter.parse(inputStream)
                        if (result.isValid) {
                            withContext(Dispatchers.IO) {
                                for (entry in result.entries) {
                                    viewModel.repository.saveEntry(entry)
                                }
                            }
                            snackbarHostState.showSnackbar("${result.entries.size} Einträge importiert")
                        } else {
                            snackbarHostState.showSnackbar("Import fehlgeschlagen: ${result.errors.firstOrNull()}")
                        }
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import fehlgeschlagen: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
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
            // Section: Input Fields
            Text(
                "Eingabefelder",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Wähle aus, welche Messwerte du erfassen möchtest. Gewicht ist immer aktiv.",
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
                            Text(type.labelDe, style = MaterialTheme.typography.bodyLarge)
                            if (type.isPrimary) {
                                Text(
                                    "Pflichtfeld",
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
                "Standard-Einheit für neue Eingaben",
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
                    Text("Prozent (%)")
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
                    Text("Kilogramm (kg)")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Theme
            Text(
                "Erscheinungsbild",
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

            // Section: Data
            Text(
                "Daten",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { exportLauncher.launch("bodytrack_export.csv") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Daten exportieren (CSV)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("text/*", "text/csv", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Daten importieren (CSV)")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Info
            Text(
                "Info",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = {
                viewModel.setOnboardingCompleted(false)
                onRestartOnboarding()
            }) {
                Text("Einführung erneut anzeigen")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
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
                text = theme.labelDe,
                style = MaterialTheme.typography.labelLarge
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Ausgewählt",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
