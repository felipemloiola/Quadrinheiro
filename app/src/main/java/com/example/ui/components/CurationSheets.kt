package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.screens.SettingsBackButton
import kotlinx.coroutines.launch

/** Existing fluid motion/material pipeline, no extra backdrop or continuous animation. */
@Composable
internal fun CurationSheet(title: String, onClose: () -> Unit, busy: Boolean=false, error: String?=null, reopenKey: Int=0,
    actionLabel: String?=null, actionEnabled: Boolean=true, onAction: ()->Unit={}, headerActions:@Composable RowScope.()->Unit={}, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest={if(!busy)onClose()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        val theme=LocalIOS27Theme.current
        val sheet=rememberFluidSheet(.92f,onClose,{},backEnabled=!busy)
        BackHandler(enabled=busy) {}
        LaunchedEffect(error,reopenKey) {if(error!=null || reopenKey>0)sheet.enter()}
        val restOffset=with(LocalDensity.current) {LocalConfiguration.current.screenHeightDp.dp.toPx()*.08f}
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=.32f)).clickable(enabled=!busy,onClickLabel="Fechar painel") {sheet.dismiss()})
            Column(Modifier.fillMaxSize().padding(top=8.dp).graphicsLayer { translationY=(sheet.offset-restOffset).coerceAtLeast(0f) }
                .background(theme.bgPrimary,RoundedCornerShape(topStart=28.dp,topEnd=28.dp))
                .contentMaterialSurface(RoundedCornerShape(topStart=28.dp,topEnd=28.dp))
                .pointerInput(Unit) {awaitEachGesture {awaitFirstDown(requireUnconsumed=false);waitForUpOrCancellation()}}.navigationBarsPadding().padding(bottom=16.dp)) {
                Row(Modifier.fillMaxWidth().then(if(busy)Modifier else sheet.dragModifier).padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(enabled=!busy,onClick={ sheet.dismiss() }) { Icon(Icons.Default.Close,"Fechar",tint=theme.textSecondary) }
                    Text(title,Modifier.weight(1f),color=theme.textPrimary,fontWeight=FontWeight.SemiBold,fontSize=18.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    headerActions()
                    actionLabel?.let { label -> Button(onClick=onAction,enabled=actionEnabled && !busy,
                        modifier=Modifier.heightIn(min=48.dp).padding(end=8.dp),shape=RoundedCornerShape(50),
                        colors=ButtonDefaults.buttonColors(containerColor=theme.textPrimary,contentColor=theme.bgPrimary)) {Text(label)} }
                }
                content()
            }
        }
    }
}

@Composable
internal fun CurationError(controller: CurationController) {
    val state by controller.state.collectAsStateWithLifecycle()
    state.error?.let { Text(it,color=LocalIOS27Theme.current.textSecondary,modifier=Modifier.padding(12.dp).semantics { liveRegion=LiveRegionMode.Polite }) }
    if(state.loading || state.saving) LinearProgressIndicator(Modifier.fillMaxWidth(),color=LocalIOS27Theme.current.accent)
}

