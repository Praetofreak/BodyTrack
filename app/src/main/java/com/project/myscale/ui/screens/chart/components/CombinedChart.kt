package com.project.myscale.ui.screens.chart.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CombinedChart(
    entries: List<BodyEntry>,
    activeTypes: Set<MeasurementType>,
    displayMode: InputMode,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty() || activeTypes.isEmpty()) return

    val sortedEntries = remember(entries) { entries.sortedBy { it.date } }

    val typesWithData = remember(sortedEntries, activeTypes, displayMode) {
        activeTypes.filter { type ->
            if (displayMode == InputMode.PERCENT && !type.supportsPercent) return@filter false
            sortedEntries.any { entry ->
                val value = entry.measurements[type]
                value != null && getDisplayValue(value, type, displayMode) != null
            }
        }.sortedBy { it.sortOrder }
    }

    if (typesWithData.isEmpty()) return

    // Wrap the chart in a key block so the entire Vico chart subtree is fully
    // recreated whenever the set of visible types or the entry set changes.
    // This avoids any inconsistency between the chart's line configuration and
    // the underlying CartesianChartModelProducer when the user switches time
    // ranges (which previously caused IndexOutOfBoundsException crashes).
    key(typesWithData, sortedEntries, displayMode, isDarkTheme) {
        CombinedChartContent(
            sortedEntries = sortedEntries,
            typesWithData = typesWithData,
            displayMode = displayMode,
            isDarkTheme = isDarkTheme,
            modifier = modifier
        )
    }
}

@Composable
private fun CombinedChartContent(
    sortedEntries: List<BodyEntry>,
    typesWithData: List<MeasurementType>,
    displayMode: InputMode,
    isDarkTheme: Boolean,
    modifier: Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    val lineColors = remember(typesWithData, isDarkTheme) {
        typesWithData.map { it.chartColor(isDarkTheme) }
    }

    val dateLabels = remember(sortedEntries) {
        val formatter = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)
        sortedEntries.mapIndexed { index, entry -> index to entry.date.format(formatter) }.toMap()
    }

    LaunchedEffect(Unit) {
        if (typesWithData.isEmpty() || sortedEntries.isEmpty()) return@LaunchedEffect

        modelProducer.runTransaction {
            lineSeries {
                for (type in typesWithData) {
                    val values = sortedEntries.map { entry ->
                        val mv = entry.measurements[type]
                        if (mv != null) {
                            getDisplayValue(mv, type, displayMode) ?: 0.0
                        } else {
                            0.0
                        }
                    }
                    series(values)
                }
            }
        }
    }

    val lines = lineColors.map { color ->
        LineCartesianLayer.rememberLine(
            fill = remember(color) { LineCartesianLayer.LineFill.single(fill(color)) }
        )
    }

    val bottomFormatter = CartesianValueFormatter { _, x, _ ->
        dateLabels[x.toInt()] ?: ""
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(lines)
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomFormatter
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 4.dp)
    )
}

private fun getDisplayValue(
    mv: com.project.myscale.data.model.MeasurementValue,
    type: MeasurementType,
    displayMode: InputMode
): Double? {
    return when {
        displayMode == InputMode.PERCENT && type.supportsPercent -> mv.valuePercent
        else -> mv.valueKg
    }
}
