package com.project.myscale.data.model

data class DevelopmentSummary(
    val type: MeasurementType,
    val startValue: Double,
    val endValue: Double,
    val unit: String,
    val absoluteChange: Double,
    val percentChange: Double,
    val trend: Trend,
    val hasSingleDataPoint: Boolean = false
)

enum class Trend {
    POSITIVE, NEGATIVE, NEUTRAL
}
