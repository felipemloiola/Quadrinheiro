package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
internal data class DetailRouteState(
    val comic: ComicEntry,
    val entity: Pair<String, String>? = null,
    val tab: Int = 0,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val cachedDetail: CatalogDetails? = null,
    val expandedDescription: Boolean = false
)

@Composable
fun ComicDetailDialog(
    comic: ComicEntry,
    profile: ReaderProfile,
    library: List<ComicEntry>,
    onClose: () -> Unit,
    onRegister: (ComicEntry) -> Unit,
    onCover: (String) -> Unit,
    onLogin: () -> Unit,
    onShare: () -> Unit,
    useDialogWindow: Boolean = false,
    isExternalOverlayOpen: Boolean = false
) {
    val motion = LocalAppMotion.current
    val theme = LocalIOS27Theme.current
    val rootDepthHost = LocalRootDepthHost.current
    val scope = rememberCoroutineScope()
    val depthId = remember { "comic_detail_${System.nanoTime()}" }
    val presentationProgress = remember { Animatable(if (motion.reduced) 1f else 0f) }
    var predictiveExitProgress by remember { mutableFloatStateOf(0f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(motion.reduced) {
        if (motion.reduced) {
            presentationProgress.snapTo(1f)
        } else {
            presentationProgress.animateTo(
                targetValue = 1f,
                animationSpec = motion.ios26DetailZoomSpring()
            )
        }
    }

    LaunchedEffect(rootDepthHost) {
        if (rootDepthHost != null) {
            snapshotFlow { (presentationProgress.value * (1f - predictiveExitProgress)).coerceIn(0f, 1f) }
                .collect { depth -> rootDepthHost.update(depthId, depth) }
        }
    }
    DisposableEffect(rootDepthHost, depthId) {
        onDispose { rootDepthHost?.remove(depthId) }
    }

    fun closeWithLiquidZoom() {
        if (isClosing) return
        isClosing = true
        scope.launch {
            if (!motion.reduced && predictiveExitProgress < 0.85f) {
                presentationProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = motion.ios26ModalExitSpring()
                )
            }
            onClose()
        }
    }

    val detailContent: @Composable () -> Unit = {
        val p = presentationProgress.value.coerceIn(0f, 1f)
        val cornerDp = if (motion.reduced || p >= 0.992f) 0.dp else lerp(32f, 0f, p).dp
        val rimAlpha = if (motion.reduced || p >= 0.96f) 0f else (1f - p) * (if (theme.isDark) 0.34f else 0.52f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = if (motion.reduced) 1f else lerp(0.92f, 1f, p)
                    scaleX = scale
                    scaleY = scale
                    alpha = if (motion.reduced) p else (p * 1.45f).coerceIn(0f, 1f)
                    if (cornerDp > 0.dp) {
                        shape = ContinuousRoundedRectangle(cornerDp)
                        clip = true
                        shadowElevation = 24.dp.toPx() * (1f - p * 0.5f)
                    }
                }
                .then(
                    if (rimAlpha > 0.01f && cornerDp > 0.dp) {
                        Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = rimAlpha),
                            shape = ContinuousRoundedRectangle(cornerDp)
                        )
                    } else Modifier
                )
        ) {
            ComicDetailScreen(
                initialComic = comic,
                profile = profile,
                library = library,
                onBack = { closeWithLiquidZoom() },
                onRegister = onRegister,
                onCover = onCover,
                onLogin = { closeWithLiquidZoom(); onLogin() },
                onShare = { closeWithLiquidZoom(); onShare() },
                isExternalOverlayOpen = isExternalOverlayOpen,
                onRootPredictiveProgress = { predictiveExitProgress = it }
            )
        }
    }

    if (useDialogWindow) {
        Dialog(
            onDismissRequest = { closeWithLiquidZoom() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            detailContent()
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }
        ) {
            detailContent()
        }
    }
}

