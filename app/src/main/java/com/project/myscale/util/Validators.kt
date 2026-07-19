package com.project.myscale.util

import java.util.Locale

object Validators {

    /** Language-neutral validation outcomes; the UI maps them to string resources. */
    enum class ValidationError {
        WEIGHT_REQUIRED,
        INVALID_NUMBER,
        WEIGHT_OUT_OF_RANGE,
        VALUE_NOT_POSITIVE,
        EXCEEDS_TOTAL_WEIGHT,
        PERCENT_OUT_OF_RANGE
    }

    data class ValidationResult(
        val isValid: Boolean,
        val error: ValidationError? = null
    )

    fun validateWeight(input: String): ValidationResult {
        if (input.isBlank()) {
            return ValidationResult(false, ValidationError.WEIGHT_REQUIRED)
        }
        val value = parseDecimalInput(input)
            ?: return ValidationResult(false, ValidationError.INVALID_NUMBER)
        if (value < 20.0 || value > 350.0) {
            return ValidationResult(false, ValidationError.WEIGHT_OUT_OF_RANGE)
        }
        return ValidationResult(true)
    }

    fun validateOptionalKg(input: String, weightKg: Double?): ValidationResult {
        if (input.isBlank()) return ValidationResult(true) // optional field
        val value = parseDecimalInput(input)
            ?: return ValidationResult(false, ValidationError.INVALID_NUMBER)
        if (value <= 0) {
            return ValidationResult(false, ValidationError.VALUE_NOT_POSITIVE)
        }
        if (weightKg != null && value > weightKg) {
            return ValidationResult(false, ValidationError.EXCEEDS_TOTAL_WEIGHT)
        }
        return ValidationResult(true)
    }

    fun validateOptionalPercent(input: String): ValidationResult {
        if (input.isBlank()) return ValidationResult(true) // optional field
        val value = parseDecimalInput(input)
            ?: return ValidationResult(false, ValidationError.INVALID_NUMBER)
        if (value < 0.1 || value > 100.0) {
            return ValidationResult(false, ValidationError.PERCENT_OUT_OF_RANGE)
        }
        return ValidationResult(true)
    }

    /** True when the new weight deviates suspiciously (> 5 kg) from the previous one. */
    fun isLargeWeightDeviation(newWeight: Double, lastWeight: Double?): Boolean {
        if (lastWeight == null) return false
        return kotlin.math.abs(newWeight - lastWeight) > 5.0
    }

    /** True when the percent shares add up to more than 100 %. */
    fun exceedsPercentSum(percentValues: List<Double>): Boolean {
        return percentValues.sum() > 100.0
    }

    fun parseDecimalInput(input: String): Double? {
        if (input.isBlank()) return null
        val normalized = input.trim().replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        // Enforce max 1 decimal place
        val parts = normalized.split('.')
        return if (parts.size > 1 && parts[1].length > 1) null else value
    }

    fun formatDecimalInput(value: Double, locale: Locale = Locale.getDefault()): String {
        return Formatters.number(value, locale)
    }
}
