package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalAppMotion

/** Navigation stays available during scrolling and always keeps the same shape. */
@Composable
internal fun SettingsBackButton(onBack: () -> Unit, modifier: Modifier = Modifier,
    collapsed: Boolean = false, label: String = "Voltar") {
    val motion = LocalAppMotion.current
    Row(modifier.height(44.dp).testTag("settings_back_button").liquidGlassNavSurface(CircleShape)
        .clickable(role = Role.Button, onClick = onBack).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Voltar", tint = LocalIOS27Theme.current.textPrimary,
            modifier = Modifier.size(23.dp))
        AnimatedVisibility(!collapsed, enter = expandHorizontally(motion.spring(1f, 700f)) + fadeIn(),
            exit = shrinkHorizontally(motion.spring(1f, 700f)) + fadeOut()) {
            Text(label, color = LocalIOS27Theme.current.textPrimary, fontSize = 15.sp,
                maxLines = 1, modifier = Modifier.padding(end = 6.dp))
        }
    }
}
