package com.project.myscale.data.model

import java.time.LocalDate

data class BodyEntry(
    val id: Long = 0,
    val date: LocalDate,
    val measurements: Map<MeasurementType, MeasurementValue>
)
