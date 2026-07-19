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
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.util.DateUtils

/** One line per measurement type; only dates that actually have a value become points. */
private data class ChartSeries(
    val type: MeasurementType,
    val x: List<Long>,
    val y: List<Double>
)

@Composable
fun CombinedChart(
    entries: List<BodyEntry>,
    activeTypes: Set<MeasurementType>,
    displayMode: InputMode,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty() || activeTypes.isEmpty()) return

    val seriesList = remember(entries, activeTypes, displayMode) {
        val sorted = entries.sortedBy { it.date }
        activeTypes
            .filter { type -> !(displayMode == InputMode.PERCENT && !type.supportsPercent) }
            .sortedBy { it.sortOrder }
            .mapNotNull { type ->
                val points = sorted.mapNotNull { entry ->
                    val value = entry.measurements[type] ?: return@mapNotNull null
                    val displayValue = getDisplayValue(value, type, displayMode)
                        ?: return@mapNotNull null
                    DateUtils.localDateToEpochDay(entry.date) to displayValue
                }
                if (points.isEmpty()) null
                else ChartSeries(type, points.map { it.first }, points.map { it.second })
            }
    }

    if (seriesList.isEmpty()) return

    // Recreate the whole Vico subtree when the visible series change so the line
    // configuration can never run ahead of the model producer's data (the
    // mismatch used to crash with IndexOutOfBoundsException on range switches).
    key(seriesList, isDarkTheme) {
        CombinedChartContent(
            seriesList = seriesList,
            isDarkTheme = isDarkTheme,
            modifier = modifier
        )
    }
}

@Composable
private fun CombinedChartContent(
    seriesList: List<ChartSeries>,
    isDarkTheme: Boolean,
    modifier: Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries {
                for (s in seriesList) {
                    series(x = s.x, y = s.y)
                }
            }
        }
    }

    val lines = seriesList.map { s ->
        val color = s.type.chartColor(isDarkTheme)
        LineCartesianLayer.rememberLine(
            fill = remember(color) { LineCartesianLayer.LineFill.single(fill(color)) }
        )
    }

    val bottomFormatter = remember {
        CartesianValueFormatter { _, x, _ ->
            DateUtils.formatChartDate(DateUtils.epochDayToLocalDate(x.toLong()))
        }
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
    mv: MeasurementValue,
    type: MeasurementType,
    displayMode: InputMode
): Double? {
    return when {
        displayMode == InputMode.PERCENT && type.supportsPercent -> mv.valuePercent
        else -> mv.valueKg
    }
}
