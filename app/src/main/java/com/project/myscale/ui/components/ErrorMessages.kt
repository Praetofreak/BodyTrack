package com.project.myscale.ui.components

import androidx.annotation.StringRes
import com.project.myscale.R
import com.project.myscale.util.CsvImporter
import com.project.myscale.util.Validators.ValidationError

@StringRes
fun ValidationError.messageRes(): Int = when (this) {
    ValidationError.WEIGHT_REQUIRED -> R.string.error_weight_required
    ValidationError.INVALID_NUMBER -> R.string.error_invalid_number
    ValidationError.WEIGHT_OUT_OF_RANGE -> R.string.error_weight_out_of_range
    ValidationError.VALUE_NOT_POSITIVE -> R.string.error_value_not_positive
    ValidationError.EXCEEDS_TOTAL_WEIGHT -> R.string.error_exceeds_total_weight
    ValidationError.PERCENT_OUT_OF_RANGE -> R.string.error_percent_out_of_range
}

@StringRes
fun CsvImporter.ErrorReason.messageRes(): Int = when (this) {
    CsvImporter.ErrorReason.INVALID_FORMAT -> R.string.import_error_invalid_format
    CsvImporter.ErrorReason.INVALID_DATE -> R.string.import_error_invalid_date
    CsvImporter.ErrorReason.MISSING_WEIGHT -> R.string.import_error_missing_weight
}
