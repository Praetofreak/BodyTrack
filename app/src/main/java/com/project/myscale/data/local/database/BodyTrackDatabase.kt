package com.project.myscale.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.project.myscale.data.local.database.dao.EntryDao
import com.project.myscale.data.local.database.dao.MeasurementValueDao
import com.project.myscale.data.local.database.entity.EntryEntity
import com.project.myscale.data.local.database.entity.MeasurementValueEntity

@Database(
    entities = [EntryEntity::class, MeasurementValueEntity::class],
    version = 1,
    exportSchema = true
)
abstract class BodyTrackDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun measurementValueDao(): MeasurementValueDao

    companion object {
        @Volatile
        private var INSTANCE: BodyTrackDatabase? = null

        fun getInstance(context: Context): BodyTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BodyTrackDatabase::class.java,
                    "bodytrack_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
