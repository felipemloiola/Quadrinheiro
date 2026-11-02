package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.AppSettings
import com.example.ui.components.AppleStepper
import com.example.ui.components.AppleSlider
import com.example.ui.components.StarRating
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.components.LiquidToggle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FluidControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun switchAccessibilityActionCommitsExactlyOnce() {
        val checked = mutableStateOf(false)
        var commits = 0
        compose.setContent {
            QuadrinheiroTheme(settings = AppSettings(reduceMotion = true)) {
                LiquidToggle({ checked.value }, { checked.value = it; commits++ }, emptyBackdrop(), Modifier.testTag("switch"))
            }
        }
        compose.onNodeWithTag("switch").assertHeightIsAtLeast(48.dp).performClick().assertIsOn()
        compose.runOnIdle { assertEquals(1, commits) }
    }

    @Test fun verticalGestureAndCancellationNeverToggleSwitch() {
        var commits = 0
        compose.setContent {
            QuadrinheiroTheme(settings = AppSettings(reduceMotion = true)) {
                LiquidToggle({ false }, { commits++ }, emptyBackdrop(), Modifier.testTag("switch"))
            }
        }
        compose.onNodeWithTag("switch").performTouchInput {
            down(center); moveTo(center + Offset(0f, 120f)); up()
        }
        compose.onNodeWithTag("switch").performTouchInput { down(center); cancel() }
        compose.runOnIdle { assertEquals(0, commits) }
    }


    @Test fun verticalGestureOverRatingDoesNotChangeIt() {
        var commits = 0
        compose.setContent {
            QuadrinheiroTheme(settings = AppSettings(reduceMotion = true)) {
                StarRating(rating = 3f, onChange = { commits++ }, modifier = Modifier.testTag("rating"))
            }
        }
        compose.onNodeWithTag("rating").assertHeightIsAtLeast(48.dp).performTouchInput {
            down(center); moveTo(center + Offset(0f, 120f)); up()
        }
        compose.runOnIdle { assertEquals(0, commits) }
    }


    @Test fun heldStepperRepeatsWithoutAnExtraReleaseIncrement() {
        val count = mutableStateOf(0)
        compose.setContent {
            QuadrinheiroTheme(settings = AppSettings(reduceMotion = true)) {
                Column { AppleStepper(value = count.value, max = 100, onChange = { count.value = it }) }
            }
        }
        compose.mainClock.autoAdvance = false
        val button = compose.onNodeWithContentDescription("Aumentar")
        button.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(850)
        compose.waitForIdle()
        val beforeRelease = count.value
        assertTrue("A held press must repeat", beforeRelease >= 2)
        button.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertEquals(beforeRelease, count.value) }
    }
}
