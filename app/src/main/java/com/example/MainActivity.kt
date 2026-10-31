package com.example

import com.example.ui.components.dismissFocusOnBackgroundTap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.MultiApiSearchEngine
import com.example.model.AuthActions
import com.example.model.AuthUiState
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import androidx.compose.animation.core.Animatable
import com.example.ui.components.AdaptiveSidebarDrawer
import com.example.ui.components.BookCover
import com.example.ui.components.CatalogSheetModal
import com.example.ui.components.ComicInfoSynopsisSheetModal
import com.example.ui.components.LiquidGlassBackdropScope
import com.example.ui.components.LocalLiquidGlassBackdrop
import com.example.ui.components.LocalLiquidGlassBackdropLayer
import com.example.ui.components.LocalLiquidGlassBackdropOrigin
import com.example.ui.components.LocalLiquidGlassScrollState
import com.example.ui.components.LucideIcons
import com.example.ui.components.ManualEntrySheetModal
import com.example.ui.components.ProgressiveBlurBackdrop
import com.example.ui.components.SculpturalWaveBackdrop
import com.example.ui.components.drawOpticalGlassMeniscus
import com.example.ui.components.inputWellSurface
import com.example.ui.components.interactiveGlassBottomBarSurface
import com.example.ui.components.liquidGlass
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.components.realLiquidGlassBackdrop
import com.example.ui.components.rememberDampedDragAnimation
import com.example.ui.components.rememberInteractiveHighlight
import com.example.ui.components.tintedLiquidBlueButton
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DiaryChronologicalTab
import com.example.ui.screens.HomeReadingNowTab
import com.example.ui.screens.SearchMultiApiTab
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.screens.ShelfLibraryTab
import com.example.ui.screens.TopFloatingToolbar
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.QuadrinheiroTheme
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.example.viewmodel.QuadrinheiroViewModel
import com.kyant.shapes.Capsule
import com.kyant.shapes.ContinuousCapsule
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.example.ui.components.rememberCurrentBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure global Coil ImageLoader lazily off the UI startup path
        val appCtx = applicationContext
        Coil.setImageLoader(
            coil.ImageLoaderFactory {
                ImageLoader.Builder(appCtx)
                    .okHttpClient {
                        MultiApiSearchEngine.initHttpCache(appCtx)
                        MultiApiSearchEngine.sharedHttpClient
                    }
                    .memoryCache {
                        MemoryCache.Builder(appCtx)
                            .maxSizePercent(0.30)
                            .strongReferencesEnabled(true)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(appCtx.cacheDir.resolve("quadrinheiro_covers_disk_cache"))
                            .maxSizeBytes(250L * 1024L * 1024L)
                            .build()
                    }
                    .crossfade(50)
                    .allowHardware(true)
                    .allowRgb565(true)
                    .respectCacheHeaders(false)
                    .build()
            }
        )

        // Let the initial window SurfaceSyncGroup (wmsSync-VRI[MainActivity]#0 & VRI[MainActivity]#1)
        // complete its first frame immediately without blocking RenderThread on 30+ cold AGSL shader
        // compilations, then turn on full offscreen Liquid Glass effects on the very next frame.
        com.kyant.backdrop.BackdropFirstFrameGate.firstFrameReady = false
        window.decorView.post {
            android.view.Choreographer.getInstance().postFrameCallback {
                com.kyant.backdrop.BackdropFirstFrameGate.firstFrameReady = true
            }
        }

        enableEdgeToEdge()
        setContent {
            // APP_BACKGROUND_FOCUS_V5: settings and inline sheets inherit background tap dismissal.
            Box(Modifier.fillMaxSize().dismissFocusOnBackgroundTap()) {
                QuadrinheiroApp()
            }
        }
    }
}

