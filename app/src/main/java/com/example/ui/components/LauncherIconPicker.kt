package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LauncherAppearance
import com.example.ui.theme.LocalIOS27Theme

/** One compact preference; changing it never disables a live launcher component. */
@Composable
fun LauncherIconPicker() {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current
    var follow by remember { mutableStateOf(LauncherAppearance.followsApp(context)) }
    Row(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(22.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (theme.isDark) Color.Black else Color.White), contentAlignment = Alignment.Center) {
            QuadrinheiroLogo(if (follow) QuadrinheiroLogoVariant.OnDark else QuadrinheiroLogoVariant.Primary, Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ícone do app", color = theme.textPrimary, fontSize = 15.sp)
            Text("Fundo do sistema", color = theme.textSecondary, fontSize = 12.sp, maxLines = 1)
        }
        AppleMenuPicker(value = if (follow) "accent" else "original", onChange = {
            follow = it == "accent"; LauncherAppearance.setFollowsApp(context, follow)
        }, options = listOf(SegmentOption("accent", "Cor do app"), SegmentOption("original", "Original")),
            modifier = Modifier.widthIn(max = 126.dp).testTag("launcher_icon_style"))
    }
}
