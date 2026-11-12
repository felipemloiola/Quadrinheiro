package com.example

import androidx.activity.BackEventCompat
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.AppSettings
import com.example.ui.components.AppPage
import com.example.ui.components.AppPageTransition
import com.example.ui.components.FluidSheetState
import com.example.ui.components.rememberFluidSheet
import com.example.ui.theme.QuadrinheiroTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PredictiveBackTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var dispatcher: OnBackPressedDispatcher

    private fun gesture(progress: Float, edge: Int = BackEventCompat.EDGE_LEFT) {
        compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, edge)) }
        compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 400f, progress, edge)) }
    }

    @Test fun cancellingPreviewsTheParentWithoutNavigatingOrRecreatingTheCurrentPage() {
        val page = mutableStateOf(AppPage("settings", "appearance", null, false))
        val alive = mutableMapOf<AppPage, Any>()
        var commits = 0
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            QuadrinheiroTheme(settings = AppSettings()) {
                AppPageTransition(page.value, backEnabled = true, onBack = { commits++; page.value = page.value.backDestination!! }) { route ->
                    val identity = remember { Any() }
                    DisposableEffect(route) { alive[route] = identity; onDispose { alive.remove(route) } }
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        var identity: Any? = null
        compose.runOnIdle { identity = alive[page.value] }
        gesture(.65f)
        compose.runOnIdle {
            assertEquals(0, commits)
            assertEquals(2, alive.size)
            assertTrue(alive.containsKey(page.value.backDestination))
            dispatcher.dispatchOnBackCancelled()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0, commits)
            assertEquals(1, alive.size)
            assertSame(identity, alive[page.value])
        }
    }

    @Test fun completingFromTheRightEdgeCommitsOnceAndKeepsThePreviewedPage() {
        val page = mutableStateOf(AppPage("shelf", null, null, false))
        var commits = 0
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            QuadrinheiroTheme(settings = AppSettings()) {
                AppPageTransition(page.value, backEnabled = true, onBack = { commits++; page.value = page.value.backDestination!! }) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        gesture(.7f, BackEventCompat.EDGE_RIGHT)
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, commits); assertEquals("profile", page.value.tab) }
    }

    @Test fun buttonBackWorksWithReducedMotion() {
        val page = mutableStateOf(AppPage("settings", "icons", "appearance", false))
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            QuadrinheiroTheme(settings = AppSettings(reduceMotion = true)) {
                AppPageTransition(page.value, backEnabled = true, onBack = { page.value = page.value.backDestination!! }) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(AppPage("settings", "appearance", null, false), page.value) }
    }

    @Test fun cancellingASheetRestoresItsExpandedPositionAndDoesNotDismiss() {
        lateinit var sheet: FluidSheetState
        var dismissals = 0
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            QuadrinheiroTheme(settings = AppSettings()) {
                sheet = rememberFluidSheet(onDismiss = { dismissals++ }, onExpand = {})
            }
        }
        compose.runOnIdle { sheet.expandToTop() }
        compose.waitForIdle()
        gesture(.6f)
        compose.runOnIdle { assertTrue(sheet.offset > 0f); dispatcher.dispatchOnBackCancelled() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0f, sheet.offset, 1f); assertEquals(0, dismissals) }
    }
}
