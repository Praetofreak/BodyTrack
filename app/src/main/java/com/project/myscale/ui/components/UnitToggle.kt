package com.project.myscale.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.myscale.data.model.InputMode

@Composable
fun UnitToggle(
    selectedMode: InputMode,
    onModeChanged: (InputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        val shape = RoundedCornerShape(
            topStart = 20.dp, bottomStart = 20.dp,
            topEnd = 0.dp, bottomEnd = 0.dp
        )
        OutlinedButton(
            onClick = { onModeChanged(InputMode.KG) },
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selectedMode == InputMode.KG) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                contentColor = if (selectedMode == InputMode.KG) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.height(40.dp)
        ) {
            Text("kg", style = MaterialTheme.typography.labelLarge)
        }

        val shapeEnd = RoundedCornerShape(
            topStart = 0.dp, bottomStart = 0.dp,
            topEnd = 20.dp, bottomEnd = 20.dp
        )
        OutlinedButton(
            onClick = { onModeChanged(InputMode.PERCENT) },
            shape = shapeEnd,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selectedMode == InputMode.PERCENT) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                contentColor = if (selectedMode == InputMode.PERCENT) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.height(40.dp)
        ) {
            Text("%", style = MaterialTheme.typography.labelLarge)
        }
    }
}
