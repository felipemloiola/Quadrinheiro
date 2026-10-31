package com.example.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.IOS27ThemeDef
import com.example.model.ThemeSchemeColors
import kotlin.math.min

@Immutable
data class AppleMaterialsComputed(
    val themeDef: IOS27ThemeDef,
    val scheme: ThemeSchemeColors,
    val liquidGlassNavFill: Color,
    val liquidGlassNavBorder: Color,
    val contentMaterialFill: Color,
    val contentMaterialBorder: Color,
    val inputWellFill: Color,
    val inputWellBorder: Color,
    val dividerColor: Color,
    val sheetCollapsedFill: Color,
    val sheetExpandedFill: Color,
    val sheetBorder: Color,
    val edgeRefraction: Boolean,
    val reduceTransparency: Boolean = false,
    val dynamicScale: Float,
    val boldText: Boolean,
    val ringOuterStart: Color,
    val ringOuterEnd: Color,
    val ringInnerStart: Color,
    val ringInnerEnd: Color
)

val LocalAppleMaterials = staticCompositionLocalOf {
    val def = IOS27ThemeDef.OCEAN_BLUE
    val s = def.dark
    AppleMaterialsComputed(
        themeDef = def,
        scheme = s,
        liquidGlassNavFill = Color(42, 45, 60, 96),
        liquidGlassNavBorder = Color(1f, 1f, 1f, 0.16f),
        contentMaterialFill = Color(28, 30, 40, 165),
        contentMaterialBorder = Color(1f, 1f, 1f, 0.10f),
        inputWellFill = Color(142, 142, 147, 46),
        inputWellBorder = Color(1f, 1f, 1f, 0.09f),
        dividerColor = Color(1f, 1f, 1f, 0.10f),
        sheetCollapsedFill = Color(28, 30, 40, 178),
        sheetExpandedFill = s.thickMaterialBg,
        sheetBorder = Color(1f, 1f, 1f, 0.13f),
        edgeRefraction = true,
        reduceTransparency = false,
        dynamicScale = 1f,
        boldText = false,
        ringOuterStart = Color(0xFF0A84FF),
        ringOuterEnd = Color(0xFF5E5CE6),
        ringInnerStart = Color(0xFF30D158),
        ringInnerEnd = Color(0xFF64D2FF)
    )
}

enum class ThemeMode(val id: String, val label: String) {
    LIGHT("LIGHT", "Claro"),
    DARK("DARK", "Escuro"),
    SYSTEM("SYSTEM", "Sistema");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.find { it.id == id } ?: SYSTEM
    }
}

enum class AppTheme(
    val id: String,
    val label: String,
    val swatchLight: Color,
    val swatchDark: Color
) {
    BLUE(
        id = "BLUE",
        label = "Azul",
        swatchLight = Color(0xFF2E7CF6),
        swatchDark = Color(0xFF4C94FF)
    ),
    WHITE(
        id = "WHITE",
        label = "Branco",
        swatchLight = Color(0xFFFFFFFF),
        swatchDark = Color(0xFFFFFFFF)
    ),
    BLACK(
        id = "BLACK",
        label = "Preto",
        swatchLight = Color(0xFF000000),
        swatchDark = Color(0xFF1C1C1E)
    ),
    RED(
        id = "RED",
        label = "Vermelho",
        swatchLight = Color(0xFFD9383A),
        swatchDark = Color(0xFFFF453A)
    ),
    GREEN(
        id = "GREEN",
        label = "Verde",
        swatchLight = Color(0xFF28A745),
        swatchDark = Color(0xFF32D74B)
    ),
    PURPLE(
        id = "PURPLE",
        label = "Roxo",
        swatchLight = Color(0xFF8B5CF6),
        swatchDark = Color(0xFFBF5AF2)
    ),
    ORANGE(
        id = "ORANGE",
        label = "Laranja",
        swatchLight = Color(0xFFE67E22),
        swatchDark = Color(0xFFFF9F0A)
    );

    val swatchColorLight: Color get() = swatchLight
    val swatchColorDark: Color get() = swatchDark

    companion object {
        fun fromId(id: String?): AppTheme {
            return when (id?.uppercase()) {
                "BLUE", "OCEAN_BLUE", "OCEANBLUE" -> BLUE
                "WHITE" -> WHITE
                "BLACK", "SAKURA_PINK" -> BLACK
                "RED", "CRIMSON_RED" -> RED
                "GREEN", "EMERALD_GREEN" -> GREEN
                "PURPLE", "ROYAL_PURPLE" -> PURPLE
                "ORANGE", "AMBER_GOLD" -> ORANGE
                else -> BLUE
            }
        }
    }
}

