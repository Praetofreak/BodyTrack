package com.project.myscale.util

import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue

object CalculationUtils {

    fun calculateMeasurementValue(
        type: MeasurementType,
        inputValue: Double,
        inputMode: InputMode,
        weightKg: Double
    ): MeasurementValue {
        return if (type == MeasurementType.WEIGHT) {
            MeasurementValue(
                valueKg = inputValue,
                valuePercent = null,
                inputMode = InputMode.KG
            )
        } else if (type.supportsPercent) {
            when (inputMode) {
                InputMode.PERCENT -> MeasurementValue(
                    valueKg = (inputValue / 100.0) * weightKg,
                    valuePercent = inputValue,
                    inputMode = InputMode.PERCENT
                )
                InputMode.KG -> MeasurementValue(
                    valueKg = inputValue,
                    valuePercent = if (weightKg > 0) (inputValue / weightKg) * 100.0 else null,
                    inputMode = InputMode.KG
                )
            }
        } else {
            MeasurementValue(
                valueKg = inputValue,
                valuePercent = null,
                inputMode = InputMode.KG
            )
        }
    }

    fun recalculateOnWeightChange(
        existingValue: MeasurementValue,
        type: MeasurementType,
        newWeightKg: Double
    ): MeasurementValue {
        if (type == MeasurementType.WEIGHT || !type.supportsPercent) {
            return existingValue
        }
        return when (existingValue.inputMode) {
            InputMode.PERCENT -> {
                val percent = existingValue.valuePercent ?: return existingValue
                existingValue.copy(
                    valueKg = (percent / 100.0) * newWeightKg
                )
            }
            InputMode.KG -> {
                existingValue.copy(
                    valuePercent = if (newWeightKg > 0) (existingValue.valueKg / newWeightKg) * 100.0 else null
                )
            }
        }
    }

    fun formatValue(value: Double, unit: String): String {
        val formatted = if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
        return "$formatted $unit"
    }
}
