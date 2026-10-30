package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.PlusJakartaSansFamily

@Immutable
enum class QuadrinheiroLogoVariant {
    Primary,        // logo original (para tela de login)
    OnDark,         // logo adaptada para fundo escuro
    OnLight,        // logo adaptada para fundo claro
    Badge           // ícone compacto usado em rodapés / cabeçalhos
}

/**
 * Logo principal do Quadrinheiro, com variações adaptadas ao tema atual.
 * - Primary: versão colorida completa (padrão para tela de login).
 * - OnDark / OnLight: ajustam contraste conforme tema.
 * - Badge: ícone compacto com fundo que se adapta ao esquema de cores (usado fora da Auth).
 */
@Composable
fun QuadrinheiroLogo(
    variant: QuadrinheiroLogoVariant = QuadrinheiroLogoVariant.Primary,
    modifier: Modifier = Modifier,
    titleVisible: Boolean = true,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null
) {
    val theme = LocalIOS27Theme.current
    val isDark = LocalIsDarkTheme.current

    val badgeBg: Color = when (variant) {
        QuadrinheiroLogoVariant.Badge -> {
            if (isDark) theme.bgElevatedStrong else theme.bgElevatedSoft
        }
        QuadrinheiroLogoVariant.OnDark -> Color.White.copy(alpha = 0.10f)
        QuadrinheiroLogoVariant.OnLight -> Color.Black.copy(alpha = 0.06f)
        QuadrinheiroLogoVariant.Primary -> Color.Transparent
    }

    val badgeBorderColor: Color = when (variant) {
        QuadrinheiroLogoVariant.Badge -> theme.borderStrong
        else -> Color.Transparent
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Badge / ícone adaptado ao tema
        Box(
            modifier = Modifier
                .size(40.dp) // container menor para evitar "vazar" visualmente
                .clip(RoundedCornerShape(12.dp))
                .background(badgeBg)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp) // logo de fato (reduzida)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        color = when {
                            variant == QuadrinheiroLogoVariant.Primary -> theme.accent
                            variant == QuadrinheiroLogoVariant.OnDark -> theme.accentSoft
                            variant == QuadrinheiroLogoVariant.OnLight -> theme.accentStrong
                            else -> theme.accent
                        }
                    )
            ) {
                // Aqui ficaria o conteúdo vetorial da marca se/quando existir
            }
        }

        if (titleVisible || !subtitle.isNullOrBlank() || leadingIcon != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = theme.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (titleVisible) {
                        Text(
                            text = "Quadrinheiro",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = theme.textPrimary
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                }
            }
        }
    }
}
