package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.theme.LocalIOS27Theme

/** Navigation stays available during scrolling and always keeps the same shape. */
@Composable
internal fun SettingsBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp).testTag("settings_back_button").liquidGlassNavSurface(CircleShape)
        .clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Voltar", tint = LocalIOS27Theme.current.textPrimary,
            modifier = Modifier.size(23.dp))
    }
}
