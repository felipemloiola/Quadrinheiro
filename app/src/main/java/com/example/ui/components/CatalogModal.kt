package com.example.ui.components

import androidx.compose.foundation.layout.PaddingValues

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
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import com.example.model.CatalogPagePolicy
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
    onDismiss: () -> Unit,
    onShareComic: (ComicEntry, SocialShareCardType, String?) -> Unit = { _, _, _ -> },
    readerProfile: com.example.model.ReaderProfile = com.example.model.ReaderProfile(),
    onLogin: () -> Unit = {}, closeTag:String="modal_cancel_button",onRegisterFromPage:((ComicEntry)->Unit)?=null
) {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    var draftComic by remember(selectedComic.id) { mutableStateOf(selectedComic) }
    // Estados primitivos isolados para campos de alta frequência (zero recomposição cruzada entre itens da LazyColumn)
    var draftTitle by remember(selectedComic.id) { mutableStateOf(selectedComic.title) }
    var draftAuthors by remember(selectedComic.id) { mutableStateOf(selectedComic.authors) }
    var draftStatus by remember(selectedComic.id) { mutableStateOf(selectedComic.status) }
    var draftCurrentPage by remember(selectedComic.id) { mutableIntStateOf(selectedComic.currentPage) }
    var draftRating by remember(selectedComic.id) { mutableFloatStateOf(selectedComic.rating) }
    var draftFavorite by remember(selectedComic.id) { mutableStateOf(selectedComic.favorite) }
    var draftReview by remember(selectedComic.id) { mutableStateOf(selectedComic.review) }
    var draftReadDate by remember(selectedComic.id) { mutableStateOf(selectedComic.readDate) }
    var draftRereadCount by remember(selectedComic.id) { mutableIntStateOf(selectedComic.rereadCount) }
    var draftTags by remember(selectedComic.id) { mutableStateOf(selectedComic.tags) }
    var draftMoments by remember(selectedComic.id) { mutableStateOf(selectedComic.moments) }

    var fullPage by remember(selectedComic.id) { mutableStateOf(false) }
    var totalPagesInput by remember(selectedComic.id) { mutableStateOf(if (selectedComic.pageCount <= 1) "" else selectedComic.pageCount.toString()) }
    val editorScope=androidx.compose.runtime.rememberCoroutineScope()
    var pagesEdited by remember(selectedComic.id){mutableStateOf(false)}
    var remoteInfo by remember(selectedComic.id){mutableStateOf<com.example.data.CatalogDetails?>(null)}
    var pageEditorOpen by remember(selectedComic.id) { mutableStateOf(false) }
    var metadataLoading by remember(selectedComic.id){mutableStateOf(false)}
    var pagesError by remember(selectedComic.id) { mutableStateOf(false) }
    var translationLoading by remember(selectedComic.id) { mutableStateOf(false) }
    var translationFailed by remember(selectedComic.id) { mutableStateOf(false) }
    var translationRetryToken by remember(selectedComic.id) { mutableIntStateOf(0) }

    var comicTagInput by remember(selectedComic.id) { mutableStateOf("") }
    var showMiniCalendar by remember(selectedComic.id) { mutableStateOf(false) }
    var momentNoteInput by remember(selectedComic.id) { mutableStateOf("") }
    var momentPageInput by remember(selectedComic.id) {
        mutableStateOf(if (selectedComic.currentPage > 0) selectedComic.currentPage.toString() else "")
    }
    var selectedMomentTag by remember(selectedComic.id) { mutableStateOf("⚡ Momento Épico") }

    fun snapshotDraftComic(): ComicEntry = draftComic.copy(
        title = draftTitle,
        authors = draftAuthors,
        status = draftStatus,
        currentPage = draftCurrentPage,
        rating = draftRating,
        favorite = draftFavorite,
        review = draftReview,
        readDate = draftReadDate,
        rereadCount = draftRereadCount,
        tags = draftTags,
        moments = draftMoments
    )

    if (fullPage) {
        val currentSnapshot = snapshotDraftComic()
        com.example.ui.screens.ComicDetailDialog(currentSnapshot, readerProfile, allUserComics,
            onClose = { fullPage = false },
            onRegister = { next ->
                fullPage = false
                if (onRegisterFromPage != null) {
                    onRegisterFromPage(next)
                } else {
                    draftComic = next
                    draftTitle = next.title
                    draftAuthors = next.authors
                    draftStatus = next.status
                    draftCurrentPage = next.currentPage
                    draftRating = next.rating
                    draftFavorite = next.favorite
                    draftReview = next.review
                    draftReadDate = next.readDate
                    draftRereadCount = next.rereadCount
                    draftTags = next.tags
                    draftMoments = next.moments
                }
            },
            onCover = { draftComic = draftComic.copy(coverUrl = it) }, onLogin = onLogin,
            onShare = { onShareComic(snapshotDraftComic(), SocialShareCardType.COMPLETED_READING, null) })
    }

    fun buildFinalComicForSave(): ComicEntry {
        val count = CatalogPagePolicy.count(totalPagesInput) ?: 0
        var finalComic = CatalogPagePolicy.withCount(snapshotDraftComic(), count)
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
                tag = selectedMomentTag,
                note = pendingNote,
                date = finalComic.readDate.ifBlank { today },
                isSpoiler = false
            )
            finalComic = finalComic.copy(moments = finalComic.moments + newMoment)
        }
        return finalComic
    }
    val sheetScrollState = rememberLazyListState()
    val context = LocalContext.current
    val sheet = rememberFluidSheet(
        initialHeightFraction = 0.80f,
        onDismiss = onDismiss,
        onExpand = { onSetExpanded(true) }
    )
    LaunchedEffect(isSheetExpanded) {
        if (isSheetExpanded) sheet.expandToTop()
        else sheet.snapToRest()
    }
    fun saveDraft() {
        if (totalPagesInput.isNotBlank() && CatalogPagePolicy.count(totalPagesInput) == null) {
            pagesError = true
            pageEditorOpen = true
            editorScope.launch { sheetScrollState.animateScrollToItem(0) }
        } else onSave(buildFinalComicForSave())
    }
    LaunchedEffect(draftComic.id, draftComic.externalId) {
        val requested = draftComic
        remoteInfo = null
        val hasEssentialData = requested.synopsisPtBr.isNotBlank() &&
            requested.authors.isNotBlank() &&
            requested.publisher.isNotBlank() &&
            requested.pageCount > 0 &&
            requested.coverUrl.isNotBlank()

        if (hasEssentialData) {
            metadataLoading = false
            return@LaunchedEffect
        }

        metadataLoading = true
        try {
            val resolvedPair = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val details = com.example.data.ComicVineCatalog.resolve(requested)
                val localPtSynopsis = if (details != null && details.description.isNotBlank() && !PortugueseComicTranslator.isPortugueseText(details.description)) {
                    PortugueseComicTranslator.translateSynopsisToPtBr(
                        rawSynopsis = details.description,
                        translatedTitle = requested.displayTitle,
                        authors = requested.authors,
                        publisher = requested.publisher.ifBlank { details.publisher },
                        releaseYear = requested.releaseYear,
                        pageCount = requested.pageCount
                    )
                } else ""
                val resolvedAuthors = if (details != null && requested.authors.isBlank()) {
                    details.creators
                        .filter { it.role.contains("writer", true) || it.role.contains("artist", true) }
                        .take(4)
                        .joinToString(", ") { it.name }
                } else ""
                Triple(details, localPtSynopsis, resolvedAuthors)
            }
            val details = resolvedPair.first
            val localPtSynopsis = resolvedPair.second
            val resolvedAuthors = resolvedPair.third
            if (details != null && draftComic.id == requested.id && draftComic.externalId == requested.externalId) {
                remoteInfo = details
                CatalogPagePolicy.remoteCount(draftComic.pageCount, isExistingInShelf, pagesEdited, details.pageCount)?.let { count ->
                    totalPagesInput = count.toString()
                    val updatedWithCount = CatalogPagePolicy.withCount(
                        draftComic.copy(status = draftStatus, currentPage = draftCurrentPage),
                        count
                    )
                    draftComic = updatedWithCount
                    draftCurrentPage = updatedWithCount.currentPage
                    pagesError = false
                }
                if (draftAuthors.isBlank() && resolvedAuthors.isNotBlank()) {
                    draftAuthors = resolvedAuthors
                }
                draftComic = draftComic.copy(
                    publisher = draftComic.publisher.ifBlank { details.publisher },
                    authors = draftAuthors.ifBlank { resolvedAuthors },
                    isbn = draftComic.isbn.ifBlank { details.isbn },
                    synopsis = draftComic.synopsis.ifBlank { details.description },
                    synopsisPtBr = draftComic.synopsisPtBr.ifBlank { localPtSynopsis },
                    coverUrl = draftComic.coverUrl.ifBlank { details.image },
                    releaseYear = if (!isExistingInShelf) details.date.take(4).toIntOrNull() ?: draftComic.releaseYear else draftComic.releaseYear
                )
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { }
        finally { metadataLoading = false }
    }

    val sheetShape = remember { RoundedRectangle(24f.dp) }
    val sheetBackdrop = rememberLayerBackdrop()
    val rawSynopsis = draftComic.synopsisPtBr.ifBlank { draftComic.synopsis }
    val resolvedSynopsis = remember(rawSynopsis) {
        PortugueseComicTranslator.formatCleanSynopsis(rawSynopsis)
    }
    val sheetWidthFraction by animateFloatAsState(
        targetValue = if (isSheetExpanded) 1f else 0.96f,
        animationSpec = motion.spring(dampingRatio = 0.85f, stiffness = 420f),
        label = "sheetWidthFraction"
    )
    val isSheetScrollingOrMoving = sheetScrollState.isScrollInProgress || sheet.isMoving

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color.Black.copy(alpha = (if (s.isLight) .14f else .24f) * sheet.scrimFraction)) }
            .clickable { sheet.dismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = sheet.offset.coerceAtLeast(0f)
                }
                .fillMaxWidth(sheetWidthFraction)
                .fillMaxHeight(0.96f)
                .clip(sheetShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            // 1. Camada de fundo Liquid Glass fluida e otimizada (congelada durante rolagem/animação da folha)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(
                        backdrop = sheetBackdrop,
                        reuseRecordedContent = true,
                        freezeSnapshot = isSheetScrollingOrMoving
                    )
            ) {
                LiquidGlassSheetRefractiveLayer(
                    coverUrl = draftComic.coverUrl,
                    scrollOffsetProvider = { 0f },
                    dragOffsetYProvider = { 0f },
                    cornerRadiusDp = 24f,
                    sheetShape = sheetShape
                )
            }

            CompositionLocalProvider(
                LocalLiquidGlassBackdrop provides sheetBackdrop,
                LocalLiquidGlassScrollState provides sheetScrollState,
                LocalLiquidGlassExtremeFling provides isSheetScrollingOrMoving
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Grabber + Cabeçalho superior arrastáveis (para cima = tela cheia, para baixo = fechar)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .then(sheet.dragModifier),
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

                    SheetToolbar(
                        title = if (isExistingInShelf) "Editar HQ" else "Registrar HQ",
                        closeTag = closeTag,
                        shareTag = "modal_share_card_button",
                        onClose = { sheet.dismiss() },
                        onShare = { onShareComic(buildFinalComicForSave(), SocialShareCardType.COMPLETED_READING, null) },
                        onSave = { saveDraft() },
                        modifier = sheet.dragModifier
                    )

                    // Compose and measure only the editor sections near the viewport.
                    val isSliderActive = com.kyant.backdrop.catalog.components.SliderTouchLock.isAnySliderActive
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f).fillMaxWidth().clipToBounds().testTag("editor_list"),
                        state = sheetScrollState,
                        userScrollEnabled = !isSliderActive,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = if (isSheetExpanded) 22.dp else 18.dp,
                            end = if (isSheetExpanded) 22.dp else 18.dp,
                            top = 4.dp,
                            bottom = 22.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "editor-section-0", contentType = "header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .inputWellSurface(RoundedCornerShape(18.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CatalogWorkSummaryHeader(
                                    coverUrl = draftComic.coverUrl,
                                    title = draftTitle,
                                    authors = draftAuthors,
                                    publisher = draftComic.publisher,
                                    releaseYear = draftComic.releaseYear,
                                    pageCount = draftComic.pageCount,
                                    onChangeTitle = { draftTitle = it },
                                    onChangeAuthors = { draftAuthors = it }
                                )
                                if (resolvedSynopsis.isNotBlank() || metadataLoading) {
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    CatalogEditionMetadata(
                                        entryId = draftComic.id,
                                        synopsis = resolvedSynopsis,
                                        pages = totalPagesInput,
                                        metadataLoading = metadataLoading,
                                        pageEditorOpen = false,
                                        pagesError = false,
                                        onOpenPageEditor = {},
                                        onChangePages = {},
                                        translationLoading = translationLoading,
                                        translationFailed = translationFailed,
                                        onRetryTranslation = { translationRetryToken++ }
                                    )
                                }
                                HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { fullPage = true }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                        .testTag("open_comic_page"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = s.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Ver página da HQ",
                                            color = s.accentColor,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = s.accentColor.copy(alpha = 0.75f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        item(key = "editor-section-2", contentType = "status_picker") {
                            CatalogStatusSegmentedBar(
                                value = draftStatus,
                                onChange = { nextStatus ->
                                    draftStatus = nextStatus
                                    if (nextStatus == ReadingStatus.READ && draftComic.pageCount > 1) {
                                        draftCurrentPage = draftComic.pageCount
                                    }
                                }
                            )
                        }

                        if (draftStatus == ReadingStatus.WANT_TO_READ) {
                            item(key = "editor-want-to-read", contentType = "want_to_read") {
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
                                            text = "Na fila de leitura",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = s.textPrimary
                                        )
                                        Text(
                                            text = "Registre o progresso quando começar a leitura.",
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                            color = s.textSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            item(key = "editor-dates", contentType = "dates_rereads") {
                                CatalogReadingDateAndRereadsSection(
                                    readDate = draftReadDate,
                                    rereadCount = draftRereadCount,
                                    allowRereads = appSettings.allowRereads,
                                    showMiniCalendar = showMiniCalendar,
                                    onToggleMiniCalendar = { showMiniCalendar = !showMiniCalendar },
                                    onChangeReadDate = { draftReadDate = it },
                                    onChangeRereadCount = { draftRereadCount = it }
                                )
                            }

                            if (draftStatus == ReadingStatus.READING && draftComic.pageCount > 1) {
                                item(key = "editor-progress", contentType = "reading_progress") {
                                    CatalogReadingProgressSection(
                                        currentPageProvider = { draftCurrentPage },
                                        pageCount = draftComic.pageCount,
                                        accentColor = s.accentColor,
                                        onChangePage = { v ->
                                            val nextPage = v.coerceIn(0, draftComic.pageCount.coerceAtLeast(1))
                                            draftCurrentPage = nextPage
                                            momentPageInput = nextPage.toString()
                                        }
                                    )
                                }
                            }
                        }

                        if (draftStatus == ReadingStatus.READ || draftRating > 0f || draftFavorite || draftReview.isNotBlank()) {
                            item(key = "editor-rating", contentType = "rating_favorite") {
                                CatalogRatingAndFavoriteSection(
                                    rating = draftRating,
                                    favorite = draftFavorite,
                                    halfStarRatings = appSettings.halfStarRatings,
                                    onChangeRating = { draftRating = it },
                                    onToggleFavorite = { draftFavorite = !draftFavorite }
                                )
                            }

                            item(key = "editor-review", contentType = "review_input") {
                                CatalogReviewCommentsSection(
                                    reviewText = draftReview,
                                    isExpanded = isSheetExpanded,
                                    onChangeReview = { draftReview = it }
                                )
                            }
                        }

                        item(key = "editor-section-4", contentType = "tags") {
                            CatalogTagsSection(
                                tags = draftTags,
                                quickReviewTags = appSettings.quickReviewTags,
                                comicTagInput = comicTagInput,
                                onChangeComicTagInput = { comicTagInput = it },
                                onUpdateTags = { nextTags -> draftTags = nextTags }
                            )
                        }

                        item(key = "editor-section-5", contentType = "moments") {
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
                                        date = draftReadDate.ifBlank { today },
                                        isSpoiler = false
                                    )
                                    draftMoments = draftMoments + newMoment
                                    momentNoteInput = ""
                                }
                            }
                            CatalogMomentsSection(
                                moments = draftMoments,
                                momentPageInput = momentPageInput,
                                momentNoteInput = momentNoteInput,
                                onChangeMomentPage = { momentPageInput = it },
                                onChangeMomentNote = { momentNoteInput = it },
                                onAddMoment = { addCurrentMoment() },
                                onRemoveMoment = { momentId ->
                                    draftMoments = draftMoments.filter { it.id != momentId }
                                },
                                onShareMoment = { moment ->
                                    onShareComic(
                                        buildFinalComicForSave(),
                                        SocialShareCardType.EPIC_MOMENT,
                                        moment.id
                                    )
                                }
                            )
                        }

                        item(key = "editor-section-6", contentType = "actions") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(999.dp))
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(s.accentBg)
                                        .clickable { saveDraft() }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Salvar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = s.accentText
                                    )
                                }

                                if (isExistingInShelf) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFFF43F5E).copy(alpha = 0.12f))
                                            .clickable { onDelete(draftComic.id) }
                                            .padding(vertical = 13.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFF43F5E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Excluir da biblioteca",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF43F5E),
                                            maxLines = 1,
                                            softWrap = false
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
    onDismiss: () -> Unit,
    onShareComic: (ComicEntry, SocialShareCardType, String?) -> Unit = { _, _, _ -> },
    readerProfile: com.example.model.ReaderProfile = com.example.model.ReaderProfile(),
    onLogin: () -> Unit = {},
    onCoverSelected: (ComicEntry, String) -> Unit = { _, _ -> },
    onSaveRegistration:(ComicEntry)->Unit = { onOpenCatalogWithStatus(it,it.status) },onDeleteRegistration:(String)->Unit = {}
) {
    var expanded by remember {mutableStateOf(false)}
    CatalogSheetModal(selectedComic=savedEntry ?: comic,isExistingInShelf=savedEntry!=null,isSheetExpanded=expanded,
        appSettings=appSettings,allUserComics=allUserComics,onlineSequencePool=onlineSequencePool,
        onSelectSequenceComic=onSelectSequenceComic,onUpdateComic={},onToggleExpand={expanded=!expanded},onSetExpanded={expanded=it},
        onSave=onSaveRegistration,onDelete=onDeleteRegistration,onDismiss=onDismiss,onShareComic=onShareComic,
        readerProfile=readerProfile,onLogin=onLogin,closeTag="synopsis_close_button",onRegisterFromPage={next->
            val saved=com.example.model.catalogEntryForEditing(next,allUserComics)
            if(saved!=null)onEditSavedComic(saved) else onOpenCatalogWithStatus(next,ReadingStatus.READ)
        })
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
    val sheet = rememberFluidSheet(onDismiss = onDismiss, onExpand = {})
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color.Black.copy(alpha = (if (s.isLight) 0.14f else 0.24f) * sheet.scrimFraction)) }
            .clickable { sheet.dismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = sheet.offset.coerceAtLeast(0f) }
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.96f)
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
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

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "manual-header", contentType = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(sheet.dragModifier),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cancelar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textSecondary,
                            modifier = Modifier.clickable { sheet.dismiss() }
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
                }

                item(key = "manual-title", contentType = "text_field") {
                    AppleTextField(
                        value = form.title,
                        onChange = { v -> onUpdateForm { it.copy(title = v) } },
                        placeholder = "Título da HQ (ex: Batman: Ano Um)"
                    )
                }

                item(key = "manual-authors-publisher", contentType = "dual_field") {
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
                }

                item(key = "manual-cover", contentType = "text_field") {
                    AppleTextField(
                        value = form.coverUrl,
                        onChange = { v -> onUpdateForm { it.copy(coverUrl = v) } },
                        placeholder = "Link da capa (opcional)"
                    )
                }

                item(key = "manual-submit", contentType = "submit_button") {
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
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val scaleXAnim = remember { Animatable(0.35f) }
    val scaleYAnim = remember { Animatable(0.65f) }
    LaunchedEffect(Unit) {
        launch {
            scaleXAnim.animateTo(
                targetValue = 1f,
                animationSpec = motion.spring(dampingRatio = 0.50f, stiffness = 360f)
            )
        }
        launch {
            scaleYAnim.animateTo(
                targetValue = 1f,
                animationSpec = motion.spring(dampingRatio = 0.56f, stiffness = 290f)
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
                    shape = RoundedCornerShape(34.dp),
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
                // Brand Header with QuadrinheiroLogo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuadrinheiroLogo(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        QuadrinheiroBrandText(
                            text = "Quadrinheiro",
                            fontSize = 17.sp,
                            color = s.textPrimary
                        )
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

                // Profile Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .inputWellSurface(RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UserAvatar(profile = profile, sizeDp = 34)
                    Column {
                        Text(
                            text = profile.name,
                            fontSize = 13.5.sp,
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
