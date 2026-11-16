package com.example.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.*
import com.example.model.ComicEntry
import com.example.ui.theme.LocalIOS27Theme

/** Original pixels, one bounded Coil request. No backdrop capture or per-frame effects. */
@Composable
internal fun ListArtwork(list: ComicList, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cover = list.coverItem()?.cover.orEmpty().replace("http://", "https://")
    val request = remember(context, cover) {
        ImageRequest.Builder(context).data(cover).size(1024, 1024).allowHardware(true).crossfade(false)
            .diskCachePolicy(LibraryDiskCacheRegistry.diskCachePolicyFor(cover)).build()
    }
    Box(modifier.clipToBounds().background(Color(0xFF111113))) {
        if (cover.isNotBlank()) AsyncImage(request, null, Modifier.fillMaxSize().graphicsLayer {
            scaleX = list.coverZoom; scaleY = list.coverZoom
            translationX = (.5f - list.coverX) * size.width * (list.coverZoom - 1f)
            translationY = (.5f - list.coverY) * size.height * (list.coverZoom - 1f)
        }, contentScale = ContentScale.Crop, alignment = BiasAlignment(list.coverX * 2 - 1, list.coverY * 2 - 1))
        else Icon(Icons.Default.CollectionsBookmark, null, tint = Color.White.copy(alpha = .3f), modifier = Modifier.align(Alignment.Center).size(48.dp))
    }
}

@Composable
internal fun ListCoverStack(list: ComicList, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-18).dp), verticalAlignment = Alignment.CenterVertically) {
        if (list.items.isEmpty()) Icon(Icons.Default.CollectionsBookmark, null, tint = LocalIOS27Theme.current.textSecondary, modifier = Modifier.size(48.dp))
        list.items.take(3).forEach { BookCover(it.cover, it.title, spine3d = false, modifier = Modifier.size(42.dp, 63.dp)) }
    }
}

@Composable
internal fun CompactListRow(list: ComicList, onClick: () -> Unit, enabled: Boolean = true, trailing: String? = null) {
    val t = LocalIOS27Theme.current
    Row(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(18.dp))
        .clickable(enabled = enabled, onClickLabel = "Abrir lista ${list.name}", onClick = onClick)
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ListCoverStack(list, Modifier.width(92.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(list.name, color = t.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(trailing ?: "${list.items.size} HQs · Privada", color = t.textSecondary, fontSize = 12.sp)
        }
        Icon(if (trailing == "Já adicionada") Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textSecondary)
    }
}

@Composable
internal fun FeaturedListCard(list: ComicList, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF111113))
        .clickable(onClickLabel = "Abrir lista ${list.name}", onClick = onClick)) {
        ListHero(list)
        Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            list.items.take(4).forEach { entry ->
                BookCover(entry.cover, entry.title, spine3d = false, modifier = Modifier.weight(1f).aspectRatio(2f / 3f))
            }
        }
    }
}

