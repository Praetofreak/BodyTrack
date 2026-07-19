package com.project.myscale.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.project.myscale.data.local.database.entity.EntryEntity
import com.project.myscale.data.local.database.entity.EntryWithValues
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM entries WHERE date = :epochDay LIMIT 1")
    suspend fun getByDate(epochDay: Long): EntryEntity?

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getWithValuesById(id: Long): EntryWithValues?

    @Transaction
    @Query("SELECT * FROM entries WHERE date = :epochDay LIMIT 1")
    suspend fun getWithValuesByDate(epochDay: Long): EntryWithValues?

    @Transaction
    @Query("SELECT * FROM entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWithValues(): EntryWithValues?

    @Transaction
    @Query("SELECT * FROM entries WHERE date < :epochDay ORDER BY date DESC LIMIT 1")
    suspend fun getWithValuesBefore(epochDay: Long): EntryWithValues?

    @Transaction
    @Query("SELECT * FROM entries ORDER BY date DESC")
    fun getAllWithValuesDescFlow(): Flow<List<EntryWithValues>>

    @Transaction
    @Query("SELECT * FROM entries ORDER BY date ASC")
    fun getAllWithValuesAscFlow(): Flow<List<EntryWithValues>>

    @Transaction
    @Query("SELECT * FROM entries ORDER BY date ASC")
    suspend fun getAllWithValuesAsc(): List<EntryWithValues>

    @Query("SELECT date FROM entries")
    suspend fun getAllDates(): List<Long>
}
