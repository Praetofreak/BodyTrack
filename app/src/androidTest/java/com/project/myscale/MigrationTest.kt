package com.project.myscale

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.myscale.data.local.database.BodyTrackDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BodyTrackDatabase::class.java
    )

    @Test
    fun migrate1To2_convertsLocalMidnightMillisToEpochDay() {
        val date = LocalDate.of(2026, 7, 19)
        val localMidnightMillis = date.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO entries (date, createdAt, updatedAt) VALUES ($localMidnightMillis, 1, 1)"
            )
        }

        helper.runMigrationsAndValidate(dbName, 2, true, BodyTrackDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT date FROM entries").use { cursor ->
                cursor.moveToFirst()
                assertEquals(date.toEpochDay(), cursor.getLong(0))
            }
        }
    }

    @Test
    fun migrate1To2_deduplicatesSameCalendarDayKeepingNewest() {
        val date = LocalDate.of(2026, 7, 19)
        val zone = ZoneId.systemDefault()
        val midnight = date.atStartOfDay(zone).toInstant().toEpochMilli()
        // Same calendar day recorded under a timezone two hours apart
        val shiftedMidnight = midnight - 2 * 3_600_000L

        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL("INSERT INTO entries (id, date, createdAt, updatedAt) VALUES (1, $midnight, 1, 1)")
            db.execSQL("INSERT INTO entries (id, date, createdAt, updatedAt) VALUES (2, $shiftedMidnight, 2, 2)")
            db.execSQL(
                "INSERT INTO measurement_values (entryId, type, valueKg, valuePercent, inputMode) " +
                    "VALUES (1, 'WEIGHT', 80.0, NULL, 'KG')"
            )
            db.execSQL(
                "INSERT INTO measurement_values (entryId, type, valueKg, valuePercent, inputMode) " +
                    "VALUES (2, 'WEIGHT', 81.0, NULL, 'KG')"
            )
        }

        helper.runMigrationsAndValidate(dbName, 2, true, BodyTrackDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT id FROM entries").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(2L, cursor.getLong(0)) // newest updatedAt survives
            }
            db.query("SELECT entryId FROM measurement_values").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(2L, cursor.getLong(0))
            }
        }
    }
}
