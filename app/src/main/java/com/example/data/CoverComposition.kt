package com.example.data

import kotlin.math.abs

data class CoverFocus(val x: Float = .5f, val y: Float = .45f)

/** Bounded, on-device visual heuristic; no network/ML, and no invented artwork. */
object CoverComposition {
    fun focus(pixels: IntArray, width: Int, height: Int): CoverFocus {
        if(width<4 || height<4 || pixels.size<width*height) return CoverFocus()
        val cropHeight=(width/2.4f).toInt().coerceIn(2,height)
        fun light(c:Int)=((c shr 16 and 255)*3+(c shr 8 and 255)*6+(c and 255))/10
        var best=Float.NEGATIVE_INFINITY;var bestY=height*.45f
        for(center in (height*.28f).toInt()..(height*.76f).toInt() step 2) {
            val start=(center-cropHeight/2).coerceIn(1,height-2)
            val end=(center+cropHeight/2).coerceIn(start,height-2)
            var energy=0f;var count=0
            for(y in start..end step 2) for(x in width/8 until width*7/8 step 2) {
                val c=pixels[y*width+x]
                val r=c shr 16 and 255;val g=c shr 8 and 255;val b=c and 255
                energy+=abs(light(c)-light(pixels[(y-1)*width+x]))+abs(light(c)-light(pixels[y*width+x-1]))+
                    (maxOf(r,g,b)-minOf(r,g,b))*.08f
                count++
            }
            val score=energy/count.coerceAtLeast(1)-abs(center.toFloat()/height-.46f)*12
            if(score>best){best=score;bestY=center.toFloat()}
        }
        return CoverFocus(.5f,(bestY/height).coerceIn(.2f,.8f))
    }
    fun alignment(focus:Float, source:Float, viewport:Float):Float =
        if(source<=viewport || source<=0 || viewport<=0) 0f
        else (((focus*source-viewport/2)/(source-viewport)).coerceIn(0f,1f)*2-1)
}
