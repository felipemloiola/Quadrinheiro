package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlin.math.min
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import com.kyant.shapes.RoundedRectangle
import com.kyant.backdrop.catalog.components.LiquidButton
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.data.CuratedCatalog
import com.example.data.PortugueseComicTranslator
import com.example.data.SeriesSequenceEngine
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ComicMoment
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.viewmodel.ManualComicForm
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive Apple iOS Mini Calendar Picker for selecting `readDate` (`YYYY-MM-DD`).
 */
@Composable
fun AppleMiniCalendarPicker(
    selectedDateStr: String,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

    val parsedParts = remember(selectedDateStr) {
        val parts = selectedDateStr.trim().split("-")
        val now = Calendar.getInstance()
        val y = parts.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.YEAR)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (now.get(Calendar.MONTH) + 1)).coerceIn(1, 12)
        val d = (parts.getOrNull(2)?.toIntOrNull() ?: now.get(Calendar.DAY_OF_MONTH)).coerceIn(1, 31)
        Triple(y, m, d)
    }

    var viewYear by remember(parsedParts.first) { mutableIntStateOf(parsedParts.first) }
    var viewMonth by remember(parsedParts.second) { mutableIntStateOf(parsedParts.second) } // 1..12

    val monthNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )
    val weekDays = listOf("D", "S", "T", "Q", "Q", "S", "S")

    val cal = remember(viewYear, viewMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val firstDayOfWeekOffset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (s.isLight) Color.White.copy(alpha = 0.65f)
                else Color.Black.copy(alpha = 0.28f)
            )
            .border(
                0.5.dp,
                if (s.isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month/Year Header + Navigation + "Hoje"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${monthNames[(viewMonth - 1).coerceIn(0, 11)]} $viewYear",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = s.textPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(s.accentColor.copy(alpha = 0.16f))
                        .clickable {
                            val today = Calendar.getInstance()
                            val ty = today.get(Calendar.YEAR)
                            val tm = today.get(Calendar.MONTH) + 1
                            val td = today.get(Calendar.DAY_OF_MONTH)
                            viewYear = ty
                            viewMonth = tm
                            onSelectDate(String.format(Locale.US, "%04d-%02d-%02d", ty, tm, td))
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Hoje",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.accentColor
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (s.isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.10f))
                        .clickable {
                            if (viewMonth == 1) {
                                viewMonth = 12
                                viewYear -= 1
                            } else {
                                viewMonth -= 1
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mês anterior",
                        tint = s.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (s.isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.10f))
                        .clickable {
                            if (viewMonth == 12) {
                                viewMonth = 1
                                viewYear += 1
                            } else {
                                viewMonth += 1
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Próximo mês",
                        tint = s.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Weekday header
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { wd ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = wd,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textTertiary
                    )
                }
            }
        }

        // Days grid
        val totalSlots = firstDayOfWeekOffset + daysInMonth
        val rows = (totalSlots + 6) / 7
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (c in 0 until 7) {
                    val slotIdx = r * 7 + c
                    val dayNum = slotIdx - firstDayOfWeekOffset + 1
                    if (dayNum in 1..daysInMonth) {
                        val isSelected = parsedParts.first == viewYear &&
                            parsedParts.second == viewMonth &&
                            parsedParts.third == dayNum

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) s.accentColor else Color.Transparent)
                                    .clickable {
                                        val formatted = String.format(
                                            Locale.US,
                                            "%04d-%02d-%02d",
                                            viewYear,
                                            viewMonth,
                                            dayNum
                                        )
                                        onSelectDate(formatted)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 12.sp,
                                    fontFamily = SpaceGroteskTabularFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else s.textPrimary
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(32.dp))
                    }
                }
            }
        }
    }
}

/**
 * Camada de fundo Liquid Glass na GPU para o Menu de Registro da HQ (idêntica à arquitetura óptica do menu inferior):
 * - Substrato translúcido adaptativo ao tema + refração suave da capa
 * - Pipeline de 4 camadas na GPU (`Modifier.liquidGlass(radiusX = 30f, radiusY = 30f)`) com isolamento `.graphicsLayer`,
 *   `RenderEffect.createBlurEffect`, luminância vertical (`0.08f -> 0.03f`), borda Fresnel 2.5D (`1.2.dp`, `0.30f -> Transparent`)
 *   e interatividade radial ao toque.
 */
