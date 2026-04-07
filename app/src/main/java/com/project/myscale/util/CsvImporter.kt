package com.project.myscale.util

import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate

object CsvImporter {

    data class ImportResult(
        val entries: List<BodyEntry>,
        val errors: List<String>,
        val isValid: Boolean
    )

    fun parse(inputStream: InputStream): ImportResult {
        val reader = InputStreamReader(inputStream, Charsets.UTF_8)
        val lines = reader.readLines()
        reader.close()

        if (lines.isEmpty()) {
            return ImportResult(emptyList(), listOf("Leere Datei"), false)
        }

        val errors = mutableListOf<String>()
        val entries = mutableListOf<BodyEntry>()

        // Skip header
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue

            val parts = line.split(";")
            if (parts.size < 2) {
                errors.add("Zeile ${i + 1}: Ungültiges Format")
                continue
            }

            try {
                val date = LocalDate.parse(parts[0].trim())
                val measurements = mutableMapOf<MeasurementType, MeasurementValue>()

                // Weight
                val weightStr = parts.getOrNull(1)?.trim() ?: ""
                if (weightStr.isNotBlank()) {
                    val weightKg = weightStr.replace(',', '.').toDoubleOrNull()
                    if (weightKg != null) {
                        measurements[MeasurementType.WEIGHT] = MeasurementValue(
                            valueKg = weightKg,
                            valuePercent = null,
                            inputMode = InputMode.KG
                        )
                    }
                }

                // Other types
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
                    errors.add("Zeile ${i + 1}: Kein Gewicht angegeben")
                }

            } catch (e: Exception) {
                errors.add("Zeile ${i + 1}: ${e.message}")
            }
        }

        return ImportResult(
            entries = entries,
            errors = errors,
            isValid = entries.isNotEmpty()
        )
    }
}
