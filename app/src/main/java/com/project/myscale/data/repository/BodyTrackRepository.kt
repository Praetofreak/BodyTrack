package com.project.myscale.data.repository

import androidx.room.withTransaction
import com.project.myscale.data.local.database.BodyTrackDatabase
import com.project.myscale.data.local.database.entity.EntryEntity
import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.util.CalculationUtils
import com.project.myscale.util.Converters
import com.project.myscale.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class BodyTrackRepository(private val database: BodyTrackDatabase) {

    private val entryDao = database.entryDao()
    private val measurementValueDao = database.measurementValueDao()

    /** Thrown when an update would move an entry onto a date that another entry occupies. */
    class DateConflictException(val conflictingEntryId: Long) : Exception()

    data class ImportStats(val inserted: Int, val overwritten: Int, val skipped: Int)

    fun getAllEntriesFlow(): Flow<List<BodyEntry>> {
        return entryDao.getAllWithValuesDescFlow().map { list -> list.map(Converters::toBodyEntry) }
    }

    fun getAllEntriesAscFlow(): Flow<List<BodyEntry>> {
        return entryDao.getAllWithValuesAscFlow().map { list -> list.map(Converters::toBodyEntry) }
    }

    fun getDistinctMeasurementTypes(): Flow<List<MeasurementType>> {
        return measurementValueDao.getDistinctTypes().map { typeStrings ->
            typeStrings.mapNotNull { name ->
                try { MeasurementType.valueOf(name) } catch (_: IllegalArgumentException) { null }
            }.sortedBy { it.sortOrder }
        }
    }

    suspend fun getEntryByDate(date: LocalDate): BodyEntry? {
        return entryDao.getWithValuesByDate(DateUtils.localDateToEpochDay(date))
            ?.let(Converters::toBodyEntry)
    }

    suspend fun getEntryById(id: Long): BodyEntry? {
        return entryDao.getWithValuesById(id)?.let(Converters::toBodyEntry)
    }

    suspend fun getLatestEntry(): BodyEntry? {
        return entryDao.getLatestWithValues()?.let(Converters::toBodyEntry)
    }

    /** Latest entry strictly before [date]; used for the weight-deviation warning. */
    suspend fun getEntryBefore(date: LocalDate): BodyEntry? {
        return entryDao.getWithValuesBefore(DateUtils.localDateToEpochDay(date))
            ?.let(Converters::toBodyEntry)
    }

    /** Inserts the entry, or overwrites the entry already stored for the same date. */
    suspend fun saveEntry(entry: BodyEntry): Long = database.withTransaction {
        writeAtDate(entry.date, entry.measurements)
    }

    /**
     * Updates an existing entry, potentially moving it to a new date. Throws
     * [DateConflictException] if another entry occupies the target date — callers
     * resolve that via merge ([mergeEntries]) after asking the user.
     */
    suspend fun updateEntry(entry: BodyEntry): Long = database.withTransaction {
        val existing = entryDao.getWithValuesById(entry.id)?.entry
            ?: return@withTransaction writeAtDate(entry.date, entry.measurements)

        val epochDay = DateUtils.localDateToEpochDay(entry.date)
        val conflict = entryDao.getByDate(epochDay)
        if (conflict != null && conflict.id != entry.id) {
            throw DateConflictException(conflict.id)
        }

        entryDao.update(existing.copy(date = epochDay, updatedAt = DateUtils.now()))
        measurementValueDao.deleteByEntryId(entry.id)
        measurementValueDao.insertAll(
            Converters.toMeasurementValueEntities(entry.id, processMeasurements(entry.measurements))
        )
        entry.id
    }

    /**
     * Resolves a date conflict: deletes [obsoleteEntryId] and stores [merged]
     * (typically built from both entries with user-chosen values) in its place.
     */
    suspend fun mergeEntries(merged: BodyEntry, obsoleteEntryId: Long): Long =
        database.withTransaction {
            entryDao.deleteById(obsoleteEntryId)
            if (merged.id != 0L && merged.id != obsoleteEntryId) {
                updateExisting(merged)
            } else {
                writeAtDate(merged.date, merged.measurements)
            }
        }

    suspend fun deleteEntry(id: Long) {
        entryDao.deleteById(id)
    }

    suspend fun restoreEntry(entry: BodyEntry): Long = saveEntry(entry)

    suspend fun getAllEntriesForExport(): List<BodyEntry> {
        return entryDao.getAllWithValuesAsc().map(Converters::toBodyEntry)
    }

    suspend fun getExistingDates(): Set<LocalDate> {
        return entryDao.getAllDates().mapTo(mutableSetOf(), DateUtils::epochDayToLocalDate)
    }

    suspend fun importEntries(
        entries: List<BodyEntry>,
        overwriteExisting: Boolean
    ): ImportStats = database.withTransaction {
        var inserted = 0
        var overwritten = 0
        var skipped = 0
        for (entry in entries) {
            val existing = entryDao.getByDate(DateUtils.localDateToEpochDay(entry.date))
            if (existing != null && !overwriteExisting) {
                skipped++
                continue
            }
            writeAtDate(entry.date, entry.measurements)
            if (existing != null) overwritten++ else inserted++
        }
        ImportStats(inserted, overwritten, skipped)
    }

    private suspend fun updateExisting(entry: BodyEntry): Long {
        val existing = entryDao.getWithValuesById(entry.id)?.entry
            ?: return writeAtDate(entry.date, entry.measurements)
        entryDao.update(
            existing.copy(date = DateUtils.localDateToEpochDay(entry.date), updatedAt = DateUtils.now())
        )
        measurementValueDao.deleteByEntryId(entry.id)
        measurementValueDao.insertAll(
            Converters.toMeasurementValueEntities(entry.id, processMeasurements(entry.measurements))
        )
        return entry.id
    }

    /** Core write path; must run inside a transaction. */
    private suspend fun writeAtDate(
        date: LocalDate,
        measurements: Map<MeasurementType, MeasurementValue>
    ): Long {
        val now = DateUtils.now()
        val epochDay = DateUtils.localDateToEpochDay(date)
        val existing = entryDao.getByDate(epochDay)

        val entryId: Long
        if (existing != null) {
            entryDao.update(existing.copy(updatedAt = now))
            entryId = existing.id
            measurementValueDao.deleteByEntryId(entryId)
        } else {
            entryId = entryDao.insert(EntryEntity(date = epochDay, createdAt = now, updatedAt = now))
        }

        measurementValueDao.insertAll(
            Converters.toMeasurementValueEntities(entryId, processMeasurements(measurements))
        )
        return entryId
    }

    /** Keeps percent/kg values of dependent measurements consistent with the entry's weight. */
    private fun processMeasurements(
        measurements: Map<MeasurementType, MeasurementValue>
    ): Map<MeasurementType, MeasurementValue> {
        val weightKg = measurements[MeasurementType.WEIGHT]?.valueKg ?: 0.0
        return measurements.mapValues { (type, value) ->
            if (type == MeasurementType.WEIGHT) value
            else CalculationUtils.recalculateOnWeightChange(value, type, weightKg)
        }
    }
}
