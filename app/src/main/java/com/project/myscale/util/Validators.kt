package com.project.myscale.util

object Validators {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val warningMessage: String? = null
    )

    fun validateWeight(input: String): ValidationResult {
        if (input.isBlank()) {
            return ValidationResult(false, "Gewicht ist ein Pflichtfeld")
        }
        val value = parseDecimalInput(input) ?: return ValidationResult(false, "Ungültige Eingabe")
        if (value < 20.0 || value > 350.0) {
            return ValidationResult(false, "Gewicht muss zwischen 20 und 350 kg liegen")
        }
        return ValidationResult(true)
    }

    fun validateOptionalKg(input: String, weightKg: Double?): ValidationResult {
        if (input.isBlank()) return ValidationResult(true) // optional field
        val value = parseDecimalInput(input) ?: return ValidationResult(false, "Ungültige Eingabe")
        if (value <= 0) {
            return ValidationResult(false, "Wert muss größer als 0 sein")
        }
        if (weightKg != null && value > weightKg) {
            return ValidationResult(false, "Darf nicht mehr als das Gesamtgewicht sein")
        }
        return ValidationResult(true)
    }

    fun validateOptionalPercent(input: String): ValidationResult {
        if (input.isBlank()) return ValidationResult(true) // optional field
        val value = parseDecimalInput(input) ?: return ValidationResult(false, "Ungültige Eingabe")
        if (value < 0.1 || value > 100.0) {
            return ValidationResult(false, "Bitte einen Wert zwischen 0.1% und 100% eingeben")
        }
        return ValidationResult(true)
    }

    fun checkWeightDeviation(newWeight: Double, lastWeight: Double?): String? {
        if (lastWeight == null) return null
        val diff = kotlin.math.abs(newWeight - lastWeight)
        if (diff > 5.0) {
            return "Große Abweichung zum letzten Eintrag. Stimmt der Wert?"
        }
        return null
    }

    fun checkPercentSum(percentValues: List<Double>): String? {
        val sum = percentValues.sum()
        if (sum > 100.0) {
            return "Die Summe der Anteile übersteigt 100%"
        }
        return null
    }

    fun parseDecimalInput(input: String): Double? {
        if (input.isBlank()) return null
        val normalized = input.replace(',', '.')
        return try {
            val value = normalized.toDouble()
            // Enforce max 1 decimal place
            val parts = normalized.split('.')
            if (parts.size > 1 && parts[1].length > 1) {
                null
            } else {
                value
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun formatDecimalInput(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
