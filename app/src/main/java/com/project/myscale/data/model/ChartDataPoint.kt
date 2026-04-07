package com.project.myscale.data.model

import java.time.LocalDate

data class ChartDataPoint(
    val date: LocalDate,
    val value: Double,
    val type: MeasurementType
)
