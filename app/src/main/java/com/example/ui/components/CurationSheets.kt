package com.example.ui.components

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlinx.coroutines.launch

/** Existing fluid motion/material pipeline, no extra backdrop or continuous animation. */
@Composable
internal fun CurationSheet(title: String, onClose: () -> Unit, busy: Boolean=false, error: String?=null, reopenKey: Int=0, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest={if(!busy)onClose()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        val theme=LocalIOS27Theme.current
        val sheet=rememberFluidSheet(.92f,onClose,{},backEnabled=!busy)
        BackHandler(enabled=busy) {}
        LaunchedEffect(error,reopenKey) {if(error!=null || reopenKey>0)sheet.enter()}
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=.32f)).clickable(enabled=!busy,onClickLabel="Fechar painel") {sheet.dismiss()})
            Column(Modifier.fillMaxSize().graphicsLayer { translationY=sheet.offset }
                .padding(top=12.dp).contentMaterialSurface(RoundedCornerShape(topStart=28.dp,topEnd=28.dp))
                .pointerInput(Unit) {detectTapGestures {}}.imePadding().navigationBarsPadding().padding(bottom=70.dp)) {
                Row(Modifier.fillMaxWidth().then(if(busy)Modifier else sheet.dragModifier).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(enabled=!busy,onClick={ sheet.dismiss() }) { Icon(Icons.Default.Close,"Fechar",tint=theme.textSecondary) }
                    Text(title,Modifier.weight(1f),color=theme.textPrimary,fontWeight=FontWeight.SemiBold,fontSize=18.sp)
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
    OutlinedTextField(value=value,enabled=enabled,onValueChange={ if(it.length<=max) onChange(it) },label={ Text(label) },
        modifier=Modifier.fillMaxWidth(),maxLines=if(max>300) 4 else 2,
        supportingText={ Text("${value.length}/$max") },
        colors=OutlinedTextFieldDefaults.colors(focusedTextColor=t.textPrimary,unfocusedTextColor=t.textPrimary,
            focusedLabelColor=t.accent,unfocusedLabelColor=t.textSecondary,cursorColor=t.accent,
            focusedBorderColor=t.accent,unfocusedBorderColor=t.textSecondary.copy(alpha=.35f)))
}

@Composable
fun AddToListAction(comic: ComicEntry, controller: CurationController) {
    val state by controller.state.collectAsStateWithLifecycle()
    var show by remember(comic.id,state.owner) { mutableStateOf(false) }
    TextButton(onClick={controller.load();show=true},modifier=Modifier.heightIn(min=48.dp)) {
        Icon(Icons.Default.PlaylistAdd,null,tint=LocalIOS27Theme.current.accent)
        Spacer(Modifier.width(8.dp));Text("Adicionar a uma lista",color=LocalIOS27Theme.current.accent)
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
    CurationSheet("Adicionar a uma lista",onClose,busy=state.saving,error=state.error) {
        CurationError(controller)
        Text("Listas privadas • não altera seu acervo",color=LocalIOS27Theme.current.textSecondary,modifier=Modifier.padding(16.dp))
        TextButton(onClick={creating=ComicList(items=listOf(ref))},enabled=!state.saving && !state.loading) { Text("Criar lista",color=LocalIOS27Theme.current.accent) }
        LazyColumn {
            items(pendingDrafts,key={"draft:${it.id}"}) {pending ->
                val included=pending.items.any {it.key==ref.key}
                TextButton(enabled=!state.saving && !state.loading && (included || pending.items.size<200),onClick={
                    creating=if(included) pending else pending.copy(items=pending.items+ref)
                }) {Text("Continuar rascunho: ${pending.name}",color=LocalIOS27Theme.current.accent)}
            }
            items(state.lists.filterNot {it.deleted},key={it.id}) { list ->
                val included=list.items.any {it.key==ref.key}
                TextButton(enabled=!included && !state.saving && !state.loading,onClick={scope.launch {
                    if(controller.save(list.copy(items=list.items+ref),state.owner)) onClose()
                }},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                    Text(list.name + if(included) " · já adicionada" else "",color=LocalIOS27Theme.current.textPrimary)
                }
            }
        }
    }
    creating?.let { key(state.owner,it.id) {ListEditor(it,emptyList(),controller,onSearch=null,onOpenComic={}) { creating=null;onClose() }} }
}

@Composable
fun PersonalListsScreen(library: List<ComicEntry>, controller: CurationController,
    onSection: (String)->Unit, onBack: ()->Unit, onSearch: ()->Unit, onComic: (ComicEntry)->Unit) {
    val state by controller.state.collectAsStateWithLifecycle()
    val t=LocalIOS27Theme.current
    val scope=rememberCoroutineScope()
    var editing by remember(state.owner) { mutableStateOf<ComicList?>(null) }
    var viewing by remember(state.owner) { mutableStateOf<ComicList?>(null) }
    var drafts by remember(state.owner) { mutableStateOf(emptyList<ComicList>()) }
    LaunchedEffect(state.owner) { controller.load(); drafts=controller.drafts() }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar ao perfil",tint=t.textPrimary) }
            Text("Biblioteca",Modifier.weight(1f),color=t.textPrimary,fontWeight=FontWeight.Bold,fontSize=26.sp)
            IconButton(onClick={editing=ComicList()},enabled=!state.loading && !state.saving) { Icon(Icons.Default.Add,"Criar lista",tint=t.accent) }
        }
        AppleSegmentedPicker(value="lists",onChange=onSection,options=listOf(SegmentOption("shelf","Estante"),SegmentOption("diary","Diário"),SegmentOption("lists","Listas")))
        CurationError(controller)
        TextButton(onClick={controller.load(true)}) { Text("Recarregar listas",color=t.accent) }
        LazyColumn(contentPadding=PaddingValues(bottom=110.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(drafts,key={"draft:${it.id}"}) {pending -> TextButton(onClick={editing=pending}) {Text("Continuar rascunho: ${pending.name}",color=t.accent)} }
            if(state.lists.none {!it.deleted} && !state.loading) item {
                Text("Suas listas, do seu jeito",color=t.textPrimary,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                Text("Organize HQs por tema ou crie seu ranking. As listas são privadas e não mudam seus registros de leitura.",color=t.textSecondary,modifier=Modifier.padding(vertical=12.dp))
                TextButton(onClick={editing=ComicList()}) { Text("Criar minha primeira lista",color=t.accent) }
            }
            items(state.lists.filterNot {it.deleted},key={it.id}) { list ->
                Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(20.dp))
                    .clickable(onClickLabel="Abrir lista ${list.name}") {viewing=list}.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        list.items.take(4).forEach { BookCover(it.cover,it.title,spine3d=false,modifier=Modifier.width(52.dp).height(78.dp)) }
                        if(list.items.isEmpty()) Icon(Icons.Default.FormatListNumbered,null,tint=t.accent,modifier=Modifier.size(52.dp))
                    }
                    Text(list.name,color=t.textPrimary,fontWeight=FontWeight.SemiBold,fontSize=17.sp)
                    Text("${list.items.size} HQs · ${if(list.ranked) "Ranking" else "Lista"} · Privada",color=t.textSecondary,fontSize=12.sp)
                    if(list.description.isNotBlank()) Text(list.description,color=t.textSecondary,maxLines=2)
                }
            }
        }
    }
    viewing?.let { selected ->
        val list=state.lists.firstOrNull {it.id==selected.id && !it.deleted} ?: selected
        CurationSheet(list.name,{viewing=null}) {
            Text("${list.items.size} HQs · Lista privada",color=t.textSecondary,modifier=Modifier.padding(horizontal=16.dp))
            TextButton(onClick={editing=list;viewing=null}) {Text("Editar lista",color=t.accent)}
            LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                if(list.description.isNotBlank()) item {Text(list.description,color=t.textPrimary)}
                items(list.items,key={it.key}) {entry ->
                    Row(Modifier.fillMaxWidth().clickable(onClickLabel="Ver ${entry.title}") {
                        viewing=null;onComic(library.firstOrNull {ListComic.from(it).key==entry.key} ?: entry.comic())
                    },horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(list.ranked) Text("${list.items.indexOf(entry)+1}",color=t.accent,fontWeight=FontWeight.Bold)
                        BookCover(entry.cover,entry.title,spine3d=false,modifier=Modifier.size(60.dp,90.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.title,color=t.textPrimary,fontWeight=FontWeight.SemiBold)
                            if(entry.note.isNotBlank()) Text(entry.note,color=t.textSecondary,modifier=Modifier.padding(top=8.dp))
                        }
                    }
                }
            }
        }
    }
    editing?.let { list -> key(state.owner,list.id) {
        ListEditor(list,library,controller,onSearch={ editing=null;onSearch() },onOpenComic=onComic) { editing=null;scope.launch {drafts=controller.drafts()} }
    } }
}

