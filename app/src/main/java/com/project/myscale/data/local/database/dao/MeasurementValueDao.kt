package com.project.myscale.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.myscale.data.local.database.entity.MeasurementValueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementValueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(values: List<MeasurementValueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: MeasurementValueEntity): Long

    @Query("SELECT * FROM measurement_values WHERE entryId = :entryId")
    suspend fun getByEntryId(entryId: Long): List<MeasurementValueEntity>

    @Query("SELECT * FROM measurement_values WHERE entryId IN (:entryIds)")
    suspend fun getByEntryIds(entryIds: List<Long>): List<MeasurementValueEntity>

    @Query("SELECT * FROM measurement_values WHERE entryId IN (:entryIds)")
    fun getByEntryIdsFlow(entryIds: List<Long>): Flow<List<MeasurementValueEntity>>

    @Query("DELETE FROM measurement_values WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: Long)

    @Query("DELETE FROM measurement_values WHERE entryId = :entryId AND type = :type")
    suspend fun deleteByEntryIdAndType(entryId: Long, type: String)

    @Query("SELECT DISTINCT type FROM measurement_values")
    fun getDistinctTypes(): Flow<List<String>>

    @Query("SELECT DISTINCT type FROM measurement_values")
    suspend fun getDistinctTypesOnce(): List<String>
}
