package com.project.myscale.ui.screens.chart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.project.myscale.data.model.MeasurementType
import com.project.myscale.data.model.InputMode

@Composable
fun CategoryFilterChips(
    availableTypes: List<MeasurementType>,
    activeTypes: Set<MeasurementType>,
    onToggleType: (MeasurementType) -> Unit,
    isDarkTheme: Boolean,
    displayMode: InputMode = InputMode.KG,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableTypes.sortedBy { it.sortOrder }.forEach { type ->
            val isEnabled = !(displayMode == InputMode.PERCENT && type == MeasurementType.WEIGHT)
            val isActive = activeTypes.contains(type) && isEnabled
            val chipColor = type.chartColor(isDarkTheme)

            FilterChip(
                selected = isActive,
                onClick = { if (isEnabled) onToggleType(type) },
                enabled = isEnabled,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(chipColor)
                        )
                        Text(type.labelDe)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
