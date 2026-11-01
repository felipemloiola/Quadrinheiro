package com.example.ui.components

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import coil.Coil
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.CuratedCatalog
import com.example.model.AppSettings
import com.example.model.AppleAvatarPreset
import com.example.model.ComicEntry
import com.example.model.ComicMoment
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.ui.screens.computeReadingStatistics
import com.example.ui.screens.rememberAppleFitnessPalette
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.example.ui.theme.getIOS27ThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Modelos de Cards de Exportação e Compartilhamento Social (Estilo Apple Music Replay / Apple Books / Letterboxd):
 * - A. COMPLETED_READING: Card "Leitura Concluída" (Padrão 9:16 para Stories / Status ou 1:1 para Feed)
 * - B. EPIC_MOMENT: Card "Citação / Momento Épico" (ComicMoments com tag, página e miniatura da capa)
 * - C. GOAL_SUMMARY: Card "Resumo da Meta Anual / Mensal" (Anéis de Atividade, % no ano e páginas lidas)
 */
enum class SocialShareCardType(val id: String, val label: String) {
    COMPLETED_READING("completed", "Leitura Concluída"),
    EPIC_MOMENT("moment", "Momento"),
    GOAL_SUMMARY("goals", "Resumo de Metas")
}

/**
 * Proporções de exportação offscreen em alta definição:
 * - STORIES_9_16: 1080x1920 (Padrão para Instagram Stories, WhatsApp Status, TikTok)
 * - FEED_1_1: 1080x1080 (Quadrado para Feed, Twitter/X, Threads)
 */
enum class SocialShareAspectRatio(
    val id: String,
    val label: String,
    val widthPx: Int,
    val heightPx: Int,
    val aspectRatio: Float
) {
    STORIES_9_16("stories", "Stories 9:16", 1080, 1920, 9f / 16f),
    FEED_1_1("feed", "Feed 1:1", 1080, 1080, 1f)
}

@Immutable
data class SocialShareRequest(
    val initialCardType: SocialShareCardType = SocialShareCardType.COMPLETED_READING,
    val comic: ComicEntry? = null,
    val initialMomentId: String? = null,
    val initialGoalPeriod: String = "ANNUAL" // "ANNUAL" | "MONTHLY"
)

/**
 * Tags editoriais predefinidas para os Momentos Épicos & Citações.
 */
val PREDEFINED_MOMENT_TAGS = listOf(
    "⚡ Momento Épico",
    "🔥 Batalha Histórica",
    "💬 Diálogo Marcante",
    "🎨 Arte Inesquecível",
    "🤯 Plot Twist"
)

/**
 * Formata datas ISO ("2026-10-30") para o formato editorial em português ("30 de Outubro de 2026").
 */
fun formatPortugueseCompletionDate(rawDate: String): String {
    val clean = rawDate.trim().take(10)
    val parts = clean.split("-")
    val monthNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )
    if (parts.size == 3) {
        val year = parts[0].toIntOrNull() ?: 2026
        val monthIdx = ((parts[1].toIntOrNull() ?: 10) - 1).coerceIn(0, 11)
        val day = (parts[2].toIntOrNull() ?: 30).coerceIn(1, 31)
        return "Concluído em $day de ${monthNames[monthIdx]} de $year"
    }
    return "Concluído em 30 de Outubro de 2026"
}

/**
 * Resolve a cor atmosférica profunda derivada da capa da HQ (ex.: Batman: Ano Um -> Vermelho-Carmim profundo).
 */
fun resolveAtmosphericAuraForComic(comic: ComicEntry?, sampledColor: Color?, fallbackAccent: Color): Color {
    if (sampledColor != null) return sampledColor
    if (comic == null) return fallbackAccent
    val t = comic.displayTitle
    return when {
        t.contains("Ano Um", ignoreCase = true) ||
            t.contains("Year One", ignoreCase = true) ||
            t.contains("Vitória Sombria", ignoreCase = true) ||
            t.contains("Dark Victory", ignoreCase = true) ||
            t.contains("Batman", ignoreCase = true) -> Color(0xFFE11D48) // Aura vermelho-carmim profunda
        t.contains("Watchmen", ignoreCase = true) -> Color(0xFFF59E0B)
        t.contains("Sandman", ignoreCase = true) -> Color(0xFF8B5CF6)
        t.contains("Berserk", ignoreCase = true) -> Color(0xFFDC2626)
        t.contains("Akira", ignoreCase = true) ||
            t.contains("Demolidor", ignoreCase = true) ||
            t.contains("Daredevil", ignoreCase = true) -> Color(0xFFEF4444)
        t.contains("Saga", ignoreCase = true) ||
            t.contains("X-Men", ignoreCase = true) -> Color(0xFFF97316)
        t.contains("Monstro do Pântano", ignoreCase = true) ||
            t.contains("Swamp Thing", ignoreCase = true) -> Color(0xFF10B981)
        comic.publisher.contains("DC", ignoreCase = true) ||
            comic.publisher.contains("Vertigo", ignoreCase = true) -> Color(0xFFE11D48)
        comic.publisher.contains("Marvel", ignoreCase = true) -> Color(0xFFEF4444)
        comic.genre.contains("Mangá", ignoreCase = true) ||
            comic.genre.contains("Manga", ignoreCase = true) -> Color(0xFFEC4899)
        else -> fallbackAccent
    }
}

/**
 * Formata a representação de estrelas douradas (ex.: "★ ★ ★ ★ ★  5.0").
 */
fun formatGoldenStarsText(rating: Float): String {
    val effective = if (rating <= 0f) 5.0f else rating.coerceIn(0.5f, 5.0f)
    val fullStars = effective.toInt().coerceIn(0, 5)
    val hasHalf = (effective - fullStars) >= 0.45f && fullStars < 5
    val builder = StringBuilder()
    for (i in 1..5) {
        if (i > 1) builder.append(" ")
        when {
            i <= fullStars -> builder.append("★")
            i == fullStars + 1 && hasHalf -> builder.append("★")
            else -> builder.append("☆")
        }
    }
    builder.append("  ")
    builder.append(String.format(Locale.US, "%.1f", effective))
    return builder.toString()
}

/**
 * Motor de Exportação e Compartilhamento Social Nativo (GraphicsLayer + Offscreen HD 1080x1920 / 1080x1080 + FileProvider + Intent.ACTION_SEND).
 */
object SocialCardExporter {

    private const val SHARED_CARDS_SUBDIR = "shared_cards"

    /**
     * Renderiza um card social em altíssima resolução (1080x1920 para Stories 9:16 ou 1080x1080 para Feed 1:1)
     * seguindo o design icônico do Letterboxd (fundo em gradiente atmosférico escuro para preto,
     * avatar circular sobreposto no topo central da capa, título centralizado em branco bold,
     * estrelas verdes #00E054 e assinatura "── ON ── ••• Quadrinheiro").
     */
    fun renderHighResolutionCardBitmap(
        cardType: SocialShareCardType,
        aspectRatio: SocialShareAspectRatio,
        comic: ComicEntry?,
        momentNote: String,
        momentTag: String,
        momentPage: Int,
        profile: ReaderProfile,
        allComics: List<ComicEntry>,
        goalPeriodMode: String,
        auraColor: Color,
        secondaryRingColor: Color,
        thirdRingColor: Color,
        coverBitmap: Bitmap? = null,
        avatarBitmap: Bitmap? = null,
        capturedGraphicsBitmap: Bitmap? = null,
        starColor: Color = Color(0xFFFFD60A)
    ): Bitmap {
        val targetW = aspectRatio.widthPx
        val targetH = aspectRatio.heightPx

        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)

