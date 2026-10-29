package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

enum class ReadingStatus(val key: String, val label: String, val shortLabel: String) {
    READ("READ", "Lido", "Lido"),
    READING("READING", "Lendo", "Lendo"),
    WANT_TO_READ("WANT_TO_READ", "Quero Ler", "Fila");

    companion object {
        fun fromKey(key: String): ReadingStatus =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: READ
    }
}

@Immutable
data class ThemeSchemeColors(
    val isLight: Boolean,
    val baseBg: Color,
    val gradTopRight: Color,
    val gradBottomLeft: Color,
    val gradLinearStart: Color,
    val gradLinearMid: Color,
    val gradLinearEnd: Color,
    val ribbonA: Color,
    val ribbonB: Color,
    val liquidGlassRgb: Triple<Int, Int, Int>,
    val contentMaterialRgb: Triple<Int, Int, Int>,
    val thickMaterialBg: Color,
    val inputRgb: Triple<Int, Int, Int>,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentColor: Color,
    val accentBg: Color,
    val accentText: Color,
    val pillActiveBg: Color,
    val starColor: Color,
    val swatchStart: Color,
    val swatchEnd: Color
)

enum class IOS27ThemeDef(
    val id: String,
    val themeName: String,
    val subtitle: String,
    val dark: ThemeSchemeColors,
    val light: ThemeSchemeColors
) {
    OCEAN_BLUE(
        id = "oceanBlue",
        themeName = "Azul Oceano",
        subtitle = "Safira profundo & azul oceano",
        dark = ThemeSchemeColors(
            isLight = false,
            baseBg = Color(0xFF071426),
            gradTopRight = Color(0x700A84FF),
            gradBottomLeft = Color(0x80034280),
            gradLinearStart = Color(0xFF0D223E),
            gradLinearMid = Color(0xFF071426),
            gradLinearEnd = Color(0xFF030914),
            ribbonA = Color(0x350A84FF),
            ribbonB = Color(0x15FFFFFF),
            liquidGlassRgb = Triple(16, 44, 78),
            contentMaterialRgb = Triple(10, 28, 52),
            thickMaterialBg = Color(0xFF0D1D36),
            inputRgb = Triple(30, 68, 114),
            textPrimary = Color(0xFFF5FBFF),
            textSecondary = Color(0xCCE0F2FE),
            textTertiary = Color(0x88BAE6FD),
            accentColor = Color(0xFF0A84FF),
            accentBg = Color(0xFF0A84FF),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x330A84FF),
            starColor = Color(0xFFFFD60A),
            swatchStart = Color(0xFF0A84FF),
            swatchEnd = Color(0xFF022B5C)
        ),
        light = ThemeSchemeColors(
            isLight = true,
            baseBg = Color(0xFFE5F1FC),
            gradTopRight = Color(0x6060A5FA),
            gradBottomLeft = Color(0x70BFDBFE),
            gradLinearStart = Color(0xFFF0F7FE),
            gradLinearMid = Color(0xFFE5F1FC),
            gradLinearEnd = Color(0xFFD0E5FA),
            ribbonA = Color(0xB8FFFFFF),
            ribbonB = Color(0x383B82F6),
            liquidGlassRgb = Triple(245, 250, 255),
            contentMaterialRgb = Triple(236, 245, 254),
            thickMaterialBg = Color(0xFFD6E9FA),
            inputRgb = Triple(60, 110, 170),
            textPrimary = Color(0xFF0B253A),
            textSecondary = Color(0xA60B253A),
            textTertiary = Color(0x6E0B253A),
            accentColor = Color(0xFF007AFF),
            accentBg = Color(0xFF007AFF),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x22007AFF),
            starColor = Color(0xFFD98200),
            swatchStart = Color(0xFFF0F7FE),
            swatchEnd = Color(0xFF7CB8F8)
        )
    ),
    CARMINE_RED(
        id = "carmineRed",
        themeName = "Vermelho Carmim",
        subtitle = "Apple Music Red & rubi intenso",
        dark = ThemeSchemeColors(
            isLight = false,
            baseBg = Color(0xFF1E080C),
            gradTopRight = Color(0x70FF375F),
            gradBottomLeft = Color(0x80800A1C),
            gradLinearStart = Color(0xFF2C0F15),
            gradLinearMid = Color(0xFF1E080C),
            gradLinearEnd = Color(0xFF100306),
            ribbonA = Color(0x35FF375F),
            ribbonB = Color(0x15FFFFFF),
            liquidGlassRgb = Triple(74, 18, 28),
            contentMaterialRgb = Triple(48, 12, 18),
            thickMaterialBg = Color(0xFF2C0F15),
            inputRgb = Triple(108, 30, 42),
            textPrimary = Color(0xFFFFF0F2),
            textSecondary = Color(0xCCFFD6DC),
            textTertiary = Color(0x88FFA8B6),
            accentColor = Color(0xFFFF375F),
            accentBg = Color(0xFFFF375F),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x33FF375F),
            starColor = Color(0xFFFFD60A),
            swatchStart = Color(0xFFFF375F),
            swatchEnd = Color(0xFF4A0812)
        ),
        light = ThemeSchemeColors(
            isLight = true,
            baseBg = Color(0xFFFCE8EB),
            gradTopRight = Color(0x60F87171),
            gradBottomLeft = Color(0x70FECDD3),
            gradLinearStart = Color(0xFFFFF5F6),
            gradLinearMid = Color(0xFFFCE8EB),
            gradLinearEnd = Color(0xFFF8D0D6),
            ribbonA = Color(0xB8FFFFFF),
            ribbonB = Color(0x38EF4444),
            liquidGlassRgb = Triple(255, 248, 249),
            contentMaterialRgb = Triple(254, 240, 242),
            thickMaterialBg = Color(0xFFF9D6DC),
            inputRgb = Triple(168, 50, 68),
            textPrimary = Color(0xFF33080F),
            textSecondary = Color(0xA633080F),
            textTertiary = Color(0x6E33080F),
            accentColor = Color(0xFFFA233B),
            accentBg = Color(0xFFFA233B),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x22FA233B),
            starColor = Color(0xFFD98200),
            swatchStart = Color(0xFFFFF5F6),
            swatchEnd = Color(0xFFF87586)
        )
    ),
    EMERALD_GREEN(
        id = "emeraldGreen",
        themeName = "Verde Esmeralda",
        subtitle = "Esmeralda puro & menta viva",
        dark = ThemeSchemeColors(
            isLight = false,
            baseBg = Color(0xFF06180E),
            gradTopRight = Color(0x7030D158),
            gradBottomLeft = Color(0x800B4D24),
            gradLinearStart = Color(0xFF0F2C1A),
            gradLinearMid = Color(0xFF06180E),
            gradLinearEnd = Color(0xFF020E07),
            ribbonA = Color(0x3530D158),
            ribbonB = Color(0x15FFFFFF),
            liquidGlassRgb = Triple(18, 58, 32),
            contentMaterialRgb = Triple(12, 38, 20),
            thickMaterialBg = Color(0xFF0B2616),
            inputRgb = Triple(34, 88, 52),
            textPrimary = Color(0xFFF0FDF4),
            textSecondary = Color(0xCCD1FAE5),
            textTertiary = Color(0x88A7F3D0),
            accentColor = Color(0xFF30D158),
            accentBg = Color(0xFF30D158),
            accentText = Color(0xFF021B08),
            pillActiveBg = Color(0x3330D158),
            starColor = Color(0xFFFFD60A),
            swatchStart = Color(0xFF30D158),
            swatchEnd = Color(0xFF063316)
        ),
        light = ThemeSchemeColors(
            isLight = true,
            baseBg = Color(0xFFE6F8ED),
            gradTopRight = Color(0x604ADE80),
            gradBottomLeft = Color(0x70BBF7D0),
            gradLinearStart = Color(0xFFF2FBF5),
            gradLinearMid = Color(0xFFE6F8ED),
            gradLinearEnd = Color(0xFFD1F2DE),
            ribbonA = Color(0xB8FFFFFF),
            ribbonB = Color(0x3822C55E),
            liquidGlassRgb = Triple(248, 255, 250),
            contentMaterialRgb = Triple(238, 252, 243),
            thickMaterialBg = Color(0xFFD3F2DF),
            inputRgb = Triple(40, 130, 70),
            textPrimary = Color(0xFF062B14),
            textSecondary = Color(0xA6062B14),
            textTertiary = Color(0x6E062B14),
            accentColor = Color(0xFF1DB954),
            accentBg = Color(0xFF1DB954),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x221DB954),
            starColor = Color(0xFFD98200),
            swatchStart = Color(0xFFF2FBF5),
            swatchEnd = Color(0xFF72D996)
        )
    ),
    ROYAL_PURPLE(
        id = "royalPurple",
        themeName = "Roxo Imperial",
        subtitle = "Ametista imperial & lavanda luminosa",
        dark = ThemeSchemeColors(
            isLight = false,
            baseBg = Color(0xFF160924),
            gradTopRight = Color(0x70BF5AF2),
            gradBottomLeft = Color(0x804A1878),
            gradLinearStart = Color(0xFF26103D),
            gradLinearMid = Color(0xFF160924),
            gradLinearEnd = Color(0xFF0B0312),
            ribbonA = Color(0x35BF5AF2),
            ribbonB = Color(0x15FFFFFF),
            liquidGlassRgb = Triple(60, 22, 94),
            contentMaterialRgb = Triple(38, 14, 60),
            thickMaterialBg = Color(0xFF241038),
            inputRgb = Triple(92, 38, 140),
            textPrimary = Color(0xFFFAF5FF),
            textSecondary = Color(0xCCE9D5FF),
            textTertiary = Color(0x88D8B4FE),
            accentColor = Color(0xFFBF5AF2),
            accentBg = Color(0xFFBF5AF2),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x33BF5AF2),
            starColor = Color(0xFFFFD60A),
            swatchStart = Color(0xFFBF5AF2),
            swatchEnd = Color(0xFF2D0A4E)
        ),
        light = ThemeSchemeColors(
            isLight = true,
            baseBg = Color(0xFFF3E8FC),
            gradTopRight = Color(0x60C084FC),
            gradBottomLeft = Color(0x70E9D5FF),
            gradLinearStart = Color(0xFFFAF5FF),
            gradLinearMid = Color(0xFFF3E8FC),
            gradLinearEnd = Color(0xFFE4CDF8),
            ribbonA = Color(0xB8FFFFFF),
            ribbonB = Color(0x38A855F7),
            liquidGlassRgb = Triple(253, 248, 255),
            contentMaterialRgb = Triple(248, 238, 254),
            thickMaterialBg = Color(0xFFE7D2F8),
            inputRgb = Triple(130, 58, 180),
            textPrimary = Color(0xFF23073B),
            textSecondary = Color(0xA623073B),
            textTertiary = Color(0x6E23073B),
            accentColor = Color(0xFF9B51E0),
            accentBg = Color(0xFF9B51E0),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x229B51E0),
            starColor = Color(0xFFD98200),
            swatchStart = Color(0xFFFAF5FF),
            swatchEnd = Color(0xFFB57EDC)
        )
    ),
    SUNSET_ORANGE(
        id = "sunsetOrange",
        themeName = "Laranja Solar",
        subtitle = "Laranja solar intenso & âmbar quente",
        dark = ThemeSchemeColors(
            isLight = false,
            baseBg = Color(0xFF1E1005),
            gradTopRight = Color(0x70FF9F0A),
            gradBottomLeft = Color(0x806E3400),
            gradLinearStart = Color(0xFF2E1908),
            gradLinearMid = Color(0xFF1E1005),
            gradLinearEnd = Color(0xFF100802),
            ribbonA = Color(0x35FF9F0A),
            ribbonB = Color(0x15FFFFFF),
            liquidGlassRgb = Triple(74, 38, 14),
            contentMaterialRgb = Triple(48, 24, 8),
            thickMaterialBg = Color(0xFF2C1A09),
            inputRgb = Triple(110, 56, 18),
            textPrimary = Color(0xFFFFFBF5),
            textSecondary = Color(0xCCFED7AA),
            textTertiary = Color(0x88FDBA74),
            accentColor = Color(0xFFFF9F0A),
            accentBg = Color(0xFFFF9F0A),
            accentText = Color(0xFF1E0E00),
            pillActiveBg = Color(0x33FF9F0A),
            starColor = Color(0xFFFFD60A),
            swatchStart = Color(0xFFFF9F0A),
            swatchEnd = Color(0xFF4A1F00)
        ),
        light = ThemeSchemeColors(
            isLight = true,
            baseBg = Color(0xFFFDF0E6),
            gradTopRight = Color(0x60FB923C),
            gradBottomLeft = Color(0x70FED7AA),
            gradLinearStart = Color(0xFFFFF8F2),
            gradLinearMid = Color(0xFFFDF0E6),
            gradLinearEnd = Color(0xFFF8DCBE),
            ribbonA = Color(0xB8FFFFFF),
            ribbonB = Color(0x38F97316),
            liquidGlassRgb = Triple(255, 252, 248),
            contentMaterialRgb = Triple(254, 244, 235),
            thickMaterialBg = Color(0xFFFADEC9),
            inputRgb = Triple(180, 78, 12),
            textPrimary = Color(0xFF3B1500),
            textSecondary = Color(0xA63B1500),
            textTertiary = Color(0x6E3B1500),
            accentColor = Color(0xFFFF6B00),
            accentBg = Color(0xFFFF6B00),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x22FF6B00),
            starColor = Color(0xFFD98200),
            swatchStart = Color(0xFFFFF8F2),
            swatchEnd = Color(0xFFFCA666)
        )
    ),
    ROSE_PINK(
        id = "rosePink",
        themeName = "Rosa Neon",
        subtitle = "Rosa choque vibrante & magenta puro",
        dark = ThemeSchemeColors(
            isLight = false,
            baseBg = Color(0xFF1C0612),
            gradTopRight = Color(0x70FF2D55),
            gradBottomLeft = Color(0x806E0038),
            gradLinearStart = Color(0xFF2C0B1D),
            gradLinearMid = Color(0xFF1C0612),
            gradLinearEnd = Color(0xFF0E0208),
            ribbonA = Color(0x35FF2D55),
            ribbonB = Color(0x15FFFFFF),
            liquidGlassRgb = Triple(72, 16, 44),
            contentMaterialRgb = Triple(46, 10, 28),
            thickMaterialBg = Color(0xFF2B0B1D),
            inputRgb = Triple(110, 26, 68),
            textPrimary = Color(0xFFFFF0F6),
            textSecondary = Color(0xCCFBCFE8),
            textTertiary = Color(0x88F472B6),
            accentColor = Color(0xFFFF2D55),
            accentBg = Color(0xFFFF2D55),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x33FF2D55),
            starColor = Color(0xFFFFD60A),
            swatchStart = Color(0xFFFF2D55),
            swatchEnd = Color(0xFF480222)
        ),
        light = ThemeSchemeColors(
            isLight = true,
            baseBg = Color(0xFFFCE7F0),
            gradTopRight = Color(0x60F472B6),
            gradBottomLeft = Color(0x70FBCFE8),
            gradLinearStart = Color(0xFFFFF2F7),
            gradLinearMid = Color(0xFFFCE7F0),
            gradLinearEnd = Color(0xFFF9D0E2),
            ribbonA = Color(0xB8FFFFFF),
            ribbonB = Color(0x38EC4899),
            liquidGlassRgb = Triple(255, 248, 252),
            contentMaterialRgb = Triple(254, 240, 248),
            thickMaterialBg = Color(0xFFF9D0E2),
            inputRgb = Triple(170, 30, 85),
            textPrimary = Color(0xFF38031C),
            textSecondary = Color(0xA638031C),
            textTertiary = Color(0x6E38031C),
            accentColor = Color(0xFFE81E63),
            accentBg = Color(0xFFE81E63),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x22E81E63),
            starColor = Color(0xFFD98200),
            swatchStart = Color(0xFFFFF2F7),
            swatchEnd = Color(0xFFF675A8)
        )
    ),
    CYBER_TEAL(
        id = "cyberTeal",
        themeName = "Turquesa & Ciano",
        subtitle = "Água-marinha pura & turquesa viva",
        dark = ThemeSchemeColors(
            isLight = false,
            baseBg = Color(0xFF04191C),
            gradTopRight = Color(0x705AC8FA),
            gradBottomLeft = Color(0x80084E59),
            gradLinearStart = Color(0xFF0A2B30),
            gradLinearMid = Color(0xFF04191C),
            gradLinearEnd = Color(0xFF020C0E),
            ribbonA = Color(0x355AC8FA),
            ribbonB = Color(0x15FFFFFF),
            liquidGlassRgb = Triple(16, 56, 64),
            contentMaterialRgb = Triple(10, 36, 42),
            thickMaterialBg = Color(0xFF08272C),
            inputRgb = Triple(26, 86, 96),
            textPrimary = Color(0xFFF0FDFA),
            textSecondary = Color(0xCC99F6E4),
            textTertiary = Color(0x885EEAD4),
            accentColor = Color(0xFF5AC8FA),
            accentBg = Color(0xFF5AC8FA),
            accentText = Color(0xFF021B20),
            pillActiveBg = Color(0x335AC8FA),
            starColor = Color(0xFFFFD60A),
            swatchStart = Color(0xFF5AC8FA),
            swatchEnd = Color(0xFF04333A)
        ),
        light = ThemeSchemeColors(
            isLight = true,
            baseBg = Color(0xFFE3F7F8),
            gradTopRight = Color(0x6038BDF8),
            gradBottomLeft = Color(0x70A5F3FC),
            gradLinearStart = Color(0xFFF0FBFC),
            gradLinearMid = Color(0xFFE3F7F8),
            gradLinearEnd = Color(0xFFCEEFF2),
            ribbonA = Color(0xB8FFFFFF),
            ribbonB = Color(0x3806B6D4),
            liquidGlassRgb = Triple(246, 254, 255),
            contentMaterialRgb = Triple(236, 250, 252),
            thickMaterialBg = Color(0xFFCDF0F2),
            inputRgb = Triple(20, 120, 130),
            textPrimary = Color(0xFF032629),
            textSecondary = Color(0xA6032629),
            textTertiary = Color(0x6E032629),
            accentColor = Color(0xFF00A8B5),
            accentBg = Color(0xFF00A8B5),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x2200A8B5),
            starColor = Color(0xFFD98200),
            swatchStart = Color(0xFFF0FBFC),
            swatchEnd = Color(0xFF63D0D9)
        )
    ),
    MONOCHROME(
        id = "monochrome",
        themeName = "Obsidian & Prata",
        subtitle = "Monocromático ultra elegante",
        dark = ThemeSchemeColors(
            isLight = false,
            baseBg = Color(0xFF08080A),
            gradTopRight = Color(0x40FFFFFF),
            gradBottomLeft = Color(0x303F3F46),
            gradLinearStart = Color(0xFF141418),
            gradLinearMid = Color(0xFF08080A),
            gradLinearEnd = Color(0xFF040405),
            ribbonA = Color(0x25FFFFFF),
            ribbonB = Color(0x10FFFFFF),
            liquidGlassRgb = Triple(36, 36, 42),
            contentMaterialRgb = Triple(24, 24, 28),
            thickMaterialBg = Color(0xFF141418),
            inputRgb = Triple(64, 64, 72),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA1A1AA),
            textTertiary = Color(0xFF71717A),
            accentColor = Color(0xFFFFFFFF),
            accentBg = Color(0xFFFFFFFF),
            accentText = Color(0xFF08080A),
            pillActiveBg = Color(0x28FFFFFF),
            starColor = Color(0xFFFFD60A),
            swatchStart = Color(0xFFFFFFFF),
            swatchEnd = Color(0xFF08080A)
        ),
        light = ThemeSchemeColors(
            isLight = true,
            baseBg = Color(0xFFF2F2F7),
            gradTopRight = Color(0x30111111),
            gradBottomLeft = Color(0x209CA3AF),
            gradLinearStart = Color(0xFFFAFAFC),
            gradLinearMid = Color(0xFFF2F2F7),
            gradLinearEnd = Color(0xFFE5E5EA),
            ribbonA = Color(0xB8FFFFFF),
            ribbonB = Color(0x20000000),
            liquidGlassRgb = Triple(250, 250, 252),
            contentMaterialRgb = Triple(242, 242, 247),
            thickMaterialBg = Color(0xFFE5E5EA),
            inputRgb = Triple(90, 90, 95),
            textPrimary = Color(0xFF111111),
            textSecondary = Color(0xFF6B7280),
            textTertiary = Color(0xFF9CA3AF),
            accentColor = Color(0xFF111111),
            accentBg = Color(0xFF111111),
            accentText = Color(0xFFFFFFFF),
            pillActiveBg = Color(0x18000000),
            starColor = Color(0xFFD98200),
            swatchStart = Color(0xFFFFFFFF),
            swatchEnd = Color(0xFF111111)
        )
    );

    companion object {
        val MIDNIGHT_SLATE get() = OCEAN_BLUE
        val WARM_SAND get() = SUNSET_ORANGE
        val AERO_CYAN get() = CYBER_TEAL
        val DEEP_COBALT get() = OCEAN_BLUE
        val ARCH_STONE get() = MONOCHROME
        val DARK_ESPRESSO get() = SUNSET_ORANGE

        fun fromId(id: String): IOS27ThemeDef = when (id) {
            "BLUE", "oceanBlue", "midnight", "midnightSlate", "deepCobalt" -> OCEAN_BLUE
            "RED", "carmineRed" -> CARMINE_RED
            "GREEN", "emeraldGreen" -> EMERALD_GREEN
            "PURPLE", "royalPurple" -> ROYAL_PURPLE
            "ORANGE", "sunsetOrange", "warmSand", "darkEspresso" -> SUNSET_ORANGE
            "WHITE", "BLACK", "monochrome", "archStone" -> MONOCHROME
            "rosePink" -> ROSE_PINK
            "cyberTeal", "aeroCyan" -> CYBER_TEAL
            else -> entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OCEAN_BLUE
        }
    }
}