@Immutable
data class IOS27ThemeColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgTertiary: Color,
    val islandSurface: Color,
    val islandBorder: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val accentMuted: Color,
    val divider: Color,
    val isAllBlack: Boolean = false,
    val isAllWhite: Boolean = false,
    val appTheme: AppTheme = AppTheme.BLUE,
    val isDark: Boolean = true
)

typealias IOS27ThemeState = IOS27ThemeColors

fun getIOS27ThemeColors(appTheme: AppTheme, isDark: Boolean): IOS27ThemeColors {
    return when {
        // 1. MODO ESCURO + PRETO (ALL-BLACK OLED STEALTH)
        isDark && appTheme == AppTheme.BLACK -> IOS27ThemeColors(
            bgPrimary = Color(0xFF000000),
            bgSecondary = Color(0xFF0A0A0C),
            bgTertiary = Color(0xFF121215),
            islandSurface = Color(0xFF0A0A0D).copy(alpha = 0.94f),
            islandBorder = Color.White.copy(alpha = 0.07f),
            glassSurface = Color(0xFF101014).copy(alpha = 0.80f),
            glassBorder = Color.White.copy(alpha = 0.10f),
            textPrimary = Color(0xFFF5F5F7),
            textSecondary = Color(0xFF98989F),
            textTertiary = Color(0xFF5E5E66),
            accent = Color(0xFFD4D4D8), // Titânio claro para visibilidade perfeita sobre o All-Black
            accentSecondary = Color(0xFF8E8E93),
            accentGlow = Color.White.copy(alpha = 0.04f),
            accentMuted = Color(0xFF1C1C21),
            divider = Color.White.copy(alpha = 0.06f),
            isAllBlack = true,
            isAllWhite = false,
            appTheme = appTheme,
            isDark = true
        )

        // 2. MODO ESCURO + BRANCO (BRILLIANT WHITE ON GRAPHITE)
        isDark && appTheme == AppTheme.WHITE -> IOS27ThemeColors(
            bgPrimary = Color(0xFF121215),
            bgSecondary = Color(0xFF1E1E24),
            bgTertiary = Color(0xFF2A2A32),
            islandSurface = Color(0xFF1E1E24).copy(alpha = 0.88f),
            islandBorder = Color.White.copy(alpha = 0.18f),
            glassSurface = Color(0xFF25252C).copy(alpha = 0.75f),
            glassBorder = Color.White.copy(alpha = 0.24f),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB4B4BC),
            textTertiary = Color(0xFF7E7E87),
            accent = Color(0xFFFFFFFF), // Branco puro brilhante
            accentSecondary = Color(0xFFD4D4D8),
            accentGlow = Color.White.copy(alpha = 0.18f),
            accentMuted = Color.White.copy(alpha = 0.16f),
            divider = Color.White.copy(alpha = 0.10f),
            isAllBlack = false,
            isAllWhite = false,
            appTheme = appTheme,
            isDark = true
        )

        // 3. MODO CLARO + BRANCO (ALL-WHITE PEARL GLASS)
        !isDark && appTheme == AppTheme.WHITE -> IOS27ThemeColors(
            bgPrimary = Color(0xFFF9F9FB),
            bgSecondary = Color(0xFFFFFFFF),
            bgTertiary = Color(0xFFF1F1F5),
            islandSurface = Color(0xFFFFFFFF).copy(alpha = 0.96f),
            islandBorder = Color(0xFFE2E2E8),
            glassSurface = Color(0xFFFFFFFF).copy(alpha = 0.88f),
            glassBorder = Color(0xFFD8D8E0),
            textPrimary = Color(0xFF27272A),
            textSecondary = Color(0xFF71717A),
            textTertiary = Color(0xFFA1A1AA),
            accent = Color(0xFF52525B), // Titânio Prata Escuro (elegante, suave e 100% legível)
            accentSecondary = Color(0xFF8E8E93),
            accentGlow = Color(0xFFD4D4D8).copy(alpha = 0.25f),
            accentMuted = Color(0xFFEFEFF4),
            divider = Color(0xFFE8E8EE),
            isAllBlack = false,
            isAllWhite = true,
            appTheme = appTheme,
            isDark = false
        )

        // 4. MODO CLARO + PRETO (HIGH-CONTRAST EDITORIAL INK)
        !isDark && appTheme == AppTheme.BLACK -> IOS27ThemeColors(
            bgPrimary = Color(0xFFE4E4EA), // Fundo pedra arquitetónico mais denso
            bgSecondary = Color(0xFFF0F0F4), // Cards em tom papel/concreto claro
            bgTertiary = Color(0xFFD8D8E0),
            islandSurface = Color(0xFFF0F0F4).copy(alpha = 0.92f),
            islandBorder = Color.Black.copy(alpha = 0.16f), // Borda preta bem definida
            glassSurface = Color(0xFFE8E8EE).copy(alpha = 0.84f),
            glassBorder = Color.Black.copy(alpha = 0.20f),
            textPrimary = Color(0xFF000000),
            textSecondary = Color(0xFF3F3F46),
            textTertiary = Color(0xFF71717A),
            accent = Color(0xFF000000), // Preto Nanquim Absoluto
            accentSecondary = Color(0xFF3F3F46),
            accentGlow = Color.Black.copy(alpha = 0.14f),
            accentMuted = Color.Black.copy(alpha = 0.12f),
            divider = Color.Black.copy(alpha = 0.12f),
            isAllBlack = false,
            isAllWhite = false,
            appTheme = appTheme,
            isDark = false
        )

        // 5. CORES CROMÁTICAS (AZUL, VERMELHO, VERDE, ROXO, LARANJA)
        else -> {
            val accent = if (isDark) appTheme.swatchDark else appTheme.swatchLight
            val accentSecondary = if (isDark) {
                lerp(accent, Color.White, 0.28f)
            } else {
                lerp(accent, Color.Black, 0.15f)
            }

            if (isDark) {
                // Superfícies neutras translúcidas estilo iOS para que a iluminação da capa da HQ ao fundo se integre perfeitamente aos cards
                val bgPrimary = Color(0xFF0C0D10)
                val cardSurface = Color(0xFF1A1B20)
                val tertiarySurface = Color(0xFF26272E)
                IOS27ThemeColors(
                    bgPrimary = bgPrimary,
                    bgSecondary = cardSurface,
                    bgTertiary = tertiarySurface,
                    islandSurface = cardSurface.copy(alpha = 0.56f),
                    islandBorder = Color.White.copy(alpha = 0.12f),
                    glassSurface = cardSurface.copy(alpha = 0.48f),
                    glassBorder = Color.White.copy(alpha = 0.16f),
                    textPrimary = Color(0xFFF5F5F7),
                    textSecondary = Color(0xFFA8A8B0),
                    textTertiary = Color(0xFF72727A),
                    accent = accent,
                    accentSecondary = accentSecondary,
                    accentGlow = accent.copy(alpha = 0.24f),
                    accentMuted = accent.copy(alpha = 0.16f),
                    divider = Color.White.copy(alpha = 0.08f),
                    isAllBlack = false,
                    isAllWhite = false,
                    appTheme = appTheme,
                    isDark = true
                )
            } else {
                // Superfícies claras translúcidas estilo iOS
                val bgPrimary = Color(0xFFF2F2F7)
                val cardSurface = Color(0xFFFFFFFF)
                val tertiarySurface = Color(0xFFE5E5EA)
                IOS27ThemeColors(
                    bgPrimary = bgPrimary,
                    bgSecondary = cardSurface,
                    bgTertiary = tertiarySurface,
                    islandSurface = cardSurface.copy(alpha = 0.72f),
                    islandBorder = Color.Black.copy(alpha = 0.07f),
                    glassSurface = cardSurface.copy(alpha = 0.68f),
                    glassBorder = Color.Black.copy(alpha = 0.10f),
                    textPrimary = Color(0xFF1C1C1E),
                    textSecondary = Color(0xFF636366),
                    textTertiary = Color(0xFF8E8E93),
                    accent = accent,
                    accentSecondary = accentSecondary,
                    accentGlow = accent.copy(alpha = 0.18f),
                    accentMuted = accent.copy(alpha = 0.13f),
                    divider = Color.Black.copy(alpha = 0.07f),
                    isAllBlack = false,
                    isAllWhite = false,
                    appTheme = appTheme,
                    isDark = false
                )
            }
        }
    }
}

