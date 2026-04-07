package com.project.myscale.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.ThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesManager(private val context: Context) {

    private object Keys {
        val ENABLED_INPUT_FIELDS = stringSetPreferencesKey("enabled_input_fields")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val DEFAULT_INPUT_MODE = stringPreferencesKey("default_input_mode")
        val CHART_DISPLAY_MODE = stringPreferencesKey("chart_display_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val enabledInputFields: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENABLED_INPUT_FIELDS] ?: setOf(MeasurementType.WEIGHT.name)
    }

    val selectedTheme: Flow<ThemeOption> = context.dataStore.data.map { prefs ->
        val themeName = prefs[Keys.SELECTED_THEME] ?: ThemeOption.FOREST.name
        try {
            ThemeOption.valueOf(themeName)
        } catch (_: Exception) {
            ThemeOption.FOREST
        }
    }

    val defaultInputMode: Flow<InputMode> = context.dataStore.data.map { prefs ->
        val modeName = prefs[Keys.DEFAULT_INPUT_MODE] ?: InputMode.PERCENT.name
        try {
            InputMode.valueOf(modeName)
        } catch (_: Exception) {
            InputMode.PERCENT
        }
    }

    val chartDisplayMode: Flow<InputMode> = context.dataStore.data.map { prefs ->
        val modeName = prefs[Keys.CHART_DISPLAY_MODE] ?: InputMode.KG.name
        try {
            InputMode.valueOf(modeName)
        } catch (_: Exception) {
            InputMode.KG
        }
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setEnabledInputFields(fields: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLED_INPUT_FIELDS] = fields
        }
    }

    suspend fun setSelectedTheme(theme: ThemeOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_THEME] = theme.name
        }
    }

    suspend fun setDefaultInputMode(mode: InputMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_INPUT_MODE] = mode.name
        }
    }

    suspend fun setChartDisplayMode(mode: InputMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CHART_DISPLAY_MODE] = mode.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }
}
