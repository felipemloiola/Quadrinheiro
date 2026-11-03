package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

internal fun renderBannerCrop(bitmap: Bitmap, viewport: IntSize, zoom: Float, offset: Offset): String {
    val base = max(viewport.width.toFloat()/bitmap.width, viewport.height.toFloat()/bitmap.height)
    val output = Bitmap.createBitmap(1200,400,Bitmap.Config.ARGB_8888)
    val ratio=1200f/viewport.width
    val matrix=Matrix().apply {
        postTranslate(-bitmap.width/2f,-bitmap.height/2f);postScale(base*zoom,base*zoom)
        postTranslate(viewport.width/2f+offset.x,viewport.height/2f+offset.y);postScale(ratio,ratio)
    }
    Canvas(output).drawBitmap(bitmap,matrix,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    val bytes=ByteArrayOutputStream();var quality=88
    do { bytes.reset();output.compress(Bitmap.CompressFormat.JPEG,quality,bytes);quality-=8 } while(bytes.size()>140000 && quality>=32)
    output.recycle()
    return "data:image/jpeg;base64,"+Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP)
}

@Composable
fun BannerCropModal(bitmap: Bitmap,onDismiss:()->Unit,onConfirm:(String)->Unit) {
    val t=LocalIOS27Theme.current;val scope=rememberCoroutineScope()
    var zoom by remember { mutableFloatStateOf(1f) };var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize(1,1)) };var busy by remember { mutableStateOf(false) }
    fun clamp(candidate:Offset,z:Float):Offset {
        val base=max(viewport.width.toFloat()/bitmap.width,viewport.height.toFloat()/bitmap.height)
        val dx=max(0f,(bitmap.width*base*z-viewport.width)/2f);val dy=max(0f,(bitmap.height*base*z-viewport.height)/2f)
        return Offset(candidate.x.coerceIn(-dx,dx),candidate.y.coerceIn(-dy,dy))
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth().background(t.bgPrimary).padding(20.dp).testTag("banner_crop"),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Text("Ajustar banner",style=MaterialTheme.typography.titleLarge,color=t.textPrimary)
            Box(Modifier.fillMaxWidth().aspectRatio(3f).onSizeChanged { viewport=it }.pointerInput(bitmap) {
                detectTransformGestures { centroid,pan,change,_ ->
                    val next=(zoom*change).coerceIn(1f,4f)
                    offset=clamp(zoomAroundCentroid(offset,centroid-Offset(viewport.width/2f,viewport.height/2f),pan,zoom,next),next);zoom=next
                }
            }.then(Modifier.graphicsLayer { clip=true })) {
                Image(bitmap.asImageBitmap(),"Prévia do banner",Modifier.fillMaxSize().graphicsLayer { scaleX=zoom;scaleY=zoom;translationX=offset.x;translationY=offset.y },contentScale=ContentScale.Crop)
            }
            Text("Arraste para enquadrar. Use dois dedos para ampliar.",style=MaterialTheme.typography.bodySmall,color=t.textSecondary)
            Slider(zoom,{zoom=it;offset=clamp(offset,it)},valueRange=1f..4f)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick=onDismiss,enabled=!busy){Text("Cancelar",color=t.textSecondary)}
                TextButton(onClick={zoom=1f;offset=Offset.Zero},enabled=!busy){Text("Repor",color=t.textSecondary)}
                TextButton(onClick={busy=true;scope.launch { val result=withContext(Dispatchers.Default){renderBannerCrop(bitmap,viewport,zoom,offset)};onConfirm(result) }},enabled=!busy){Text(if(busy)"Salvando…" else "Concluir",color=t.accent)}
            }
        }
    }
}