        // 1. Fundo em Gradiente Vertical Estilo Letterboxd:
        // Topo em tom ardósia/atmosférico derivado da capa -> Transição suave -> Preto profundo na metade inferior
        val topAtmospheric = mixColors(Color(0xFF424E5D), auraColor, 0.34f)
        val upperMidAtmospheric = mixColors(Color(0xFF232932), auraColor, 0.22f)
        val lowerMidDark = Color(0xFF0D0F13)
        val bottomPitchBlack = Color(0xFF050507)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, targetH.toFloat(),
                intArrayOf(
                    topAtmospheric.toArgb(),
                    upperMidAtmospheric.toArgb(),
                    lowerMidDark.toArgb(),
                    bottomPitchBlack.toArgb()
                ),
                floatArrayOf(0f, 0.38f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), bgPaint)

        // Brilho atmosférico suave atrás da capa (estilo Letterboxd Stories)
        val topRadialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                targetW * 0.5f,
                targetH * 0.28f,
                targetW * 0.72f,
                intArrayOf(
                    mixColors(Color(0xFF536273), auraColor, 0.38f).copy(alpha = 0.38f).toArgb(),
                    android.graphics.Color.TRANSPARENT
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), topRadialPaint)

        when (cardType) {
            SocialShareCardType.COMPLETED_READING -> drawCompletedReadingCanvas(
                canvas = canvas,
                w = targetW,
                h = targetH,
                isStories = aspectRatio == SocialShareAspectRatio.STORIES_9_16,
                comic = comic,
                profile = profile,
                auraColor = auraColor,
                coverBitmap = coverBitmap,
                avatarBitmap = avatarBitmap,
                starColor = starColor
            )

            SocialShareCardType.EPIC_MOMENT -> drawEpicMomentCanvas(
                canvas = canvas,
                w = targetW,
                h = targetH,
                isStories = aspectRatio == SocialShareAspectRatio.STORIES_9_16,
                comic = comic,
                momentNote = momentNote,
                momentTag = momentTag,
                momentPage = momentPage,
                profile = profile,
                auraColor = auraColor,
                coverBitmap = coverBitmap,
                avatarBitmap = avatarBitmap,
                starColor = starColor
            )

            SocialShareCardType.GOAL_SUMMARY -> drawGoalSummaryCanvas(
                canvas = canvas,
                w = targetW,
                h = targetH,
                isStories = aspectRatio == SocialShareAspectRatio.STORIES_9_16,
                comics = allComics,
                profile = profile,
                periodMode = goalPeriodMode,
                moveColor = auraColor,
                exerciseColor = secondaryRingColor,
                standColor = thirdRingColor,
                avatarBitmap = avatarBitmap,
                starColor = starColor
            )
        }

        return output
    }

    private fun drawCompletedReadingCanvas(
        canvas: AndroidCanvas,
        w: Int,
        h: Int,
        isStories: Boolean,
        comic: ComicEntry?,
        profile: ReaderProfile,
        auraColor: Color,
        coverBitmap: Bitmap?,
        avatarBitmap: Bitmap?,
        starColor: Color
    ) {
        val title = comic?.displayTitle?.ifBlank { "Batman: Ano Um" } ?: "Batman: Ano Um"
        val publisher = comic?.publisher?.ifBlank { "DC Comics" } ?: "DC Comics"
        val rating = comic?.rating?.takeIf { it > 0f } ?: 5.0f

        // Pôster / Capa centralizada em grande destaque (proporção 2:3 idêntica ao Letterboxd)
        val coverH = if (isStories) 960f else 540f
        val coverW = coverH * (2f / 3f)
        val coverLeft = (w - coverW) / 2f
        val coverTop = if (isStories) 310f else 140f
        val coverRect = RectF(coverLeft, coverTop, coverLeft + coverW, coverTop + coverH)

        drawLetterboxdPosterCoverOnCanvas(
            canvas = canvas,
            rect = coverRect,
            coverBitmap = coverBitmap,
            title = title,
            publisher = publisher,
            auraColor = auraColor
        )

        // Avatar circular sobreposto na borda superior central da capa (micro-avatar sem contorno, idêntico ao Letterboxd)
        val avatarRadius = if (isStories) 26f else 20f
        drawOverlappingCircularAvatarOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            cy = coverRect.top,
            radius = avatarRadius,
            profile = profile,
            avatarBitmap = avatarBitmap,
            accentColor = auraColor
        )

        // Título da Obra abaixo da capa (Branco Bold centralizado)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = if (isStories) 48f else 36f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = -0.02f
        }
        val titleMaxW = (w - 200).coerceAtLeast(300)
        val titleLayout = StaticLayout.Builder
            .obtain(title, 0, title.length, titlePaint, titleMaxW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .setIncludePad(false)
            .build()

        val titleTopY = coverRect.bottom + (if (isStories) 36f else 24f)
        canvas.save()
        canvas.translate((w - titleMaxW) / 2f, titleTopY)
        titleLayout.draw(canvas)
        canvas.restore()

        // Linha de Estrelas na cor configurada pelo usuário no app (espaçamento compacto)
        val starsCenterY = titleTopY + titleLayout.height + (if (isStories) 22f else 16f)
        drawLetterboxdStarRowOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            cy = starsCenterY,
            rating = rating,
            starColor = starColor,
            starRadius = if (isStories) 13f else 10f,
            starSpacing = if (isStories) 32f else 25f
        )

        // Assinatura: "──── ON ────" + Logo Quadrinheiro + "Quadrinheiro" (sem fundo quadrado)
        val brandTopY = starsCenterY + (if (isStories) 30f else 22f)
        drawLetterboxdBrandLockupOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            topY = brandTopY,
            isStories = isStories
        )
    }

    private fun drawEpicMomentCanvas(
        canvas: AndroidCanvas,
        w: Int,
        h: Int,
        isStories: Boolean,
        comic: ComicEntry?,
        momentNote: String,
        momentTag: String,
        momentPage: Int,
        profile: ReaderProfile,
        auraColor: Color,
        coverBitmap: Bitmap?,
        avatarBitmap: Bitmap?,
        starColor: Color
    ) {
        val cleanTag = momentTag.ifBlank { "⚡ Momento Épico" }
        val cleanPage = if (momentPage > 0) momentPage else (comic?.currentPage?.takeIf { it > 0 } ?: 84)
        val cleanQuote = momentNote.trim().ifBlank {
            comic?.moments?.firstOrNull()?.note?.ifBlank { null }
                ?: comic?.review?.ifBlank { null }
                ?: "Um momento decisivo que redefine toda a jornada do personagem com arte e narrativa impecáveis."
        }
        val title = comic?.displayTitle?.ifBlank { "Watchmen: Edição Definitiva" } ?: "Watchmen: Edição Definitiva"
        val publisher = comic?.publisher?.ifBlank { "DC / Vertigo" } ?: "DC / Vertigo"
        val rating = comic?.rating?.takeIf { it > 0f } ?: 5.0f

        val coverH = if (isStories) 520f else 300f
        val coverW = coverH * (2f / 3f)
        val coverLeft = (w - coverW) / 2f
        val coverTop = if (isStories) 240f else 120f
        val coverRect = RectF(coverLeft, coverTop, coverLeft + coverW, coverTop + coverH)

        drawLetterboxdPosterCoverOnCanvas(
            canvas = canvas,
            rect = coverRect,
            coverBitmap = coverBitmap,
            title = title,
            publisher = publisher,
            auraColor = auraColor
        )

        drawOverlappingCircularAvatarOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            cy = coverRect.top,
            radius = if (isStories) 38f else 28f,
            profile = profile,
            avatarBitmap = avatarBitmap,
            accentColor = auraColor
        )

        // Citação em destaque abaixo da capa
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = if (isStories) 40f else 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val formattedQuote = "“$cleanQuote”"
        val maxQuoteW = (w - 200).coerceAtLeast(300)
        val quoteLayout = StaticLayout.Builder
            .obtain(formattedQuote, 0, formattedQuote.length, quotePaint, maxQuoteW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(if (isStories) 5 else 3)
            .setLineSpacing(6f, 1.12f)
            .build()

        val quoteTopY = coverRect.bottom + (if (isStories) 46f else 26f)
        canvas.save()
        canvas.translate((w - maxQuoteW) / 2f, quoteTopY)
        quoteLayout.draw(canvas)
        canvas.restore()

        val titleY = quoteTopY + quoteLayout.height + (if (isStories) 42f else 26f)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = if (isStories) 40f else 30f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val truncTitle = if (title.length > 32) title.take(31) + "…" else title
        canvas.drawText(truncTitle, w / 2f, titleY, titlePaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(180, 220, 225, 235)
            textSize = if (isStories) 25f else 20f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val metaY = titleY + (if (isStories) 36f else 26f)
        canvas.drawText("$cleanTag • Página $cleanPage", w / 2f, metaY, metaPaint)

        val starsY = metaY + (if (isStories) 38f else 26f)
        drawLetterboxdStarRowOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            cy = starsY,
            rating = rating,
            starColor = starColor,
            starRadius = if (isStories) 14f else 11f,
            starSpacing = if (isStories) 34f else 27f
        )

        val brandTopY = starsY + (if (isStories) 50f else 34f)
        drawLetterboxdBrandLockupOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            topY = brandTopY,
            isStories = isStories
        )
    }

    private fun drawGoalSummaryCanvas(
        canvas: AndroidCanvas,
        w: Int,
        h: Int,
        isStories: Boolean,
        comics: List<ComicEntry>,
        profile: ReaderProfile,
        periodMode: String,
        moveColor: Color,
        exerciseColor: Color,
        standColor: Color,
        avatarBitmap: Bitmap?,
        starColor: Color
    ) {
        val stats = computeReadingStatistics(
            comics = comics,
            profile = profile,
            accentColor = moveColor,
            secondaryColor = exerciseColor
        )
        val isMonthly = periodMode == "MONTHLY"
        val outerProgress = if (isMonthly) {
            val mGoal = (stats.yearlyGoal / 12f).coerceAtLeast(1f)
            (stats.currentMonthComics.toFloat() / mGoal).coerceIn(0f, 1f)
        } else {
            (stats.readComicsCount.toFloat() / stats.yearlyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        }
        val innerProgress = (stats.dailyPagesPace.toFloat() / stats.dailyPagesGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val thirdProgress = (stats.activeMonthsCount.toFloat() / 12f).coerceIn(0f, 1f)
        val pct = (outerProgress * 100f).roundToInt().coerceIn(0, 999)

        val ringDiameter = if (isStories) 520f else 330f
        val ringCx = w / 2f
        val ringCy = if (isStories) 600f else 350f
        val strokeW = ringDiameter * 0.13f
        val gap = strokeW * 0.11f

        val ringColors = listOf(moveColor, exerciseColor, standColor)
        val ringValues = listOf(outerProgress, innerProgress, thirdProgress)

        for (i in 0..2) {
            val radius = (ringDiameter / 2f) - strokeW / 2f - i * (strokeW + gap)
            val oval = RectF(ringCx - radius, ringCy - radius, ringCx + radius, ringCy + radius)

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                strokeCap = Paint.Cap.ROUND
                color = ringColors[i].copy(alpha = 0.22f).toArgb()
            }
            canvas.drawArc(oval, 0f, 360f, false, trackPaint)

            val prog = ringValues[i].coerceIn(0f, 1f)
            if (prog > 0.005f) {
                val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    strokeCap = Paint.Cap.ROUND
                    color = ringColors[i].toArgb()
                }
                canvas.drawArc(oval, -90f, (prog * 360f).coerceAtLeast(6f), false, progPaint)
            }
        }

        drawOverlappingCircularAvatarOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            cy = ringCy - ringDiameter / 2f - (if (isStories) 36f else 24f),
            radius = if (isStories) 40f else 30f,
            profile = profile,
            avatarBitmap = avatarBitmap,
            accentColor = moveColor
        )

        val centerPctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = if (isStories) 64f else 44f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pctOffsetY = (centerPctPaint.descent() + centerPctPaint.ascent()) / 2f
        canvas.drawText("$pct%", ringCx, ringCy - pctOffsetY, centerPctPaint)

        val metricsTopY = ringCy + ringDiameter / 2f + (if (isStories) 78f else 48f)
        val mainStatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = if (isStories) 50f else 36f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pagesCount = if (isMonthly) stats.currentMonthPages else stats.totalPagesRead
        canvas.drawText("$pagesCount páginas lidas", w / 2f, metricsTopY, mainStatPaint)

        val subStatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(195, 220, 225, 235)
            textSize = if (isStories) 28f else 22f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val worksSubtitle = if (isMonthly) {
            "${stats.currentMonthComics} quadrinhos em ${stats.currentMonthName}"
        } else {
            "${stats.readComicsCount} de ${stats.yearlyGoal} quadrinhos no ano"
        }
        val subY = metricsTopY + (if (isStories) 46f else 32f)
        canvas.drawText(worksSubtitle, w / 2f, subY, subStatPaint)

        val starsY = subY + (if (isStories) 40f else 28f)
        drawLetterboxdStarRowOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            cy = starsY,
            rating = 5.0f,
            starColor = starColor,
            starRadius = if (isStories) 14f else 11f,
            starSpacing = if (isStories) 34f else 27f
        )

        val brandTopY = starsY + (if (isStories) 52f else 36f)
        drawLetterboxdBrandLockupOnCanvas(
            canvas = canvas,
            cx = w / 2f,
            topY = brandTopY,
            isStories = isStories
        )
    }

    /**
     * Desenha o pôster / capa no estilo Letterboxd (cantos arredondados sutis de 10f, sombra natural e borda fina).
     */
    private fun drawLetterboxdPosterCoverOnCanvas(
        canvas: AndroidCanvas,
        rect: RectF,
        coverBitmap: Bitmap?,
        title: String,
        publisher: String,
        auraColor: Color
    ) {
        val cornerRadius = 10f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(135, 0, 0, 0)
            maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
        }
        val shadowRect = RectF(rect.left + 2f, rect.top + 8f, rect.right - 2f, rect.bottom + 16f)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        val clipPath = AndroidPath().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, AndroidPath.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        if (coverBitmap != null && !coverBitmap.isRecycled) {
            val srcRatio = coverBitmap.width.toFloat() / coverBitmap.height.coerceAtLeast(1).toFloat()
            val dstRatio = rect.width() / rect.height()
            val srcRect = if (srcRatio > dstRatio) {
                val cropW = (coverBitmap.height * dstRatio).toInt()
                val left = (coverBitmap.width - cropW) / 2
                Rect(left, 0, left + cropW, coverBitmap.height)
            } else {
                val cropH = (coverBitmap.width / dstRatio).toInt()
                val top = (coverBitmap.height - cropH) / 2
                Rect(0, top, coverBitmap.width, top + cropH)
            }
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(coverBitmap, srcRect, rect, bmpPaint)
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(
                        auraColor.toArgb(),
                        mixColors(Color(0xFF14141D), auraColor, 0.35f).toArgb()
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(rect, fallbackPaint)

            val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = rect.width() * 0.10f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(title.take(18), rect.centerX(), rect.centerY(), tPaint)
        }
        canvas.restore()

        // Borda sutil estilo pôster Letterboxd
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = android.graphics.Color.argb(32, 255, 255, 255)
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    /**
     * Desenha o avatar circular do usuário sobreposto na borda superior central do pôster (estilo Letterboxd).
     * SEM BORDA / CONTORNO ao redor da foto, 100% idêntico ao original Letterboxd!
     */
    private fun drawOverlappingCircularAvatarOnCanvas(
        canvas: AndroidCanvas,
        cx: Float,
        cy: Float,
        radius: Float,
        profile: ReaderProfile,
        avatarBitmap: Bitmap?,
        accentColor: Color
    ) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(120, 0, 0, 0)
            maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(cx, cy + 3f, radius, shadowPaint)

        val clipPath = AndroidPath().apply {
            addCircle(cx, cy, radius, AndroidPath.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        val dstRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        if (avatarBitmap != null && !avatarBitmap.isRecycled) {
            val minDim = minOf(avatarBitmap.width, avatarBitmap.height).coerceAtLeast(1)
            val left = (avatarBitmap.width - minDim) / 2
            val top = (avatarBitmap.height - minDim) / 2
            val srcRect = Rect(left, top, left + minDim, top + minDim)
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(avatarBitmap, srcRect, dstRect, bmpPaint)
        } else {
            val preset = AppleAvatarPreset.fromId(profile.avatarPreset)
            val startC = if (preset == AppleAvatarPreset.THEME) accentColor else preset.startColor
            val endC = if (preset == AppleAvatarPreset.THEME) mixColors(accentColor, Color(0xFF1E293B), 0.45f) else preset.endColor
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    dstRect.left, dstRect.top, dstRect.right, dstRect.bottom,
                    intArrayOf(startC.toArgb(), endC.toArgb()),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(dstRect, gradPaint)

            val initial = (profile.name.trim().firstOrNull()?.uppercaseChar() ?: 'F').toString()
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = radius * 0.92f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(initial, cx, textY, textPaint)
        }
        canvas.restore()
        // SEM borda / ringPaint ao redor da foto
    }

    /**
     * Desenha a fileira de estrelas na cor selecionada pelo usuário no app.
     */
    private fun drawLetterboxdStarRowOnCanvas(
        canvas: AndroidCanvas,
        cx: Float,
        cy: Float,
        rating: Float,
        starColor: Color,
        starRadius: Float,
        starSpacing: Float
    ) {
        val effective = if (rating <= 0f) 5.0f else rating.coerceIn(0.5f, 5.0f)
        val fullStars = effective.toInt().coerceIn(0, 5)
        val hasHalf = (effective - fullStars) >= 0.45f && fullStars < 5
        val totalSlots = fullStars + (if (hasHalf) 1 else 0)
        val count = totalSlots.coerceIn(1, 5)

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = starColor.toArgb()
        }

        val totalWidth = (count - 1) * starSpacing
        val startX = cx - totalWidth / 2f

        for (i in 0 until count) {
            val starCx = startX + i * starSpacing
            val isHalfStar = hasHalf && i == count - 1 && i >= fullStars
            val path = createFivePointStarPath(starCx, cy, starRadius, starRadius * 0.44f)
            if (isHalfStar) {
                canvas.save()
                canvas.clipRect(starCx - starRadius, cy - starRadius, starCx, cy + starRadius)
                canvas.drawPath(path, starPaint)
                canvas.restore()
            } else {
                canvas.drawPath(path, starPaint)
            }
        }
    }

    private fun createFivePointStarPath(
        cx: Float,
        cy: Float,
        outerRadius: Float,
        innerRadius: Float
    ): AndroidPath {
        val path = AndroidPath()
        val startAngle = -Math.PI / 2.0
        val step = Math.PI / 5.0
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = startAngle + i * step
            val x = (cx + cos(angle) * r).toFloat()
            val y = (cy + sin(angle) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    /**
     * Desenha a marca/logo do Quadrinheiro em vetor diretamente no Canvas:
     * As 3 facetas geométricas originais com seus gradientes característicos,
     * SEM nenhum fundo quadrado (transparente, exatamente como a logo do Letterboxd).
     */
    private fun drawQuadrinheiroLogoMarkOnCanvas(
        canvas: AndroidCanvas,
        left: Float,
        top: Float,
        size: Float
    ) {
        val glyphW = 60.7f
        val glyphH = 57.4f
        val scale = (size / maxOf(glyphW, glyphH)) * 0.95f
        val offsetX = left - 25.0f * scale + (size - glyphW * scale) / 2f
        val offsetY = top - 26.3f * scale + (size - glyphH * scale) / 2f

        // Faceta 1: Rosa Carmim
        val p1 = AndroidPath().apply {
            moveTo(offsetX + 44.8f * scale, offsetY + 26.3f * scale)
            lineTo(offsetX + 28.9f * scale, offsetY + 32.9f * scale)
            lineTo(offsetX + 25.0f * scale, offsetY + 54.0f * scale)
            lineTo(offsetX + 31.6f * scale, offsetY + 75.1f * scale)
            lineTo(offsetX + 39.5f * scale, offsetY + 69.8f * scale)
            lineTo(offsetX + 42.8f * scale, offsetY + 60.6f * scale)
            lineTo(offsetX + 40.1f * scale, offsetY + 52.7f * scale)
            lineTo(offsetX + 43.4f * scale, offsetY + 39.5f * scale)
            close()
        }
        val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                offsetX + 25f * scale, offsetY + 26.3f * scale,
                offsetX + 44.8f * scale, offsetY + 75.1f * scale,
                intArrayOf(Color(0xFFFF1493).toArgb(), Color(0xFFE11D48).toArgb(), Color(0xFF881337).toArgb()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(p1, paint1)

        // Faceta 2: Ouro/Laranja Solar
        val p2 = AndroidPath().apply {
            moveTo(offsetX + 49.4f * scale, offsetY + 26.3f * scale)
            lineTo(offsetX + 67.2f * scale, offsetY + 26.3f * scale)
            lineTo(offsetX + 80.4f * scale, offsetY + 35.5f * scale)
            lineTo(offsetX + 80.4f * scale, offsetY + 50.7f * scale)
            lineTo(offsetX + 63.9f * scale, offsetY + 50.7f * scale)
            lineTo(offsetX + 61.9f * scale, offsetY + 44.8f * scale)
            lineTo(offsetX + 53.3f * scale, offsetY + 38.8f * scale)
            lineTo(offsetX + 48.1f * scale, offsetY + 39.5f * scale)
            close()
        }
        val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                offsetX + 49.4f * scale, offsetY + 26.3f * scale,
                offsetX + 80.4f * scale, offsetY + 50.7f * scale,
                intArrayOf(Color(0xFFFFD000).toArgb(), Color(0xFFFF9100).toArgb(), Color(0xFFFF3D00).toArgb()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(p2, paint2)

        // Faceta 3: Safira/Ciano
        val p3 = AndroidPath().apply {
            moveTo(offsetX + 63.9f * scale, offsetY + 55.3f * scale)
            lineTo(offsetX + 79.1f * scale, offsetY + 55.3f * scale)
            lineTo(offsetX + 76.4f * scale, offsetY + 63.9f * scale)
            lineTo(offsetX + 85.7f * scale, offsetY + 77.8f * scale)
            lineTo(offsetX + 83.7f * scale, offsetY + 83.7f * scale)
            lineTo(offsetX + 69.2f * scale, offsetY + 78.4f * scale)
            lineTo(offsetX + 55.3f * scale, offsetY + 80.4f * scale)
            lineTo(offsetX + 40.1f * scale, offsetY + 77.1f * scale)
            lineTo(offsetX + 46.1f * scale, offsetY + 65.9f * scale)
            lineTo(offsetX + 54.0f * scale, offsetY + 63.2f * scale)
            close()
        }
        val paint3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                offsetX + 40.1f * scale, offsetY + 63.2f * scale,
                offsetX + 85.7f * scale, offsetY + 83.7f * scale,
                intArrayOf(Color(0xFF1E3A8A).toArgb(), Color(0xFF2563EB).toArgb(), Color(0xFF00B4D8).toArgb(), Color(0xFF00F5FF).toArgb()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(p3, paint3)
    }

    /**
     * Desenha a assinatura inferior idêntica ao Letterboxd:
     * Linha 1: ────── ON ──────
     * Linha 2: [Logo Quadrinheiro sem fundo] Quadrinheiro
     */
    private fun drawLetterboxdBrandLockupOnCanvas(
        canvas: AndroidCanvas,
        cx: Float,
        topY: Float,
        isStories: Boolean
    ) {
        val onTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(135, 255, 255, 255)
            textSize = if (isStories) 20f else 16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.14f
        }
        val onTextY = topY - (onTextPaint.descent() + onTextPaint.ascent()) / 2f
        canvas.drawText("ON", cx, onTextY, onTextPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(55, 255, 255, 255)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val gapFromText = if (isStories) 28f else 20f
        val lineW = if (isStories) 70f else 50f
        canvas.drawLine(cx - gapFromText - lineW, topY, cx - gapFromText, topY, linePaint)
        canvas.drawLine(cx + gapFromText, topY, cx + gapFromText + lineW, topY, linePaint)

        // Linha 2: Logo da marca (apenas a logo sem fundo quadrado) + "Quadrinheiro"
        val brandRowCy = topY + (if (isStories) 36f else 26f)
        val brandTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = if (isStories) 32f else 24f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            letterSpacing = -0.02f
        }
        val brandName = "Quadrinheiro"
        val brandTextW = brandTextPaint.measureText(brandName)

        val logoSize = if (isStories) 32f else 24f
        val logoToTextGap = if (isStories) 10f else 7f
        val totalLockupW = logoSize + logoToTextGap + brandTextW
        val startX = cx - totalLockupW / 2f

        drawQuadrinheiroLogoMarkOnCanvas(
            canvas = canvas,
            left = startX,
            top = brandRowCy - logoSize / 2f,
            size = logoSize
        )

        val brandTextY = brandRowCy - (brandTextPaint.descent() + brandTextPaint.ascent()) / 2f
        canvas.drawText(brandName, startX + logoSize + logoToTextGap, brandTextY, brandTextPaint)
    }

    internal fun mixColors(base: Color, overlay: Color, ratio: Float): Color {
        val r = ratio.coerceIn(0f, 1f)
        return Color(
            red = base.red * (1f - r) + overlay.red * r,
            green = base.green * (1f - r) + overlay.green * r,
            blue = base.blue * (1f - r) + overlay.blue * r,
            alpha = 1f
        )
    }

    /**
     * Salva o Bitmap PNG no diretório `cacheDir/shared_cards` e retorna o arquivo pronto para o FileProvider.
     */
    fun saveBitmapToSharedCacheFile(
        context: Context,
        bitmap: Bitmap,
        fileNamePrefix: String = "quadrinheiro_card"
    ): File {
        val sharedDir = File(context.cacheDir, SHARED_CARDS_SUBDIR)
        if (!sharedDir.exists()) {
            sharedDir.mkdirs()
        }
        // Limpa arquivos antigos com mais de 1 hora para manter o cache enxuto
        val cutoff = System.currentTimeMillis() - 3600_000L
        sharedDir.listFiles()?.forEach { f ->
            if (f.lastModified() < cutoff) {
                runCatching { f.delete() }
            }
        }
        val cleanPrefix = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32)
        val outFile = File(sharedDir, "${cleanPrefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
        }
        return outFile
    }

    /**
     * Dispara a Folha de Compartilhamento Nativa do Android (`Intent.ACTION_SEND`) usando `FileProvider`.
     */
    fun shareBitmapWithAndroidSheet(
        context: Context,
        bitmap: Bitmap,
        shareCaption: String
    ): Boolean {
        return runCatching {
            val pngFile = saveBitmapToSharedCacheFile(context, bitmap)
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, pngFile)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareCaption)
                clipData = ClipData.newRawUri("Quadrinheiro Card", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Compartilhar Card do Quadrinheiro").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        }.getOrElse { false }
    }

    /**
     * Salva o Bitmap PNG diretamente na Galeria do dispositivo (`MediaStore.Images.Media` em `Pictures/Quadrinheiro`).
     */
    fun saveBitmapToDeviceGallery(
        context: Context,
        bitmap: Bitmap,
        displayTitle: String
    ): Boolean {
        return runCatching {
            val resolver = context.contentResolver
            val cleanName = displayTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(28).ifBlank { "card" }
            val filename = "Quadrinheiro_${cleanName}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Quadrinheiro")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrElse { false }
    }
}

/**
 * Modal de Prévia e Exportação Social de Cards Estilo Apple (Apple Music Replay / Apple Books / Letterboxd):
 * - Permite alternar entre os 3 modelos:
 *   A. Card "Leitura Concluída" (9:16 Stories / 1:1 Feed)
 *   B. Card "Citação / Momento Épico" (ComicMoments)
 *   C. Card "Resumo da Meta Anual / Mensal" (Activity Rings)
 * - Captura o card com `GraphicsLayer.record` + exportador HD nativo (`1080x1920` ou `1080x1080`)
 * - Dispara a Folha de Compartilhamento Nativa (`Intent.ACTION_SEND`) ou salva na galeria.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialShareCardSheetModal(
    request: SocialShareRequest,
    allComics: List<ComicEntry>,
    profile: ReaderProfile,
    appSettings: AppSettings,
    onShowToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val fitnessPalette = rememberAppleFitnessPalette()

    // Se o request veio sem uma HQ específica (ex.: aberto a partir de Estatísticas), usa a última HQ lida ou em leitura
    val targetComic = remember(request.comic, allComics) {
        request.comic
            ?: allComics.firstOrNull { it.status == ReadingStatus.READ }
            ?: allComics.firstOrNull { it.status == ReadingStatus.READING }
            ?: allComics.firstOrNull()
    }

    var selectedCardType by rememberSaveable(request.initialCardType) {
        mutableStateOf(request.initialCardType)
    }
    var selectedAspectRatio by rememberSaveable {
        mutableStateOf(SocialShareAspectRatio.STORIES_9_16)
    }
    var goalPeriodMode by rememberSaveable(request.initialGoalPeriod) {
        mutableStateOf(request.initialGoalPeriod)
    }

    // Momentos salvos na HQ
    val comicMoments = remember(targetComic) {
        targetComic?.moments.orEmpty()
    }
    val initialMomentIndex = remember(comicMoments, request.initialMomentId) {
        if (request.initialMomentId == null) 0
        else comicMoments.indexOfFirst { it.id == request.initialMomentId }.coerceAtLeast(0)
    }
    var selectedMomentIndex by rememberSaveable(targetComic?.id, request.initialMomentId) {
        mutableIntStateOf(initialMomentIndex)
    }
    val currentMoment = comicMoments.getOrNull(selectedMomentIndex)

    var customMomentTag by rememberSaveable(currentMoment?.id, currentMoment?.tag) {
        mutableStateOf(
            currentMoment?.tag?.takeIf { it.isNotBlank() } ?: PREDEFINED_MOMENT_TAGS.first()
        )
    }
    var customMomentPage by rememberSaveable(currentMoment?.id, currentMoment?.page, targetComic?.currentPage) {
        mutableIntStateOf(
            currentMoment?.page?.takeIf { it > 0 }
                ?: targetComic?.currentPage?.takeIf { it > 0 }
                ?: targetComic?.pageCount?.let { (it / 2).coerceAtLeast(1) }
                ?: 84
        )
    }
    var customMomentQuote by rememberSaveable(currentMoment?.id, currentMoment?.note, targetComic?.review) {
        mutableStateOf(
            currentMoment?.note?.takeIf { it.isNotBlank() }
                ?: targetComic?.review?.takeIf { it.isNotBlank() }
                ?: "Uma sequência memorável com narrativa envolvente e arte impressionante do início ao fim."
        )
    }

    // Extrai a cor vibrante e o Bitmap de alta resolução da capa da HQ
    val resolvedCoverUrl = remember(targetComic) {
        val raw = targetComic?.coverUrl?.trim()?.replace("http://", "https://").orEmpty()
        if (raw.isEmpty() || raw.contains("covers.openlibrary.org")) {
            CuratedCatalog.findCoverUrlByTitle(targetComic?.title.orEmpty()) ?: raw
        } else raw
    }

    var sampledCoverColor by remember(resolvedCoverUrl) { mutableStateOf<Color?>(null) }
    var loadedCoverBitmap by remember(resolvedCoverUrl) { mutableStateOf<Bitmap?>(null) }
    var loadedAvatarBitmap by remember(profile.avatarUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(resolvedCoverUrl) {
        if (resolvedCoverUrl.isNotBlank()) {
            sampledCoverColor = AmbientImageProcessor.processAmbientCoverColor(
                context = context,
                rawUrl = resolvedCoverUrl,
                dispatcher = Dispatchers.Default
            )
            loadedCoverBitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val req = ImageRequest.Builder(context)
                        .data(resolvedCoverUrl)
                        .size(600, 900)
                        .allowHardware(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    val res = Coil.imageLoader(context).execute(req)
                    (res.drawable as? BitmapDrawable)?.bitmap
                }.getOrNull()
            }
        }
    }

    LaunchedEffect(profile.avatarUrl) {
        val rawAvatar = profile.avatarUrl.trim()
        loadedAvatarBitmap = if (rawAvatar.startsWith("data:image")) {
            withContext(Dispatchers.Default) {
                runCatching {
                    val base64Part = rawAvatar.substringAfter("base64,", "")
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
            }
        } else if (rawAvatar.startsWith("http")) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val req = ImageRequest.Builder(context)
                        .data(rawAvatar)
                        .size(240, 240)
                        .allowHardware(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    val res = Coil.imageLoader(context).execute(req)
                    (res.drawable as? BitmapDrawable)?.bitmap
                }.getOrNull()
            }
        } else {
            null
        }
    }

    val auraColor by animateColorAsState(
        targetValue = resolveAtmosphericAuraForComic(
            comic = targetComic,
            sampledColor = sampledCoverColor,
            fallbackAccent = fitnessPalette.moveRingColor
        ),
        animationSpec = tween(420),
        label = "socialCardAuraColor"
    )

    // GraphicsLayer para gravação nativa acelerada por hardware do Card Compose
    val cardGraphicsLayer = rememberGraphicsLayer()
    var isExporting by remember { mutableStateOf(false) }

    suspend fun generateFinalShareBitmap(): Bitmap {
        val capturedComposeBmp = runCatching {
            cardGraphicsLayer.toImageBitmap().asAndroidBitmap()
        }.getOrNull()

        return withContext(Dispatchers.Default) {
            SocialCardExporter.renderHighResolutionCardBitmap(
                cardType = selectedCardType,
                aspectRatio = selectedAspectRatio,
                comic = targetComic,
                momentNote = customMomentQuote,
                momentTag = customMomentTag,
                momentPage = customMomentPage,
                profile = profile,
                allComics = allComics,
                goalPeriodMode = goalPeriodMode,
                auraColor = auraColor,
                secondaryRingColor = fitnessPalette.exerciseRingColor,
                thirdRingColor = fitnessPalette.standRingColor,
                coverBitmap = loadedCoverBitmap,
                avatarBitmap = loadedAvatarBitmap,
                capturedGraphicsBitmap = capturedComposeBmp,
                starColor = s.starColor
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(65f)
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .contentMaterialSurface(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Grabber superior
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(s.textSecondary.copy(alpha = 0.45f))
                )
            }

            // Cabeçalho do Modal de Compartilhamento Social
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fechar",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = s.textSecondary,
                    modifier = Modifier
                        .testTag("social_share_close_button")
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Compartilhar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${selectedAspectRatio.widthPx}×${selectedAspectRatio.heightPx} • Letterboxd",
                        fontSize = 10.5.sp,
                        fontFamily = SpaceGroteskTabularFamily,
                        color = s.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Alternador rápido Stories 9:16 vs Feed 1:1
                Row(
                    modifier = Modifier
                        .testTag("social_share_aspect_toggle")
                        .inputWellSurface(RoundedCornerShape(999.dp))
                        .clickable {
                            selectedAspectRatio = if (selectedAspectRatio == SocialShareAspectRatio.STORIES_9_16) {
                                SocialShareAspectRatio.FEED_1_1
                            } else {
                                SocialShareAspectRatio.STORIES_9_16
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selectedAspectRatio.label,
                        fontSize = 11.5.sp,
                        fontFamily = SpaceGroteskTabularFamily,
                        fontWeight = FontWeight.Bold,
                        color = s.accentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Seletor Segmentado Liquid Glass dos 3 Modelos de Card (Leitura Concluída | Momento | Metas)
            AppleSegmentedPicker(
                value = selectedCardType,
                onChange = { selectedCardType = it },
                options = listOf(
                    SegmentOption(SocialShareCardType.COMPLETED_READING, "Concluída"),
                    SegmentOption(SocialShareCardType.EPIC_MOMENT, "Momento"),
                    SegmentOption(SocialShareCardType.GOAL_SUMMARY, "Metas")
                )
            )

            // Área rolável central: Prévia ao vivo do Card + Controles contextuais do modelo selecionado
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Container de Prévia do Card (Grava em GraphicsLayer em tempo real!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            if (selectedAspectRatio == SocialShareAspectRatio.STORIES_9_16) 0.78f else 0.90f
                        )
                        .aspectRatio(selectedAspectRatio.aspectRatio)
                        .shadow(22.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.65f))
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("social_share_card_preview_canvas")
                        .drawWithContent {
                            cardGraphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(cardGraphicsLayer)
                        }
                ) {
                    SocialShareCardLiveArtwork(
                        cardType = selectedCardType,
                        aspectRatio = selectedAspectRatio,
                        comic = targetComic,
                        momentNote = customMomentQuote,
                        momentTag = customMomentTag,
                        momentPage = customMomentPage,
                        profile = profile,
                        avatarBitmap = loadedAvatarBitmap,
                        allComics = allComics,
                        goalPeriodMode = goalPeriodMode,
                        auraColor = auraColor,
                        secondaryRingColor = fitnessPalette.exerciseRingColor,
                        thirdRingColor = fitnessPalette.standRingColor,
                        starColor = s.starColor
                    )
                }

                // Controles Contextuais de Personalização Rápida para o Modelo B (Momento Épico) ou Modelo C (Metas)
                if (selectedCardType == SocialShareCardType.EPIC_MOMENT) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .inputWellSurface(RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PERSONALIZAR CITAÇÃO / MOMENTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = s.accentColor
                        )

                        // Se a HQ possui momentos anotados, permite escolher qual momento exibir no card
                        if (comicMoments.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                comicMoments.forEachIndexed { idx, m ->
                                    val isSel = idx == selectedMomentIndex
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isSel) s.accentBg else Color.White.copy(alpha = 0.08f))
                                            .clickable {
                                                selectedMomentIndex = idx
                                                if (m.page > 0) customMomentPage = m.page
                                                if (m.note.isNotBlank()) customMomentQuote = m.note
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Pág. ${m.page.takeIf { it > 0 } ?: 1}: ${m.note.take(20)}…",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) s.accentText else s.textPrimary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Edição rápida do texto da nota/citação e da página
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(78.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (s.isLight) Color.White else Color.Black.copy(alpha = 0.32f))
                                    .border(0.6.dp, s.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                BasicTextField(
                                    value = customMomentPage.toString(),
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(4)
                                        customMomentPage = digits.toIntOrNull() ?: 1
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = SpaceGroteskTabularFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = s.textPrimary
                                    ),
                                    cursorBrush = SolidColor(s.accentColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (s.isLight) Color.White else Color.Black.copy(alpha = 0.32f))
                                    .border(0.6.dp, s.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                BasicTextField(
                                    value = customMomentQuote,
                                    onValueChange = { customMomentQuote = it },
                                    maxLines = 2,
                                    textStyle = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontSize = 12.sp,
                                        color = s.textPrimary
                                    ),
                                    cursorBrush = SolidColor(s.accentColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else if (selectedCardType == SocialShareCardType.GOAL_SUMMARY) {
                    AppleSegmentedPicker(
                        value = goalPeriodMode,
                        onChange = { goalPeriodMode = it },
                        options = listOf(
                            SegmentOption("ANNUAL", "Meta Anual 2026"),
                            SegmentOption("MONTHLY", "Resumo do Mês")
                        )
                    )
                }
            }

            // Barra de Ações Inferior: Salvar na Galeria + Compartilhar Card (Folha Nativa do Android)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão Secundário: Salvar na Galeria
                Row(
                    modifier = Modifier
                        .testTag("social_share_save_gallery_button")
                        .height(48.dp)
                        .inputWellSurface(RoundedCornerShape(999.dp))
                        .clickable(enabled = !isExporting) {
                            scope.launch {
                                isExporting = true
                                val bmp = generateFinalShareBitmap()
                                val saved = withContext(Dispatchers.IO) {
                                    SocialCardExporter.saveBitmapToDeviceGallery(
                                        context = context,
                                        bitmap = bmp,
                                        displayTitle = targetComic?.displayTitle ?: "Metas_Quadrinheiro"
                                    )
                                }
                                isExporting = false
                                if (saved) {
                                    onShowToast("Card salvo em alta resolução na Galeria!")
                                } else {
                                    onShowToast("Não foi possível salvar o card na galeria.")
                                }
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Salvar na Galeria",
                        tint = s.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Salvar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Botão Principal: Compartilhar (Instagram Stories, Twitter/X, WhatsApp via Intent.ACTION_SEND)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("social_share_dispatch_button")
                        .shadow(10.dp, RoundedCornerShape(999.dp), spotColor = auraColor.copy(alpha = 0.40f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(s.accentBg)
                        .clickable(enabled = !isExporting) {
                            scope.launch {
                                isExporting = true
                                val bmp = generateFinalShareBitmap()
                                val caption = when (selectedCardType) {
                                    SocialShareCardType.COMPLETED_READING -> {
                                        val t = targetComic?.displayTitle ?: "Quadrinho"
                                        "Acabei de concluir \"$t\" no Quadrinheiro! ★ ${String.format(Locale.US, "%.1f", targetComic?.rating?.takeIf { it > 0f } ?: 5.0f)}"
                                    }
                                    SocialShareCardType.EPIC_MOMENT -> {
                                        val t = targetComic?.displayTitle ?: "Quadrinho"
                                        "“$customMomentQuote” — $t (Pág. $customMomentPage) via Quadrinheiro"
                                    }
                                    SocialShareCardType.GOAL_SUMMARY -> {
                                        "Meu ritmo de leitura de quadrinhos em 2026 no Quadrinheiro!"
                                    }
                                }
                                val launched = SocialCardExporter.shareBitmapWithAndroidSheet(
                                    context = context,
                                    bitmap = bmp,
                                    shareCaption = caption
                                )
                                isExporting = false
                                if (!launched) {
                                    onShowToast("Card preparado no cache para compartilhamento.")
                                }
                            }
                        }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = s.accentText,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = LucideIcons.ShareIos,
                            contentDescription = "Compartilhar",
                            tint = s.accentText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExporting) "Renderizando Card HD…" else "Compartilhar Card",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.accentText,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Arte em tempo real do Card Social no estilo Letterboxd (renderizada com Jetpack Compose e capturada via `GraphicsLayer.record`):
 * - Fundo full-bleed em gradiente vertical atmosférico (tom ardósia/capa no topo -> preto profundo na metade inferior)
 * - Avatar circular do usuário sobreposto exatamente na borda superior central do pôster
 * - Pôster em proporção 2:3 com cantos arredondados e sombra profunda
 * - Título centralizado em branco bold
 * - Fileira de estrelas verdes Letterboxd (#00E054)
 * - Assinatura inferior "────── ON ──────" + "••• Quadrinheiro"
 */
@Composable
private fun SocialShareCardLiveArtwork(
    cardType: SocialShareCardType,
    aspectRatio: SocialShareAspectRatio,
    comic: ComicEntry?,
    momentNote: String,
    momentTag: String,
    momentPage: Int,
    profile: ReaderProfile,
    avatarBitmap: Bitmap?,
    allComics: List<ComicEntry>,
    goalPeriodMode: String,
    auraColor: Color,
    secondaryRingColor: Color,
    thirdRingColor: Color,
    starColor: Color = LocalAppleMaterials.current.scheme.starColor
) {
    val isStories = aspectRatio == SocialShareAspectRatio.STORIES_9_16

    val topAtmospheric = remember(auraColor) {
        SocialCardExporter.mixColors(Color(0xFF424E5D), auraColor, 0.34f)
    }
    val upperMidAtmospheric = remember(auraColor) {
        SocialCardExporter.mixColors(Color(0xFF232932), auraColor, 0.22f)
    }
    val radialHighlight = remember(auraColor) {
        SocialCardExporter.mixColors(Color(0xFF536273), auraColor, 0.38f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507)),
        contentAlignment = Alignment.Center
    ) {
        // 1. Gradiente Vertical Full-Bleed estilo Letterboxd (Topo atmosférico -> Preto profundo na metade inferior)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to topAtmospheric,
                        0.38f to upperMidAtmospheric,
                        0.68f to Color(0xFF0D0F13),
                        1.00f to Color(0xFF050507)
                    )
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        radialHighlight.copy(alpha = 0.36f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = size.maxDimension * 0.65f
                )
            )
        }

        // 2. Conteúdo Centralizado estilo Letterboxd (sem moldura retangular interna)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isStories) 20.dp else 16.dp,
                    vertical = if (isStories) 20.dp else 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            when (cardType) {
                SocialShareCardType.COMPLETED_READING -> {
                    CompletedReadingCardContent(
                        comic = comic,
                        profile = profile,
                        avatarBitmap = avatarBitmap,
                        isStories = isStories,
                        auraColor = auraColor,
                        starColor = starColor
                    )
                }

                SocialShareCardType.EPIC_MOMENT -> {
                    EpicMomentCardContent(
                        comic = comic,
                        profile = profile,
                        avatarBitmap = avatarBitmap,
                        momentNote = momentNote,
                        momentTag = momentTag,
                        momentPage = momentPage,
                        isStories = isStories,
                        auraColor = auraColor,
                        starColor = starColor
                    )
                }

                SocialShareCardType.GOAL_SUMMARY -> {
                    GoalSummaryCardContent(
                        comics = allComics,
                        profile = profile,
                        avatarBitmap = avatarBitmap,
                        goalPeriodMode = goalPeriodMode,
                        isStories = isStories,
                        moveColor = auraColor,
                        exerciseColor = secondaryRingColor,
                        standColor = thirdRingColor,
                        starColor = starColor
                    )
                }
            }
        }
    }
}

