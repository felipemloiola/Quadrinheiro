package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.animation.core.Animatable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.example.ui.theme.LocalAppMotion
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.ui.screens.ComicDetailDialog
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.screens.SettingsBackButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/** Existing fluid motion/material pipeline, no extra backdrop or continuous animation. */
@Stable
internal class CurationPanelState {
    var request by mutableIntStateOf(0);private set
    private var after:()->Unit={}
    private var completed=false
    fun close(afterClose:()->Unit={}) {if(request==0){after=afterClose;request++}}
    fun complete(onClose:()->Unit){if(completed)return;completed=true;val callback=after;after={};onClose();callback()}
}
@Composable internal fun rememberCurationPanelState()=remember {CurationPanelState()}

internal val LocalCurationPageDepth=staticCompositionLocalOf {0}

@Composable internal fun CurationSubpage(content:@Composable ()->Unit) {
    CompositionLocalProvider(LocalCurationPageDepth provides LocalCurationPageDepth.current+1,content=content)
}

/** Uses the same anchored, retained glass renderer as the profile picker. */
@Composable internal fun CurationActionMenu(label:String,items:List<LiquidGlassMenuItemData>,modifier:Modifier=Modifier,
    enabled:Boolean=true, valueLabel:String?=null, selectionMenu:Boolean=false) {
    val host=LocalLiquidGlassMenuHost.current
    val id=remember {java.util.UUID.randomUUID().toString()}
    var bounds by remember {mutableStateOf(IntRect.Zero)}
    val latestItems by rememberUpdatedState(items)
    DisposableEffect(host,id) {onDispose {host?.dismissMenu(id)}}
    val trigger=modifier.onGloballyPositioned {
        val point=it.positionInRoot()
        bounds=IntRect(point.x.toInt(),point.y.toInt(),point.x.toInt()+it.size.width,point.y.toInt()+it.size.height)
        host?.updateAnchorBounds(id,bounds)
    }
    val open = {
        if(bounds.width>0 && bounds.height>0)host?.showMenu(LiquidGlassMenuRequest(id,bounds,label,Icons.Default.MoreHoriz,latestItems,{},selectionMenu))
        Unit
    }
    val theme = LocalIOS27Theme.current
    if (valueLabel == null) {
        IconButton(enabled=enabled,onClick=open,modifier=trigger) {
            Icon(Icons.Default.MoreHoriz,label,tint=theme.textPrimary)
        }
    } else {
        Row(trigger.heightIn(min=52.dp).clip(RoundedCornerShape(16.dp))
            .background(theme.bgSecondary).clickable(enabled=enabled, role=Role.Button, onClick=open)
            .padding(horizontal=14.dp,vertical=10.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=theme.textPrimary)
            Text(valueLabel,style=MaterialTheme.typography.bodyMedium,color=theme.textSecondary)
            Icon(LucideIcons.ChevronRight,null,modifier=Modifier.size(16.dp),tint=theme.textSecondary)
        }
    }
}

