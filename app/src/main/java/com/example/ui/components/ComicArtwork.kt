package com.example.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
                drawRect(Brush.verticalGradient(0f to Color.Black,.45f to Color.Black,.72f to Color.Black.copy(alpha=.8f),1f to Color.Transparent),blendMode=BlendMode.DstIn)
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
internal fun ExpandedComicCover(url:String,title:String,onClose:()->Unit) {
    var scale by remember(url){mutableFloatStateOf(1f)}
    var offset by remember(url){mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        BackHandler {onClose()}
        Box(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            Box(Modifier.fillMaxSize().clipToBounds()
                .pointerInput(url){detectTransformGestures {_,pan,zoom,_ ->
                    scale=(scale*zoom).coerceIn(1f,5f)
                    offset=if(scale==1f) androidx.compose.ui.geometry.Offset.Zero else offset+pan
                }}) {
                AsyncImage(url,title,Modifier.fillMaxSize().graphicsLayer {scaleX=scale;scaleY=scale;translationX=offset.x.coerceIn(-size.width*(scale-1)/2,size.width*(scale-1)/2);translationY=offset.y.coerceIn(-size.height*(scale-1)/2,size.height*(scale-1)/2)},contentScale=ContentScale.Fit)
            }
            IconButton(onClick=onClose,modifier=Modifier.align(Alignment.TopEnd).padding(8.dp)) {Icon(Icons.Default.Close,"Fechar capa",tint=Color.White)}
        }
    }
}