/**
 * MODELO A: Card "Leitura Concluída" (Idêntico ao Letterboxd Stories / Feed)
 */
@Composable
private fun CompletedReadingCardContent(
    comic: ComicEntry?,
    profile: ReaderProfile,
    avatarBitmap: Bitmap?,
    isStories: Boolean,
    auraColor: Color,
    starColor: Color
) {
    val title = comic?.displayTitle?.ifBlank { "Batman: Ano Um" } ?: "Batman: Ano Um"
    val authors = comic?.authors?.ifBlank { "Frank Miller & David Mazzucchelli" } ?: "Frank Miller & David Mazzucchelli"
    val publisher = comic?.publisher?.ifBlank { "DC Comics" } ?: "DC Comics"
    val releaseYear = comic?.releaseYear?.takeIf { it > 0 } ?: 2026
    val rating = comic?.rating?.takeIf { it > 0f } ?: 5.0f

    // Micro-avatar sobreposto autêntico ao Letterboxd (~20dp em Stories / 16dp no feed, sem contorno)
    val avatarSize = if (isStories) 20.dp else 16.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Espaço superior para acomodar a metade superior do avatar sobreposto
        Spacer(modifier = Modifier.height(avatarSize / 2))

        // Pôster 2:3 com cantos sutis de 4dp, sombra suave realista e micro-avatar centralizado no topo
        Box(
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isStories) 0.64f else 0.56f)
                    .aspectRatio(2f / 3f)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(4.dp),
                        spotColor = Color.Black.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            ) {
                BookCover(
                    src = comic?.coverUrl.orEmpty(),
                    title = title,
                    authors = authors,
                    publisher = publisher,
                    year = releaseYear,
                    spine3d = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Avatar circular do usuário sobreposto na borda superior central da capa (sem borda/anel)
            LetterboxdOverlappingAvatar(
                profile = profile,
                avatarBitmap = avatarBitmap,
                accentColor = auraColor,
                sizeDp = avatarSize,
                modifier = Modifier
                    .offset(y = -(avatarSize / 2))
                    .zIndex(2f)
            )
        }

        Spacer(modifier = Modifier.height(if (isStories) 10.dp else 7.dp))

        // Título em branco bold marcante com espaçamento apertado estilo Apple/Letterboxd
        Text(
            text = title,
            fontSize = if (isStories) 18.sp else 15.sp,
            lineHeight = if (isStories) 21.sp else 18.sp,
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(if (isStories) 5.dp else 3.5.dp))

        // Estrelas com a cor configurada pelo usuário (alinhadas logo abaixo do título)
        LetterboxdStarRow(
            rating = rating,
            starColor = starColor,
            starSize = if (isStories) 13.dp else 11.dp,
            starGap = if (isStories) 3.dp else 2.dp
        )

        Spacer(modifier = Modifier.height(if (isStories) 7.dp else 4.dp))

        // Assinatura "────── ON ──────" + Marca Quadrinheiro (sem caixa quadrada)
        LetterboxdBrandLockup(isStories = isStories)
    }
}

