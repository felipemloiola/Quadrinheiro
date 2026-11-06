package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FirebaseAiRecommendationEnhancer
import com.example.data.ModernCatalog
import com.example.data.MultiApiSearchEngine
import com.example.data.PersonalizedDiscovery
import com.example.data.RecommendationCandidateRetriever
import com.example.data.RecommendedComic
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.components.BookCover
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalIOS27Theme
import com.example.viewmodel.QuadrinheiroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PERSISTED_CACHE_TTL_MS = 12L * 60L * 60L * 1000L

internal object ExploreShowcaseBuilder {
    @Volatile
    private var memoCatalogStamp: String = ""
    @Volatile
    var memoStarter: List<ComicEntry> = emptyList()
        private set
    @Volatile
    var memoDc: List<ComicEntry> = emptyList()
        private set
    @Volatile
    var memoMarvel: List<ComicEntry> = emptyList()
        private set
    @Volatile
    var memoRecommendations: List<RecommendedComic> = emptyList()
        private set

    fun catalogStamp(catalog: List<ComicEntry>): String =
        if (catalog.isEmpty()) "empty" else "${catalog.size}:${catalog.first().id}:${catalog.last().id}"

    fun filterStarter(catalog: List<ComicEntry>): List<ComicEntry> {
        if (catalog.isEmpty()) return emptyList()
        val matches = catalog.filter {
            it.displayTitle.contains("Ano Um", true) ||
                it.displayTitle.contains("Watchmen", true) ||
                it.displayTitle.contains("Maus", true) ||
                it.displayTitle.contains("Sandman", true)
        }
        return matches.ifEmpty { catalog.take(12) }
    }

    fun filterDc(catalog: List<ComicEntry>): List<ComicEntry> {
        if (catalog.isEmpty()) return emptyList()
        return catalog.filter {
            it.publisher.contains("DC", true) ||
                it.publisher.contains("Vertigo", true) ||
                it.displayTitle.contains("Batman", true)
        }
    }

    fun filterMarvel(catalog: List<ComicEntry>): List<ComicEntry> {
        if (catalog.isEmpty()) return emptyList()
        return catalog.filter {
            it.publisher.contains("Marvel", true) ||
                it.displayTitle.contains("Aranha", true) ||
                it.displayTitle.contains("Demolidor", true)
        }
    }

    fun hasSameComicIds(current: List<ComicEntry>, next: List<ComicEntry>): Boolean {
        if (current.size != next.size) return false
        for (i in current.indices) {
            if (current[i].id != next[i].id || current[i].coverUrl != next[i].coverUrl) return false
        }
        return true
    }

    fun hasSameRecommendations(current: List<RecommendedComic>, next: List<RecommendedComic>): Boolean {
        if (current.size != next.size) return false
        for (i in current.indices) {
            if (current[i].comic.id != next[i].comic.id ||
                current[i].reasonLabel != next[i].reasonLabel ||
                current[i].comic.coverUrl != next[i].comic.coverUrl
            ) return false
        }
        return true
    }

    @Synchronized
    fun updateMemoizedSlices(
        stamp: String,
        starter: List<ComicEntry>,
        dc: List<ComicEntry>,
        marvel: List<ComicEntry>
    ) {
        memoCatalogStamp = stamp
        memoStarter = starter
        memoDc = dc
        memoMarvel = marvel
    }

    @Synchronized
    fun updateMemoizedRecommendations(recs: List<RecommendedComic>) {
        memoRecommendations = recs
    }

    @Synchronized
    fun resetMemoForTests() {
        memoCatalogStamp = ""
        memoStarter = emptyList()
        memoDc = emptyList()
        memoMarvel = emptyList()
        memoRecommendations = emptyList()
    }
}