enum class AppleAvatarPreset(
    val id: String,
    val label: String,
    val startColor: Color,
    val endColor: Color
) {
    INDIGO("indigo", "Safira", Color(0xFF007AFF), Color(0xFF5856D6)),
    SUNSET("sunset", "Âmbar", Color(0xFFFF9500), Color(0xFFFF2D55)),
    EMERALD("emerald", "Esmeralda", Color(0xFF34C759), Color(0xFF30B0C7)),
    VIOLET("violet", "Ametista", Color(0xFFAF52DE), Color(0xFF5856D6)),
    CRIMSON("crimson", "Carmim", Color(0xFFFF3B30), Color(0xFFC62828)),
    TITANIUM("titanium", "Titânio", Color(0xFF8E8E93), Color(0xFF3A3A3C)),
    THEME("theme", "Tema", Color(0xFFFFFFFF), Color(0xFFD4D4D8));

    companion object {
        fun fromId(id: String): AppleAvatarPreset =
            entries.firstOrNull { it.id == id } ?: INDIGO
    }
}

@Immutable
data class ComicMoment(
    val id: String = "",
    val page: Int = 0,
    val tag: String = "⚡ Momento Épico",
    val note: String = "",
    val date: String = "",
    val isSpoiler: Boolean = false
)

@Stable
@Immutable
data class ComicEntry(
    val id: String,
    val externalId: String,
    val title: String,
    val titlePtBr: String = "",
    val authors: String,
    val publisher: String,
    val genre: String = "Graphic Novel",
    val format: String = "Encadernado (TPB)",
    val releaseYear: Int = 2026,
    val pageCount: Int = 160,
    val currentPage: Int = 0,
    val status: ReadingStatus = ReadingStatus.READ,
    val rating: Float = 0f,
    val favorite: Boolean = false,
    val rereadCount: Int = 0,
    val tags: List<String> = emptyList(),
    val review: String = "",
    val readDate: String = "",
    val addedAt: String = "",
    val isbn: String = "",
    val coverUrl: String = "",
    val synopsis: String = "",
    val synopsisPtBr: String = "",
    val sourceLabel: String = "Catálogo Base",
    val isOnlineResult: Boolean = false,
    val moments: List<ComicMoment> = emptyList(),
    val seriesName: String = "",
    val issueNumber: Int = 0,
    val updatedAt: Long = 0L,
    val isDirty: Boolean = false
)