@Composable
internal fun ComicDetailScreen(
    initialComic: ComicEntry,
    profile: ReaderProfile,
    library: List<ComicEntry>,
    onBack: () -> Unit,
    onRegister: (ComicEntry) -> Unit,
    onCover: (String) -> Unit,
    onLogin: () -> Unit,
    onShare: () -> Unit,
    isExternalOverlayOpen: Boolean = false,
    initialDetailOverride: CatalogDetails? = null,
    detailLoader: (suspend (ComicEntry, Pair<String, String>?) -> CatalogDetails?)? = null,
    onRootPredictiveProgress: ((Float) -> Unit)? = null
) {
    val libraryMatch = remember(initialComic.id, initialComic.externalId, initialComic.displayTitle, library) {
        com.example.model.catalogEntryForEditing(initialComic, library)
    }
    var comic by remember(initialComic.id) { mutableStateOf(libraryMatch ?: initialComic) }
    var entity by remember { mutableStateOf<Pair<String, String>?>(null) }
    val history = remember { mutableStateListOf<DetailRouteState>() }
    var tab by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf(initialDetailOverride) }
    var loading by remember { mutableStateOf(initialDetailOverride == null) }
    var translating by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var expandedDescription by remember { mutableStateOf(false) }
    var fallbackBanner by remember { mutableStateOf("") }
    var editionPicker by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf<CommunityRating?>(null) }
    var pendingScrollTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var scrollRestoreToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(libraryMatch, history.size) {
        if (history.isEmpty() && entity == null && libraryMatch != null) {
            comic = libraryMatch
        }
    }

    val context = LocalContext.current
    val repository = remember(context) { runCatching { CommunityRepository(context) }.getOrNull() }
    val listState = rememberLazyListState()
    val menuHost = LocalLiquidGlassMenuHost.current

    fun goBack() {
        if (editionPicker) {
            editionPicker = false
            return
        }
        if (menuHost?.currentRequest != null) {
            menuHost.dismissMenu()
            return
        }
        if (history.isNotEmpty()) {
            val previous = history.removeAt(history.lastIndex)
            comic = previous.comic
            entity = previous.entity
            tab = previous.tab
            detail = previous.cachedDetail
            loading = previous.cachedDetail == null
            failed = false
            translating = false
            fallbackBanner = ""
            expandedDescription = previous.expandedDescription
            pendingScrollTarget = previous.scrollIndex to previous.scrollOffset
            scrollRestoreToken++
        } else {
            onBack()
        }
    }

    fun openComic(next: ComicEntry) {
        history.add(
            DetailRouteState(
                comic = comic,
                entity = entity,
                tab = tab,
                scrollIndex = listState.firstVisibleItemIndex,
                scrollOffset = listState.firstVisibleItemScrollOffset,
                cachedDetail = detail,
                expandedDescription = expandedDescription
            )
        )
        comic = next
        entity = null
        tab = 0
        detail = null
        loading = true
        failed = false
        translating = false
        fallbackBanner = ""
        expandedDescription = false
        pendingScrollTarget = 0 to 0
        scrollRestoreToken++
    }

    fun openEntity(type: String, id: String) {
        history.add(
            DetailRouteState(
                comic = comic,
                entity = entity,
                tab = tab,
                scrollIndex = listState.firstVisibleItemIndex,
                scrollOffset = listState.firstVisibleItemScrollOffset,
                cachedDetail = detail,
                expandedDescription = expandedDescription
            )
        )
        entity = type to id
        tab = 0
        detail = null
        loading = true
        failed = false
        translating = false
        fallbackBanner = ""
        expandedDescription = false
        pendingScrollTarget = 0 to 0
        scrollRestoreToken++
    }

    fun selectTab(nextTab: Int) {
        if (tab != nextTab) {
            tab = nextTab
            pendingScrollTarget = 0 to 0
            scrollRestoreToken++
        }
    }

    LaunchedEffect(scrollRestoreToken) {
        val target = pendingScrollTarget ?: return@LaunchedEffect
        pendingScrollTarget = null
        listState.scrollToItem(target.first.coerceAtLeast(0), target.second.coerceAtLeast(0))
    }

    LaunchedEffect(comic.id, comic.externalId, entity, retry) {
        if (detail != null && retry == 0) {
            loading = false
            failed = false
            return@LaunchedEffect
        }
        val hasLocalData = detailLoader == null && entity == null && comic.authors.isNotBlank() &&
            comic.publisher.isNotBlank() && comic.pageCount > 0 &&
            (comic.synopsisPtBr.isNotBlank() || comic.synopsis.isNotBlank())
        if (hasLocalData && retry == 0) {
            loading = false
            failed = false
            return@LaunchedEffect
        }
        loading = true
        failed = false
        try {
            detail = if (detailLoader != null) {
                detailLoader(comic, entity)
            } else {
                withContext(Dispatchers.IO) {
                    entity?.let { ComicVineCatalog.details(it.first, it.second) }
                        ?: ComicVineCatalog.autoResolve(comic)
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    LaunchedEffect(detail?.id, detail?.type, comic.id, comic.externalId, entity, retry) {
        fallbackBanner = ""
        val resource = detail ?: return@LaunchedEffect
        if (resource.banner.isNotBlank()) return@LaunchedEffect
        if (retry <= 0) return@LaunchedEffect
        try {
            val banner = ComicBannerResolver.resolve(
                resource,
                if (entity == null) comic.displayTitle else resource.title,
                force = true
            )
            if (detail?.id == resource.id && detail?.type == resource.type) fallbackBanner = banner
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(detail?.id, detail?.type) {
        val original = detail ?: return@LaunchedEffect
        if (original.description.isBlank()) return@LaunchedEffect
        if (PortugueseComicTranslator.isPortugueseText(original.description)) return@LaunchedEffect
        val instant = PortugueseComicTranslator.translateSynopsisToPtBr(
            rawSynopsis = original.description,
            translatedTitle = original.title,
            authors = original.creators.joinToString(", ") { it.name },
            publisher = original.publisher,
            releaseYear = 0,
            pageCount = original.pageCount,
            allowNetworkTranslation = false
        )
        if (instant.isNotBlank()) {
            if (detail?.id == original.id && detail?.type == original.type) {
                detail = original.copy(description = instant)
            }
            return@LaunchedEffect
        }
        translating = true
        try {
            val translated = withContext(Dispatchers.IO) {
                kotlinx.coroutines.withTimeoutOrNull(3500L) {
                    val online = PortugueseComicTranslator.translateSynopsisToPtBr(
                        rawSynopsis = original.description,
                        translatedTitle = original.title,
                        authors = original.creators.joinToString(", ") { it.name },
                        publisher = original.publisher,
                        releaseYear = 0,
                        pageCount = original.pageCount,
                        allowNetworkTranslation = true
                    )
                    if (online.isNotBlank()) original.copy(description = online)
                    else ComicVineCatalog.translated(original)
                }
            }
            if (translated != null && detail?.id == translated.id && detail?.type == translated.type) {
                detail = translated
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
        } finally {
            translating = false
        }
    }

    LaunchedEffect(comic.id, comic.externalId, repository?.currentUid()) {
        rating = null
        if (repository?.currentUid() != null) {
            try {
                rating = repository.rating(comic)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
            }
        }
    }

    val catalogEntries by com.example.data.ModernCatalog.entries.collectAsState()
    val hasActiveOverlay = editionPicker || isExternalOverlayOpen || menuHost?.currentRequest != null
    val previousRoute = history.lastOrNull()
    val detailOverlayBackdrop = rememberLayerBackdrop()

    PredictiveBackRootExitContainer(
        enabled = history.isEmpty() && !hasActiveOverlay,
        onExit = onBack,
        onProgressChanged = onRootPredictiveProgress
    ) {
        PredictiveBackRouteContainer(
            canNavigateBack = history.isNotEmpty(),
            enabled = !hasActiveOverlay,
            onBackCommit = { goBack() },
            previousContent = previousRoute?.let { prev ->
                {
                    val previewListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = prev.scrollIndex,
                        initialFirstVisibleItemScrollOffset = prev.scrollOffset
                    )
                    ComicDetailRouteBody(
                        comic = prev.comic,
                        entity = prev.entity,
                        tab = prev.tab,
                        detail = prev.cachedDetail,
                        loading = false,
                        translating = false,
                        failed = false,
                        expandedDescription = prev.expandedDescription,
                        fallbackBanner = "",
                        rating = null,
                        profile = profile,
                        library = library,
                        catalogEntries = catalogEntries,
                        listState = previewListState,
                        isPreview = true,
                        onTabChange = {},
                        onToggleDescription = {},
                        onRetry = {},
                        onOpenEditionPicker = {},
                        onOpenComic = {},
                        onOpenEntity = { _, _ -> },
                        onRegister = {},
                        onLogin = {},
                        onShare = {},
                        onBack = {}
                    )
                }
            }
        ) {
            ComicDetailRouteBody(
                comic = comic,
                entity = entity,
                tab = tab,
                detail = detail,
                loading = loading,
                translating = translating,
                failed = failed,
                expandedDescription = expandedDescription,
                fallbackBanner = fallbackBanner,
                rating = rating,
                profile = profile,
                library = library,
                catalogEntries = catalogEntries,
                listState = listState,
                isPreview = false,
                captureBackdrop = detailOverlayBackdrop,
                onTabChange = { selectTab(it) },
                onToggleDescription = { expandedDescription = !expandedDescription },
                onRetry = { retry++ },
                onOpenEditionPicker = { editionPicker = true },
                onOpenComic = { openComic(it) },
                onOpenEntity = { type, id -> openEntity(type, id) },
                onRegister = onRegister,
                onLogin = onLogin,
                onShare = onShare,
                onBack = { goBack() }
            )
        }
    }

    if (editionPicker) {
        CompositionLocalProvider(LocalLiquidGlassOverlayBackdrop provides detailOverlayBackdrop) {
            EditionChooser(comic.displayTitle, onClose = { editionPicker = false }) { edition ->
                comic = comic.copy(
                    externalId = edition.externalId,
                    sourceLabel = edition.sourceLabel,
                    publisher = edition.publisher.ifBlank { comic.publisher }
                )
                detail = null
                editionPicker = false
            }
        }
    }
}

@Composable
private fun ComicDetailRouteBody(
    comic: ComicEntry,
    entity: Pair<String, String>?,
    tab: Int,
    detail: CatalogDetails?,
    loading: Boolean,
    translating: Boolean,
    failed: Boolean,
    expandedDescription: Boolean,
    fallbackBanner: String,
    rating: CommunityRating?,
    profile: ReaderProfile,
    library: List<ComicEntry>,
    catalogEntries: List<ComicEntry>,
    listState: LazyListState,
    isPreview: Boolean,
    captureBackdrop: LayerBackdrop? = null,
    onTabChange: (Int) -> Unit,
    onToggleDescription: () -> Unit,
    onRetry: () -> Unit,
    onOpenEditionPicker: () -> Unit,
    onOpenComic: (ComicEntry) -> Unit,
    onOpenEntity: (String, String) -> Unit,
    onRegister: (ComicEntry) -> Unit,
    onLogin: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    val t = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val isComic = entity == null
    val effectiveCover = comic.coverUrl.ifBlank { detail?.image.orEmpty() }
    val detailBackdrop = captureBackdrop ?: rememberLayerBackdrop()
    val collapsed by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24 }
    }
    val related = remember(comic.id, comic.externalId, detail?.issues, detail?.characters, detail?.creators, library, catalogEntries) {
        val pool = library + catalogEntries + CuratedCatalog.curatedComicsCatalog
        ComicDetailPresentation.relatedEditionsWithReasons(comic, detail, pool)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(t.bgPrimary)
            .testTag(if (isPreview) "comic_detail_preview_page" else "comic_detail_page")
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(t.bgPrimary)
                .layerBackdrop(detailBackdrop)
        ) {
        if (effectiveCover.isNotBlank() && !settings.reduceTransparency) {
            ReadingCoverAtmosphere(effectiveCover, t.bgPrimary, fraction = .55f)
        }
        if (isComic && tab == 3) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 68.dp)
            ) {
                DetailTabs(tab, onTabChange)
                if (!isPreview) {
                    CommunityScreen(library, profile, onLogin, comic)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(if (isPreview) "comic_detail_preview_list" else "comic_detail_list"),
                contentPadding = PaddingValues(
                    bottom = 32.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item(key = "detail_header") {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val banner = detail?.banner.orEmpty().ifBlank { fallbackBanner }
                        val heroHeight = maxWidth / 1.9f
                        if (banner.isNotBlank()) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(heroHeight)
                                    .testTag(if (isPreview) "comic_detail_preview_banner" else "comic_detail_banner")
                            ) {
                                ProfileImage(banner, null, Modifier.fillMaxSize())
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0f to t.bgPrimary.copy(alpha = .4f),
                                                .4f to Color.Transparent,
                                                1f to t.bgPrimary
                                            )
                                        )
                                )
                            }
                        }
                        Column {
                            Spacer(
                                Modifier.height(
                                    if (banner.isNotBlank()) (heroHeight - 28.dp).coerceAtLeast(72.dp) else 72.dp
                                )
                            )
                            Row(
                                Modifier.padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (isComic) {
                                    BookCover(
                                        effectiveCover,
                                        comic.displayTitle,
                                        spine3d = false,
                                        modifier = Modifier.width(104.dp).aspectRatio(2f / 3f)
                                    )
                                } else {
                                    ProfileImage(
                                        detail?.image.orEmpty(),
                                        detail?.title,
                                        Modifier.size(110.dp).clip(RoundedCornerShape(22.dp)),
                                        face = true
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isComic) comic.displayTitle else detail?.title ?: "Carregando…",
                                        color = t.textPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 29.sp,
                                        modifier = Modifier
                                            .semantics { heading() }
                                            .testTag(if (isPreview) "comic_detail_preview_title" else "comic_detail_title")
                                    )
                                    val publisher = detail?.publisher.orEmpty().ifBlank { if (isComic) comic.publisher else "" }
                                    if (publisher.isNotBlank()) Text(publisher, color = t.textSecondary, fontSize = 14.sp)
                                    val authors = comic.authors.ifBlank {
                                        detail?.creators.orEmpty()
                                            .filter { it.role.contains("writer", true) || it.role.contains("artist", true) }
                                            .take(3)
                                            .joinToString(", ") { it.name }
                                    }
                                    if (isComic && authors.isNotBlank()) Text(authors, color = t.textSecondary, fontSize = 14.sp)
                                    if (isComic) {
                                        rating?.takeIf { it.count > 0 }?.let {
                                            Text(
                                                "★ ${String.format(java.util.Locale.getDefault(), "%.1f", it.average)} · ${it.count} avaliações",
                                                color = t.textPrimary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        val currentLibraryEntry = com.example.model.catalogEntryForEditing(comic, library)
                                        if (currentLibraryEntry != null && currentLibraryEntry.status == ReadingStatus.WANT_TO_READ) {
                                            Text(
                                                text = "Na lista Quero Ler",
                                                color = t.textSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.testTag("comic_detail_status_badge")
                                            )
                                        }
                                        val actionLabel = when {
                                            currentLibraryEntry != null && currentLibraryEntry.status == ReadingStatus.READING ->
                                                if (currentLibraryEntry.pageCount > 0) "Em leitura · pág. ${currentLibraryEntry.currentPage}/${currentLibraryEntry.pageCount}"
                                                else "Em leitura (pág. ${currentLibraryEntry.currentPage})"
                                            currentLibraryEntry != null && currentLibraryEntry.status == ReadingStatus.READ ->
                                                if (currentLibraryEntry.rating > 0f) "Concluída · ★ ${currentLibraryEntry.rating.toInt()}/5" else "Concluída ★"
                                            else -> "Registrar leitura"
                                        }
                                        TextButton(
                                            onClick = {
                                                val target = currentLibraryEntry ?: comic.copy(
                                                    coverUrl = effectiveCover,
                                                    pageCount = detail?.pageCount?.takeIf { it > 0 } ?: comic.pageCount,
                                                    isbn = comic.isbn.ifBlank { detail?.isbn.orEmpty() }
                                                )
                                                onRegister(target)
                                            },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("comic_detail_register_button"),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Text(actionLabel, color = t.accent, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (isComic) item(key = "detail_tabs") { DetailTabs(tab, onTabChange) }
                if (loading) {
                    item(key = "detail_loading") {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .semantics {
                                    contentDescription = "Carregando informações da edição"
                                    liveRegion = LiveRegionMode.Polite
                                },
                            color = t.accent
                        )
                    }
                }
                if (failed) {
                    item(key = "detail_retry") {
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .heightIn(min = 48.dp)
                                .testTag("comic_detail_retry_button")
                        ) {
                            Text("Tentar carregar informações novamente", color = t.accent)
                        }
                    }
                }
                if (isComic && detail == null && !loading) {
                    item(key = "detail_find_edition") {
                        TextButton(
                            onClick = onOpenEditionPicker,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .heightIn(min = 48.dp)
                                .testTag("comic_detail_edition_button")
                        ) {
                            Text("Encontrar edição no catálogo", color = t.accent)
                        }
                    }
                }
                if (!isComic || tab == 0) {
                    val candidateSynopsis = when {
                        isComic && comic.synopsisPtBr.isNotBlank() && !comic.synopsisPtBr.contains("series digitais", ignoreCase = true) -> comic.synopsisPtBr
                        isComic && comic.synopsis.isNotBlank() && !comic.synopsis.contains("series digitais", ignoreCase = true) -> comic.synopsis
                        detail?.description?.isNotBlank() == true && !detail.description.contains("series digitais", ignoreCase = true) -> detail.description
                        else -> if (isComic) comic.synopsisPtBr.ifBlank { comic.synopsis.ifBlank { detail?.description.orEmpty() } } else detail?.description.orEmpty()
                    }
                    val description = PortugueseComicTranslator.formatCleanSynopsis(candidateSynopsis)
                    if (description.isNotBlank()) {
                        item(key = "detail_synopsis") {
                            DetailSection(if (isComic) "Sinopse" else "Biografia") {
                                DetailDescription(description, translating, expandedDescription, onToggleDescription)
                            }
                        }
                    }
                    item(key = "detail_meta") {
                        DetailSection("Detalhes") {
                            val rows = if (isComic) {
                                listOf(
                                    "Publicação" to detail?.date.orEmpty().ifBlank { comic.releaseYear.takeIf { it > 0 }?.toString().orEmpty() },
                                    "Formato" to detail?.format.orEmpty().ifBlank { comic.format },
                                    "ISBN" to comic.isbn.ifBlank { detail?.isbn.orEmpty() },
                                    "Páginas" to (detail?.pageCount?.takeIf { it > 0 } ?: comic.pageCount).takeIf { it > 0 }?.toString().orEmpty()
                                )
                            } else {
                                listOf(
                                    "Nome" to detail?.realName.orEmpty(),
                                    "Também conhecido como" to detail?.aliases.orEmpty(),
                                    "Aparições" to detail?.appearances?.takeIf { it > 0 }?.toString().orEmpty(),
                                    "Poderes" to detail?.powers.orEmpty().joinToString(", ")
                                )
                            }
                            rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                                Text(label, color = t.textSecondary, fontSize = 12.sp)
                                Text(value, color = t.textPrimary, fontSize = 15.sp)
                                HorizontalDivider(color = t.divider)
                            }
                        }
                    }
                    if (related.isNotEmpty()) {
                        item(key = "detail_related") {
                            DetailSection(if (isComic) "Edições relacionadas" else "Aparições em quadrinhos") {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(related, key = { it.comic.externalId.ifBlank { it.comic.id } }) { rec ->
                                        val issue = rec.comic
                                        Column(
                                            Modifier
                                                .width(104.dp)
                                                .clickable(
                                                    role = Role.Button,
                                                    onClickLabel = "Abrir ${issue.displayTitle}"
                                                ) { onOpenComic(issue) }
                                                .testTag("related_comic_${issue.id}"),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            BookCover(
                                                issue.coverUrl,
                                                issue.displayTitle,
                                                spine3d = false,
                                                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                            )
                                            if (isComic && rec.reasonLabel.isNotBlank()) {
                                                Text(
                                                    rec.reasonLabel,
                                                    color = t.accent,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                issue.displayTitle,
                                                color = t.textPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val credits = (if (tab == 1) detail?.characters.orEmpty() else detail?.creators.orEmpty()).distinctBy { it.id }
                    if (detail?.creditsScope == "series") {
                        item(key = "detail_credits_scope") {
                            Text(
                                "Personagens e equipe da série",
                                color = t.textSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                    itemsIndexed(credits, key = { _, it -> "${tab}_${it.id}" }) { index, credit ->
                        val type = if (tab == 1) "character" else "person"
                        CreditRow(credit, type, autoLoad = !isPreview && index < 6) { onOpenEntity(type, credit.id) }
                    }
                    if (!loading && credits.isEmpty()) {
                        item(key = "detail_credits_empty") {
                            Text(
                                "Ainda não há informações desta edição no catálogo.",
                                color = t.textSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }
        }
        CompositionLocalProvider(LocalLiquidGlassBackdrop provides detailBackdrop) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        if (settings.reduceTransparency) {
                            Brush.verticalGradient(listOf(t.bgPrimary, t.bgPrimary.copy(alpha = .94f), Color.Transparent))
                        } else {
                            Brush.verticalGradient(listOf(t.bgPrimary.copy(alpha = .38f), t.bgPrimary.copy(alpha = .14f), Color.Transparent))
                        }
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsBackButton(
                    onBack = onBack,
                    collapsed = collapsed,
                    label = if (isComic) "Voltar" else "HQ",
                    testTag = if (isPreview) "comic_detail_preview_back" else "settings_back_button"
                )
                Spacer(Modifier.weight(1f))
                if (isComic) {
                    LiquidGlassCircleButton(
                        icon = LucideIcons.ShareIos,
                        contentDescription = "Compartilhar",
                        onClick = onShare,
                        size = 48.dp,
                        iconSize = 20.dp,
                        tint = t.accent,
                        tag = if (isPreview) "comic_detail_preview_share" else "comic_detail_share_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailDescription(text: String, translating: Boolean, expanded: Boolean, onToggle: () -> Unit) {
    val t = LocalIOS27Theme.current
    var overflowing by remember(text) { mutableStateOf(false) }
    Text(
        text,
        color = t.textPrimary,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 5,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("comic_description"),
        onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow }
    )
    if (overflowing || expanded) {
        TextButton(
            onClick = onToggle,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("comic_description_toggle"),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text(if (expanded) "Menos" else "Mais", color = t.accent)
        }
    }
    if (translating) {
        Text(
            "Atualizando tradução…",
            color = t.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

@Composable
private fun DetailTabs(tab: Int, onChange: (Int) -> Unit) {
    val t = LocalIOS27Theme.current
    val labels = remember { listOf("Sobre", "Personagens", "Equipe", "Comunidade") }
    LazyRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .semantics { selectableGroup() },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(labels.size) { index ->
            val isSelected = index == tab
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(
                    onClick = { onChange(index) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics {
                            selected = isSelected
                            role = Role.Tab
                        }
                        .testTag("detail_tab_$index")
                ) {
                    Text(
                        labels[index],
                        color = if (isSelected) t.accent else t.textSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (isSelected) Box(Modifier.size(24.dp, 2.dp).background(t.accent, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalIOS27Theme.current
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            color = t.textPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        content()
    }
}

@Composable
private fun CreditRow(credit: CatalogCredit, type: String, autoLoad: Boolean = true, onClick: () -> Unit) {
    val t = LocalIOS27Theme.current
    var image by remember(type, credit.id) { mutableStateOf(CatalogPortraits.cached(type, credit.id).orEmpty()) }
    LaunchedEffect(type, credit.id, autoLoad) {
        if (autoLoad && image.isBlank()) {
            try {
                image = CatalogPortraits.image(type, credit.id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Throwable) {
            }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .focusable()
            .clickable(role = Role.Button, onClickLabel = "Abrir ${credit.name}", onClick = onClick)
            .testTag("credit_row_${type}_${credit.id}")
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(t.bgSecondary),
            contentAlignment = Alignment.Center
        ) {
            if (image.isNotBlank()) ProfileImage(image, null, Modifier.fillMaxSize(), face = true)
            else Text(
                credit.name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) },
                color = t.textSecondary,
                fontSize = 18.sp
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(credit.name, color = t.textPrimary)
            if (credit.role.isNotBlank()) {
                Text(ComicDetailPresentation.creditRole(credit.role), color = t.textSecondary, fontSize = 13.sp)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textTertiary)
    }
}

@Composable
private fun EditionChooser(initialQuery: String, onClose: () -> Unit, onChoose: (ComicEntry) -> Unit) {
    val t = LocalIOS27Theme.current
    var query by remember { mutableStateOf(initialQuery) }
    var entries by remember { mutableStateOf(emptyList<ComicEntry>()) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val rowShape = remember { ContinuousRoundedRectangle(16.dp) }
    LaunchedEffect(query) {
        entries = emptyList(); failed = false; busy = false
        if (query.length < 2) return@LaunchedEffect
        busy = true
        try {
            kotlinx.coroutines.delay(300)
            entries = ComicVineCatalog.search(query).items
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            failed = true
        } finally {
            busy = false
        }
    }
    CurationSheet(onDismiss = onClose, maxHeightFraction = 0.84f) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .testTag("edition_chooser_sheet"),
            contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CurationSheetHeader(
                    title = "Edição",
                    subtitle = "Escolha a edição correspondente para ver personagens e equipe.",
                    onClose = onClose,
                    closeTag = "edition_chooser_close"
                )
            }
            item {
                AppleTextField(
                    value = query,
                    onChange = { query = it.take(100) },
                    placeholder = "Buscar edição",
                    shape = ContinuousCapsule,
                    leadingIcon = Icons.Default.Search,
                    useContentMaterial = false
                )
            }
            if (busy) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Buscando edições"
                                liveRegion = LiveRegionMode.Polite
                            },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IOSLoadingIndicator(modifier = Modifier.size(18.dp), color = t.accent)
                        Text("Buscando edições…", color = t.textSecondary, fontSize = 13.sp)
                    }
                }
            }
            if (failed) {
                item {
                    Text(
                        "Não foi possível buscar. Tente outro termo.",
                        color = t.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
            items(entries.distinctBy { it.id }, key = { it.id }) { entry ->
                val interaction = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 76.dp)
                        .pressScaleFeedback(interaction, targetScale = 0.98f)
                        .contentMaterialSurface(rowShape)
                        .focusable()
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Selecionar ${entry.displayTitle}"
                        ) {
                            onChoose(entry)
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BookCover(entry.coverUrl, entry.displayTitle, spine3d = false, modifier = Modifier.width(48.dp).height(72.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.displayTitle, color = t.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val meta = listOfNotNull(
                            entry.issueNumber.takeIf { it > 0 }?.let { "Edição $it" },
                            entry.releaseYear.takeIf { it > 0 }?.toString()
                        ).joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(meta, color = t.textSecondary, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textTertiary)
                }
            }
            if (!busy && !failed && entries.isEmpty()) {
                item {
                    Text(
                        "Nenhuma edição encontrada. Tente o título original.",
                        color = t.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        }
    }
}
