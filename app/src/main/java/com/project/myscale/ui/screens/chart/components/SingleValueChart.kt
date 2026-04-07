package com.project.myscale.ui.screens.chart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun SingleValueChart(
    type: MeasurementType,
    entries: List<BodyEntry>,
    displayMode: InputMode,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val sortedEntries = remember(entries) { entries.sortedBy { it.date } }
    val chartColor = type.chartColor(isDarkTheme)
    val modelProducer = remember { CartesianChartModelProducer() }

    val dataPoints = remember(sortedEntries, type, displayMode) {
        sortedEntries.mapNotNull { entry ->
            val value = entry.measurements[type] ?: return@mapNotNull null
            val displayValue = when {
                displayMode == InputMode.PERCENT && type.supportsPercent -> value.valuePercent
                else -> value.valueKg
            } ?: return@mapNotNull null
            entry.date to displayValue
        }
    }

    val dateLabels = remember(dataPoints) {
        val formatter = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)
        dataPoints.mapIndexed { index, (date, _) -> index to date.format(formatter) }.toMap()
    }

    LaunchedEffect(dataPoints) {
        if (dataPoints.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                series(dataPoints.map { it.second })
            }
        }
    }

    if (dataPoints.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(chartColor)
                )
                Text(
                    text = type.labelDe,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val bottomFormatter = CartesianValueFormatter { _, x, _ ->
                dateLabels[x.toInt()] ?: ""
            }

            val line = LineCartesianLayer.rememberLine(
                fill = remember(chartColor) { LineCartesianLayer.LineFill.single(fill(chartColor)) }
            )

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(line)
                    ),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomFormatter
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 8.dp)
            )
        }
    }
}
