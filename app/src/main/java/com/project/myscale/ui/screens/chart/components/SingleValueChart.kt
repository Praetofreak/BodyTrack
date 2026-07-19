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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import com.project.myscale.util.DateUtils

@Composable
fun SingleValueChart(
    type: MeasurementType,
    entries: List<BodyEntry>,
    displayMode: InputMode,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val chartColor = type.chartColor(isDarkTheme)

    val dataPoints = remember(entries, type, displayMode) {
        entries.sortedBy { it.date }.mapNotNull { entry ->
            val value = entry.measurements[type] ?: return@mapNotNull null
            val displayValue = when {
                displayMode == InputMode.PERCENT && type.supportsPercent -> value.valuePercent
                else -> value.valueKg
            } ?: return@mapNotNull null
            DateUtils.localDateToEpochDay(entry.date) to displayValue
        }
    }

    if (dataPoints.isEmpty()) return

    // Recreate the Vico subtree when the data changes so the model producer can
    // never hold data inconsistent with the chart configuration (crash fix).
    key(dataPoints, type, isDarkTheme) {
        SingleValueChartContent(
            type = type,
            chartColor = chartColor,
            dataPoints = dataPoints,
            modifier = modifier
        )
    }
}

@Composable
private fun SingleValueChartContent(
    type: MeasurementType,
    chartColor: Color,
    dataPoints: List<Pair<Long, Double>>,
    modifier: Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries {
                series(x = dataPoints.map { it.first }, y = dataPoints.map { it.second })
            }
        }
    }

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
                    text = stringResource(type.labelRes),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val bottomFormatter = remember {
                CartesianValueFormatter { _, x, _ ->
                    DateUtils.formatChartDate(DateUtils.epochDayToLocalDate(x.toLong()))
                }
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