@Composable
internal fun ListEditor(initial: ComicList, library: List<ComicEntry>, controller: CurationController,
    onSearch: (() -> Unit)?, onOpenComic: (ComicEntry)->Unit, onClose: ()->Unit) {
    val state by controller.state.collectAsStateWithLifecycle()
    val owner=remember {state.owner}
    var draft by remember(initial.id) {mutableStateOf(initial)}
    var adding by remember {mutableStateOf(false)}
    var deleting by remember {mutableStateOf(false)}
    var discarding by remember {mutableStateOf(false)}
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
    CurationSheet(if(initial.revision==0L) "Criar lista" else "Editar lista",::closeDraft,busy=state.saving,error=state.error,reopenKey=closeFailure) {
        CurationError(controller)
        if(state.error!=null) TextButton(onClick={scope.launch {controller.preserveDraft(draft,owner);controller.load(true);reloadRequested=true}}) {
            Text("Recarregar versão salva · preservar rascunho",color=t.accent)
        }
        if(state.error!=null && initial.revision>0) TextButton(enabled=!state.saving,onClick={scope.launch {
            if(controller.save(draft.copy(id=java.util.UUID.randomUUID().toString(),revision=0,deleted=false),owner)) onClose()
        }}) {Text("Salvar meu rascunho como nova lista",color=t.accent)}
        if(state.error!=null) TextButton(enabled=!state.saving,onClick={discarding=true}) {Text("Descartar este rascunho",color=t.textSecondary)}
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick={scope.launch {if(controller.save(draft,owner)) onClose()}},enabled=draft.name.isNotBlank() && !state.saving && !state.loading) {Text(if(state.saving) "Salvando…" else "Salvar lista",color=t.accent)}
            if(initial.revision>0) TextButton(onClick={deleting=true},enabled=!state.saving) {Text("Excluir lista",color=t.textSecondary)}
        }
        LazyColumn(Modifier.weight(1f).testTag("list_editor_items"),state=editorListState,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                CurationField("Nome",draft.name,100,enabled=!state.saving) {draft=draft.copy(name=it)}
                CurationField("Descrição",draft.description,2000,enabled=!state.saving) {draft=draft.copy(description=it)}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Ranking numerado",Modifier.weight(1f),color=t.textPrimary)
                    AppleToggle(draft.ranked,onChange={if(!state.saving)draft=draft.copy(ranked=it)},modifier=Modifier.semantics {contentDescription="Ranking numerado";if(state.saving)disabled()})
                }
                Text("Privada · ${draft.items.size}/200 HQs. A ordem e as notas só são enviadas ao salvar.",color=t.textSecondary,fontSize=12.sp)
                TextButton(onClick={adding=true},enabled=draft.items.size<200 && !state.saving) {Text("Adicionar HQ da biblioteca",color=t.accent)}
                if(onSearch!=null) TextButton(onClick={scope.launch {if(controller.preserveDraft(draft,owner)){onClose();onSearch()}}},enabled=!state.saving) {Text("Buscar HQ no catálogo",color=t.accent)}
            }
            items(draft.items,key={it.key}) { entry ->
                Column(Modifier.fillMaxWidth()
                    .graphicsLayer {translationY=if(dragging==entry.key)dragOffset else 0f}
                    .contentMaterialSurface(RoundedCornerShape(16.dp)).padding(12.dp)) {
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
                        BookCover(entry.cover,entry.title,spine3d=false,modifier=Modifier.size(38.dp,57.dp))
                        Text(entry.title,color=t.textPrimary,modifier=Modifier.weight(1f).clickable(onClickLabel="Ver ${entry.title}") {
                            if(!state.saving) scope.launch {if(controller.preserveDraft(draft,owner)){onClose();onOpenComic(library.firstOrNull {ListComic.from(it).key==entry.key} ?: entry.comic())}}
                        })
                    }
                    if(entry.note.isNotBlank()) Text(entry.note,color=t.textSecondary,modifier=Modifier.padding(top=8.dp))
                    Row {
                        TextButton(enabled=!state.saving,onClick={moving=entry;position="${draft.items.indexOf(entry)+1}"}) {Text("Mover",color=t.accent)}
                        TextButton(enabled=!state.saving,onClick={noting=entry;note=entry.note}) {Text("Nota",color=t.accent)}
                        TextButton(enabled=!state.saving,onClick={draft=draft.copy(items=draft.items.filterNot {it.key==entry.key})}) {Text("Remover",color=t.textSecondary)}
                    }
                }
            }
        }
    }
    if(adding) CurationSheet("Adicionar da biblioteca",{adding=false}) {
        AppleTextField(query,{query=it},"Buscar na biblioteca",modifier=Modifier.padding(12.dp))
        LazyColumn {
            items(library.filter {it.displayTitle.contains(query,true) || it.authors.contains(query,true)},key={it.id}) { comic ->
                val ref=ListComic.from(comic); val included=draft.items.any {it.key==ref.key}
                TextButton(enabled=!included && draft.items.size<200,onClick={draft=draft.copy(items=draft.items+ref)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                    Text(comic.displayTitle+if(included) " · adicionada" else "",color=t.textPrimary)
                }
            }
        }
    }
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
                item {Text(recommendation.comic.displayTitle,color=t.textPrimary,fontWeight=FontWeight.Bold,fontSize=20.sp)}
                item {Text(recommendation.explanation?.detail ?: "${recommendation.reasonLabel}. Esta seleção antiga não tem uma referência detalhada salva.",color=t.textSecondary)}
                item { TextButton(enabled=!state.saving && !state.loading,onClick={scope.launch {controller.feedback(recommendation.comic,"more")}}) {Text("Mais como esta",color=t.accent)} }
                item { TextButton(enabled=!state.saving && !state.loading,onClick={scope.launch {controller.feedback(recommendation.comic,"hide")}}) {Text("Não tenho interesse",color=t.accent)} }
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
