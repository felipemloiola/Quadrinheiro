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
        com.example.data.PortugueseComicTranslator.localizeComic(selectedComic, allowNetworkTranslation = false)
    }
    var draftComic by remember(selectedComic.id) { mutableStateOf(localizedInitial) }
    LaunchedEffect(selectedComic.id, selectedComic.synopsisPtBr) {
        if (selectedComic.synopsisPtBr.isNotBlank() && draftComic.synopsisPtBr != selectedComic.synopsisPtBr) {
            draftComic = draftComic.copy(
                synopsis = selectedComic.synopsisPtBr,
                synopsisPtBr = selectedComic.synopsisPtBr
            )
        } else if (draftComic.synopsisPtBr.isBlank()) {
            val enriched = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.PortugueseComicTranslator.localizeComic(selectedComic, allowNetworkTranslation = true)
            }
            if (enriched.synopsisPtBr.isNotBlank()) {
                draftComic = draftComic.copy(
                    synopsis = enriched.synopsisPtBr,
                    synopsisPtBr = enriched.synopsisPtBr
                )
            }
        }
    }
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
    val sheetScrollState = rememberScrollState(0)
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
                    scrollOffsetProvider = { sheetScrollState.value.toFloat() },
                    dragOffsetYProvider = { dragOffsetYAnim.value },
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

                // Conteúdo rolável do modal com Column + verticalScroll contínuo (mantém todas as seções medidas sem saltos/teleporte ao rolar para cima)
                val isSliderActive = com.kyant.backdrop.catalog.components.SliderTouchLock.isAnySliderActive
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            state = sheetScrollState,
                            enabled = !isSliderActive
                        )
                        .padding(
                            start = if (isSheetExpanded) 22.dp else 18.dp,
                            end = if (isSheetExpanded) 22.dp else 18.dp,
                            top = 4.dp,
                            bottom = 22.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Item 1: Resumo da Obra (Capa + Título + Autor)
                    CatalogWorkSummaryHeader(
                        draftComic = draftComic,
                        onUpdateDraftComic = { transform -> draftComic = transform(draftComic) }
                    )

                    // Item 1.5: Sequência da Série (#1, #2, #3...)
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

                    // Item 2: Seletor de Status ultra rápido (Lido / Lendo / Na Fila)
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

                    if (draftComic.status == ReadingStatus.WANT_TO_READ) {
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
                    } else {
                        // Item 3: Data da Leitura (com Mini Calendário) & Releitura
                        CatalogReadingDateAndRereadsSection(
                            draftComic = draftComic,
                            allowRereads = appSettings.allowRereads,
                            showMiniCalendar = showMiniCalendar,
                            onToggleMiniCalendar = { showMiniCalendar = !showMiniCalendar },
                            onUpdateDraftComic = { transform -> draftComic = transform(draftComic) }
                        )

                        // Item 4: Progresso de Páginas quando está Lendo (com leitura diferida de estado)
                        if (draftComic.status == ReadingStatus.READING) {
                            CatalogReadingProgressSection(
                                currentPageProvider = { draftComic.currentPage },
                                pageCount = draftComic.pageCount,
                                accentColor = s.accentColor,
                                onChangePage = { v -> draftComic = draftComic.copy(currentPage = v) }
                            )
                        }

                        // Item 5 e 6: Avaliação, Gostei e Resenha somente quando já concluído (Lido)
                        if (draftComic.status == ReadingStatus.READ) {
                            // Item 5: Avaliar + Gostei (Coração)
                            CatalogRatingAndFavoriteSection(
                                draftComic = draftComic,
                                halfStarRatings = appSettings.halfStarRatings,
                                onUpdateDraftComic = { transform -> draftComic = transform(draftComic) }
                            )

                            // Item 6: Resenha / Comentários
                            CatalogReviewCommentsSection(
                                reviewText = draftComic.review,
                                isExpanded = isSheetExpanded,
                                onChangeReview = { r -> draftComic = draftComic.copy(review = r) }
                            )
                        }
                    }

                    // Item 7: Seção Própria e Exclusiva de Tags
                    CatalogTagsSection(
                        tags = draftComic.tags,
                        quickReviewTags = appSettings.quickReviewTags,
                        comicTagInput = comicTagInput,
                        onChangeComicTagInput = { comicTagInput = it },
                        onUpdateTags = { nextTags -> draftComic = draftComic.copy(tags = nextTags) }
                    )

                    // Item 7.5: Seção de Momentos Marcantes e Notas Rápidas
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
                    CatalogMomentsSection(
                        moments = draftComic.moments,
                        momentPageInput = momentPageInput,
                        momentNoteInput = momentNoteInput,
                        onChangeMomentPage = { momentPageInput = it },
                        onChangeMomentNote = { momentNoteInput = it },
                        onAddMoment = { addCurrentMoment() },
                        onRemoveMoment = { momentId ->
                            draftComic = draftComic.copy(
                                moments = draftComic.moments.filter { it.id != momentId }
                            )
                        }
                    )

                    // Item 8: Botão Salvar Registro
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
    val infoScrollState = rememberScrollState(0)

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

    var displayComic by remember(comic) {
        mutableStateOf(PortugueseComicTranslator.localizeComic(comic, allowNetworkTranslation = false))
    }

    LaunchedEffect(comic.id, comic.synopsisPtBr) {
        if (comic.synopsisPtBr.isNotBlank() && displayComic.synopsisPtBr != comic.synopsisPtBr) {
            displayComic = displayComic.copy(
                synopsis = comic.synopsisPtBr,
                synopsisPtBr = comic.synopsisPtBr
            )
        } else if (displayComic.synopsisPtBr.isBlank()) {
            val enriched = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                PortugueseComicTranslator.localizeComic(comic, allowNetworkTranslation = true)
            }
            displayComic = enriched
        }
    }

    val synopsisText = remember(displayComic) {
        val candidate = displayComic.synopsisPtBr.trim().ifBlank { displayComic.synopsis.trim() }
        if (candidate.isNotBlank() && PortugueseComicTranslator.isPortugueseText(candidate)) {
            candidate
        } else {
            PortugueseComicTranslator.translateSynopsisToPtBr(
                rawSynopsis = candidate,
                translatedTitle = displayComic.title,
                authors = displayComic.authors,
                publisher = displayComic.publisher,
                releaseYear = displayComic.releaseYear,
                pageCount = displayComic.pageCount,
                issueNumber = displayComic.issueNumber,
                allowNetworkTranslation = false
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
                    scrollOffsetProvider = { infoScrollState.value.toFloat() },
                    dragOffsetYProvider = { dragOffsetYAnim.value },
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(infoScrollState)
                        .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: Cover + Primary Info
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

                    // Item 1.5: Sequência da Série (#1, #2, #3...)
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

                    // Item 2: Sinopse da Obra
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

                    // Item 3: Ficha Técnica da Edição
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

                    // Item 3.5: Momentos e Notas Salvos
                    if (savedEntry?.moments?.isNotEmpty() == true) {
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

                    // Item 4: Ações de Registro na Biblioteca
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
                    scrollOffsetProvider = { 0f },
                    dragOffsetYProvider = { 0f },
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
                .width(288.dp)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = scaleXAnim.value.coerceIn(0.35f, 1f)
                    scaleY = scaleYAnim.value.coerceIn(0.35f, 1f)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
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
