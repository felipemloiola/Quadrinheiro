package com.example

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.AppSettings
import com.example.ui.components.*
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class MenuTrackingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draggingOutsideRetainsAndCommitsLastValidMenuRow() {
        var chosen = -1
        val host = LiquidGlassMenuHostState()
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = "dark")) {
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

    @Test
    fun longMenuKeepsItsLastActionReachableInACompactViewport() {
        val host = LiquidGlassMenuHostState()
        var chosen = -1
        compose.setContent {
            QuadrinheiroTheme(AppSettings(reduceMotion = true, reduceTransparency = true)) {
                Box(Modifier.fillMaxWidth().height(220.dp)) {
                    LiquidGlassMenuOverlayHost(host, emptyBackdrop())
                }
            }
        }
        compose.runOnIdle {
            host.showMenu(LiquidGlassMenuRequest(
                id = "long-menu",
                anchorBounds = IntRect(80, 100, 200, 160),
                items = (1..12).map { index ->
                    LiquidGlassMenuItemData("Opção $index", onClick = { chosen = index })
                },
                onDismiss = {}
            ))
        }

        compose.onNodeWithText("Opção 12").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(12, chosen) }
    }

    @Test
    fun outsideTapsDuringCollapseNeverActivateTheBackground() {
        val host = LiquidGlassMenuHostState()
        var backgroundClicks = 0
        var dismissals = 0
        compose.setContent {
            QuadrinheiroTheme(AppSettings()) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().clickable { backgroundClicks++ })
                    LiquidGlassMenuOverlayHost(host, emptyBackdrop())
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            host.showMenu(LiquidGlassMenuRequest(
                id = "scrim",
                anchorBounds = IntRect(80, 100, 200, 160),
                items = listOf(LiquidGlassMenuItemData("Abrir detalhes", onClick = {})),
                onDismiss = { dismissals++ }
            ))
        }
        compose.mainClock.advanceTimeBy(400)
        repeat(2) {
            compose.onRoot().performTouchInput {
                click(Offset(width * 0.7f, height * 0.8f))
            }
        }
        compose.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(0, backgroundClicks)
        }
        compose.mainClock.advanceTimeBy(250)
        compose.onRoot().performTouchInput {
            click(Offset(width * 0.7f, height * 0.8f))
        }
        compose.runOnIdle { assertEquals(1, backgroundClicks) }
    }

    @Test
    fun selectedActionCanOpenAnotherMenuFromTheSameTrigger() {
        val host = LiquidGlassMenuHostState()
        val anchor = IntRect(80, 100, 200, 160)
        var completed = 0
        compose.setContent {
            QuadrinheiroTheme(AppSettings()) {
                Box(Modifier.fillMaxSize()) { LiquidGlassMenuOverlayHost(host, emptyBackdrop()) }
            }
        }
        compose.runOnIdle {
            host.showMenu(LiquidGlassMenuRequest(
                id = "commands",
                anchorBounds = anchor,
                items = listOf(LiquidGlassMenuItemData("Mais opções", onClick = {
                    host.showMenu(LiquidGlassMenuRequest(
                        id = "commands",
                        anchorBounds = anchor,
                        items = listOf(LiquidGlassMenuItemData("Segunda ação", onClick = { completed++ })),
                        onDismiss = {}
                    ))
                })),
                onDismiss = {}
            ))
        }
        compose.onNodeWithText("Mais opções").performClick()
        compose.onNodeWithText("Segunda ação").assertIsDisplayed().performClick()
        compose.onNodeWithTag("retained_glass_menu").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, completed) }
    }

    @Test
    fun actionWaitsForCollapseAndAReplacementDiscardsTheEarlierAction() {
        val host = LiquidGlassMenuHostState()
        var staleClicks = 0
        var newClicks = 0
        fun request(label: String, onClick: () -> Unit) = LiquidGlassMenuRequest(
            id = "replaceable",
            anchorBounds = IntRect(80, 100, 200, 160),
            items = listOf(LiquidGlassMenuItemData(label, onClick = onClick)),
            onDismiss = {}
        )
        compose.setContent {
            QuadrinheiroTheme(AppSettings()) {
                Box(Modifier.fillMaxSize()) { LiquidGlassMenuOverlayHost(host, emptyBackdrop()) }
            }
        }

        compose.mainClock.autoAdvance = false
        compose.runOnIdle { host.showMenu(request("Primeira ação") { staleClicks++ }) }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("Primeira ação").performClick()
        compose.runOnIdle { assertEquals(0, staleClicks) }

        compose.runOnIdle { host.showMenu(request("Segunda ação") { newClicks++ }) }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("Segunda ação").performClick()
        compose.runOnIdle {
            assertEquals(0, staleClicks)
            assertEquals(0, newClicks)
        }

        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithTag("retained_glass_menu").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, staleClicks)
            assertEquals(1, newClicks)
        }
    }

    @Test
    fun circularTriggerStaysAlignedWithTheOpenMenuInBothThemes() {
        val scheme = mutableStateOf("dark")
        val host = LiquidGlassMenuHostState()
        val options = listOf(SegmentOption(0, "Mais recentes"), SegmentOption(1, "Melhor avaliação"))
        val selection = mutableStateOf(0)
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = scheme.value)) {
                val backdrop = rememberLayerBackdrop()
                CompositionLocalProvider(
                    LocalLiquidGlassMenuHost provides host,
                    LocalLiquidGlassBackdrop provides backdrop
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.fillMaxSize().layerBackdrop(backdrop).background(
                                Brush.linearGradient(listOf(Color(0xFFF4C77C), Color(0xFF152D43), Color(0xFF843958)))
                            )
                        )
                        Row(
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassCircleButton(LucideIcons.ShareIos, "Compartilhar", onClick = {})
                            AppleIconMenuButton(
                                options = options,
                                value = selection.value,
                                onChange = { selection.value = it },
                                modifier = Modifier.testTag("sort-trigger")
                            )
                        }
                        LiquidGlassMenuOverlayHost(host, backdrop)
                    }
                }
            }
        }

        listOf("dark", "light").forEach { theme ->
            compose.runOnIdle {
                scheme.value = theme
                selection.value = 0
            }
            val trigger = compose.onNodeWithTag("sort-trigger").assertHeightIsAtLeast(48.dp)
            val triggerBounds = trigger.getUnclippedBoundsInRoot()
            val shareBounds = compose.onNodeWithContentDescription("Compartilhar").getUnclippedBoundsInRoot()
            assertEquals(triggerBounds.top, shareBounds.top)
            snapshot("liquid-glass-toolbar-$theme.png")
            trigger.performClick()
            val panelBounds = compose.onNodeWithTag("retained_glass_menu").getUnclippedBoundsInRoot()
            assertTrue(abs((triggerBounds.right - panelBounds.right).value) <= 1f)
            compose.onNodeWithText("Melhor avaliação").assertIsDisplayed()
            snapshot("liquid-glass-menu-$theme.png")
            compose.onNodeWithText("Melhor avaliação").performClick()
            compose.runOnIdle { assertEquals(1, selection.value) }
        }
    }

    private fun snapshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            compose.onRoot().captureToImage().asAndroidBitmap()
        } else {
            checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        }
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