@Composable
internal fun CurationField(label: String, value: String, max: Int, enabled: Boolean=true, onChange: (String)->Unit) {
    val t=LocalIOS27Theme.current
    TextField(value=value,enabled=enabled,onValueChange={ if(it.length<=max) onChange(it) },label={ Text(label) },
        modifier=Modifier.fillMaxWidth(),maxLines=if(max>300) 4 else 2,
        shape=RoundedCornerShape(16.dp),
        colors=TextFieldDefaults.colors(focusedTextColor=t.textPrimary,unfocusedTextColor=t.textPrimary,
            focusedLabelColor=t.accent,unfocusedLabelColor=t.textSecondary,cursorColor=t.accent,
            focusedContainerColor=t.bgSecondary,unfocusedContainerColor=t.bgSecondary,
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
    CurationSheet("Adicionar a uma lista",onClose,busy=state.saving,error=state.error) {
        CurationError(controller)
        Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            BookCover(comic.coverUrl,comic.displayTitle,spine3d=false,modifier=Modifier.size(48.dp,72.dp))
            Column { Text(comic.displayTitle,color=t.textPrimary,fontWeight=FontWeight.SemiBold);Text("Listas privadas",color=t.textSecondary,fontSize=12.sp) }
        }
        AppleTextField(query,{query=it},"Buscar lista",modifier=Modifier.padding(horizontal=16.dp))
        EditorialAction("Criar nova lista",{creating=ComicList(items=listOf(ref))},Modifier.fillMaxWidth().padding(16.dp),!state.saving && !state.loading)
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(pendingDrafts.filter {it.name.contains(query,true)},key={"draft:${it.id}"}) {pending ->
                val included=pending.items.any {it.key==ref.key}
                TextButton(enabled=!state.saving && !state.loading && (included || pending.items.size<200),onClick={
                    creating=if(included) pending else pending.copy(items=pending.items+ref)
                }) {Text("Continuar rascunho: ${pending.name}",color=t.textPrimary)}
            }
            val matches=state.lists.filter {!it.deleted && it.name.contains(query,true)}
            if(matches.isEmpty() && !state.loading) item {Text("Nenhuma lista encontrada",color=t.textSecondary)}
            items(matches,key={it.id}) { list ->
                val included=list.items.any {it.key==ref.key}
                CompactListRow(list,onClick={scope.launch {
                    if(controller.save(list.copy(items=list.items+ref),state.owner)) onClose()
                }},enabled=!included && list.items.size<200 && !state.saving && !state.loading,
                    trailing=if(included) "Já adicionada" else if(list.items.size>=200) "Limite de 200 HQs" else "${list.items.size} HQs · Privada")
            }
        }
    }
    creating?.let { key(state.owner,it.id) {ListEditor(it,emptyList(),controller,onSearch=null,onOpenComic={}) { creating=null;onClose() }} }
}