@Immutable
data class ReaderProfile(
    val name: String = "Felipe",
    val handle: String = "@felipe",
    val bio: String = "Colecionador de encadernados, graphic novels e mangás.",
    val favoriteUniverse: String = "DC / Vertigo",
    val avatarUrl: String = "",
    val avatarPreset: String = "indigo",
    val yearlyGoal: Int = 24,
    val dailyPagesGoal: Int = 30,
    val showGoalOnHome: Boolean = true,
    val updatedAt: Long = 0L,
    val isDirty: Boolean = false
)

@Stable
@Immutable
data class AppSettings(
    val themeId: String = "midnightSlate",
    val colorScheme: String = "dark", // "dark" | "light"
    val glassLevel: Int = 26, // 0 = Clear, 100 = Tinted
    val glassVariant: String = "regular", // "regular" | "clear"
    val edgeRefraction: Boolean = true,
    val glassIntensity: Float = 1.0f,
    val glassBlurIntensity: Float = 1.0f,
    val enableLensRefraction: Boolean = true,
    val enableChromaticAberration: Boolean = true,
    val glassOpacity: Float = 1.0f,
    val backgroundExtension: Boolean = true,
    val showBottomAccessory: Boolean = true,
    val showFloatingMenu: Boolean = true,
    val minimizeTabBarOnScroll: Boolean = true,
    val reduceTransparency: Boolean = false,
    val increaseContrast: Boolean = false,
    val boldText: Boolean = false,
    val dynamicTypeScale: Int = 100, // 85..115
    val reduceMotion: Boolean = false,
    val onOffLabels: Boolean = false,
    val shelfColumns: Int = 3, // 2, 3, 4
    val showProgressOnCovers: Boolean = true,
    val showRatingsOnGrid: Boolean = true,
    val spine3dEffect: Boolean = true,
    val defaultShelfSort: String = "RECENT", // "RECENT" | "RATING" | "TITLE" | "YEAR"
    val defaultFormat: String = "Encadernado (TPB)",
    val searchLanguage: String = "ALL", // "ALL" | "PT"
    val searchProvider: String = "MULTI", // "MULTI" | "BOOKS" | "MANGA"
    val hdCovers: Boolean = true,
    val spoilerProtection: Boolean = false,
    val halfStarRatings: Boolean = true,
    val allowRereads: Boolean = true,
    val autoFillReadDate: Boolean = true,
    val showPagesInDiary: Boolean = true,
    val quickReviewTags: List<String> = listOf(
        "Roteiro impecável",
        "Arte incrível",
        "Clássico absoluto",
        "Final surpreendente"
    )
) {
    val glassSettings: GlassSettings
        get() = GlassSettings(
            blurRadius = (8f * glassBlurIntensity).coerceIn(1f, 32f),
            refractionHeight = (10f * glassIntensity).coerceIn(1f, 30f),
            refractionAmount = (14f * glassIntensity).coerceIn(1f, 30f),
            chromaticAberration = enableChromaticAberration,
            glassAlpha = glassOpacity.coerceIn(0.1f, 2f)
        )
}

@Immutable
data class GlassSettings(
    val blurRadius: Float = 8f,
    val refractionHeight: Float = 10f,
    val refractionAmount: Float = 14f,
    val chromaticAberration: Boolean = true,
    val glassAlpha: Float = 1f
)

@Immutable
data class ApiEndpointStatus(
    val name: String,
    val ok: Boolean,
    val ms: Long
)

@Stable
@Immutable
data class ApiDiagnosticsReport(
    val testedAt: String,
    val totalMs: Long,
    val endpoints: List<ApiEndpointStatus>
)
