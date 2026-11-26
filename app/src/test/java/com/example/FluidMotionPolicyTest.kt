package com.example

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Offset
import com.example.ui.components.projectSheetEndpoint
import com.example.ui.components.sheetResistance
import com.example.ui.components.zoomAroundCentroid
import com.example.ui.theme.AppMotion
import org.junit.Assert.*
import org.junit.Test

class FluidMotionPolicyTest {
    @Test fun normalMotionPreservesTheRequestedLiquidSpring() {
        val spring = AppMotion().spring<Float>(0.5f, 300f) as SpringSpec
        assertEquals(0.5f, spring.dampingRatio, 0f)
        assertEquals(300f, spring.stiffness, 0f)
    }
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

    @Test fun defaultTweenUsesSub300msAndResponsiveEaseOutStrongCurve() {
        val defaultTween = AppMotion(reduced = false).tween<Float>()
        assertTrue("Default UI duration must be under 300ms", defaultTween.durationMillis < 300)
        assertEquals(AppMotion.UI_ENTER_MS, defaultTween.durationMillis)
        assertEquals(AppMotion.EaseOutStrong, defaultTween.easing)
        // Strong ease-out covers >50% of distance in the first 30% of time (responsive start, no ease-in lag)
        assertTrue(AppMotion.EaseOutStrong.transform(0.30f) > 0.50f)
        assertTrue(AppMotion.EaseOutQuint.transform(0.30f) > 0.50f)
        assertTrue(AppMotion.UI_EXIT_MS < AppMotion.UI_ENTER_MS)
    }

    @Test fun ios26SpringPresetsEnforceOrganicDecelerationAndFasterExits() {
        val motion = AppMotion(reduced = false)
        val modalEnter = motion.ios26ModalSpring<Float>() as SpringSpec
        val modalExit = motion.ios26ModalExitSpring<Float>() as SpringSpec
        val detailZoom = motion.ios26DetailZoomSpring<Float>() as SpringSpec
        val jellyPress = motion.ios26JellyPressSpring<Float>() as SpringSpec
        val menuX = motion.ios26FloatingMenuSpring<Float>(axis = 0) as SpringSpec
        val menuY = motion.ios26FloatingMenuSpring<Float>(axis = 1) as SpringSpec
        val menuCollapse = motion.ios26FloatingMenuCollapseSpring<Float>() as SpringSpec

        assertTrue("Modal exit must be stiffer than entry", modalExit.stiffness > modalEnter.stiffness)
        assertTrue("Modal exit must be at least as damped as entry", modalExit.dampingRatio >= modalEnter.dampingRatio)
        assertTrue("Menu collapse must be stiffer than open", menuCollapse.stiffness > menuX.stiffness && menuCollapse.stiffness > menuY.stiffness)
        assertTrue("Detail zoom spring must be organically damped without excessive bounce", detailZoom.dampingRatio in 0.85f..1.0f)
        assertTrue("Jelly press spring must be elastic", jellyPress.dampingRatio < 0.75f)
        assertNotEquals("Floating menu axes should have organic phase offset", menuX.stiffness, menuY.stiffness)
    }

    @Test fun dragReleaseSpringAppliesBounceOnlyWhenMomentumWasCarried() {
        val motion = AppMotion(reduced = false)
        val withMomentum = motion.ios26DragReleaseSpring<Float>(carriedMomentum = true) as SpringSpec
        val withoutMomentum = motion.ios26DragReleaseSpring<Float>(carriedMomentum = false) as SpringSpec

        assertTrue("Carried momentum allows subtle bounce", withMomentum.dampingRatio < 1.0f)
        assertEquals("Programmatic settle is critically damped", 1.0f, withoutMomentum.dampingRatio, 0.001f)
    }

    @Test fun pressFeedbackTweenIsInstantSub150msWithEaseOutQuint() {
        val pressTween = AppMotion(reduced = false).pressFeedbackTween<Float>()
        assertTrue("Press feedback must be <= 150ms", pressTween.durationMillis <= 150)
        assertEquals(AppMotion.PRESS_FEEDBACK_MS, pressTween.durationMillis)
        assertEquals(AppMotion.EaseOutQuint, pressTween.easing)
        assertEquals(0, AppMotion(reduced = true).pressFeedbackTween<Float>().durationMillis)
    }

    @Test fun customEasingCurvesAndMomentumPhysicsBehaveAsExpected() {
        assertTrue(AppMotion.EaseSheet.transform(0.35f) > 0.65f)
        // EaseInOutStrong (0.77, 0, 0.175, 1): slow launch, steep center, soft landing
        assertTrue(AppMotion.EaseInOutStrong.transform(0.15f) < 0.10f)
        assertTrue(AppMotion.EaseInOutStrong.transform(0.85f) > 0.90f)

        val projectedForward = AppMotion.projectMomentum(100f, 1000f, decelerationRate = 0.99f)
        val projectedBackward = AppMotion.projectMomentum(100f, -1000f, decelerationRate = 0.99f)
        assertTrue(projectedForward > 190f)
        assertTrue(projectedBackward < 10f)
        assertEquals(100f, AppMotion.projectMomentum(100f, 0f), 0.001f)

        assertEquals(0f, AppMotion.rubberband(0f, 600f), 0f)
        val r1 = AppMotion.rubberband(100f, 600f)
        val r2 = AppMotion.rubberband(200f, 600f)
        assertTrue("Rubberband yields less the further you pull", r1 > 0f && r2 > r1 && (r2 - r1) < r1)
        assertEquals(-r1, AppMotion.rubberband(-100f, 600f), 0.001f)
    }

    @Test fun reducedMotionCollapsesAllIos26SpringsToSnap() {
        val reduced = AppMotion(reduced = true)
        assertTrue(reduced.ios26ModalSpring<Float>() is SnapSpec)
        assertTrue(reduced.ios26ModalExitSpring<Float>() is SnapSpec)
        assertTrue(reduced.ios26DetailZoomSpring<Float>() is SnapSpec)
        assertTrue(reduced.ios26JellyPressSpring<Float>() is SnapSpec)
        assertTrue(reduced.ios26FloatingMenuSpring<Float>(0) is SnapSpec)
        assertTrue(reduced.ios26FloatingMenuSpring<Float>(1) is SnapSpec)
        assertTrue(reduced.ios26FloatingMenuCollapseSpring<Float>() is SnapSpec)
        assertTrue(reduced.ios26DragReleaseSpring<Float>(true) is SnapSpec)
        assertTrue(reduced.ios26DragReleaseSpring<Float>(false) is SnapSpec)
    }
}