@Composable
fun PersonalListsScreen(library: List<ComicEntry>, controller: CurationController,
    onSection: (String)->Unit, onBack: ()->Unit, onSearch: ()->Unit, onComic: (ComicEntry)->Unit,onConnectAccount:(()->Unit)?=null) {
    val state by controller.state.collectAsStateWithLifecycle()
    val t=LocalIOS27Theme.current
    val scope=rememberCoroutineScope()
    var editing by remember(state.owner) { mutableStateOf<ComicList?>(null) }
    var viewing by remember(state.owner) { mutableStateOf<ComicList?>(null) }
    var drafts by remember(state.owner) { mutableStateOf(emptyList<ComicList>()) }
    var publicBrowse by remember(state.owner) {mutableStateOf(false)}
    var addingFromDetail by remember(state.owner) {mutableStateOf(false)}
    LaunchedEffect(state.owner) { controller.load(); drafts=controller.drafts() }
    val active=state.lists.filterNot {it.deleted}
    val featured=featuredComicList(active)
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp).padding(top=12.dp)) {
        SettingsBackButton(onBack,label="Perfil")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(bottom=14.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Suas coleções",color=t.textSecondary,fontSize=12.sp,fontWeight=FontWeight.Medium)
                Text("Biblioteca",color=t.textPrimary,fontWeight=FontWeight.Bold,fontSize=28.sp,letterSpacing=(-.5).sp)
            }
            IconButton(onClick={publicBrowse=true}) {Icon(Icons.Default.Public,"Explorar listas públicas",tint=t.textPrimary)}
            IconButton(onClick={editing=ComicList()},enabled=!state.loading && !state.saving) { Icon(Icons.Default.Add,"Criar lista",tint=t.textPrimary) }
        }
        HorizontalDivider(color=t.divider,thickness=.5.dp)
        Spacer(Modifier.height(16.dp))
        AppleSegmentedPicker(value="lists",onChange=onSection,options=listOf(SegmentOption("shelf","Estante",LucideIcons.Library),SegmentOption("diary","Diário",LucideIcons.Calendar),SegmentOption("lists","Listas",Icons.Default.FormatListNumbered)))
        CurationError(controller)
        if(state.error!=null) TextButton(onClick={controller.load(true)}) {Text("Tentar novamente",color=t.textPrimary)}
        if(state.localOnly && state.owner!="guest_local") Text("Listas salvas nesta conta, neste aparelho",color=t.textSecondary,fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
        LazyColumn(contentPadding=PaddingValues(top=16.dp,bottom=110.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(drafts,key={"draft:${it.id}"}) {pending ->
                CompactListRow(pending,{editing=pending},trailing="Continuar rascunho · ${pending.items.size} HQs")
            }
            if(active.isEmpty() && !state.loading) item {
                Column(Modifier.fillMaxWidth().padding(vertical=64.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.CollectionsBookmark,null,tint=t.textSecondary,modifier=Modifier.size(64.dp))
                    Text("Suas listas começam aqui",color=t.textPrimary,fontSize=22.sp,fontWeight=FontWeight.SemiBold)
                    Text("Organize HQs por tema ou crie seu ranking.",color=t.textSecondary)
                    EditorialAction("Criar minha primeira lista",{editing=ComicList()})
                }
            }
            if(featured!=null) item(key="featured:${featured.id}") {FeaturedListCard(featured,{viewing=featured})}
            items(active.filterNot {it.id==featured?.id},key={it.id}) {list -> CompactListRow(list,{viewing=list})}
        }
    }
    viewing?.let {selected ->
        val list=active.firstOrNull {it.id==selected.id}
        if(list!=null) ListDetailSheet(list,{viewing=null},onComic={next ->
            viewing=null;onComic(library.firstOrNull {ListComic.from(it).key==ListComic.from(next).key} ?: next)
        },onEdit={addingFromDetail=false;editing=list;viewing=null},onAdd={addingFromDetail=true;editing=list;viewing=null},onHighlight={scope.launch {
            val stamp=maxOf(System.currentTimeMillis(),(active.maxOfOrNull {it.featuredAt} ?: 0)+1)
            controller.save(list.copy(featuredAt=stamp),state.owner)
        }},busy=state.saving || state.loading)
    }
    editing?.let { list -> key(state.owner,list.id) {
        ListEditor(list,library,controller,onSearch={ editing=null;onSearch() },onOpenComic=onComic,onConnectAccount=onConnectAccount,startAdding=addingFromDetail) { editing=null;addingFromDetail=false;scope.launch {drafts=controller.drafts()} }
    } }
    if(publicBrowse) PublicListsSheet({publicBrowse=false},onComic)
}

