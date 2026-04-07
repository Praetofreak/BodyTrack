package com.project.myscale.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.project.myscale.data.local.database.entity.EntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryEntity?

    @Query("SELECT * FROM entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: Long): EntryEntity?

    @Query("SELECT * FROM entries ORDER BY date DESC")
    fun getAllEntriesFlow(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date >= :startDate ORDER BY date DESC")
    fun getEntriesFromDate(startDate: Long): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getEntriesInRange(startDate: Long, endDate: Long): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestEntry(): EntryEntity?

    @Query("SELECT * FROM entries ORDER BY date ASC")
    fun getAllEntriesAscFlow(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY date ASC")
    suspend fun getAllEntriesAsc(): List<EntryEntity>

    @Query("SELECT COUNT(*) FROM entries")
    fun getEntryCount(): Flow<Int>

    @Query("SELECT DISTINCT type FROM measurement_values")
    fun getDistinctMeasurementTypes(): Flow<List<String>>
}
