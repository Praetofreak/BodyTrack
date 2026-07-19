package com.project.myscale

import com.project.myscale.data.model.BodyEntry
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.util.CsvExporter
import com.project.myscale.util.CsvImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class CsvRoundTripTest {

    @Test
    fun `export then import preserves entries`() {
        val entries = listOf(
            BodyEntry(
                date = LocalDate.of(2026, 7, 1),
                measurements = mapOf(
                    MeasurementType.WEIGHT to MeasurementValue(80.5, null, InputMode.KG),
                    MeasurementType.BODY_FAT to MeasurementValue(20.1, 25.0, InputMode.PERCENT)
                )
            ),
            BodyEntry(
                date = LocalDate.of(2026, 7, 2),
                measurements = mapOf(
                    MeasurementType.WEIGHT to MeasurementValue(80.0, null, InputMode.KG),
                    MeasurementType.MUSCLE to MeasurementValue(40.0, 50.0, InputMode.KG)
                )
            )
        )

        val out = ByteArrayOutputStream()
        CsvExporter.export(entries, out)
        val result = CsvImporter.parse(ByteArrayInputStream(out.toByteArray()))

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertEquals(2, result.entries.size)

        val first = result.entries[0]
        assertEquals(LocalDate.of(2026, 7, 1), first.date)
        assertEquals(80.5, first.measurements[MeasurementType.WEIGHT]!!.valueKg, 0.0001)
        assertEquals(20.1, first.measurements[MeasurementType.BODY_FAT]!!.valueKg, 0.0001)
        assertEquals(25.0, first.measurements[MeasurementType.BODY_FAT]!!.valuePercent!!, 0.0001)

        val second = result.entries[1]
        assertEquals(40.0, second.measurements[MeasurementType.MUSCLE]!!.valueKg, 0.0001)
        assertEquals(50.0, second.measurements[MeasurementType.MUSCLE]!!.valuePercent!!, 0.0001)
    }

    @Test
    fun `export uses dot as decimal separator regardless of locale`() {
        val entries = listOf(
            BodyEntry(
                date = LocalDate.of(2026, 7, 1),
                measurements = mapOf(
                    MeasurementType.WEIGHT to MeasurementValue(80.5, null, InputMode.KG)
                )
            )
        )
        val out = ByteArrayOutputStream()
        CsvExporter.export(entries, out)
        val csv = out.toString("UTF-8")
        assertTrue(csv.contains("80.5"))
    }

    @Test
    fun `import reports rows without weight and bad dates`() {
        val csv = """
            date;weight_kg;body_fat_kg;body_fat_percent;water_kg;water_percent;muscle_kg;muscle_percent;bone_mass_kg;bone_mass_percent
            2026-07-01;80.5
            2026-07-02;;20
            not-a-date;80
        """.trimIndent()
        val result = CsvImporter.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(1, result.entries.size)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.any { it.reason == CsvImporter.ErrorReason.MISSING_WEIGHT })
        assertTrue(result.errors.any { it.reason == CsvImporter.ErrorReason.INVALID_DATE })
    }

    @Test
    fun `import accepts comma decimals`() {
        val csv = """
            date;weight_kg
            2026-07-01;80,5
        """.trimIndent()
        val result = CsvImporter.parse(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(80.5, result.entries[0].measurements[MeasurementType.WEIGHT]!!.valueKg, 0.0001)
    }
}
