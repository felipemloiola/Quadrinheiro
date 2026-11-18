package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalComicDetailViewModel =
    staticCompositionLocalOf<com.example.viewmodel.QuadrinheiroViewModel?> { null }

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
    recommendationContext: RecommendedComic? = null,
    resolveDetails: suspend (ComicEntry) -> CatalogDetails? = { ComicVineCatalog.autoResolve(it) }
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ComicDetailScreen(
            initialComic = comic,
            profile = profile,
            library = library,
            onBack = onClose,
            onRegister = onRegister,
            onCover = onCover,
            onLogin = { onClose(); onLogin() },
            onShare = onShare,
            recommendationContext = recommendationContext,
            resolveDetails = resolveDetails
        )
    }
}

@Composable
private fun ComicDetailScreen(
    initialComic: ComicEntry,
    profile: ReaderProfile,
    library: List<ComicEntry>,
    onBack: () -> Unit,
    onRegister: (ComicEntry) -> Unit,
    onCover: (String) -> Unit,
    onLogin: () -> Unit,
    onShare: () -> Unit,
    recommendationContext: RecommendedComic? = null,
    resolveDetails: suspend (ComicEntry) -> CatalogDetails?
) {
    val curationViewModel = LocalComicDetailViewModel.current
        ?: androidx.lifecycle.viewmodel.compose.viewModel<com.example.viewmodel.QuadrinheiroViewModel>()
    val liveLibrary by curationViewModel.comics.collectAsStateWithLifecycle()
    var observedLiveLibrary by remember(curationViewModel) { mutableStateOf(liveLibrary.isNotEmpty()) }
    SideEffect { if (liveLibrary.isNotEmpty()) observedLiveLibrary = true }
    val effectiveLibrary = if (observedLiveLibrary || liveLibrary.isNotEmpty()) liveLibrary else library
    val libraryMatch = remember(initialComic.id, initialComic.externalId, initialComic.displayTitle, effectiveLibrary) {
        com.example.model.catalogEntryForEditing(initialComic, effectiveLibrary)
    }
    var comic by remember(initialComic.id, libraryMatch?.id) { mutableStateOf(libraryMatch ?: initialComic) }
    LaunchedEffect(libraryMatch) {
        if (libraryMatch != null) comic = libraryMatch
    }
    val t = LocalIOS27Theme.current
    val context = LocalContext.current
    var entity by remember { mutableStateOf<Pair<String, String>?>(null) }
    val history = remember { mutableStateListOf<Pair<ComicEntry, Pair<String, String>?>>() }
    fun goBack() {
        if (history.isNotEmpty()) { val previous = history.removeAt(history.lastIndex); comic = previous.first; entity = previous.second }
        else onBack()
    }
    fun openComic(next: ComicEntry) { history.add(comic to entity); comic = next; entity = null }
    fun openEntity(type: String, id: String) { history.add(comic to entity); entity = type to id }
    var detail by remember(comic.id, comic.externalId, entity) { mutableStateOf<CatalogDetails?>(null) }
    var loading by remember(comic.id, comic.externalId, entity) { mutableStateOf(true) }
    var translating by remember(comic.id, comic.externalId, entity) { mutableStateOf(false) }
    var failed by remember(comic.id, comic.externalId, entity) { mutableStateOf(false) }
    var retry by remember(comic.id, comic.externalId, entity) { mutableIntStateOf(0) }
    var tab by remember(comic.id, comic.externalId, entity) { mutableIntStateOf(0) }
    var expandedDescription by remember(comic.id, comic.externalId, entity) { mutableStateOf(false) }
    var fallbackBanner by remember(comic.id, comic.externalId, entity) { mutableStateOf("") }
    var editionPicker by remember { mutableStateOf(false) }
    var expandedCover by remember(comic.id) { mutableStateOf(false) }
    var coverBounds by remember(comic.id) {mutableStateOf<Rect?>(null)}
    var shareSheetOpen by remember(comic.id) { mutableStateOf(false) }
    var addToListOpen by remember(comic.id) { mutableStateOf(false) }
    var whySheetOpen by remember(comic.id) { mutableStateOf(false) }
    var registrationSheetOpen by remember(comic.id) { mutableStateOf(false) }
    var registrationSheetExpanded by remember(comic.id) { mutableStateOf(false) }
    var shareNoticeText by remember(comic.id) { mutableStateOf<String?>(null) }
    val detailMenuHost = remember { LiquidGlassMenuHostState() }
    val moreMenuId = remember { "comic_detail_more_menu" }
    var moreMenuBounds by remember { mutableStateOf(IntRect.Zero) }
    LaunchedEffect(shareNoticeText) {
        if (shareNoticeText != null) {
            kotlinx.coroutines.delay(3200)
            shareNoticeText = null
        }
    }
    var translatedSynopsis by remember(comic.id,entity) {mutableStateOf("")}
    var rating by remember(comic.id, comic.externalId) { mutableStateOf<CommunityRating?>(null) }
    val repository = remember { CommunityRepository(context) }
    val listState = rememberLazyListState()
    val communityListState = rememberLazyListState()
    LaunchedEffect(comic.id, comic.externalId, entity, tab) { listState.scrollToItem(0) }
    val canNavigateBack = !shareSheetOpen && !registrationSheetOpen && !expandedCover && detailMenuHost.currentRequest == null
    val dialogBackProgress = rememberDialogBackProgress(enabled = canNavigateBack && history.isEmpty(), onClose = onBack)
    BackHandler(enabled = canNavigateBack && history.isNotEmpty()) { goBack() }
    LaunchedEffect(comic.id, comic.externalId, entity, retry) {
        loading = true; failed = false
        try {
            detail = withContext(Dispatchers.IO) {
                entity?.let { ComicVineCatalog.details(it.first, it.second) } ?: resolveDetails(comic)
            }
        }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { failed = true }
        finally { loading = false }
    }
    LaunchedEffect(detail?.id, detail?.type, comic.id, comic.externalId, entity, retry) {
        fallbackBanner = ""
        val resource = detail ?: return@LaunchedEffect
        if (resource.banner.isNotBlank()) return@LaunchedEffect
        try {
            val banner = ComicBannerResolver.resolve(resource, if (entity == null) comic.displayTitle else resource.title, force = retry > 0, allowCharacterFallback = entity != null)
            if (detail?.id == resource.id && detail?.type == resource.type) fallbackBanner = banner
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { }
    }
    val synopsisSource = if(entity!=null) detail?.description.orEmpty() else listOf(comic.synopsisPtBr,detail?.description.orEmpty(),comic.synopsis)
        .firstOrNull {it.isNotBlank() && PortugueseComicTranslator.isPortugueseText(it)}
        ?: comic.synopsis.ifBlank {comic.synopsisPtBr.ifBlank {detail?.description.orEmpty()}}
    LaunchedEffect(synopsisSource,comic.id,entity) {
        translatedSynopsis=""
        if(synopsisSource.isBlank())return@LaunchedEffect
        if(PortugueseComicTranslator.isPortugueseText(synopsisSource)){translatedSynopsis=synopsisSource;return@LaunchedEffect}
        translating = true
        try {
            translatedSynopsis=kotlinx.coroutines.runInterruptible(Dispatchers.IO) {
                PortugueseComicTranslator.translateSynopsisToPtBr(synopsisSource,comic.displayTitle,comic.authors,comic.publisher,comic.releaseYear,comic.pageCount,allowNetworkTranslation=true)
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { }
        finally { translating = false }
    }
    LaunchedEffect(comic.id, comic.externalId, repository.currentUid()) {
        if (repository.currentUid() != null) try { rating = repository.rating(comic) }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { }
    }
    val isComic = entity == null
    val curationState by curationViewModel.curation.state.collectAsStateWithLifecycle()
    LaunchedEffect(isComic, curationState.owner) { if (isComic) curationViewModel.curation.load() }
    var listToOpen by remember(curationState.owner, comic.id) { mutableStateOf<ComicList?>(null) }
    var listComic by remember(curationState.owner, comic.id) { mutableStateOf<ComicEntry?>(null) }
    var addedListNotice by remember(curationState.owner, comic.id) { mutableStateOf<ComicList?>(null) }
    LaunchedEffect(addedListNotice?.id) {
        if (addedListNotice != null) {
            kotlinx.coroutines.delay(3600)
            addedListNotice = null
        }
    }
    val currentReading = com.example.model.catalogEntryForEditing(comic, effectiveLibrary)
    val readingPresentation = ComicReadingPresentation.from(currentReading)
    fun registerCurrent() {
        registrationSheetExpanded = false
        registrationSheetOpen = true
    }
    val effectiveCover = comic.coverUrl.ifBlank { detail?.image.orEmpty() }
    FullScreenDialogWindow(
        lightStatusBar = !com.example.ui.theme.LocalIsDarkTheme.current &&
            (tab == 3 || (effectiveCover.isBlank() && detail?.banner.isNullOrBlank() && fallbackBanner.isBlank()))
    )
    val collapsed by rememberScrollCollapsed(if (isComic && tab == 3) communityListState else listState)
    val catalogEntries by com.example.data.ModernCatalog.entries.collectAsState()
    val onlineSequence by curationViewModel.onlineSequencePool.collectAsStateWithLifecycle()
    val editorialShelves by EditorialDiscovery.shelves.collectAsStateWithLifecycle()
    val exploreRecState by curationViewModel.exploreRecommendationState.collectAsStateWithLifecycle()
    val related = remember(
        comic.id,
        comic.externalId,
        detail?.issues,
        detail?.characters,
        detail?.creators,
        library,
        catalogEntries,
        onlineSequence,
        editorialShelves,
        exploreRecState.recommendations
    ) {
        val pool = library +
            onlineSequence +
            catalogEntries +
            editorialShelves.values.flatten() +
            exploreRecState.recommendations.map { it.comic } +
            CuratedCatalog.curatedComicsCatalog
        ComicDetailPresentation.relatedEditionsWithReasons(comic, detail, pool)
    }
    val motion=com.example.ui.theme.LocalAppMotion.current
    val settings=com.example.ui.theme.LocalAppSettings.current
    val isDark=LocalIsDarkTheme.current
    val detailBackdrop=rememberLayerBackdrop()
    val activeRecommendation = if (history.isEmpty() && isComic) recommendationContext else null
    val freezeDetailBackdrop=expandedCover || editionPicker || listToOpen != null || listComic != null ||
        shareSheetOpen || addToListOpen || whySheetOpen || registrationSheetOpen || detailMenuHost.currentRequest != null
    val fadeDistance=with(LocalDensity.current){180.dp.toPx()}
    val scrollFraction:()->Float={if(listState.firstVisibleItemIndex>0) 1f else (listState.firstVisibleItemScrollOffset/fadeDistance).coerceIn(0f,1f)}
    Box(Modifier.fillMaxSize().graphicsLayer { translationX = size.width * dialogBackProgress.value }) {
    BoxWithConstraints(Modifier.fillMaxSize().background(t.bgPrimary).testTag("comic_detail_page")) {
        val persistentBanner=detail?.banner.orEmpty().ifBlank {fallbackBanner}.ifBlank {if(isComic)effectiveCover else ""}
        val persistentHeroHeight=(maxWidth/1.85f).coerceIn(160.dp,260.dp)
        Box(
            Modifier.fillMaxSize()
                .background(t.bgPrimary)
                .layerBackdrop(detailBackdrop, reuseRecordedContent = true, freezeSnapshot = freezeDetailBackdrop)
        ) {
        if (effectiveCover.isNotBlank() && !settings.reduceTransparency) {
            ReadingCoverAtmosphere(effectiveCover, t.bgPrimary, fraction = .55f, modifier=Modifier.graphicsLayer {alpha=if(motion.reduced) .08f else scrollFraction()*.35f})
        }
        if(persistentBanner.isNotBlank() && (!isComic || tab!=3)) ComicArtwork(persistentBanner,Modifier.fillMaxWidth().height(persistentHeroHeight)
            .graphicsLayer {alpha=if(motion.reduced){if(listState.firstVisibleItemIndex>0)0f else 1f}else 1f-scrollFraction();translationY=if(motion.reduced)0f else -listState.firstVisibleItemScrollOffset*.18f}
            .testTag("comic_detail_banner"),fade=true)
        if (isComic && tab == 3) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(top = 68.dp)) {
                DetailTabs(tab, { tab = it })
                CommunityScreen(library, profile, onLogin, comic, listState = communityListState)
            }
        } else LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                bottom = (if (isComic) 112.dp else 32.dp) + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(key = "detail_header") {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val banner = detail?.banner.orEmpty().ifBlank { fallbackBanner }.ifBlank { if (isComic) effectiveCover else "" }
                    val heroHeight = (maxWidth / 1.85f).coerceIn(160.dp,260.dp)
                    Column {
                        Spacer(Modifier.height(if (banner.isNotBlank()) (heroHeight - 54.dp).coerceAtLeast(68.dp) else 68.dp))
                        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                            if (isComic) BookCover(effectiveCover, comic.displayTitle, spine3d = false, modifier = Modifier.width(104.dp).aspectRatio(2f / 3f).onGloballyPositioned {coverBounds=it.boundsInWindow()}.clickable(onClickLabel="Ampliar capa",enabled=effectiveCover.isNotBlank()) {expandedCover=true})
                            else ProfileImage(detail?.image.orEmpty(), detail?.title, Modifier.size(110.dp).clip(ContinuousRoundedRectangle(22.dp)), face = true)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(if (isComic) comic.displayTitle else detail?.title ?: "Carregando…", color = t.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                                val publisher = detail?.publisher.orEmpty().ifBlank { if (isComic) comic.publisher else "" }
                                if (publisher.isNotBlank()) Text(publisher, color = t.textSecondary, fontSize = 14.sp)
                                val authors = comic.authors.ifBlank { detail?.creators.orEmpty().filter { it.role.contains("writer", true) || it.role.contains("artist", true) }.take(3).joinToString(", ") { it.name } }
                                if (isComic && authors.isNotBlank()) Text(authors, color = t.textSecondary, fontSize = 14.sp)
                                if (isComic) {
                                    rating?.takeIf { it.count > 0 }?.let { Text("★ ${String.format(java.util.Locale.getDefault(), "%.1f", it.average)} · ${it.count} avaliações", color = t.textPrimary, fontSize = 13.sp) }

                                }
                            }
                        }
                    }
                }
            }
            if (isComic) item { DetailTabs(tab, { tab = it }) }
            if (isComic && tab == 0) item { ComicReadingSummary(readingPresentation, ::registerCurrent) }
            if (loading) item { LoadingLine(Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = t.accent) }
            if (failed) item { TextButton(onClick = { retry++ }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Tentar carregar informações novamente", color = t.accent) } }
            if (!isComic || tab == 0) {
                val candidateSynopsis = translatedSynopsis.ifBlank {synopsisSource}
                val description = PortugueseComicTranslator.formatCleanSynopsis(candidateSynopsis)
                if (description.isNotBlank()) item {
                    DetailSection(if (isComic) "Sinopse" else "Biografia") {
                        DetailDescription(description, translating, expandedDescription, { expandedDescription = !expandedDescription })
                    }
                }
                item {
                    DetailSection("Detalhes") {
                        val rows = if (isComic) listOf("Publicação" to detail?.date.orEmpty().ifBlank { comic.releaseYear.takeIf { it > 0 }?.toString().orEmpty() },
                            "Formato" to detail?.format.orEmpty().ifBlank { comic.format }, "ISBN" to comic.isbn.ifBlank { detail?.isbn.orEmpty() },
                            "Páginas" to (detail?.pageCount?.takeIf { it > 0 } ?: comic.pageCount).takeIf { it > 0 }?.toString().orEmpty())
                        else listOf("Nome" to detail?.realName.orEmpty(), "Também conhecido como" to detail?.aliases.orEmpty(),
                            "Aparições" to detail?.appearances?.takeIf { it > 0 }?.toString().orEmpty(), "Poderes" to detail?.powers.orEmpty().joinToString(", "))
                        rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(label, color = t.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(.4f))
                                Text(value, color = t.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(.6f))
                            }
                            HorizontalDivider(color = t.divider)
                        }
                    }
                }
                if (isComic) {
                    val editionKey = ListComic.from(comic).key
                    val memberships = curationState.lists.filter { !it.deleted && it.items.any { entry -> entry.key == editionKey } }
                    if (memberships.isNotEmpty()) item {
                        DetailSection("Suas listas") {
                            memberships.forEach { list ->
                                CompactListRow(list, { listToOpen = list })
                            }
                        }
                    }
                }
                if (related.isNotEmpty()) item {
                    DetailSection(if (isComic) "Edições relacionadas" else "Aparições em quadrinhos") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(related, key = { it.comic.externalId.ifBlank { it.comic.id } }) { rec ->
                                val issue = rec.comic
                                Column(Modifier.width(104.dp).clickable { openComic(issue) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    BookCover(issue.coverUrl, issue.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                                    if (isComic && rec.reasonLabel.isNotBlank()) {
                                        Text(rec.reasonLabel, color = t.accent, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(issue.displayTitle, color = t.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                val credits = (if (tab == 1) detail?.characters.orEmpty() else detail?.creators.orEmpty()).distinctBy { it.id }
                if (detail?.creditsScope == "series") item { Text("Esta ficha inclui créditos da série", color = t.textSecondary, modifier = Modifier.padding(horizontal = 20.dp), fontSize = 12.sp) }
                if (detail?.creditsScope == "work") item { Text("Créditos de edição relacionada", color=t.textSecondary,modifier=Modifier.padding(horizontal=20.dp),fontSize=12.sp) }
                itemsIndexed(credits, key = { _, it -> it.id }) { index, credit ->
                    val type = if (tab == 1) "character" else "person"
                    CreditRow(credit, type, autoLoad = index < 6) { openEntity(type, credit.id) }
                }
                if (!loading && credits.isEmpty()) item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text("Ainda não há informações desta edição no catálogo.", color = t.textSecondary)
                        TextButton(onClick = { editionPicker = true }) { Text("Escolher edição", color = t.accent) }
                    }
                }
            }
        }
        } // end detailBackdrop layer
        CompositionLocalProvider(
            LocalLiquidGlassBackdrop provides detailBackdrop,
            LocalLiquidGlassMenuHost provides detailMenuHost
        ) {
            Row(Modifier.align(Alignment.TopStart).fillMaxWidth()
                .drawWithContent {
                    val sf = scrollFraction()
                    if (sf > 0.01f) {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(
                                    t.bgPrimary.copy(alpha = sf * 0.94f),
                                    t.bgPrimary.copy(alpha = sf * 0.62f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    drawContent()
                }
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = 54.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                SettingsBackButton({ goBack() }, collapsed = collapsed, label = if (isComic) "Voltar" else "HQ")
                if (collapsed) Text(if (isComic) comic.displayTitle else detail?.title.orEmpty(), color = t.textPrimary,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                else Spacer(Modifier.weight(1f))
                if (isComic) {
                    LiquidGlassEffectContainer(spacing = 8.dp, backdrop = detailBackdrop) {
                        LiquidMorphingIconButton(
                            icon = LucideIcons.ShareIos,
                            contentDescription = "Compartilhar",
                            onClick = { shareSheetOpen = true },
                            size = 42.dp,
                            slotIndex = 0
                        )
                        LiquidMorphingIconButton(
                            icon = LucideIcons.MoreEllipsis,
                            contentDescription = "Mais opções",
                            menuId = moreMenuId,
                            iconRotationWhenMenuOpen = 0f,
                            size = 42.dp,
                            slotIndex = 1,
                            onAnchorBoundsChanged = {
                                moreMenuBounds = it
                                detailMenuHost.updateAnchorBounds(moreMenuId, it)
                            },
                            onClick = {
                                val menuItems = buildList {
                                    if (activeRecommendation != null) {
                                        add(
                                            LiquidGlassMenuItemData(
                                                label = "Por que esta HQ?",
                                                icon = Icons.Default.AutoAwesome,
                                                onClick = {
                                                    curationViewModel.curation.load()
                                                    whySheetOpen = true
                                                }
                                            )
                                        )
                                    }
                                    add(
                                        LiquidGlassMenuItemData(
                                            label = "Escolher edição",
                                            icon = Icons.Default.CollectionsBookmark,
                                            onClick = { editionPicker = true }
                                        )
                                    )
                                    add(
                                        LiquidGlassMenuItemData(
                                            label = "Adicionar a uma lista",
                                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                            onClick = {
                                                curationViewModel.curation.load()
                                                addToListOpen = true
                                            }
                                        )
                                    )
                                    add(
                                        LiquidGlassMenuItemData(
                                            label = "Compartilhar leitura",
                                            icon = LucideIcons.ShareIos,
                                            onClick = { shareSheetOpen = true }
                                        )
                                    )
                                }
                                if (moreMenuBounds.width > 0 && moreMenuBounds.height > 0) {
                                    detailMenuHost.showMenu(
                                        LiquidGlassMenuRequest(
                                            id = moreMenuId,
                                            anchorBounds = moreMenuBounds,
                                            triggerLabel = "Opções",
                                            triggerIcon = LucideIcons.MoreEllipsis,
                                            items = menuItems,
                                            onDismiss = {},
                                            selectionMenu = false
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
            addedListNotice?.let { targetList ->
                val pillShape = ContinuousCapsule
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 104.dp, start = 20.dp, end = 20.dp)
                        .then(
                            if (settings.reduceTransparency) Modifier.contentMaterialSurface(pillShape)
                            else Modifier.liquidGlass(shape = pillShape, elevation = 12.dp, surfaceAlpha = if (isDark) 0.52f else 0.68f)
                        )
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                        Text(
                            "Adicionada a \"${targetList.name}\"",
                            color = t.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            Modifier
                                .clip(pillShape)
                                .background(t.textPrimary, pillShape)
                                .clickable {
                                    val fresh = curationState.lists.firstOrNull { it.id == targetList.id && !it.deleted } ?: targetList
                                    addedListNotice = null
                                    listToOpen = fresh
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Ver lista", color = t.bgPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            shareNoticeText?.let { noticeMsg ->
                val pillShape = ContinuousCapsule
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 104.dp, start = 20.dp, end = 20.dp)
                        .then(
                            if (settings.reduceTransparency) Modifier.contentMaterialSurface(pillShape)
                            else Modifier.liquidGlass(shape = pillShape, elevation = 12.dp, surfaceAlpha = if (isDark) 0.52f else 0.68f)
                        )
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                        Text(
                            noticeMsg,
                            color = t.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (isComic && tab != 3) {
                val editionKey = remember(comic) { ListComic.from(comic).key }
                val listCount = remember(curationState.lists, editionKey) {
                    curationState.lists.count { !it.deleted && it.items.any { entry -> entry.key == editionKey } }
                }
                val primaryIcon = when (readingPresentation.title) {
                    "Lido" -> Icons.Default.CheckCircle
                    "Lendo" -> Icons.AutoMirrored.Filled.MenuBook
                    "Quero ler" -> Icons.Default.Bookmark
                    else -> Icons.Default.BookmarkBorder
                }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(108.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    t.bgPrimary.copy(alpha = 0.52f),
                                    t.bgPrimary.copy(alpha = 0.86f)
                                )
                            )
                        )
                )
                LiquidGlassEffectContainer(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                    spacing = 10.dp,
                    backdrop = detailBackdrop
                ) {
                    LiquidGlassPillButton(
                        label = readingPresentation.action,
                        icon = primaryIcon,
                        prominent = true,
                        slotIndex = 0,
                        height = 52.dp,
                        modifier = Modifier.weight(1f),
                        onClick = ::registerCurrent
                    )
                    LiquidGlassPillButton(
                        label = if (listCount > 0) "Listas ($listCount)" else "Lista",
                        icon = if (listCount > 0) Icons.Default.Check else Icons.AutoMirrored.Filled.PlaylistAdd,
                        prominent = false,
                        slotIndex = 1,
                        height = 52.dp,
                        accessibilityLabel = "Adicionar a uma lista",
                        onClick = {
                            curationViewModel.curation.load()
                            addToListOpen = true
                        }
                    )
                }
            }
            LiquidGlassMenuOverlayHost(
                detailMenuHost,
                detailBackdrop,
                Modifier.matchParentSize()
            )
            if (shareSheetOpen) {
                val shareSource = (currentReading ?: comic).copy(
                    coverUrl = comic.coverUrl.ifBlank { detail?.image.orEmpty() },
                    pageCount = detail?.pageCount?.takeIf { it > 0 } ?: comic.pageCount
                )
                SocialShareCardSheetModal(
                    request = SocialShareRequest(
                        initialCardType = SocialShareCardType.COMPLETED_READING,
                        comic = shareSource
                    ),
                    allComics = library,
                    profile = profile,
                    appSettings = settings,
                    onShowToast = { msg -> shareNoticeText = msg },
                    onDismiss = { shareSheetOpen = false }
                )
            }
        }
    }
    if(expandedCover) ExpandedComicCover(effectiveCover,comic.displayTitle,coverBounds,inPlace=true) {expandedCover=false}
    }
    if (addToListOpen) {
        AddToListSheet(
            comic = comic,
            controller = curationViewModel.curation,
            library = library,
            searchCatalog = curationViewModel::searchCatalogForList,
            onAddedToList = { added -> addedListNotice = added },
            onClose = { addToListOpen = false }
        )
    }
    if (whySheetOpen && activeRecommendation != null) {
        RecommendationExplanationSheet(
            recommendation = activeRecommendation,
            controller = curationViewModel.curation,
            onClose = { whySheetOpen = false },
            onRegister = {
                whySheetOpen = false
                registerCurrent()
            }
        )
    }
    listToOpen?.let { selected ->
        val list = curationState.lists.firstOrNull { it.id == selected.id && !it.deleted }
        if (list != null) ListDetailSheet(list, { listToOpen = null }, onComic = { next -> listComic = next })
    }
    listComic?.let { selected ->
        ComicDetailDialog(
            selected, profile, library,
            onClose = { listComic = null },
            onRegister = {},
            onCover = onCover,
            onLogin = { listComic = null; listToOpen = null; onLogin() },
            onShare = { onShare() }
        )
    }
    if (editionPicker) EditionChooser(comic.displayTitle, onClose = { editionPicker = false }) { edition ->
        comic = comic.copy(externalId = edition.externalId, sourceLabel = edition.sourceLabel, publisher = edition.publisher.ifBlank { comic.publisher })
        editionPicker = false
    }
    if (registrationSheetOpen) {
        val sheetTarget = currentReading ?: comic.copy(
            coverUrl = comic.coverUrl.ifBlank { detail?.image.orEmpty() },
            pageCount = detail?.pageCount?.takeIf { it > 0 } ?: comic.pageCount,
            isbn = comic.isbn.ifBlank { detail?.isbn.orEmpty() }
        )
        CatalogSheetModal(
            selectedComic = sheetTarget,
            isExistingInShelf = currentReading != null,
            isSheetExpanded = registrationSheetExpanded,
            appSettings = settings,
            allUserComics = library,
            onlineSequencePool = onlineSequence,
            onSelectSequenceComic = { openComic(it) },
            onUpdateComic = {},
            onToggleExpand = { registrationSheetExpanded = !registrationSheetExpanded },
            onSetExpanded = { registrationSheetExpanded = it },
            onSave = { updated ->
                registrationSheetOpen = false
                curationViewModel.saveComicInPlace(updated)
                comic = updated
                onRegister(updated)
            },
            onDelete = { id ->
                registrationSheetOpen = false
                curationViewModel.deleteComicInPlace(id)
            },
            onDismiss = { registrationSheetOpen = false },
            onShareComic = { c, type, mid ->
                curationViewModel.openSocialShareSheet(SocialShareRequest(type, c, initialMomentId = mid))
            },
            readerProfile = profile,
            onLogin = onLogin,
            recommendationContext = activeRecommendation
        )
    }
}

@Composable
private fun DetailDescription(text: String, translating: Boolean, expanded: Boolean, onToggle: () -> Unit) {
    val t = LocalIOS27Theme.current
    var overflowing by remember(text) { mutableStateOf(false) }
    Text(text, color = t.textPrimary, fontSize = 15.sp, lineHeight = 23.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("comic_description"), onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow })
    if (overflowing || expanded) TextButton(onClick = onToggle, modifier = Modifier.heightIn(min = 44.dp).testTag("comic_description_toggle"), contentPadding = PaddingValues(horizontal = 0.dp)) {
        Text(if (expanded) "Menos" else "Mais", color = t.accent)
    }
    if (translating) Text("Atualizando tradução…", color = t.textSecondary, fontSize = 12.sp)
}

@Composable
private fun DetailTabs(tab: Int, onChange: (Int) -> Unit) {
    val t = LocalIOS27Theme.current
    val labels = listOf("Sobre", "Personagens", "Equipe", "Comunidade")
    Box(Modifier.padding(horizontal = 16.dp)) {
        AppleSegmentedPicker(value = tab.toString(), onChange = { onChange(it.toInt()) },
            options = labels.mapIndexed { index, label -> SegmentOption(index.toString(), label) })
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalIOS27Theme.current
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = t.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun CreditRow(credit: CatalogCredit, type: String, autoLoad: Boolean = true, onClick: () -> Unit) {
    val t = LocalIOS27Theme.current
    var image by remember(type, credit.id) { mutableStateOf(CatalogPortraits.cached(type, credit.id).orEmpty()) }
    LaunchedEffect(type, credit.id, autoLoad) {
        if (autoLoad && image.isBlank()) {
            try { image = CatalogPortraits.image(type, credit.id) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Throwable) { }
        }
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(62.dp).clip(ContinuousRoundedRectangle(14.dp)).background(t.bgSecondary), contentAlignment = Alignment.Center) {
            if (image.isNotBlank()) ProfileImage(image, null, Modifier.fillMaxSize(), face = true)
            else Text(credit.name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }, color = t.textSecondary, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(credit.name, color = t.textPrimary)
            if (credit.role.isNotBlank()) Text(ComicDetailPresentation.creditRole(credit.role), color = t.textSecondary, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textTertiary)
    }
}

@Composable
private fun EditionChooser(initialQuery: String, onClose: () -> Unit, onChoose: (ComicEntry) -> Unit) {
    val t = LocalIOS27Theme.current
    val panel = rememberCurationPanelState()
    var query by remember { mutableStateOf(initialQuery) }
    var entries by remember { mutableStateOf(emptyList<ComicEntry>()) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        entries = emptyList(); failed = false; busy = false
        if (query.length < 2) return@LaunchedEffect
        busy = true
        try { kotlinx.coroutines.delay(300); entries = ComicVineCatalog.search(query).items }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { failed = true }
        finally { busy = false }
    }
    CurationSheet("Escolher edição", onClose, panel = panel, compact = true) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp).imePadding(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Escolha a edição correspondente para ver personagens e equipe.", color = t.textSecondary, fontSize = 13.sp) }
            item { AppleTextField(query, { query = it.take(100) }, "Buscar edição") }
            if (busy) item { LoadingLine(Modifier.fillMaxWidth(), color = t.accent) }
            if (failed) item { Text("Não foi possível buscar. Tente outro termo.", color = t.textSecondary) }
            items(entries.distinctBy { it.id }, key = { it.id }) { entry ->
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = panel.request == 0) { panel.close { onChoose(entry) } }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BookCover(entry.coverUrl, entry.displayTitle, spine3d = false, modifier = Modifier.width(48.dp).height(72.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.displayTitle, color = t.textPrimary, fontWeight = FontWeight.Medium)
                        Text(listOfNotNull(entry.issueNumber.takeIf { it > 0 }?.let { "Edição $it" }, entry.releaseYear.takeIf { it > 0 }?.toString()).joinToString(" · "), color = t.textSecondary, fontSize = 12.sp)
                    }
                }
            }
            if (!busy && !failed && entries.isEmpty()) item { Text("Nenhuma edição encontrada. Tente o título original.", color = t.textSecondary) }
        }
    }
}
