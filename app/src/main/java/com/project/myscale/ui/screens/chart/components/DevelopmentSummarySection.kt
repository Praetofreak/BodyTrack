package com.project.myscale.ui.screens.chart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.myscale.R
import com.project.myscale.data.model.DevelopmentSummary
import com.project.myscale.data.model.Trend
import com.project.myscale.ui.screens.chart.TimeRange
import com.project.myscale.ui.theme.TrendNegative
import com.project.myscale.ui.theme.TrendNeutral
import com.project.myscale.ui.theme.TrendPositive
import com.project.myscale.util.Formatters

@Composable
fun DevelopmentSummarySection(
    summaries: List<DevelopmentSummary>,
    timeRange: TimeRange,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        val timeRangeLabel = stringResource(
            when (timeRange) {
                TimeRange.ONE_WEEK -> R.string.range_one_week
                TimeRange.ONE_MONTH -> R.string.range_one_month
                TimeRange.THREE_MONTHS -> R.string.range_three_months
                TimeRange.SIX_MONTHS -> R.string.range_six_months
                TimeRange.ONE_YEAR -> R.string.range_one_year
                TimeRange.ALL -> R.string.range_total
            }
        )

        Text(
            text = stringResource(R.string.development_title, timeRangeLabel),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        for (summary in summaries) {
            DevelopmentRow(
                summary = summary,
                isDarkTheme = isDarkTheme
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DevelopmentRow(
    summary: DevelopmentSummary,
    isDarkTheme: Boolean
) {
    val chartColor = summary.type.chartColor(isDarkTheme)
    val trendColor = when (summary.trend) {
        Trend.POSITIVE -> TrendPositive
        Trend.NEGATIVE -> TrendNegative
        Trend.NEUTRAL -> TrendNeutral
    }
    val trendIcon = when {
        summary.hasSingleDataPoint -> Icons.AutoMirrored.Rounded.ArrowForward
        summary.absoluteChange < 0 -> Icons.Rounded.ArrowDownward
        summary.absoluteChange > 0 -> Icons.Rounded.ArrowUpward
        else -> Icons.AutoMirrored.Rounded.ArrowForward
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(chartColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(summary.type.labelRes),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (summary.hasSingleDataPoint) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.valueWithUnit(summary.endValue, summary.unit),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.development_single_value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${Formatters.valueWithUnit(summary.startValue, summary.unit)} → ${Formatters.valueWithUnit(summary.endValue, summary.unit)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${Formatters.signedWithUnit(summary.absoluteChange, summary.unit)} (${Formatters.signedPercent(summary.percentChange)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = trendColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