@Composable
internal fun CurationSheet(title: String, onClose: () -> Unit, busy: Boolean=false, error: String?=null, reopenKey: Int=0,
    actionLabel: String?=null, actionEnabled: Boolean=true, onAction: ()->Unit={}, headerActions:@Composable RowScope.()->Unit={},
    panel:CurationPanelState=rememberCurationPanelState(),compact:Boolean=false,onCloseRequest:(()->Unit)?=null,menus:Boolean=false,
    content: @Composable ColumnScope.() -> Unit) {
    val requestClose=remember {mutableStateOf<()->Unit>({})}
    val contentDensity=LocalDensity.current
    Dialog(onDismissRequest={requestClose.value()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        // A Dialog creates an Android owner; retain the app's typography scale inside it.
        CompositionLocalProvider(LocalDensity provides contentDensity) {
        val theme=LocalIOS27Theme.current
        val opaque=com.example.ui.theme.LocalAppSettings.current.reduceTransparency
        val childPage=LocalCurationPageDepth.current>0 && !compact
        val menuHost=remember {LiquidGlassMenuHostState()}
        val menuBackdrop=if(menus)rememberLayerBackdrop() else null
        val fraction=if(compact || childPage)1f else .92f
        val sheet = rememberFluidSheet(
            initialHeightFraction = fraction,
            onDismiss = { panel.complete(onClose) },
            backEnabled = !busy && onCloseRequest == null && panel.request == 0 && menuHost.currentRequest == null,
            onDragDismissRequest = {
                if (onCloseRequest != null) requestClose.value()
                onCloseRequest == null && !busy && panel.request == 0
            }
        )
        FullScreenDialogWindow()
        SideEffect {requestClose.value={if(!busy && panel.request==0){if(onCloseRequest!=null)onCloseRequest() else panel.close()}}}
        BackHandler(enabled=busy || panel.request>0) {}
        BackHandler(enabled=!busy && onCloseRequest!=null && panel.request==0) {onCloseRequest?.invoke()}
        LaunchedEffect(panel.request){if(panel.request>0)sheet.dismiss()}
        LaunchedEffect(error,reopenKey) {if(panel.request==0 && (error!=null || reopenKey>0))sheet.enter()}
        val screenHeight=with(LocalDensity.current) {LocalConfiguration.current.screenHeightDp.dp.toPx()}
        val screenWidth=with(LocalDensity.current) {LocalConfiguration.current.screenWidthDp.dp.toPx()}
        val restOffset=screenHeight*(1-fraction)
        var panelHeight by remember {mutableFloatStateOf(screenHeight)}
        CompositionLocalProvider(LocalLiquidGlassMenuHost provides menuHost) {
        Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().then(if(menuBackdrop!=null)Modifier.layerBackdrop(menuBackdrop,reuseRecordedContent=true,freezeSnapshot=menuHost.currentRequest!=null) else Modifier).statusBarsPadding().imePadding()) {
            Box(Modifier.matchParentSize().graphicsLayer {alpha=if(childPage)0f else (sheet.scrimFraction/fraction).coerceIn(0f,1f)}.background(Color.Black.copy(alpha=.32f)).clickable(enabled=!busy,onClickLabel="Fechar painel") {requestClose.value()})
            Column((if(compact)Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max=maxHeight*.85f) else Modifier.fillMaxSize().padding(top=8.dp))
                .onSizeChanged {panelHeight=it.height.toFloat()}
                .graphicsLayer {
                    val progress=(sheet.offset-restOffset).coerceAtLeast(0f)/(screenHeight*fraction)
                    translationX=if(childPage)progress*screenWidth else 0f
                    translationY=if(childPage)0f else progress*(if(compact)panelHeight else screenHeight)
                }
                .background(theme.bgPrimary,RoundedCornerShape(topStart=28.dp,topEnd=28.dp))
                .then(if(opaque) Modifier else Modifier.liquidGlassRegistrationSheet(cornerRadiusDp=28f, edgeHighlight=false))
                .pointerInput(Unit) {awaitEachGesture {awaitFirstDown(requireUnconsumed=false);waitForUpOrCancellation()}}.navigationBarsPadding().padding(bottom=16.dp)) {
                Row(Modifier.fillMaxWidth().then(if(busy || childPage || panel.request>0)Modifier else sheet.dragModifier)
                    .padding(start=14.dp,end=24.dp,top=8.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(enabled=!busy && panel.request==0,onClick={requestClose.value()}) { Icon(Icons.Default.Close,"Fechar",tint=theme.textSecondary) }
                    Text(title,Modifier.weight(1f),color=theme.textPrimary,fontWeight=FontWeight.SemiBold,fontSize=18.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    headerActions()
                    actionLabel?.let { label ->
                        val enabled = actionEnabled && !busy && panel.request==0
                        TextButton(onClick=onAction,enabled=enabled,modifier=Modifier.padding(start=8.dp).heightIn(min=48.dp),
                            contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)) {
                            Text(label,color=if(enabled) theme.accent else theme.textSecondary,
                                style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
                content()
            }
        }
        if(menuBackdrop!=null)LiquidGlassMenuOverlayHost(menuHost,menuBackdrop,Modifier.matchParentSize())
        }
        }
        }
    }
}

@Composable
internal fun CurationGlassAction(label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    icon:androidx.compose.ui.graphics.vector.ImageVector?=null,primary:Boolean=false) {
    val t=LocalIOS27Theme.current
    val shape=RoundedCornerShape(50)
    val opaque=com.example.ui.theme.LocalAppSettings.current.reduceTransparency
    Row(modifier.heightIn(min=48.dp).clip(shape)
        .then(if(primary) Modifier.background(t.textPrimary,shape) else if(opaque) Modifier.contentMaterialSurface(shape) else Modifier.liquidGlass(shape=shape,surfaceAlpha=.3f))
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick).padding(horizontal=16.dp,vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
        val foreground=if(primary)t.bgPrimary else t.textPrimary
        if(icon!=null){Icon(icon,null,tint=foreground.copy(alpha=if(enabled)1f else .4f),modifier=Modifier.size(18.dp));Spacer(Modifier.width(8.dp))}
        Text(label,color=foreground.copy(alpha=if(enabled)1f else .4f),fontWeight=FontWeight.SemiBold,maxLines=2,
            textAlign=androidx.compose.ui.text.style.TextAlign.Center,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
internal fun CurationError(controller: CurationController) {
    val state by controller.state.collectAsStateWithLifecycle()
    state.error?.let { Text(it,color=LocalIOS27Theme.current.textSecondary,modifier=Modifier.padding(12.dp).semantics { liveRegion=LiveRegionMode.Polite }) }
    if(state.loading || state.saving) LoadingLine(Modifier.fillMaxWidth(),color=LocalIOS27Theme.current.accent)
}

@Composable
internal fun CurationField(label: String, value: String, max: Int, enabled: Boolean=true, onChange: (String)->Unit) {
    val t=LocalIOS27Theme.current
    TextField(value=value,enabled=enabled,onValueChange={ if(it.length<=max) onChange(it) },label={ Text(label) },
        modifier=Modifier.fillMaxWidth().inputWellSurface(),maxLines=if(max>300) 4 else 2,
        shape=RoundedCornerShape(16.dp),
        colors=TextFieldDefaults.colors(focusedTextColor=t.textPrimary,unfocusedTextColor=t.textPrimary,
            focusedLabelColor=t.accent,unfocusedLabelColor=t.textSecondary,cursorColor=t.accent,
            focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,
            focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
}

@Composable
fun AddToListAction(comic: ComicEntry, controller: CurationController, compact: Boolean = false) {
    val state by controller.state.collectAsStateWithLifecycle()
    var show by remember(comic.id,state.owner) { mutableStateOf(false) }
    TextButton(onClick={controller.load();show=true},modifier=Modifier.heightIn(min=48.dp).semantics { contentDescription = "Adicionar a uma lista" }) {
        Icon(Icons.Default.PlaylistAdd,null,tint=LocalIOS27Theme.current.accent)
        Spacer(Modifier.width(8.dp));Text(if (compact) "Adicionar à lista" else "Adicionar a uma lista",color=LocalIOS27Theme.current.accent)
    }
    if(show) AddToListSheet(comic,controller) { show=false }
}

@Composable
private fun AddToListSheet(comic: ComicEntry, controller: CurationController, onClose: ()->Unit) {
    val state by controller.state.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    var creating by remember(state.owner) { mutableStateOf<ComicList?>(null) }
    var pendingDrafts by remember(state.owner) {mutableStateOf(emptyList<ComicList>())}
    LaunchedEffect(state.owner) {pendingDrafts=controller.drafts()}
    val ref=remember(comic) { ListComic.from(comic) }
    val t = LocalIOS27Theme.current
    var query by remember { mutableStateOf("") }
    val panel=rememberCurationPanelState()
    val matchingDrafts = remember(pendingDrafts, query) { pendingDrafts.filter { it.name.contains(query, true) } }
    val matches = remember(state.lists, query) { state.lists.filter { !it.deleted && it.name.contains(query, true) } }
    CurationSheet("Adicionar a uma lista",onClose,busy=state.saving,error=state.error,panel=panel) {
        CurationError(controller)
        Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            BookCover(comic.coverUrl,comic.displayTitle,spine3d=false,modifier=Modifier.size(48.dp,72.dp))
            Text(comic.displayTitle, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
        }
        AppleTextField(query,{query=it},"Buscar lista",modifier=Modifier.padding(horizontal=16.dp))
        EditorialAction("Criar nova lista",{creating=ComicList(items=listOf(ref))},Modifier.fillMaxWidth().padding(16.dp),!state.saving && !state.loading)
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(matchingDrafts,key={"draft:${it.id}"}) {pending ->
                val included=pending.items.any {it.key==ref.key}
                TextButton(enabled=!state.saving && !state.loading && (included || pending.items.size<200),onClick={
                    creating=if(included) pending else pending.copy(items=pending.items+ref)
                }) {Text("Continuar rascunho: ${pending.name}",color=t.textPrimary)}
            }
            if(matches.isEmpty() && matchingDrafts.isEmpty() && !state.loading) item {Text("Nenhuma lista encontrada",color=t.textSecondary)}
            items(matches,key={it.id}) { list ->
                val included=list.items.any {it.key==ref.key}
                CompactListRow(list,onClick={scope.launch {
                    if(controller.save(list.copy(items=list.items+ref),state.owner)) panel.close()
                }},enabled=!included && list.items.size<200 && !state.saving && !state.loading,
                    trailing=if(included) "Já adicionada" else if(list.items.size>=200) "Limite de 200 HQs" else null)
            }
        }
    }
    creating?.let { CurationSubpage {key(state.owner,it.id) {ListEditor(it,emptyList(),controller,searchCatalog=null,onOpenComic={}) { creating=null }} } }
}

internal enum class ListEditorDestination { Editor, AddComics, ChooseCover }

@Composable
fun PersonalListsScreen(library: List<ComicEntry>, controller: CurationController,
    onSection: (String)->Unit, onBack: ()->Unit, onComic: (ComicEntry)->Unit, onConnectAccount: (()->Unit)?=null,
    searchCatalog: ((String) -> Flow<List<ComicEntry>>)? = null, showLibraryHeader: Boolean = true,
    profile: ReaderProfile = ReaderProfile(), onShareComic: (ComicEntry) -> Unit = {}) {
    val state by controller.state.collectAsStateWithLifecycle()
    val t=LocalIOS27Theme.current
    val scope=rememberCoroutineScope()
    var editing by remember(state.owner) { mutableStateOf<ComicList?>(null) }
    var viewing by remember(state.owner) { mutableStateOf<ComicList?>(null) }
    var preview by remember(state.owner) { mutableStateOf<ComicEntry?>(null) }
    var drafts by remember(state.owner) { mutableStateOf(emptyList<ComicList>()) }
    var destination by remember(state.owner) { mutableStateOf(ListEditorDestination.Editor) }
    LaunchedEffect(state.owner) { controller.load(); drafts=controller.drafts() }
    val active = remember(state.lists) { state.lists.filterNot { it.deleted } }
    val featured = remember(active) { featuredComicList(active) }
    val compactLists = remember(active, featured?.id) { active.filterNot { it.id == featured?.id } }
    fun openEditor(list: ComicList, target: ListEditorDestination = ListEditorDestination.Editor) {
        destination = target
        editing = list
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp).padding(top=12.dp)) {
        if (showLibraryHeader) {
        SettingsBackButton(onBack,label="Perfil")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(bottom=14.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Suas coleções",color=t.textSecondary,fontSize=12.sp,fontWeight=FontWeight.Medium)
                Text("Biblioteca",color=t.textPrimary,fontWeight=FontWeight.Bold,fontSize=28.sp,letterSpacing=(-.5).sp)
            }
            IconButton(onClick={openEditor(ComicList())},enabled=!state.loading && !state.saving) { Icon(Icons.Default.Add,"Criar lista",tint=t.textPrimary) }
        }
        HorizontalDivider(color=t.divider,thickness=.5.dp)
        Spacer(Modifier.height(16.dp))
        AppleSegmentedPicker(value="lists",onChange=onSection,options=listOf(SegmentOption("shelf","Estante",LucideIcons.Library),SegmentOption("diary","Diário",LucideIcons.Calendar),SegmentOption("lists","Listas",Icons.Default.FormatListNumbered)))
        } else if (active.isNotEmpty() || drafts.isNotEmpty()) {
            TextButton(onClick={openEditor(ComicList())},enabled=!state.loading && !state.saving,modifier=Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Add,null,modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Criar lista")
            }
        }
        CurationError(controller)
        if(state.error!=null) TextButton(onClick={controller.load(true)}) {Text("Tentar novamente",color=t.textPrimary)}
        if(state.localOnly && state.owner!="guest_local") Text("Listas salvas nesta conta, neste aparelho",color=t.textSecondary,fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
        LazyColumn(Modifier.testTag("lists_page"), contentPadding=PaddingValues(top=16.dp,bottom=110.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(drafts,key={"draft:${it.id}"}) {pending ->
                CompactListRow(pending,{openEditor(pending)},trailing="Continuar rascunho · ${pending.items.size} HQs")
            }
            if(active.isEmpty() && drafts.isEmpty() && !state.loading) item {
                Column(Modifier.fillMaxWidth().padding(vertical=64.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.CollectionsBookmark,null,tint=t.textSecondary,modifier=Modifier.size(64.dp))
                    Text("Suas listas começam aqui",color=t.textPrimary,fontSize=22.sp,fontWeight=FontWeight.SemiBold)
                    Text("Organize HQs por tema ou crie seu ranking.",color=t.textSecondary)
                    EditorialAction("Criar minha primeira lista",{openEditor(ComicList())})
                }
            }
            if(featured!=null) item(key="featured:${featured.id}") {FeaturedListCard(featured,{viewing=featured})}
            items(compactLists,key={it.id}) {list -> CompactListRow(list,{viewing=list})}
        }
    }
    viewing?.let {selected ->
        val list=active.firstOrNull {it.id==selected.id}
        if(list!=null) ListDetailSheet(list,{viewing=null},onComic={next ->
            preview=library.firstOrNull {ListComic.from(it).key==ListComic.from(next).key} ?: next
        },onEdit={openEditor(list)},onAdd={openEditor(list,ListEditorDestination.AddComics)},
            onChooseCover=if(list.coverItem()!=null) ({openEditor(list,ListEditorDestination.ChooseCover)}) else null,
            onHighlight={scope.launch {
            val stamp=maxOf(System.currentTimeMillis(),(active.maxOfOrNull {it.featuredAt} ?: 0)+1)
            controller.save(list.copy(featuredAt=stamp),state.owner)
        }},busy=state.saving || state.loading)
    }
    editing?.let { list -> CompositionLocalProvider(LocalCurationPageDepth provides if(viewing!=null)1 else 0) {key(state.owner,list.id) {
        ListEditor(list,library,controller,searchCatalog,onOpenComic={preview=it},onConnectAccount=onConnectAccount,destination=destination) { editing=null;scope.launch {drafts=controller.drafts()} }
    }} }
    preview?.let { comic ->
        ComicDetailDialog(comic,profile,library,onClose={preview=null},
            onRegister={ selected -> preview=null; viewing=null; editing=null; onComic(selected) },
            onCover={},
            onLogin={ preview=null; onConnectAccount?.invoke() },
            onShare={ preview=null; viewing=null; editing=null; onShareComic(comic) })
    }
}

@Composable
internal fun ListEditor(initial: ComicList, library: List<ComicEntry>, controller: CurationController,
    searchCatalog: ((String) -> Flow<List<ComicEntry>>)?, onOpenComic: (ComicEntry)->Unit,onConnectAccount:(()->Unit)?=null,
    destination: ListEditorDestination = ListEditorDestination.Editor, onClose: ()->Unit) {
    val state by controller.state.collectAsStateWithLifecycle()
    val owner=remember {state.owner}
    var draft by remember(initial.id) {mutableStateOf(initial)}
    var adding by remember {mutableStateOf(destination == ListEditorDestination.AddComics)}
    var artwork by remember {mutableStateOf(destination == ListEditorDestination.ChooseCover)}
    var deleting by remember {mutableStateOf(false)}
    var discarding by remember {mutableStateOf(false)}
    var leaving by remember {mutableStateOf(false)}
    var publishConfirmation by remember {mutableStateOf(false)}
    var connectConfirmation by remember {mutableStateOf(false)}
    var closeFailure by remember {mutableIntStateOf(0)}
    var reloadRequested by remember {mutableStateOf(false)}
    var moving by remember {mutableStateOf<ListComic?>(null)}
    var position by remember {mutableStateOf("")}
    var noting by remember {mutableStateOf<ListComic?>(null)}
    var note by remember {mutableStateOf("")}
    var dragging by remember {mutableStateOf<String?>(null)}
    var dragOffset by remember {mutableFloatStateOf(0f)}
    var settlingKey by remember { mutableStateOf<String?>(null) }
    var settlingOffset by remember { mutableFloatStateOf(0f) }
    var dragStartCenter by remember {mutableFloatStateOf(0f)}
    val motion=LocalAppMotion.current
    val editorListState=rememberLazyListState()
    val haptic=LocalHapticFeedback.current
    val scope=rememberCoroutineScope()
    val t=LocalIOS27Theme.current
    val latest by rememberUpdatedState(draft)
    val editorPanel=rememberCurationPanelState()
    LaunchedEffect(state.loading,state.lists,reloadRequested) {
        if(reloadRequested && !state.loading && state.error==null) {
            state.lists.firstOrNull {it.id==initial.id && !it.deleted}?.let {draft=it}
            reloadRequested=false
        }
    }
    fun move(key: String, target: Int) {
        val index = latest.items.indexOfFirst { it.key == key }
        if (index < 0 || index == target) return
        val items = latest.items.toMutableList()
        val item = items.removeAt(index)
        items.add(target.coerceIn(0, items.size), item)
        draft = latest.copy(items = items)
    }
    fun moveToDragTarget(key: String) {
        val center = dragStartCenter + dragOffset
        val target = editorListState.layoutInfo.visibleItemsInfo.asSequence()
            .filter { it.contentType == "comic" }
            .minByOrNull { kotlin.math.abs(it.offset + it.size / 2f - center) }
        if (target != null) {
            val index = latest.items.indexOfFirst { it.key == target.key }
            if (index >= 0) move(key, index)
        }
    }
    val edgeZone = with(LocalDensity.current) { 56.dp.toPx() }
    val maxScrollSpeed = with(LocalDensity.current) { 760.dp.toPx() }
    LaunchedEffect(dragging) {
        val key = dragging ?: return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, .032f)
            previousFrame = frame
            val layout = editorListState.layoutInfo
            val center = dragStartCenter + dragOffset
            val top = layout.viewportStartOffset + edgeZone
            val bottom = layout.viewportEndOffset - edgeZone
            val pressure = when {
                center < top -> ((center - top) / edgeZone).coerceAtLeast(-1f)
                center > bottom -> ((center - bottom) / edgeZone).coerceAtMost(1f)
                else -> 0f
            }
            if (pressure != 0f && editorListState.scrollBy(pressure * maxScrollSpeed * seconds) != 0f) {
                moveToDragTarget(key)
            }
        }
    }
    LaunchedEffect(settlingKey, motion.reduced) {
        if (settlingKey != null && !motion.reduced) {
            Animatable(settlingOffset).animateTo(0f, motion.spring(stiffness = 650f)) {
                settlingOffset = value
            }
        }
        settlingKey = null
        settlingOffset = 0f
    }
    fun closeDraft() { if(state.saving) return;scope.launch {if(controller.preserveDraft(draft,owner)) editorPanel.close() else closeFailure++} }
    CurationSheet(if(initial.revision==0L) "Criar lista" else "Editar lista",onClose,busy=state.saving,error=state.error,reopenKey=closeFailure,panel=editorPanel,menus=true,
        onCloseRequest=if(draft==initial)null else ({leaving=true}),
        actionLabel=if(state.saving) "Salvando…" else "Salvar lista",
        actionEnabled=draft.name.isNotBlank() && state.ready && !state.loading,
        onAction={scope.launch {if(controller.save(draft,owner)) editorPanel.close()}}) {
        CurationError(controller)
        if(state.error!=null && !state.ready) TextButton(onClick={controller.load(true)}) {Text("Tentar novamente",color=t.textPrimary)}
        if(state.error!=null && state.ready) TextButton(onClick={scope.launch {controller.preserveDraft(draft,owner);controller.load(true);reloadRequested=true}}) {
            Text("Recarregar versão salva · preservar rascunho",color=t.accent)
        }
        if(state.error!=null && state.ready && initial.revision>0) TextButton(enabled=!state.saving,onClick={scope.launch {
            if(controller.save(draft.copy(id=java.util.UUID.randomUUID().toString(),revision=0,deleted=false),owner)) editorPanel.close()
        }}) {Text("Salvar meu rascunho como nova lista",color=t.accent)}
        if(state.error!=null && state.ready) TextButton(enabled=!state.saving,onClick={discarding=true}) {Text("Descartar este rascunho",color=t.textSecondary)}
        LazyColumn(Modifier.weight(1f).testTag("list_editor_items"),state=editorListState,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                if(draft.coverItem()!=null) {
                    ListArtwork(draft,Modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(18.dp)))
                    Spacer(Modifier.height(12.dp))
                    CurationGlassAction("Escolher capa",{artwork=true},Modifier.fillMaxWidth(),enabled=!state.saving,icon=Icons.Default.Image,primary=true)
                    Spacer(Modifier.height(20.dp))
                }
                CurationField("Nome",draft.name,100,enabled=!state.saving) {draft=draft.copy(name=it)}
                Spacer(Modifier.height(12.dp))
                CurationField("Descrição",draft.description,2000,enabled=!state.saving) {draft=draft.copy(description=it)}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Ranking numerado",Modifier.weight(1f),color=t.textPrimary)
                    AppleToggle(draft.ranked,onChange={if(!state.saving)draft=draft.copy(ranked=it)},modifier=Modifier.semantics {contentDescription="Ranking numerado";if(state.saving)disabled()})
                }
                CurationActionMenu("Visibilidade da lista",listOf(
                    LiquidGlassMenuItemData("Privada",LucideIcons.Lock,selected=!draft.isPublic) { draft=draft.copy(isPublic=false) },
                    LiquidGlassMenuItemData("Pública",LucideIcons.Globe,selected=draft.isPublic) {
                        if(!draft.isPublic) {
                            if(state.localOnly || !state.ready) connectConfirmation=true else publishConfirmation=true
                        }
                    }
                ),modifier=Modifier.fillMaxWidth().padding(vertical=12.dp),enabled=!state.saving,
                    valueLabel=if(draft.isPublic)"Pública" else "Privada",selectionMenu=true)
                Text("${draft.items.size}/200 HQs",color=t.textSecondary,fontSize=12.sp)
                Spacer(Modifier.height(12.dp))
                CurationGlassAction("Adicionar HQs",{adding=true},Modifier.fillMaxWidth(),enabled=draft.items.size<200 && !state.saving,icon=Icons.Default.Add)
            }
            itemsIndexed(draft.items,key={_, entry -> entry.key},contentType={_, _ -> "comic"}) { index, entry ->
                Column(Modifier.fillMaxWidth()
                    .animateItem(fadeInSpec=null,fadeOutSpec=null,placementSpec=if(motion.reduced || dragging==entry.key || settlingKey==entry.key)null else motion.spring(stiffness=500f))
                    .zIndex(if(dragging==entry.key || settlingKey==entry.key)1f else 0f)
                    .graphicsLayer {
                        translationY = when (entry.key) {
                            dragging -> editorListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entry.key }
                                ?.let { dragStartCenter + dragOffset - (it.offset + it.size / 2f) } ?: 0f
                            settlingKey -> settlingOffset
                            else -> 0f
                        }
                    }
                    .padding(vertical=8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(48.dp).semantics { contentDescription = "Reordenar ${entry.title}" }
                            .clickable(enabled = !state.saving, role = Role.Button, onClickLabel = "Mover para posição") {
                                moving = entry
                                position = "${index + 1}"
                            }
                            .pointerInput(entry.key,state.saving) {if(!state.saving)detectDragGesturesAfterLongPress(
                                onDragStart={
                                    settlingKey = null
                                    dragging=entry.key;dragOffset=0f
                                    val row=editorListState.layoutInfo.visibleItemsInfo.firstOrNull {it.key==entry.key}
                                    dragStartCenter=(row?.offset ?: 0)+(row?.size ?: 0)/2f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag={change,amount ->
                                    change.consume();dragOffset+=amount.y
                                    moveToDragTarget(entry.key)
                                },
                                onDragCancel={dragging=null;dragOffset=0f},
                                onDragEnd={
                                    moveToDragTarget(entry.key)
                                    val row = editorListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entry.key }
                                    if (row != null && !motion.reduced) {
                                        settlingOffset = dragStartCenter + dragOffset - (row.offset + row.size / 2f)
                                        settlingKey = entry.key
                                    }
                                    dragging=null;dragOffset=0f
                                })},contentAlignment=Alignment.Center) {
                            Icon(Icons.Default.DragHandle,null,tint=t.textSecondary)
                        }
                        if(draft.ranked) Text("${index+1}",color=t.accent,fontWeight=FontWeight.Bold)
                        BookCover(entry.cover,entry.title,spine3d=false,modifier=Modifier.size(52.dp,78.dp).clearAndSetSemantics {})
                        Box(Modifier.weight(1f).heightIn(min=48.dp).clickable(enabled=!state.saving,onClickLabel="Ver ${entry.title}") {
                            scope.launch {
                                if (controller.preserveDraft(draft, owner)) {
                                    onOpenComic(library.firstOrNull { ListComic.from(it).key == entry.key } ?: entry.comic())
                                }
                            }
                        }, contentAlignment = Alignment.CenterStart) {
                            Text(entry.title, color = t.textPrimary)
                        }
                        CurationActionMenu("Opções de ${entry.title}",listOf(
                            LiquidGlassMenuItemData("Mover",Icons.Outlined.SwapVert) {moving=entry;position="${index+1}"},
                            LiquidGlassMenuItemData("Editar nota",Icons.Outlined.Edit) {noting=entry;note=entry.note},
                            LiquidGlassMenuItemData("Remover da lista",Icons.Outlined.RemoveCircleOutline) {draft=draft.copy(items=draft.items.filterNot {it.key==entry.key})}
                        ),Modifier.testTag("list_item_options_${entry.key}"),enabled=!state.saving)
                    }
                    if(entry.note.isNotBlank()) Text(entry.note,color=t.textSecondary,modifier=Modifier.padding(top=8.dp))
                    HorizontalDivider(color=t.divider)
                }
            }
            if(initial.revision>0) item(key="delete-list") {
                TextButton(onClick={deleting=true},enabled=!state.saving) {Text("Excluir lista",color=t.textSecondary)}
            }
        }
    }
    CurationSubpage {
    if(artwork) ListArtworkEditor(draft,{draft=it},{artwork=false})
    if(publishConfirmation) CurationPrompt("Publicar esta lista?",{publishConfirmation=false},"Usar pública",{draft=draft.copy(isPublic=true)}) {
        Text("Ao salvar, o nome, a descrição e as HQs ficarão públicos. Notas dos itens e dados de leitura continuam privados.",color=t.textSecondary)
    }
    if(connectConfirmation) CurationPrompt("Conecte uma conta Google",{connectConfirmation=false},if(onConnectAccount!=null)"Conectar Google" else "Entendi",{
        if(onConnectAccount!=null)scope.launch {if(controller.preserveDraft(draft,owner))editorPanel.close(onConnectAccount)}
    }) {Text("Esta conta salva listas neste aparelho. Uma conta Google é necessária para publicar. Seu rascunho permanece nesta conta.",color=t.textSecondary)}
    if (adding) ListComicPicker(library, draft.items, searchCatalog, { adding = false }) { selected ->
        draft = draft.copy(items = draft.items + selected)
    }
    if(leaving) CurationPrompt("Sair da edição?",{leaving=false},"Guardar rascunho e sair",::closeDraft,cancelLabel="Continuar editando",
        alternativeLabel="Descartar alterações",onAlternative={discarding=true}) {Text("Você pode guardar as alterações e continuar depois.",color=t.textSecondary)}
    if(discarding) CurationPrompt("Descartar este rascunho?",{discarding=false},"Descartar rascunho",{scope.launch {
        if(controller.discardDraft(draft.id,owner))editorPanel.close()
    }},cancelLabel="Continuar editando") {Text("As alterações não salvas nesta lista serão descartadas. Seu acervo e a lista salva permanecem.",color=t.textSecondary)}
    if(deleting) CurationPrompt("Excluir esta lista?",{deleting=false},"Excluir lista",{scope.launch {
        if(controller.save(initial.copy(deleted=true,items=emptyList(),description="",name="Lista excluída"),owner))editorPanel.close()
    }}) {Text("Suas HQs, resenhas e registros de leitura serão preservados.",color=t.textSecondary)}
    moving?.let {entry -> CurationPrompt("Mover para posição",{moving=null},"Mover",{move(entry.key,position.toInt()-1)},confirmEnabled=position.toIntOrNull() in 1..draft.items.size) {
        CurationField("Posição de 1 a ${draft.items.size}",position,4) {position=it.filter(Char::isDigit)}
    }}
    noting?.let {entry -> CurationPrompt("Nota nesta lista",{noting=null},"Concluir",{
        draft=draft.copy(items=draft.items.map {if(it.key==entry.key)it.copy(note=note) else it})
    }) {CurationField("Sua nota",note,1000) {note=it}}}
    }
}

