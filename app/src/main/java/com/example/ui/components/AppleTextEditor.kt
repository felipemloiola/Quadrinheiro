package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIOS27Theme

@Composable
fun AppleTextEditor(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val theme = LocalIOS27Theme.current
    BasicTextField(value, onChange, modifier.fillMaxWidth().inputWellSurface(RoundedCornerShape(14.dp)).padding(14.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = theme.textPrimary, lineHeight = 23.sp),
        minLines = 4, maxLines = 8, cursorBrush = SolidColor(theme.accent),
        decorationBox = { field -> Box {
            if (value.isBlank()) Text(placeholder, color = theme.textSecondary, style = MaterialTheme.typography.bodyLarge)
            field()
        } })
}
