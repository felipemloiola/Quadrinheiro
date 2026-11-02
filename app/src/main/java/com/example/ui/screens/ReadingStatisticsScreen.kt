package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.ui.components.AppleActivityRingsGauge
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.components.LucideIcons
import com.example.ui.components.SegmentOption
import com.example.ui.components.contentMaterialSurface
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SpaceGroteskTabularFamily
import androidx.compose.ui.graphics.lerp
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Retorna cor de texto com contraste garantido sobre qualquer cor de fundo (evita texto branco sobre fundo branco).
 */
fun contrastingTextOn(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.52f) Color(0xFF090A0F) else Color.White
}

/**
 * Paleta vibrante inspirada no Apple Fitness adaptada para o tema ativo e para Light/Dark mode.
 * Mesmo nos temas Monocromáticos (Branco/Preto), mantém os anéis e gráficos vibrantes e legíveis.
 */
@Immutable
data class AppleFitnessPalette(
    val moveRingColor: Color,       // Anel Externo: Meta Anual (Vermelho/Rosa Apple Fitness ou cor vibrante do tema)
    val exerciseRingColor: Color,   // Anel Médio: Ritmo Diário (Verde Neon Apple Fitness)
    val standRingColor: Color,      // Anel Interno: Constância Mensal (Ciano Apple Fitness)
    val sparklinePurple: Color,     // Card de Tendência Esquerdo (Violeta Apple Fitness)
    val sparklineCyan: Color,       // Card de Tendência Direito (Ciano/Azul Apple Fitness)
    val activeChipBg: Color,        // Fundo de pílula selecionada com alto contraste
    val activeChipText: Color,      // Texto de pílula selecionada com contraste garantido
    val categoryColors: List<Color> // Paleta multicor para Donut e barras de categorias
)

@Composable
fun rememberAppleFitnessPalette(): AppleFitnessPalette {
    val themeColors = LocalIOS27Theme.current
    val isDark = LocalIsDarkTheme.current
    return remember(themeColors.appTheme, themeColors.accent, themeColors.accentSecondary, isDark) {
        val (moveColor, exerciseColor, standColor) = com.example.ui.theme.getAppleFitnessRingsColors(themeColors.appTheme, isDark)

        val chipBg = themeColors.accent
        val chipText = contrastingTextOn(chipBg)

        val categories = listOf(
            moveColor,
            exerciseColor,
            standColor,
            lerp(moveColor, exerciseColor, 0.5f),
            lerp(exerciseColor, standColor, 0.5f),
            lerp(moveColor, if (isDark) Color.White else Color.Black, 0.28f),
            lerp(standColor, if (isDark) Color.White else Color.Black, 0.25f)
        )

        AppleFitnessPalette(
            moveRingColor = moveColor,
            exerciseRingColor = exerciseColor,
            standRingColor = standColor,
            sparklinePurple = moveColor,
            sparklineCyan = exerciseColor,
            activeChipBg = chipBg,
            activeChipText = chipText,
            categoryColors = categories
        )
    }
}

@Immutable
data class MonthlyReadingBucket(
    val monthIndex: Int, // 0..11 (Jan..Dez)
    val shortLabel: String,
    val singleLetter: String,
    val fullName: String,
    val pagesRead: Int,
    val comicsRead: Int
)

@Immutable
data class CategoryBreakdownItem(
    val label: String,
    val count: Int,
    val pages: Int,
    val percentage: Int,
    val fraction: Float,
    val color: Color
)

@Immutable
data class AuthorRankingItem(
    val rank: Int,
    val name: String,
    val worksCount: Int,
    val totalPages: Int,
    val avgRating: Float,
    val fractionOfTop: Float
)

@Immutable
data class ReadingStatisticsSummary(
    val readComicsCount: Int,
    val readingComicsCount: Int,
    val queueComicsCount: Int,
    val yearlyGoal: Int,
    val totalPagesRead: Int,
    val currentMonthIndex: Int,
    val currentMonthPages: Int,
    val currentMonthComics: Int,
    val currentMonthName: String,
    val dailyPagesPace: Int,
    val dailyPagesGoal: Int,
    val activeMonthsCount: Int,
    val projectedYearEndComics: Int,
    val projectionMessage: String,
    val isGoalOnTrack: Boolean,
    val monthlyBuckets: List<MonthlyReadingBucket>,
    val formatBreakdown: List<CategoryBreakdownItem>,
    val publisherBreakdown: List<CategoryBreakdownItem>,
    val topAuthors: List<AuthorRankingItem>,
    val averageRating: Float,
    val ratedCount: Int
)

/**
 * Calcula de forma determinística e otimizada todas as métricas do Painel de Estatísticas.
 */
