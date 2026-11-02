package com.example

import androidx.compose.animation.core.SnapSpec
import androidx.compose.ui.geometry.Offset
import com.example.ui.components.projectSheetEndpoint
import com.example.ui.components.sheetResistance
import com.example.ui.components.zoomAroundCentroid
import com.example.ui.theme.AppMotion
import org.junit.Assert.*
import org.junit.Test

class FluidMotionPolicyTest {
    @Test fun flickDirectionChangesTheProjectedDestination() {
        assertTrue(projectSheetEndpoint(30f, 1400f) > 95f)
        assertTrue(projectSheetEndpoint(30f, -1400f) < -55f)
        assertEquals(30f, projectSheetEndpoint(30f, 0f), 0.001f)
    }

    @Test fun resistanceIsContinuousAndProgressive() {
        assertEquals(0f, sheetResistance(0f, 800f), 0f)
        assertEquals(100f, sheetResistance(100f, 800f), 0f)
        val near = -sheetResistance(-100f, 800f)
        val far = -sheetResistance(-200f, 800f)
        assertTrue(near > 0f && far > near)
        assertTrue(far - near < near)
    }

    @Test fun pinchKeepsTheSameImagePointUnderTheCentroid() {
        val offset = Offset(15f, -30f)
        val centroid = Offset(80f, 40f)
        val pan = Offset(5f, 9f)
        val imagePoint = (centroid - offset) / 2f
        val next = zoomAroundCentroid(offset, centroid, pan, 2f, 3f)
        assertEquals(centroid + pan, imagePoint * 3f + next)
    }

    @Test fun reducedMotionRemovesSpringTravelAndTransitionDelays() {
        assertTrue(AppMotion(reduced = true).spring<Float>() is SnapSpec)
        val fade = AppMotion(reduced = true).tween<Float>(400, 200)
        assertEquals(0, fade.durationMillis)
        assertEquals(0, fade.delay)
    }
}
