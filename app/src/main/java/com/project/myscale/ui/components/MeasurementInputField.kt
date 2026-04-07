package com.project.myscale.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType

@Composable
fun MeasurementInputField(
    type: MeasurementType,
    value: String,
    onValueChange: (String) -> Unit,
    inputMode: InputMode,
    onInputModeChange: (InputMode) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    // Allow only numeric input with max 1 decimal
                    val filtered = filterDecimalInput(newValue)
                    if (filtered != null) {
                        onValueChange(filtered)
                    }
                },
                label = { Text(type.labelDe) },
                suffix = {
                    Text(
                        if (inputMode == InputMode.KG) type.unitPrimary
                        else type.unitSecondary ?: type.unitPrimary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = errorMessage != null,
                supportingText = if (errorMessage != null) {
                    { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            if (type.supportsPercent) {
                Spacer(modifier = Modifier.width(8.dp))
                UnitToggle(
                    selectedMode = inputMode,
                    onModeChanged = onInputModeChange
                )
            }
        }
    }
}

@Composable
fun WeightInputField(
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String?,
    warningMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                val filtered = filterDecimalInput(newValue)
                if (filtered != null) {
                    onValueChange(filtered)
                }
            },
            label = { Text("Gewicht") },
            suffix = { Text("kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = errorMessage != null,
            supportingText = if (errorMessage != null) {
                { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
            } else if (warningMessage != null) {
                {
                    Text(
                        warningMessage,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else null,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun filterDecimalInput(input: String): String? {
    if (input.isEmpty()) return input
    val normalized = input.replace(',', '.')
    // Allow partial valid input (like "7." while typing)
    if (normalized == ".") return "0."
    val regex = Regex("^\\d*\\.?\\d{0,1}$")
    return if (regex.matches(normalized)) normalized else null
}