fun computeReadingStatistics(
    comics: List<ComicEntry>,
    profile: ReaderProfile,
    accentColor: Color,
    secondaryColor: Color,
    categoryPalette: List<Color> = emptyList()
): ReadingStatisticsSummary {
    val cal = Calendar.getInstance()
    val currentYear = 2026
    val currentMonthZeroBased = cal.get(Calendar.MONTH).coerceIn(0, 11)
    val dayOfYear = cal.get(Calendar.DAY_OF_YEAR).coerceIn(1, 365)

    val monthShortLabels = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
    val monthSingleLetters = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    val monthFullNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    val pagesByMonth = IntArray(12)
    val comicsByMonth = IntArray(12)

    var readCount = 0
    var readingCount = 0
    var queueCount = 0
    var totalPages = 0
    var ratedCount = 0
    var ratingSum = 0f

    val formatCounts = LinkedHashMap<String, Pair<Int, Int>>() // label -> (count, pages)
    val publisherCounts = LinkedHashMap<String, Pair<Int, Int>>()
    val authorMap = LinkedHashMap<String, Triple<Int, Int, MutableList<Float>>>() // author -> (works, pages, ratings)

    val engagedComics = comics.filter { it.status != ReadingStatus.WANT_TO_READ }

    for (comic in comics) {
        when (comic.status) {
            ReadingStatus.READ -> {
                readCount++
                val p = comic.pageCount.coerceAtLeast(0)
                totalPages += p
                if (comic.rating > 0f) {
                    ratedCount++
                    ratingSum += comic.rating
                }
            }
            ReadingStatus.READING -> {
                readingCount++
                totalPages += comic.currentPage.coerceAtLeast(0)
            }
            ReadingStatus.WANT_TO_READ -> {
                queueCount++
            }
        }
    }

    // Distribuição mensal (Jan..Dez) para obras Lidas ou Lendo
    for (comic in engagedComics) {
        val rawDate = comic.readDate.ifBlank { comic.addedAt.take(10) }
        val parts = rawDate.split("-")
        val parsedMonth = parts.getOrNull(1)?.toIntOrNull()
        val mIdx = if (parsedMonth != null && parsedMonth in 1..12) {
            parsedMonth - 1
        } else {
            currentMonthZeroBased
        }
        val contributedPages = when (comic.status) {
            ReadingStatus.READ -> comic.pageCount.coerceAtLeast(0)
            ReadingStatus.READING -> comic.currentPage.coerceAtLeast(0)
            ReadingStatus.WANT_TO_READ -> 0
        }
        pagesByMonth[mIdx] += contributedPages
        if (comic.status == ReadingStatus.READ) {
            comicsByMonth[mIdx] += 1
        }

        // Formatos canônicos: Encadernados (TPB), Mangás, Edições Especiais/Capa Dura, Mensais (Grampo)
        val normalizedFormat = normalizeFormatCategory(comic)
        val prevFmt = formatCounts[normalizedFormat] ?: (0 to 0)
        formatCounts[normalizedFormat] = (prevFmt.first + 1) to (prevFmt.second + contributedPages)

        // Editoras / Universos canônicos: DC / Vertigo, Marvel, Panini, Dark Horse, Mangás, etc.
        val normalizedPub = normalizePublisherCategory(comic)
        val prevPub = publisherCounts[normalizedPub] ?: (0 to 0)
        publisherCounts[normalizedPub] = (prevPub.first + 1) to (prevPub.second + contributedPages)

        // Autores e Artistas (separa múltiplos nomes por vírgula, '&', '/' ou ';')
        val individualAuthors = comic.authors
            .split(",", "&", "/", ";")
            .map { it.trim() }
            .filter { it.length >= 2 && !it.equals("Autor Desconhecido", ignoreCase = true) }

        val authorsToCredit = if (individualAuthors.isEmpty() && comic.authors.isNotBlank()) {
            listOf(comic.authors.trim())
        } else {
            individualAuthors
        }

        for (author in authorsToCredit) {
            val prevAuth = authorMap[author]
            val ratingsList = prevAuth?.third ?: mutableListOf()
            if (comic.rating > 0f) ratingsList.add(comic.rating)
            authorMap[author] = Triple(
                first = (prevAuth?.first ?: 0) + 1,
                second = (prevAuth?.second ?: 0) + contributedPages,
                third = ratingsList
            )
        }
    }

    // Mês ativo para destaque no mini-resumo do Diário (mês atual ou o mês mais recente com leituras)
    val activeMonthIdx = if (pagesByMonth[currentMonthZeroBased] > 0 || comicsByMonth[currentMonthZeroBased] > 0) {
        currentMonthZeroBased
    } else {
        (11 downTo 0).firstOrNull { pagesByMonth[it] > 0 || comicsByMonth[it] > 0 } ?: currentMonthZeroBased
    }

    val currentMonthPages = pagesByMonth[activeMonthIdx]
    val currentMonthComics = max(
        comicsByMonth[activeMonthIdx],
        engagedComics.count {
            val d = it.readDate.ifBlank { it.addedAt.take(10) }
            val m = d.split("-").getOrNull(1)?.toIntOrNull()
            (m != null && m - 1 == activeMonthIdx)
        }
    )

    // Ritmo diário (baseado na janela ativa de 30 dias ou média do ano)
    val activeDaysWindow = 30
    val referencePagesForPace = if (currentMonthPages > 0) currentMonthPages else totalPages
    val dailyPace = if (referencePagesForPace > 0) {
        max(1, (referencePagesForPace.toFloat() / activeDaysWindow.toFloat()).roundToInt())
    } else {
        0
    }
    val dailyGoal = profile.dailyPagesGoal.coerceAtLeast(5)
    val yearlyGoal = profile.yearlyGoal.coerceAtLeast(1)

    // Meses ativos no ano (para o 3º anel de Constância do Apple Fitness)
    val activeMonthsRaw = (0..11).count { comicsByMonth[it] > 0 || pagesByMonth[it] > 0 }
    val activeMonthsCount = if (engagedComics.isNotEmpty()) activeMonthsRaw.coerceAtLeast(1) else 0

    // Projeção anual inteligente
    val activeMonthsDivisor = activeMonthsCount.coerceAtLeast(1)
    val elapsedYearFraction = (dayOfYear.toFloat() / 365f).coerceIn(0.15f, 1f)
    val rawProjected = if (readCount > 0) {
        max(
            readCount,
            max(
                (readCount.toFloat() / elapsedYearFraction).roundToInt(),
                ((readCount.toFloat() / activeMonthsDivisor.toFloat()) * 12f * 0.65f).roundToInt()
            )
        )
    } else if (readingCount > 0) {
        max(yearlyGoal / 2, readingCount * 4)
    } else {
        0
    }

    val isGoalOnTrack = rawProjected >= yearlyGoal || readCount >= yearlyGoal || dailyPace >= dailyGoal
    val projectionMessage = when {
        readCount >= yearlyGoal ->
            "Meta anual de $yearlyGoal obras concluída com sucesso! ($readCount lidas em $currentYear)"
        rawProjected >= yearlyGoal ->
            "No ritmo atual, você fechará $currentYear com ~$rawProjected quadrinhos (+${rawProjected - yearlyGoal} acima da meta)."
        rawProjected > 0 ->
            "Projeção atual: ~$rawProjected de $yearlyGoal obras em $currentYear. Leia +${max(1, yearlyGoal - readCount)} para atingir 100%."
        else ->
            "Registre sua primeira leitura para calcular sua projeção anual de $currentYear."
    }

    val palette = if (categoryPalette.isNotEmpty()) {
        categoryPalette
    } else {
        listOf(
            accentColor,
            secondaryColor,
            lerp(accentColor, secondaryColor, 0.5f),
            lerp(accentColor, Color.White, 0.25f),
            lerp(secondaryColor, Color.White, 0.25f),
            lerp(accentColor, Color.Black, 0.25f),
            lerp(secondaryColor, Color.Black, 0.25f)
        )
    }

    val totalEngagedCount = engagedComics.size.coerceAtLeast(1)

    val formatBreakdown = formatCounts.entries
        .sortedByDescending { it.value.first }
        .mapIndexed { idx, entry ->
            val count = entry.value.first
            val pages = entry.value.second
            val frac = (count.toFloat() / totalEngagedCount.toFloat()).coerceIn(0f, 1f)
            CategoryBreakdownItem(
                label = entry.key,
                count = count,
                pages = pages,
                percentage = (frac * 100f).roundToInt().coerceIn(0, 100),
                fraction = frac,
                color = palette[idx % palette.size]
            )
        }

    val publisherBreakdown = publisherCounts.entries
        .sortedByDescending { it.value.first }
        .take(6)
        .mapIndexed { idx, entry ->
            val count = entry.value.first
            val pages = entry.value.second
            val frac = (count.toFloat() / totalEngagedCount.toFloat()).coerceIn(0f, 1f)
            CategoryBreakdownItem(
                label = entry.key,
                count = count,
                pages = pages,
                percentage = (frac * 100f).roundToInt().coerceIn(0, 100),
                fraction = frac,
                color = palette[idx % palette.size]
            )
        }

    val sortedAuthors = authorMap.entries
        .sortedWith(compareByDescending<Map.Entry<String, Triple<Int, Int, MutableList<Float>>>> { it.value.first }
            .thenByDescending { it.value.second })
        .take(6)
    val topAuthorMaxWorks = (sortedAuthors.firstOrNull()?.value?.first ?: 1).coerceAtLeast(1)

    val topAuthors = sortedAuthors.mapIndexed { index, entry ->
        val works = entry.value.first
        val pages = entry.value.second
        val ratings = entry.value.third
        val avg = if (ratings.isNotEmpty()) ratings.average().toFloat() else 0f
        AuthorRankingItem(
            rank = index + 1,
            name = entry.key,
            worksCount = works,
            totalPages = pages,
            avgRating = avg,
            fractionOfTop = (works.toFloat() / topAuthorMaxWorks.toFloat()).coerceIn(0.15f, 1f)
        )
    }

    val monthlyBuckets = (0..11).map { m ->
        MonthlyReadingBucket(
            monthIndex = m,
            shortLabel = monthShortLabels[m],
            singleLetter = monthSingleLetters[m],
            fullName = monthFullNames[m],
            pagesRead = pagesByMonth[m],
            comicsRead = comicsByMonth[m]
        )
    }

    return ReadingStatisticsSummary(
        readComicsCount = readCount,
        readingComicsCount = readingCount,
        queueComicsCount = queueCount,
        yearlyGoal = yearlyGoal,
        totalPagesRead = totalPages,
        currentMonthIndex = activeMonthIdx,
        currentMonthPages = currentMonthPages,
        currentMonthComics = currentMonthComics,
        currentMonthName = monthFullNames[activeMonthIdx],
        dailyPagesPace = dailyPace,
        dailyPagesGoal = dailyGoal,
        activeMonthsCount = activeMonthsCount,
        projectedYearEndComics = rawProjected,
        projectionMessage = projectionMessage,
        isGoalOnTrack = isGoalOnTrack,
        monthlyBuckets = monthlyBuckets,
        formatBreakdown = formatBreakdown,
        publisherBreakdown = publisherBreakdown,
        topAuthors = topAuthors,
        averageRating = if (ratedCount > 0) ratingSum / ratedCount.toFloat() else 0f,
        ratedCount = ratedCount
    )
}

