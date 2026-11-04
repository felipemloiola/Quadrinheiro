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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var detail by remember(comic.id, comic.externalId, entity) { mutableStateOf<CatalogDetails?>(null) }
    var loading by remember(comic.id, comic.externalId, entity) { mutableStateOf(true) }
    var translating by remember(comic.id, comic.externalId, entity) { mutableStateOf(false) }
    var failed by remember(comic.id, comic.externalId, entity) { mutableStateOf(false) }
    var retry by remember(comic.id, comic.externalId, entity) { mutableIntStateOf(0) }
    var tab by remember(comic.id, comic.externalId, entity) { mutableIntStateOf(0) }
    var expandedDescription by remember(comic.id, comic.externalId, entity) { mutableStateOf(false) }
    var editionPicker by remember { mutableStateOf(false) }
    var rating by remember(comic.id, comic.externalId) { mutableStateOf<CommunityRating?>(null) }
    val repository = remember { CommunityRepository(context) }
    val listState = rememberLazyListState()
    LaunchedEffect(comic.id, comic.externalId, entity, tab) { listState.scrollToItem(0) }
    BackHandler { goBack() }
    LaunchedEffect(comic.id, comic.externalId, entity, retry) {
        loading = true; failed = false
        try { detail = withContext(Dispatchers.IO) { entity?.let { ComicVineCatalog.details(it.first, it.second) } ?: ComicVineCatalog.resolve(comic) } }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { failed = true }
        finally { loading = false }
    }
    LaunchedEffect(detail?.id, detail?.type) {
        val original = detail ?: return@LaunchedEffect
        if (original.description.isBlank()) return@LaunchedEffect
        translating = true
        try {
            val translated = ComicVineCatalog.translated(original)
            if (detail?.id == translated.id && detail?.type == translated.type) detail = translated
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
    val effectiveCover = comic.coverUrl.ifBlank { detail?.image.orEmpty() }
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24 } }
    val related = remember(comic.id, comic.externalId, detail?.issues) { ComicDetailPresentation.relatedEditions(comic, detail) }
    Box(Modifier.fillMaxSize().background(t.bgPrimary).windowInsetsPadding(WindowInsets.systemBars).testTag("comic_detail_page")) {
        if (isComic && tab == 3) {
            Column(Modifier.fillMaxSize().padding(top = 68.dp)) {
                DetailTabs(tab, { tab = it })
                CommunityScreen(library, profile, onLogin, comic)
            }
        } else LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            item(key = "detail_header") {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val banner = detail?.banner.orEmpty()
                    val heroHeight = maxWidth / 1.9f
                    if (banner.isNotBlank()) Box(Modifier.fillMaxWidth().height(heroHeight).testTag("comic_detail_banner")) {
                        ProfileImage(banner, null, Modifier.fillMaxSize())
                        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                            0f to t.bgPrimary.copy(alpha = .4f), .4f to Color.Transparent, 1f to t.bgPrimary)))
                    }
                    Column {
                        Spacer(Modifier.height(if (banner.isNotBlank()) (heroHeight - 28.dp).coerceAtLeast(68.dp) else 68.dp))
                        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                            if (isComic) BookCover(effectiveCover, comic.displayTitle, spine3d = false, modifier = Modifier.width(104.dp).aspectRatio(2f / 3f))
                            else ProfileImage(detail?.image.orEmpty(), detail?.title, Modifier.size(110.dp).clip(RoundedCornerShape(22.dp)), face = true)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(if (isComic) comic.displayTitle else detail?.title ?: "Carregando…", color = t.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                                val publisher = detail?.publisher.orEmpty().ifBlank { if (isComic) comic.publisher else "" }
                                if (publisher.isNotBlank()) Text(publisher, color = t.textSecondary, fontSize = 14.sp)
                                val authors = comic.authors.ifBlank { detail?.creators.orEmpty().filter { it.role.contains("writer", true) || it.role.contains("artist", true) }.take(3).joinToString(", ") { it.name } }
                                if (isComic && authors.isNotBlank()) Text(authors, color = t.textSecondary, fontSize = 14.sp)
                                if (isComic) {
                                    rating?.takeIf { it.count > 0 }?.let { Text("★ ${String.format(java.util.Locale.getDefault(), "%.1f", it.average)} · ${it.count} avaliações", color = t.textPrimary, fontSize = 13.sp) }
                                    TextButton(onClick = {
                                        onRegister(comic.copy(coverUrl = effectiveCover,
                                            pageCount = detail?.pageCount?.takeIf { it > 0 } ?: comic.pageCount,
                                            isbn = comic.isbn.ifBlank { detail?.isbn.orEmpty() }))
                                    }, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Registrar leitura", color = t.accent) }
                                }
                            }
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
                if (description.isNotBlank()) item {
                    DetailSection(if (isComic) "Sinopse" else "Biografia") {
                        DetailDescription(description, translating, expandedDescription, { expandedDescription = !expandedDescription })
                    }
                }
                item {
                    DetailSection("Detalhes") {
                        val rows = if (isComic) listOf("Publicação" to detail?.date.orEmpty().ifBlank { comic.releaseYear.toString() },
                            "Formato" to detail?.format.orEmpty().ifBlank { comic.format }, "ISBN" to comic.isbn.ifBlank { detail?.isbn.orEmpty() },
                            "Páginas" to (detail?.pageCount?.takeIf { it > 0 } ?: comic.pageCount).takeIf { it > 0 }?.toString().orEmpty())
                        else listOf("Nome" to detail?.realName.orEmpty(), "Também conhecido como" to detail?.aliases.orEmpty(),
                            "Aparições" to detail?.appearances?.takeIf { it > 0 }?.toString().orEmpty(), "Poderes" to detail?.powers.orEmpty().joinToString(", "))
                        rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                            Text(label, color = t.textSecondary, fontSize = 12.sp)
                            Text(value, color = t.textPrimary, fontSize = 15.sp)
                            HorizontalDivider(color = t.divider)
                        }
                    }
                }
                if (related.isNotEmpty()) item {
                    DetailSection(if (isComic) "Edições relacionadas" else "Aparições em quadrinhos") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(related, key = { it.externalId.ifBlank { it.id } }) { issue ->
                                Column(Modifier.width(104.dp).clickable { openComic(issue) }) {
                                    BookCover(issue.coverUrl, issue.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                                    Text(issue.displayTitle, color = t.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                val credits = (if (tab == 1) detail?.characters.orEmpty() else detail?.creators.orEmpty()).distinctBy { it.id }
                if (detail?.creditsScope == "series") item { Text("Personagens e equipe da série", color = t.textSecondary, modifier = Modifier.padding(horizontal = 20.dp), fontSize = 12.sp) }
                items(credits, key = { it.id }) { credit ->
                    val type = if (tab == 1) "character" else "person"
                    CreditRow(credit, type) { openEntity(type, credit.id) }
                }
                if (!loading && credits.isEmpty()) item { Text("Ainda não há informações desta edição no catálogo.", color = t.textSecondary, modifier = Modifier.padding(horizontal = 20.dp)) }
            }
        }
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth().heightIn(min = 60.dp)
            .background(Brush.verticalGradient(listOf(t.bgPrimary.copy(alpha = .96f), t.bgPrimary.copy(alpha = .65f), Color.Transparent)))
            .padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingsBackButton({ goBack() }, collapsed = collapsed, label = if (isComic) "Voltar" else "HQ")
            Spacer(Modifier.weight(1f))
            if (isComic) IconButton(onClick = onShare) { Icon(LucideIcons.ShareIos, "Compartilhar", tint = t.accent) }
        }
    }
    if (editionPicker) EditionChooser(comic.displayTitle, onClose = { editionPicker = false }) { edition ->
        comic = comic.copy(externalId = edition.externalId, sourceLabel = edition.sourceLabel, publisher = edition.publisher.ifBlank { comic.publisher })
        editionPicker = false
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
    var image by remember(type, credit.id) { mutableStateOf(CatalogPortraits.cached(type, credit.id).orEmpty()) }
    LaunchedEffect(type, credit.id) {
        try { image = CatalogPortraits.image(type, credit.id) }
        catch (cancel: CancellationException) { throw cancel }
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)).background(t.bgSecondary), contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditionChooser(initialQuery: String, onClose: () -> Unit, onChoose: (ComicEntry) -> Unit) {
    val t = LocalIOS27Theme.current
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
    ModalBottomSheet(onDismissRequest = onClose, containerColor = t.bgSecondary) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp).imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SectionHeading("Edição", "Escolha a edição correspondente para ver personagens e equipe.") }
            item { AppleTextField(query, { query = it.take(100) }, "Buscar edição") }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = t.accent) }
            if (failed) item { Text("Não foi possível buscar. Tente outro termo.", color = t.textSecondary) }
            items(entries.distinctBy { it.id }, key = { it.id }) { entry ->
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
