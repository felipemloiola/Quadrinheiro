package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.AppSettings
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidSlider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoredControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun originalSelectorsSliderAndTabBarStillChangeTheirValues() {
        val selected = mutableStateOf(0)
        val tab = mutableStateOf(0)
        val value = mutableStateOf(0.25f)
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = "dark")) {
                Column(Modifier.width(300.dp)) {
                    AppleSegmentedPicker(listOf("Estante", "Diário"), selected.value, { selected.value = it })
                    LiquidSlider({ value.value }, { value.value = it }, 0f..1f, 0.001f, emptyBackdrop(), Modifier.testTag("slider"))
                    LiquidBottomTabs({ tab.value }, { tab.value = it }, emptyBackdrop(), 3) {
                        listOf("Lendo", "Biblioteca", "Ajustes").forEachIndexed { index, label ->
                            LiquidBottomTab({ tab.value = index }) { Text(label) }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Diário").performClick()
        compose.runOnIdle { assertEquals(1, selected.value) }
        compose.onNodeWithTag("slider").performTouchInput { click(Offset(width * 0.8f, centerY)) }
        compose.runOnIdle { assertTrue(value.value > 0.5f) }
        compose.onNodeWithText("Biblioteca").performClick()
        compose.runOnIdle { assertEquals(1, tab.value) }
    }
}
