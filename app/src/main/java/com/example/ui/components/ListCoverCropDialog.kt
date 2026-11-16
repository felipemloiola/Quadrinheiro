package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ComicList

/** Shared app sheet and controls; the original image stays continuous under the crop mask. */
@Composable
internal fun ListCoverCropDialog(initial:ComicList,onApply:(ComicList)->Unit,onBack:()->Unit) {
    var draft by remember(initial.id,initial.coverKey){mutableStateOf(initial)}
    val latest by rememberUpdatedState(draft)
    val white=Color.White
    val t=com.example.ui.theme.LocalIOS27Theme.current
    val panel=rememberCurationPanelState()
    var applyRequested by remember {mutableStateOf(false)}
    CurationSheet("Ajustar capa",{if(applyRequested)onApply(draft) else onBack()},panel=panel,actionLabel="Usar capa",onAction={applyRequested=true;panel.close()}) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(20.dp)) {
            val studioHeight=(maxHeight*.65f).coerceAtMost(380.dp)
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                Text("Arraste e dê zoom para enquadrar",color=t.textSecondary,fontSize=13.sp)
                BoxWithConstraints(Modifier.fillMaxWidth().height(studioHeight).clip(RoundedCornerShape(28.dp)).background(Color(0xFF14161D))
                    .border(1.dp,white.copy(alpha=.16f),RoundedCornerShape(28.dp)),contentAlignment=Alignment.Center) {
                    val cropWidth=minOf(maxWidth*.88f,maxHeight*1.6f)
                    val density=androidx.compose.ui.platform.LocalDensity.current
                    val cropW=with(density){cropWidth.toPx()}
                    val cropH=cropW/2f
                    var ratio by remember(initial.coverKey){mutableFloatStateOf(2f/3f)}
                    val context=androidx.compose.ui.platform.LocalContext.current
                    val request=remember(initial.coverKey){coil.request.ImageRequest.Builder(context).data(initial.coverItem()?.cover).size(1024,1024).crossfade(false)
                        .diskCachePolicy(com.example.data.LibraryDiskCacheRegistry.diskCachePolicyFor(initial.coverItem()?.cover.orEmpty())).build()}
                    AsyncImage(request,null,Modifier.fillMaxSize().graphicsLayer {
                        val fitW=minOf(size.width,size.height*ratio);val fitH=fitW/ratio
                        val factor=maxOf(cropW/fitW,cropH/fitH)*latest.coverZoom
                        scaleX=factor;scaleY=factor
                        translationX=(.5f-latest.coverX)*(fitW*factor-cropW)
                        translationY=(.5f-latest.coverY)*(fitH*factor-cropH)
                    },contentScale=ContentScale.Fit,onSuccess={ratio=it.result.drawable.intrinsicWidth.toFloat()/it.result.drawable.intrinsicHeight.coerceAtLeast(1)})
                    Canvas(Modifier.matchParentSize().graphicsLayer {compositingStrategy=androidx.compose.ui.graphics.CompositingStrategy.Offscreen}
                        .pointerInput(initial.id,initial.coverKey,cropW) {detectTransformGestures {centroid,pan,zoom,_ ->
                            val old=latest;val next=(old.coverZoom*zoom).coerceIn(1f,3f)
                            val pivot=centroid-Offset(size.width/2f,size.height/2f)
                            draft=old.copy(coverZoom=next,
                                coverX=(old.coverX-pan.x/cropW-pivot.x/cropW*(next-old.coverZoom)/next).coerceIn(0f,1f),
                                coverY=(old.coverY-pan.y/cropH-pivot.y/cropH*(next-old.coverZoom)/next).coerceIn(0f,1f))
                        }}.semantics {contentDescription="Enquadramento da capa. Arraste para mover e use dois dedos para ampliar.";stateDescription="Zoom "+(draft.coverZoom*100).toInt()+"%"
                            customActions=listOf(
                                CustomAccessibilityAction("Mover para cima"){draft=draft.copy(coverY=(draft.coverY+.1f).coerceAtMost(1f));true},
                                CustomAccessibilityAction("Mover para baixo"){draft=draft.copy(coverY=(draft.coverY-.1f).coerceAtLeast(0f));true},
                                CustomAccessibilityAction("Mover para a esquerda"){draft=draft.copy(coverX=(draft.coverX+.1f).coerceAtMost(1f));true},
                                CustomAccessibilityAction("Mover para a direita"){draft=draft.copy(coverX=(draft.coverX-.1f).coerceAtLeast(0f));true})}) {
                        val left=(size.width-cropW)/2;val top=(size.height-cropH)/2
                        drawRect(Color.Black.copy(alpha=.68f))
                        drawRect(Color.Transparent,Offset(left,top),androidx.compose.ui.geometry.Size(cropW,cropH),blendMode=androidx.compose.ui.graphics.BlendMode.Clear)
                        for(i in 1..2) {
                            drawLine(white.copy(alpha=.22f),Offset(left+cropW*i/3f,top),Offset(left+cropW*i/3f,top+cropH),1.dp.toPx())
                            drawLine(white.copy(alpha=.22f),Offset(left,top+cropH*i/3f),Offset(left+cropW,top+cropH*i/3f),1.dp.toPx())
                        }
                        drawRect(white.copy(alpha=.9f),Offset(left,top),androidx.compose.ui.geometry.Size(cropW,cropH),style=androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                    }
                }
                Row(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(20.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    ListArtwork(draft,Modifier.width(64.dp).aspectRatio(2f).clip(RoundedCornerShape(6.dp)))
                    Column(Modifier.weight(1f)) {
                        Text("Zoom · ${(draft.coverZoom*100).toInt()}%",color=t.textPrimary,fontSize=12.sp)
                        AppleSlider(value=draft.coverZoom,min=1f,max=3f,step=.01f,onChange={draft=draft.copy(coverZoom=it)},modifier=Modifier.semantics {contentDescription="Zoom da capa"})
                    }
                }
                CurationGlassAction("Centralizar",{draft=draft.copy(coverX=.5f,coverY=.5f,coverZoom=1f)},Modifier.fillMaxWidth(),icon=Icons.Default.Replay)
            }
        }
    }
}
