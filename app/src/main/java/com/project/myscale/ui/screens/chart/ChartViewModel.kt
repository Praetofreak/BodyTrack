package com.project.myscale.ui.screens.chart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.myscale.BodyTrackApplication
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.DevelopmentSummary
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.Trend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TimeRange(val label: String, val months: Int?) {
    ONE_WEEK("1W", null),
    ONE_MONTH("1M", 1),
    THREE_MONTHS("3M", 3),
    SIX_MONTHS("6M", 6),
    ONE_YEAR("1J", 12),
    ALL("Alles", null);

    fun getStartDate(): LocalDate? {
        return when (this) {
            ONE_WEEK -> LocalDate.now().minusWeeks(1)
            ONE_MONTH -> LocalDate.now().minusMonths(1)
            THREE_MONTHS -> LocalDate.now().minusMonths(3)
            SIX_MONTHS -> LocalDate.now().minusMonths(6)
            ONE_YEAR -> LocalDate.now().minusYears(1)
            ALL -> null
        }
    }
}

enum class ChartViewMode {
    COMBINED, INDIVIDUAL
}

data class ChartUiState(
    val viewMode: ChartViewMode = ChartViewMode.COMBINED,
    val displayMode: InputMode = InputMode.KG,
    val timeRange: TimeRange = TimeRange.THREE_MONTHS,
    val availableTypes: List<MeasurementType> = emptyList(),
    val activeTypes: Set<MeasurementType> = emptySet(),
    val allEntries: List<BodyEntry> = emptyList(),
    val filteredEntries: List<BodyEntry> = emptyList(),
    val developmentSummaries: List<DevelopmentSummary> = emptyList()
)

class ChartViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodyTrackApplication
    private val repository = app.repository
    private val preferencesManager = app.preferencesManager

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.chartDisplayMode.collect { mode ->
                _uiState.value = _uiState.value.copy(displayMode = mode)
                recalculate()
            }
        }

        viewModelScope.launch {
            combine(
                repository.getAllEntriesAscFlow(),
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

    fun setViewMode(mode: ChartViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setDisplayMode(mode: InputMode) {
        viewModelScope.launch {
            preferencesManager.setChartDisplayMode(mode)
        }
        _uiState.value = _uiState.value.copy(displayMode = mode)
        recalculate()
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(timeRange = range)
        recalculate()
    }

    fun toggleType(type: MeasurementType) {
        val current = _uiState.value.activeTypes
        if (current.contains(type) && current.size <= 1) return // Keep at least one
        val newActive = if (current.contains(type)) current - type else current + type
        _uiState.value = _uiState.value.copy(activeTypes = newActive)
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val startDate = state.timeRange.getStartDate()
        val filtered = if (startDate != null) {
            state.allEntries.filter { !it.date.isBefore(startDate) }
        } else {
            state.allEntries
        }

        val summaries = calculateDevelopmentSummaries(filtered, state.activeTypes, state.displayMode)

        _uiState.value = state.copy(
            filteredEntries = filtered,
            developmentSummaries = summaries
        )
    }

    private fun calculateDevelopmentSummaries(
        entries: List<BodyEntry>,
        activeTypes: Set<MeasurementType>,
        displayMode: InputMode
    ): List<DevelopmentSummary> {
        val summaries = mutableListOf<DevelopmentSummary>()

        for (type in activeTypes.sortedBy { it.sortOrder }) {
            val dataPoints = entries.mapNotNull { entry ->
                val value = entry.measurements[type] ?: return@mapNotNull null
                val displayValue = when {
                    displayMode == InputMode.PERCENT && type.supportsPercent -> value.valuePercent
                    else -> value.valueKg
                } ?: return@mapNotNull null
                displayValue
            }

            if (dataPoints.isEmpty()) continue

            val unit = when {
                displayMode == InputMode.PERCENT && type.supportsPercent -> "%"
                else -> "kg"
            }

            if (dataPoints.size == 1) {
                summaries.add(
                    DevelopmentSummary(
                        type = type,
                        startValue = dataPoints.first(),
                        endValue = dataPoints.first(),
                        unit = unit,
                        absoluteChange = 0.0,
                        percentChange = 0.0,
                        trend = Trend.NEUTRAL,
                        hasSingleDataPoint = true
                    )
                )
                continue
            }

            val startValue = dataPoints.first()
            val endValue = dataPoints.last()
            val absoluteChange = endValue - startValue
            val percentChange = if (startValue != 0.0) (absoluteChange / startValue) * 100.0 else 0.0

            val trend = when {
                kotlin.math.abs(percentChange) < 0.1 -> Trend.NEUTRAL
                absoluteChange < 0 -> {
                    if (type.decreaseIsPositive) Trend.POSITIVE else Trend.NEGATIVE
                }
                else -> {
                    if (type.decreaseIsPositive) Trend.NEGATIVE else Trend.POSITIVE
                }
            }

            summaries.add(
                DevelopmentSummary(
                    type = type,
                    startValue = startValue,
                    endValue = endValue,
                    unit = unit,
                    absoluteChange = absoluteChange,
                    percentChange = percentChange,
                    trend = trend,
                    hasSingleDataPoint = false
                )
            )
        }

        return summaries
    }
}
