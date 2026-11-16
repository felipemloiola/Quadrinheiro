package com.example

import com.example.data.CoverComposition
import org.junit.Assert.*
import org.junit.Test

class CoverCompositionTest {
    @Test fun uniformArtworkStaysCenteredAndInvalidImagesAreSafe() {
        val focus=CoverComposition.focus(IntArray(96*144){0xff000000.toInt()},96,144)
        assertTrue(focus.y in .4f.. .5f)
        assertEquals(.45f,CoverComposition.focus(intArrayOf(),0,0).y)
        assertEquals(0f,CoverComposition.alignment(.5f,1f,1f))
    }
    @Test fun foregroundBelowEmptyTitleAreaPullsCropDownAndBoundsStayFinite() {
        val pixels=IntArray(96*144){0xff000000.toInt()}
        for(y in 75..110) for(x in 20..75) pixels[y*96+x]=if((x+y)%4<2) 0xffff5000.toInt() else 0xff204080.toInt()
        val focus=CoverComposition.focus(pixels,96,144)
        assertTrue(focus.y in .5f.. .8f)
        for(value in listOf(0f,.2f,.5f,1f)) assertTrue(CoverComposition.alignment(value,1f,.3f) in -1f..1f)
    }
}
