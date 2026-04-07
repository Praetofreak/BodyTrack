package com.project.myscale.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {

    private val germanLocale = Locale.GERMAN
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", germanLocale)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d. MMM yyyy", germanLocale)
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", germanLocale)

    fun localDateToEpochMilli(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun epochMilliToLocalDate(epochMilli: Long): LocalDate {
        return Instant.ofEpochMilli(epochMilli)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun formatFullDate(date: LocalDate): String {
        return date.format(fullDateFormatter)
    }

    fun formatShortDate(date: LocalDate): String {
        return date.format(shortDateFormatter)
    }

    fun formatDayOfWeek(date: LocalDate): String {
        return date.format(dayOfWeekFormatter)
    }

    fun now(): Long = System.currentTimeMillis()

    fun today(): LocalDate = LocalDate.now()
}
