package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.example.ui.theme.readableForeground
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlin.math.roundToInt

/**
 * Accessible zoom and directional pan controls for crop dialogs (Banner and Profile Photo).
 * Uses continuous squircle geometry (`ContinuousRoundedRectangle`) and Liquid Glass circular buttons,
 * ensuring TalkBack, switch-access, and keyboard users can adjust framing without multi-touch gestures,
 * with >= 48dp touch targets, high contrast, and reduced-transparency fallback.
 */
@Composable
internal fun CropPositionControls(
    zoom: Float,
    zoomRange: ClosedFloatingPointRange<Float> = 1f..4f,
    onZoomChange: (Float) -> Unit,
    onNudge: (dxFraction: Float, dyFraction: Float) -> Unit,
    onReset: () -> Unit,
    enabled: Boolean = true,
    onDarkScrim: Boolean = false,
    zoomSliderTag: String = "crop_zoom_slider",
    modifier: Modifier = Modifier
) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val containerShape = remember { ContinuousRoundedRectangle(22.dp) }
    val containerBg = if (onDarkScrim) {
        if (settings.reduceTransparency) Color(0xFF1C1E26) else Color.White.copy(alpha = 0.10f)
    } else {
        if (settings.reduceTransparency) theme.bgPrimary else theme.bgSecondary
    }
    val borderColor = if (onDarkScrim) {
        Color.White.copy(alpha = if (settings.reduceTransparency) 0.28f else 0.18f)
    } else {
        theme.divider
    }
    val primaryText = if (onDarkScrim) Color.White else readableForeground(theme.textPrimary, containerBg, 4.5f)
    val accentTint = readableForeground(theme.accent, if (onDarkScrim) Color(0xFF14161D) else containerBg, 4.5f)
    val zoomPercent = (zoom * 100f).roundToInt()
    val canZoomOut = enabled && zoom > zoomRange.start + 0.01f
    val canZoomIn = enabled && zoom < zoomRange.endInclusive - 0.01f

    val containerModifier = when {
        settings.reduceTransparency -> Modifier
            .clip(containerShape)
            .background(containerBg, containerShape)
            .border(0.9.dp, borderColor, containerShape)
        onDarkScrim -> Modifier
            .clip(containerShape)
            .liquidGlass(shape = containerShape, surfaceAlpha = 0.16f)
            .border(0.85.dp, borderColor, containerShape)
        else -> Modifier.contentMaterialSurface(shape = containerShape)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(containerModifier)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("crop_position_controls"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Zoom e enquadramento",
                color = primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$zoomPercent%",
                color = accentTint,
                fontSize = 13.sp,
                fontFamily = SpaceGroteskTabularFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Ampliação atual $zoomPercent por cento"
                    }
                    .testTag("crop_zoom_value")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropIconStepButton(
                icon = Icons.Default.Remove,
                label = "Diminuir zoom",
                tag = "crop_zoom_out",
                enabled = canZoomOut,
                onDarkScrim = onDarkScrim,
                reduceTransparency = settings.reduceTransparency,
                onClick = { onZoomChange((zoom - 0.25f).coerceIn(zoomRange.start, zoomRange.endInclusive)) }
            )
            Slider(
                value = zoom,
                onValueChange = { onZoomChange(it.coerceIn(zoomRange.start, zoomRange.endInclusive)) },
                enabled = enabled,
                valueRange = zoomRange,
                colors = SliderDefaults.colors(
                    thumbColor = if (onDarkScrim) Color.White else accentTint,
                    activeTrackColor = accentTint,
                    inactiveTrackColor = if (onDarkScrim) Color.White.copy(alpha = 0.25f) else theme.divider
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Controle de ampliação"
                        stateDescription = "$zoomPercent por cento"
                    }
                    .testTag(zoomSliderTag)
            )
            CropIconStepButton(
                icon = Icons.Default.Add,
                label = "Aumentar zoom",
                tag = "crop_zoom_in",
                enabled = canZoomIn,
                onDarkScrim = onDarkScrim,
                reduceTransparency = settings.reduceTransparency,
                onClick = { onZoomChange((zoom + 0.25f).coerceIn(zoomRange.start, zoomRange.endInclusive)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CropIconStepButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                label = "Mover imagem para esquerda",
                tag = "crop_pan_left",
                enabled = enabled,
                onDarkScrim = onDarkScrim,
                reduceTransparency = settings.reduceTransparency,
                onClick = { onNudge(0.12f, 0f) }
            )
            CropIconStepButton(
                icon = Icons.Default.KeyboardArrowUp,
                label = "Mover imagem para cima",
                tag = "crop_pan_up",
                enabled = enabled,
                onDarkScrim = onDarkScrim,
                reduceTransparency = settings.reduceTransparency,
                onClick = { onNudge(0f, 0.12f) }
            )
            CropIconStepButton(
                icon = Icons.Default.CenterFocusStrong,
                label = "Centralizar recorte",
                tag = "crop_reset_position",
                enabled = enabled,
                onDarkScrim = onDarkScrim,
                reduceTransparency = settings.reduceTransparency,
                onClick = onReset
            )
            CropIconStepButton(
                icon = Icons.Default.KeyboardArrowDown,
                label = "Mover imagem para baixo",
                tag = "crop_pan_down",
                enabled = enabled,
                onDarkScrim = onDarkScrim,
                reduceTransparency = settings.reduceTransparency,
                onClick = { onNudge(0f, -0.12f) }
            )
            CropIconStepButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                label = "Mover imagem para direita",
                tag = "crop_pan_right",
                enabled = enabled,
                onDarkScrim = onDarkScrim,
                reduceTransparency = settings.reduceTransparency,
                onClick = { onNudge(-0.12f, 0f) }
            )
        }
    }
}

@Composable
private fun CropIconStepButton(
    icon: ImageVector,
    label: String,
    tag: String,
    enabled: Boolean,
    onDarkScrim: Boolean,
    reduceTransparency: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalIOS27Theme.current
    val interactions = remember { MutableInteractionSource() }
    val bg = when {
        onDarkScrim && reduceTransparency -> Color(0xFF2B2E3A)
        onDarkScrim -> Color.White.copy(alpha = 0.14f)
        reduceTransparency -> theme.bgPrimary
        else -> theme.bgPrimary.copy(alpha = 0.85f)
    }
    val fg = if (onDarkScrim) Color.White else readableForeground(theme.textPrimary, bg, 4.5f)
    val border = if (onDarkScrim) Color.White.copy(alpha = 0.24f) else theme.divider

    val surfaceModifier = when {
        reduceTransparency -> Modifier
            .clip(CircleShape)
            .background(bg.copy(alpha = if (enabled) bg.alpha else bg.alpha * 0.45f), CircleShape)
            .border(0.9.dp, border, CircleShape)
        onDarkScrim -> Modifier
            .clip(CircleShape)
            .liquidGlass(shape = CircleShape, surfaceAlpha = if (enabled) 0.16f else 0.08f)
            .border(0.75.dp, border, CircleShape)
        else -> Modifier
            .clip(CircleShape)
            .liquidGlassNavSurface(CircleShape)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .pressScaleFeedback(interactions, targetScale = 0.94f)
            .then(surfaceModifier)
            .focusable(enabled = enabled)
            .semantics(mergeDescendants = true) {
                contentDescription = label
            }
            .testTag(tag)
            .clickable(
                enabled = enabled,
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}
