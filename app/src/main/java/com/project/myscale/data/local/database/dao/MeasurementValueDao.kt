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

    @Query("DELETE FROM measurement_values WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: Long)

    @Query("SELECT DISTINCT type FROM measurement_values")
    fun getDistinctTypes(): Flow<List<String>>
}
