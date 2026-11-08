package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LauncherAppearance
import com.example.LauncherIconStyle
import com.example.model.AppSettings
import com.example.ui.theme.LocalIOS27Theme

/**
 * Seletor de estilo e cor do ícone do aplicativo com preview em tempo real.
 * Permite escolher: Original, Seguir Tema da interface, ou escolher diretamente
 * entre as cores de destaque disponíveis (Preto, Branco, Roxo, Azul, Verde, Laranja, Vermelho).
 */
@Composable
fun LauncherIconPicker(
    appSettings: AppSettings? = null,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit = {}
) {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current

    val currentMode = appSettings?.launcherIconMode ?: remember { LauncherAppearance.selectedMode(context) }
    val currentThemeId = appSettings?.themeId ?: "SYSTEM_MONO"

    // Determina o estilo do ícone correspondente
    val activeStyle = when (currentMode) {
        "original", "default" -> LauncherIconStyle.DEFAULT
        "theme" -> LauncherIconStyle.fromThemeId(currentThemeId)
        else -> LauncherIconStyle.fromId(currentMode)
    }

    // Cor do fundo do preview baseada no estilo ativo
    val previewBgColor by animateColorAsState(
        targetValue = when (activeStyle) {
            LauncherIconStyle.DEFAULT -> if (theme.isDark) Color(0xFF1C1C1E) else Color(0xFFF3F4F7)
            LauncherIconStyle.BLACK -> Color(0xFF101010)
            LauncherIconStyle.WHITE -> Color(0xFFF8F9FA)
            LauncherIconStyle.PURPLE -> Color(0xFF8B5CF6)
            LauncherIconStyle.BLUE -> Color(0xFF2E7CF6)
            LauncherIconStyle.GREEN -> Color(0xFF28A745)
            LauncherIconStyle.ORANGE -> Color(0xFFE67E22)
            LauncherIconStyle.RED -> Color(0xFFD9383A)
        },
        animationSpec = tween(durationMillis = 200),
        label = "launcher_preview_bg"
    )

    val logoVariant = when (activeStyle) {
        LauncherIconStyle.DEFAULT -> if (theme.isDark) QuadrinheiroLogoVariant.OnDark else QuadrinheiroLogoVariant.Primary
        LauncherIconStyle.WHITE -> QuadrinheiroLogoVariant.Primary
        else -> QuadrinheiroLogoVariant.OnDark
    }

    fun applyMode(newMode: String) {
        LauncherAppearance.setLauncherMode(context, newMode, currentThemeId)
        onUpdateSettings { it.copy(launcherIconMode = newMode) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .contentMaterialSurface(RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewBgColor)
                    .border(0.5.dp, if (activeStyle == LauncherIconStyle.WHITE) Color(0x33000000) else Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                QuadrinheiroLogo(logoVariant, Modifier.size(34.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Ícone do aplicativo",
                    color = theme.textPrimary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (currentMode) {
                        "original", "default" -> "Padrão monocromático"
                        "theme" -> "Seguindo o tema (${activeStyle.label})"
                        else -> "Cor: ${activeStyle.label}"
                    },
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            AppleMenuPicker(
                value = when (currentMode) {
                    "original", "default" -> "original"
                    "theme" -> "theme"
                    else -> "custom"
                },
                onChange = { selected ->
                    when (selected) {
                        "original" -> applyMode("original")
                        "theme" -> applyMode("theme")
                        "custom" -> applyMode(activeStyle.takeIf { it != LauncherIconStyle.DEFAULT }?.id ?: "blue")
                    }
                },
                options = listOf(
                    SegmentOption("original", "Original"),
                    SegmentOption("theme", "Tema"),
                    SegmentOption("custom", "Cores")
                ),
                modifier = Modifier.widthIn(max = 160.dp).testTag("launcher_icon_style")
            )
        }

        // Paleta de cores selecionáveis quando no modo "custom" ou para troca rápida
        val colorStyles = remember {
            listOf(
                LauncherIconStyle.BLACK,
                LauncherIconStyle.WHITE,
                LauncherIconStyle.PURPLE,
                LauncherIconStyle.BLUE,
                LauncherIconStyle.GREEN,
                LauncherIconStyle.ORANGE,
                LauncherIconStyle.RED
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colorStyles.forEach { style ->
                val isSelected = currentMode == style.id || (currentMode == "theme" && activeStyle == style)
                val colorValue = when (style) {
                    LauncherIconStyle.BLACK -> Color(0xFF101010)
                    LauncherIconStyle.WHITE -> Color(0xFFF3F4F7)
                    LauncherIconStyle.PURPLE -> Color(0xFF8B5CF6)
                    LauncherIconStyle.BLUE -> Color(0xFF2E7CF6)
                    LauncherIconStyle.GREEN -> Color(0xFF28A745)
                    LauncherIconStyle.ORANGE -> Color(0xFFE67E22)
                    LauncherIconStyle.RED -> Color(0xFFD9383A)
                    else -> Color(0xFF2E7CF6)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorValue)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) theme.accent else (if (style == LauncherIconStyle.WHITE) Color(0x33000000) else Color(0x22FFFFFF)),
                            shape = CircleShape
                        )
                        .clickable {
                            applyMode(style.id)
                        }
                        .testTag("launcher_color_${style.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (style == LauncherIconStyle.WHITE) Color.Black else Color.White)
                        )
                    }
                }
            }
        }
    }
}
