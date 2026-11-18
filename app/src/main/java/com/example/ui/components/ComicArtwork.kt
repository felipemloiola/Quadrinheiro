package com.example.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.IntSize
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
internal fun ExpandedComicCover(url:String,title:String,source:Rect?=null,inPlace:Boolean=false,onClose:()->Unit) {
    val close by rememberUpdatedState(onClose)
    val content: @Composable () -> Unit = { ExpandedCoverContent(url,title,source,close) }
    if (inPlace) content()
    else Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        FullScreenDialogWindow(lightStatusBar=false)
        content()
    }
}

@Composable
private fun ExpandedCoverContent(url:String,title:String,source:Rect?,onClose:()->Unit) {
    val motion=com.example.ui.theme.LocalAppMotion.current
    val reduceTransparency=com.example.ui.theme.LocalAppSettings.current.reduceTransparency
    val progress=remember {Animatable(0f)}
    var closing by remember {mutableStateOf(false)}
    var viewport by remember {mutableStateOf<Rect?>(null)}
    var scale by remember(url){mutableFloatStateOf(1f)}
    var offset by remember(url){mutableStateOf(Offset.Zero)}
    var coverSize by remember { mutableStateOf(IntSize.Zero) }
    var adjusting by remember { mutableStateOf(false) }
    fun positionLimit(zoom: Float) = Offset(coverSize.width * (zoom - 1f) / 2f, coverSize.height * (zoom - 1f) / 2f)
    fun clamp(candidate: Offset, zoom: Float): Offset = positionLimit(zoom).let {
        Offset(candidate.x.coerceIn(-it.x, it.x), candidate.y.coerceIn(-it.y, it.y))
    }
    fun setZoom(zoom: Float) {
        if (closing) return
        scale = zoom.coerceIn(1f, 5f)
        offset = clamp(offset, scale)
    }
    val positioned = viewport != null
    LaunchedEffect(closing, positioned) {
        if (!positioned && !closing) return@LaunchedEffect
        progress.animateTo(if(closing)0f else 1f,motion.tween(if(closing)150 else 240))
        if(closing)onClose()
    }
    BackHandler {closing=true}
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned {viewport=it.boundsInWindow()}) {
        Box(Modifier.fillMaxSize().graphicsLayer {alpha=progress.value}
            .background(Color.Black.copy(alpha=if(reduceTransparency).96f else .72f)).clickable(onClickLabel="Fechar capa ampliada") {closing=true})
        val width=minOf(maxWidth*.82f,maxHeight*.74f*2f/3f)
        Box(Modifier.align(Alignment.Center).width(width).aspectRatio(2f/3f)
            .onSizeChanged { coverSize = it; offset = clamp(offset, scale) }
            .graphicsLayer {
                val p=progress.value
                val area=viewport
                val origin=source
                val startX=origin?.width?.div(size.width.coerceAtLeast(1f)) ?: .94f
                val startY=origin?.height?.div(size.height.coerceAtLeast(1f)) ?: .94f
                val zoom = 1f + (scale - 1f) * p
                scaleX=(startX+(1-startX)*p)*zoom
                scaleY=(startY+(1-startY)*p)*zoom
                translationX = offset.x * p
                translationY = offset.y * p
                if(origin!=null && area!=null) {
                    translationX += (origin.center.x-area.center.x)*(1-p)
                    translationY += (origin.center.y-area.center.y)*(1-p)
                }
                alpha = if (area == null) 0f else (p * 12f).coerceIn(0f, 1f)
                shape = RoundedCornerShape(18.dp)
                clip = true
            }.semantics(mergeDescendants=true) {
                contentDescription = title
                stateDescription="Ampliação de ${(scale*100).toInt()}%"
                customActions=listOf(
                    CustomAccessibilityAction("Ampliar capa"){setZoom(scale + .5f);true},
                    CustomAccessibilityAction("Reduzir capa"){setZoom(scale - .5f);true},
                    CustomAccessibilityAction("Redefinir ampliação"){setZoom(1f);true})
            }
                .pointerInput(url, closing){
                    if (closing) return@pointerInput
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(1f, 5f)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        offset = clamp(zoomAroundCentroid(offset, centroid - center, pan, scale, next), next)
                        scale = next
                    }
                }) {
            BookCover(url,title,spine3d=false,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize().clearAndSetSemantics {})
        }
        val theme = com.example.ui.theme.LocalIOS27Theme.current
        IconButton(onClick={closing=true},modifier=Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(16.dp)
            .size(48.dp).graphicsLayer { alpha = progress.value }
            .contentMaterialSurface(com.kyant.shapes.ContinuousCapsule)) {
            Icon(Icons.Default.Close,"Fechar capa",tint=theme.textPrimary)
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
            .graphicsLayer { alpha = progress.value }
            .widthIn(max = 360.dp).contentMaterialSurface(com.kyant.shapes.ContinuousRoundedRectangle(18.dp))
            .padding(horizontal = 12.dp)) {
            TextButton(onClick = { adjusting = !adjusting }) {
                Text(if (adjusting) "Ocultar ajustes" else "Ajustar ampliação", color = theme.textPrimary)
            }
            if (adjusting) {
                AppleSlider(scale, ::setZoom, valueRange = 1f..5f,
                    modifier = Modifier.semantics { contentDescription = "Zoom da capa" })
                CropPositionControls(offset, positionLimit(scale), { if (!closing) offset = it }, theme.textPrimary)
            }
        }
    }
}