private fun normalizeFormatCategory(comic: ComicEntry): String {
    val raw = comic.format.lowercase(Locale.ROOT)
    val genre = comic.genre.lowercase(Locale.ROOT)
    return when {
        raw.contains("mang") || genre.contains("mang") || raw.contains("tanko") ->
            "Mangás"
        raw.contains("dura") || raw.contains("especial") || raw.contains("omnibus") || raw.contains("absolut") || raw.contains("deluxe") ->
            "Edições Especiais / Capa Dura"
        raw.contains("mensal") || raw.contains("grampo") || raw.contains("single") || (comic.pageCount in 1..48 && !raw.contains("tpb")) ->
            "Mensais (Grampo)"
        else ->
            "Encadernados (TPB)"
    }
}

private fun normalizePublisherCategory(comic: ComicEntry): String {
    val pub = comic.publisher.trim()
    val genre = comic.genre.trim()
    val lowerPub = pub.lowercase(Locale.ROOT)
    val lowerGenre = genre.lowercase(Locale.ROOT)
    return when {
        lowerPub.contains("dc") || lowerPub.contains("vertigo") || lowerGenre.contains("dc") -> "DC / Vertigo"
        lowerPub.contains("marvel") || lowerGenre.contains("marvel") -> "Marvel"
        lowerPub.contains("panini") -> "Panini"
        lowerPub.contains("dark horse") -> "Dark Horse"
        lowerPub.contains("image") -> "Image Comics"
        lowerPub.contains("jbc") || lowerPub.contains("shueisha") || lowerPub.contains("kodansha") || lowerGenre.contains("mang") -> "Mangás"
        pub.isNotBlank() -> pub
        genre.isNotBlank() -> genre
        else -> "Independente / Outros"
    }
}

/**
 * Mini-resumo Liquid Glass expansível exibido no topo do segmento Diário na aba Biblioteca.
 * Exibe resumo imediato (ex.: "364 páginas lidas este mês • 4 obras"), permite expandir métricas rápidas
 * e, ao tocar para abrir estatísticas detalhadas, navega para o Painel Completo de Estatísticas.
 */
