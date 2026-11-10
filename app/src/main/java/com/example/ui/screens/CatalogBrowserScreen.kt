package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CatalogWindow
import com.example.data.MultiApiSearchEngine
import com.example.model.ComicEntry
import com.example.ui.components.BookCover
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun CatalogBrowserDialog(universe:String,onClose:()->Unit,onComic:(ComicEntry)->Unit){
    val t=LocalIOS27Theme.current
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        val state=rememberLazyGridState()
        val window=remember{CatalogWindow()}
        val engine=remember{MultiApiSearchEngine()}
        val scope=rememberCoroutineScope()
        var entries by remember{mutableStateOf(emptyList<ComicEntry>())}
        var busy by remember{mutableStateOf(false)}
        var error by remember{mutableStateOf(false)}
        fun load(){
            if(busy||!window.hasMore)return
            busy=true;error=false
            scope.launch{
                try{window.accept(engine.fetchCatalogPage(window.nextPage,universe));entries=window.items}
                catch(cancel:CancellationException){throw cancel}
                catch(_:Exception){error=true}
                finally{busy=false}
            }
        }
        LaunchedEffect(Unit){load()}
        LaunchedEffect(entries.size,busy,error){
            if(!busy&&!error&&window.hasMore&&window.autoLoad)snapshotFlow{state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1}.collect {last->
                if(last>=entries.size-6)load()
            }
        }
        val isScrolled by remember(state) {
            androidx.compose.runtime.derivedStateOf {
                state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 24
            }
        }
        Box(Modifier.fillMaxSize().background(t.bgPrimary).windowInsetsPadding(WindowInsets.systemBars).testTag("catalog_browser")){
            LazyVerticalGrid(columns=GridCells.Fixed(3),state=state,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=68.dp,bottom=40.dp),
                horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
                item(span={GridItemSpan(3)}){
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text("Catálogo",color=t.textPrimary,fontSize=32.sp,fontWeight=FontWeight.Bold)
                        Text("Edições para descobrir e registrar.",color=t.textSecondary,fontSize=14.sp)
                    }
                }
                items(entries,key={it.id}){comic->Column(Modifier.clickable{onComic(comic)},verticalArrangement=Arrangement.spacedBy(8.dp)){
                    BookCover(comic.coverUrl,comic.displayTitle,spine3d=false,modifier=Modifier.fillMaxWidth().aspectRatio(2f/3f))
                    Text(comic.displayTitle,color=t.textPrimary,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text(comic.releaseYear.toString(),color=t.textSecondary,fontSize=11.sp)
                }}
                item(span={GridItemSpan(3)}){
                    Box(Modifier.fillMaxWidth().padding(vertical=20.dp),contentAlignment=androidx.compose.ui.Alignment.Center){
                        if(busy)CircularProgressIndicator(Modifier.size(24.dp),color=t.accent,strokeWidth=2.dp)
                        else if(error||(!window.autoLoad&&window.hasMore))TextButton(onClick={load()}){Text(if(error)"Tentar novamente" else "Carregar mais",color=t.accent)}
                        else if(entries.isEmpty())Text("Nenhuma edição disponível agora.",color=t.textSecondary)
                    }
                }
            }
            SettingsBackButton(
                onBack = onClose,
                collapsed = isScrolled,
                label = "Perfil",
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart).padding(start = 16.dp, top = 12.dp)
            )
        }
    }
}
