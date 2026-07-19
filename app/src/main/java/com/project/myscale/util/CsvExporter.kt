package com.project.myscale.util

import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.MeasurementType
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * Writes entries as semicolon-separated CSV. Headers use stable, language-neutral
 * keys and numbers use a dot as decimal separator, so exports are portable across
 * devices, app languages, and system locales. Import is column-order based, so
 * files exported by older (German-header) versions remain importable.
 */
object CsvExporter {

    fun export(entries: List<BodyEntry>, outputStream: OutputStream) {
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            val headers = mutableListOf("date", "weight_kg")
            secondaryTypes().forEach { type ->
                headers.add("${type.csvKey}_kg")
                if (type.supportsPercent) {
                    headers.add("${type.csvKey}_percent")
                }
            }
            writer.write(headers.joinToString(";"))
            writer.write("\n")

            for (entry in entries.sortedBy { it.date }) {
                val row = mutableListOf<String>()
                row.add(entry.date.toString())

                val weight = entry.measurements[MeasurementType.WEIGHT]
                row.add(weight?.valueKg?.let { Formatters.csvNumber(it) } ?: "")

                secondaryTypes().forEach { type ->
                    val value = entry.measurements[type]
                    row.add(value?.valueKg?.let { Formatters.csvNumber(it) } ?: "")
                    if (type.supportsPercent) {
                        row.add(value?.valuePercent?.let { Formatters.csvNumber(it) } ?: "")
                    }
                }

                writer.write(row.joinToString(";"))
                writer.write("\n")
            }
        }
    }

    private fun secondaryTypes() =
        MeasurementType.entries.filter { !it.isPrimary }.sortedBy { it.sortOrder }
}
