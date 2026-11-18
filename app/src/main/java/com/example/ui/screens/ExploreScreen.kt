package com.example.ui.screens

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EditorialDiscovery
import com.example.data.RecommendedComic
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.components.BookCover
import com.example.ui.theme.LocalIOS27Theme
import com.example.viewmodel.QuadrinheiroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    onLogin: () -> Unit = {},
    onRecommendedComic: (RecommendedComic) -> Unit = { onComic(it.comic) }
) {
    val theme = LocalIOS27Theme.current
    val appContext = LocalContext.current.applicationContext
    var collection by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val isBackPreview = com.example.ui.components.LocalBackPreview.current
    val exploreRecommendationState by viewModel.exploreRecommendationState.collectAsStateWithLifecycle()
    val recommendations = exploreRecommendationState.recommendations
    val editorial by EditorialDiscovery.shelves.collectAsStateWithLifecycle()
    val editorialScope = rememberCoroutineScope()
    var editorialLoading by remember { mutableStateOf(true) }
    val editorialExtraPages = remember { mutableStateMapOf<String, Int>() }
    var loadingMoreShelf by remember { mutableStateOf<String?>(null) }
    if (!isBackPreview) {
        LaunchedEffect(viewModel) {
            viewModel.curation.load()
            viewModel.exploreReselectEvents.collect { collection=null;tab=0 }
        }
        LaunchedEffect(appContext) {
            try {
                withContext(Dispatchers.IO) { EditorialDiscovery.load(appContext) }
            } finally {
                editorialLoading = false
            }
        }
    }
    fun retryEditorial(title: String) {
        if (editorialLoading) return
        editorialLoading = true
        editorialScope.launch {
            try {
                withContext(Dispatchers.IO) { EditorialDiscovery.load(appContext, retryShelf = title) }
            } finally {
                editorialLoading = false
            }
        }
    }
    com.example.ui.components.AppPageTransition(
        page = com.example.ui.components.AppPage("explore", collection, null, false),
        backEnabled = collection != null,
        backPage = com.example.ui.components.AppPage("explore", null, null, false),
        onBack = { collection = null }
    ) { destination ->
        val selected = destination.settings
        Column(Modifier.fillMaxSize()) {
            if (selected == null) {
                Column(
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
            }
            if (selected == null && tab == 1) {
                CommunityScreen(comics, profile, onLogin)
            } else {
                val expandedState = rememberLazyListState()
                val backCollapsed by remember {
                    derivedStateOf {
                        expandedState.firstVisibleItemIndex > 0 || expandedState.firstVisibleItemScrollOffset > 24
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = if (selected == null) listState else expandedState,
                        modifier = Modifier.fillMaxSize().testTag(if (selected == null) "explore_page" else "explore_collection"),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = if (selected == null) 16.dp else 56.dp,
                            bottom = 208.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(26.dp)
                    ) {
                        if (selected != null) {
                            item(key = "selected_header", contentType = "explore_header") {
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 8.dp),
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
                            val personal = selected == "Para você"
                            val entries = if (personal) recommendations.map { it.comic } else editorial[selected].orEmpty()
                            val recommendationsById = if (personal) recommendations.associateBy { it.comic.id } else emptyMap()
                            items(
                                items = entries.chunked(3),
                                key = { it.first().id },
                                contentType = { "explore_grid_row" }
                            ) { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { comic ->
                                        val recommendation = recommendationsById[comic.id]
                                        Column(Modifier.weight(1f)) {
                                            ExplorePoster(
                                                comic = comic,
                                                modifier = Modifier.fillMaxWidth(),
                                                onComic = { selectedComic ->
                                                    if (recommendation != null) onRecommendedComic(recommendation)
                                                    else onComic(selectedComic)
                                                },
                                                reasonLabel = recommendation?.reasonLabel
                                            )
                                        }
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            if (entries.isEmpty()) {
                                item(key = "selected_empty", contentType = "explore_empty") {
                                    if (personal) RecommendationEmptyState(exploreRecommendationState.isRefreshing)
                                    else EditorialEmptyState(editorialLoading) { retryEditorial(selected) }
                                }
                            } else if (!personal && entries.size < 48) {
                                item(key = "selected_load_more", contentType = "explore_load_more") {
                                    val isLoadingMore = loadingMoreShelf == selected
                                    Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                                        TextButton(
                                            enabled = !isLoadingMore,
                                            onClick = {
                                                if (loadingMoreShelf != null) return@TextButton
                                                val nextPage = editorialExtraPages.getOrDefault(selected, 0)
                                                loadingMoreShelf = selected
                                                editorialScope.launch {
                                                    try {
                                                        withContext(Dispatchers.IO) {
                                                            EditorialDiscovery.loadMoreForShelf(appContext, selected, nextPage)
                                                        }
                                                        editorialExtraPages[selected] = nextPage + 1
                                                    } finally {
                                                        if (loadingMoreShelf == selected) loadingMoreShelf = null
                                                    }
                                                }
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text(
                                                if (isLoadingMore) "Buscando mais edições…" else "Carregar mais edições",
                                                color = theme.accent,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            item(key = "Para você", contentType = "explore_for_you_section") {
                                ExploreForYouSection(
                                    recommendations = recommendations,
                                    loading = exploreRecommendationState.isRefreshing,
                                    onExpand = { collection = "Para você" },
                                    onRecommendedComic = onRecommendedComic
                                )
                            }
                            items(EditorialDiscovery.selections.keys.toList(), key = { it }, contentType = { "explore_shelf_section" }) { title ->
                                ExploreCatalogSection(
                                    title = title, entries = editorial[title].orEmpty(), loading = editorialLoading,
                                    onExpand = { collection = title }, onRetry = { retryEditorial(title) },
                                    onComic = onComic
                                )
                            }
                        }
                    }
                    if (selected != null) {
                        SettingsBackButton(
                            onBack = { collection = null },
                            collapsed = backCollapsed,
                            label = "Explorar",
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 16.dp, top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreForYouSection(
    recommendations: List<RecommendedComic>,
    loading: Boolean,
    onExpand: () -> Unit,
    onRecommendedComic: (RecommendedComic) -> Unit
) {
    val theme = LocalIOS27Theme.current
    if (recommendations.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading("Para você")
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Ver seleção Para você", tint = theme.textSecondary)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    items = recommendations.take(10),
                    key = { it.comic.id },
                    contentType = { "explore_poster" }
                ) { recommendation ->
                    Column(Modifier.width(124.dp)) {
                        ExplorePoster(
                            comic = recommendation.comic,
                            modifier = Modifier.fillMaxWidth(),
                            onComic = { onRecommendedComic(recommendation) },
                            reasonLabel = recommendation.reasonLabel
                        )
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeading("Para você")
            RecommendationEmptyState(loading)
        }
    }
}

@Composable
private fun ExploreCatalogSection(
    title: String,
    entries: List<ComicEntry>,
    loading: Boolean,
    onExpand: () -> Unit,
    onRetry: () -> Unit,
    onComic: (ComicEntry) -> Unit
) {
    val theme = LocalIOS27Theme.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = entries.isNotEmpty(), onClick = onExpand),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeading(title)
            if (entries.isNotEmpty()) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Ver seleção $title", tint = theme.textSecondary)
        }
        if (entries.isEmpty()) EditorialEmptyState(loading, onRetry)
        else LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                items = entries.take(10),
                key = { it.id },
                contentType = { "explore_poster" }
            ) { comic ->
                ExplorePoster(comic, Modifier.width(112.dp), onComic, null)
            }
        }
    }
}

@Composable
private fun RecommendationEmptyState(loading: Boolean) {
    Text(
        if (loading) "Preparando suas sugestões…" else "Suas sugestões aparecem aqui com base nas leituras e nos favoritos.",
        color = LocalIOS27Theme.current.textSecondary, fontSize = 13.sp
    )
}

@Composable
private fun EditorialEmptyState(loading: Boolean, onRetry: () -> Unit) {
    val theme = LocalIOS27Theme.current
    Column {
        Text(if (loading) "Carregando seleção…" else "Seleção indisponível no momento.", color = theme.textSecondary, fontSize = 13.sp)
        if (!loading) TextButton(onClick = onRetry) { Text("Tentar novamente", color = theme.accent) }
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
    val semanticDescription = buildString {
        append(comic.displayTitle)
        if (comic.authors.isNotBlank()) append(", de ").append(comic.authors)
        if (!reasonLabel.isNullOrBlank()) append(". Motivo: ").append(reasonLabel)
    }
    Column(
        modifier
            .testTag("explore_poster")
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            }
            .clickable(onClickLabel = "Ver detalhes de ${comic.displayTitle}") { onComic(comic) },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
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
