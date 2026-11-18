package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import com.kyant.shapes.ContinuousRoundedRectangle
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
import androidx.compose.ui.platform.testTag
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

private val listArtworkBackground = Color(0xFF111113)
private val listArtworkScrim = Brush.verticalGradient(listOf(Color.Transparent, listArtworkBackground))

/** Original pixels, one bounded Coil request. No backdrop capture or per-frame effects. */
@Composable
internal fun ListArtwork(list: ComicList, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cover = remember(list.items, list.coverKey) {
        list.coverItem()?.cover.orEmpty().trim().replace("http://", "https://")
    }
    val request = remember(context, cover) {
        cover.takeIf(String::isNotEmpty)?.let {
            ImageRequest.Builder(context).data(it).size(1024, 1024).allowHardware(true).crossfade(false)
            .diskCachePolicy(LibraryDiskCacheRegistry.diskCachePolicyFor(cover)).build()
        }
    }
    Box(modifier.clipToBounds().background(listArtworkBackground)) {
        if (request != null) AsyncImage(request, null, Modifier.fillMaxSize().graphicsLayer {
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
        list.items.take(3).forEach {
            BookCover(it.cover, it.title, spine3d = false, modifier = Modifier.size(42.dp, 63.dp).clearAndSetSemantics {})
        }
    }
}

@Composable
internal fun CompactListRow(list: ComicList, onClick: () -> Unit, enabled: Boolean = true, trailing: String? = null) {
    val t = LocalIOS27Theme.current
    Row(Modifier.fillMaxWidth().contentMaterialSurface(ContinuousRoundedRectangle(18.dp))
        .clickable(enabled = enabled, onClickLabel = "Abrir lista ${list.name}", onClick = onClick)
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ListCoverStack(list, Modifier.width(92.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(list.name, color = t.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(trailing ?: "${list.items.size} HQs · ${if(list.isPublic) "Pública" else "Privada"}", color = t.textSecondary, fontSize = 12.sp)
        }
        Icon(if (trailing == "Já adicionada") Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.textSecondary)
    }
}

@Composable
internal fun FeaturedListCard(list: ComicList, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(ContinuousRoundedRectangle(22.dp)).background(listArtworkBackground)
        .clickable(onClickLabel = "Abrir lista ${list.name}", onClick = onClick)) {
        ListHero(list)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(start=14.dp,end=14.dp,bottom=16.dp)) {
            val coverWidth=((maxWidth-30.dp)/4).coerceIn(32.dp,64.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                list.items.take(4).forEach { entry ->
                    BookCover(entry.cover, entry.title, spine3d = false,
                        modifier = Modifier.width(coverWidth).height(coverWidth*1.5f).clearAndSetSemantics {})
                }
            }
        }
    }
}

@Composable
internal fun ListHero(list: ComicList) {
    Box(Modifier.fillMaxWidth()) {
        if (list.coverItem() != null) ListArtwork(list, Modifier.fillMaxWidth().aspectRatio(2f))
        else Spacer(Modifier.fillMaxWidth().height(88.dp))
        Box(Modifier.matchParentSize().background(listArtworkScrim))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(list.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 29.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if(list.isPublic) Icons.Default.Public else Icons.Default.Lock, null, tint = Color.White.copy(alpha = .8f), modifier = Modifier.size(14.dp))
                Text("${list.items.size} HQs · ${if(list.isPublic) "Pública" else "Privada"}", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun ListDetailSheet(list: ComicList, onClose: () -> Unit, onComic: (ComicEntry) -> Unit,
    onEdit: (() -> Unit)? = null, onAdd: (() -> Unit)? = onEdit, onHighlight: (() -> Unit)? = null,
    onChooseCover: (() -> Unit)? = null, onDelete: (() -> Unit)? = null, busy: Boolean = false) {
    val t = LocalIOS27Theme.current
    val panel = rememberCurationPanelState()
    var confirmingDelete by remember(list.id) { mutableStateOf(false) }
    val hasMenu = onAdd != null || onHighlight != null || onChooseCover != null || onDelete != null
    CurationSheet(list.name, onClose, busy = busy, menus = hasMenu, actionLabel = if (onEdit != null) "Editar" else null,
        onAction = { onEdit?.invoke() }, panel = panel, headerActions = {
            if (hasMenu) CurationActionMenu("Opções da lista", buildList {
                if (onAdd != null) add(LiquidGlassMenuItemData("Adicionar HQs", Icons.Default.Add, onClick = onAdd))
                if (onChooseCover != null) add(LiquidGlassMenuItemData("Escolher capa", Icons.Default.Image, onClick = onChooseCover))
                if (onHighlight != null) add(LiquidGlassMenuItemData("Destacar na Biblioteca", Icons.Default.Star, onClick = onHighlight))
                if (onDelete != null) add(LiquidGlassMenuItemData("Excluir lista", Icons.Outlined.DeleteOutline, onClick = { confirmingDelete = true }))
            }, enabled = !busy)
        }) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ListHero(list) }
            if (list.description.isNotBlank()) item { Text(list.description, color = t.textSecondary, modifier = Modifier.padding(horizontal = 20.dp)) }
            if (onAdd != null) item { EditorialAction("Adicionar HQs", onAdd, Modifier.fillMaxWidth().padding(horizontal = 16.dp), !busy) }
            if (list.items.isEmpty()) item {
                Text("Nenhuma HQ por aqui", color = t.textPrimary, modifier = Modifier.padding(24.dp), fontSize = 20.sp)
            }
            itemsIndexed(list.items, key = { _, it -> it.key }) { index, entry ->
                Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Ver ${entry.title}") { onComic(entry.comic()) }.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (list.ranked) Text("${index + 1}", color = t.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        BookCover(entry.cover, entry.title, spine3d = false,
                            modifier = Modifier.size(58.dp, 87.dp).clearAndSetSemantics {})
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
    if (confirmingDelete && onDelete != null) {
        CurationSubpage {
            CurationPrompt(
                "Excluir esta lista?",
                { confirmingDelete = false },
                "Excluir lista",
                { panel.close { onDelete() } }
            ) {
                Text("Suas HQs, resenhas e registros de leitura serão preservados.", color = t.textSecondary)
            }
        }
    }
}

@Composable
internal fun ListArtworkEditor(list:ComicList,onApply:(ComicList)->Unit,onClose:()->Unit) {
    var selected by remember(list.id){mutableStateOf<ComicList?>(null)}
    val t=LocalIOS27Theme.current
    val pickerPanel=rememberCurationPanelState()
    val coverRows = remember(list.items) { list.items.filter { it.cover.isNotBlank() }.chunked(2) }
    val currentCoverKey = remember(list.items, list.coverKey) { list.coverItem()?.key }
    CurationSheet("Escolher capa",onClose,panel=pickerPanel) {
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {Text("Use uma HQ desta lista",color=t.textSecondary)}
            items(coverRows,key={it.first().key}) {pair ->
                Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    pair.forEach {entry ->
                        val isCurrent = currentCoverKey == entry.key
                        val shape=RoundedCornerShape(18.dp)
                        Column(Modifier.weight(1f).testTag("list_cover_choice_${entry.key}").clip(shape).contentMaterialSurface(shape)
                            .then(if(isCurrent)Modifier.border(1.dp,t.textPrimary.copy(alpha=.6f),shape) else Modifier)
                            .selectable(selected=isCurrent,enabled=pickerPanel.request==0,role=Role.RadioButton) {
                            selected=if(list.coverKey==entry.key)list else list.copy(coverKey=entry.key,coverX=.5f,coverY=.5f,coverZoom=1f)
                        }.semantics {stateDescription=if(isCurrent)"Capa atual" else "Usar esta capa"}.padding(12.dp)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(2f/3f)) {
                                BookCover(entry.cover,entry.title,spine3d=false,modifier=Modifier.fillMaxSize().clearAndSetSemantics {})
                                if(isCurrent) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(26.dp)
                                    .background(t.bgPrimary,androidx.compose.foundation.shape.CircleShape).padding(5.dp)) {
                                    Icon(Icons.Default.Check,null,tint=t.textPrimary,modifier=Modifier.fillMaxSize())
                                }
                            }
                            Text(entry.title,color=t.textPrimary,fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium,modifier=Modifier.fillMaxWidth().padding(top=10.dp),minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis)
                        }
                    }
                    if(pair.size==1)Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    selected?.let {cover -> CurationSubpage {
        ListCoverCropDialog(cover,{onApply(it);onClose()},{selected=null})
    }}
}
