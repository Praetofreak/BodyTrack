package com.project.myscale

import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.util.CalculationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculationUtilsTest {

    @Test
    fun `percent input derives kg from weight`() {
        val value = CalculationUtils.calculateMeasurementValue(
            type = MeasurementType.BODY_FAT,
            inputValue = 25.0,
            inputMode = InputMode.PERCENT,
            weightKg = 80.0
        )
        assertEquals(20.0, value.valueKg, 0.0001)
        assertEquals(25.0, value.valuePercent!!, 0.0001)
        assertEquals(InputMode.PERCENT, value.inputMode)
    }

    @Test
    fun `kg input derives percent from weight`() {
        val value = CalculationUtils.calculateMeasurementValue(
            type = MeasurementType.MUSCLE,
            inputValue = 40.0,
            inputMode = InputMode.KG,
            weightKg = 80.0
        )
        assertEquals(40.0, value.valueKg, 0.0001)
        assertEquals(50.0, value.valuePercent!!, 0.0001)
    }

    @Test
    fun `kg input with zero weight has no percent`() {
        val value = CalculationUtils.calculateMeasurementValue(
            type = MeasurementType.MUSCLE,
            inputValue = 40.0,
            inputMode = InputMode.KG,
            weightKg = 0.0
        )
        assertNull(value.valuePercent)
    }

    @Test
    fun `weight itself never carries a percent`() {
        val value = CalculationUtils.calculateMeasurementValue(
            type = MeasurementType.WEIGHT,
            inputValue = 80.0,
            inputMode = InputMode.PERCENT, // mode is ignored for weight
            weightKg = 80.0
        )
        assertEquals(80.0, value.valueKg, 0.0001)
        assertNull(value.valuePercent)
        assertEquals(InputMode.KG, value.inputMode)
    }

    @Test
    fun `weight change recalculates kg for percent-entered values`() {
        val original = MeasurementValue(valueKg = 20.0, valuePercent = 25.0, inputMode = InputMode.PERCENT)
        val updated = CalculationUtils.recalculateOnWeightChange(original, MeasurementType.BODY_FAT, 90.0)
        assertEquals(22.5, updated.valueKg, 0.0001)
        assertEquals(25.0, updated.valuePercent!!, 0.0001)
    }

    @Test
    fun `weight change recalculates percent for kg-entered values`() {
        val original = MeasurementValue(valueKg = 40.0, valuePercent = 50.0, inputMode = InputMode.KG)
        val updated = CalculationUtils.recalculateOnWeightChange(original, MeasurementType.MUSCLE, 100.0)
        assertEquals(40.0, updated.valueKg, 0.0001)
        assertEquals(40.0, updated.valuePercent!!, 0.0001)
    }

    @Test
    fun `weight measurement is not recalculated`() {
        val original = MeasurementValue(valueKg = 80.0, valuePercent = null, inputMode = InputMode.KG)
        val updated = CalculationUtils.recalculateOnWeightChange(original, MeasurementType.WEIGHT, 90.0)
        assertEquals(original, updated)
    }
}
