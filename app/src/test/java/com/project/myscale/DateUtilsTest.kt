package com.project.myscale

import com.project.myscale.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `epoch day round trip`() {
        val date = LocalDate.of(2026, 7, 19)
        val epochDay = DateUtils.localDateToEpochDay(date)
        assertEquals(date, DateUtils.epochDayToLocalDate(epochDay))
    }

    @Test
    fun `utc millis round trip is timezone independent`() {
        val date = LocalDate.of(2026, 1, 1)
        val millis = DateUtils.localDateToUtcMillis(date)
        assertEquals(date, DateUtils.utcMillisToLocalDate(millis))
        // Anything strictly inside the UTC day maps back to the same date
        assertEquals(date, DateUtils.utcMillisToLocalDate(millis + 86_399_999))
    }

    @Test
    fun `migration formula maps local midnight millis to correct epoch day`() {
        // Simulates the SQL in MIGRATION_1_2: (millis + 12h) / 24h
        val date = LocalDate.of(2026, 7, 19)
        val expectedEpochDay = date.toEpochDay()
        // Local midnight for offsets from UTC-11 to UTC+12
        for (offsetHours in -11..12) {
            val localMidnightUtcMillis =
                (expectedEpochDay * 86_400_000L) - (offsetHours * 3_600_000L)
            val migrated = (localMidnightUtcMillis + 43_200_000L) / 86_400_000L
            assertEquals("offset $offsetHours", expectedEpochDay, migrated)
        }
    }
}
