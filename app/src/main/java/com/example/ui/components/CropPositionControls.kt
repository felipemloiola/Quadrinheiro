package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun CropPositionControls(
    offset: Offset,
    limit: Offset,
    onChange: (Offset) -> Unit,
    labelColor: Color,
    enabled: Boolean = true
) {
    Column {
        if (limit.x > 1f) {
            Text("Posição horizontal", color = labelColor)
            Slider(
                value = offset.x.coerceIn(-limit.x, limit.x),
                onValueChange = { onChange(offset.copy(x = it)) },
                valueRange = -limit.x..limit.x,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Posição horizontal" }
            )
        }
        if (limit.y > 1f) {
            Text("Posição vertical", color = labelColor)
            Slider(
                value = offset.y.coerceIn(-limit.y, limit.y),
                onValueChange = { onChange(offset.copy(y = it)) },
                valueRange = -limit.y..limit.y,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Posição vertical" }
            )
        }
    }
}
