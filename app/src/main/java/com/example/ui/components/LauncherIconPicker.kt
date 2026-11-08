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
    val currentColorScheme = appSettings?.colorScheme ?: "system"

    // Determina o estilo do ícone correspondente
    val activeStyle = when (currentMode) {
        "original", "default" -> LauncherIconStyle.DEFAULT
        else -> LauncherIconStyle.fromThemeId(currentThemeId)
    }

    // Cor do fundo do preview:
    // Original: fundo neutro do ícone clássico (#1C1C1E no escuro / #F3F4F7 no claro)
    // Tema: fundo do tema ativo do app (claro ou escuro)
    val isThemeMode = currentMode == "theme"
    val previewBgColor by animateColorAsState(
        targetValue = if (!isThemeMode) {
            if (theme.isDark) Color(0xFF1C1C1E) else Color(0xFFF3F4F7)
        } else {
            if (theme.isDark) Color(0xFF101010) else Color(0xFFF3F4F7)
        },
        animationSpec = tween(durationMillis = 200),
        label = "launcher_preview_bg"
    )

    // Original: sempre exibe o logotipo colorido cromático original (QuadrinheiroLogoVariant.Primary)
    // Tema: exibe o "Q" colorido com a cor de destaque (theme.accent) sobre o fundo do tema ativo
    val logoVariant = if (!isThemeMode) {
        QuadrinheiroLogoVariant.Primary
    } else {
        QuadrinheiroLogoVariant.OnDark
    }

    fun applyMode(newMode: String) {
        LauncherAppearance.setLauncherMode(context, newMode, currentThemeId, currentColorScheme)
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
                    .border(0.5.dp, if (theme.isDark) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(12.dp)),
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
                    text = if (isThemeMode) {
                        "Tema ativo (${if (theme.isDark) "Escuro" else "Claro"} • ${activeStyle.label})"
                    } else {
                        "Original colorido"
                    },
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            AppleMenuPicker(
                value = if (isThemeMode) "theme" else "original",
                onChange = { selected ->
                    applyMode(selected)
                },
                options = listOf(
                    SegmentOption("original", "Original"),
                    SegmentOption("theme", "Tema")
                ),
                modifier = Modifier.widthIn(max = 160.dp).testTag("launcher_icon_style")
            )
        }
    }
}
