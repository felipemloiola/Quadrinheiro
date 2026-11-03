package com.example.ui.screens

import com.example.ui.components.homeSkeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PortugueseComicTranslator
import com.example.data.SeriesSequenceEngine
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import com.example.ui.components.AppleCapsuleButton
import com.example.ui.components.AppleStepper
import com.example.ui.components.BookCover
import com.example.ui.components.LucideIcons
import com.example.ui.components.StarRating
import com.example.ui.components.contentMaterialSurface
import com.example.ui.components.inputWellSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.SpaceGroteskTabularFamily
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Linha de quadrinho na visualização em lista da Biblioteca (ShelfLibraryTab).
 */
@Composable
fun ShelfListComicRow(
    comic: ComicEntry,
    onSelectComic: (ComicEntry) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .contentMaterialSurface(RoundedCornerShape(22.dp))
            .clickable { onSelectComic(comic) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookCover(
            src = comic.coverUrl,
            title = comic.title,
            publisher = comic.publisher,
            year = comic.releaseYear,
            modifier = Modifier
                .width(56.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = comic.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = s.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = comic.authors,
                fontSize = 12.sp,
                color = s.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (comic.status == ReadingStatus.READ) {
                    StarRating(
                        rating = comic.rating,
                        readOnly = true,
                        size = "xs",
                        starColor = s.starColor
                    )
                }
                Text(
                    text = "${comic.status.label} • ${comic.releaseYear}",
                    fontSize = 11.sp,
                    color = s.textTertiary
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = s.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Barra de progresso com Leitura Diferida de Estado (`progressProvider: () -> Float`)
 * desenhada inteiramente na fase de draw (`Modifier.drawWithCache`), evitando recomposições
 * ou relayouts em cascata sobre os shaders `com.kyant.backdrop`.
 */
@Composable
fun DeferredLinearProgressBar(
    progressProvider: () -> Float,
    trackColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    if (com.example.ui.components.LocalHomeSkeleton.current != null) {
        com.example.ui.components.HomeSkeletonBlock(modifier); return
    }

    Box(
        modifier = modifier.drawWithCache {
            val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            onDrawBehind {
                drawRoundRect(
                    color = trackColor,
                    size = size,
                    cornerRadius = cornerRadius
                )
                val fraction = progressProvider().coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = fillColor,
                        size = Size(width = size.width * fraction, height = size.height),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }
    )
}

/**
 * Card de leitura em andamento ("Em leitura") na aba Lendo Agora, isolado com `progressProvider: () -> Float`.
 */
@Composable
fun HomeReadingNowComicCard(
    item: ComicEntry,
    appSettings: AppSettings,
    progressProvider: () -> Float,
    onSelectComic: (ComicEntry) -> Unit,
    onQuickUpdateReadingPage: (ComicEntry, Int) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val pct = remember(item.currentPage, item.pageCount) {
        ((item.currentPage.toFloat() / item.pageCount.coerceAtLeast(1).toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .contentMaterialSurface(RoundedCornerShape(26.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookCover(
            src = item.coverUrl,
            title = item.title,
            authors = item.authors,
            publisher = item.publisher,
            year = item.releaseYear,
            spine3d = appSettings.spine3dEffect,
            modifier = Modifier
                .width(80.dp)
                .aspectRatio(2f / 3f)
                .shadow(6.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectComic(item) }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectComic(item) }
            ) {
                com.example.ui.components.HomeSkeletonText(
                    text = "${item.publisher.uppercase()} • $pct% CONCLUÍDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = s.textSecondary
                )
                com.example.ui.components.HomeSkeletonText(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = s.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                com.example.ui.components.HomeSkeletonText(
                    text = item.authors,
                    fontSize = 13.sp,
                    color = s.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DeferredLinearProgressBar(
                    progressProvider = progressProvider,
                    trackColor = if (s.isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.12f),
                    fillColor = s.accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.HomeSkeletonText(
                        text = "Pág. ${item.currentPage} de ${item.pageCount}",
                        fontSize = 12.sp,
                        fontFamily = SpaceGroteskTabularFamily,
                        fontWeight = FontWeight.Medium,
                        color = s.textSecondary
                    )

                    AppleStepper(
                        modifier = Modifier.homeSkeleton(0.16f),
                        value = item.currentPage,
                        min = 0,
                        max = item.pageCount.coerceAtLeast(1),
                        step = 5,
                        onChange = { nextPage ->
                            onQuickUpdateReadingPage(item, nextPage)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Card de resumo estatístico e histograma de notas do Diário Cronológico.
 */
@Composable
fun DiaryAnalyticsSummaryCard(comics: List<ComicEntry>) {
    val s = LocalAppleMaterials.current.scheme
    val summary = remember(comics) {
        val read = comics.filter { it.status == ReadingStatus.READ }
        val rated = read.filter { it.rating > 0f }
        val pages = comics.sumOf {
            when (it.status) {
                ReadingStatus.READ -> it.pageCount.coerceAtLeast(0)
                ReadingStatus.READING -> it.currentPage.coerceIn(0, it.pageCount.coerceAtLeast(0))
                else -> 0
            }
        }
        listOf(
            read.size.toString() to "Concluídas",
            pages.toString() to "Páginas lidas",
            (if (rated.isEmpty()) "—" else String.format(Locale.getDefault(), "%.1f", rated.map { it.rating }.average())) to "Nota média"
        )
    }
    Row(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(24.dp)).padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        summary.forEach { (value, label) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFamily = SpaceGroteskTabularFamily, color = s.textPrimary)
                Text(label, fontSize = 12.sp, color = s.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/**
 * Grupo mensal de entradas do Diário Cronológico.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryMonthGroupCard(
    monthLabel: String,
    entries: List<Pair<ComicEntry, String>>,
    appSettings: AppSettings,
    revealedSpoilers: MutableMap<String, Boolean>,
    onSelectComic: (ComicEntry) -> Unit,
    onShareComicReview: (ComicEntry) -> Unit = {}
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = monthLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = s.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .contentMaterialSurface(RoundedCornerShape(24.dp))
        ) {
            entries.forEachIndexed { idx, (entry, displayDay) ->
                if (idx > 0) {
                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                }
                val isSpoilerHidden = appSettings.spoilerProtection &&
                    entry.review.isNotBlank() &&
                    !(revealedSpoilers[entry.id] ?: false)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectComic(entry) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.width(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayDay,
                            fontSize = 18.sp,
                            fontFamily = SpaceGroteskTabularFamily,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary
                        )
                        Text(
                            text = entry.status.shortLabel.uppercase(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = s.textTertiary
                        )
                    }

                    BookCover(
                        src = entry.coverUrl,
                        title = entry.title,
                        modifier = Modifier
                            .width(48.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(6.dp))
                    )

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
                                text = entry.title,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = s.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (appSettings.allowRereads && entry.rereadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .inputWellSurface(RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "↻ ${entry.rereadCount}x",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = s.textPrimary
                                    )
                                }
                            }
                        }

                        Text(
                            text = buildString {
                                append(entry.authors)
                                if (appSettings.showPagesInDiary) {
                                    append(" • ${entry.publisher} • ${entry.pageCount} págs")
                                }
                            },
                            fontSize = 12.sp,
                            color = s.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (entry.status == ReadingStatus.READ && (entry.rating > 0f || entry.favorite)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (entry.rating > 0f) {
                                    StarRating(
                                        rating = entry.rating,
                                        readOnly = true,
                                        size = "xs",
                                        starColor = s.starColor
                                    )
                                }
                                if (entry.favorite) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        if (entry.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                entry.tags.forEach { t ->
                                    Box(
                                        modifier = Modifier
                                            .inputWellSurface(RoundedCornerShape(999.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = t,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = s.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        if (entry.status == ReadingStatus.READ && entry.review.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable(enabled = isSpoilerHidden) {
                                        revealedSpoilers[entry.id] = true
                                    }
                            ) {
                                Text(
                                    text = "“${entry.review}”",
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = s.textSecondary,
                                    modifier = if (isSpoilerHidden) Modifier.blur(6.dp) else Modifier
                                )
                                if (isSpoilerHidden) {
                                    Text(
                                        text = "Toque para revelar resenha",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = s.accentColor,
                                        modifier = Modifier.padding(top = 2.dp)
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
 * Card de resultado de busca online/curada na aba Buscar (SearchMultiApiTab).
 * Implementa Tradução Progressiva Baseada em Visibilidade: renderiza imediatamente o resultado do
 * `MultiApiSearchEngine` e dispara a tradução local/em cache da sinopse e título em background
 * (`LaunchedEffect` na `Dispatchers.IO`) apenas para os cards visíveis na tela.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchResultComicCard(
    item: ComicEntry,
    savedEntry: ComicEntry?,
    appSettings: AppSettings,
    onOpenComicInfoModal: (ComicEntry) -> Unit,
    onSelectSavedComic: (ComicEntry) -> Unit,
    onOpenCatalogSheetForComic: (ComicEntry, ReadingStatus) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

    var progressiveComic by remember(item.id, item.externalId, item.synopsisPtBr, item.titlePtBr) {
        val cached = PortugueseComicTranslator.getCachedTranslationForComicId(item.id)
            ?: PortugueseComicTranslator.getCachedTranslationForComicId(item.externalId)
        mutableStateOf(
            if (cached != null) {
                item.copy(
                    title = cached.titlePtBr.ifBlank { item.title },
                    titlePtBr = cached.titlePtBr.ifBlank { item.titlePtBr },
                    synopsis = cached.synopsisPtBr.ifBlank { item.synopsis },
                    synopsisPtBr = cached.synopsisPtBr.ifBlank { item.synopsisPtBr }
                )
            } else {
                item
            }
        )
    }

    // Tradução Progressiva Baseada em Visibilidade: traduz apenas os itens visíveis no viewport da LazyColumn
    LaunchedEffect(item.id, item.externalId, item.synopsis) {
        if (progressiveComic.synopsisPtBr.isBlank() && item.synopsis.isNotBlank()) {
            val localized = withContext(Dispatchers.IO) {
                PortugueseComicTranslator.localizeComic(item, allowNetworkTranslation = false)
            }
            if (localized.synopsisPtBr.isNotBlank() || localized.titlePtBr.isNotBlank()) {
                progressiveComic = localized
            }
        }
    }

    val displayItem = progressiveComic

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .contentMaterialSurface(RoundedCornerShape(22.dp))
            .clickable { onOpenComicInfoModal(displayItem) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        BookCover(
            src = displayItem.coverUrl,
            title = displayItem.title,
            authors = displayItem.authors,
            publisher = displayItem.publisher,
            year = displayItem.releaseYear,
            spine3d = appSettings.spine3dEffect,
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(2f / 3f)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenComicInfoModal(displayItem) }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val issueNum = remember(displayItem.id, displayItem.title, displayItem.issueNumber) {
                SeriesSequenceEngine.inferIssueNumber(displayItem)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .inputWellSurface(RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = displayItem.format.ifBlank { displayItem.publisher },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (issueNum > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(s.accentColor.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Sequência • Ed. #$issueNum",
                            fontSize = 11.sp,
                            fontFamily = SpaceGroteskTabularFamily,
                            fontWeight = FontWeight.Bold,
                            color = s.accentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Box(
                    modifier = Modifier.padding(vertical = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = displayItem.genre,
                        fontSize = 11.sp,
                        color = s.textTertiary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = displayItem.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = s.textPrimary,
                lineHeight = 22.sp
            )

            Text(
                text = displayItem.authors,
                fontSize = 13.sp,
                color = s.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${displayItem.publisher} • ${displayItem.releaseYear} • ${displayItem.pageCount} págs",
                fontSize = 11.sp,
                color = s.textTertiary
            )

            val previewSynopsis = displayItem.synopsisPtBr.ifBlank {
                if (PortugueseComicTranslator.isPortugueseText(displayItem.synopsis)) displayItem.synopsis else ""
            }
            if (previewSynopsis.isNotBlank()) {
                Text(
                    text = previewSynopsis,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = s.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Toque para ver sinopse e informações ›",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = s.accentColor
            )

            if (savedEntry != null) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .inputWellSurface(RoundedCornerShape(999.dp))
                        .clickable { onSelectSavedComic(savedEntry) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Editar na Biblioteca (${savedEntry.status.shortLabel})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textPrimary
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.ui.components.CompactReadingAction(
                        label = "+ Lido",
                        prominent = true,
                        onClick = {
                            onOpenCatalogSheetForComic(item, ReadingStatus.READ)
                        }
                    )

                    com.example.ui.components.CompactReadingAction(
                        label = "Lendo",
                        prominent = false,
                        onClick = {
                            onOpenCatalogSheetForComic(item, ReadingStatus.READING)
                        }
                    )

                    com.example.ui.components.CompactReadingAction(
                        label = "Quero Ler",
                        prominent = false,
                        onClick = {
                            onOpenCatalogSheetForComic(item, ReadingStatus.WANT_TO_READ)
                        }
                    )
                }
            }
        }
    }
}
