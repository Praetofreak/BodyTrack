package com.project.myscale.util

import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.MeasurementType
import java.io.OutputStream
import java.io.OutputStreamWriter

object CsvExporter {

    fun export(entries: List<BodyEntry>, outputStream: OutputStream) {
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

        // Header
        val headers = mutableListOf("Datum", "Gewicht_kg")
        MeasurementType.entries.filter { !it.isPrimary }.sortedBy { it.sortOrder }.forEach { type ->
            headers.add("${type.labelDe}_kg")
            if (type.supportsPercent) {
                headers.add("${type.labelDe}_percent")
            }
        }
        writer.write(headers.joinToString(";"))
        writer.write("\n")

        // Data rows
        for (entry in entries.sortedBy { it.date }) {
            val row = mutableListOf<String>()
            row.add(entry.date.toString())

            val weight = entry.measurements[MeasurementType.WEIGHT]
            row.add(weight?.valueKg?.let { formatCsvValue(it) } ?: "")

            MeasurementType.entries.filter { !it.isPrimary }.sortedBy { it.sortOrder }.forEach { type ->
                val value = entry.measurements[type]
                row.add(value?.valueKg?.let { formatCsvValue(it) } ?: "")
                if (type.supportsPercent) {
                    row.add(value?.valuePercent?.let { formatCsvValue(it) } ?: "")
                }
            }

            writer.write(row.joinToString(";"))
            writer.write("\n")
        }

        writer.flush()
        writer.close()
    }

    private fun formatCsvValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
