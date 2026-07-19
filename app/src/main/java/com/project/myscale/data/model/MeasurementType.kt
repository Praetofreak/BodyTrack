package com.project.myscale.data.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.project.myscale.R

enum class MeasurementType(
    @param:StringRes val labelRes: Int,
    val csvKey: String,
    val isPrimary: Boolean,
    val supportsPercent: Boolean,
    val defaultEnabled: Boolean,
    val decreaseIsPositive: Boolean,
    val unitPrimary: String,
    val unitSecondary: String?,
    val colorLight: Long,
    val colorDark: Long,
    val sortOrder: Int
) {
    WEIGHT(
        labelRes = R.string.measurement_weight,
        csvKey = "weight",
        isPrimary = true,
        supportsPercent = false,
        defaultEnabled = true,
        decreaseIsPositive = true,
        unitPrimary = "kg",
        unitSecondary = null,
        colorLight = 0xFF2E7D32,
        colorDark = 0xFF81C784,
        sortOrder = 0
    ),
    BODY_FAT(
        labelRes = R.string.measurement_body_fat,
        csvKey = "body_fat",
        isPrimary = false,
        supportsPercent = true,
        defaultEnabled = false,
        decreaseIsPositive = true,
        unitPrimary = "kg",
        unitSecondary = "%",
        colorLight = 0xFFE53935,
        colorDark = 0xFFEF9A9A,
        sortOrder = 1
    ),
    WATER(
        labelRes = R.string.measurement_water,
        csvKey = "water",
        isPrimary = false,
        supportsPercent = true,
        defaultEnabled = false,
        decreaseIsPositive = false,
        unitPrimary = "kg",
        unitSecondary = "%",
        colorLight = 0xFF1565C0,
        colorDark = 0xFF64B5F6,
        sortOrder = 2
    ),
    MUSCLE(
        labelRes = R.string.measurement_muscle,
        csvKey = "muscle",
        isPrimary = false,
        supportsPercent = true,
        defaultEnabled = false,
        decreaseIsPositive = false,
        unitPrimary = "kg",
        unitSecondary = "%",
        colorLight = 0xFF7B1FA2,
        colorDark = 0xFFCE93D8,
        sortOrder = 3
    ),
    BONE_MASS(
        labelRes = R.string.measurement_bone_mass,
        csvKey = "bone_mass",
        isPrimary = false,
        supportsPercent = true,
        defaultEnabled = false,
        decreaseIsPositive = false,
        unitPrimary = "kg",
        unitSecondary = "%",
        colorLight = 0xFF795548,
        colorDark = 0xFFBCAAA4,
        sortOrder = 4
    );

    fun chartColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) Color(this.colorDark) else Color(this.colorLight)
    }
}
