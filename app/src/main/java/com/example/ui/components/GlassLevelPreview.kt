package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.*
import kotlin.math.roundToInt

@Composable
internal fun GlassLevelPreview(level: Int, onChange: (Int) -> Unit, edgeRefraction: Boolean) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val backdrop = rememberLayerBackdrop()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(146.dp).clip(RoundedCornerShape(20.dp)).testTag("glass_level_preview")) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(theme.accent.copy(alpha = .65f), Color(0xFF8096B0), theme.bgPrimary))).layerBackdrop(backdrop)) {
                Text("Sua biblioteca", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopStart).padding(18.dp))
                Text("Histórias para descobrir", color = Color.White.copy(alpha = .8f), fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(14.dp).fillMaxWidth().height(44.dp)
                .drawBackdrop(backdrop, shape = { RoundedCornerShape(22.dp) }, effects = {
                    if (!settings.reduceTransparency) {
                        blur((5.5f * settings.glassBlurIntensity).dp.toPx())
                        if (edgeRefraction && settings.enableLensRefraction) lens(10.dp.toPx() * settings.glassIntensity, 18.dp.toPx() * settings.glassIntensity)
                    }
                }, onDrawSurface = { drawRect(theme.bgSecondary.copy(alpha = if (settings.reduceTransparency) 1f else (.28f * settings.glassOpacity).coerceIn(.1f, .95f))) })
                .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Search, null, tint = theme.textPrimary, modifier = Modifier.size(18.dp))
                Text("Buscar", fontSize = 15.sp, color = theme.textPrimary)
            }
        }
        AppleSlider(value = level.toFloat(), min = 0f, max = 100f,
            modifier = Modifier.testTag("glass_level_slider"), onChange = { onChange(it.roundToInt()) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mais nítido", fontSize = 12.sp, color = theme.textSecondary)
            Text("Mais fosco", fontSize = 12.sp, color = theme.textSecondary)
        }
    }
}