@Composable
internal fun ListHero(list: ComicList) {
    Box(Modifier.fillMaxWidth()) {
        if (list.coverItem() != null) ListArtwork(list, Modifier.fillMaxWidth().aspectRatio(2f))
        else Spacer(Modifier.fillMaxWidth().height(88.dp))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF111113)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(list.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 29.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = .8f), modifier = Modifier.size(14.dp))
                Text("${list.items.size} HQs · Privada", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun ListDetailSheet(list: ComicList, onClose: () -> Unit, onComic: (ComicEntry) -> Unit,
    onEdit: (() -> Unit)? = null, onHighlight: (() -> Unit)? = null, busy: Boolean = false) {
    val t = LocalIOS27Theme.current
    var menu by remember { mutableStateOf(false) }
    CurationSheet("Lista", onClose, busy = busy) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
            if (onEdit != null) TextButton(onClick = onEdit, enabled = !busy) { Text("Editar lista", color = t.textPrimary) }
            if (onHighlight != null) Box {
                IconButton(onClick = { menu = true }, enabled = !busy) { Icon(Icons.Default.MoreHoriz, "Opções da lista", tint = t.textPrimary) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Destacar na Biblioteca") }, onClick = { menu = false; onHighlight() })
                    if (onEdit != null) DropdownMenuItem(text = { Text("Escolher capa e enquadramento") }, onClick = { menu = false; onEdit() })
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ListHero(list) }
            if (list.description.isNotBlank()) item { Text(list.description, color = t.textSecondary, modifier = Modifier.padding(horizontal = 20.dp)) }
            if (onEdit != null) item { EditorialAction("Adicionar HQs", onEdit, Modifier.fillMaxWidth().padding(horizontal = 16.dp), !busy) }
            if (list.items.isEmpty()) item {
                Text("Nenhuma HQ por aqui", color = t.textPrimary, modifier = Modifier.padding(24.dp), fontSize = 20.sp)
            }
            itemsIndexed(list.items, key = { _, it -> it.key }) { index, entry ->
                Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Ver ${entry.title}") { onComic(entry.comic()) }.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (list.ranked) Text("${index + 1}", color = t.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        BookCover(entry.cover, entry.title, spine3d = false, modifier = Modifier.size(58.dp, 87.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.title, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
                            if (entry.note.isNotBlank()) Text(entry.note, color = t.textSecondary, fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 12.dp), color = t.divider)
                }
            }
        }
    }
}

@Composable
internal fun ListArtworkEditor(list: ComicList, onApply: (ComicList) -> Unit, onClose: () -> Unit) {
    val t = LocalIOS27Theme.current
    var draft by remember(list.id) { mutableStateOf(list) }
    var tab by remember { mutableStateOf("cover") }
    CurationSheet("Capa da lista", onClose) {
        AppleSegmentedPicker(value = tab, onChange = { tab = it }, options = listOf(SegmentOption("cover", "Escolher capa"), SegmentOption("focus", "Enquadramento")))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == "cover") {
                item { Text("Use uma HQ desta lista", color = t.textSecondary) }
                items(list.items.chunked(2), key = { it.first().key }) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { entry ->
                            Column(Modifier.weight(1f).clickable(role = Role.RadioButton, onClickLabel = "Usar capa de ${entry.title}") {
                                draft = draft.copy(coverKey = entry.key, coverX = .5f, coverY = .5f, coverZoom = 1f)
                            }.semantics { selected = (draft.coverKey.ifBlank { draft.coverItem()?.key.orEmpty() }) == entry.key }) {
                                BookCover(entry.cover, entry.title, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                                Text((if (draft.coverKey.ifBlank { draft.coverItem()?.key.orEmpty() } == entry.key) "✓ " else "") + entry.title, color = t.textPrimary, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                item { ListArtwork(draft, Modifier.fillMaxWidth().aspectRatio(2f)) }
                item { Text("Ajuste o foco da arte", color = t.textPrimary, fontWeight = FontWeight.SemiBold) }
                item { FocusSlider("Posição horizontal", draft.coverX, 0f..1f) { draft = draft.copy(coverX = it) } }
                item { FocusSlider("Posição vertical", draft.coverY, 0f..1f) { draft = draft.copy(coverY = it) } }
                item { FocusSlider("Zoom", draft.coverZoom, 1f..3f) { draft = draft.copy(coverZoom = it) } }
                item { TextButton(onClick = { draft = draft.copy(coverX = .5f, coverY = .5f, coverZoom = 1f) }) { Text("Redefinir", color = t.textPrimary) } }
            }
        }
        EditorialAction("Concluir", { onApply(draft); onClose() }, Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Composable
private fun FocusSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val t = LocalIOS27Theme.current
    Column {
        Text(label, color = t.textPrimary)
        Slider(value, onChange, valueRange = range, modifier = Modifier.semantics { contentDescription = label },
            colors = SliderDefaults.colors(thumbColor = t.textPrimary, activeTrackColor = t.textPrimary))
    }
}
