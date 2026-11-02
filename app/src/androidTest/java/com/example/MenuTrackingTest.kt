package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.components.*
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuTrackingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draggingOutsideRetainsAndCommitsLastValidMenuRow() {
        var chosen = -1
        val host = LiquidGlassMenuHostState()
        compose.setContent {
            QuadrinheiroTheme(com.example.model.AppSettings(colorScheme = "dark")) {
                Box(Modifier.fillMaxSize()) { LiquidGlassMenuOverlayHost(host, emptyBackdrop()) }
            }
        }
        compose.runOnIdle {
            host.showMenu(LiquidGlassMenuRequest("test", IntRect(80, 100, 200, 160),
                items = listOf("Mais Recentes", "Melhor Avaliação", "Título").mapIndexed { i, label ->
                    LiquidGlassMenuItemData(label, selected = i == 0, onClick = { chosen = i })
                }, onDismiss = {}))
        }
        compose.waitForIdle()
        val target = compose.onNodeWithText("Melhor Avaliação").getUnclippedBoundsInRoot()
        val density = compose.density
        val point = with(density) { Offset(((target.left + target.right) / 2).toPx(), ((target.top + target.bottom) / 2).toPx()) }
        compose.onRoot().performTouchInput {
            down(point)
            advanceEventTime(500)
            moveTo(Offset(width - 1f, point.y))
            advanceEventTime(300)
            up()
        }
        compose.runOnIdle { assertEquals(1, chosen) }
    }
}
