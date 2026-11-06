package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FirebaseAiRecommendationEnhancer
import com.example.data.MultiApiSearchEngine
import com.example.data.PersonalizedDiscovery
import com.example.data.RecommendationCandidateRetriever
import com.example.data.RecommendedComic
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.components.BookCover
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalAppMotion
import com.example.viewmodel.QuadrinheiroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PERSISTED_CACHE_TTL_MS = 12L * 60L * 60L * 1000L

@Composable
fun ExploreScreen(comics: List<ComicEntry>, listState: LazyListState,
    onComic: (ComicEntry) -> Unit, onSearch: (String) -> Unit,
    profile: ReaderProfile = ReaderProfile(),
    viewModel: QuadrinheiroViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLogin: () -> Unit = {}) {
    val theme = LocalIOS27Theme.current
    val motion = LocalAppMotion.current
    // Same Activity-scoped owner as the app host; its existing settings flow is isolated by account.
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    var collection by remember { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val catalog by com.example.data.ModernCatalog.entries.collectAsState()
    LaunchedEffect(listState) { listState.scrollToItem(0) }
    LaunchedEffect(Unit) { com.example.data.ModernCatalog.load() }
    val recommendationState = remember {
        val restored = PersonalizedDiscovery.decodeCachedRecommendations(settings.cachedRecommendationsJson)
            .filter { rec ->
                comics.none { lib ->
                    lib.id == rec.comic.id ||
                        (lib.externalId.isNotBlank() && lib.externalId == rec.comic.externalId)
                }
            }
        mutableStateOf(
            restored.ifEmpty {
                PersonalizedDiscovery.recommendWithReasons(catalog, comics, profile.favoriteUniverse, settings)
            }
        )
    }
    val recommendations by recommendationState
    val recommendationMap = remember(recommendations) {
        recommendations.associate { it.comic.id to it.reasonLabel }
    }

    LaunchedEffect(comics, profile.favoriteUniverse, settings.profileFavoriteComicIds, settings.profileFavoriteCharacters, catalog) {
        val taste = PersonalizedDiscovery.extractTasteProfile(comics, settings, profile.favoriteUniverse)
        val storedParts = settings.recommendationTasteHash.split(":")
        val storedHash = storedParts.firstOrNull().orEmpty()
        val storedTimeMs = storedParts.getOrNull(1)?.toLongOrNull() ?: 0L
        val nowMs = System.currentTimeMillis()
        val persistedValid = storedHash == taste.tasteHash &&
            storedTimeMs > 0L &&
            (nowMs - storedTimeMs) < PERSISTED_CACHE_TTL_MS &&
            settings.cachedRecommendationsJson.isNotBlank()

        // First compute fast local recommendations if nothing cached or if taste changed
        val localFast = withContext(Dispatchers.Default) {
            PersonalizedDiscovery.recommendWithReasons(
                catalog + RecommendationCandidateRetriever.getCachedPool(),
                comics,
                profile.favoriteUniverse,
                settings
            )
        }
        if (localFast.isNotEmpty() && (!persistedValid || recommendationState.value.isEmpty())) {
            recommendationState.value = localFast
        }

        val needsActiveRefresh = !persistedValid || !RecommendationCandidateRetriever.isCacheValid(taste.tasteHash)
        if (!needsActiveRefresh) return@LaunchedEffect

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
            recommendationState.value = computed
            val encodedJson = PersonalizedDiscovery.encodeCachedRecommendations(computed)
            val newHashStamp = "${taste.tasteHash}:${System.currentTimeMillis()}"
            if (encodedJson.isNotBlank() && (encodedJson != settings.cachedRecommendationsJson || storedHash != taste.tasteHash)) {
                viewModel.updateSettings { current ->
                    current.copy(
                        cachedRecommendationsJson = encodedJson,
                        recommendationTasteHash = newHashStamp
                    )
                }
            }
        }
    }

    val recommendationComics = remember(recommendations) { recommendations.map { it.comic } }
    val collections = remember(catalog, recommendationComics) { linkedMapOf(
        "Para você" to (if (recommendationComics.isNotEmpty()) recommendationComics else catalog.take(12)),
        "Para começar" to catalog.filter { it.displayTitle.contains("Ano Um", true) || it.displayTitle.contains("Watchmen", true) || it.displayTitle.contains("Maus", true) || it.displayTitle.contains("Sandman", true) }.ifEmpty { catalog.take(12) },
        "Universo DC" to catalog.filter { it.publisher.contains("DC", true) || it.publisher.contains("Vertigo", true) || it.displayTitle.contains("Batman", true) },
        "Universo Marvel" to catalog.filter { it.publisher.contains("Marvel", true) || it.displayTitle.contains("Aranha", true) || it.displayTitle.contains("Demolidor", true) }
    ) }
    BackHandler(collection != null) { collection = null }
    Column(Modifier.fillMaxSize()) {
        if (collection == null) Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Explorar", Modifier.weight(1f), color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                IconButton(onClick = { onSearch("") }) { Icon(Icons.Default.Search, "Buscar quadrinhos", tint = theme.textPrimary) }
            }
            AppleSegmentedPicker(listOf("Descobrir", "Comunidade"), tab, { tab = it }, Modifier.fillMaxWidth())
        }
        if (collection == null && tab == 1) CommunityScreen(comics, profile, onLogin) else
        AnimatedContent(collection, transitionSpec = { fadeIn(motion.tween(180)) togetherWith fadeOut(motion.tween(120)) }, label = "explore_collection") { selected ->
            val expandedState = rememberLazyListState()
            LazyColumn(state = if (selected == null) listState else expandedState,
                modifier = Modifier.fillMaxSize().testTag(if (selected == null) "explore_page" else "explore_collection"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 208.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp)) {
                if (selected != null) item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingsBackButton({ collection = null }, label = "Explorar")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(selected, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                            }
                            IconButton(onClick = { onSearch("") }) { Icon(Icons.Default.Search, "Buscar quadrinhos", tint = theme.textPrimary) }
                        }
                    }
                }
                if (selected != null) {
                    val entries = collections[selected].orEmpty()
                    items(entries.chunked(3), key = { it.first().id }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { comic ->
                                val reason = if (selected == "Para você") recommendationMap[comic.id] else null
                                ExplorePoster(comic, Modifier.weight(1f), onComic, reason)
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (entries.isEmpty()) item { Text("Ainda não há sugestões compatíveis no catálogo carregado.", color = theme.textSecondary) }
                    item(key = "explore_load_more") {
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
                                                com.example.data.ModernCatalog.loadMore(univ)
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
                    collections.forEach { (title, entries) ->
                        if (entries.isNotEmpty()) item(key = title) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { collection = title },
                                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    SectionHeading(title)
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Ver seleção $title", tint = theme.textSecondary)
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(entries.take(8), key = { it.id }) { comic ->
                                        val reason = if (title == "Para você") recommendationMap[comic.id] else null
                                        ExplorePoster(comic, Modifier.width(112.dp), onComic, reason)
                                    }
                                }
                            }
                        } else if (title == "Para você") item(key = "discovery_empty") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionHeading(title)
                                Text("Ainda não há sugestões compatíveis no catálogo carregado.", color = theme.textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorePoster(comic: ComicEntry, modifier: Modifier, onComic: (ComicEntry) -> Unit, reasonLabel: String? = null) {
    val t = LocalIOS27Theme.current
    Column(modifier.clickable { onComic(comic) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BookCover(comic.coverUrl, comic.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        if (!reasonLabel.isNullOrBlank()) {
            Text(reasonLabel, color = t.accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(comic.displayTitle, color = t.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(comic.authors, color = t.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