/**
 * MODELO B: Card "Momento" (Com estética Letterboxd)
 */
@Composable
private fun EpicMomentCardContent(
    comic: ComicEntry?,
    profile: ReaderProfile,
    avatarBitmap: Bitmap?,
    momentNote: String,
    momentTag: String,
    momentPage: Int,
    isStories: Boolean,
    auraColor: Color,
    starColor: Color
) {
    val title = comic?.displayTitle?.ifBlank { "Watchmen: Edição Definitiva" } ?: "Watchmen: Edição Definitiva"
    val authors = comic?.authors?.ifBlank { "Alan Moore & Dave Gibbons" } ?: "Alan Moore & Dave Gibbons"
    val publisher = comic?.publisher?.ifBlank { "DC / Vertigo" } ?: "DC / Vertigo"
    val releaseYear = comic?.releaseYear?.takeIf { it > 0 } ?: 2026
    val rating = comic?.rating?.takeIf { it > 0f } ?: 5.0f

    val avatarSize = if (isStories) 20.dp else 16.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(avatarSize / 2))

        // Capa compacta com avatar sobreposto na borda superior (sem borda)
        Box(contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isStories) 0.42f else 0.36f)
                    .aspectRatio(2f / 3f)
                    .shadow(10.dp, RoundedCornerShape(4.dp), spotColor = Color.Black.copy(alpha = 0.55f))
                    .clip(RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            ) {
                BookCover(
                    src = comic?.coverUrl.orEmpty(),
                    title = title,
                    authors = authors,
                    publisher = publisher,
                    year = releaseYear,
                    spine3d = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            LetterboxdOverlappingAvatar(
                profile = profile,
                avatarBitmap = avatarBitmap,
                accentColor = auraColor,
                sizeDp = avatarSize,
                modifier = Modifier
                    .offset(y = -(avatarSize / 2))
                    .zIndex(2f)
            )
        }

        Spacer(modifier = Modifier.height(if (isStories) 10.dp else 6.dp))

        // Citação entre aspas elegantes
        Text(
            text = "“${momentNote.trim()}”",
            fontSize = if (isStories) 13.sp else 11.sp,
            lineHeight = if (isStories) 18.sp else 14.5.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.94f),
            textAlign = TextAlign.Center,
            maxLines = if (isStories) 4 else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(if (isStories) 8.dp else 5.dp))

        Text(
            text = title,
            fontSize = if (isStories) 15.sp else 12.5.sp,
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (momentPage > 0) "Pág. $momentPage" else "Momento",
            fontSize = if (isStories) 9.5.sp else 8.5.sp,
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.64f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(if (isStories) 5.dp else 3.5.dp))

        LetterboxdStarRow(
            rating = rating,
            starColor = starColor,
            starSize = if (isStories) 12.dp else 10.dp,
            starGap = 2.dp
        )

        Spacer(modifier = Modifier.height(if (isStories) 7.dp else 4.dp))

        LetterboxdBrandLockup(isStories = isStories)
    }
}

