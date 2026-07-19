package com.project.myscale.util

import com.project.myscale.data.local.database.entity.EntryWithValues
import com.project.myscale.data.local.database.entity.MeasurementValueEntity
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue

object Converters {

    fun toBodyEntry(entryWithValues: EntryWithValues): BodyEntry {
        val measurements = mutableMapOf<MeasurementType, MeasurementValue>()
        for (ve in entryWithValues.values) {
            val type = try {
                MeasurementType.valueOf(ve.type)
            } catch (_: IllegalArgumentException) {
                continue
            }
            val mode = try {
                InputMode.valueOf(ve.inputMode)
            } catch (_: IllegalArgumentException) {
                InputMode.KG
            }
            measurements[type] = MeasurementValue(
                valueKg = ve.valueKg,
                valuePercent = ve.valuePercent,
                inputMode = mode
            )
        }
        return BodyEntry(
            id = entryWithValues.entry.id,
            date = DateUtils.epochDayToLocalDate(entryWithValues.entry.date),
            measurements = measurements
        )
    }

    fun toMeasurementValueEntities(
        entryId: Long,
        measurements: Map<MeasurementType, MeasurementValue>
    ): List<MeasurementValueEntity> {
        return measurements.map { (type, value) ->
            MeasurementValueEntity(
                id = 0,
                entryId = entryId,
                type = type.name,
                valueKg = value.valueKg,
                valuePercent = value.valuePercent,
                inputMode = value.inputMode.name
            )
        }
    }
}
