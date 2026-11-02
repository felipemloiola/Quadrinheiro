package com.example

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.catalog.utils.DampedDragAnimation
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiquidSpringTest {
    @get:Rule val compose = createComposeRule()

    @Test fun independentAxesExpandAndRepressInterruptsRetraction() {
        lateinit var controller: DampedDragAnimation
        compose.setContent {
            val scope = rememberCoroutineScope()
            controller = remember {
                DampedDragAnimation(scope, 0f, 0f..1f, 0.001f, 1f, 1.5f,
                    onDragStarted = {}, onDragStopped = {}, onDrag = { _, _ -> })
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { controller.press() }
        compose.mainClock.advanceTimeBy(112)
        compose.runOnIdle {
            assertTrue(controller.scaleX > 1.1f)
            assertTrue("X/Y must retain independent spring responses", kotlin.math.abs(controller.scaleX - controller.scaleY) > 0.001f)
            controller.updateValue(0.4f)
            controller.updateValue(controller.targetValue + 0.4f)
            assertEquals(0.8f, controller.targetValue, 0.001f)
            controller.release()
        }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle { controller.press() }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle {
            assertEquals("An old release must not shrink a new press", 1.5f, controller.scaleX, 0.01f)
            assertEquals("Cancelled travel must not leave a permanent velocity stretch", 0f, controller.velocity, 0.01f)
            controller.release()
        }
        compose.mainClock.advanceTimeBy(1200)
        compose.runOnIdle {
            assertEquals(1f, controller.scaleX, 0.01f)
            assertEquals(1f, controller.scaleY, 0.01f)
        }
    }

    @Test fun reducedMotionSuppressesExpansionAndVelocityDeformation() {
        lateinit var controller: DampedDragAnimation
        compose.setContent {
            val scope = rememberCoroutineScope()
            controller = remember {
                DampedDragAnimation(scope, 0f, 0f..1f, 0.001f, 1f, 1.5f,
                    onDragStarted = {}, onDragStopped = {}, onDrag = { _, _ -> },
                    reduceMotion = { true })
            }
        }
        compose.runOnIdle { controller.press(); controller.updateValue(1f) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1f, controller.value, 0f)
            assertEquals(1f, controller.scaleX, 0f)
            assertEquals(1f, controller.scaleY, 0f)
            assertEquals(0f, controller.velocity, 0f)
            controller.release()
        }
    }
}
