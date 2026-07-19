package com.project.myscale.util

import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeParseException

object CsvImporter {

    /** Language-neutral row error; the UI maps [reason] to a string resource. */
    data class RowError(val lineNumber: Int, val reason: ErrorReason)

    enum class ErrorReason {
        INVALID_FORMAT,
        INVALID_DATE,
        MISSING_WEIGHT
    }

    data class ImportResult(
        val entries: List<BodyEntry>,
        val errors: List<RowError>,
        val isValid: Boolean
    )

    fun parse(inputStream: InputStream): ImportResult {
        val lines = InputStreamReader(inputStream, Charsets.UTF_8).use { it.readLines() }

        if (lines.isEmpty()) {
            return ImportResult(emptyList(), listOf(RowError(1, ErrorReason.INVALID_FORMAT)), false)
        }

        val errors = mutableListOf<RowError>()
        val entries = mutableListOf<BodyEntry>()

        // Skip header (column order is the contract, header text is ignored)
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            val lineNumber = i + 1

            val parts = line.split(";")
            if (parts.size < 2) {
                errors.add(RowError(lineNumber, ErrorReason.INVALID_FORMAT))
                continue
            }

            val date = try {
                LocalDate.parse(parts[0].trim())
            } catch (_: DateTimeParseException) {
                errors.add(RowError(lineNumber, ErrorReason.INVALID_DATE))
                continue
            }

            val measurements = mutableMapOf<MeasurementType, MeasurementValue>()

            val weightKg = parts.getOrNull(1)?.trim()?.replace(',', '.')?.toDoubleOrNull()
            if (weightKg != null) {
                measurements[MeasurementType.WEIGHT] = MeasurementValue(
                    valueKg = weightKg,
                    valuePercent = null,
                    inputMode = InputMode.KG
                )
            }

            var colIndex = 2
            MeasurementType.entries.filter { !it.isPrimary }.sortedBy { it.sortOrder }.forEach { type ->
                val kgStr = parts.getOrNull(colIndex)?.trim() ?: ""
                colIndex++
                val percentStr = if (type.supportsPercent) {
                    val s = parts.getOrNull(colIndex)?.trim() ?: ""
                    colIndex++
                    s
                } else null

                val kgVal = kgStr.replace(',', '.').toDoubleOrNull()
                val percentVal = percentStr?.replace(',', '.')?.toDoubleOrNull()

                if (kgVal != null || percentVal != null) {
                    measurements[type] = MeasurementValue(
                        valueKg = kgVal ?: 0.0,
                        valuePercent = percentVal,
                        inputMode = if (percentVal != null && kgVal == null) InputMode.PERCENT else InputMode.KG
                    )
                }
            }

            if (measurements.containsKey(MeasurementType.WEIGHT)) {
                entries.add(BodyEntry(date = date, measurements = measurements))
            } else {
                errors.add(RowError(lineNumber, ErrorReason.MISSING_WEIGHT))
            }
        }

        return ImportResult(
            entries = entries,
            errors = errors,
            isValid = entries.isNotEmpty()
        )
    }
}
