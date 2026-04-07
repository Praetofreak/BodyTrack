package com.project.myscale.data.repository

import com.project.myscale.data.local.database.dao.EntryDao
import com.project.myscale.data.local.database.dao.MeasurementValueDao
import com.project.myscale.data.local.database.entity.EntryEntity
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.util.CalculationUtils
import com.project.myscale.util.Converters
import com.project.myscale.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class BodyTrackRepository(
    private val entryDao: EntryDao,
    private val measurementValueDao: MeasurementValueDao
) {

    fun getAllEntriesFlow(): Flow<List<BodyEntry>> {
        return entryDao.getAllEntriesFlow().map { entities ->
            mapEntitiesToBodyEntries(entities)
        }
    }

    fun getAllEntriesAscFlow(): Flow<List<BodyEntry>> {
        return entryDao.getAllEntriesAscFlow().map { entities ->
            mapEntitiesToBodyEntries(entities)
        }
    }

    fun getEntriesFromDate(startDate: LocalDate): Flow<List<BodyEntry>> {
        val startMillis = DateUtils.localDateToEpochMilli(startDate)
        return entryDao.getEntriesFromDate(startMillis).map { entities ->
            mapEntitiesToBodyEntries(entities)
        }
    }

    fun getDistinctMeasurementTypes(): Flow<List<MeasurementType>> {
        return measurementValueDao.getDistinctTypes().map { typeStrings ->
            typeStrings.mapNotNull { name ->
                try { MeasurementType.valueOf(name) } catch (_: Exception) { null }
            }.sortedBy { it.sortOrder }
        }
    }

    suspend fun getEntryByDate(date: LocalDate): BodyEntry? {
        val millis = DateUtils.localDateToEpochMilli(date)
        val entity = entryDao.getByDate(millis) ?: return null
        val values = measurementValueDao.getByEntryId(entity.id)
        return Converters.toBodyEntry(entity, values)
    }

    suspend fun getEntryById(id: Long): BodyEntry? {
        val entity = entryDao.getById(id) ?: return null
        val values = measurementValueDao.getByEntryId(entity.id)
        return Converters.toBodyEntry(entity, values)
    }

    suspend fun getLatestEntry(): BodyEntry? {
        val entity = entryDao.getLatestEntry() ?: return null
        val values = measurementValueDao.getByEntryId(entity.id)
        return Converters.toBodyEntry(entity, values)
    }

    suspend fun saveEntry(entry: BodyEntry): Long {
        val now = DateUtils.now()
        val dateMillis = DateUtils.localDateToEpochMilli(entry.date)

        // Check if entry for this date already exists
        val existing = entryDao.getByDate(dateMillis)

        val entryId: Long
        if (existing != null) {
            // Update existing entry
            entryDao.update(existing.copy(updatedAt = now))
            entryId = existing.id
            measurementValueDao.deleteByEntryId(entryId)
        } else {
            // Create new entry
            val newEntity = EntryEntity(
                date = dateMillis,
                createdAt = now,
                updatedAt = now
            )
            entryId = entryDao.insert(newEntity)
        }

        // Recalculate dependent values if weight changed
        val weightValue = entry.measurements[MeasurementType.WEIGHT]
        val weightKg = weightValue?.valueKg ?: 0.0

        val processedMeasurements = entry.measurements.map { (type, value) ->
            if (type == MeasurementType.WEIGHT) {
                type to value
            } else {
                type to CalculationUtils.recalculateOnWeightChange(value, type, weightKg)
            }
        }.toMap()

        val valueEntities = Converters.toMeasurementValueEntities(entryId, processedMeasurements)
        measurementValueDao.insertAll(valueEntities)

        return entryId
    }

    suspend fun updateEntry(entry: BodyEntry): Long {
        val now = DateUtils.now()
        val dateMillis = DateUtils.localDateToEpochMilli(entry.date)

        val existingEntity = entryDao.getById(entry.id)
        if (existingEntity != null) {
            // Check if date changed and if there's a conflict
            if (existingEntity.date != dateMillis) {
                val dateConflict = entryDao.getByDate(dateMillis)
                if (dateConflict != null && dateConflict.id != entry.id) {
                    // Delete the conflicting entry, we'll overwrite
                    entryDao.deleteById(dateConflict.id)
                }
            }
            entryDao.update(existingEntity.copy(date = dateMillis, updatedAt = now))
            measurementValueDao.deleteByEntryId(entry.id)
        }

        val weightValue = entry.measurements[MeasurementType.WEIGHT]
        val weightKg = weightValue?.valueKg ?: 0.0

        val processedMeasurements = entry.measurements.map { (type, value) ->
            if (type == MeasurementType.WEIGHT) {
                type to value
            } else {
                type to CalculationUtils.recalculateOnWeightChange(value, type, weightKg)
            }
        }.toMap()

        val valueEntities = Converters.toMeasurementValueEntities(entry.id, processedMeasurements)
        measurementValueDao.insertAll(valueEntities)
        return entry.id
    }

    suspend fun deleteEntry(id: Long) {
        entryDao.deleteById(id)
    }

    suspend fun restoreEntry(entry: BodyEntry): Long {
        return saveEntry(entry)
    }

    suspend fun getAllEntriesForExport(): List<BodyEntry> {
        val entities = entryDao.getAllEntriesAsc()
        return mapEntitiesToBodyEntriesSuspend(entities)
    }

    private suspend fun mapEntitiesToBodyEntries(entities: List<EntryEntity>): List<BodyEntry> {
        return mapEntitiesToBodyEntriesSuspend(entities)
    }

    private suspend fun mapEntitiesToBodyEntriesSuspend(entities: List<EntryEntity>): List<BodyEntry> {
        if (entities.isEmpty()) return emptyList()
        val entryIds = entities.map { it.id }
        val allValues = measurementValueDao.getByEntryIds(entryIds)
        val valuesByEntry = allValues.groupBy { it.entryId }
        return entities.map { entity ->
            Converters.toBodyEntry(entity, valuesByEntry[entity.id] ?: emptyList())
        }
    }
}
