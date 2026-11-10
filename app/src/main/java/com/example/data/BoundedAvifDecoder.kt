package com.example.data

import android.os.Build
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.Decoder
import coil.decode.DecodeResult
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Dimension
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer

/** Bounded platform AVIF decoder; legacy BitmapFactory input guards remain unchanged. */
internal class BoundedAvifDecoder : Decoder.Factory {
    override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
        if(Build.VERSION.SDK_INT < 31)return null
        val source=result.source.source()
        if(!source.request(12))return null
        val header=source.peek().readByteArray(12)
        if(String(header,4,4,Charsets.US_ASCII)!="ftyp" ||
            String(header,8,4,Charsets.US_ASCII) !in setOf("avif","avis"))return null
        return Decoder { withContext(Dispatchers.IO) {
            if(source.request(8L*1024*1024+1))throw IOException("AVIF exceeds decode budget")
            var sampled=false
            val bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(source.readByteArray()))) { decoder,info,_ ->
                if(info.size.width <= 0 || info.size.height <= 0 || info.size.width > 8192 || info.size.height > 8192 ||
                    info.size.width.toLong()*info.size.height > 16L*1024*1024) throw IOException("AVIF dimensions exceed decode budget")
                val width=((options.size.width as? Dimension.Pixels)?.px ?: 1024).coerceIn(1,1024)
                val height=((options.size.height as? Dimension.Pixels)?.px ?: 1024).coerceIn(1,1024)
                val desired=if(options.scale==Scale.FILL) maxOf(width.toDouble()/info.size.width,height.toDouble()/info.size.height)
                    else minOf(width.toDouble()/info.size.width,height.toDouble()/info.size.height)
                val scale=minOf(1.0,desired,1024.0/info.size.width,1024.0/info.size.height)
                decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSize((info.size.width*scale).toInt().coerceAtLeast(1),
                    (info.size.height*scale).toInt().coerceAtLeast(1))
                sampled=scale<1.0
            }
            DecodeResult(BitmapDrawable(options.context.resources,bitmap),sampled)
        } }
    }
}
