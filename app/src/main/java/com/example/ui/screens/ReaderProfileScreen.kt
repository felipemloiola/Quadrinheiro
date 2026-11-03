package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CuratedCatalog
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderProfileScreen(comics: List<ComicEntry>, profile: ReaderProfile, settings: AppSettings,
    listState: LazyListState, onSettings: () -> Unit, onLibrary: () -> Unit,
    onEditProfile: () -> Unit, onUpdateShowcase: (List<String>, List<String>, Map<String, String>) -> Unit,
    onCommitProfile: (ReaderProfile, String) -> Unit = { _, _ -> },
    onComic: (ComicEntry) -> Unit, onStatistics: () -> Unit) {
    val theme = LocalIOS27Theme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var characterResults by remember { mutableStateOf(emptyList<com.example.data.CharacterResult>()) }
    var characterLoading by remember { mutableStateOf(false) }
    var characterError by remember { mutableStateOf(false) }
    var editSlot by rememberSaveable { mutableIntStateOf(-1) }
    var characters by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val candidates = remember(comics, settings.profileShowcaseDetails) {
        val snapshots = settings.profileShowcaseDetails.filterKeys { it.startsWith("comic:") }.values.mapNotNull(com.example.data.ProfileShowcase::restoreComic)
        (comics + snapshots + CuratedCatalog.curatedComicsCatalog).distinctBy { it.id }
    }
    val byId = remember(candidates) { candidates.associateBy { it.id } }
    val readCount = remember(comics) { comics.count { it.status == ReadingStatus.READ } }
    val reviews = remember(comics) { comics.filter { it.review.isNotBlank() }.sortedByDescending { it.updatedAt }.take(3) }
    LaunchedEffect(settings.profileFavoriteCharacters, settings.profileShowcaseDetails) {
        val missing = settings.profileFavoriteCharacters.filter { settings.profileShowcaseDetails["character:$it"].isNullOrBlank() }
        if (missing.isEmpty()) return@LaunchedEffect
        val details = settings.profileShowcaseDetails.toMutableMap()
        try {
            for (name in missing) com.example.data.CharacterDirectory.search(context, name).firstOrNull()?.let { details["character:$name"] = it.image }
            if (details != settings.profileShowcaseDetails) onUpdateShowcase(settings.profileFavoriteComicIds, settings.profileFavoriteCharacters, details)
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { /* Existing choices remain visible while offline. */ }
    }
    LaunchedEffect(editSlot, characters, query) {
        if (editSlot < 0 || !characters) return@LaunchedEffect
        characterLoading = true; characterError = false
        try {
            kotlinx.coroutines.delay(180)
            characterResults = com.example.data.CharacterDirectory.search(context, query)
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (e: Exception) { characterError = true }
        finally { characterLoading = false }
    }
    if (editorOpen) {
        ProfileEditorScreen(profile, settings, { editorOpen = false }, onCommitProfile)
        return
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("reader_profile"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 208.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Perfil", color = theme.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onSettings, modifier = Modifier.testTag("profile_settings")) {
                    Icon(LucideIcons.Settings, "Ajustes", tint = theme.textPrimary)
                }
            }
        }
        if (settings.profileBannerUrl.isNotBlank()) item {
            ProfileImage(settings.profileBannerUrl, "Banner de "+profile.name,
                Modifier.fillMaxWidth().aspectRatio(3f).clip(RoundedCornerShape(20.dp)).testTag("profile_banner"))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UserAvatar(profile, sizeDp = 72)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(profile.name, color = theme.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(profile.handle, color = theme.textSecondary, fontSize = 14.sp)
                        Text("Editar perfil", color = theme.accent, fontSize = 14.sp,
                            modifier = Modifier.clickable { editorOpen = true }.padding(vertical = 10.dp))
                    }
                }
                if (profile.bio.isNotBlank()) Text(profile.bio, color = theme.textSecondary, fontSize = 15.sp, lineHeight = 21.sp)
                Row(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(20.dp)).clickable(onClick = onStatistics).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround) {
                    ProfileMetric("$readCount", "Lidos", Modifier.weight(1f))
                    ProfileMetric("${comics.count { it.status == ReadingStatus.READING }}", "Em leitura", Modifier.weight(1f))
                    ProfileMetric("${comics.count { it.rating > 0 }}", "Avaliações", Modifier.weight(1f))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("HQs favoritas", "Escolha quatro histórias que dizem quem você é.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { slot ->
                        val comic = settings.profileFavoriteComicIds.getOrNull(slot)?.let(byId::get)
                        Column(Modifier.weight(1f).testTag("favorite-comic-$slot"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(10.dp))
                                .background(theme.bgSecondary).clickable { characters = false; editSlot = slot; query = "" }, contentAlignment = Alignment.Center) {
                                if (comic != null) BookCover(comic.coverUrl, comic.displayTitle, modifier = Modifier.fillMaxSize())
                                else Icon(Icons.Default.Add, "Escolher HQ favorita ${slot + 1}", tint = theme.textTertiary)
                            }
                            Text(comic?.displayTitle ?: "Escolher", color = theme.textSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Personagens favoritos")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { slot ->
                        val name = settings.profileFavoriteCharacters.getOrNull(slot)
                        Column(Modifier.weight(1f).clickable { characters = true; editSlot = slot; query = "" }
                            .testTag("favorite-character-$slot"), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(theme.bgSecondary), contentAlignment = Alignment.Center) {
                                if (name == null) Icon(Icons.Default.Add, "Escolher personagem ${slot + 1}", tint = theme.textTertiary)
                                else if (!settings.profileShowcaseDetails["character:$name"].isNullOrBlank()) ProfileImage(settings.profileShowcaseDetails["character:$name"].orEmpty(), name, Modifier.fillMaxSize(), face = true)
                                else Text(name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }, color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Text(name ?: "Escolher", color = theme.textSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (settings.profileShowcaseDetails.filterKeys { it.startsWith("character:") }.values.any { it.contains("comicvine.gamespot.com") }) {
                    Text("Comic Vine ↗", color = theme.textTertiary, fontSize = 11.sp,
                        modifier = Modifier.clickable { uriHandler.openUri("https://comicvine.gamespot.com/") }.padding(vertical = 6.dp))
                }
            }
        }
        item {
            Column(Modifier.contentMaterialSurface(RoundedCornerShape(22.dp))) {
                ProfileLink("Biblioteca", "Sua coleção e seu diário", Icons.AutoMirrored.Filled.MenuBook, onLibrary)
            }
        }
        if (reviews.isNotEmpty()) {
            item { SectionHeading("Suas resenhas") }
            items(reviews, key = { "review-${it.id}" }) { comic ->
                Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(20.dp)).clickable { onComic(comic) }.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(comic.displayTitle, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                    Text(comic.review, color = theme.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                }
            }
        }
    }
    if (editSlot >= 0) {
        val close = { editSlot = -1; query = "" }
        ModalBottomSheet(onDismissRequest = close, containerColor = theme.bgSecondary) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (characters) "Personagem favorito" else "HQ favorita", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    TextButton(onClick = close) { Text("Fechar", color = theme.accent) }
                }
                AppleTextField(query, { query = it.take(100) }, "Buscar", leadingIcon = Icons.Default.Search)
                fun select(value: String?, portrait: String? = null) {
                    val old = if (characters) settings.profileFavoriteCharacters else settings.profileFavoriteComicIds
                    val next = old.toMutableList()
                    if (value == null) { if (editSlot < next.size) next.removeAt(editSlot) }
                    else { if (editSlot < next.size) next[editSlot] = value else next.add(value) }
                    val clean = next.filter { it.isNotBlank() }.distinct().take(4)
                    val nextIds = if (characters) settings.profileFavoriteComicIds else clean
                    val nextCharacters = if (characters) clean else settings.profileFavoriteCharacters
                    val details = settings.profileShowcaseDetails.toMutableMap()
                    if (value != null) {
                        if (characters && portrait != null) details["character:$value"] = portrait
                        if (!characters) byId[value]?.let { details["comic:$value"] = com.example.data.ProfileShowcase.comicSnapshot(it) }
                    }
                    val retained = details.filterKeys { key -> nextIds.any { key == "comic:$it" } || nextCharacters.any { key == "character:$it" } }
                    onUpdateShowcase(if (characters) settings.profileFavoriteComicIds else clean,
                        if (characters) clean else settings.profileFavoriteCharacters, retained)
                    close()
                }
                if (editSlot < (if (characters) settings.profileFavoriteCharacters else settings.profileFavoriteComicIds).size) {
                    TextButton(onClick = { select(null) }) { Text("Remover dos favoritos", color = theme.accent) }
                }
                val excluded = (if (characters) settings.profileFavoriteCharacters else settings.profileFavoriteComicIds).filterIndexed { index, _ -> index != editSlot }.toSet()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                    if (characters) {
                        if (characterLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = theme.accent) }
                        if (characterError) item { Text("Não foi possível carregar os personagens. Confira sua conexão e tente outra busca.", color = theme.textSecondary) }
                        items(characterResults.filter { it.name.take(60) !in excluded }, key = { it.id }) { result ->
                            Row(Modifier.fillMaxWidth().clickable { select(result.name.take(60), result.image) }.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                ProfileImage(result.image, result.name, Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)), face = true)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(result.name, color = theme.textPrimary, fontSize = 15.sp)
                                    if (!result.name.contains(result.originalName, true)) Text(result.originalName, color = theme.textSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        if (!characterLoading && !characterError && characterResults.isEmpty()) item { Text("Nenhum personagem encontrado. Tente o nome em português ou inglês.", color = theme.textSecondary) }
                    } else {
                        val shown = candidates.filter { it.id !in excluded && (it.displayTitle.contains(query, true) || it.authors.contains(query, true)) }.take(80)
                        items(shown, key = { it.id }) { comic ->
                            Row(Modifier.fillMaxWidth().clickable { select(comic.id) }.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                BookCover(comic.coverUrl, comic.displayTitle, spine3d = false, modifier = Modifier.size(40.dp, 60.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(comic.displayTitle, color = theme.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                                    Text(comic.authors, color = theme.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                }
                            }
                        }
                        if (shown.isEmpty()) item { Text("Nenhuma HQ encontrada no catálogo local.", color = theme.textSecondary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val t = LocalIOS27Theme.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, color = t.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = t.textSecondary, fontSize = 12.sp)
    }
}

@Composable
internal fun SectionHeading(title: String, subtitle: String? = null) {
    val t = LocalIOS27Theme.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = t.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, color = t.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ProfileLink(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val t = LocalIOS27Theme.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = t.accent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = t.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textTertiary)
    }
}