@Composable
fun QuadrinheiroApp(
    viewModel: QuadrinheiroViewModel = viewModel()
) {
    val authScreenUiState by viewModel.authScreenUiState.collectAsState()
    val authUiState = authScreenUiState.sessionState
    val showAuthModalFromSettings = authScreenUiState.showLinkModalFromSettings

    val activeTab by viewModel.activeTab.collectAsState()
    val lastCapsuleIndex by viewModel.lastCapsuleIndex.collectAsState()
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsState()
    val settingsSubPage by viewModel.settingsSubPage.collectAsState()
    val settingsParentPage by viewModel.settingsParentPage.collectAsState()

    val comics by viewModel.comics.collectAsState()
    val storedProfile by viewModel.profile.collectAsState()
    // GUEST_PRESENTATION_V7: presentation only; never overwrite account data.
    val profile = if (authUiState == AuthUiState.Guest) storedProfile.copy(
        name = "Convidado", handle = "", bio = "", favoriteUniverse = "",
        avatarUrl = "", avatarPreset = "theme", showGoalOnHome = false
    ) else storedProfile
    val appSettings by viewModel.appSettings.collectAsState()

    val shelfSection by viewModel.shelfSection.collectAsState()
    val scrollToTopTrigger by viewModel.scrollToTopTrigger.collectAsState()
    val searchFocusTrigger by viewModel.searchFocusTrigger.collectAsState()
    val shelfStatus by viewModel.shelfStatus.collectAsState()
    val shelfGenre by viewModel.shelfGenre.collectAsState()
    val shelfLayout by viewModel.shelfLayout.collectAsState()
    val shelfSort by viewModel.shelfSort.collectAsState()
    val shelfFilterQuery by viewModel.shelfFilterQuery.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val remoteResults by viewModel.remoteResults.collectAsState()
    val isSearchingApi by viewModel.isSearchingApi.collectAsState()
    val searchSourceFilter by viewModel.searchSourceFilter.collectAsState()

    val apiDiagnostics by viewModel.apiDiagnostics.collectAsState()
    val isTestingApis by viewModel.isTestingApis.collectAsState()

    val selectedComic by viewModel.selectedComic.collectAsState()
    val infoModalComic by viewModel.infoModalComic.collectAsState()
    val onlineSequencePool by viewModel.onlineSequencePool.collectAsState()
    val showManualModal by viewModel.showManualModal.collectAsState()
    val isSheetExpanded by viewModel.isSheetExpanded.collectAsState()
    val manualForm by viewModel.manualForm.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val isLoggingOut by viewModel.isLoggingOut.collectAsState()
    val isEnteringApp by viewModel.isEnteringApp.collectAsState()

    // Persistent LazyListState per route key so scroll position is preserved across tabs, library sections & settings sub-menus
    val currentRouteKey = remember(activeTab, settingsSubPage, shelfSection) {
        viewModel.getCurrentRouteKey(activeTab, settingsSubPage, shelfSection)
    }

    val activeListState = remember(currentRouteKey) {
        val saved = viewModel.scrollMemory[currentRouteKey]
        LazyListState(
            firstVisibleItemIndex = saved?.first ?: 0,
            firstVisibleItemScrollOffset = saved?.second ?: 0
        )
    }

    LaunchedEffect(currentRouteKey, activeListState) {
        androidx.compose.runtime.snapshotFlow {
            activeListState.isScrollInProgress
        }.collect { inProgress ->
            if (!inProgress) {
                viewModel.scrollMemory[currentRouteKey] =
                    activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
            }
        }
    }

    DisposableEffect(currentRouteKey, activeListState) {
        onDispose {
            viewModel.scrollMemory[currentRouteKey] =
                activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
        }
    }

    val context = LocalContext.current

    var isTabBarCollapsed by remember { mutableStateOf(false) }
    LaunchedEffect(appSettings.minimizeTabBarOnScroll) {
        if (!appSettings.minimizeTabBarOnScroll) {
            isTabBarCollapsed = false
        }
    }
    val tabBarScrollConnection = remember(appSettings.minimizeTabBarOnScroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!appSettings.minimizeTabBarOnScroll) {
                    if (isTabBarCollapsed) isTabBarCollapsed = false
                    return Offset.Zero
                }
                if (available.y < -8f && !isTabBarCollapsed) {
                    isTabBarCollapsed = true
                } else if (available.y > 8f && isTabBarCollapsed) {
                    isTabBarCollapsed = false
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0L) {
            isTabBarCollapsed = false
            val isAtTop = activeListState.firstVisibleItemIndex == 0 &&
                activeListState.firstVisibleItemScrollOffset <= 24
            if (activeTab == "search" && isAtTop) {
                viewModel.requestSearchFocus()
            } else {
                activeListState.animateScrollToItem(0, 0)
            }
        }
    }

    BackHandler(
        enabled = selectedComic != null ||
            infoModalComic != null ||
            showManualModal ||
            isSidebarOpen ||
            (activeTab != "home" && !(activeTab == "settings" && settingsSubPage != null))
    ) {
        when {
            selectedComic != null -> viewModel.selectComicForSheet(null)
            infoModalComic != null -> viewModel.openInfoSynopsisModal(null)
            showManualModal -> viewModel.setShowManualModal(false)
            isSidebarOpen -> viewModel.setSidebarOpen(false)
            activeTab == "shelf" && shelfSection != "shelf" -> viewModel.setShelfSection("shelf")
            else -> viewModel.switchMainTab("home")
        }
    }

    val activeReadingComicId by viewModel.activeReadingComicId.collectAsState()
    val readingNowList = remember(comics) { comics.filter { it.status == ReadingStatus.READING } }
    val readComicsList = remember(comics) { comics.filter { it.status == ReadingStatus.READ } }
    val activeReadingIndex = remember(readingNowList, activeReadingComicId) {
        if (readingNowList.isEmpty()) 0
        else readingNowList.indexOfFirst {
            it.id == activeReadingComicId || (it.externalId.isNotBlank() && it.externalId == activeReadingComicId)
        }.let { if (it >= 0) it else 0 }
    }
    val featuredReadingItem = readingNowList.getOrNull(activeReadingIndex)
    val totalPagesRead = remember(comics) {
        comics.sumOf {
            when (it.status) {
                ReadingStatus.READ -> it.pageCount
                ReadingStatus.READING -> it.currentPage
                ReadingStatus.WANT_TO_READ -> 0
            }
        }
    }

    QuadrinheiroTheme(settings = appSettings) {
        if (isLoggingOut) {
            SessionTransitionScreen("Saindo da conta…")
            return@QuadrinheiroTheme
        }
        if (isEnteringApp && authUiState != AuthUiState.Unauthenticated) {
            SessionTransitionScreen("Preparando sua coleção…")
            return@QuadrinheiroTheme
        }

        // ROTEAMENTO CONDICIONAL SÍNCRONO NO BOOT (SEM FLASH DE TELA):
        // Se o usuário ainda não tiver sessão ativa nem tiver escolhido o Modo Convidado,
        // renderiza exclusivamente a AuthScreen edge-to-edge.
        if (authUiState == AuthUiState.Unauthenticated) {
            androidx.compose.animation.AnimatedVisibility(
                visibleState = remember {
                    androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
                },
                enter = fadeIn(tween(450))
            ) {
            AuthScreen(
                uiState = authScreenUiState,
                isLinkingFromGuest = false,
                actions = AuthActions(
                    onClearFeedback = viewModel::clearAuthFeedback,
                    onLoginWithEmail = viewModel::loginWithEmail,
                    onRegisterWithEmail = viewModel::registerWithEmail,
                    onLoginWithGoogle = { viewModel.loginWithGoogle(context) },
                    onContinueAsGuest = viewModel::enterAsGuest,
                    onResetPassword = viewModel::resetPassword,
                    onDismissModal = null
                )
            )
            }
            return@QuadrinheiroTheme
        }

        val mat = LocalAppleMaterials.current
        val s = mat.scheme
        val themeColors = LocalIOS27Theme.current
        val isDark = !s.isLight
        // SINGLE_PASS_APP_BACKDROPS_V16: retain all rendering recipes; reuse recorded content.
        val backgroundBackdrop = rememberLayerBackdrop()
        val mainContentBackdrop = rememberLayerBackdrop()

        // HQ com status "Lendo" ativa. Se não houver nenhuma HQ com status READING, currentlyReadingComic é null.
        val currentlyReadingComic = remember(readingNowList, activeReadingComicId) {
            if (readingNowList.isEmpty()) null
            else {
                val idx = readingNowList.indexOfFirst {
                    it.id == activeReadingComicId || (it.externalId.isNotBlank() && it.externalId == activeReadingComicId)
                }
                if (idx >= 0) readingNowList[idx] else readingNowList.first()
            }
        }

        var isFirstFrameRendered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            androidx.compose.runtime.withFrameNanos { }
            delay(120)
            isFirstFrameRendered = true
        }

        // Cache em memória da cor dominante vibrante extraída diretamente do bitmap real da capa
        val extractedCoverColors = remember { androidx.compose.runtime.mutableStateMapOf<String, Color>() }
        val activeCoverUrl = currentlyReadingComic?.coverUrl?.trim().orEmpty()
        LaunchedEffect(isFirstFrameRendered, activeCoverUrl, currentlyReadingComic?.title) {
            if (!isFirstFrameRendered) return@LaunchedEffect
            val comic = currentlyReadingComic ?: return@LaunchedEffect
            val rawUrl = comic.coverUrl.trim().replace("http://", "https://").let { u ->
                if (u.isEmpty() || u.contains("covers.openlibrary.org")) {
                    com.example.data.CuratedCatalog.findCoverUrlByTitle(comic.title) ?: u
                } else u
            }
            if (rawUrl.isNotEmpty() && !extractedCoverColors.containsKey(rawUrl)) {
                val color = com.example.ui.components.AmbientImageProcessor.processAmbientCoverColor(
                    context = context,
                    rawUrl = rawUrl,
                    dispatcher = kotlinx.coroutines.Dispatchers.Default
                )
                if (color != null) {
                    extractedCoverColors[rawUrl] = color
                }
            }
        }

        // Cor de luz de fundo: segue o tema ativo se não houver HQ com status "Lendo".
        // Quando há uma HQ com status READING ("Lendo"), usa a cor vibrante extraída da capa (ou fallback fiel à capa)!
        val resolvedCoverUrl = remember(currentlyReadingComic) {
            val comic = currentlyReadingComic ?: return@remember ""
            val raw = comic.coverUrl.trim().replace("http://", "https://")
            if (raw.isEmpty() || raw.contains("covers.openlibrary.org")) {
                com.example.data.CuratedCatalog.findCoverUrlByTitle(comic.title) ?: raw
            } else raw
        }
        val sampledCoverColor = if (resolvedCoverUrl.isNotEmpty()) extractedCoverColors[resolvedCoverUrl] else null

        val backgroundGlowColor = remember(
            currentlyReadingComic,
            sampledCoverColor,
            themeColors.accent,
            themeColors.accentSecondary,
            isDark
        ) {
            if (currentlyReadingComic == null) {
                if (isDark) themeColors.accentSecondary else themeColors.accent
            } else if (sampledCoverColor != null) {
                sampledCoverColor
            } else {
                when {
                    currentlyReadingComic.title.contains("Ano Um", ignoreCase = true) ||
                        currentlyReadingComic.title.contains("Year One", ignoreCase = true) ||
                        currentlyReadingComic.title.contains("Batman", ignoreCase = true) -> Color(0xFFE11D48) // Vermelho Carmim da capa
                    currentlyReadingComic.title.contains("Watchmen", ignoreCase = true) -> Color(0xFFF5C518)
                    currentlyReadingComic.title.contains("Sandman", ignoreCase = true) -> Color(0xFFD97706)
                    currentlyReadingComic.title.contains("Berserk", ignoreCase = true) -> Color(0xFFDC2626)
                    currentlyReadingComic.title.contains("Persepolis", ignoreCase = true) ||
                        currentlyReadingComic.title.contains("Study Guide", ignoreCase = true) -> Color(0xFF14B8A6)
                    currentlyReadingComic.title.contains("X-Men", ignoreCase = true) -> Color(0xFFEA580C)
                    currentlyReadingComic.title.contains("Saga", ignoreCase = true) -> Color(0xFFEA580C)
                    currentlyReadingComic.title.contains("Akira", ignoreCase = true) ||
                        currentlyReadingComic.title.contains("Spider", ignoreCase = true) ||
                        currentlyReadingComic.title.contains("Aranha", ignoreCase = true) ||
                        currentlyReadingComic.title.contains("Demolidor", ignoreCase = true) ||
                        currentlyReadingComic.title.contains("Daredevil", ignoreCase = true) -> Color(0xFFEF4444)
                    currentlyReadingComic.genre.contains("Manga", ignoreCase = true) ||
                        currentlyReadingComic.genre.contains("Mangá", ignoreCase = true) -> Color(0xFFEC4899)
                    currentlyReadingComic.genre.contains("Sci-Fi", ignoreCase = true) -> Color(0xFF06B6D4)
                    currentlyReadingComic.publisher.contains("Marvel", ignoreCase = true) -> Color(0xFFEF4444)
                    currentlyReadingComic.publisher.contains("DC", ignoreCase = true) -> Color(0xFFE11D48)
                    else -> {
                        val hash = kotlin.math.abs(currentlyReadingComic.title.hashCode())
                        val palette = listOf(
                            Color(0xFFE11D48), Color(0xFFF5C518), Color(0xFF8B5CF6),
                            Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFFF97316)
                        )
                        palette[hash % palette.size]
                    }
                }
            }
        }
        val animatedGlowColor by animateColorAsState(
            targetValue = backgroundGlowColor,
            animationSpec = tween(550),
            label = "coverColorGlow"
        )
        val animatedAccentColor by animateColorAsState(
            targetValue = themeColors.accent,
            animationSpec = tween(550),
            label = "accentColorGlow"
        )

        val menuHostState = remember { com.example.ui.components.LiquidGlassMenuHostState() }
        LaunchedEffect(activeTab, settingsSubPage, shelfSection) {
            menuHostState.dismissMenu()
        }

        CompositionLocalProvider(
            LocalLiquidGlassBackdrop provides backgroundBackdrop,
            LocalLiquidGlassScrollState provides activeListState,
            com.example.ui.components.LocalLiquidGlassMenuHost provides menuHostState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(tabBarScrollConnection)
            ) {
                // 1. CAMADA GRAVADA NO mainContentBackdrop (Fundo + Telas)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(mainContentBackdrop, reuseRecordedContent = true)
                ) {
                    // 1.1 Iluminação de Fundo gravada no backgroundBackdrop:
                    // Usa a PRÓPRIA IMAGEM DA CAPA da HQ em leitura com desfoque atmosférico e efeitos (igual ao iOS / Apple Books)
                    val hasReadingCoverBg = isFirstFrameRendered &&
                        currentlyReadingComic != null &&
                        appSettings.backgroundExtension &&
                        resolvedCoverUrl.isNotEmpty() &&
                        !themeColors.isAllBlack

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backgroundBackdrop, reuseRecordedContent = true)
                            .background(themeColors.bgPrimary)
                    ) {
                        // A. Gradiente escultural base do tema + luz ambiente suave
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(themeColors.bgPrimary)

                            if (!themeColors.isAllBlack) {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(s.gradLinearStart, s.gradLinearMid, s.gradLinearEnd),
                                        start = Offset.Zero,
                                        end = Offset(size.width, size.height)
                                    ),
                                    alpha = if (hasReadingCoverBg) 0.35f else 0.85f
                                )
                            }

                            val globalAlphaMultiplier = if (themeColors.isAllBlack) 0.05f else 1.0f

                            if (appSettings.backgroundExtension) {
                                val topGlowAlpha = if (currentlyReadingComic != null) {
                                    0.24f * globalAlphaMultiplier
                                } else {
                                    (if (isDark) 0.14f else 0.09f) * globalAlphaMultiplier
                                }
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            animatedGlowColor.copy(alpha = topGlowAlpha),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.32f, size.height * 0.22f),
                                        radius = size.maxDimension * 0.82f
                                    )
                                )
                            }

                            val bottomGlowColor = if (currentlyReadingComic != null && appSettings.backgroundExtension) {
                                animatedGlowColor
                            } else {
                                animatedAccentColor
                            }
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        bottomGlowColor.copy(alpha = (if (isDark) 0.18f else 0.12f) * globalAlphaMultiplier),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.90f),
                                    radius = size.maxDimension * 0.68f
                                )
                            )
                        }

                        // B. A própria imagem da HQ em leitura como iluminação difusa perceptível e elegante
                        // Desfoque orgânico de 14.dp + opacidade calibrada (32% em ambos os modos claro e escuro) para iluminação ambiente viva e equilibrada
                        if (hasReadingCoverBg) {
                            val bgCoverRequest = remember(resolvedCoverUrl, context) {
                                ImageRequest.Builder(context)
                                    .data(resolvedCoverUrl)
                                    .size(28, 42)
                                    .precision(coil.size.Precision.INEXACT)
                                    .crossfade(false)
                                    .allowHardware(true)
                                    .allowRgb565(true)
                                    .memoryCacheKey(resolvedCoverUrl + "_ambient")
                                    .diskCacheKey(resolvedCoverUrl + "_ambient")
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }

                            // Atmosfera difusa perceptível de tela inteira (iluminação cromática da capa em 32% no claro e no escuro)
                            AsyncImage(
                                model = bgCoverRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 1.14f
                                        scaleY = 1.14f
                                    }
                                    /* LAG_FIX_NO_FULLSCREEN_BLUR: 28x42 source is already soft when upscaled */
                                    .alpha(0.32f)
                            )

                            // Véu de vinheta atmosférica mais translúcido no topo (24% em ambos os modos claro e escuro) para deixar a capa brilhar sem perder legibilidade
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                themeColors.bgPrimary.copy(alpha = 0.24f),
                                                themeColors.bgPrimary.copy(alpha = if (isDark) 0.52f else 0.44f),
                                                themeColors.bgPrimary.copy(alpha = if (isDark) 0.82f else 0.74f)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // 1.2 Telas Principais (Home, Shelf, Diary, Settings, Search)
                    // NUNCA chamam drawBackdrop(pagerBackdrop); usam backgroundBackdrop (irmão já gravado!).
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 600.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            androidx.compose.runtime.key(currentRouteKey) {
                                when (activeTab) {
                                    "home" -> HomeReadingNowTab(
                                        comics = comics,
                                        profile = profile,
                                        appSettings = appSettings,
                                        listState = activeListState,
                                        onOpenSidebar = { viewModel.setSidebarOpen(true) },
                                        onQuickRegister = { viewModel.switchMainTab("search") },
                                        onOpenProfileSettings = {
                                            viewModel.scrollMemory[currentRouteKey] =
                                                activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
                                            viewModel.switchMainTab("settings")
                                            viewModel.openSettingsCategory("profile")
                                        },
                                        onGoToSearch = { viewModel.switchMainTab("search") },
                                        onGoToShelf = { viewModel.switchMainTab("shelf") },
                                        onSelectComic = { viewModel.selectComicForSheet(it) },
                                        onQuickUpdateReadingPage = viewModel::quickUpdateReadingPage,
                                        onToggleBottomAccessory = { enabled ->
                                            viewModel.updateSettings { it.copy(showBottomAccessory = enabled) }
                                        }
                                    )

                                    "shelf" -> ShelfLibraryTab(
                                        comics = comics,
                                        appSettings = appSettings,
                                        shelfSection = shelfSection,
                                        shelfStatus = shelfStatus,
                                        shelfGenre = shelfGenre,
                                        shelfLayout = shelfLayout,
                                        shelfSort = shelfSort,
                                        shelfFilterQuery = shelfFilterQuery,
                                        listState = activeListState,
                                        onOpenSidebar = { viewModel.setSidebarOpen(true) },
                                        onQuickRegister = { viewModel.switchMainTab("search") },
                                        onChangeShelfSection = { nextSection ->
                                            viewModel.scrollMemory[currentRouteKey] =
                                                activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
                                            viewModel.setShelfSection(nextSection)
                                        },
                                        onChangeShelfStatus = viewModel::setShelfStatus,
                                        onChangeShelfGenre = viewModel::setShelfGenre,
                                        onToggleShelfLayout = viewModel::toggleShelfLayout,
                                        onChangeShelfLayout = viewModel::setShelfLayout,
                                        onChangeShelfSort = viewModel::setShelfSort,
                                        onChangeShelfFilterQuery = viewModel::setShelfFilterQuery,
                                        onGoToSearch = { viewModel.switchMainTab("search") },
                                        onSelectComic = { viewModel.selectComicForSheet(it) }
                                    )

                                    "diary" -> DiaryChronologicalTab(
                                        comics = comics,
                                        appSettings = appSettings,
                                        listState = activeListState,
                                        onOpenSidebar = { viewModel.setSidebarOpen(true) },
                                        onQuickRegister = { viewModel.switchMainTab("search") },
                                        onSelectComic = { viewModel.selectComicForSheet(it) }
                                    )

                                    "settings" -> SettingsTabScreen(
                                        viewModel = viewModel,
                                        comics = comics,
                                        profile = profile,
                                        appSettings = appSettings,
                                        settingsSubPage = settingsSubPage,
                                        settingsParentPage = settingsParentPage,
                                        apiDiagnostics = apiDiagnostics,
                                        isTestingApis = isTestingApis,
                                        listState = activeListState,
                                        onOpenSidebar = { viewModel.setSidebarOpen(true) },
                                        onQuickRegister = { viewModel.switchMainTab("search") }
                                    )

                                    "search" -> SearchMultiApiTab(
                                        comics = comics,
                                        appSettings = appSettings,
                                        searchQuery = searchQuery,
                                        remoteResults = remoteResults,
                                        isSearchingApi = isSearchingApi,
                                        searchSourceFilter = searchSourceFilter,
                                        listState = activeListState,
                                        searchFocusTrigger = searchFocusTrigger,
                                        onOpenSidebar = { viewModel.setSidebarOpen(true) },
                                        onQuickRegister = { viewModel.switchMainTab("search") },
                                        onChangeSearchQuery = viewModel::setSearchQuery,
                                        onTriggerImmediateSearch = viewModel::triggerImmediateSearch,
                                        onChangeSearchSourceFilter = viewModel::setSearchSourceFilter,
                                        onOpenManualModal = { prefillTitle ->
                                            viewModel.updateManualForm { it.copy(title = prefillTitle) }
                                            viewModel.setShowManualModal(true)
                                        },
                                        onOpenCatalogSheetForComic = viewModel::openCatalogSheetForComic,
                                        onSelectSavedComic = { viewModel.selectComicForSheet(it) },
                                        onOpenComicInfoModal = { viewModel.openInfoSynopsisModal(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. CAMADAS DE VIDRO FLUTUANTES (IRMÃOS EXTERNOS FORA DO mainContentBackdrop, usam mainContentBackdrop):
                CompositionLocalProvider(
                    LocalLiquidGlassBackdrop provides mainContentBackdrop
                ) {
                    if (activeTab != "settings" && appSettings.showFloatingMenu) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 16.dp, top = 12.dp)
                                        .zIndex(5f)
                                ) {
                                    TopFloatingToolbar(
                                        hasComics = comics.isNotEmpty(),
                                        onOpenSidebar = { viewModel.setSidebarOpen(true) },
                                        onQuickRegister = { viewModel.switchMainTab("search") },
                                        visible = true
                                    )
                                }
                            }
                        }
                    }

                    // Mini Menu Flutuante de Leitura ("Continuar Lendo") + Tab Bar
                    IOS27BottomTabViewContainer(
                    showBottomAccessory = appSettings.showBottomAccessory,
                    featuredReadingItem = featuredReadingItem,
                    readingNowCount = readingNowList.size,
                    activeReadingIndex = activeReadingIndex,
                    onCycleReadingComic = viewModel::cycleActiveReadingComic,
                    onOpenReadingItem = { viewModel.selectComicForSheet(it) },
                    activeTab = activeTab,
                    onSelectTab = { nextTab ->
                        viewModel.scrollMemory[currentRouteKey] =
                            activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
                        viewModel.switchMainTab(nextTab)
                    },
                    isTabBarCollapsed = isTabBarCollapsed,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 16.dp),
                    backdrop = mainContentBackdrop
                )

                // Floating Toast Notice
                AnimatedVisibility(
                    visible = toast != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 96.dp, start = 20.dp, end = 20.dp)
                ) {
                    toast?.let { currentToast ->
                        Box(
                            modifier = Modifier
                                .shadow(14.dp, RoundedCornerShape(999.dp))
                                .liquidGlass(shape = RoundedCornerShape(999.dp), elevation = 14.dp)
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.30f),
                                            Color.Transparent
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    ),
                                    shape = RoundedCornerShape(999.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = currentToast.message,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (currentToast.actionLabel != null && currentToast.onAction != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(s.accentBg)
                                            .clickable { currentToast.onAction.invoke() }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = currentToast.actionLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = s.accentText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Adaptive Sidebar Drawer (NavigationSplitView / sidebarAdaptable)
                if (isSidebarOpen) {
                    AdaptiveSidebarDrawer(
                        profile = profile,
                        activeTab = if (activeTab == "shelf" && shelfSection == "diary") "diary" else activeTab,
                        readingNowCount = readingNowList.size,
                        totalComicsCount = comics.size,
                        readComicsCount = readComicsList.size,
                        totalPagesRead = totalPagesRead,
                        shelfGenre = shelfGenre,
                        glassLevel = appSettings.glassLevel,
                        onSelectTab = { nextTab ->
                            viewModel.scrollMemory[currentRouteKey] =
                                activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
                            if (nextTab == "shelf") {
                                viewModel.setShelfSection("shelf")
                            }
                            viewModel.switchMainTab(nextTab)
                        },
                        onSelectShelfGenre = viewModel::setShelfGenre,
                        onOpenLiquidGlassSettings = {
                            viewModel.scrollMemory[currentRouteKey] =
                                activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
                            viewModel.switchMainTab("settings")
                            viewModel.openSettingsCategory("liquidGlass")
                        },
                        onDismiss = { viewModel.setSidebarOpen(false) }
                    )
                }

                // Modal de Informações e Sinopse (ao clicar em qualquer HQ na aba Buscar)
                infoModalComic?.let { currentInfoComic ->
                    val savedMatch = remember(currentInfoComic.externalId, currentInfoComic.id, comics) {
                        comics.firstOrNull {
                            (currentInfoComic.externalId.isNotBlank() && it.externalId == currentInfoComic.externalId) ||
                                (currentInfoComic.id.isNotBlank() && it.id == currentInfoComic.id)
                        }
                    }
                    ComicInfoSynopsisSheetModal(
                        comic = currentInfoComic,
                        savedEntry = savedMatch,
                        appSettings = appSettings,
                        allUserComics = comics,
                        onlineSequencePool = onlineSequencePool,
                        onSelectSequenceComic = { seqComic ->
                            viewModel.openInfoSynopsisModal(seqComic)
                        },
                        onOpenCatalogWithStatus = { comic, status ->
                            viewModel.openCatalogSheetForComic(comic, status)
                        },
                        onEditSavedComic = { saved ->
                            viewModel.selectComicForSheet(saved)
                        },
                        onDismiss = { viewModel.openInfoSynopsisModal(null) }
                    )
                }

                // Cataloging & Review Sheet Modal
                selectedComic?.let { currentComic ->
                    val isExisting = remember(currentComic.id, comics) {
                        comics.any { it.id == currentComic.id }
                    }
                    CatalogSheetModal(
                        selectedComic = currentComic,
                        isExistingInShelf = isExisting,
                        isSheetExpanded = isSheetExpanded,
                        appSettings = appSettings,
                        allUserComics = comics,
                        onlineSequencePool = onlineSequencePool,
                        onSelectSequenceComic = { seqComic ->
                            val existingMatch = comics.firstOrNull {
                                (seqComic.externalId.isNotBlank() && it.externalId == seqComic.externalId) ||
                                    (seqComic.id.isNotBlank() && it.id == seqComic.id)
                            }
                            if (existingMatch != null) {
                                viewModel.selectComicForSheet(existingMatch)
                            } else {
                                viewModel.openInfoSynopsisModal(seqComic)
                            }
                        },
                        onUpdateComic = viewModel::updateSelectedComic,
                        onToggleExpand = { viewModel.setSheetExpanded(!isSheetExpanded) },
                        onSetExpanded = { expanded -> viewModel.setSheetExpanded(expanded) },
                        onSave = viewModel::saveComic,
                        onDelete = viewModel::deleteComic,
                        onDismiss = { viewModel.selectComicForSheet(null) }
                    )
                }

                // Manual Entry Modal
                if (showManualModal) {
                    ManualEntrySheetModal(
                        form = manualForm,
                        onUpdateForm = viewModel::updateManualForm,
                        onSave = {
                            if (manualForm.title.isBlank()) {
                                viewModel.showNotice("Informe o título da edição")
                            } else {
                                viewModel.saveComic(
                                    ComicEntry(
                                        id = "manual-${System.currentTimeMillis()}",
                                        externalId = "manual-${System.currentTimeMillis()}",
                                        title = manualForm.title,
                                        authors = manualForm.authors,
                                        publisher = manualForm.publisher,
                                        genre = manualForm.genre,
                                        format = manualForm.format,
                                        releaseYear = manualForm.releaseYear,
                                        pageCount = manualForm.pageCount,
                                        currentPage = manualForm.pageCount,
                                        status = ReadingStatus.READ,
                                        rating = manualForm.rating,
                                        tags = manualForm.tags,
                                        review = manualForm.review,
                                        coverUrl = manualForm.coverUrl
                                    )
                                )
                                viewModel.updateManualForm {
                                    it.copy(
                                        title = "",
                                        authors = "",
                                        publisher = "",
                                        coverUrl = "",
                                        rating = 0f,
                                        tags = emptyList(),
                                        review = ""
                                    )
                                }
                            }
                        },
                        onDismiss = { viewModel.setShowManualModal(false) }
                    )
                }

                // Host Flutuante de Menus Liquid Glass (ex.: "Mais Recentes" na Biblioteca e seletores de Ajustes)
                com.example.ui.components.LiquidGlassMenuOverlayHost(
                    state = menuHostState,
                    backdrop = mainContentBackdrop
                )

                // Modal de Autenticação (quando aberto a partir da tela de Ajustes no Modo Convidado para vincular conta)
                AnimatedVisibility(
                    visible = showAuthModalFromSettings,
                    enter = fadeIn(tween(260)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(50f)
                ) {
                    BackHandler(enabled = showAuthModalFromSettings) {
                        viewModel.dismissAuthModalForLinking()
                    }
                    AuthScreen(
                        uiState = authScreenUiState,
                        isLinkingFromGuest = authUiState == AuthUiState.Guest,
                        actions = AuthActions(
                            onClearFeedback = viewModel::clearAuthFeedback,
                            onLoginWithEmail = viewModel::loginWithEmail,
                            onRegisterWithEmail = viewModel::registerWithEmail,
                            onLoginWithGoogle = { viewModel.loginWithGoogle(context) },
                            onContinueAsGuest = viewModel::dismissAuthModalForLinking,
                            onResetPassword = viewModel::resetPassword,
                            onDismissModal = viewModel::dismissAuthModalForLinking
                        )
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun IOS27BottomTabViewContainer(
    showBottomAccessory: Boolean,
    featuredReadingItem: ComicEntry?,
    readingNowCount: Int,
    activeReadingIndex: Int,
    onCycleReadingComic: (Int) -> Unit,
    onOpenReadingItem: (ComicEntry) -> Unit,
    activeTab: String = "home",
    onSelectTab: (String) -> Unit = {},
    isTabBarCollapsed: Boolean = false,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = rememberLayerBackdrop()
) {
    val collapsedFraction by animateFloatAsState(
        targetValue = if (isTabBarCollapsed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.94f,
            stiffness = 360f,
            visibilityThreshold = 0.0005f
        ),
        label = "TabBarCollapseFraction"
    )
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val isLightTheme = s.isLight

    val settings = com.example.ui.theme.LocalAppSettings.current

    val tabContentColor = if (isLightTheme) Color(0xFF1A1A1A) else Color.White
    val effectiveAccentColor = if (isLightTheme && s.accentColor == Color.White) Color(0xFF3A3A3C) else s.accentColor
    val configuredGlassAlpha = (0.4f * settings.glassOpacity).coerceIn(0.15f, 0.70f)
    val glassContainerColor = if (isLightTheme) {
        Color(0xFFFAFAFA).copy(alpha = configuredGlassAlpha)
    } else {
        Color(0xFF121212).copy(alpha = configuredGlassAlpha)
    }
    val configuredBlurRadius = if (settings.reduceTransparency) 20.dp else (8f * settings.glassBlurIntensity).coerceIn(2f, 28f).dp
    val configuredRefractionMultiplier = if (!settings.reduceTransparency && settings.enableLensRefraction) settings.glassIntensity.coerceIn(0.2f, 2.5f) else 0f
    val configuredChromaticAberration = !settings.reduceTransparency && settings.enableChromaticAberration
    val configuredDepthEffect = !settings.reduceTransparency && settings.edgeRefraction

    val baseHighlightAlpha = if (isLightTheme) 0.5f else 0.35f
    val effectiveGlassBlur = configuredBlurRadius.coerceIn(2f.dp, 24f.dp)
    val effectiveGlassRefraction = if (configuredRefractionMultiplier > 0f) {
        configuredRefractionMultiplier.coerceIn(0.25f, 2.2f)
    } else {
        0f
    }

    val mainBackdrop = backdrop

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini Player "Lendo Agora" — Estilo Apple Music (Bottom Accessory) com Aberração Cromática e Refração reais
        AnimatedVisibility(
            visible = showBottomAccessory && featuredReadingItem != null && !isTabBarCollapsed,
            modifier = Modifier.padding(bottom = 8.dp),
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 } + expandVertically(tween(240), expandFrom = Alignment.Top),
            exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 2 } + shrinkVertically(tween(200), shrinkTowards = Alignment.Top)
        ) {
            featuredReadingItem?.let { item ->
                val progressFraction by animateFloatAsState(
                    targetValue = (item.currentPage.toFloat() / item.pageCount.coerceAtLeast(1)).coerceIn(0f, 1f),
                    animationSpec = spring(stiffness = 380f, dampingRatio = 0.85f),
                    label = "miniPlayerProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp)
                        .drawBackdrop(
                            backdrop = mainBackdrop,
                            shape = { ContinuousCapsule },
                            highlight = {
                                Highlight.Default.copy(alpha = baseHighlightAlpha)
                            },
                            effects = {
                                if (!settings.reduceTransparency) {
                                    vibrancy()
                                }
                                blur(effectiveGlassBlur.toPx())
                                if (effectiveGlassRefraction > 0f) {
                                    lens(
                                        refractionHeight = 24f.dp.toPx() * effectiveGlassRefraction,
                                        refractionAmount = 34f.dp.toPx() * effectiveGlassRefraction,
                                        depthEffect = configuredDepthEffect,
                                        chromaticAberration = configuredChromaticAberration
                                    )
                                }
                            },
                            onDrawSurface = { drawRect(glassContainerColor) }
                        )
                        .clip(ContinuousCapsule)
                        .clickable { onOpenReadingItem(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mini Foto Quadrada 32.dp x 32.dp (Capa Álbum Apple Music)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(7.dp))
                        ) {
                            com.example.ui.components.BookCover(
                                src = item.coverUrl,
                                title = item.title,
                                authors = item.authors,
                                publisher = item.publisher,
                                spine3d = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Textos (Título + Progresso pág. X/Y)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 13.5.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLightTheme) Color(0xFF111111) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "pág. ${item.currentPage}/${item.pageCount}",
                                fontSize = 11.5.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isLightTheme) Color(0xFF111111).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Barrinha de Progresso Horizontal em Formato de Cápsula
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(4.5.dp)
                                .clip(Capsule())
                                .background(if (isLightTheme) Color.Black.copy(0.12f) else Color.White.copy(0.18f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .clip(Capsule())
                                    .background(s.accentColor)
                            )
                        }

                        // Alternador de HQs em Leitura
                        if (readingNowCount > 1) {
                            Row(
                                modifier = Modifier
                                    .height(26.dp)
                                    .clip(Capsule())
                                    .background(if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable { onCycleReadingComic(-1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "HQ anterior",
                                        tint = if (isLightTheme) Color(0xFF111111) else Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "/",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLightTheme) Color(0xFF111111).copy(0.7f) else Color.White.copy(0.7f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable { onCycleReadingComic(1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Próxima HQ",
                                        tint = if (isLightTheme) Color(0xFF111111) else Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val selectedTabIndex = when (activeTab) {
            "shelf", "diary" -> 1
            "settings" -> 2
            "search" -> -1
            else -> 0
        }
        val isSearchSelected = activeTab == "search"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = lerp(16.dp, 28.dp, collapsedFraction)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidBottomTabs(
                selectedTabIndex = { selectedTabIndex },
                onTabSelected = { index ->
                    when (index) {
                        0 -> onSelectTab("home")
                        1 -> onSelectTab("shelf")
                        2 -> onSelectTab("settings")
                    }
                },
                backdrop = mainBackdrop,
                tabsCount = 3,
                collapsedFraction = { collapsedFraction },
                isLightTheme = isLightTheme,
                accentColor = effectiveAccentColor,
                unselectedContentColor = tabContentColor,
                containerColor = glassContainerColor,
                blurRadius = configuredBlurRadius,
                refractionMultiplier = configuredRefractionMultiplier,
                chromaticAberrationEnabled = configuredChromaticAberration,
                depthEffectEnabled = configuredDepthEffect,
                vibrancyEnabled = !settings.reduceTransparency,
                modifier = Modifier.weight(1f)
            ) {
                val tabLayerColor = com.kyant.backdrop.catalog.components.LocalLiquidBottomTabColor.current ?: tabContentColor

                // Tab 0: Lendo (transição 100% em sub-pixels na GPU sem AnimatedVisibility que travava o layout)
                LiquidBottomTab(onClick = { onSelectTab("home") }) {
                    Icon(
                        imageVector = if (selectedTabIndex == 0) LucideIcons.OpenBookFilled else LucideIcons.OpenBook,
                        contentDescription = "Lendo",
                        tint = tabLayerColor,
                        modifier = Modifier
                            .size(29.dp)
                            .graphicsLayer {
                                val c = collapsedFraction
                                translationY = lerp(-7.5f.dp.toPx(), 0f, c)
                                val sc = lerp(1f, 0.90f, c)
                                scaleX = sc
                                scaleY = sc
                            }
                    )
                    BasicText(
                        text = "Lendo",
                        style = TextStyle(
                            color = tabLayerColor,
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            val c = collapsedFraction
                            alpha = (1f - c * 2.15f).coerceIn(0f, 1f)
                            translationY = lerp(13.5f.dp.toPx(), 6f.dp.toPx(), c)
                            val sc = lerp(1f, 0.80f, c)
                            scaleX = sc
                            scaleY = sc
                        }
                    )
                }

                // Tab 1: Biblioteca
                LiquidBottomTab(onClick = { onSelectTab("shelf") }) {
                    Icon(
                        imageVector = if (selectedTabIndex == 1) LucideIcons.BooksVertical else LucideIcons.BookClosed,
                        contentDescription = "Biblioteca",
                        tint = tabLayerColor,
                        modifier = Modifier
                            .size(29.dp)
                            .graphicsLayer {
                                val c = collapsedFraction
                                translationY = lerp(-7.5f.dp.toPx(), 0f, c)
                                val sc = lerp(1f, 0.90f, c)
                                scaleX = sc
                                scaleY = sc
                            }
                    )
                    BasicText(
                        text = "Biblioteca",
                        style = TextStyle(
                            color = tabLayerColor,
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            val c = collapsedFraction
                            alpha = (1f - c * 2.15f).coerceIn(0f, 1f)
                            translationY = lerp(13.5f.dp.toPx(), 6f.dp.toPx(), c)
                            val sc = lerp(1f, 0.80f, c)
                            scaleX = sc
                            scaleY = sc
                        }
                    )
                }

                // Tab 2: Ajustes
                LiquidBottomTab(onClick = { onSelectTab("settings") }) {
                    Icon(
                        imageVector = LucideIcons.Settings,
                        contentDescription = "Ajustes",
                        tint = tabLayerColor,
                        modifier = Modifier
                            .size(29.dp)
                            .graphicsLayer {
                                val c = collapsedFraction
                                translationY = lerp(-7.5f.dp.toPx(), 0f, c)
                                val sc = lerp(1f, 0.90f, c)
                                scaleX = sc
                                scaleY = sc
                            }
                    )
                    BasicText(
                        text = "Ajustes",
                        style = TextStyle(
                            color = tabLayerColor,
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            val c = collapsedFraction
                            alpha = (1f - c * 2.15f).coerceIn(0f, 1f)
                            translationY = lerp(13.5f.dp.toPx(), 6f.dp.toPx(), c)
                            val sc = lerp(1f, 0.80f, c)
                            scaleX = sc
                            scaleY = sc
                        }
                    )
                }
            }

            // Botão Circular da Lupa — Animação Liquid Glass idêntica ao iOS 26
            val searchButtonSize = lerp(64f.dp, 54f.dp, collapsedFraction)
            val searchAnimationScope = rememberCoroutineScope()
            var isSearchPressed by remember { mutableStateOf(false) }
            var handledByPointer by remember { mutableStateOf(false) }
            val searchScaleAnim = remember { Animatable(1f) }
            val searchGlassProgressAnim = remember { Animatable(0f) }

            val searchInteractiveHighlight = remember(searchAnimationScope) {
                com.kyant.backdrop.catalog.utils.InteractiveHighlight(
                    animationScope = searchAnimationScope
                )
            }

            // Caso a aba "search" seja acionada por atalho externo (ex.: botão Buscar na tela)
            var wasSearchSelected by remember { mutableStateOf(isSearchSelected) }
            LaunchedEffect(isSearchSelected) {
                if (isSearchSelected && !wasSearchSelected && !handledByPointer) {
                    searchAnimationScope.launch {
                        searchGlassProgressAnim.animateTo(1f, tween(55))
                        searchGlassProgressAnim.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 350f))
                    }
                    searchAnimationScope.launch {
                        searchScaleAnim.animateTo(1.15f, tween(55))
                        searchScaleAnim.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 380f))
                    }
                }
                handledByPointer = false
                wasSearchSelected = isSearchSelected
            }

            // No press-down (frames 24-25), o ícone escurece/reduz opacidade para 0.22f; no release (frame 26), volta a 1f já azul
            val searchIconAlpha by animateFloatAsState(
                targetValue = if (isSearchPressed && !isSearchSelected) 0.22f else 1f,
                animationSpec = tween(durationMillis = if (isSearchPressed) 65 else 50),
                label = "SearchIconAlpha"
            )

            val currentOnSelectTab by rememberUpdatedState(onSelectTab)

            Box(
                modifier = Modifier
                    .size(searchButtonSize)
                    .drawBackdrop(
                        backdrop = mainBackdrop,
                        shape = { ContinuousCapsule },
                        highlight = {
                            val p = searchGlassProgressAnim.value
                            Highlight.Default.copy(
                                alpha = lerp(baseHighlightAlpha, 0.90f, p)
                            )
                        },
                        shadow = {
                            val p = searchGlassProgressAnim.value
                            if (p > 0.01f) {
                                com.kyant.backdrop.shadow.Shadow(
                                    radius = 14.dp * p,
                                    color = Color.Black.copy(alpha = (if (isLightTheme) 0.14f else 0.32f) * p)
                                )
                            } else {
                                null
                            }
                        },
                        innerShadow = {
                            val p = searchGlassProgressAnim.value
                            if (p > 0.01f) {
                                com.kyant.backdrop.shadow.InnerShadow(
                                    radius = 8.dp * p,
                                    alpha = 0.65f * p
                                )
                            } else {
                                null
                            }
                        },
                        effects = {
                            val p = searchGlassProgressAnim.value
                            if (!settings.reduceTransparency) {
                                vibrancy()
                            }
                            blur(effectiveGlassBlur.toPx())
                            if (effectiveGlassRefraction > 0f) {
                                lens(
                                    refractionHeight = (24f.dp.toPx() + 8f.dp.toPx() * p) * effectiveGlassRefraction,
                                    refractionAmount = (30f.dp.toPx() + 12f.dp.toPx() * p) * effectiveGlassRefraction,
                                    depthEffect = configuredDepthEffect,
                                    chromaticAberration = configuredChromaticAberration
                                )
                            }
                        },
                        layerBlock = {
                            val sc = searchScaleAnim.value
                            scaleX = sc
                            scaleY = sc
                        },
                        onDrawSurface = {
                            val p = searchGlassProgressAnim.value
                            drawRect(glassContainerColor)
                            if (p > 0.01f) {
                                drawRect(
                                    color = if (isLightTheme) Color.White.copy(alpha = 0.12f * p)
                                    else Color.White.copy(alpha = 0.08f * p)
                                )
                            }
                        }
                    )
                    .then(searchInteractiveHighlight.modifier)
                    .then(searchInteractiveHighlight.gestureModifier)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isSearchPressed = true
                            searchAnimationScope.launch {
                                searchGlassProgressAnim.animateTo(
                                    1f,
                                    spring(dampingRatio = 0.9f, stiffness = 700f)
                                )
                            }
                            searchAnimationScope.launch {
                                searchScaleAnim.animateTo(
                                    1.15f,
                                    spring(dampingRatio = 0.75f, stiffness = 550f)
                                )
                            }

                            var releasedInside = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    releasedInside = change.position.x in 0f..size.width.toFloat() &&
                                        change.position.y in 0f..size.height.toFloat()
                                    break
                                }
                            }

                            isSearchPressed = false
                            if (releasedInside) {
                                handledByPointer = true
                                currentOnSelectTab("search")
                                searchAnimationScope.launch {
                                    if (searchScaleAnim.value < 1.13f) {
                                        searchScaleAnim.animateTo(1.15f, tween(50))
                                    }
                                    searchScaleAnim.animateTo(
                                        1f,
                                        spring(dampingRatio = 0.48f, stiffness = 380f)
                                    )
                                }
                                searchAnimationScope.launch {
                                    if (searchGlassProgressAnim.value < 0.85f) {
                                        searchGlassProgressAnim.animateTo(1f, tween(50))
                                    }
                                    searchGlassProgressAnim.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.85f, stiffness = 340f)
                                    )
                                }
                            } else {
                                searchAnimationScope.launch {
                                    searchScaleAnim.animateTo(
                                        1f,
                                        spring(dampingRatio = 0.65f, stiffness = 400f)
                                    )
                                }
                                searchAnimationScope.launch {
                                    searchGlassProgressAnim.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.85f, stiffness = 400f)
                                    )
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            val sc = searchScaleAnim.value
                            scaleX = sc
                            scaleY = sc
                            alpha = searchIconAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LucideIcons.Search,
                        contentDescription = "Buscar",
                        tint = if (isSearchSelected) effectiveAccentColor else tabContentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

/**
 * Extrai a cor vibrante dominante diretamente do bitmap da capa da HQ (ignorando sombras pretas e textos brancos),
 * garantindo que capas vermelhas (ex.: Batman: Ano Um), amarelas (Watchmen), etc. iluminem o fundo com sua cor real.
 */
private suspend fun extractVibrantCoverColor(
    bitmap: android.graphics.Bitmap,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
): Color? = com.example.ui.components.AmbientImageProcessor.calculateAverageCoverColor(bitmap, dispatcher)

@Preview(name = "Quadrinheiro App Preview", showBackground = true, showSystemUi = true)
@Composable
fun QuadrinheiroAppPreview() {
    QuadrinheiroApp()
}

@Composable
private fun SessionTransitionScreen(message: String) {
    val themeColors = LocalIOS27Theme.current
    val visibleState = remember {
        androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.bgPrimary),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visibleState = visibleState, enter = fadeIn(tween(300))) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                com.example.ui.components.IOSLoadingIndicator(
                    color = themeColors.textSecondary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = message,
                    color = themeColors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSansFamily
                )
            }
        }
    }
}

// IOS_SESSION_INDICATOR_V2
