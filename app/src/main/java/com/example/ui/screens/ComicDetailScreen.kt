package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ComicDetailDialog(comic: ComicEntry, profile: ReaderProfile, library: List<ComicEntry>, onClose: () -> Unit,
    onRegister: (ComicEntry) -> Unit, onCover: (String) -> Unit, onLogin: () -> Unit, onShare: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ComicDetailScreen(comic, profile, library, onClose, onRegister, onCover,
            onLogin = { onClose(); onLogin() }, onShare = { onClose(); onShare() })
    }
}

@Composable
private fun ComicDetailScreen(initialComic: ComicEntry, profile: ReaderProfile, library: List<ComicEntry>, onBack: () -> Unit,
    onRegister: (ComicEntry) -> Unit, onCover: (String) -> Unit, onLogin: () -> Unit, onShare: () -> Unit) {
    var comic by remember(initialComic.id) { mutableStateOf(initialComic) }
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
    var detail by remember(comic.id, entity) { mutableStateOf<CatalogDetails?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var tab by remember(comic.id, entity) { mutableIntStateOf(0) }
    var chosenCover by remember(comic.id) { mutableStateOf(comic.coverUrl) }
    var coversOpen by remember { mutableStateOf(false) }
    var editionPicker by remember { mutableStateOf(false) }
    var rating by remember(comic.id) { mutableStateOf<CommunityRating?>(null) }
    val repository = remember { CommunityRepository(context) }
    val listState = rememberLazyListState()
    LaunchedEffect(comic.id, entity) { listState.scrollToItem(0) }
    val uriHandler = LocalUriHandler.current
    BackHandler { goBack() }
    LaunchedEffect(comic.id, comic.externalId, entity, retry) {
        loading = true; failed = false
        try { detail = withContext(Dispatchers.IO) { entity?.let { ComicVineCatalog.details(it.first, it.second) } ?: ComicVineCatalog.resolve(comic) } }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { failed = true }
        finally { loading = false }
    }
    LaunchedEffect(comic.id, comic.externalId, repository.currentUid()) {
        if (repository.currentUid() != null) try { rating = repository.rating(comic) }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { /* An unavailable average is never replaced by an invented one. */ }
    }
    val isComic = entity == null
    val effectiveCover = chosenCover.ifBlank { detail?.image.orEmpty() }
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24 } }
    Column(Modifier.fillMaxSize().background(t.bgPrimary).windowInsetsPadding(WindowInsets.systemBars).testTag("comic_detail_page")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingsBackButton({ goBack() }, collapsed = collapsed, label = if (isComic) "Voltar" else "HQ")
            Spacer(Modifier.weight(1f))
            if (isComic) IconButton(onClick = onShare) { Icon(LucideIcons.ShareIos, "Compartilhar", tint = t.accent) }
        }
        if (isComic && tab == 3) {
            DetailTabs(tab, { tab = it })
            CommunityScreen(library, profile, onLogin, comic)
        } else LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(t.bgSecondary)) {
                    val banner = detail?.banner.orEmpty().ifBlank { chosenCover }
                    if (banner.isNotBlank()) ProfileImage(banner, null, Modifier.fillMaxSize())
                    Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, t.bgPrimary))))
                }
            }
            item {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                    if (isComic) BookCover(effectiveCover, comic.displayTitle, spine3d = false, modifier = Modifier.width(104.dp).aspectRatio(2f / 3f).clickable { coversOpen = true })
                    else ProfileImage(detail?.image.orEmpty(), detail?.title, Modifier.size(110.dp).clip(RoundedCornerShape(22.dp)), face = true)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (isComic) comic.displayTitle else detail?.title ?: "Carregando…", color = t.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                        val publisher = detail?.publisher.orEmpty().ifBlank { if (isComic) comic.publisher else "" }
                        if (publisher.isNotBlank()) Text(publisher, color = t.textSecondary, fontSize = 14.sp)
                        val authors = comic.authors.ifBlank { detail?.creators.orEmpty().filter { it.role.contains("writer", true) || it.role.contains("artist", true) }.take(3).joinToString(", ") { it.name } }
                        if (isComic && authors.isNotBlank()) Text(authors, color = t.textSecondary, fontSize = 14.sp)
                        if (isComic) {
                            rating?.takeIf { it.count > 0 }?.let { Text("★ ${String.format(java.util.Locale.getDefault(), "%.1f", it.average)} · ${it.count} avaliações", color = t.textPrimary, fontSize = 13.sp) }
                            TextButton(onClick = { onRegister(comic.copy(coverUrl = effectiveCover)) }, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Registrar leitura", color = t.accent) }
                        }
                    }
                }
            }
            if (isComic) item { DetailTabs(tab, { tab = it }) }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = t.accent) }
            if (failed) item { TextButton(onClick = { retry++ }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Tentar carregar informações novamente", color = t.accent) } }
            if (isComic && detail == null && !loading) item {
                TextButton(onClick = { editionPicker = true }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Encontrar edição no catálogo", color = t.accent) }
            }
            if (!isComic || tab == 0) {
                val description = detail?.description.orEmpty().ifBlank { if (isComic) comic.synopsisPtBr.ifBlank { comic.synopsis } else "" }
                if (description.isNotBlank()) item { DetailSection(if (isComic) "Sobre a HQ" else "Biografia") { Text(description, color = t.textPrimary, fontSize = 15.sp, lineHeight = 23.sp) } }
                item {
                    DetailSection("Detalhes") {
                        val rows = if (isComic) listOf("Publicação" to detail?.date.orEmpty().ifBlank { comic.releaseYear.toString() },
                            "Formato" to comic.format, "ISBN" to comic.isbn,
                            "Páginas" to comic.pageCount.takeIf { it > 1 }?.toString().orEmpty())
                        else listOf("Nome" to detail?.realName.orEmpty(), "Também conhecido como" to detail?.aliases.orEmpty(),
                            "Aparições" to detail?.appearances?.takeIf { it > 0 }?.toString().orEmpty(), "Poderes" to detail?.powers.orEmpty().joinToString(", "))
                        rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                            Text(label, color = t.textSecondary, fontSize = 12.sp)
                            Text(value, color = t.textPrimary, fontSize = 15.sp)
                            HorizontalDivider(color = t.divider)
                        }
                    }
                }
                if (isComic && detail?.covers.orEmpty().size > 1) item { TextButton(onClick = { coversOpen = true }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Escolher capa", color = t.accent) } }
                val appearances = detail?.issues.orEmpty()
                if (!isComic && appearances.isNotEmpty()) item {
                    DetailSection("Aparições em quadrinhos") {
                        Text("Seleção de edições registradas no Comic Vine.", color = t.textSecondary, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(appearances, key = { it.id }) { issue ->
                                Column(Modifier.width(100.dp).clickable { openComic(issue) }) {
                                    BookCover(issue.coverUrl, issue.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                                    Text(issue.displayTitle, color = t.textPrimary, maxLines = 2, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                if (isComic) item {
                    val similar = remember(comic, library) { SeriesSequenceEngine.findRecommendationsForComic(comic, library, CuratedCatalog.curatedComicsCatalog).map { it.comic }.take(8) }
                    if (similar.isNotEmpty()) DetailSection("Histórias semelhantes") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(similar, key = { it.id }) { other ->
                                Column(Modifier.width(104.dp).clickable { openComic(other) }) {
                                    BookCover(other.coverUrl, other.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                                    Text(other.displayTitle, color = t.textPrimary, fontSize = 12.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            } else {
                val credits = if (tab == 1) detail?.characters.orEmpty() else detail?.creators.orEmpty()
                items(credits, key = { it.id }) { credit ->
                    CreditRow(credit, if (tab == 1) "character" else "person") { openEntity(if (tab == 1) "character" else "person", credit.id) }
                }
                if (!loading && credits.isEmpty()) item { Text("Ainda não há informações desta edição no catálogo.", color = t.textSecondary, modifier = Modifier.padding(horizontal = 20.dp)) }
            }
            detail?.url?.takeIf { it.startsWith("https://comicvine.gamespot.com/") }?.let { source -> item {
                TextButton(onClick = { uriHandler.openUri(source) }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Informações: Comic Vine ↗", color = t.textSecondary, fontSize = 12.sp) }
            } }
        }
    }
    if (coversOpen) CoverChooser(detail?.covers.orEmpty().ifEmpty { listOf(chosenCover).filter { it.isNotBlank() } }, chosenCover,
        onChoose = { chosenCover = it; if (comic.id == initialComic.id) onCover(it); coversOpen = false }, onClose = { coversOpen = false })
    if (editionPicker) EditionChooser(comic.displayTitle, onClose = { editionPicker = false }) { edition ->
        comic = comic.copy(externalId = edition.externalId, sourceLabel = edition.sourceLabel,
            publisher = edition.publisher.ifBlank { comic.publisher })
        editionPicker = false
    }
}

@Composable
private fun DetailTabs(tab: Int, onChange: (Int) -> Unit) {
    val t = LocalIOS27Theme.current
    LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        items(4) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { onChange(index) }) { Text(listOf("Sobre", "Personagens", "Equipe", "Comunidade")[index], color = if (index == tab) t.accent else t.textSecondary) }
                if (index == tab) Box(Modifier.size(24.dp, 2.dp).background(t.accent, RoundedCornerShape(2.dp)))
            }
        }
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
private fun CreditRow(credit: CatalogCredit, type: String, onClick: () -> Unit) {
    val t = LocalIOS27Theme.current
    var image by remember(credit.id) { mutableStateOf("") }
    // Thumbnails load only for visible rows; the bounded catalog cache reuses detail requests.
    LaunchedEffect(credit.id) {
        try { image = withContext(Dispatchers.IO) { ComicVineCatalog.details(type, credit.id).image } }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { }
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)).background(t.bgSecondary)) { ProfileImage(image, null, Modifier.fillMaxSize(), face = true) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(credit.name, color = t.textPrimary)
            if (credit.role.isNotBlank()) Text(credit.role, color = t.textSecondary, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverChooser(covers: List<String>, selected: String, onChoose: (String) -> Unit, onClose: () -> Unit) {
    val t = LocalIOS27Theme.current
    ModalBottomSheet(onDismissRequest = onClose, containerColor = t.bgSecondary) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SectionHeading("Capa")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(covers.distinct().take(6)) { cover ->
                    Column(Modifier.width(116.dp).clickable { onChoose(cover) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        BookCover(cover, "Capa alternativa", spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                        Text(if (cover == selected) "Selecionada" else "Escolher", color = t.accent, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
            if (covers.size <= 1) Text("Esta edição tem apenas uma capa disponível na fonte.", color = t.textSecondary, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditionChooser(initialQuery: String, onClose: () -> Unit, onChoose: (ComicEntry) -> Unit) {
    val t = LocalIOS27Theme.current
    var query by remember { mutableStateOf(initialQuery) }
    var entries by remember { mutableStateOf(emptyList<ComicEntry>()) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.length < 2) return@LaunchedEffect
        busy = true; failed = false
        try { kotlinx.coroutines.delay(300); entries = ComicVineCatalog.search(query).items }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { failed = true }
        finally { busy = false }
    }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = t.bgSecondary) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp).imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SectionHeading("Edição", "Escolha a edição correspondente para ver personagens e equipe.") }
            item { AppleTextField(query, { query = it.take(100) }, "Buscar edição") }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = t.accent) }
            if (failed) item { Text("Não foi possível buscar. Tente outro termo.", color = t.textSecondary) }
            items(entries, key = { it.id }) { entry ->
                Row(Modifier.fillMaxWidth().clickable { onChoose(entry) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BookCover(entry.coverUrl, entry.displayTitle, spine3d = false, modifier = Modifier.width(48.dp).height(72.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.displayTitle, color = t.textPrimary)
                        Text(listOfNotNull(entry.issueNumber.takeIf { it > 0 }?.let { "Edição $it" }, entry.releaseYear.toString()).joinToString(" · "), color = t.textSecondary, fontSize = 12.sp)
                    }
                }
            }
            if (!busy && !failed && entries.isEmpty()) item { Text("Nenhuma edição encontrada. Tente o título original.", color = t.textSecondary) }
        }
    }
}