@Immutable
data class ThemeColorsState(
    val accent: Color = Color(0xFF0088FF),
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color(0xFF8E8E93),
    val baseBg: Color = Color(0xFF0B0D14)
)

val LocalIOS27Theme = staticCompositionLocalOf {
    getIOS27ThemeColors(AppTheme.BLUE, isDark = true)
}
val LocalIsDarkTheme = staticCompositionLocalOf { true }
val LocalThemeColors = staticCompositionLocalOf { ThemeColorsState() }
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

private val ThemeTransitionEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

@Composable
private fun animateThemeColor(target: Color, reduceMotion: Boolean): Color {
    val state by animateColorAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else 400,
            easing = ThemeTransitionEasing
        ),
        label = "themeColorTransition"
    )
    return state
}

private fun rgbWithAlpha(rgb: Triple<Int, Int, Int>, alpha: Float): Color {
    val a = (alpha.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
    return Color(rgb.first, rgb.second, rgb.third, a)
}

@Composable
fun QuadrinheiroTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    // SYSTEM_MONO_THEME_V7: explicit user choices always take precedence.
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (settings.colorScheme.lowercase()) {
        "system" -> systemDark
        "light" -> false
        else -> true
    }
    val appTheme = remember(settings.themeId, isDark) {
        if (settings.themeId == "SYSTEM_MONO") {
            if (isDark) AppTheme.WHITE else AppTheme.BLACK
        } else AppTheme.fromId(settings.themeId)
    }
    val themeState = remember(appTheme, isDark) { getIOS27ThemeColors(appTheme, isDark) }
    val themeDef = remember(settings.themeId) { if (settings.themeId == "SYSTEM_MONO") IOS27ThemeDef.MONOCHROME else IOS27ThemeDef.fromId(settings.themeId) }
    val rawScheme = remember(themeDef, isDark, themeState) {
        val baseScheme = if (isDark) themeDef.dark else themeDef.light
        baseScheme.copy(
            baseBg = themeState.bgPrimary,
            thickMaterialBg = themeState.bgSecondary,
            textPrimary = themeState.textPrimary,
            textSecondary = themeState.textSecondary,
            textTertiary = themeState.textTertiary,
            accentColor = themeState.accent,
            accentBg = themeState.accent
        )
    }
    val reduceMotion = settings.reduceMotion

    // Smooth 400ms cubic-bezier transition for key scheme colors
    val animatedBaseBg = animateThemeColor(rawScheme.baseBg, reduceMotion)
    val animatedThickMaterialBg = animateThemeColor(rawScheme.thickMaterialBg, reduceMotion)
    val animatedTextPrimary = animateThemeColor(rawScheme.textPrimary, reduceMotion)
    val animatedTextSecondary = animateThemeColor(rawScheme.textSecondary, reduceMotion)
    val animatedTextTertiary = animateThemeColor(rawScheme.textTertiary, reduceMotion)
    val animatedAccentColor = animateThemeColor(rawScheme.accentColor, reduceMotion)
    val animatedAccentBg = animateThemeColor(rawScheme.accentBg, reduceMotion)
    val animatedAccentText = animateThemeColor(rawScheme.accentText, reduceMotion)
    val animatedPillActiveBg = animateThemeColor(rawScheme.pillActiveBg, reduceMotion)

    val scheme = remember(
        rawScheme,
        animatedBaseBg,
        animatedThickMaterialBg,
        animatedTextPrimary,
        animatedTextSecondary,
        animatedTextTertiary,
        animatedAccentColor,
        animatedAccentBg,
        animatedAccentText,
        animatedPillActiveBg
    ) {
        rawScheme.copy(
            baseBg = animatedBaseBg,
            thickMaterialBg = animatedThickMaterialBg,
            textPrimary = animatedTextPrimary,
            textSecondary = animatedTextSecondary,
            textTertiary = animatedTextTertiary,
            accentColor = animatedAccentColor,
            accentBg = animatedAccentBg,
            accentText = animatedAccentText,
            pillActiveBg = animatedPillActiveBg
        )
    }

    val levelRatio = (settings.glassLevel.toFloat() / 100f).coerceIn(0f, 1f)
    val isClearVariant = settings.glassVariant == "clear"
    val reduceTransp = settings.reduceTransparency
    val highContrast = settings.increaseContrast

    val navBaseAlpha = when {
        reduceTransp -> 0.92f
        isClearVariant -> if (scheme.isLight) 0.50f + levelRatio * 0.24f else 0.42f + levelRatio * 0.24f
        else -> if (scheme.isLight) 0.62f + levelRatio * 0.20f else 0.50f + levelRatio * 0.22f
    }

    val contentAlpha = when {
        reduceTransp -> 0.90f
        scheme.isLight -> 0.60f + levelRatio * 0.18f
        else -> 0.36f + levelRatio * 0.16f
    }

    val inputAlpha = if (scheme.isLight) {
        if (highContrast) 0.12f else 0.065f
    } else {
        if (highContrast) 0.22f else 0.13f
    }

    val dynamicScale = (settings.dynamicTypeScale / 100f).coerceIn(0.85f, 1.15f)

    val animatedNavFill = animateThemeColor(themeState.glassSurface.copy(alpha = navBaseAlpha), reduceMotion)
    val animatedNavBorder = animateThemeColor(themeState.glassBorder, reduceMotion)
    val animatedContentFill = animateThemeColor(themeState.islandSurface.copy(alpha = contentAlpha), reduceMotion)
    val animatedContentBorder = animateThemeColor(themeState.islandBorder, reduceMotion)
    val animatedInputFill = animateThemeColor(
        if (scheme.isLight) Color.Black.copy(alpha = inputAlpha) else Color.White.copy(alpha = inputAlpha * 0.55f),
        reduceMotion
    )
    val animatedDivider = animateThemeColor(themeState.divider, reduceMotion)
    val animatedRingOuterStart = animateThemeColor(themeState.accent, reduceMotion)
    val animatedRingOuterEnd = animateThemeColor(themeState.accentSecondary, reduceMotion)

    val computed = remember(
        themeDef,
        scheme,
        animatedNavFill,
        animatedNavBorder,
        animatedContentFill,
        animatedContentBorder,
        animatedInputFill,
        animatedDivider,
        themeState,
        contentAlpha,
        settings.edgeRefraction,
        reduceTransp,
        dynamicScale,
        settings.boldText,
        animatedRingOuterStart,
        animatedRingOuterEnd
    ) {
        AppleMaterialsComputed(
            themeDef = themeDef,
            scheme = scheme,
            liquidGlassNavFill = animatedNavFill,
            liquidGlassNavBorder = animatedNavBorder,
            contentMaterialFill = animatedContentFill,
            contentMaterialBorder = animatedContentBorder,
            inputWellFill = animatedInputFill,
            inputWellBorder = animatedNavBorder.copy(alpha = 0.15f),
            dividerColor = animatedDivider,
            sheetCollapsedFill = themeState.islandSurface.copy(alpha = min(0.76f, contentAlpha)),
            sheetExpandedFill = themeState.bgSecondary,
            sheetBorder = animatedContentBorder,
            edgeRefraction = settings.edgeRefraction,
            reduceTransparency = reduceTransp,
            dynamicScale = dynamicScale,
            boldText = settings.boldText,
            ringOuterStart = animatedRingOuterStart,
            ringOuterEnd = animatedRingOuterEnd,
            ringInnerStart = animatedRingOuterEnd,
            ringInnerEnd = animatedRingOuterStart
        )
    }

    val colorScheme = remember(scheme) {
        if (!scheme.isLight) {
            darkColorScheme(
                primary = scheme.accentColor,
                background = scheme.baseBg,
                surface = scheme.thickMaterialBg,
                onPrimary = scheme.accentText,
                onBackground = scheme.textPrimary,
                onSurface = scheme.textPrimary
            )
        } else {
            lightColorScheme(
                primary = scheme.accentColor,
                background = scheme.baseBg,
                surface = scheme.thickMaterialBg,
                onPrimary = scheme.accentText,
                onBackground = scheme.textPrimary,
                onSurface = scheme.textPrimary
            )
        }
    }

    // APP_HIG_THEME_SCALE_V12: LocalDensity applies the user scale exactly once.
    val typography = remember(dynamicScale, settings.boldText) {
        buildQuadrinheiroTypography(
            scaleFactor = 1f,
            forceBold = settings.boldText
        )
    }

    val themeColorsState = remember(themeState) {
        ThemeColorsState(
            accent = themeState.accent,
            textPrimary = themeState.textPrimary,
            textSecondary = themeState.textSecondary,
            baseBg = themeState.bgPrimary
        )
    }

    val context = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val baseDensity = LocalDensity.current

    val defaultFontResolver = LocalFontFamilyResolver.current
    val customFontResolver = remember(settings.boldText, baseConfig, defaultFontResolver) {
        if (!settings.boldText) {
            defaultFontResolver
        } else {
            val cfg = Configuration(baseConfig)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                cfg.fontWeightAdjustment = 300
            }
            createFontFamilyResolver(context.createConfigurationContext(cfg))
        }
    }

    val customDensity = remember(baseDensity, dynamicScale, settings.boldText) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * dynamicScale
        )
    }

    CompositionLocalProvider(
        LocalAppleMaterials provides computed,
        LocalAppSettings provides settings,
        LocalIOS27Theme provides themeState,
        LocalIsDarkTheme provides isDark,
        LocalThemeColors provides themeColorsState,
        LocalFontFamilyResolver provides customFontResolver,
        LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            ProvideTextStyle(
                value = typography.bodyLarge.copy(
                    fontFamily = AppSansFamily,
                    fontWeight = if (settings.boldText) FontWeight.SemiBold else FontWeight.Normal,
                    fontSynthesis = FontSynthesis.None,
                    letterSpacing = 0.sp,
                    shadow = null
                ),
                content = content
            )
        }
    }
}