@Composable
fun ExploreScreen(
    comics: List<ComicEntry>,
    listState: LazyListState,
    onComic: (ComicEntry) -> Unit,
    onSearch: (String) -> Unit,
    profile: ReaderProfile = ReaderProfile(),
    viewModel: QuadrinheiroViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLogin: () -> Unit = {}
) {
    val theme = LocalIOS27Theme.current
    val motion = LocalAppMotion.current
    val appContext = LocalContext.current.applicationContext
    // Same Activity-scoped owner as the app host; its existing settings flow is isolated by account.
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    var collection by remember { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val catalog by ModernCatalog.entries.collectAsState()

    // Estados separados e progressivos: inicializados a partir do snapshot em memória (zero cálculo pesado na UI thread)
    val recommendationState = remember {
        mutableStateOf(ExploreShowcaseBuilder.memoRecommendations)
    }
    val starterComicsState = remember {
        mutableStateOf(ExploreShowcaseBuilder.memoStarter)
    }
    val dcComicsState = remember {
        mutableStateOf(ExploreShowcaseBuilder.memoDc)
    }
    val marvelComicsState = remember {
        mutableStateOf(ExploreShowcaseBuilder.memoMarvel)
    }

    LaunchedEffect(listState) { listState.scrollToItem(0) }

    // 1. Garante restauração imediata do cache em disco do ModernCatalog e atualização silenciosa se TTL expirado
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ModernCatalog.load(context = appContext)
        }
    }

    // 2. Publicação progressiva e independente das vitrines editoriais fora da Main Thread
    LaunchedEffect(catalog) {
        if (catalog.isEmpty()) return@LaunchedEffect
        val stamp = ExploreShowcaseBuilder.catalogStamp(catalog)
        val starter = withContext(Dispatchers.Default) {
            ExploreShowcaseBuilder.filterStarter(catalog)
        }
        if (!ExploreShowcaseBuilder.hasSameComicIds(starterComicsState.value, starter)) {
            starterComicsState.value = starter
        }
        val dc = withContext(Dispatchers.Default) {
            ExploreShowcaseBuilder.filterDc(catalog)
        }
        if (!ExploreShowcaseBuilder.hasSameComicIds(dcComicsState.value, dc)) {
            dcComicsState.value = dc
        }
        val marvel = withContext(Dispatchers.Default) {
            ExploreShowcaseBuilder.filterMarvel(catalog)
        }
        if (!ExploreShowcaseBuilder.hasSameComicIds(marvelComicsState.value, marvel)) {
            marvelComicsState.value = marvel
        }
        ExploreShowcaseBuilder.updateMemoizedSlices(stamp, starter, dc, marvel)
    }

    // 3. Publicação progressiva das recomendações "Para você" fora da Main Thread
    LaunchedEffect(
        comics,
        profile.favoriteUniverse,
        settings.profileFavoriteComicIds,
        settings.profileFavoriteCharacters,
        catalog
    ) {
        // Etapa 3a: Decodifica cache local persistido em Dispatchers.Default para abertura imediata
        if (recommendationState.value.isEmpty() && settings.cachedRecommendationsJson.isNotBlank()) {
            val restoredFromDisk = withContext(Dispatchers.Default) {
                PersonalizedDiscovery.decodeCachedRecommendations(settings.cachedRecommendationsJson)
                    .filter { rec ->
                        comics.none { lib ->
                            lib.id == rec.comic.id ||
                                (lib.externalId.isNotBlank() && lib.externalId == rec.comic.externalId)
                        }
                    }
            }
            if (restoredFromDisk.isNotEmpty() &&
                !ExploreShowcaseBuilder.hasSameRecommendations(recommendationState.value, restoredFromDisk)
            ) {
                recommendationState.value = restoredFromDisk
                ExploreShowcaseBuilder.updateMemoizedRecommendations(restoredFromDisk)
            }
        }

        // Se o catálogo base ainda está restaurando/carregando e vazio, aguarda brevemente para evitar duplo cálculo
        if (catalog.isEmpty() && ModernCatalog.isLoading.value) {
            var waited = 0L
            while (ModernCatalog.current.isEmpty() && ModernCatalog.isLoading.value && waited < 600L) {
                delay(50L)
                waited += 50L
            }
        }

        val taste = withContext(Dispatchers.Default) {
            PersonalizedDiscovery.extractTasteProfile(comics, settings, profile.favoriteUniverse)
        }
        val storedParts = settings.recommendationTasteHash.split(":")
        val storedHash = storedParts.firstOrNull().orEmpty()
        val storedTimeMs = storedParts.getOrNull(1)?.toLongOrNull() ?: 0L
        val nowMs = System.currentTimeMillis()
        val persistedValid = storedHash == taste.tasteHash &&
            storedTimeMs > 0L &&
            (nowMs - storedTimeMs) < PERSISTED_CACHE_TTL_MS &&
            settings.cachedRecommendationsJson.isNotBlank()

        // Etapa 3b: Recomendações heurísticas locais rápidas em Dispatchers.Default
        val localFast = withContext(Dispatchers.Default) {
            PersonalizedDiscovery.recommendWithReasons(
                catalog + RecommendationCandidateRetriever.getCachedPool(),
                comics,
                profile.favoriteUniverse,
                settings
            )
        }
        if (localFast.isNotEmpty() && (!persistedValid || recommendationState.value.isEmpty())) {
            if (!ExploreShowcaseBuilder.hasSameRecommendations(recommendationState.value, localFast)) {
                recommendationState.value = localFast
                ExploreShowcaseBuilder.updateMemoizedRecommendations(localFast)
            }
        }

        // Etapa 3c: Busca ativa escalonada (somente após o catálogo base não estar disputando rede inicial)
        val needsActiveRefresh = !persistedValid || !RecommendationCandidateRetriever.isCacheValid(taste.tasteHash)
        if (!needsActiveRefresh || (catalog.isEmpty() && ModernCatalog.isLoading.value)) return@LaunchedEffect

        val computed = withContext(Dispatchers.Default) {
            val retrieved = RecommendationCandidateRetriever.retrieveCandidates(taste, MultiApiSearchEngine())
            val ranked = PersonalizedDiscovery.recommendWithReasons(
                catalog + retrieved,
                comics,
                profile.favoriteUniverse,
                settings
            )
            FirebaseAiRecommendationEnhancer.enhanceOrFallback(
                taste = taste,
                candidates = ranked
            )
        }
        if (computed.isNotEmpty()) {
            if (!ExploreShowcaseBuilder.hasSameRecommendations(recommendationState.value, computed)) {
                recommendationState.value = computed
                ExploreShowcaseBuilder.updateMemoizedRecommendations(computed)
            }
            val encodedJson = withContext(Dispatchers.Default) {
                PersonalizedDiscovery.encodeCachedRecommendations(computed)
            }
            val newHashStamp = "${taste.tasteHash}:${System.currentTimeMillis()}"
            if (encodedJson.isNotBlank() &&
                (encodedJson != settings.cachedRecommendationsJson || storedHash != taste.tasteHash)
            ) {
                viewModel.updateSettings { current ->
                    current.copy(
                        cachedRecommendationsJson = encodedJson,
                        recommendationTasteHash = newHashStamp
                    )
                }
            }
        }
    }

    BackHandler(collection != null) { collection = null }
    Column(Modifier.fillMaxSize()) {
        if (collection == null) Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Explorar", Modifier.weight(1f), color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                IconButton(onClick = { onSearch("") }) {
                    Icon(Icons.Default.Search, "Buscar quadrinhos", tint = theme.textPrimary)
                }
            }
            AppleSegmentedPicker(listOf("Descobrir", "Comunidade"), tab, { tab = it }, Modifier.fillMaxWidth())
        }
        if (collection == null && tab == 1) {
            CommunityScreen(comics, profile, onLogin)
        } else {
            AnimatedContent(
                collection,
                transitionSpec = { fadeIn(motion.tween(180)) togetherWith fadeOut(motion.tween(120)) },
                label = "explore_collection"
            ) { selected ->
                val expandedState = rememberLazyListState()
                LazyColumn(
                    state = if (selected == null) listState else expandedState,
                    modifier = Modifier.fillMaxSize().testTag(if (selected == null) "explore_page" else "explore_collection"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 208.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    if (selected != null) {
                        item(key = "selected_header", contentType = "explore_header") {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SettingsBackButton({ collection = null }, label = "Explorar")
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(selected, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                                    }
                                    IconButton(onClick = { onSearch("") }) {
                                        Icon(Icons.Default.Search, "Buscar quadrinhos", tint = theme.textPrimary)
                                    }
                                }
                            }
                        }
                        val recs = recommendationState.value
                        val reasonMap = recs.associate { it.comic.id to it.reasonLabel }
                        val entries = when (selected) {
                            "Para você" -> recs.map { it.comic }.ifEmpty { catalog.take(12) }
                            "Para começar" -> starterComicsState.value.ifEmpty { catalog.take(12) }
                            "Universo DC" -> dcComicsState.value
                            "Universo Marvel" -> marvelComicsState.value
                            else -> emptyList()
                        }
                        items(
                            items = entries.chunked(3),
                            key = { it.first().id },
                            contentType = { "explore_grid_row" }
                        ) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { comic ->
                                    val reason = if (selected == "Para você") reasonMap[comic.id] else null
                                    ExplorePoster(comic, Modifier.weight(1f), onComic, reason)
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (entries.isEmpty()) {
                            item(key = "selected_empty", contentType = "explore_empty") {
                                Text("Ainda não há sugestões compatíveis no catálogo carregado.", color = theme.textSecondary)
                            }
                        }
                        item(key = "explore_load_more", contentType = "explore_footer") {
                            val scope = rememberCoroutineScope()
                            var loadingMore by remember { mutableStateOf(false) }
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                if (loadingMore) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = theme.accent, strokeWidth = 2.dp)
                                } else {
                                    TextButton(
                                        onClick = {
                                            loadingMore = true
                                            scope.launch {
                                                try {
                                                    val univ = when (selected) {
                                                        "Universo DC" -> "DC / Vertigo"
                                                        "Universo Marvel" -> "Marvel"
                                                        else -> profile.favoriteUniverse
                                                    }
                                                    ModernCatalog.loadMore(univ)
                                                } finally {
                                                    loadingMore = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.heightIn(min = 44.dp).testTag("explore_load_more_button")
                                    ) {
                                        Text("Carregar mais edições", color = theme.accent)
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "Para você", contentType = "explore_for_you_section") {
                            ExploreForYouSection(
                                recommendationState = recommendationState,
                                catalogFallback = catalog,
                                onExpand = { collection = "Para você" },
                                onComic = onComic
                            )
                        }
                        item(key = "Para começar", contentType = "explore_shelf_section") {
                            ExploreCatalogSection(
                                title = "Para começar",
                                entriesState = starterComicsState,
                                onExpand = { collection = "Para começar" },
                                onComic = onComic
                            )
                        }
                        item(key = "Universo DC", contentType = "explore_shelf_section") {
                            ExploreCatalogSection(
                                title = "Universo DC",
                                entriesState = dcComicsState,
                                onExpand = { collection = "Universo DC" },
                                onComic = onComic
                            )
                        }
                        item(key = "Universo Marvel", contentType = "explore_shelf_section") {
                            ExploreCatalogSection(
                                title = "Universo Marvel",
                                entriesState = marvelComicsState,
                                onExpand = { collection = "Universo Marvel" },
                                onComic = onComic
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreForYouSection(
    recommendationState: State<List<RecommendedComic>>,
    catalogFallback: List<ComicEntry>,
    onExpand: () -> Unit,
    onComic: (ComicEntry) -> Unit
) {
    val theme = LocalIOS27Theme.current
    val recs by recommendationState
    val entries = remember(recs, catalogFallback) {
        if (recs.isNotEmpty()) recs.map { it.comic } else catalogFallback.take(12)
    }
    val reasonMap = remember(recs) {
        recs.associate { it.comic.id to it.reasonLabel }
    }
    if (entries.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(onClick = onExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading("Para você")
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Ver seleção Para você", tint = theme.textSecondary)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    items = entries.take(8),
                    key = { it.id },
                    contentType = { "explore_poster" }
                ) { comic ->
                    ExplorePoster(comic, Modifier.width(112.dp), onComic, reasonMap[comic.id])
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeading("Para você")
            Text(
                "Ainda não há sugestões compatíveis no catálogo carregado.",
                color = theme.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ExploreCatalogSection(
    title: String,
    entriesState: State<List<ComicEntry>>,
    onExpand: () -> Unit,
    onComic: (ComicEntry) -> Unit
) {
    val theme = LocalIOS27Theme.current
    val entries by entriesState
    if (entries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(onClick = onExpand),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeading(title)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Ver seleção $title", tint = theme.textSecondary)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                items = entries.take(8),
                key = { it.id },
                contentType = { "explore_poster" }
            ) { comic ->
                ExplorePoster(comic, Modifier.width(112.dp), onComic, null)
            }
        }
    }
}

@Composable
private fun ExplorePoster(
    comic: ComicEntry,
    modifier: Modifier,
    onComic: (ComicEntry) -> Unit,
    reasonLabel: String? = null
) {
    val t = LocalIOS27Theme.current
    Column(modifier.clickable { onComic(comic) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BookCover(
            comic.coverUrl,
            comic.displayTitle,
            spine3d = false,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
        )
        if (!reasonLabel.isNullOrBlank()) {
            Text(
                reasonLabel,
                color = t.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            comic.displayTitle,
            color = t.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            comic.authors,
            color = t.textSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