@Composable
private fun ListComicPicker(
    library: List<ComicEntry>,
    included: List<ListComic>,
    searchCatalog: ((String) -> Flow<List<ComicEntry>>)?,
    onClose: () -> Unit,
    onConfirm: (List<ListComic>) -> Unit
) {
    val theme = LocalIOS27Theme.current
    val panel = rememberCurationPanelState()
    var catalog by remember { mutableStateOf(false) }
    var libraryQuery by remember { mutableStateOf("") }
    var catalogQuery by remember { mutableStateOf("") }
    var retry by remember { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf(emptyMap<String, ListComic>()) }
    val references = remember(library) { library.map { it to ListComic.from(it) } }
    val includedKeys = remember(included) { included.mapTo(HashSet()) { it.key } }
    val catalogTerm = catalogQuery.trim()
    val catalogSearch = if (catalog) searchCatalog else null
    val libraryMatches = remember(references, libraryQuery) {
        references.asSequence()
            .filter { (comic, _) -> comic.displayTitle.contains(libraryQuery, true) || comic.authors.contains(libraryQuery, true) }
            .map { it.second }.distinctBy { it.key }.toList()
    }
    var catalogMatches by remember(catalogTerm) { mutableStateOf<List<ListComic>>(emptyList()) }
    var loading by remember(catalogTerm, retry) { mutableStateOf(catalogTerm.length >= 2) }
    var failed by remember(catalogTerm, retry) { mutableStateOf(false) }
    LaunchedEffect(catalogSearch, catalogTerm, retry) {
        if (catalogSearch == null || catalogTerm.length < 2) return@LaunchedEffect
        if (catalogMatches.isNotEmpty() && !loading) return@LaunchedEffect
        loading = true
        try {
            delay(300)
            catalogSearch(catalogTerm).collect { editions ->
                catalogMatches = editions.map(ListComic::from).distinctBy { it.key }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            failed = true
        }
        loading = false
    }
    val libraryScroll = rememberLazyListState()
    val catalogScroll = rememberLazyListState()
    CurationSheet("Adicionar HQs", onClose, panel = panel, actionLabel = "Adicionar (${selection.size})",
        actionEnabled = selection.isNotEmpty(), onAction = {
            val fromLibrary = references.asSequence().map { it.second }.filter { it.key in selection }
                .distinctBy { it.key }.toList()
            val libraryKeys = fromLibrary.mapTo(HashSet()) { it.key }
            val selected = (fromLibrary + selection.values.filterNot { it.key in libraryKeys })
                .filterNot { it.key in includedKeys }.take((200 - included.size).coerceAtLeast(0))
            panel.close { onConfirm(selected) }
        }) {
        if (searchCatalog != null) {
            AppleSegmentedPicker(listOf("Minha estante", "Catálogo"), if (catalog) 1 else 0,
                { catalog = it == 1 }, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        AppleTextField(if (catalog) catalogQuery else libraryQuery, {
            if (catalog) catalogQuery = it.take(100) else libraryQuery = it
        }, if (catalog) "Buscar no catálogo" else "Buscar na biblioteca",
            modifier = Modifier.padding(16.dp), usePlaceholderAsLabel = true)
        val matches = if (catalog) catalogMatches else libraryMatches
        LazyColumn(Modifier.weight(1f), state = if (catalog) catalogScroll else libraryScroll,
            contentPadding = PaddingValues(horizontal = 16.dp)) {
            when {
                catalog && catalogTerm.length < 2 -> item { Text("Digite pelo menos duas letras para buscar no catálogo.", color = theme.textSecondary) }
                catalog && loading && matches.isEmpty() -> item {
                    Text("Buscando HQs…", color = theme.textSecondary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                catalog && (failed || matches.isEmpty()) -> item {
                    Text(if (failed) "A busca foi interrompida." else "Nenhuma HQ encontrada no catálogo.",
                        color = theme.textSecondary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    TextButton(onClick = { retry++ }) { Text("Tentar novamente", color = theme.accent) }
                }
                matches.isEmpty() -> item { Text("Nenhuma HQ encontrada na estante.", color = theme.textSecondary) }
            }
            items(matches, key = { it.key }) { ref ->
                val alreadyIncluded = ref.key in includedKeys
                val checked = alreadyIncluded || ref.key in selection
                Row(Modifier.fillMaxWidth().toggleable(
                    value = checked, role = Role.Checkbox,
                    enabled = panel.request == 0 && !alreadyIncluded && (checked || included.size + selection.size < 200),
                    onValueChange = { selected -> selection = if (selected) selection + (ref.key to ref) else selection - ref.key }
                ).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BookCover(ref.cover, ref.title, spine3d = false, modifier = Modifier.size(48.dp, 72.dp).clearAndSetSemantics {})
                    Text(ref.title, Modifier.weight(1f), color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                    if (alreadyIncluded) Text("Já na lista", color = theme.textSecondary, fontSize = 12.sp)
                    else Checkbox(checked, null)
                }
            }
        }
    }
}

@Composable
fun RecommendationExplanationAction(recommendation: RecommendedComic, controller: CurationController, onRegister: (ComicEntry)->Unit) {
    val ownerState by controller.state.collectAsStateWithLifecycle()
    var show by remember(recommendation.comic.id,ownerState.owner) {mutableStateOf(false)}
    val t=LocalIOS27Theme.current
    TextButton(onClick={controller.load();show=true},modifier=Modifier.heightIn(min=48.dp)) {Text("Por que esta HQ?",color=t.accent,fontSize=12.sp)}
    if(show) {
        val state by controller.state.collectAsStateWithLifecycle()
        val scope=rememberCoroutineScope()
        CurationSheet("Por que esta HQ?",{show=false},compact=true) {
            CurationError(controller)
            LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    BookCover(recommendation.comic.coverUrl,recommendation.comic.displayTitle,spine3d=false,modifier=Modifier.size(52.dp,78.dp).clearAndSetSemantics {})
                    Text(recommendation.comic.displayTitle,color=t.textPrimary,fontWeight=FontWeight.Bold,fontSize=20.sp,modifier=Modifier.weight(1f))
                }}
                item {Text(recommendation.explanation?.detail ?: "${recommendation.reasonLabel}. Esta seleção antiga não tem uma referência detalhada salva.",color=t.textSecondary)}
                item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    EditorialAction("Mais como esta",{scope.launch {controller.feedback(recommendation.comic,"more")}},Modifier.weight(1f),!state.saving && !state.loading)
                    EditorialAction("Não tenho interesse",{scope.launch {controller.feedback(recommendation.comic,"hide")}},Modifier.weight(1f),!state.saving && !state.loading)
                } }
                item { TextButton(enabled=!state.saving,onClick={scope.launch {controller.feedback(recommendation.comic,"undo")}}) {Text("Desfazer preferência",color=t.accent)} }
                item { TextButton(onClick={show=false;onRegister(recommendation.comic)}) {Text("Já li / Quero ler…",color=t.accent)} }
            }
        }
    }
}

@Composable
fun RecommendationPreferencesAction(controller: CurationController) {
    val state by controller.state.collectAsStateWithLifecycle()
    var show by remember(state.owner) {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    TextButton(onClick={controller.load();show=true}) {Text("Gerenciar preferências de sugestões",color=LocalIOS27Theme.current.accent)}
    if(show) CurationSheet("Preferências de sugestões",{show=false}) {
        CurationError(controller)
        LazyColumn(contentPadding=PaddingValues(16.dp)) {
            if(state.feedback.hidden.isEmpty() && state.feedback.boosted.isEmpty()) item {Text("Nenhuma preferência registrada.",color=LocalIOS27Theme.current.textSecondary)}
            items(state.feedback.hidden.map {it to "Oculta"}+state.feedback.boosted.map {it to "Mais como esta"},key={it.first.key+it.second}) { (item,label) ->
                Text("${item.title} · $label",color=LocalIOS27Theme.current.textPrimary)
                TextButton(enabled=!state.saving,onClick={scope.launch {controller.feedback(item.comic(),"undo")}}) {Text("Remover preferência",color=LocalIOS27Theme.current.accent)}
            }
        }
    }
}
