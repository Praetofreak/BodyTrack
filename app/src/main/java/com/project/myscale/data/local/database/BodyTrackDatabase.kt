package com.project.myscale.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.project.myscale.data.local.database.dao.EntryDao
import com.project.myscale.data.local.database.dao.MeasurementValueDao
import com.project.myscale.data.local.database.entity.EntryEntity
import com.project.myscale.data.local.database.entity.MeasurementValueEntity

@Database(
    entities = [EntryEntity::class, MeasurementValueEntity::class],
    version = 2,
    exportSchema = true
)
abstract class BodyTrackDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun measurementValueDao(): MeasurementValueDao

    companion object {
        @Volatile
        private var INSTANCE: BodyTrackDatabase? = null

        /**
         * v1 stored `date` as epoch millis of local midnight; v2 stores epoch days
         * (LocalDate.toEpochDay), which is timezone-independent. Adding half a day
         * before the integer division maps local midnight to the correct calendar
         * day for every UTC offset in (-12h, +12h] without needing to know the
         * timezone the value was written in.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // If a past timezone change produced two rows for the same calendar
                // day, keep only the most recently updated one so the unique index
                // on `date` survives the conversion. Child rows are removed
                // explicitly because migrations may run without FK cascades.
                db.execSQL(
                    """
                    DELETE FROM measurement_values WHERE entryId IN (
                        SELECT id FROM entries WHERE id NOT IN (
                            SELECT id FROM (
                                SELECT id, MAX(updatedAt) FROM entries
                                GROUP BY (date + 43200000) / 86400000
                            )
                        )
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    DELETE FROM entries WHERE id NOT IN (
                        SELECT id FROM (
                            SELECT id, MAX(updatedAt) FROM entries
                            GROUP BY (date + 43200000) / 86400000
                        )
                    )
                    """.trimIndent()
                )
                db.execSQL("UPDATE entries SET date = (date + 43200000) / 86400000")
            }
        }

        fun getInstance(context: Context): BodyTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BodyTrackDatabase::class.java,
                    "bodytrack_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