@Composable
fun LiquidGlassSheetRefractiveLayer(
    coverUrl: String,
    scrollOffsetPx: Float = 0f,
    dragOffsetY: Float = 0f,
    cornerRadiusDp: Float = 24f,
    sheetShape: Shape = RoundedRectangle(cornerRadiusDp.dp),
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val context = LocalContext.current
    val rootBackdrop = rememberCurrentBackdrop()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Vidro óptico refrativo usando exclusivamente o backdrop raiz do aplicativo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassRegistrationSheet(
                    backdrop = rootBackdrop,
                    cornerRadiusDp = cornerRadiusDp
                )
        )

        // 2. Capa difusa em baixa resolução (24x36) para blur instantâneo na GPU sem queda de FPS na rolagem
        if (coverUrl.isNotBlank()) {
            val cleanCover = remember(coverUrl) {
                coverUrl.trim().replace("http://", "https://")
            }
            val blurredRequest = remember(cleanCover, context) {
                ImageRequest.Builder(context)
                    .data(cleanCover)
                    .size(24, 36)
                    .precision(coil.size.Precision.INEXACT)
                    .crossfade(false)
                    .allowHardware(true)
                    .allowRgb565(true)
                    .memoryCacheKey(cleanCover + "_sheet_amb")
                    .diskCacheKey(cleanCover + "_sheet_amb")
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.15f
                        scaleY = 1.18f
                    }
                    .alpha(if (s.isLight) 0.14f else 0.20f)
            ) {
                AsyncImage(
                    model = blurredRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CatalogStatusSegmentedBar(
    value: ReadingStatus,
    onChange: (ReadingStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    AppleSegmentedPicker(
        options = listOf(
            SegmentOption(ReadingStatus.READ, "Lido"),
            SegmentOption(ReadingStatus.READING, "Lendo"),
            SegmentOption(ReadingStatus.WANT_TO_READ, "Quero Ler")
        ),
        value = value,
        onChange = onChange,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Card horizontal interativo para visualizar e navegar pelas edições da mesma sequência
 * (ex.: Batman #1, #2, #3, #4, #5 de Matt Fraction).
 */
@Composable
fun ComicSequenceCarouselCard(
    currentComic: ComicEntry,
    allUserComics: List<ComicEntry>,
    onlineSequencePool: List<ComicEntry>,
    spine3d: Boolean,
    onSelectSequenceIssue: (ComicEntry) -> Unit,
    onAddNextIssue: ((ComicEntry) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

    val sequenceGroup = remember(currentComic, allUserComics, onlineSequencePool) {
        SeriesSequenceEngine.findSequenceForComic(
            target = currentComic,
            userComics = allUserComics,
            catalogComics = CuratedCatalog.curatedComicsCatalog,
            extraOnlineComics = onlineSequencePool
        )
    }

    val sequenceItems = sequenceGroup.issues
    val baseSeriesTitle = sequenceGroup.baseSeriesTitle
    val primaryAuthor = sequenceGroup.primaryAuthor
    val currentIssueNum = remember(currentComic) {
        SeriesSequenceEngine.inferIssueNumber(currentComic)
    }
    val inShelfCount = sequenceGroup.savedCount
    val nextIssueNumber = sequenceGroup.nextIssueNumber

    // Mostra o carrossel sempre que houver mais de 1 edição na sequência ou quando a HQ tiver número de edição (#1, Vol. 1...)
    if (sequenceItems.size <= 1 && currentIssueNum <= 0 && currentComic.seriesName.isBlank()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .inputWellSurface(RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = LucideIcons.Layers,
                        contentDescription = null,
                        tint = s.accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SEQUÊNCIA DA SÉRIE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = s.accentColor
                    )
                }
                Text(
                    text = buildString {
                        append(baseSeriesTitle)
                        if (primaryAuthor.isNotBlank()) {
                            append(" • ")
                            append(primaryAuthor)
                        }
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = s.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(s.accentColor.copy(alpha = 0.14f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$inShelfCount/${sequenceItems.size} na estante",
                    fontSize = 10.5.sp,
                    fontFamily = SpaceGroteskTabularFamily,
                    fontWeight = FontWeight.Bold,
                    color = s.accentColor
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            sequenceItems.forEach { seqItem ->
                val isCurrent = seqItem.comic.id == currentComic.id ||
                    (currentComic.externalId.isNotBlank() && seqItem.comic.externalId == currentComic.externalId) ||
                    (currentIssueNum > 0 && seqItem.issueNumber == currentIssueNum)

                val userStatus = seqItem.savedEntry?.status
                val statusBadgeText = when (userStatus) {
                    ReadingStatus.READ -> "✓ Lido"
                    ReadingStatus.READING -> "Lendo"
                    ReadingStatus.WANT_TO_READ -> "Fila"
                    null -> "+ Ver"
                }
                val statusBadgeBg = when (userStatus) {
                    ReadingStatus.READ -> Color(0xFF10B981).copy(alpha = 0.20f)
                    ReadingStatus.READING -> s.accentColor.copy(alpha = 0.20f)
                    ReadingStatus.WANT_TO_READ -> Color(0xFFF59E0B).copy(alpha = 0.20f)
                    null -> if (s.isLight) Color.Black.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.10f)
                }
                val statusBadgeColor = when (userStatus) {
                    ReadingStatus.READ -> if (s.isLight) Color(0xFF047857) else Color(0xFF34D399)
                    ReadingStatus.READING -> s.accentColor
                    ReadingStatus.WANT_TO_READ -> if (s.isLight) Color(0xFFB45309) else Color(0xFFFBBF24)
                    null -> s.textSecondary
                }

                Column(
                    modifier = Modifier
                        .width(78.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCurrent) s.accentColor.copy(alpha = if (s.isLight) 0.14f else 0.22f)
                            else if (s.isLight) Color.White.copy(alpha = 0.65f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            width = if (isCurrent) 1.5.dp else 0.6.dp,
                            color = if (isCurrent) s.accentColor
                            else if (s.isLight) Color.Black.copy(alpha = 0.08f)
                            else Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectSequenceIssue(seqItem.comic) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        BookCover(
                            src = seqItem.comic.coverUrl,
                            title = seqItem.comic.title,
                            authors = seqItem.comic.authors,
                            publisher = seqItem.comic.publisher,
                            year = seqItem.comic.releaseYear,
                            spine3d = spine3d,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Issue badge (#1, #2, #3... ou ano quando obra sem número)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isCurrent) s.accentColor
                                    else Color.Black.copy(alpha = 0.75f)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (seqItem.issueNumber > 0) "#${seqItem.issueNumber}" else "${seqItem.comic.releaseYear}",
                                fontSize = 9.5.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Text(
                        text = if (seqItem.issueNumber > 0) "Edição #${seqItem.issueNumber}" else seqItem.comic.title,
                        fontSize = 10.5.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isCurrent) s.accentColor else s.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(statusBadgeBg)
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusBadgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusBadgeColor,
                            maxLines = 1
                        )
                    }
                }
            }

            // Card rápido para adicionar a próxima edição da sequência (#N+1)
            if (onAddNextIssue != null) {
                Column(
                    modifier = Modifier
                        .width(78.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (s.isLight) Color.White.copy(alpha = 0.50f)
                            else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            width = 1.dp,
                            color = s.accentColor.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            val nextTemplate = SeriesSequenceEngine.createNextIssueTemplate(
                                fromComic = currentComic,
                                nextIssueNumber = nextIssueNumber
                            )
                            onAddNextIssue(nextTemplate)
                        }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(s.accentColor.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "+ #$nextIssueNumber",
                                fontSize = 14.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = s.accentColor
                            )
                            Text(
                                text = "Próxima",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = s.textSecondary
                            )
                        }
                    }

                    Text(
                        text = "Edição #$nextIssueNumber",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.accentColor,
                        maxLines = 1
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(s.accentBg)
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Criar",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.accentText
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4.7 Modal de Catalogação e Diário (`selectedComic`) — SwiftUI `.sheet` + `.inspector`:
 * - Novo Liquid Glass na GPU (idêntico ao menu inferior) com refração óptica, luminância vertical e borda Fresnel 2.5D de 1.2.dp
 * - Estado local `draftComic` isolado para zero recomposição da árvore principal durante digitação, avaliação e sliders
 * - Barra superior arrastável na GPU via `graphicsLayer` (para cima -> Tela Cheia; para baixo -> Fechar menu)
 * - Seleção de estrelas clicável e arrastável continuamente
 * - Mini calendário interativo ao tocar em "Data da leitura".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogSheetModal(
    selectedComic: ComicEntry,
    isExistingInShelf: Boolean,
    isSheetExpanded: Boolean,
    appSettings: AppSettings,
    allUserComics: List<ComicEntry> = emptyList(),
    onlineSequencePool: List<ComicEntry> = emptyList(),
    onSelectSequenceComic: (ComicEntry) -> Unit = {},
    onUpdateComic: ((ComicEntry) -> ComicEntry) -> Unit,
    onToggleExpand: () -> Unit,
    onSetExpanded: (Boolean) -> Unit = { _ -> onToggleExpand() },
    onSave: (ComicEntry) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val localizedInitial = remember(selectedComic) {
        com.example.data.PortugueseComicTranslator.localizeComic(selectedComic)
    }
    var draftComic by remember(selectedComic.id) { mutableStateOf(localizedInitial) }
    var comicTagInput by remember(selectedComic.id) { mutableStateOf("") }
    var showMiniCalendar by remember(selectedComic.id) { mutableStateOf(false) }
    var momentNoteInput by remember(selectedComic.id) { mutableStateOf("") }
    var momentPageInput by remember(selectedComic.id) {
        mutableStateOf(if (selectedComic.currentPage > 0) selectedComic.currentPage.toString() else "")
    }

    fun buildFinalComicForSave(): ComicEntry {
        var finalComic = draftComic
        val pendingTag = comicTagInput.trim().replace(",", "")
        if (pendingTag.isNotEmpty() && !finalComic.tags.contains(pendingTag)) {
            finalComic = finalComic.copy(tags = finalComic.tags + pendingTag)
        }
        val pendingNote = momentNoteInput.trim()
        if (pendingNote.isNotEmpty()) {
            val pageNum = momentPageInput.toIntOrNull() ?: 0
            val today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val newMoment = ComicMoment(
                id = "moment_${System.currentTimeMillis()}_${(100..999).random()}",
                page = pageNum,
                tag = "",
                note = pendingNote,
                date = finalComic.readDate.ifBlank { today },
                isSpoiler = false
            )
            finalComic = finalComic.copy(moments = finalComic.moments + newMoment)
        }
        return finalComic
    }
    val sheetLazyListState = rememberLazyListState()
    val context = LocalContext.current
    val dragScope = androidx.compose.runtime.rememberCoroutineScope()
    val dragOffsetYAnim = remember { Animatable(0f) }

    // Prefetching strategy otimizada para reduzir CPU/memória e garantir rolagem ultra fluida
    LaunchedEffect(draftComic.coverUrl) {
        val cleanUrl = draftComic.coverUrl.trim().replace("http://", "https://")
        if (cleanUrl.isNotBlank()) {
            val prefetch = ImageRequest.Builder(context)
                .data(cleanUrl)
                .size(240, 360)
                .precision(coil.size.Precision.INEXACT)
                .memoryCacheKey(cleanUrl)
                .diskCacheKey(cleanUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .allowHardware(true)
                .allowRgb565(true)
                .build()
            coil.Coil.imageLoader(context).enqueue(prefetch)
        }
    }

    val sheetShape = remember { RoundedRectangle(24f.dp) }
    val sheetBackdrop = rememberLayerBackdrop()

    val targetMaxHeight by animateDpAsState(
        targetValue = if (isSheetExpanded) 860.dp else 650.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "sheetMaxHeight"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (s.isLight) 0.14f else 0.24f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = dragOffsetYAnim.value
                }
                .fillMaxWidth(if (isSheetExpanded) 1f else 0.96f)
                .then(
                    if (isSheetExpanded) Modifier.fillMaxHeight(0.96f)
                    else Modifier
                        .padding(bottom = 10.dp)
                        .heightIn(max = targetMaxHeight)
                )
                .clip(sheetShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            // 1. Camada de fundo Liquid Glass na GPU capturada exclusivamente no sheetBackdrop do próprio modal
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(sheetBackdrop)
            ) {
                LiquidGlassSheetRefractiveLayer(
                    coverUrl = draftComic.coverUrl,
                    cornerRadiusDp = 24f,
                    sheetShape = sheetShape
                )
            }

            // 2. Controles que agora refratam a PRÓPRIA superfície do menu de edição (e não a tela atrás do modal)
            CompositionLocalProvider(
                LocalLiquidGlassBackdrop provides sheetBackdrop
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                // 1. Grabber superior arrastável (para cima = tela cheia, para baixo = fechar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(isSheetExpanded) {
                            var cumulativeDragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = {
                                    cumulativeDragY = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulativeDragY += dragAmount
                                    val targetY = if (cumulativeDragY > 0f) {
                                        cumulativeDragY
                                    } else {
                                        (cumulativeDragY * 0.35f).coerceAtLeast(-40f)
                                    }
                                    dragScope.launch {
                                        dragOffsetYAnim.snapTo(targetY)
                                    }
                                },
                                onDragEnd = {
                                    when {
                                        cumulativeDragY < -55f -> {
                                            dragScope.launch {
                                                dragOffsetYAnim.animateTo(
                                                    0f,
                                                    spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium)
                                                )
                                            }
                                            if (!isSheetExpanded) {
                                                onSetExpanded(true)
                                            }
                                        }
                                        cumulativeDragY > 95f -> {
                                            dragScope.launch { dragOffsetYAnim.snapTo(0f) }
                                            onDismiss()
                                        }
                                        else -> {
                                            dragScope.launch {
                                                dragOffsetYAnim.animateTo(
                                                    0f,
                                                    spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium)
                                                )
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    dragScope.launch {
                                        dragOffsetYAnim.animateTo(
                                            0f,
                                            spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium)
                                        )
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(5.5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(s.textSecondary.copy(alpha = 0.55f))
                    )
                }

                // 2. Cabeçalho de Ações ("Cancelar", Título, "Salvar" / "Excluir") sem interceptação de gestos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (isSheetExpanded) 22.dp else 18.dp,
                            end = if (isSheetExpanded) 22.dp else 18.dp,
                            top = 2.dp,
                            bottom = 8.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancelar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textSecondary,
                        modifier = Modifier
                            .testTag("modal_cancel_button")
                            .clip(RoundedCornerShape(99.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )

                    Text(
                        text = if (isExistingInShelf) "Editar Quadrinho" else "Registrar Quadrinho",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isExistingInShelf) {
                            Text(
                                text = "Excluir",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF43F5E),
                                modifier = Modifier
                                    .testTag("modal_delete_button")
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable { onDelete(draftComic.id) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        Text(
                            text = "Salvar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.accentColor,
                            modifier = Modifier
                                .testTag("modal_header_save_button")
                                .clip(RoundedCornerShape(99.dp))
                                .clickable { onSave(buildFinalComicForSave()) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                // Conteúdo rolável do modal otimizado com LazyColumn e trava vertical durante uso do slider
                val isSliderActive = com.kyant.backdrop.catalog.components.SliderTouchLock.isAnySliderActive
                LazyColumn(
                    state = sheetLazyListState,
                    userScrollEnabled = !isSliderActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (isSheetExpanded) 22.dp else 18.dp,
                            end = if (isSheetExpanded) 22.dp else 18.dp,
                            top = 4.dp,
                            bottom = 22.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Item 1: Resumo da Obra (Capa + Título + Autor)
                    item(key = "comic_work_summary", contentType = "work_summary") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookCover(
                                src = draftComic.coverUrl,
                                title = draftComic.title,
                                publisher = draftComic.publisher,
                                year = draftComic.releaseYear,
                                modifier = Modifier
                                    .width(64.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                BasicTextField(
                                    value = draftComic.title,
                                    onValueChange = { v -> draftComic = draftComic.copy(title = v) },
                                    textStyle = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = s.textPrimary
                                    ),
                                    cursorBrush = SolidColor(s.accentColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                BasicTextField(
                                    value = draftComic.authors,
                                    onValueChange = { v -> draftComic = draftComic.copy(authors = v) },
                                    textStyle = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontSize = 12.sp,
                                        color = s.textSecondary
                                    ),
                                    cursorBrush = SolidColor(s.accentColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${draftComic.publisher} • ${draftComic.releaseYear} • ${draftComic.pageCount} págs",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                            }
                        }
                    }

                    // Item 1.5: Sequência da Série (#1, #2, #3...)
                    item(key = "comic_sequence_strip", contentType = "sequence_strip") {
                        ComicSequenceCarouselCard(
                            currentComic = draftComic,
                            allUserComics = allUserComics,
                            onlineSequencePool = onlineSequencePool,
                            spine3d = appSettings.spine3dEffect,
                            onSelectSequenceIssue = { nextComic ->
                                onSelectSequenceComic(nextComic)
                            },
                            onAddNextIssue = { nextTemplate ->
                                onSelectSequenceComic(nextTemplate)
                            }
                        )
                    }

                    // Item 2: Seletor de Status ultra rápido (Lido / Lendo / Na Fila)
                    item(key = "reading_status_selector", contentType = "status_picker") {
                        CatalogStatusSegmentedBar(
                            value = draftComic.status,
                            onChange = { nextStatus ->
                                draftComic = draftComic.copy(
                                    status = nextStatus,
                                    currentPage = when (nextStatus) {
                                        ReadingStatus.READ -> draftComic.pageCount
                                        ReadingStatus.WANT_TO_READ -> 0
                                        ReadingStatus.READING -> draftComic.currentPage
                                    },
                                    rating = if (nextStatus != ReadingStatus.READ) 0f else draftComic.rating,
                                    favorite = if (nextStatus != ReadingStatus.READ) false else draftComic.favorite,
                                    review = if (nextStatus != ReadingStatus.READ) "" else draftComic.review,
                                    rereadCount = if (nextStatus == ReadingStatus.WANT_TO_READ) 0 else draftComic.rereadCount
                                )
                            }
                        )
                    }

                    if (draftComic.status == ReadingStatus.WANT_TO_READ) {
                        item(key = "want_to_read_notice_card", contentType = "notice_card") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .inputWellSurface(RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(s.accentColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = s.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Na sua Fila de Leitura (Quero Ler)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = s.textPrimary
                                    )
                                    Text(
                                        text = "Esta edição ficará salva na aba Fila da sua Biblioteca sem avaliação em estrelas até que você a leia.",
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        color = s.textSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        // Item 3: Data da Leitura (com Mini Calendário) & Releitura
                        item(key = "reading_date_and_rereads", contentType = "date_section") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .inputWellSurface(RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showMiniCalendar = !showMiniCalendar }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = s.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Data da leitura",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = s.textPrimary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (showMiniCalendar) s.accentColor.copy(alpha = 0.18f)
                                                else s.accentColor.copy(alpha = 0.10f)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = draftComic.readDate.ifBlank { "Selecionar data" },
                                            fontFamily = SpaceGroteskTabularFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = s.accentColor
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showMiniCalendar,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    AppleMiniCalendarPicker(
                                        selectedDateStr = draftComic.readDate,
                                        onSelectDate = { pickedDate ->
                                            draftComic = draftComic.copy(readDate = pickedDate)
                                            showMiniCalendar = false
                                        }
                                    )
                                }

                                if (appSettings.allowRereads) {
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Já havia lido esta obra antes",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = s.textPrimary
                                            )
                                            Text(
                                                text = if (draftComic.rereadCount > 0) {
                                                    "Registrado como releitura (${draftComic.rereadCount}x)"
                                                } else {
                                                    "Primeira leitura"
                                                },
                                                fontSize = 11.sp,
                                                color = s.textSecondary
                                            )
                                        }
                                        AppleStepper(
                                            value = draftComic.rereadCount,
                                            min = 0,
                                            max = 50,
                                            step = 1,
                                            onChange = { v -> draftComic = draftComic.copy(rereadCount = v) }
                                        )
                                    }
                                }
                            }
                        }

                        // Item 4: Progresso de Páginas quando está Lendo
                        if (draftComic.status == ReadingStatus.READING) {
                            item(key = "reading_page_progress", contentType = "progress_section") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .inputWellSurface(RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Página atual (${draftComic.currentPage} de ${draftComic.pageCount})",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = s.textPrimary
                                        )
                                        AppleStepper(
                                            value = draftComic.currentPage,
                                            min = 0,
                                            max = draftComic.pageCount.coerceAtLeast(1),
                                            step = 1,
                                            onChange = { v -> draftComic = draftComic.copy(currentPage = v) }
                                        )
                                    }
                                    AppleSlider(
                                        value = draftComic.currentPage.toFloat(),
                                        min = 0f,
                                        max = draftComic.pageCount.coerceAtLeast(1).toFloat(),
                                        step = 1f,
                                        accentColor = s.accentColor,
                                        onChange = { v -> draftComic = draftComic.copy(currentPage = v.roundToInt()) }
                                    )
                                }
                            }
                        }

                        // Item 5 e 6: Avaliação, Gostei e Resenha somente quando já concluído (Lido)
                        if (draftComic.status == ReadingStatus.READ) {
                            // Item 5: Avaliar + Gostei (Coração)
                            item(key = "rating_and_favorite_block", contentType = "rating_section") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .inputWellSurface(RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "AVALIAÇÃO",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                color = s.textPrimary.copy(alpha = 0.85f)
                                            )
                                            if (draftComic.rating > 0f) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.1f", draftComic.rating)} ★ (Limpar)",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = s.accentColor,
                                                    modifier = Modifier.clickable {
                                                        draftComic = draftComic.copy(rating = 0f)
                                                    }
                                                )
                                            } else {
                                                Text(
                                                    text = "Toque ou arraste para avaliar",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = s.textSecondary
                                                )
                                            }
                                        }
                                        StarRating(
                                            rating = draftComic.rating,
                                            onChange = { r -> draftComic = draftComic.copy(rating = r) },
                                            size = "lg",
                                            starColor = s.starColor,
                                            allowHalf = appSettings.halfStarRatings
                                        )
                                    }

                                    // Gostei (Coração)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(42.dp)
                                                .background(mat.dividerColor)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "GOSTEI",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                color = s.textPrimary.copy(alpha = 0.85f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (draftComic.favorite) Color(0xFFF43F5E).copy(alpha = 0.18f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable {
                                                        draftComic = draftComic.copy(favorite = !draftComic.favorite)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (draftComic.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Gostei",
                                                    tint = if (draftComic.favorite) Color(0xFFF43F5E) else s.textSecondary.copy(alpha = 0.65f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Item 6: Resenha / Comentários
                            item(key = "review_comments_block", contentType = "review_section") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "RESENHA OU COMENTÁRIOS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = s.textPrimary.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isSheetExpanded) 110.dp else 84.dp)
                                            .inputWellSurface(RoundedCornerShape(16.dp))
                                            .padding(12.dp)
                                    ) {
                                        if (draftComic.review.isEmpty()) {
                                            Text(
                                                text = "Adicionar resenha...",
                                                fontSize = 12.5.sp,
                                                color = s.textSecondary.copy(alpha = 0.70f)
                                            )
                                        }
                                        BasicTextField(
                                            value = draftComic.review,
                                            onValueChange = { r -> draftComic = draftComic.copy(review = r) },
                                            textStyle = TextStyle(
                                                fontFamily = PlusJakartaSansFamily,
                                                fontSize = 12.5.sp,
                                                lineHeight = 18.sp,
                                                color = s.textPrimary
                                            ),
                                            cursorBrush = SolidColor(s.accentColor),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item 7: Seção Própria e Exclusiva de Tags
                    item(key = "tags_section_block", contentType = "tags_section") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .inputWellSurface(RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = s.accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "TAGS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = s.textPrimary.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${draftComic.tags.size} ${if (draftComic.tags.size == 1) "selecionada" else "selecionadas"}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            if (draftComic.tags.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    draftComic.tags.forEach { tag ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(s.accentBg)
                                                .clickable {
                                                    draftComic = draftComic.copy(tags = draftComic.tags - tag)
                                                }
                                                .padding(horizontal = 11.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = s.accentText
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                tint = s.accentText.copy(alpha = 0.85f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (s.isLight) Color.White.copy(alpha = 0.88f)
                                            else Color.Black.copy(alpha = 0.35f)
                                        )
                                        .border(
                                            width = 0.6.dp,
                                            color = if (s.isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    if (comicTagInput.isEmpty()) {
                                        Text(
                                            text = "Adicionar tag (ex: edição de luxo, favorito)...",
                                            fontSize = 12.sp,
                                            color = s.textSecondary.copy(alpha = 0.70f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = comicTagInput,
                                        onValueChange = { comicTagInput = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontSize = 12.sp,
                                            color = s.textPrimary
                                        ),
                                        cursorBrush = SolidColor(s.accentColor),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (comicTagInput.trim().isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(s.accentBg)
                                            .clickable {
                                                val clean = comicTagInput.trim().replace(",", "")
                                                if (clean.isNotEmpty() && !draftComic.tags.contains(clean)) {
                                                    draftComic = draftComic.copy(tags = draftComic.tags + clean)
                                                }
                                                comicTagInput = ""
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "+ Tag",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = s.accentText
                                        )
                                    }
                                }
                            }

                            if (appSettings.quickReviewTags.isNotEmpty()) {
                                Text(
                                    text = "Sugestões rápidas (toque para marcar/desmarcar):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    appSettings.quickReviewTags.forEach { tag ->
                                        val isSelected = draftComic.tags.contains(tag)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    if (isSelected) s.accentBg
                                                    else if (s.isLight) Color.White.copy(alpha = 0.90f)
                                                    else Color.White.copy(alpha = 0.14f)
                                                )
                                                .border(
                                                    width = 0.5.dp,
                                                    color = if (isSelected) Color.Transparent
                                                            else if (s.isLight) Color.Black.copy(alpha = 0.16f)
                                                            else Color.White.copy(alpha = 0.20f),
                                                    shape = RoundedCornerShape(999.dp)
                                                )
                                                .clickable {
                                                    val next = if (isSelected) draftComic.tags - tag else draftComic.tags + tag
                                                    draftComic = draftComic.copy(tags = next)
                                                }
                                                .padding(horizontal = 11.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isSelected) "✓ $tag" else "+ $tag",
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) s.accentText else s.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Item 7.5: Seção de Momentos Marcantes e Notas Rápidas (Mesmo padrão de adição das Tags, sem tipo de momento)
                    item(key = "moments_section_block", contentType = "moments_section") {
                        val addCurrentMoment = {
                            val cleanNote = momentNoteInput.trim()
                            if (cleanNote.isNotEmpty()) {
                                val pageNum = momentPageInput.toIntOrNull() ?: 0
                                val today = java.time.LocalDate.now()
                                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                val newMoment = ComicMoment(
                                    id = "moment_${System.currentTimeMillis()}_${(100..999).random()}",
                                    page = pageNum,
                                    tag = "",
                                    note = cleanNote,
                                    date = draftComic.readDate.ifBlank { today },
                                    isSpoiler = false
                                )
                                draftComic = draftComic.copy(moments = draftComic.moments + newMoment)
                                momentNoteInput = ""
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .inputWellSurface(RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = s.accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "MOMENTOS MARCANTES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = s.textPrimary.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(s.accentColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${draftComic.moments.size} ${if (draftComic.moments.size == 1) "adicionado" else "adicionados"}",
                                        fontSize = 10.5.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = s.accentColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Momentos já adicionados (exibidos em pílulas/cards acima do campo, assim como nas Tags, com botão X para remover)
                            if (draftComic.moments.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    draftComic.moments.forEach { moment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(s.accentBg.copy(alpha = 0.14f))
                                                .border(
                                                    width = 0.5.dp,
                                                    color = s.accentColor.copy(alpha = 0.30f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    draftComic = draftComic.copy(
                                                        moments = draftComic.moments.filter { it.id != moment.id }
                                                    )
                                                }
                                                .padding(horizontal = 11.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (moment.page > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(s.accentBg)
                                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Pág. ${moment.page}",
                                                            fontSize = 10.sp,
                                                            fontFamily = SpaceGroteskTabularFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            color = s.accentText,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = moment.note,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = s.textPrimary,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover momento",
                                                tint = s.textSecondary.copy(alpha = 0.85f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Linha de adição rápida contínua (idêntica às Tags: digita e toca em + Adicionar quantas vezes quiser)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Campo opcional compacto de Página
                                Box(
                                    modifier = Modifier
                                        .width(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (s.isLight) Color.White.copy(alpha = 0.88f)
                                            else Color.Black.copy(alpha = 0.35f)
                                        )
                                        .border(
                                            width = 0.6.dp,
                                            color = if (s.isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (momentPageInput.isEmpty()) {
                                        Text(
                                            text = "Pág.",
                                            fontSize = 11.sp,
                                            color = s.textSecondary.copy(alpha = 0.70f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    BasicTextField(
                                        value = momentPageInput,
                                        onValueChange = { input ->
                                            if (input.all { it.isDigit() } && input.length <= 5) {
                                                momentPageInput = input
                                            }
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

                                // Campo de texto para digitar a nota ou momento marcante
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (s.isLight) Color.White.copy(alpha = 0.88f)
                                            else Color.Black.copy(alpha = 0.35f)
                                        )
                                        .border(
                                            width = 0.6.dp,
                                            color = if (s.isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 9.dp)
                                ) {
                                    if (momentNoteInput.isEmpty()) {
                                        Text(
                                            text = "Adicionar momento ou citação...",
                                            fontSize = 12.sp,
                                            color = s.textSecondary.copy(alpha = 0.70f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = momentNoteInput,
                                        onValueChange = { momentNoteInput = it },
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                        ),
                                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                            onDone = { addCurrentMoment() }
                                        ),
                                        textStyle = TextStyle(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontSize = 12.sp,
                                            color = s.textPrimary
                                        ),
                                        cursorBrush = SolidColor(s.accentColor),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                val canAddMoment = momentNoteInput.trim().isNotEmpty()
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (canAddMoment) s.accentBg
                                            else s.accentColor.copy(alpha = 0.14f)
                                        )
                                        .clickable(enabled = canAddMoment) {
                                            addCurrentMoment()
                                        }
                                        .padding(horizontal = 11.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ Adicionar",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAddMoment) s.accentText else s.accentColor.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Item 8: Botão Salvar Registro
                    item(key = "save_registration_button", contentType = "action_button") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(999.dp))
                                .clip(RoundedCornerShape(999.dp))
                                .background(s.accentBg)
                                .clickable { onSave(buildFinalComicForSave()) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Salvar Registro",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = s.accentText
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Modal de Informações e Sinopse da HQ (Ao tocar em qualquer HQ na aba de Busca).
 * Inclui refração óptica de Liquid Glass de altura completa, borda suave sem branco forte,
 * barra superior arrastável (para cima = tela cheia, para baixo = fechar), sinopse detalhada
 * e botões rápidos de catalogação (sem expor detalhes técnicos de APIs).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComicInfoSynopsisSheetModal(
    comic: ComicEntry,
    savedEntry: ComicEntry?,
    appSettings: AppSettings,
    allUserComics: List<ComicEntry> = emptyList(),
    onlineSequencePool: List<ComicEntry> = emptyList(),
    onSelectSequenceComic: (ComicEntry) -> Unit = {},
    onOpenCatalogWithStatus: (ComicEntry, ReadingStatus) -> Unit,
    onEditSavedComic: (ComicEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val dragScope = androidx.compose.runtime.rememberCoroutineScope()
    val dragOffsetYAnim = remember { Animatable(0f) }
    val infoLazyListState = rememberLazyListState()

    // Prefetching strategy: decodifica e armazena em cache de memória antecipadamente a capa
    LaunchedEffect(comic.coverUrl) {
        val cleanUrl = comic.coverUrl.trim().replace("http://", "https://")
        if (cleanUrl.isNotBlank()) {
            val prefetch = ImageRequest.Builder(context)
                .data(cleanUrl)
                .size(240, 360)
                .precision(coil.size.Precision.INEXACT)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            coil.Coil.imageLoader(context).enqueue(prefetch)
        }
    }

    val sheetShape = remember { RoundedRectangle(24f.dp) }

    val displayComic = remember(comic) {
        PortugueseComicTranslator.localizeComic(comic)
    }

    val synopsisText = remember(displayComic) {
        displayComic.synopsis.trim().ifBlank {
            PortugueseComicTranslator.translateSynopsisToPtBr(
                rawSynopsis = "",
                translatedTitle = displayComic.title,
                authors = displayComic.authors,
                publisher = displayComic.publisher,
                releaseYear = displayComic.releaseYear,
                pageCount = displayComic.pageCount,
                issueNumber = displayComic.issueNumber
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (s.isLight) 0.14f else 0.24f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = dragOffsetYAnim.value
                }
                .fillMaxWidth(if (isExpanded) 1f else 0.96f)
                .then(
                    if (isExpanded) Modifier.fillMaxHeight(0.95f)
                    else Modifier
                        .padding(bottom = 12.dp)
                        .heightIn(max = 620.dp)
                )
                .clip(sheetShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            val synopsisBackdrop = rememberLayerBackdrop()
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(synopsisBackdrop)
            ) {
                LiquidGlassSheetRefractiveLayer(
                    coverUrl = comic.coverUrl,
                    cornerRadiusDp = 24f,
                    sheetShape = sheetShape
                )
            }

            CompositionLocalProvider(
                LocalLiquidGlassBackdrop provides synopsisBackdrop
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Grabber superior arrastável (para cima = tela cheia, para baixo = fechar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(isExpanded) {
                            var cumulativeDragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = { cumulativeDragY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulativeDragY += dragAmount
                                    val targetY = if (cumulativeDragY > 0f) cumulativeDragY else (cumulativeDragY * 0.35f).coerceAtLeast(-40f)
                                    dragScope.launch { dragOffsetYAnim.snapTo(targetY) }
                                },
                                onDragEnd = {
                                    when {
                                        cumulativeDragY < -50f -> {
                                            dragScope.launch {
                                                dragOffsetYAnim.animateTo(0f, spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium))
                                            }
                                            isExpanded = true
                                        }
                                        cumulativeDragY > 90f -> {
                                            dragScope.launch { dragOffsetYAnim.snapTo(0f) }
                                            onDismiss()
                                        }
                                        else -> {
                                            dragScope.launch {
                                                dragOffsetYAnim.animateTo(0f, spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium))
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    dragScope.launch {
                                        dragOffsetYAnim.animateTo(0f, spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(5.5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(s.textSecondary.copy(alpha = 0.55f))
                    )
                }

                // 2. Cabeçalho com botão Fechar diretamente clicável
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fechar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textSecondary,
                        modifier = Modifier
                            .testTag("synopsis_close_button")
                            .clip(RoundedCornerShape(99.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )

                    Text(
                        text = "Informações da HQ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textPrimary
                    )

                    Spacer(modifier = Modifier.width(48.dp))
                }

                LazyColumn(
                    state = infoLazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: Cover + Primary Info
                    item(key = "info_cover_and_metadata", contentType = "cover_meta") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            BookCover(
                                src = displayComic.coverUrl,
                                title = displayComic.title,
                                authors = displayComic.authors,
                                publisher = displayComic.publisher,
                                year = displayComic.releaseYear,
                                spine3d = appSettings.spine3dEffect,
                                modifier = Modifier
                                    .width(92.dp)
                                    .aspectRatio(2f / 3f)
                                    .shadow(8.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .inputWellSurface(RoundedCornerShape(999.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = displayComic.genre,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = s.accentColor
                                        )
                                    }
                                }

                                Text(
                                    text = displayComic.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary,
                                    lineHeight = 23.sp
                                )

                                Text(
                                    text = displayComic.authors,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )

                                Text(
                                    text = "${displayComic.publisher} • ${displayComic.releaseYear} • ${displayComic.pageCount} páginas",
                                    fontSize = 11.5.sp,
                                    color = s.textSecondary
                                )
                            }
                        }
                    }

                    // Item 1.5: Sequência da Série (#1, #2, #3...)
                    item(key = "info_sequence_carousel", contentType = "sequence_carousel") {
                        ComicSequenceCarouselCard(
                            currentComic = savedEntry ?: displayComic,
                            allUserComics = allUserComics,
                            onlineSequencePool = onlineSequencePool,
                            spine3d = appSettings.spine3dEffect,
                            onSelectSequenceIssue = { nextComic ->
                                onSelectSequenceComic(nextComic)
                            },
                            onAddNextIssue = { nextTemplate ->
                                onOpenCatalogWithStatus(nextTemplate, ReadingStatus.WANT_TO_READ)
                            }
                        )
                    }

                    // Item 2: Sinopse da Obra
                    item(key = "info_synopsis_card", contentType = "synopsis_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .inputWellSurface(RoundedCornerShape(18.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = s.accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "SINOPSE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = s.accentColor
                                )
                            }
                            Text(
                                text = synopsisText,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = s.textPrimary
                            )
                        }
                    }

                    // Item 3: Ficha Técnica da Edição
                    item(key = "info_technical_specs", contentType = "specs_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .inputWellSurface(RoundedCornerShape(18.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "FICHA TÉCNICA DA EDIÇÃO",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = s.textPrimary.copy(alpha = 0.85f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Editora", fontSize = 12.sp, color = s.textSecondary)
                                Text(displayComic.publisher, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                            }
                            HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Formato", fontSize = 12.sp, color = s.textSecondary)
                                Text(
                                    displayComic.format.ifBlank { "Encadernado (TPB)" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textPrimary
                                )
                            }
                            HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ano e Extensão", fontSize = 12.sp, color = s.textSecondary)
                                Text(
                                    "${displayComic.releaseYear} • ${displayComic.pageCount} págs",
                                    fontSize = 12.sp,
                                    fontFamily = SpaceGroteskTabularFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textPrimary
                                )
                            }
                            if (displayComic.isbn.isNotBlank()) {
                                HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ISBN", fontSize = 12.sp, color = s.textSecondary)
                                    Text(
                                        displayComic.isbn,
                                        fontSize = 12.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = s.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Item 3.5: Momentos e Notas Salvos
                    if (savedEntry?.moments?.isNotEmpty() == true) {
                        item(key = "info_saved_moments", contentType = "moments_list") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .inputWellSurface(RoundedCornerShape(18.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = s.accentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "MOMENTOS MARCANTES & NOTAS (${savedEntry.moments.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = s.accentColor
                                    )
                                }
                                savedEntry.moments.forEach { m ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (s.isLight) Color.White.copy(alpha = 0.80f)
                                                else Color.White.copy(alpha = 0.08f)
                                            )
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (m.page > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(s.accentBg)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Pág. ${m.page}",
                                                    fontSize = 9.5.sp,
                                                    fontFamily = SpaceGroteskTabularFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = s.accentText
                                                )
                                            }
                                        }
                                        Text(
                                            text = m.note,
                                            fontSize = 11.5.sp,
                                            color = s.textPrimary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item 4: Ações de Registro na Biblioteca
                    item(key = "info_library_actions", contentType = "actions_card") {
                        if (savedEntry != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(6.dp, RoundedCornerShape(999.dp))
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(s.accentBg)
                                    .clickable { onEditSavedComic(savedEntry) }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Editar Registro na Biblioteca (${savedEntry.status.shortLabel})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.accentText
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(999.dp))
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(s.accentBg)
                                        .clickable { onOpenCatalogWithStatus(displayComic, ReadingStatus.READ) }
                                        .padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ Registrar como Lido",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = s.accentText
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .inputWellSurface(RoundedCornerShape(999.dp))
                                            .clickable { onOpenCatalogWithStatus(displayComic, ReadingStatus.READING) }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Lendo Agora",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = s.textPrimary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .inputWellSurface(RoundedCornerShape(999.dp))
                                            .clickable { onOpenCatalogWithStatus(displayComic, ReadingStatus.WANT_TO_READ) }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Adicionar à Fila",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = s.textPrimary
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
}
}

/**
 * Inset Sheet de Cadastro Manual (`showManualModal`).
 */
@Composable
fun ManualEntrySheetModal(
    form: ManualComicForm,
    onUpdateForm: ((ManualComicForm) -> ManualComicForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val shape = remember { RoundedRectangle(24f.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (s.isLight) 0.14f else 0.24f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(bottom = 12.dp)
                .clip(shape)
                .clickable(enabled = false) {}
        ) {
            Box(
                modifier = Modifier.matchParentSize()
            ) {
                LiquidGlassSheetRefractiveLayer(
                    coverUrl = form.coverUrl,
                    scrollOffsetPx = 0f,
                    dragOffsetY = 0f,
                    cornerRadiusDp = 24f,
                    sheetShape = shape
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancelar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = s.textSecondary,
                    modifier = Modifier.clickable { onDismiss() }
                )
                Text(
                    text = "Nova edição manual",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = s.textPrimary
                )
                Text(
                    text = "Salvar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = s.accentColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable { onSave() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            AppleTextField(
                value = form.title,
                onChange = { v -> onUpdateForm { it.copy(title = v) } },
                placeholder = "Título da HQ (ex: Batman: Ano Um)"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppleTextField(
                    value = form.authors,
                    onChange = { v -> onUpdateForm { it.copy(authors = v) } },
                    placeholder = "Autor / Arte",
                    modifier = Modifier.weight(1f)
                )
                AppleTextField(
                    value = form.publisher,
                    onChange = { v -> onUpdateForm { it.copy(publisher = v) } },
                    placeholder = "Editora",
                    modifier = Modifier.weight(1f)
                )
            }

            AppleTextField(
                value = form.coverUrl,
                onChange = { v -> onUpdateForm { it.copy(coverUrl = v) } },
                placeholder = "Link da capa (opcional)"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(s.accentBg)
                    .clickable { onSave() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Adicionar à Biblioteca",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = s.accentText
                )
            }
        }
        }
    }
}

/**
 * SwiftUI NavigationSplitView / sidebarAdaptable Floating Drawer (`isSidebarOpen`).
 * Sem o botão de ativar/desativar "Lendo agora" (que fica exclusivamente em Biblioteca e Exibição).
 */
@Composable
fun AdaptiveSidebarDrawer(
    profile: ReaderProfile,
    activeTab: String,
    readingNowCount: Int,
    totalComicsCount: Int,
    readComicsCount: Int,
    totalPagesRead: Int,
    shelfGenre: String,
    glassLevel: Int,
    showBottomAccessory: Boolean = true,
    onToggleBottomAccessory: (Boolean) -> Unit = {},
    onSelectTab: (String) -> Unit,
    onSelectShelfGenre: (String) -> Unit,
    onOpenLiquidGlassSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val scaleXAnim = remember { Animatable(0.35f) }
    val scaleYAnim = remember { Animatable(0.65f) }
    LaunchedEffect(Unit) {
        launch {
            scaleXAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.50f, stiffness = 360f)
            )
        }
        launch {
            scaleYAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.56f, stiffness = 290f)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.14f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .width((288f * scaleXAnim.value).coerceAtLeast(96f).dp)
                .fillMaxHeight(scaleYAnim.value.coerceIn(0.35f, 1f))
                .liquidGlassNavSurface(
                    shape = com.kyant.shapes.ContinuousRoundedRectangle(34.dp),
                    elevation = 22.dp
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = false
                ) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserAvatar(profile = profile, sizeDp = 34)
                        Column {
                            Text(
                                text = profile.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = s.textPrimary
                            )
                            Text(
                                text = profile.handle.ifBlank { "@leitor" },
                                fontSize = 10.sp,
                                color = s.textSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .inputWellSurface(CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuOpen,
                            contentDescription = "Fechar menu",
                            tint = s.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Navegação Principal
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Navegação Principal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    val navItems = listOf(
                        Triple("home", "Lendo", LucideIcons.BookOpen to readingNowCount),
                        Triple("shelf", "Biblioteca", LucideIcons.Layers to totalComicsCount),
                        Triple("diary", "Diário", LucideIcons.Calendar to readComicsCount),
                        Triple("search", "Buscar e Catalogar", LucideIcons.Search to 0),
                        Triple("settings", "Ajustes", LucideIcons.SlidersHorizontal to 0)
                    )

                    navItems.forEach { (id, label, iconAndBadge) ->
                        val (icon, badge) = iconAndBadge
                        val active = activeTab == id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) s.pillActiveBg else Color.Transparent)
                                .clickable {
                                    onSelectTab(id)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = s.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textPrimary
                                )
                            }
                            if (badge > 0) {
                                Box(
                                    modifier = Modifier
                                        .inputWellSurface(RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badge.toString(),
                                        fontSize = 11.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        color = s.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Filtros Rápidos da Estante
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Filtros Rápidos da Estante",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    listOf("Todos", "DC / Vertigo", "Marvel", "Mangá", "Graphic Novel").forEach { g ->
                        val active = shelfGenre == g
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    onSelectShelfGenre(g)
                                    onSelectTab("shelf")
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = g,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (active) s.textPrimary else s.textSecondary
                            )
                            if (active) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = s.textPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Well
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .inputWellSurface(RoundedCornerShape(22.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "$totalPagesRead páginas lidas",
                        fontSize = 10.sp,
                        color = s.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(s.accentBg)
                        .clickable {
                            onOpenLiquidGlassSettings()
                            onDismiss()
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$glassLevel% Glass",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.accentText
                    )
                }
            }
        }
    }
}
