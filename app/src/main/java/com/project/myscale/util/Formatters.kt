package com.project.myscale.util

import java.util.Locale

/**
 * Single home for number formatting. UI formatting follows the current locale
 * (decimal comma in German/French); CSV formatting is locale-independent so
 * export files stay portable between devices and languages.
 */
object Formatters {

    /** "75" for whole numbers, "75.5"/"75,5" (locale-dependent) otherwise. */
    fun number(value: Double, locale: Locale = Locale.getDefault()): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(locale, "%.1f", value)
        }
    }

    fun valueWithUnit(value: Double, unit: String, locale: Locale = Locale.getDefault()): String {
        return "${number(value, locale)} $unit"
    }

    fun signedWithUnit(value: Double, unit: String, locale: Locale = Locale.getDefault()): String {
        val sign = if (value >= 0) "+" else ""
        return "$sign${number(value, locale)} $unit"
    }

    fun signedPercent(value: Double, locale: Locale = Locale.getDefault()): String {
        val sign = if (value >= 0) "+" else ""
        return "$sign${String.format(locale, "%.1f", value)}%"
    }

    /** Locale-independent (dot as decimal separator) for CSV files. */
    fun csvNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.1f", value)
        }
    }
}
