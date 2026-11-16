package com.example.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CoverComposition
import com.example.data.CoverFocus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val focusCache=object:LinkedHashMap<String,CoverFocus>(32,.75f,true){
    override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,CoverFocus>?)=size>32
}

@Composable
internal fun ComicArtwork(url:String, modifier:Modifier=Modifier, fade:Boolean=false) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var focus by remember(url){mutableStateOf(synchronized(focusCache){focusCache[url]} ?: CoverFocus())}
    var sourceRatio by remember(url){mutableFloatStateOf(2f/3f)}
    val request=remember(context,url){ImageRequest.Builder(context).data(url).size(960,960).allowHardware(false).crossfade(false)
        .diskCachePolicy(com.example.data.LibraryDiskCacheRegistry.diskCachePolicyFor(url)).build()}
    BoxWithConstraints(modifier.clipToBounds()) {
        val ratio=(maxWidth/maxHeight).coerceAtLeast(.1f)
        val alignment=BiasAlignment(0f,CoverComposition.alignment(focus.y,1f,sourceRatio/ratio))
        val fadeModifier=if(!fade) Modifier else Modifier.graphicsLayer {compositingStrategy=CompositingStrategy.Offscreen}
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Black,.12f to Color.Black.copy(alpha=.98f),.3f to Color.Black.copy(alpha=.88f),.48f to Color.Black.copy(alpha=.65f),.65f to Color.Black.copy(alpha=.34f),.8f to Color.Black.copy(alpha=.12f),.94f to Color.Transparent,1f to Color.Transparent),blendMode=BlendMode.DstIn)
            }
        AsyncImage(request,null,Modifier.fillMaxSize().then(fadeModifier),contentScale=ContentScale.Crop,alignment=alignment,
            onSuccess={state ->
                val bitmap=(state.result.drawable as? BitmapDrawable)?.bitmap
                if(bitmap!=null) {
                    sourceRatio=bitmap.width.toFloat()/bitmap.height
                    if(synchronized(focusCache){!focusCache.containsKey(url)}) scope.launch {
                        val result=withContext(Dispatchers.Default) {
                            val h=(96f/bitmap.width*bitmap.height).toInt().coerceIn(4,192)
                            val sample=android.graphics.Bitmap.createScaledBitmap(bitmap,96,h,true)
                            val pixels=IntArray(96*h);sample.getPixels(pixels,0,96,0,0,96,h)
                            val found=CoverComposition.focus(pixels,96,h)
                            if(sample!==bitmap)sample.recycle()
                            found
                        }
                        synchronized(focusCache){focusCache[url]=result};focus=result
                    }
                }
            })
    }
}

@Composable
internal fun ExpandedComicCover(url:String,title:String,source:Rect?=null,onClose:()->Unit) {
    val motion=com.example.ui.theme.LocalAppMotion.current
    val reduceTransparency=com.example.ui.theme.LocalAppSettings.current.reduceTransparency
    val progress=remember {Animatable(0f)}
    var closing by remember {mutableStateOf(false)}
    var viewport by remember {mutableStateOf<Rect?>(null)}
    var scale by remember(url){mutableFloatStateOf(1f)}
    var offset by remember(url){mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)}
    LaunchedEffect(closing) {
        progress.animateTo(if(closing)0f else 1f,motion.tween(if(closing)150 else 240))
        if(closing)onClose()
    }
    BackHandler {closing=true}
    Dialog(onDismissRequest={closing=true},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned {viewport=it.boundsInWindow()}) {
        Box(Modifier.fillMaxSize().graphicsLayer {alpha=progress.value}
            .background(Color.Black.copy(alpha=if(reduceTransparency).96f else .72f)).clickable(onClickLabel="Fechar capa ampliada") {closing=true})
        val width=minOf(maxWidth*.82f,maxHeight*.74f*2f/3f)
        Box(Modifier.align(Alignment.Center).width(width).aspectRatio(2f/3f)
            .graphicsLayer {
                val p=progress.value
                val area=viewport
                val destination=area?.let {Rect(it.center.x-size.width/2,it.center.y-size.height/2,it.center.x+size.width/2,it.center.y+size.height/2)}
                val origin=source
                val startX=if(origin!=null && destination!=null)origin.width/destination.width else .82f
                val startY=if(origin!=null && destination!=null)origin.height/destination.height else .82f
                scaleX=(startX+(1-startX)*p)*scale;scaleY=(startY+(1-startY)*p)*scale
                if(origin!=null && destination!=null) {
                    translationX=(origin.center.x-destination.center.x)*(1-p)+offset.x
                    translationY=(origin.center.y-destination.center.y)*(1-p)+offset.y
                }
                alpha=p.coerceIn(0f,1f)
            }.clip(RoundedCornerShape(12.dp)).clipToBounds().semantics(mergeDescendants=true) {
                stateDescription="Ampliação de ${(scale*100).toInt()}%"
                customActions=listOf(
                    CustomAccessibilityAction("Ampliar capa"){scale=(scale+.5f).coerceAtMost(5f);true},
                    CustomAccessibilityAction("Reduzir capa"){scale=(scale-.5f).coerceAtLeast(1f);if(scale==1f)offset=androidx.compose.ui.geometry.Offset.Zero;true},
                    CustomAccessibilityAction("Redefinir ampliação"){scale=1f;offset=androidx.compose.ui.geometry.Offset.Zero;true})
            }
                .pointerInput(url){detectTransformGestures {_,pan,zoom,_ ->
                    scale=(scale*zoom).coerceIn(1f,5f)
                    offset=if(scale==1f) androidx.compose.ui.geometry.Offset.Zero else androidx.compose.ui.geometry.Offset((offset.x+pan.x).coerceIn(-size.width*(scale-1)/2,size.width*(scale-1)/2),(offset.y+pan.y).coerceIn(-size.height*(scale-1)/2,size.height*(scale-1)/2))
                }}) {
            AsyncImage(url,title,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
        }
        IconButton(onClick={closing=true},modifier=Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(16.dp).background(Color.White.copy(alpha=.12f),RoundedCornerShape(50))) {Icon(Icons.Default.Close,"Fechar capa",tint=Color.White)}
    }
    }
}