/**
 * MODELO C: Card "Resumo da Meta Anual / Mensal" (Com estética Letterboxd)
 */
@Composable
private fun GoalSummaryCardContent(
    comics: List<ComicEntry>,
    profile: ReaderProfile,
    avatarBitmap: Bitmap?,
    goalPeriodMode: String,
    isStories: Boolean,
    moveColor: Color,
    exerciseColor: Color,
    standColor: Color,
    starColor: Color
) {
    val stats = remember(comics, profile, moveColor, exerciseColor) {
        computeReadingStatistics(
            comics = comics,
            profile = profile,
            accentColor = moveColor,
            secondaryColor = exerciseColor
        )
    }
    val isMonthly = goalPeriodMode == "MONTHLY"
    val outerProgress = if (isMonthly) {
        val mGoal = (stats.yearlyGoal / 12f).coerceAtLeast(1f)
        (stats.currentMonthComics.toFloat() / mGoal).coerceIn(0f, 1f)
    } else {
        (stats.readComicsCount.toFloat() / stats.yearlyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    }
    val innerProgress = (stats.dailyPagesPace.toFloat() / stats.dailyPagesGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val thirdProgress = (stats.activeMonthsCount.toFloat() / 12f).coerceIn(0f, 1f)
    val pct = (outerProgress * 100f).roundToInt().coerceIn(0, 999)
    val pagesInPeriod = if (isMonthly) stats.currentMonthPages else stats.totalPagesRead

    val avatarSize = if (isStories) 20.dp else 16.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LetterboxdOverlappingAvatar(
            profile = profile,
            avatarBitmap = avatarBitmap,
            accentColor = moveColor,
            sizeDp = avatarSize
        )

        Spacer(modifier = Modifier.height(if (isStories) 8.dp else 5.dp))

        // Anéis de Atividade em Alta Definição (Sem contorno preto)
        AppleActivityRingsGauge(
            outerProgress = outerProgress,
            innerProgress = innerProgress,
            thirdProgress = thirdProgress,
            centerLabel = "$pct%",
            sizeDp = if (isStories) 136.dp else 94.dp,
            outerColor = moveColor,
            innerColor = exerciseColor,
            thirdColor = standColor,
            showCenterLabel = true
        )

        Spacer(modifier = Modifier.height(if (isStories) 12.dp else 7.dp))

        Text(
            text = "$pagesInPeriod páginas lidas",
            fontSize = if (isStories) 17.sp else 14.sp,
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (isMonthly) {
                "${stats.currentMonthComics} quadrinhos em ${stats.currentMonthName}"
            } else {
                "${stats.readComicsCount} de ${stats.yearlyGoal} quadrinhos em 2026"
            },
            fontSize = if (isStories) 11.sp else 9.5.sp,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(if (isStories) 5.dp else 3.5.dp))

        LetterboxdStarRow(
            rating = 5.0f,
            starColor = starColor,
            starSize = if (isStories) 12.dp else 10.dp,
            starGap = 2.dp
        )

        Spacer(modifier = Modifier.height(if (isStories) 7.dp else 4.dp))

        LetterboxdBrandLockup(isStories = isStories)
    }
}

/**
 * Avatar circular sobreposto no topo central da capa (estilo Letterboxd Stories - SEM BORDA).
 */
@Composable
private fun LetterboxdOverlappingAvatar(
    profile: ReaderProfile,
    avatarBitmap: Bitmap?,
    accentColor: Color,
    sizeDp: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val preset = remember(profile.avatarPreset) { AppleAvatarPreset.fromId(profile.avatarPreset) }
    val startColor = if (preset == AppleAvatarPreset.THEME) accentColor else preset.startColor
    val endColor = if (preset == AppleAvatarPreset.THEME) {
        SocialCardExporter.mixColors(accentColor, Color(0xFF1E293B), 0.45f)
    } else {
        preset.endColor
    }
    val initial = remember(profile.name) {
        (profile.name.trim().firstOrNull()?.uppercaseChar() ?: 'F').toString()
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.50f))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(startColor, endColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null && !avatarBitmap.isRecycled) {
            Image(
                bitmap = avatarBitmap.asImageBitmap(),
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                fontSize = (sizeDp.value * 0.42f).sp,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Fileira de estrelas desenhadas em vetor na cor configurada pelo usuário.
 */
@Composable
private fun LetterboxdStarRow(
    rating: Float,
    starColor: Color,
    starSize: androidx.compose.ui.unit.Dp,
    starGap: androidx.compose.ui.unit.Dp
) {
    val effective = if (rating <= 0f) 5.0f else rating.coerceIn(0.5f, 5.0f)
    val fullStars = effective.toInt().coerceIn(0, 5)
    val hasHalf = (effective - fullStars) >= 0.45f && fullStars < 5
    val count = (fullStars + (if (hasHalf) 1 else 0)).coerceIn(1, 5)

    Row(
        horizontalArrangement = Arrangement.spacedBy(starGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until count) {
            val isHalfStar = hasHalf && i == count - 1 && i >= fullStars
            Canvas(modifier = Modifier.size(starSize)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerR = size.minDimension / 2f
                val innerR = outerR * 0.44f
                val path = androidx.compose.ui.graphics.Path()
                val startAngle = -Math.PI / 2.0
                val step = Math.PI / 5.0
                for (pt in 0 until 10) {
                    val r = if (pt % 2 == 0) outerR else innerR
                    val angle = startAngle + pt * step
                    val x = (cx + cos(angle) * r).toFloat()
                    val y = (cy + sin(angle) * r).toFloat()
                    if (pt == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()

                if (isHalfStar) {
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(0f, 0f, cx, size.height)
                    drawPath(path = path, color = starColor)
                    drawContext.canvas.restore()
                } else {
                    drawPath(path = path, color = starColor)
                }
            }
        }
    }
}

/**
 * Assinatura inferior idêntica ao Letterboxd:
 * - Linha 1: `────── ON ──────`
 * - Linha 2: Marca Quadrinheiro (sem fundo quadrado) + `Quadrinheiro`
 */
@Composable
private fun LetterboxdBrandLockup(
    isStories: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isStories) 3.5.dp else 2.5.dp)
    ) {
        // Linha 1: ────── ON ──────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(if (isStories) 24.dp else 18.dp)
                    .height(0.6.dp)
                    .background(Color.White.copy(alpha = 0.22f))
            )
            Text(
                text = "ON",
                fontSize = if (isStories) 7.sp else 6.2.sp,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                softWrap = false
            )
            Box(
                modifier = Modifier
                    .width(if (isStories) 24.dp else 18.dp)
                    .height(0.6.dp)
                    .background(Color.White.copy(alpha = 0.22f))
            )
        }

        // Linha 2: Logo da marca (sem caixa ou fundo quadrado) + Quadrinheiro
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            QuadrinheiroLogo(
                variant = QuadrinheiroLogoVariant.Primary,
                modifier = Modifier.size(if (isStories) 14.dp else 11.dp)
            )

            Text(
                text = "Quadrinheiro",
                fontSize = if (isStories) 12.5.sp else 10.5.sp,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

