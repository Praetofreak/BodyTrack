package com.project.myscale.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    // Entry dates are persisted as epoch days (LocalDate.toEpochDay), which is
    // timezone-independent: the same calendar date always maps to the same value.
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    // The Material 3 DatePicker works in UTC milliseconds, so conversions for it
    // must use UTC midnight — not the device timezone.
    fun localDateToUtcMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    fun utcMillisToLocalDate(utcMillis: Long): LocalDate {
        return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    fun formatFullDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", locale))
    }

    fun formatShortDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return date.format(DateTimeFormatter.ofPattern("d. MMM yyyy", locale))
    }

    fun formatDayOfWeek(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE", locale))
    }

    fun formatChartDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return date.format(DateTimeFormatter.ofPattern("d. MMM", locale))
    }

    fun now(): Long = System.currentTimeMillis()

    fun today(): LocalDate = LocalDate.now()
}