@Composable
fun DiaryExpandableLiquidGlassSummary(
    comics: List<ComicEntry>,
    profile: ReaderProfile,
    onOpenFullStatistics: () -> Unit,
    onShareGoalSummary: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val fitnessPalette = rememberAppleFitnessPalette()
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val stats = remember(comics, profile, fitnessPalette) {
        computeReadingStatistics(
            comics = comics,
            profile = profile,
            accentColor = fitnessPalette.moveRingColor,
            secondaryColor = fitnessPalette.exerciseRingColor,
            categoryPalette = fitnessPalette.categoryColors
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("diary_monthly_summary_card")
            .contentMaterialSurface(RoundedCornerShape(26.dp))
            .clickable { onOpenFullStatistics() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                AppleActivityRingsGauge(
                    outerProgress = (stats.readComicsCount.toFloat() / stats.yearlyGoal.toFloat()).coerceIn(0f, 1f),
                    innerProgress = (stats.dailyPagesPace.toFloat() / stats.dailyPagesGoal.toFloat()).coerceIn(0f, 1f),
                    thirdProgress = (stats.activeMonthsCount.toFloat() / 12f).coerceIn(0f, 1f),
                    centerLabel = "",
                    sizeDp = 44.dp,
                    outerColor = fitnessPalette.moveRingColor,
                    innerColor = fitnessPalette.exerciseRingColor,
                    thirdColor = fitnessPalette.standRingColor,
                    showCenterLabel = false
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "RESUMO DE ${stats.currentMonthName.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = fitnessPalette.moveRingColor
                    )
                    Text(
                        text = "${stats.currentMonthPages} páginas lidas este mês • ${stats.currentMonthComics} ${if (stats.currentMonthComics == 1) "obra" else "obras"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textPrimary
                    )
                    Text(
                        text = "Toque para abrir os Círculos de Atividade e gráficos",
                        fontSize = 11.sp,
                        color = s.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (s.isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f))
                        .clickable { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Recolher prévia" else "Expandir prévia",
                        tint = s.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (s.isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f))
                        .clickable { onShareGoalSummary("MONTHLY") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LucideIcons.ShareIos,
                        contentDescription = "Compartilhar Resumo da Meta",
                        tint = fitnessPalette.moveRingColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(fitnessPalette.moveRingColor.copy(alpha = 0.16f))
                        .clickable { onOpenFullStatistics() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Abrir Estatísticas",
                        tint = fitnessPalette.moveRingColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(motion.tween(240)) + fadeIn(motion.tween(220)),
            exit = shrinkVertically(motion.tween(200)) + fadeOut(motion.tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniGlassMetricPill(
                        label = "META ANUAL",
                        value = "${stats.readComicsCount}/${stats.yearlyGoal}",
                        sub = "Obras em 2026",
                        accent = fitnessPalette.moveRingColor,
                        modifier = Modifier.weight(1f)
                    )
                    MiniGlassMetricPill(
                        label = "RITMO DIÁRIO",
                        value = "${stats.dailyPagesPace}/${stats.dailyPagesGoal}",
                        sub = "Páginas/dia",
                        accent = fitnessPalette.exerciseRingColor,
                        modifier = Modifier.weight(1f)
                    )
                    MiniGlassMetricPill(
                        label = "CONSTÂNCIA",
                        value = "${stats.activeMonthsCount}/12",
                        sub = "Meses ativos",
                        accent = fitnessPalette.standRingColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(fitnessPalette.moveRingColor.copy(alpha = 0.14f))
                        .clickable { onOpenFullStatistics() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ver gráficos de ritmo, formatos, editoras e autores",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = fitnessPalette.moveRingColor
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = fitnessPalette.moveRingColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniGlassMetricPill(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val s = LocalAppleMaterials.current.scheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (s.isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f))
            .border(
                0.5.dp,
                if (s.isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = FontWeight.Bold,
            color = s.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            fontSize = 9.5.sp,
            color = s.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Botão circular (-+) no estilo Apple Fitness (com anel de progresso ao redor de "-+").
 */
@Composable
private fun AppleRingPlusMinusButton(
    ringColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 36.dp
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.06f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = 2.4.dp.toPx()
            drawArc(
                color = ringColor.copy(alpha = 0.28f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 275f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "-+",
            fontSize = 10.5.sp,
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = FontWeight.ExtraBold,
            color = ringColor,
            letterSpacing = (-0.5).sp
        )
    }
}

/**
 * TELA DEDICADA DE ESTATÍSTICAS EM TELA CHEIA — Inspirada no Apple Fitness (Summary & Activity Rings):
 * - Cabeçalho "< Resumo" com seletor de meses em Mini Activity Rings (J F M A M J J A S O N D)
 * - Editor interativo de Metas no estilo Apple Fitness ("(-) 24 (+)" OBRAS/ANO e "(-) 30 (+)" PÁGS/DIA)
 * - 1. Card "Círculos de Atividade" com 3 anéis concêntricos (Movimento/Meta Anual →, Exercício/Ritmo Diário ↠, Constância ↑) + Projeção do Ano
 * - 2. Cards de Tendência Lado a Lado com Sparklines verticais (inspirados em Step Count & Step Distance do Apple Fitness)
 * - 3. Gráfico de Ritmo Mensal (Barras Verticais Jan–Dez com linha de base pontilhada, guias tracejadas e chips sem quebra de linha e com contraste garantido)
 * - 4. Distribuição por Formatos & Editoras (Donut multicor + Barras Horizontais)
 * - 5. Autores e Artistas Mais Lidos (Ranking com contraste garantido em todos os temas)
 */
@Composable
fun ReadingStatisticsScreen(
    comics: List<ComicEntry>,
    profile: ReaderProfile,
    appSettings: AppSettings,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenGoalSettings: () -> Unit,
    onUpdateGoals: (yearlyGoal: Int, dailyPagesGoal: Int) -> Unit = { _, _ -> },
    onShareGoalSummary: (String) -> Unit = {}
) {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    BackHandler(onBack = onBack)

    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val isDark = LocalIsDarkTheme.current
    val fitnessPalette = rememberAppleFitnessPalette()

    val moveColor = fitnessPalette.moveRingColor
    val exerciseColor = fitnessPalette.exerciseRingColor
    val standColor = fitnessPalette.standRingColor

    val stats = remember(comics, profile, fitnessPalette) {
        computeReadingStatistics(
            comics = comics,
            profile = profile,
            accentColor = moveColor,
            secondaryColor = exerciseColor,
            categoryPalette = fitnessPalette.categoryColors
        )
    }

    var selectedMonthIndex by rememberSaveable(stats.currentMonthIndex) {
        mutableIntStateOf(stats.currentMonthIndex)
    }
    var showGoalAdjuster by rememberSaveable { mutableStateOf(false) }
    var monthlyChartMode by rememberSaveable { mutableStateOf("PAGES") } // "PAGES" | "COMICS"
    var distributionTab by rememberSaveable { mutableStateOf("FORMATS") } // "FORMATS" | "PUBLISHERS"

    var animateCharts by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateCharts = true
    }
    val chartAnimProgress by animateFloatAsState(
        targetValue = if (animateCharts) 1f else 0f,
        animationSpec = motion.tween(durationMillis = 680, easing = EaseOutCubic),
        label = "statsChartProgress"
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("reading_statistics_screen"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 104.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CABEÇALHO APPLE FITNESS + BARRA DE 12 MINI ACTIVITY RINGS MENSAIS
        item(key = "stats-top-bar", contentType = "stats_header") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .testTag("stats_back_button")
                            .liquidGlassNavSurface(RoundedCornerShape(999.dp), elevation = 6.dp)
                            .clickable { onBack() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Voltar",
                            tint = s.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Resumo",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Text(
                        text = "${stats.monthlyBuckets.getOrNull(selectedMonthIndex)?.fullName ?: stats.currentMonthName}, 2026",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textSecondary,
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("stats_share_goal_card_button")
                                .size(34.dp)
                                .liquidGlassNavSurface(CircleShape, elevation = 6.dp)
                                .clickable { onShareGoalSummary("ANNUAL") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LucideIcons.ShareIos,
                                contentDescription = "Compartilhar Resumo da Meta",
                                tint = moveColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .testTag("stats_adjust_goals_button")
                                .liquidGlassNavSurface(RoundedCornerShape(999.dp), elevation = 6.dp)
                                .clickable { showGoalAdjuster = !showGoalAdjuster }
                                .padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = if (showGoalAdjuster) "Concluir" else "Metas",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = moveColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ATIVIDADE DE LEITURA • ${profile.name.uppercase()}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        color = s.textSecondary
                    )
                    Text(
                        text = "Estatísticas de Leitura",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textPrimary,
                        letterSpacing = (-0.6).sp
                    )
                }

                // Tira superior de 12 Mini Activity Rings (Jan..Dez) inspirada na barra superior do Apple Fitness
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(22.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val monthlyPagesGoal = (stats.dailyPagesGoal * 25).coerceAtLeast(30)
                    val monthlyComicsGoal = (stats.yearlyGoal / 12f).coerceAtLeast(1f)

                    stats.monthlyBuckets.forEachIndexed { index, bucket ->
                        val isSelected = index == selectedMonthIndex
                        val mOuter = (bucket.comicsRead.toFloat() / monthlyComicsGoal).coerceIn(0f, 1f)
                        val mInner = (bucket.pagesRead.toFloat() / monthlyPagesGoal.toFloat()).coerceIn(0f, 1f)
                        val mThird = if (bucket.pagesRead > 0 || bucket.comicsRead > 0) 1f else 0f

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedMonthIndex = index }
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) fitnessPalette.activeChipBg
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bucket.singleLetter,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) fitnessPalette.activeChipText else s.textSecondary
                                )
                            }

                            AppleActivityRingsGauge(
                                outerProgress = mOuter * chartAnimProgress,
                                innerProgress = mInner * chartAnimProgress,
                                thirdProgress = mThird * chartAnimProgress,
                                centerLabel = "",
                                sizeDp = 22.dp,
                                outerColor = moveColor,
                                innerColor = exerciseColor,
                                thirdColor = standColor,
                                showCenterLabel = false
                            )
                        }
                    }
                }

                // Editor Interativo de Metas no estilo Apple Fitness: (-) 24 (+) OBRAS/ANO e (-) 30 (+) PÁGINAS/DIA
                AnimatedVisibility(
                    visible = showGoalAdjuster,
                    enter = expandVertically(motion.tween(260)) + fadeIn(motion.tween(220)),
                    exit = shrinkVertically(motion.tween(220)) + fadeOut(motion.tween(180))
                ) {
                    AppleFitnessGoalAdjusterCard(
                        yearlyGoal = stats.yearlyGoal,
                        dailyPagesGoal = stats.dailyPagesGoal,
                        moveColor = moveColor,
                        exerciseColor = exerciseColor,
                        onUpdateGoals = onUpdateGoals,
                        onOpenFullProfileSettings = onOpenGoalSettings
                    )
                }
            }
        }

        // 1. TOPO: CARD "CÍRCULOS DE ATIVIDADE" (3 ANÉIS APPLE FITNESS + MÉTRICAS EMPILHADAS + PROJEÇÃO DO ANO)
        item(key = "stats-expanded-activity-rings", contentType = "stats_rings_card") {
            val outerRatio = (stats.readComicsCount.toFloat() / stats.yearlyGoal.toFloat()).coerceIn(0f, 1f)
            val innerRatio = (stats.dailyPagesPace.toFloat() / stats.dailyPagesGoal.toFloat()).coerceIn(0f, 1f)
            val thirdRatio = (stats.activeMonthsCount.toFloat() / 12f).coerceIn(0f, 1f)
            val outerPct = (outerRatio * 100f).roundToInt().coerceIn(0, 999)
            val innerPct = (innerRatio * 100f).roundToInt().coerceIn(0, 999)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .contentMaterialSurface(RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "METAS & RITMO • 2026",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = s.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = "Círculos de Atividade",
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (stats.isGoalOnTrack) exerciseColor.copy(alpha = 0.16f)
                                    else moveColor.copy(alpha = 0.16f)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (stats.isGoalOnTrack) "No Ritmo" else "Ativo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stats.isGoalOnTrack) exerciseColor else moveColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        AppleRingPlusMinusButton(
                            ringColor = moveColor,
                            onClick = { showGoalAdjuster = !showGoalAdjuster },
                            sizeDp = 34.dp
                        )
                    }
                }

                // Layout principal inspirado no Apple Fitness: Anéis grandes à esquerda + Métricas coloridas empilhadas à direita
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleActivityRingsGauge(
                        outerProgress = outerRatio * chartAnimProgress,
                        innerProgress = innerRatio * chartAnimProgress,
                        thirdProgress = thirdRatio * chartAnimProgress,
                        centerLabel = "",
                        sizeDp = 138.dp,
                        outerColor = moveColor,
                        innerColor = exerciseColor,
                        thirdColor = standColor,
                        showCenterLabel = false
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Anel 1 (Externo): Meta Anual de Quadrinhos (Movimento →)
                        AppleFitnessRingMetricBlock(
                            label = "Meta Anual ($outerPct%)",
                            currentValue = "${stats.readComicsCount}",
                            goalValue = "${stats.yearlyGoal}",
                            unit = "OBRAS",
                            color = moveColor
                        )

                        // Anel 2 (Médio): Ritmo Diário de Páginas (Exercício ↠)
                        AppleFitnessRingMetricBlock(
                            label = "Ritmo Diário ($innerPct%)",
                            currentValue = "${stats.dailyPagesPace}",
                            goalValue = "${stats.dailyPagesGoal}",
                            unit = "PÁGS/DIA",
                            color = exerciseColor
                        )

                        // Anel 3 (Interno): Constância Mensal no Ano (Assiduidade ↑)
                        AppleFitnessRingMetricBlock(
                            label = "Constância no Ano",
                            currentValue = "${stats.activeMonthsCount}",
                            goalValue = "12",
                            unit = "MESES",
                            color = standColor
                        )
                    }
                }

                // Banner interno de Projeção de Conclusão do Ano
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (s.isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            0.7.dp,
                            moveColor.copy(alpha = 0.30f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(moveColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (stats.isGoalOnTrack) Icons.Default.CheckCircle else Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = moveColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "PROJEÇÃO DE CONCLUSÃO DO ANO",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = moveColor
                        )
                        Text(
                            text = stats.projectionMessage,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = s.textPrimary
                        )
                    }
                }
            }
        }

        // 2. CARDS DUPLOS DE TENDÊNCIA COM SPARKLINES (INSPIRADOS EM "STEP COUNT" E "STEP DISTANCE" DO APPLE FITNESS)
        item(key = "stats-dual-sparkline-cards", contentType = "stats_sparkline_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppleFitnessSparklineCard(
                    title = "Páginas Lidas",
                    subtitle = "Acumulado 2026",
                    mainValue = "${stats.totalPagesRead}",
                    unit = "PÁGS",
                    accentColor = fitnessPalette.sparklinePurple,
                    monthlyValues = stats.monthlyBuckets.map { it.pagesRead },
                    animProgress = chartAnimProgress,
                    onClick = { monthlyChartMode = "PAGES" },
                    modifier = Modifier.weight(1f)
                )

                AppleFitnessSparklineCard(
                    title = "Acervo & Ritmo",
                    subtitle = "${stats.readingComicsCount} lendo • ${String.format(Locale.US, "%.1f★", stats.averageRating)}",
                    mainValue = "${stats.readComicsCount}",
                    unit = if (stats.readComicsCount == 1) "OBRA" else "OBRAS",
                    accentColor = fitnessPalette.sparklineCyan,
                    monthlyValues = stats.monthlyBuckets.map {
                        it.comicsRead * 10 + (if (it.pagesRead > 0) 4 else 0)
                    },
                    animProgress = chartAnimProgress,
                    onClick = { monthlyChartMode = "COMICS" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. GRÁFICO DE RITMO MENSAL (BARRAS VERTICAIS ESTILO APPLE FITNESS - JAN A DEZ)
        item(key = "stats-monthly-bar-chart", contentType = "stats_monthly_chart") {
            val activeChartColor = if (monthlyChartMode == "PAGES") moveColor else exerciseColor
            val values = remember(stats.monthlyBuckets, monthlyChartMode) {
                stats.monthlyBuckets.map {
                    if (monthlyChartMode == "PAGES") it.pagesRead else it.comicsRead
                }
            }
            val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1)
            val selectedBucket = stats.monthlyBuckets.getOrNull(selectedMonthIndex)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .contentMaterialSurface(RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Topo do card com título e seletor Liquid Glass padrão do app (idêntico a Estante | Diário)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ÚLTIMOS 12 MESES (JAN – DEZ)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = s.textSecondary
                        )
                        Text(
                            text = "Ritmo Mensal",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary
                        )
                    }

                    AppleSegmentedPicker(
                        value = monthlyChartMode,
                        onChange = { monthlyChartMode = it },
                        options = listOf(
                            SegmentOption("PAGES", "Páginas", Icons.Default.BarChart),
                            SegmentOption("COMICS", "Obras Lidas", LucideIcons.Library)
                        )
                    )
                }

                // Destaque numérico estilo Apple Fitness ("299/400 CAL" + botão (-+))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = if (selectedBucket != null) {
                                "Destaque de ${selectedBucket.fullName}"
                            } else {
                                "Total Anual"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = s.textSecondary
                        )
                        Text(
                            text = buildAnnotatedString {
                                if (monthlyChartMode == "PAGES") {
                                    val monthPages = selectedBucket?.pagesRead ?: stats.currentMonthPages
                                    val monthGoal = (stats.dailyPagesGoal * 30).coerceAtLeast(30)
                                    withStyle(
                                        SpanStyle(
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeChartColor
                                        )
                                    ) {
                                        append("$monthPages/$monthGoal")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeChartColor
                                        )
                                    ) {
                                        append(" PÁGS")
                                    }
                                } else {
                                    withStyle(
                                        SpanStyle(
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeChartColor
                                        )
                                    ) {
                                        append("${stats.readComicsCount}/${stats.yearlyGoal}")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeChartColor
                                        )
                                    ) {
                                        append(" OBRAS")
                                    }
                                }
                            },
                            fontFamily = SpaceGroteskTabularFamily
                        )
                    }

                    AppleRingPlusMinusButton(
                        ringColor = activeChartColor,
                        onClick = { showGoalAdjuster = !showGoalAdjuster },
                        sizeDp = 36.dp
                    )
                }

                val gridLineColor = if (s.isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.12f)
                val barTrackColor = if (s.isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Escala superior esquerda + valores acima das barras ativas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MÁX: $maxVal ${if (monthlyChartMode == "PAGES") "PÁGS" else "OBRAS"}",
                            fontSize = 9.5.sp,
                            fontFamily = SpaceGroteskTabularFamily,
                            fontWeight = FontWeight.Bold,
                            color = s.textSecondary
                        )
                        Text(
                            text = "Toque em um mês para inspecionar",
                            fontSize = 9.5.sp,
                            color = s.textTertiary
                        )
                    }

                    // Valores numéricos no topo das barras com leitura
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        stats.monthlyBuckets.forEachIndexed { idx, _ ->
                            val v = values[idx]
                            val isSel = idx == selectedMonthIndex
                            Text(
                                text = if (v > 0) "$v" else "",
                                fontSize = 9.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel || v == maxVal) activeChartColor else s.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Canvas Apple Fitness com linhas pontilhadas horizontais, pontos de base e barras verticais brilhantes
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val dotEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 6f), 0f)

                        // 3 linhas horizontais pontilhadas estilo Apple Fitness
                        listOf(0.08f, 0.50f, 0.92f).forEach { yFrac ->
                            val y = h * yFrac
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dotEffect
                            )
                        }

                        val slotWidth = w / 12f
                        val barWidth = (slotWidth * 0.48f).coerceAtLeast(7f)
                        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        val baselineY = h - 4.dp.toPx()

                        for (i in 0 until 12) {
                            val value = values[i]
                            val centerX = slotWidth * i + slotWidth / 2f
                            val leftX = centerX - barWidth / 2f
                            val isSelectedSlot = i == selectedMonthIndex

                            // Trilho translúcido sutil
                            drawRoundRect(
                                color = if (isSelectedSlot) activeChartColor.copy(alpha = 0.10f) else barTrackColor,
                                topLeft = Offset(leftX, 0f),
                                size = Size(barWidth, h),
                                cornerRadius = cornerRadius
                            )

                            // Ponto colorido na linha de base (idêntico à linha pontilhada inferior do Apple Fitness)
                            drawCircle(
                                color = activeChartColor.copy(alpha = if (value > 0) 0.95f else 0.42f),
                                radius = (barWidth * 0.28f).coerceIn(2.2f, 4.2f),
                                center = Offset(centerX, baselineY)
                            )

                            if (value > 0) {
                                val normalized = (value.toFloat() / maxVal.toFloat()).coerceIn(0.10f, 1f)
                                val barHeight = (h * normalized * chartAnimProgress).coerceAtLeast(barWidth)
                                val topY = h - barHeight

                                val topHighlight = Color(
                                    red = (activeChartColor.red * 0.75f + 0.25f).coerceIn(0f, 1f),
                                    green = (activeChartColor.green * 0.75f + 0.25f).coerceIn(0f, 1f),
                                    blue = (activeChartColor.blue * 0.75f + 0.25f).coerceIn(0f, 1f),
                                    alpha = 1f
                                )

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(topHighlight, activeChartColor),
                                        startY = topY,
                                        endY = h
                                    ),
                                    topLeft = Offset(leftX, topY),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = cornerRadius
                                )
                            }
                        }
                    }

                    // Rótulos dos 12 meses (Jan a Dez) clicáveis
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        stats.monthlyBuckets.forEachIndexed { idx, bucket ->
                            val isActive = values[idx] > 0
                            val isSelected = idx == selectedMonthIndex
                            Text(
                                text = bucket.shortLabel,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected || isActive) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected -> activeChartColor
                                    isActive -> s.textPrimary
                                    else -> s.textTertiary
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMonthIndex = idx }
                            )
                        }
                    }

                    // Rodapé "TOTAL" colorido estilo Apple Fitness ("TOTAL 1,296 CAL")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (monthlyChartMode == "PAGES") {
                                "TOTAL NO ANO: ${stats.totalPagesRead} PÁGINAS"
                            } else {
                                "TOTAL NO ANO: ${stats.readComicsCount} OBRAS CONCLUÍDAS"
                            },
                            fontSize = 11.sp,
                            fontFamily = SpaceGroteskTabularFamily,
                            fontWeight = FontWeight.Bold,
                            color = activeChartColor
                        )
                        Text(
                            text = "RITMO: ${stats.dailyPagesPace} PÁGS/DIA",
                            fontSize = 10.5.sp,
                            fontFamily = SpaceGroteskTabularFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textSecondary
                        )
                    }
                }
            }
        }

        // 4. DISTRIBUIÇÃO POR FORMATOS & EDITORAS (DONUT MULTICOR + BARRAS HORIZONTAIS)
        item(key = "stats-format-publisher-distribution", contentType = "stats_distribution_card") {
            val activeItems = if (distributionTab == "FORMATS") stats.formatBreakdown else stats.publisherBreakdown

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .contentMaterialSurface(RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "PERFIL DO ACERVO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = s.textSecondary
                        )
                        Text(
                            text = "Formatos & Editoras",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary
                        )
                    }

                    // Seletor Liquid Glass padrão do app (idêntico a Estante | Diário)
                    AppleSegmentedPicker(
                        value = distributionTab,
                        onChange = { distributionTab = it },
                        options = listOf(
                            SegmentOption("FORMATS", "Formatos", LucideIcons.Library),
                            SegmentOption("PUBLISHERS", "Editoras", Icons.Default.BarChart)
                        )
                    )
                }

                if (activeItems.isEmpty()) {
                    Text(
                        text = "Adicione quadrinhos lidos ou em leitura para visualizar a distribuição por categoria.",
                        fontSize = 12.sp,
                        color = s.textSecondary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gráfico Donut em Canvas com anel de trilha translúcido
                        Box(
                            modifier = Modifier.size(116.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 15.dp.toPx()
                                val diameter = size.minDimension - strokeWidth
                                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                                val arcSize = Size(diameter, diameter)
                                var startAngle = -90f
                                val gapDegrees = if (activeItems.size > 1) 5f else 0f

                                // Trilho base
                                drawArc(
                                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth)
                                )

                                activeItems.forEach { item ->
                                    val sweep = (item.fraction * 360f * chartAnimProgress - gapDegrees).coerceAtLeast(3f)
                                    drawArc(
                                        color = item.color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    startAngle += item.fraction * 360f * chartAnimProgress
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${activeItems.sumOf { it.count }}",
                                    fontSize = 21.sp,
                                    fontFamily = SpaceGroteskTabularFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                                Text(
                                    text = if (activeItems.sumOf { it.count } == 1) "obra" else "obras",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                            }
                        }

                        // Legenda rápida das top categorias ao lado do Donut
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeItems.take(4).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(item.color)
                                        )
                                        Text(
                                            text = item.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = s.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${item.percentage}%",
                                        fontSize = 12.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = item.color
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                    // Barras horizontais detalhadas com quantidade e páginas
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        activeItems.forEach { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = s.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "${item.count} ${if (item.count == 1) "obra" else "obras"} • ${item.pages} págs (${item.percentage}%)",
                                        fontSize = 10.5.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = s.textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (s.isLight) Color.Black.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.09f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((item.fraction * chartAnimProgress).coerceIn(0.05f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(item.color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. AUTORES E ARTISTAS MAIS LIDOS (LISTA COMPACTA EM CARD DE VIDRO COM CONTRASTE GARANTIDO)
        item(key = "stats-top-authors-ranking", contentType = "stats_authors_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .contentMaterialSurface(RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "ROTEIRISTAS & DESENHISTAS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = s.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = "Autores e Artistas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(moveColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = moveColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (stats.topAuthors.isEmpty()) {
                    Text(
                        text = "Seus roteiristas e artistas mais lidos aparecerão aqui conforme você registrar suas leituras.",
                        fontSize = 12.sp,
                        color = s.textSecondary
                    )
                } else {
                    stats.topAuthors.forEachIndexed { idx, author ->
                        if (idx > 0) {
                            HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                        }
                        val badgeColor = when (author.rank) {
                            1 -> moveColor
                            2 -> exerciseColor
                            3 -> standColor
                            else -> Color.Transparent
                        }
                        val hasColoredBadge = author.rank in 1..3
                        val badgeBg = if (hasColoredBadge) {
                            badgeColor
                        } else if (s.isLight) {
                            Color.Black.copy(alpha = 0.08f)
                        } else {
                            Color.White.copy(alpha = 0.12f)
                        }
                        val badgeTextColor = if (hasColoredBadge) {
                            contrastingTextOn(badgeColor)
                        } else {
                            s.textPrimary
                        }
                        val barColor = fitnessPalette.categoryColors[idx % fitnessPalette.categoryColors.size]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${author.rank}",
                                    fontSize = 11.5.sp,
                                    fontFamily = SpaceGroteskTabularFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeTextColor
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = author.name,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = s.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${author.worksCount} ${if (author.worksCount == 1) "obra" else "obras"}",
                                        fontSize = 12.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (s.isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.09f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((author.fractionOfTop * chartAnimProgress).coerceIn(0.08f, 1f))
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(barColor)
                                        )
                                    }

                                    Text(
                                        text = "${author.totalPages} págs" +
                                            if (author.avgRating > 0f) String.format(Locale.US, " • %.1f★", author.avgRating) else "",
                                        fontSize = 10.5.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        color = s.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bloco de métrica empilhada ao lado dos Círculos de Atividade (idêntico ao "Move / 370/800 CAL" do Apple Fitness).
 */
@Composable
private fun AppleFitnessRingMetricBlock(
    label: String,
    currentValue: String,
    goalValue: String,
    unit: String,
    color: Color
) {
    val s = LocalAppleMaterials.current.scheme
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = s.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                ) {
                    append("$currentValue/$goalValue")
                }
                withStyle(
                    SpanStyle(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                ) {
                    append(" $unit")
                }
            },
            fontFamily = SpaceGroteskTabularFamily,
            maxLines = 1,
            softWrap = false
        )
    }
}


/**
 * Card compacto de Sparkline estilo Apple Fitness ("Step Count / Today / 2,210" e "Step Distance / Today / 2.77 MI").
 */
@Composable
private fun AppleFitnessSparklineCard(
    title: String,
    subtitle: String,
    mainValue: String,
    unit: String,
    accentColor: Color,
    monthlyValues: List<Int>,
    animProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppleMaterials.current.scheme
    val isDark = LocalIsDarkTheme.current
    val maxVal = (monthlyValues.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(
        modifier = modifier
            .contentMaterialSurface(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = s.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = s.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = s.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    ) {
                        append(mainValue)
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    ) {
                        append(" $unit")
                    }
                },
                fontFamily = SpaceGroteskTabularFamily,
                maxLines = 1
            )
        }

        // Sparkline vertical com linhas guia de fundo estilo Apple Fitness
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            val w = size.width
            val h = size.height
            val guideColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)

            // 24 linhas verticais finas de grade (como nos cards Step Count / Step Distance)
            val totalGuides = 24
            for (g in 0 until totalGuides) {
                val gx = (w / (totalGuides - 1).toFloat()) * g
                drawLine(
                    color = guideColor,
                    start = Offset(gx, 0f),
                    end = Offset(gx, h),
                    strokeWidth = 0.8.dp.toPx()
                )
            }

            // 12 barras finas representando Jan..Dez
            val slotW = w / 12f
            val barW = (slotW * 0.42f).coerceAtLeast(3.5f)
            for (i in 0 until minOf(12, monthlyValues.size)) {
                val v = monthlyValues[i]
                val cx = slotW * i + slotW / 2f
                if (v > 0) {
                    val norm = (v.toFloat() / maxVal.toFloat()).coerceIn(0.16f, 1f)
                    val bh = (h * norm * animProgress).coerceAtLeast(barW)
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(cx - barW / 2f, h - bh),
                        size = Size(barW, bh),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                } else {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.38f),
                        radius = 1.6.dp.toPx(),
                        center = Offset(cx, h - 2.dp.toPx())
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Jan", "Abr", "Jul", "Out").forEach { lbl ->
                Text(
                    text = lbl,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = s.textTertiary
                )
            }
        }
    }
}

/**
 * Editor de Metas inspirado na tela "Altere a sua meta de movimento diário: (-) 360 (+)" do Apple Fitness.
 */
@Composable
private fun AppleFitnessGoalAdjusterCard(
    yearlyGoal: Int,
    dailyPagesGoal: Int,
    moveColor: Color,
    exerciseColor: Color,
    onUpdateGoals: (yearlyGoal: Int, dailyPagesGoal: Int) -> Unit,
    onOpenFullProfileSettings: () -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .contentMaterialSurface(RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "AJUSTE RÁPIDO DE METAS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = moveColor
            )
            Text(
                text = "Altere suas metas de leitura",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = s.textPrimary
            )
        }

        // Stepper 1: Meta Anual de Obras (Anel Vermelho/Rosa)
        AppleFitnessStepperRow(
            title = "META ANUAL DE QUADRINHOS",
            value = yearlyGoal,
            unitLabel = "OBRAS/ANO",
            color = moveColor,
            onDecrement = { onUpdateGoals((yearlyGoal - 1).coerceAtLeast(1), dailyPagesGoal) },
            onIncrement = { onUpdateGoals((yearlyGoal + 1).coerceAtMost(500), dailyPagesGoal) }
        )

        HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

        // Stepper 2: Meta de Ritmo Diário (Anel Verde)
        AppleFitnessStepperRow(
            title = "META DE RITMO DIÁRIO",
            value = dailyPagesGoal,
            unitLabel = "PÁGINAS/DIA",
            color = exerciseColor,
            onDecrement = { onUpdateGoals(yearlyGoal, (dailyPagesGoal - 5).coerceAtLeast(5)) },
            onIncrement = { onUpdateGoals(yearlyGoal, (dailyPagesGoal + 5).coerceAtMost(500)) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (s.isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.06f))
                .clickable { onOpenFullProfileSettings() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Editar perfil completo do leitor",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = s.textSecondary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = s.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AppleFitnessStepperRow(
    title: String,
    value: Int,
    unitLabel: String,
    color: Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val s = LocalAppleMaterials.current.scheme
    val iconTint = contrastingTextOn(color)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = s.textSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onDecrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Diminuir",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$value",
                    fontSize = 34.sp,
                    fontFamily = SpaceGroteskTabularFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = s.textPrimary
                )
                Text(
                    text = unitLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = color
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aumentar",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
