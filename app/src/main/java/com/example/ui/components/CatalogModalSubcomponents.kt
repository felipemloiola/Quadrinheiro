package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.CuratedCatalog
import com.example.data.SeriesSequenceEngine
import com.example.data.SeriesSequenceGroup
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ComicMoment
import com.example.model.ReadingStatus
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.kyant.shapes.RoundedRectangle
import java.util.Calendar
import java.util.Locale

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
 * Camada de fundo Liquid Glass na GPU para o Menu de Registro da HQ.
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassRegistrationSheet(
                    backdrop = rootBackdrop,
                    cornerRadiusDp = cornerRadiusDp
                )
        )

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

/**
 * Seletor de Status de Leitura (Lido / Lendo / Quero Ler).
 */
@Composable
internal fun CatalogStatusSegmentedBar(
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
 * Card horizontal interativo para visualizar e navegar pelas edições da mesma sequência.
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

    val cacheKey = remember(
        currentComic.id,
        currentComic.externalId,
        currentComic.title,
        currentComic.seriesName,
        currentComic.issueNumber,
        allUserComics.size,
        onlineSequencePool.size
    ) {
        SeriesSequenceEngine.buildCacheKey(currentComic, allUserComics.size, onlineSequencePool.size)
    }

    var sequenceGroupState by remember(currentComic.id, currentComic.externalId) {
        mutableStateOf(
            SeriesSequenceEngine.getCachedSequenceForComic(
                target = currentComic,
                userComics = allUserComics,
                extraOnlineComics = onlineSequencePool
            )
        )
    }

    LaunchedEffect(cacheKey) {
        sequenceGroupState = SeriesSequenceEngine.findSequenceForComicAsync(
            target = currentComic,
            userComics = allUserComics,
            catalogComics = CuratedCatalog.curatedComicsCatalog,
            extraOnlineComics = onlineSequencePool
        )
    }

    val sequenceGroup = sequenceGroupState ?: return

    val sequenceItems = sequenceGroup.issues
    val baseSeriesTitle = sequenceGroup.baseSeriesTitle
    val primaryAuthor = sequenceGroup.primaryAuthor
    val currentIssueNum = remember(currentComic) {
        SeriesSequenceEngine.inferIssueNumber(currentComic)
    }
    val inShelfCount = sequenceGroup.savedCount
    val nextIssueNumber = sequenceGroup.nextIssueNumber

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
 * Cabeçalho da Capa, Título Editável, Autor e Metadados no Modal de Catalogação.
 */
@Composable
internal fun CatalogWorkSummaryHeader(
    draftComic: ComicEntry,
    onUpdateDraftComic: ((ComicEntry) -> ComicEntry) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

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
                onValueChange = { v -> onUpdateDraftComic { it.copy(title = v) } },
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
                onValueChange = { v -> onUpdateDraftComic { it.copy(authors = v) } },
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

/**
 * Seção de Data da Leitura (com Mini Calendário) e Releituras.
 */
@Composable
internal fun CatalogReadingDateAndRereadsSection(
    draftComic: ComicEntry,
    allowRereads: Boolean,
    showMiniCalendar: Boolean,
    onToggleMiniCalendar: () -> Unit,
    onUpdateDraftComic: ((ComicEntry) -> ComicEntry) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .inputWellSurface(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleMiniCalendar() }
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
                    onUpdateDraftComic { it.copy(readDate = pickedDate) }
                    onToggleMiniCalendar()
                }
            )
        }

        if (allowRereads) {
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
                    onChange = { v -> onUpdateDraftComic { it.copy(rereadCount = v) } }
                )
            }
        }
    }
}

/**
 * Seção de Avaliação com Estrelas e Favorito (Gostei).
 */
@Composable
internal fun CatalogRatingAndFavoriteSection(
    draftComic: ComicEntry,
    halfStarRatings: Boolean,
    onUpdateDraftComic: ((ComicEntry) -> ComicEntry) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

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
                            onUpdateDraftComic { it.copy(rating = 0f) }
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
                onChange = { r -> onUpdateDraftComic { it.copy(rating = r) } },
                size = "lg",
                starColor = s.starColor,
                allowHalf = halfStarRatings
            )
        }

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
                            onUpdateDraftComic { it.copy(favorite = !it.favorite) }
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

/**
 * Seção de Resenha ou Comentários.
 */
@Composable
internal fun CatalogReviewCommentsSection(
    reviewText: String,
    isExpanded: Boolean,
    onChangeReview: (String) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

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
                .height(if (isExpanded) 110.dp else 84.dp)
                .inputWellSurface(RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (reviewText.isEmpty()) {
                Text(
                    text = "Adicionar resenha...",
                    fontSize = 12.5.sp,
                    color = s.textSecondary.copy(alpha = 0.70f)
                )
            }
            BasicTextField(
                value = reviewText,
                onValueChange = onChangeReview,
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

/**
 * Seção de Tags Personalizadas e Sugestões Rápidas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CatalogTagsSection(
    tags: List<String>,
    quickReviewTags: List<String>,
    comicTagInput: String,
    onChangeComicTagInput: (String) -> Unit,
    onUpdateTags: (List<String>) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

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
                text = "${tags.size} ${if (tags.size == 1) "selecionada" else "selecionadas"}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = s.textSecondary,
                maxLines = 1,
                softWrap = false
            )
        }

        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(s.accentBg)
                            .clickable {
                                onUpdateTags(tags - tag)
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
                    onValueChange = onChangeComicTagInput,
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
                            if (clean.isNotEmpty() && !tags.contains(clean)) {
                                onUpdateTags(tags + clean)
                            }
                            onChangeComicTagInput("")
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

        if (quickReviewTags.isNotEmpty()) {
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
                quickReviewTags.forEach { tag ->
                    val isSelected = tags.contains(tag)
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
                                val next = if (isSelected) tags - tag else tags + tag
                                onUpdateTags(next)
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

/**
 * Seção de Momentos Marcantes e Notas Rápidas do Diário.
 */
@Composable
internal fun CatalogMomentsSection(
    moments: List<ComicMoment>,
    momentPageInput: String,
    momentNoteInput: String,
    onChangeMomentPage: (String) -> Unit,
    onChangeMomentNote: (String) -> Unit,
    onAddMoment: () -> Unit,
    onRemoveMoment: (String) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

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
                    text = "${moments.size} ${if (moments.size == 1) "adicionado" else "adicionados"}",
                    fontSize = 10.5.sp,
                    fontFamily = SpaceGroteskTabularFamily,
                    fontWeight = FontWeight.Bold,
                    color = s.accentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (moments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                moments.forEach { moment ->
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
                            .clickable { onRemoveMoment(moment.id) }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                            onChangeMomentPage(input)
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
                    onValueChange = onChangeMomentNote,
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { onAddMoment() }
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
                        onAddMoment()
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
