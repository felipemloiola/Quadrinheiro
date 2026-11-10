package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderProfileScreen(comics: List<ComicEntry>, profile: ReaderProfile, settings: AppSettings,
    listState: LazyListState, onSettings: () -> Unit, onLibrary: () -> Unit,
    onEditProfile: () -> Unit, onUpdateShowcase: (List<String>, List<String>, Map<String, String>) -> Unit,
    onCommitProfile: (ReaderProfile, String) -> Unit = { _, _ -> },
    onHandleChange: (String) -> Unit = {}, handleFeedback: String = "",
    onComic: (ComicEntry) -> Unit, onStatistics: () -> Unit,
    isGuest: Boolean = false,
    onLogin: () -> Unit = {}
) {
    val theme = LocalIOS27Theme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var slotMenu by remember { mutableStateOf(false) }
    var remoteComics by remember { mutableStateOf(emptyList<ComicEntry>()) }
    val remoteCatalog by com.example.data.ModernCatalog.entries.collectAsState()
    LaunchedEffect(Unit) { com.example.data.CharacterDirectory.preload(context) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var characterResults by remember { mutableStateOf(emptyList<com.example.data.CharacterResult>()) }
    var characterLoading by remember { mutableStateOf(false) }
    var characterError by remember { mutableStateOf(false) }
    var editSlot by rememberSaveable { mutableIntStateOf(-1) }
    var characters by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val candidates = remember(comics, settings.profileShowcaseDetails, remoteComics, remoteCatalog) {
        val snapshots = settings.profileShowcaseDetails.filterKeys { it.startsWith("comic:") }.values.mapNotNull(com.example.data.ProfileShowcase::restoreComic)
        com.example.data.CatalogSearchPolicy.unique(
            snapshots + comics + remoteComics + remoteCatalog + com.example.data.CuratedCatalog.curatedComicsCatalog
        )
    }
    val byId = remember(candidates) { candidates.associateBy { it.id } }
    val readCount = remember(comics) { comics.count { it.status == ReadingStatus.READ } }
    val reviews = remember(comics) { comics.filter { it.review.isNotBlank() }.sortedByDescending { it.updatedAt }.take(3) }
    LaunchedEffect(settings.profileFavoriteCharacters, settings.profileShowcaseDetails) {
        val missing = settings.profileFavoriteCharacters.filter { settings.profileShowcaseDetails["character:$it"].isNullOrBlank() || settings.profileShowcaseDetails["character:$it"]?.trimStart()?.startsWith("{") != true }
        if (missing.isEmpty()) return@LaunchedEffect
        val details = settings.profileShowcaseDetails.toMutableMap()
        try {
            for (name in missing) com.example.data.CharacterDirectory.search(context, name).firstOrNull()?.let { details["character:$name"] = com.example.data.ProfileShowcase.characterSnapshot(it) }
            if (details != settings.profileShowcaseDetails) onUpdateShowcase(settings.profileFavoriteComicIds, settings.profileFavoriteCharacters, details)
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { }
    }
    var comicLoading by remember { mutableStateOf(false) }
    val multiSearchEngine = remember { com.example.data.MultiApiSearchEngine() }
    LaunchedEffect(editSlot, characters, query, slotMenu) {
        if (editSlot < 0 || slotMenu) return@LaunchedEffect
        if (!characters) {
            val trimmed = query.trim()
            if (trimmed.length >= 2) {
                comicLoading = true
                try {
                    kotlinx.coroutines.delay(180)
                    val preferredLang = if ("PT" in settings.searchLanguage) "PT" else "ALL"
                    val collected = mutableListOf<ComicEntry>()
                    multiSearchEngine.searchOnlineProgressive(
                        query = trimmed,
                        searchLanguage = settings.searchLanguage,
                        provider = settings.searchProvider,
                        hdCovers = settings.hdCovers
                    ).collect { partial ->
                        collected.clear()
                        collected.addAll(partial)
                        remoteComics = com.example.data.CatalogSearchPolicy.filterAndRankForQuery(
                            collected,
                            trimmed,
                            preferredLang
                        )
                    }
                    if (collected.isEmpty()) {
                        val terms = com.example.data.CatalogSearchPolicy.searchTerms(trimmed)
                        for (term in terms.take(2)) {
                            runCatching { com.example.data.ComicVineCatalog.search(term).items }.getOrNull()?.let { collected += it }
                        }
                        remoteComics = com.example.data.CatalogSearchPolicy.filterAndRankForQuery(
                            collected,
                            trimmed,
                            preferredLang
                        )
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                } finally {
                    comicLoading = false
                }
            } else {
                remoteComics = emptyList()
                comicLoading = false
            }
            return@LaunchedEffect
        }
        characterLoading = true; characterError = false
        try { kotlinx.coroutines.delay(180); characterResults = com.example.data.CharacterDirectory.search(context, query) }
        catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { characterError = true }
        finally { characterLoading = false }
    }
    if (editorOpen) {
        ProfileEditorScreen(profile, settings, { editorOpen = false }, onCommitProfile, onHandleChange, handleFeedback, { isCharacter, slot ->
            characters = isCharacter; editSlot = slot
            slotMenu = (if (isCharacter) settings.profileFavoriteCharacters else settings.profileFavoriteComicIds).getOrNull(slot) != null
            query = ""
        })
    } else LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("reader_profile"),
        contentPadding = PaddingValues(bottom = 208.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item(key = "profile_header") {
            Column(Modifier.fillMaxWidth().background(theme.bgPrimary)) {
                val hasBanner = settings.profileBannerUrl.isNotBlank()
                val bannerHeight = if (hasBanner) 144.dp else 88.dp
                val avatarSize = 80.dp
                val halfAvatar = 40.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeight + halfAvatar)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
                            .align(Alignment.TopCenter)
                    ) {
                        if (hasBanner) {
                            ProfileImage(
                                settings.profileBannerUrl,
                                "Banner de " + profile.name,
                                Modifier.fillMaxSize().testTag("profile_banner")
                            )
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        0.00f to Color.Black.copy(alpha = 0.28f),
                                        0.38f to Color.Transparent,
                                        0.68f to theme.bgPrimary.copy(alpha = 0.35f),
                                        0.88f to theme.bgPrimary.copy(alpha = 0.78f),
                                        1.00f to theme.bgPrimary
                                    )
                                )
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Perfil",
                                color = if (hasBanner) Color.White else theme.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                            IconButton(
                                onClick = onSettings,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("profile_settings")
                                    .background(
                                        if (hasBanner) Color.Black.copy(alpha = 0.38f) else theme.bgSecondary.copy(alpha = 0.85f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    LucideIcons.Settings,
                                    "Ajustes",
                                    tint = if (hasBanner) Color.White else theme.textPrimary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp)
                            .size(avatarSize)
                            .clip(CircleShape)
                            .background(theme.bgPrimary)
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .testTag("profile_avatar")
                    ) {
                        UserAvatar(profile, sizeDp = 75)
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(halfAvatar)
                            .padding(start = 112.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile.name,
                            color = theme.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                        )
                        val editProfileInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .pressScaleFeedback(editProfileInteraction, targetScale = 0.96f)
                                .clip(CircleShape)
                                .background(theme.textPrimary.copy(alpha = if (theme.isDark) 0.10f else 0.07f))
                                .clickable(
                                    interactionSource = editProfileInteraction,
                                    indication = null
                                ) {
                                    if (isGuest) onLogin() else editorOpen = true
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Editar perfil",
                                color = theme.textPrimary.copy(alpha = 0.88f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (profile.handle.isNotBlank()) {
                        Text(profile.handle, color = theme.textSecondary, fontSize = 14.sp)
                    }
                    if (profile.bio.isNotBlank()) {
                        Text(profile.bio, color = theme.textSecondary, fontSize = 15.sp, lineHeight = 21.sp)
                    }
                    Row(Modifier.fillMaxWidth().clickable(onClick = onStatistics).padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        ProfileMetric("$readCount", "Lidos", Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(36.dp).background(theme.divider))
                        ProfileMetric("${comics.count { it.status == ReadingStatus.READING }}", "Em leitura", Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(36.dp).background(theme.divider))
                        ProfileMetric("${comics.count { it.rating > 0 }}", "Avaliações", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("HQs favoritas", "Escolha quatro histórias que dizem quem você é.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { slot ->
                        val comic = settings.profileFavoriteComicIds.getOrNull(slot)?.let(byId::get)
                        Column(Modifier.weight(1f).testTag("favorite-comic-$slot"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(10.dp)).background(theme.bgSecondary)
                                .clickable {
                                    if (isGuest) {
                                        onLogin()
                                    } else {
                                        characters = false
                                        editSlot = slot
                                        slotMenu = settings.profileFavoriteComicIds.getOrNull(slot) != null
                                        query = ""
                                    }
                                }, contentAlignment = Alignment.Center) {
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
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Personagens favoritos")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { slot ->
                        val name = settings.profileFavoriteCharacters.getOrNull(slot)
                        Column(Modifier.weight(1f).clickable {
                            if (isGuest) {
                                onLogin()
                            } else {
                                characters = true
                                editSlot = slot
                                slotMenu = settings.profileFavoriteCharacters.getOrNull(slot) != null
                                query = ""
                            }
                        }
                            .testTag("favorite-character-$slot"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(theme.bgSecondary), contentAlignment = Alignment.Center) {
                                if (name == null) Icon(Icons.Default.Add, "Escolher personagem ${slot + 1}", tint = theme.textTertiary)
                                else if (!settings.profileShowcaseDetails["character:$name"].isNullOrBlank()) ProfileImage(settings.profileShowcaseDetails["character:$name"].orEmpty(), name, Modifier.fillMaxSize(), face = true)
                                else Text(name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }, color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Text(name ?: "Escolher", color = theme.textSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().contentMaterialSurface(RoundedCornerShape(22.dp))) {
                ProfileLink("Biblioteca", "Estante e diário", Icons.AutoMirrored.Filled.MenuBook, onLibrary)
            }
        }
        if (reviews.isNotEmpty()) {
            item { Box(Modifier.padding(horizontal = 20.dp)) { SectionHeading("Suas resenhas") } }
            items(reviews, key = { "review-${it.id}" }) { comic ->
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().contentMaterialSurface(RoundedCornerShape(20.dp)).clickable { onComic(comic) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(comic.displayTitle, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                    Text(comic.review, color = theme.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                }
            }
        }
    }
    if (editSlot >= 0 && slotMenu) {
        val name = settings.profileFavoriteCharacters.getOrNull(editSlot)
        val selected = settings.profileFavoriteComicIds.getOrNull(editSlot)?.let(byId::get)
        FavoriteArtworkSheet(if (characters) null else selected, if (characters) name else null, settings.profileShowcaseDetails["character:$name"].orEmpty(),
            onClose = { editSlot = -1 }, onReplace = { slotMenu = false; query = "" }, onRemove = {
                val ids = settings.profileFavoriteComicIds.toMutableList(); val chars = settings.profileFavoriteCharacters.toMutableList()
                if (characters) chars.removeAt(editSlot) else ids.removeAt(editSlot)
                onUpdateShowcase(ids, chars, settings.profileShowcaseDetails.filterKeys { k -> ids.any { k == "comic:$it" } || chars.any { k == "character:$it" } }); editSlot = -1
            }, onChoose = { image ->
                val details = settings.profileShowcaseDetails.toMutableMap()
                if (characters && name != null) {
                    val old = runCatching { org.json.JSONObject(details["character:$name"].orEmpty()) }.getOrDefault(org.json.JSONObject())
                    old.put("image", image); details["character:$name"] = old.toString()
                } else selected?.let { details["comic:${it.id}"] = com.example.data.ProfileShowcase.comicSnapshot(it.copy(coverUrl = image)) }
                onUpdateShowcase(settings.profileFavoriteComicIds, settings.profileFavoriteCharacters, details); editSlot = -1
            })
    }
    if (editSlot >= 0 && !slotMenu) {
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
                        if (characters && portrait != null) details["character:$value"] = characterResults.firstOrNull { it.name.take(60) == value }?.let(com.example.data.ProfileShowcase::characterSnapshot) ?: portrait
                        if (!characters) byId[value]?.let { details["comic:$value"] = com.example.data.ProfileShowcase.comicSnapshot(it) }
                    }
                    val retained = details.filterKeys { key -> nextIds.any { key == "comic:$it" } || nextCharacters.any { key == "character:$it" } }
                    onUpdateShowcase(nextIds, nextCharacters, retained); close()
                }
                if (editSlot < (if (characters) settings.profileFavoriteCharacters else settings.profileFavoriteComicIds).size) TextButton(onClick = { select(null) }) { Text("Remover dos favoritos", color = theme.accent) }
                val excluded = (if (characters) settings.profileFavoriteCharacters else settings.profileFavoriteComicIds).filterIndexed { index, _ -> index != editSlot }.toSet()
                val preferredLang = if ("PT" in settings.searchLanguage) "PT" else "EN"
                val shownComics = remember(candidates, excluded, query, preferredLang) {
                    val available = candidates.filter { it.id !in excluded }
                    if (query.isBlank()) {
                        available.take(80)
                    } else {
                        com.example.data.CatalogSearchPolicy.filterAndRankForQuery(
                            available,
                            query,
                            preferredLang
                        ).take(80)
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                    if (characters) {
                        if (characterLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = theme.accent) }
                        if (characterError) item { Text("Não foi possível carregar os personagens. Confira sua conexão e tente outra busca.", color = theme.textSecondary) }
                        items(characterResults.filter { it.name.take(60) !in excluded }, key = { it.id }) { result ->
                            Row(Modifier.fillMaxWidth().clickable { select(result.name.take(60), result.image) }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                ProfileImage(result.image, result.name, Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)), face = true)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(result.name, color = theme.textPrimary, fontSize = 15.sp)
                                    if (!result.name.contains(result.originalName, true)) Text(result.originalName, color = theme.textSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        if (!characterLoading && !characterError && characterResults.isEmpty()) item { Text("Nenhum personagem encontrado. Tente o nome em português ou inglês.", color = theme.textSecondary) }
                    } else {
                        if (comicLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = theme.accent) }
                        items(shownComics, key = { it.id }) { comic ->
                            val metaLine = remember(comic.authors, comic.publisher, comic.releaseYear, comic.pageCount) {
                                listOfNotNull(
                                    comic.authors.takeIf { it.isNotBlank() },
                                    comic.publisher.takeIf { it.isNotBlank() },
                                    comic.releaseYear.takeIf { it > 0 }?.toString(),
                                    comic.pageCount.takeIf { it > 0 }?.let { "$it págs" }
                                ).joinToString(" • ")
                            }
                            Row(Modifier.fillMaxWidth().clickable { select(comic.id) }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                BookCover(comic.coverUrl, comic.displayTitle, spine3d = false, modifier = Modifier.size(44.dp, 66.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(comic.displayTitle, color = theme.textPrimary, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                                    if (metaLine.isNotEmpty()) {
                                        Text(metaLine, color = theme.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        if (!comicLoading && shownComics.isEmpty()) item { Text("Nenhuma HQ encontrada. Tente outro título.", color = theme.textSecondary) }
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
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScaleFeedback(interactionSource, targetScale = 0.985f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = t.accent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = t.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textTertiary)
    }
}
