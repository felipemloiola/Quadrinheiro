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
import com.example.data.PersonalizedDiscovery
import com.example.model.ComicEntry
import com.example.ui.components.BookCover
import com.example.ui.components.contentMaterialSurface
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalAppMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.ReaderProfile
import com.example.ui.components.AppleSegmentedPicker

@Composable
fun ExploreScreen(comics: List<ComicEntry>, listState: LazyListState,
    onComic: (ComicEntry) -> Unit, onSearch: (String) -> Unit,
    profile: ReaderProfile = ReaderProfile(), onLogin: () -> Unit = {}) {
    val theme = LocalIOS27Theme.current
    val motion = LocalAppMotion.current
    var collection by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val catalog by com.example.data.ModernCatalog.entries.collectAsState()
    LaunchedEffect(Unit){com.example.data.ModernCatalog.load()}
    val recommendations by produceState<List<ComicEntry>>(emptyList(), comics, profile.favoriteUniverse,catalog) {
        value = withContext(Dispatchers.Default) { PersonalizedDiscovery.recommend(catalog, comics, profile.favoriteUniverse) }
    }
    val collections = remember(catalog, recommendations) { linkedMapOf(
        "Para você" to recommendations,
        "Para começar" to catalog.filter { it.displayTitle.contains("Ano Um", true) || it.displayTitle.contains("Watchmen", true) || it.displayTitle.contains("Maus", true) || it.displayTitle.contains("Sandman", true) }.ifEmpty { catalog.take(12) },
        "Universo DC" to catalog.filter { it.publisher.contains("DC", true) || it.publisher.contains("Vertigo", true) || it.displayTitle.contains("Batman", true) },
        "Universo Marvel" to catalog.filter { it.publisher.contains("Marvel", true) || it.displayTitle.contains("Aranha", true) || it.displayTitle.contains("Demolidor", true) }
    ) }
    BackHandler(collection != null) { collection = null }
    Column(Modifier.fillMaxSize()) {
        if(collection==null) Column(Modifier.padding(horizontal=20.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("Explorar",Modifier.weight(1f),color=theme.textPrimary,fontWeight=FontWeight.Bold,fontSize=32.sp)
                IconButton(onClick={onSearch("")}){Icon(Icons.Default.Search,"Buscar quadrinhos",tint=theme.textPrimary)}
            }
            AppleSegmentedPicker(listOf("Descobrir","Comunidade"),tab,{tab=it},Modifier.fillMaxWidth())
        }
        if(collection==null&&tab==1) CommunityScreen(comics,profile,onLogin) else
    AnimatedContent(collection, transitionSpec = { fadeIn(motion.tween(180)) togetherWith fadeOut(motion.tween(120)) }, label = "explore_collection") { selected ->
        val expandedState = rememberLazyListState()
        LazyColumn(state = if (selected == null) listState else expandedState,
            modifier = Modifier.fillMaxSize().testTag(if (selected == null) "explore_page" else "explore_collection"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 208.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)) {
            if(selected!=null) item {
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
                        row.forEach { comic -> ExplorePoster(comic, Modifier.weight(1f), onComic) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (entries.isEmpty()) item { Text("Novas seleções aparecerão aqui conforme o catálogo crescer.", color = theme.textSecondary) }
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
                                items(entries.take(8), key = { it.id }) { comic -> ExplorePoster(comic, Modifier.width(112.dp), onComic) }
                            }
                        }
                    }
                }

            }
        }
    }
    }
}

@Composable
private fun ExplorePoster(comic: ComicEntry, modifier: Modifier, onComic: (ComicEntry) -> Unit) {
    val t = LocalIOS27Theme.current
    Column(modifier.clickable { onComic(comic) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BookCover(comic.coverUrl, comic.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        Text(comic.displayTitle, color = t.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(comic.authors, color = t.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

