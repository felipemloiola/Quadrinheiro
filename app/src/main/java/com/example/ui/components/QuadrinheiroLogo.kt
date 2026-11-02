package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily

/**
 * Primary = cores originais da marca (usar somente na tela de login).
 * Badge/OnDark/OnLight = marca nas cores de destaque do tema atual.
 */
enum class QuadrinheiroLogoVariant { Primary, OnDark, OnLight, Badge }

/**
 * Família de fonte oficial extraída diretamente dos assets (SF Pro Display Bold)
 * para renderizar a marca "Quadrinheiro" exatamente igual ao card de compartilhamento.
 */
val QuadrinheiroBrandFontFamily = FontFamily(
    Font(R.font.sf_pro_display_bold, FontWeight.Bold),
    Font(R.font.sf_pro_display_bold, FontWeight.ExtraBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Black)
)

/**
 * Tipografia oficial e padronizada da marca "Quadrinheiro",
 * COPIADA EXATAMENTE (1:1) do motor de renderização `drawLetterboxdBrandLockupOnCanvas`
 * do Card de Compartilhamento Social (`R.font.sf_pro_display_bold`, `Paint.Style.FILL_AND_STROKE`,
 * `strokeWidth = textSizePx * (0.9f / 44f)`, `letterSpacing = -0.03f`).
 */
@Composable
fun QuadrinheiroBrandText(
    text: String = "Quadrinheiro",
    fontSize: TextUnit = 18.sp,
    color: Color = LocalIOS27Theme.current.textPrimary,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val textSizePx = with(density) { fontSize.toPx() }
    val sfBoldTypeface = remember(context) {
        runCatching {
            androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sf_pro_display_bold)
        }.getOrNull() ?: android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }
    val brandTextPaint = remember(sfBoldTypeface, textSizePx, color) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.argb(
                (color.alpha * 255f).toInt().coerceIn(0, 255),
                (color.red * 255f).toInt().coerceIn(0, 255),
                (color.green * 255f).toInt().coerceIn(0, 255),
                (color.blue * 255f).toInt().coerceIn(0, 255)
            )
            this.textSize = textSizePx
            this.typeface = sfBoldTypeface
            this.style = android.graphics.Paint.Style.FILL_AND_STROKE
            this.strokeWidth = textSizePx * (0.9f / 44f)
            this.textAlign = android.graphics.Paint.Align.LEFT
            this.letterSpacing = -0.03f
        }
    }

    Text(
        text = text,
        fontSize = fontSize,
        fontFamily = QuadrinheiroBrandFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = TextUnit(-0.025f, TextUnitType.Em),
        color = Color.Transparent,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.drawWithContent {
            val measuredW = brandTextPaint.measureText(text)
            val startX = (size.width - measuredW) / 2f
            val baselineY = size.height / 2f - (brandTextPaint.descent() + brandTextPaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(text, startX, baselineY, brandTextPaint)
        }
    )
}

@Composable
// ADAPTIVE_LOGO_BADGE_V8: opt-in for Settings only.
private fun LogoMark(variant: QuadrinheiroLogoVariant, modifier: Modifier, adaptiveBackground: Boolean = false) {
    val theme = LocalIOS27Theme.current
    val original = variant == QuadrinheiroLogoVariant.Primary
    val accent = theme.accent
    val paths = remember {
        listOf(
            "M44.8,26.3 L28.9,32.9 L25,54 L31.6,75.1 L39.5,69.8 L42.8,60.6 L40.1,52.7 L43.4,39.5 Z",
            "M49.4,26.3 L67.2,26.3 L80.4,35.5 L80.4,50.7 L63.9,50.7 L61.9,44.8 L53.3,38.8 L48.1,39.5 Z",
            "M63.9,55.3 L79.1,55.3 L76.4,63.9 L85.7,77.8 L83.7,83.7 L69.2,78.4 L55.3,80.4 L40.1,77.1 L46.1,65.9 L54,63.2 Z",
            "M44.8,26.3 L28.9,32.9 L25,54 L40.1,52.7 L43.4,39.5 Z",
            "M63.9,55.3 L79.1,55.3 L76.4,63.9 L85.7,77.8 L83.7,83.7 L54,63.2 Z"
        ).map { PathParser().parsePathString(it).toPath() }
    }
    val brushes = remember(original, accent) {
        if (original) listOf(
            Brush.linearGradient(listOf(Color(0xFFFF1493), Color(0xFFE11D48), Color(0xFF881337)), Offset(25f, 26.3f), Offset(44.8f, 75.1f)),
            Brush.linearGradient(listOf(Color(0xFFFFD000), Color(0xFFFF9100), Color(0xFFFF3D00)), Offset(49.4f, 26.3f), Offset(80.4f, 50.7f)),
            Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF00B4D8), Color(0xFF00F5FF)), Offset(40.1f, 63.2f), Offset(85.7f, 83.7f))
        ) else listOf(
            Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.65f))),
            Brush.linearGradient(listOf(accent.copy(alpha = 0.75f), accent)),
            Brush.linearGradient(listOf(accent.copy(alpha = 0.90f), accent.copy(alpha = 0.55f)))
        )
    }
    val sized = if (modifier == Modifier) Modifier.size(64.dp) else modifier
    val framed = if (variant == QuadrinheiroLogoVariant.Badge) {
        sized.background(
            if (adaptiveBackground && theme.isDark) {
                Color(0xFF000000)
            } else if (theme.isDark) {
                Color(0xFF000000)
            } else {
                theme.bgPrimary
            },
            RoundedCornerShape(12.dp)
        )
    } else sized
    Box(framed, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val glyphWidth = 60.7f
            val glyphHeight = 57.4f
            val glyphCenterX = 55.35f
            val glyphCenterY = 55.0f

            val fillRatio = if (variant == QuadrinheiroLogoVariant.Badge) 0.62f else 0.94f
            val scaleFactor = minOf(size.width / glyphWidth, size.height / glyphHeight) * fillRatio

            val cx = size.width / 2f
            val cy = size.height / 2f
            withTransform({
                translate(left = cx - glyphCenterX * scaleFactor, top = cy - glyphCenterY * scaleFactor)
                scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            }) {
                for (i in 0..2) drawPath(paths[i], brushes[i])
                drawPath(paths[3], Brush.linearGradient(listOf(Color.White.copy(alpha = 0.24f), Color.Transparent), Offset(28.9f, 32.9f), Offset(40.1f, 52.7f)))
                drawPath(paths[4], Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent), Offset(63.9f, 55.3f), Offset(85.7f, 83.7f)))
            }
        }
    }
}

@Composable
fun QuadrinheiroLogo(
    variant: QuadrinheiroLogoVariant = QuadrinheiroLogoVariant.Badge,
    modifier: Modifier = Modifier,
    titleVisible: Boolean = false,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    adaptiveBadgeBackground: Boolean = false
) {
    if (!titleVisible && subtitle.isNullOrBlank() && leadingIcon == null) {
        LogoMark(variant, modifier, adaptiveBadgeBackground)
        return
    }
    val theme = LocalIOS27Theme.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LogoMark(variant, Modifier.size(40.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                if (titleVisible) {
                    QuadrinheiroBrandText(
                        text = "Quadrinheiro",
                        fontSize = 18.sp,
                        color = theme.textPrimary
                    )
                }
            }
            if (!subtitle.isNullOrBlank()) Text(subtitle, fontFamily = PlusJakartaSansFamily, fontSize = 12.sp, color = theme.textSecondary)
        }
    }
}
