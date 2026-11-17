package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.ComicList
import com.example.data.PublicListsRepository
import com.example.model.ComicEntry
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun PublicListsSheet(onClose:()->Unit,onComic:(ComicEntry)->Unit) {
    val context=LocalContext.current
    val repository=remember {PublicListsRepository(context)}
    val scope=rememberCoroutineScope()
    val theme=LocalIOS27Theme.current
    var lists by remember {mutableStateOf(emptyList<ComicList>())}
    var loading by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf(false)}
    var selected by remember {mutableStateOf<ComicList?>(null)}
    fun load() {if(loading)return;scope.launch {
        loading=true;error=false
        try {lists=repository.load()}
        catch(cancel:CancellationException){throw cancel}
        catch(_:Exception){error=true}
        finally {loading=false}
    }}
    LaunchedEffect(Unit){load()}
    CurationSheet("Listas públicas",onClose) {
        if(loading) LoadingLine(Modifier.fillMaxWidth())
        if(error) {
            Text("Não foi possível carregar as listas públicas.",color=theme.textSecondary,modifier=Modifier.padding(16.dp))
            TextButton(onClick=::load){Text("Tentar novamente")}
        }
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(!loading && !error && lists.isEmpty()) item {Text("Nenhuma lista publicada por enquanto",color=theme.textSecondary)}
            itemsIndexed(lists,key={index,it->"$index:${it.id}"}) {_,list -> CompactListRow(list,{selected=list})}
        }
    }
    selected?.let {list -> ListDetailSheet(list,{selected=null},onComic={selected=null;onClose();onComic(it)})}
}
