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
import com.example.data.ProfileShowcase
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle

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
    var slotMenu by rememberSaveable { mutableStateOf(false) }
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
    val favoriteValues = if (characters) settings.profileFavoriteCharacters else settings.profileFavoriteComicIds
    val currentFavorite = favoriteValues.getOrNull(editSlot)
    val currentComic = if (characters) null else currentFavorite?.let(byId::get)
    val editingArtwork = slotMenu && if (characters) currentFavorite != null else currentComic != null

    fun openFavorite(isCharacter: Boolean, slot: Int) {
        characters = isCharacter
        editSlot = slot
        slotMenu = if (isCharacter) {
            settings.profileFavoriteCharacters.getOrNull(slot) != null
        } else {
            settings.profileFavoriteComicIds.getOrNull(slot)?.let(byId::containsKey) == true
        }
        query = ""
        characterResults = emptyList()
        remoteComics = emptyList()
    }

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
    LaunchedEffect(editSlot, characters, query, editingArtwork) {
        if (editSlot < 0 || editingArtwork) return@LaunchedEffect
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
        ProfileEditorScreen(profile, settings, { editorOpen = false }, onCommitProfile, onHandleChange, handleFeedback, ::openFavorite)
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
                    val profileBannerBackdrop = rememberLayerBackdrop()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
                            .align(Alignment.TopCenter)
                    ) {
                        if (hasBanner) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(profileBannerBackdrop)
                            ) {
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
                            LiquidGlassCircleButton(
                                icon = LucideIcons.Settings,
                                contentDescription = "Ajustes",
                                onClick = onSettings,
                                size = 44.dp,
                                iconSize = 20.dp,
                                tint = if (hasBanner) Color.White else theme.textPrimary,
                                onDarkScrim = hasBanner,
                                tag = "profile_settings",
                                backdropOverride = if (hasBanner) profileBannerBackdrop else null
                            )
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
                        LiquidGlassPillButton(
                            text = "Editar perfil",
                            onClick = { if (isGuest) onLogin() else editorOpen = true },
                            minHeight = 44.dp,
                            horizontalPadding = 16.dp,
                            verticalPadding = 10.dp
                        )
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
            val comicSlotShape = remember { ContinuousRoundedRectangle(14.dp) }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("HQs favoritas", "Escolha quatro histórias que dizem quem você é.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { slot ->
                        val comic = settings.profileFavoriteComicIds.getOrNull(slot)?.let(byId::get)
                        val slotInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        Column(
                            Modifier
                                .weight(1f)
                                .pressScaleFeedback(slotInteraction, targetScale = 0.96f)
                                .testTag("favorite-comic-$slot"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f)
                                    .contentMaterialSurface(comicSlotShape)
                                    .clickable(
                                        interactionSource = slotInteraction,
                                        indication = null
                                    ) {
                                        if (isGuest) {
                                            onLogin()
                                        } else {
                                            openFavorite(false, slot)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (comic != null) BookCover(comic.coverUrl, comic.displayTitle, modifier = Modifier.fillMaxSize().clip(comicSlotShape))
                                else Icon(Icons.Default.Add, "Escolher HQ favorita ${slot + 1}", tint = theme.textTertiary)
                            }
                            Text(comic?.displayTitle ?: "Escolher", color = theme.textSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            val characterSlotShape = remember { ContinuousRoundedRectangle(16.dp) }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Personagens favoritos")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { slot ->
                        val name = settings.profileFavoriteCharacters.getOrNull(slot)
                        val slotInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        Column(
                            Modifier
                                .weight(1f)
                                .pressScaleFeedback(slotInteraction, targetScale = 0.96f)
                                .clickable(
                                    interactionSource = slotInteraction,
                                    indication = null
                                ) {
                                    if (isGuest) {
                                        onLogin()
                                    } else {
                                        openFavorite(true, slot)
                                    }
                                }
                                .testTag("favorite-character-$slot"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .contentMaterialSurface(characterSlotShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (name == null) Icon(Icons.Default.Add, "Escolher personagem ${slot + 1}", tint = theme.textTertiary)
                                else if (!settings.profileShowcaseDetails["character:$name"].isNullOrBlank()) ProfileImage(settings.profileShowcaseDetails["character:$name"].orEmpty(), name, Modifier.fillMaxSize().clip(characterSlotShape), face = true)
                                else Text(name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }, color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Text(name ?: "Escolher", color = theme.textSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().contentMaterialSurface(ContinuousRoundedRectangle(22.dp))) {
                ProfileLink("Biblioteca", "Estante e diário", Icons.AutoMirrored.Filled.MenuBook, onLibrary)
            }
        }
        if (reviews.isNotEmpty()) {
            item { Box(Modifier.padding(horizontal = 20.dp)) { SectionHeading("Suas resenhas") } }
            items(reviews, key = { "review-${it.id}" }) { comic ->
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().contentMaterialSurface(ContinuousRoundedRectangle(20.dp)).clickable { onComic(comic) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(comic.displayTitle, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                    Text(comic.review, color = theme.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                }
            }
        }
    }
    if (editSlot >= 0) {
        val slot = editSlot
        val close = {
            editSlot = -1
            slotMenu = false
            query = ""
        }
        fun commitFavorite(choice: FavoriteChoice?) {
            val next = favoriteValues.toMutableList()
            if (choice == null) {
                if (slot in next.indices) next.removeAt(slot)
            } else if (slot in next.indices) {
                next[slot] = choice.value
            } else {
                next.add(choice.value)
            }
            val clean = next.filter { it.isNotBlank() }.distinct().take(4)
            val nextIds = if (characters) settings.profileFavoriteComicIds else clean
            val nextCharacters = if (characters) clean else settings.profileFavoriteCharacters
            val details = settings.profileShowcaseDetails.toMutableMap()
            if (choice?.snapshot != null) {
                val type = if (characters) "character" else "comic"
                details["$type:${choice.value}"] = choice.snapshot
            }
            val retainedKeys = nextIds.map { "comic:$it" } + nextCharacters.map { "character:$it" }
            onUpdateShowcase(nextIds, nextCharacters, details.filterKeys { it in retainedKeys })
            close()
        }
        if (editingArtwork) {
            val character = currentFavorite.takeIf { characters }
            val portrait = settings.profileShowcaseDetails["character:$character"].orEmpty()
            FavoriteArtworkSheet(
                comic = currentComic,
                character = character,
                portrait = portrait,
                onClose = close,
                onReplace = { slotMenu = false },
                onRemove = { commitFavorite(null) },
                onChoose = { image ->
                    val details = settings.profileShowcaseDetails.toMutableMap()
                    if (character != null) {
                        val snapshot = runCatching { org.json.JSONObject(portrait) }.getOrDefault(org.json.JSONObject())
                        details["character:$character"] = snapshot.put("image", image).toString()
                    } else if (currentComic != null) {
                        details["comic:${currentComic.id}"] = ProfileShowcase.comicSnapshot(currentComic.copy(coverUrl = image))
                    }
                    onUpdateShowcase(settings.profileFavoriteComicIds, settings.profileFavoriteCharacters, details)
                    close()
                }
            )
        } else {
            val excluded = favoriteValues.filterIndexed { index, _ -> index != slot }.toSet()
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
            val filteredCharacters = remember(characterResults, excluded) {
                characterResults.filter { it.name.take(60) !in excluded }
                    .distinctBy { it.name.take(60) }
            }
            val currentSelection = currentFavorite?.let { value ->
                val title = if (characters) value else currentComic?.displayTitle ?: "HQ atual"
                FavoriteChoice(value, title)
            }
            FavoriteSelectionSheet(
                characters = characters,
                query = query,
                onQueryChange = {
                    query = it
                    characterResults = emptyList()
                    remoteComics = emptyList()
                },
                currentSelection = currentSelection,
                characterLoading = characterLoading,
                characterError = characterError,
                characterResults = filteredCharacters,
                comicLoading = comicLoading,
                shownComics = shownComics,
                onConfirm = ::commitFavorite,
                onClose = close
            )
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