@Composable
internal fun ListEditor(initial: ComicList, library: List<ComicEntry>, controller: CurationController,
    onSearch: (() -> Unit)?, onOpenComic: (ComicEntry)->Unit,onConnectAccount:(()->Unit)?=null,startAdding:Boolean=false, onClose: ()->Unit) {
    val state by controller.state.collectAsStateWithLifecycle()
    val owner=remember {state.owner}
    var draft by remember(initial.id) {mutableStateOf(initial)}
    var adding by remember {mutableStateOf(startAdding)}
    var artwork by remember {mutableStateOf(false)}
    var selection by remember {mutableStateOf(emptySet<String>())}
    var deleting by remember {mutableStateOf(false)}
    var discarding by remember {mutableStateOf(false)}
    var leaving by remember {mutableStateOf(false)}
    var visibility by remember {mutableStateOf(false)}
    var publishConfirmation by remember {mutableStateOf(false)}
    var connectConfirmation by remember {mutableStateOf(false)}
    var closeFailure by remember {mutableIntStateOf(0)}
    var reloadRequested by remember {mutableStateOf(false)}
    var query by remember {mutableStateOf("")}
    var moving by remember {mutableStateOf<ListComic?>(null)}
    var position by remember {mutableStateOf("")}
    var noting by remember {mutableStateOf<ListComic?>(null)}
    var note by remember {mutableStateOf("")}
    var dragging by remember {mutableStateOf<String?>(null)}
    var dragOffset by remember {mutableFloatStateOf(0f)}
    var dragStartCenter by remember {mutableFloatStateOf(0f)}
    val editorListState=rememberLazyListState()
    val haptic=LocalHapticFeedback.current
    val scope=rememberCoroutineScope()
    val t=LocalIOS27Theme.current
    val latest by rememberUpdatedState(draft)
    LaunchedEffect(state.loading,state.lists,reloadRequested) {
        if(reloadRequested && !state.loading && state.error==null) {
            state.lists.firstOrNull {it.id==initial.id && !it.deleted}?.let {draft=it}
            reloadRequested=false
        }
    }
    fun move(key: String, target: Int) {
        val items=latest.items.toMutableList(); val index=items.indexOfFirst {it.key==key}
        if(index>=0) {val item=items.removeAt(index);items.add(target.coerceIn(0,items.size),item);draft=latest.copy(items=items)}
    }
    fun closeDraft() { if(state.saving) return;scope.launch {if(controller.preserveDraft(draft,owner)) onClose() else closeFailure++} }
    CurationSheet(if(initial.revision==0L) "Criar lista" else "Editar lista",{if(draft==initial) onClose() else {leaving=true;closeFailure++}},busy=state.saving,error=state.error,reopenKey=closeFailure,
        actionLabel=if(state.saving) "Salvando…" else "Salvar lista",
        actionEnabled=draft.name.isNotBlank() && state.ready && !state.loading,
        onAction={scope.launch {if(controller.save(draft,owner)) onClose()}}) {
        CurationError(controller)
        if(state.error!=null && !state.ready) TextButton(onClick={controller.load(true)}) {Text("Tentar novamente",color=t.textPrimary)}
        if(state.error!=null && state.ready) TextButton(onClick={scope.launch {controller.preserveDraft(draft,owner);controller.load(true);reloadRequested=true}}) {
            Text("Recarregar versão salva · preservar rascunho",color=t.accent)
        }
        if(state.error!=null && state.ready && initial.revision>0) TextButton(enabled=!state.saving,onClick={scope.launch {
            if(controller.save(draft.copy(id=java.util.UUID.randomUUID().toString(),revision=0,deleted=false),owner)) onClose()
        }}) {Text("Salvar meu rascunho como nova lista",color=t.accent)}
        if(state.error!=null && state.ready) TextButton(enabled=!state.saving,onClick={discarding=true}) {Text("Descartar este rascunho",color=t.textSecondary)}
        LazyColumn(Modifier.weight(1f).testTag("list_editor_items"),state=editorListState,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                if(draft.items.isNotEmpty()) {
                    ListArtwork(draft,Modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(18.dp)))
                    OutlinedButton(enabled=!state.saving,onClick={artwork=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(50),
                        border=BorderStroke(1.dp,t.textSecondary.copy(alpha=.4f)),
                        colors=ButtonDefaults.outlinedButtonColors(containerColor=t.textPrimary.copy(alpha=.04f),contentColor=t.textPrimary)) {
                        Icon(Icons.Default.Image,null);Spacer(Modifier.width(8.dp));Text("Escolher capa")
                    }
                }
                CurationField("Nome",draft.name,100,enabled=!state.saving) {draft=draft.copy(name=it)}
                Spacer(Modifier.height(12.dp))
                CurationField("Descrição",draft.description,2000,enabled=!state.saving) {draft=draft.copy(description=it)}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Ranking numerado",Modifier.weight(1f),color=t.textPrimary)
                    AppleToggle(draft.ranked,onChange={if(!state.saving)draft=draft.copy(ranked=it)},modifier=Modifier.semantics {contentDescription="Ranking numerado";if(state.saving)disabled()})
                }
                Row(Modifier.fillMaxWidth().clickable(onClickLabel="Alterar visibilidade da lista",enabled=!state.saving) {visibility=true}.padding(vertical=14.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(if(draft.isPublic) Icons.Default.Public else Icons.Default.Lock,null,tint=t.textSecondary)
                    Column(Modifier.weight(1f)) {Text("Visibilidade",color=t.textSecondary,fontSize=12.sp);Text(if(draft.isPublic) "Pública" else "Privada",color=t.textPrimary)}
                    Icon(Icons.Default.ChevronRight,null,tint=t.textSecondary)
                }
                Text("${draft.items.size}/200 HQs",color=t.textSecondary,fontSize=12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick={selection=emptySet();adding=true},enabled=draft.items.size<200 && !state.saving,
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(50),
                    colors=ButtonDefaults.buttonColors(containerColor=t.textPrimary,contentColor=t.bgPrimary)) {
                    Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Adicionar HQ da biblioteca")
                }
                if(onSearch!=null) TextButton(onClick={scope.launch {if(controller.preserveDraft(draft,owner)){onClose();onSearch()}}},enabled=!state.saving) {Text("Buscar HQ no catálogo",color=t.accent)}
            }
            items(draft.items,key={it.key}) { entry ->
                Column(Modifier.fillMaxWidth()
                    .graphicsLayer {translationY=if(dragging==entry.key)dragOffset else 0f}
                    .padding(vertical=8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(48.dp).semantics {contentDescription="Arrastar ${entry.title} para reordenar"}
                            .pointerInput(entry.key,state.saving) {if(!state.saving)detectDragGesturesAfterLongPress(
                                onDragStart={
                                    dragging=entry.key;dragOffset=0f
                                    val row=editorListState.layoutInfo.visibleItemsInfo.firstOrNull {it.key==entry.key}
                                    dragStartCenter=(row?.offset ?: 0)+(row?.size ?: 0)/2f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag={change,amount -> change.consume();dragOffset+=amount.y},
                                onDragCancel={dragging=null;dragOffset=0f},
                                onDragEnd={
                                    val keys=latest.items.map {it.key}.toSet()
                                    val target=editorListState.layoutInfo.visibleItemsInfo.filter {it.key in keys}.minByOrNull {
                                        kotlin.math.abs(it.offset+it.size/2f-(dragStartCenter+dragOffset))
                                    }
                                    val index=latest.items.indexOfFirst {it.key==target?.key}
                                    if(index>=0)move(entry.key,index)
                                    dragging=null;dragOffset=0f
                                })},contentAlignment=Alignment.Center) {
                            Icon(Icons.Default.DragHandle,null,tint=t.textSecondary)
                        }
                        if(draft.ranked) Text("${draft.items.indexOf(entry)+1}",color=t.accent,fontWeight=FontWeight.Bold)
                        BookCover(entry.cover,entry.title,spine3d=false,modifier=Modifier.size(52.dp,78.dp))
                        Text(entry.title,color=t.textPrimary,modifier=Modifier.weight(1f).clickable(onClickLabel="Ver ${entry.title}") {
                            if(!state.saving) scope.launch {if(controller.preserveDraft(draft,owner)){onClose();onOpenComic(library.firstOrNull {ListComic.from(it).key==entry.key} ?: entry.comic())}}
                        })
                    }
                    if(entry.note.isNotBlank()) Text(entry.note,color=t.textSecondary,modifier=Modifier.padding(top=8.dp))
                    var menu by remember(entry.key) {mutableStateOf(false)}
                    Box(Modifier.align(Alignment.End)) {
                        IconButton(enabled=!state.saving,onClick={menu=true},modifier=Modifier.testTag("list_item_options_${entry.key}")) {
                            Icon(Icons.Default.MoreHoriz,"Opções de ${entry.title}",tint=t.textSecondary)
                        }
                        DropdownMenu(menu,{menu=false}) {
                            DropdownMenuItem(text={Text("Mover")},onClick={menu=false;moving=entry;position="${draft.items.indexOf(entry)+1}"})
                            DropdownMenuItem(text={Text("Nota")},onClick={menu=false;noting=entry;note=entry.note})
                            DropdownMenuItem(text={Text("Remover da lista")},onClick={menu=false;draft=draft.copy(items=draft.items.filterNot {it.key==entry.key})})
                        }
                    }
                    HorizontalDivider(color=t.divider)
                }
            }
            if(initial.revision>0) item(key="delete-list") {
                TextButton(onClick={deleting=true},enabled=!state.saving) {Text("Excluir lista",color=t.textSecondary)}
            }
        }
    }
    if(artwork) ListArtworkEditor(draft,{draft=it},{artwork=false})
    if(visibility) AlertDialog(onDismissRequest={visibility=false},title={Text("Visibilidade da lista")},text={Column {
        Row(Modifier.fillMaxWidth().clickable {draft=draft.copy(isPublic=false);visibility=false}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
            RadioButton(!draft.isPublic,null);Text("Privada · Apenas você")
        }
        Row(Modifier.fillMaxWidth().clickable {visibility=false;if(state.localOnly || !state.ready) connectConfirmation=true else publishConfirmation=true}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
            RadioButton(draft.isPublic,null);Text("Pública · Visível para todos")
        }
    }},confirmButton={TextButton(onClick={visibility=false}) {Text("Fechar")}})
    if(publishConfirmation) AlertDialog(onDismissRequest={publishConfirmation=false},title={Text("Publicar esta lista?")},
        text={Text("Ao salvar, o nome, a descrição e as HQs ficarão públicos. Notas dos itens e dados de leitura continuam privados.")},
        confirmButton={TextButton(onClick={draft=draft.copy(isPublic=true);publishConfirmation=false}) {Text("Usar pública")}},
        dismissButton={TextButton(onClick={publishConfirmation=false}) {Text("Cancelar")}})
    if(connectConfirmation) AlertDialog(onDismissRequest={connectConfirmation=false},title={Text("Conecte uma conta Google")},
        text={Text("Esta conta salva listas neste aparelho. Uma conta Google é necessária para publicar. Seu rascunho permanece nesta conta.")},
        confirmButton={TextButton(onClick={connectConfirmation=false;if(onConnectAccount!=null) scope.launch {if(controller.preserveDraft(draft,owner)){onClose();onConnectAccount()}}}) {Text(if(onConnectAccount!=null) "Conectar Google" else "Entendi")}},
        dismissButton={TextButton(onClick={connectConfirmation=false}) {Text("Cancelar")}})
    if(adding) CurationSheet("Adicionar HQs",{adding=false},actionLabel="Adicionar (${selection.size})",actionEnabled=selection.isNotEmpty() && !state.saving,onAction={
        draft=draft.copy(items=draft.items+library.map(ListComic::from).distinctBy {it.key}.filter {it.key in selection && draft.items.none {old->old.key==it.key}})
        adding=false;selection=emptySet()
    }) {
        AppleTextField(query,{query=it},"Buscar na biblioteca",modifier=Modifier.padding(16.dp))
        if(onSearch!=null) TextButton(enabled=!state.saving,onClick={scope.launch {
            if(controller.preserveDraft(draft,owner)){onClose();onSearch()}
        }}) {Text("Buscar HQ no catálogo",color=t.textPrimary)}
        val matches=library.filter {it.displayTitle.contains(query,true) || it.authors.contains(query,true)}.distinctBy {ListComic.from(it).key}
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp)) {
            if(matches.isEmpty()) item {Text("Nenhuma HQ encontrada",color=t.textSecondary)}
            items(matches,key={ListComic.from(it).key}) {comic ->
                val ref=ListComic.from(comic);val included=draft.items.any {it.key==ref.key}
                Row(Modifier.fillMaxWidth().clickable(enabled=!included && !state.saving && (ref.key in selection || draft.items.size+selection.size<200),role=Role.Checkbox) {
                    selection=if(ref.key in selection) selection-ref.key else selection+ref.key
                }.padding(vertical=8.dp).semantics {selected=included || ref.key in selection},
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    BookCover(ref.cover,ref.title,spine3d=false,modifier=Modifier.size(48.dp,72.dp))
                    Text(ref.title,Modifier.weight(1f),color=t.textPrimary,fontWeight=FontWeight.SemiBold)
                    if(included) Text("Já na lista",color=t.textSecondary,fontSize=12.sp)
                    else Checkbox(ref.key in selection,null)
                }
            }
        }
    }
    if(leaving) AlertDialog(onDismissRequest={leaving=false},title={Text("Sair da edição?")},
        text={Column {
            Text("Você pode guardar as alterações e continuar depois.")
            TextButton(onClick={leaving=false;discarding=true}) {Text("Descartar alterações")}
        }},
        confirmButton={TextButton(onClick={leaving=false;closeDraft()}) {Text("Guardar rascunho e sair")}},
        dismissButton={TextButton(onClick={leaving=false}) {Text("Continuar editando")}})
    if(discarding) AlertDialog(onDismissRequest={discarding=false},title={Text("Descartar este rascunho?")},
        text={Text("As alterações não salvas nesta lista serão descartadas. Seu acervo e a lista salva permanecem.")},
        confirmButton={TextButton(onClick={scope.launch {if(controller.discardDraft(draft.id,owner)) onClose()}}) {Text("Descartar rascunho")}},
        dismissButton={TextButton(onClick={discarding=false}) {Text("Continuar editando")}})
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("Excluir esta lista?")},
        text={Text("Suas HQs, resenhas e registros de leitura serão preservados.")},
        confirmButton={TextButton(onClick={scope.launch {if(controller.save(initial.copy(deleted=true,items=emptyList(),description="",name="Lista excluída"),owner)) onClose()}}) {Text("Excluir lista")}},
        dismissButton={TextButton(onClick={deleting=false}) {Text("Cancelar")}})
    moving?.let {entry -> AlertDialog(onDismissRequest={moving=null},title={Text("Mover para posição")},
        text={CurationField("Posição de 1 a ${draft.items.size}",position,4) {position=it.filter(Char::isDigit)}},
        confirmButton={TextButton(enabled=position.toIntOrNull() in 1..draft.items.size,onClick={move(entry.key,position.toInt()-1);moving=null}) {Text("Mover")}},
        dismissButton={TextButton(onClick={moving=null}) {Text("Cancelar")}}) }
    noting?.let {entry -> AlertDialog(onDismissRequest={noting=null},title={Text("Nota nesta lista")},
        text={CurationField("Sua nota",note,1000) {note=it}},
        confirmButton={TextButton(onClick={draft=draft.copy(items=draft.items.map {if(it.key==entry.key) it.copy(note=note) else it});noting=null}) {Text("Concluir")}},
        dismissButton={TextButton(onClick={noting=null}) {Text("Cancelar")}}) }
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
        CurationSheet("Por que esta HQ?",{show=false}) {
            CurationError(controller)
            LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    BookCover(recommendation.comic.coverUrl,recommendation.comic.displayTitle,spine3d=false,modifier=Modifier.size(76.dp,114.dp))
                    Text(recommendation.comic.displayTitle,color=t.textPrimary,fontWeight=FontWeight.Bold,fontSize=20.sp,modifier=Modifier.weight(1f))
                }}
                item {Text(recommendation.explanation?.detail ?: "${recommendation.reasonLabel}. Esta seleção antiga não tem uma referência detalhada salva.",color=t.textSecondary)}
                item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    EditorialAction("Mais como esta",{scope.launch {controller.feedback(recommendation.comic,"more")}},Modifier.weight(1f),!state.saving && !state.loading)
                    EditorialAction("Não tenho interesse",{scope.launch {controller.feedback(recommendation.comic,"hide")}},Modifier.weight(1f),!state.saving && !state.loading)
                } }
                item { TextButton(enabled=!state.saving,onClick={scope.launch {controller.feedback(recommendation.comic,"undo")}}) {Text("Desfazer preferência",color=t.accent)} }
                item { TextButton(onClick={show=false;onRegister(recommendation.comic)}) {Text("Já li / Quero ler…",color=t.accent)} }
                item {Text("Estas preferências não alteram seus favoritos nem avaliações.",color=t.textSecondary,fontSize=12.sp)}
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
