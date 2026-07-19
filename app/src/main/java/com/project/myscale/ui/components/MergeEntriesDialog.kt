package com.project.myscale.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.myscale.R
import com.project.myscale.data.model.InputMode
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.MeasurementValue
import com.project.myscale.util.DateUtils
import com.project.myscale.util.Formatters
import java.time.LocalDate

/** A measurement that both entries define with different values. */
data class MergeConflictField(
    val type: MeasurementType,
    val editedValue: MeasurementValue,
    val existingValue: MeasurementValue
)

/**
 * Shown when an edited entry is moved onto a date that already has an entry.
 * Fields present in only one entry merge automatically; for each conflicting
 * field the user picks which value survives.
 */
@Composable
fun MergeEntriesDialog(
    targetDate: LocalDate,
    conflicts: List<MergeConflictField>,
    onConfirm: (chosenEdited: Set<MeasurementType>) -> Unit,
    onDismiss: () -> Unit
) {
    // true = keep the edited entry's value (default), false = keep the existing one
    val selections = remember(conflicts) {
        mutableStateMapOf<MeasurementType, Boolean>().apply {
            conflicts.forEach { put(it.type, true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.merge_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(
                        R.string.merge_dialog_message,
                        DateUtils.formatShortDate(targetDate)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (conflicts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.merge_dialog_choose_values),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                for (conflict in conflicts) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(conflict.type.labelRes),
                        style = MaterialTheme.typography.titleSmall
                    )
                    MergeOption(
                        label = stringResource(R.string.merge_option_edited),
                        valueText = formatMergeValue(conflict.type, conflict.editedValue),
                        selected = selections[conflict.type] == true,
                        onSelect = { selections[conflict.type] = true }
                    )
                    MergeOption(
                        label = stringResource(R.string.merge_option_existing),
                        valueText = formatMergeValue(conflict.type, conflict.existingValue),
                        selected = selections[conflict.type] == false,
                        onSelect = { selections[conflict.type] = false }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selections.filterValues { it }.keys)
            }) {
                Text(stringResource(R.string.action_merge))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun MergeOption(
    label: String,
    valueText: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 2.dp)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMergeValue(type: MeasurementType, value: MeasurementValue): String {
    return if (value.inputMode == InputMode.PERCENT && value.valuePercent != null) {
        Formatters.valueWithUnit(value.valuePercent, "%")
    } else {
        Formatters.valueWithUnit(value.valueKg, type.unitPrimary)
    }
}
