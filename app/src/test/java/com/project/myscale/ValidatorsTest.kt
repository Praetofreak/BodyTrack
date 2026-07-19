package com.project.myscale

import com.project.myscale.util.Validators
import com.project.myscale.util.Validators.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `parseDecimalInput accepts dot and comma`() {
        assertEquals(75.5, Validators.parseDecimalInput("75.5")!!, 0.0001)
        assertEquals(75.5, Validators.parseDecimalInput("75,5")!!, 0.0001)
        assertEquals(75.0, Validators.parseDecimalInput("75")!!, 0.0001)
    }

    @Test
    fun `parseDecimalInput rejects more than one decimal place`() {
        assertNull(Validators.parseDecimalInput("75.55"))
    }

    @Test
    fun `parseDecimalInput rejects garbage and blank`() {
        assertNull(Validators.parseDecimalInput("abc"))
        assertNull(Validators.parseDecimalInput(""))
        assertNull(Validators.parseDecimalInput("  "))
    }

    @Test
    fun `validateWeight enforces range`() {
        assertTrue(Validators.validateWeight("75").isValid)
        assertEquals(
            ValidationError.WEIGHT_OUT_OF_RANGE,
            Validators.validateWeight("19.9").error
        )
        assertEquals(
            ValidationError.WEIGHT_OUT_OF_RANGE,
            Validators.validateWeight("350.1").error
        )
        assertEquals(
            ValidationError.WEIGHT_REQUIRED,
            Validators.validateWeight("").error
        )
        assertEquals(
            ValidationError.INVALID_NUMBER,
            Validators.validateWeight("abc").error
        )
    }

    @Test
    fun `validateOptionalKg checks positivity and total weight`() {
        assertTrue(Validators.validateOptionalKg("", 80.0).isValid) // optional
        assertTrue(Validators.validateOptionalKg("20", 80.0).isValid)
        assertEquals(
            ValidationError.VALUE_NOT_POSITIVE,
            Validators.validateOptionalKg("0", 80.0).error
        )
        assertEquals(
            ValidationError.EXCEEDS_TOTAL_WEIGHT,
            Validators.validateOptionalKg("81", 80.0).error
        )
        // Unknown weight -> no exceeds check
        assertTrue(Validators.validateOptionalKg("81", null).isValid)
    }

    @Test
    fun `validateOptionalPercent enforces range`() {
        assertTrue(Validators.validateOptionalPercent("").isValid)
        assertTrue(Validators.validateOptionalPercent("50").isValid)
        assertEquals(
            ValidationError.PERCENT_OUT_OF_RANGE,
            Validators.validateOptionalPercent("0").error
        )
        assertEquals(
            ValidationError.PERCENT_OUT_OF_RANGE,
            Validators.validateOptionalPercent("100.1").error
        )
    }

    @Test
    fun `weight deviation warning above 5 kg`() {
        assertFalse(Validators.isLargeWeightDeviation(80.0, null))
        assertFalse(Validators.isLargeWeightDeviation(80.0, 75.5))
        assertTrue(Validators.isLargeWeightDeviation(80.0, 74.5))
        assertTrue(Validators.isLargeWeightDeviation(70.0, 76.0))
    }

    @Test
    fun `percent sum warning above 100`() {
        assertFalse(Validators.exceedsPercentSum(listOf(50.0, 50.0)))
        assertTrue(Validators.exceedsPercentSum(listOf(60.0, 50.0)))
        assertFalse(Validators.exceedsPercentSum(emptyList()))
    }
}
