package com.project.myscale.util

import com.project.myscale.data.local.database.entity.EntryEntity
import com.project.myscale.data.local.database.entity.MeasurementValueEntity
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue

object Converters {

    fun toBodyEntry(
        entity: EntryEntity,
        valueEntities: List<MeasurementValueEntity>
    ): BodyEntry {
        val measurements = mutableMapOf<MeasurementType, MeasurementValue>()
        for (ve in valueEntities) {
            val type = try {
                MeasurementType.valueOf(ve.type)
            } catch (_: Exception) {
                continue
            }
            val mode = try {
                InputMode.valueOf(ve.inputMode)
            } catch (_: Exception) {
                InputMode.KG
            }
            measurements[type] = MeasurementValue(
                valueKg = ve.valueKg,
                valuePercent = ve.valuePercent,
                inputMode = mode
            )
        }
        return BodyEntry(
            id = entity.id,
            date = DateUtils.epochMilliToLocalDate(entity.date),
            measurements = measurements
        )
    }

    fun toEntryEntity(entry: BodyEntry, existingId: Long? = null): EntryEntity {
        val now = DateUtils.now()
        return EntryEntity(
            id = existingId ?: entry.id,
            date = DateUtils.localDateToEpochMilli(entry.date),
            createdAt = if (existingId != null) now else now,
            updatedAt = now
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
