package com.example

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.screens.HomeReadingNowTab
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidToggle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OpeningAndLoadingTest {
    @get:Rule val compose = createComposeRule()
    private val comic = ComicEntry("fixture", "", "Batman", authors = "Autor", publisher = "DC", status = ReadingStatus.READING, pageCount = 160, currentPage = 30)

    private fun screenshot(name: String): Bitmap {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    @Test fun heldToggleMovesVisuallyBeforeReleaseAndCommitsOnce() {
        val selected = mutableStateOf(false)
        var commits = 0
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = "dark")) {
                Box(Modifier.fillMaxSize().background(Color.Black).padding(40.dp)) {
                    LiquidToggle({ selected.value }, { selected.value = it; commits++ }, emptyBackdrop(), Modifier.testTag("toggle"))
                }
            }
        }
        compose.mainClock.autoAdvance = false
        val toggle = compose.onNodeWithTag("toggle")
        toggle.performTouchInput { down(Offset(width * 0.25f, centerY)) }
        compose.mainClock.advanceTimeBy(400)
        val held = screenshot("toggle-held.png")
        toggle.performTouchInput { moveTo(Offset(width * 0.6f, centerY)) }
        compose.mainClock.advanceTimeBy(300)
        val dragged = screenshot("toggle-dragged.png")
        assertFalse(selected.value)
        assertEquals(0, commits)
        assertFalse("The lens must move while the pointer is still held", held.sameAs(dragged))
        toggle.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertTrue(selected.value); assertEquals(1, commits) }
    }

    @Test fun homeSkeletonUsesTheRealHeaderAndGoalLayout() {
        val loading = mutableStateOf(true)
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = "dark", reduceMotion = true)) {
                HomeReadingNowTab(listOf(comic), ReaderProfile(name = "Felipe"), AppSettings(showFloatingMenu = false), rememberLazyListState(),
                    {}, {}, {}, {}, {}, {}, { _, _ -> }, isLoading = loading.value)
            }
        }
        val header = compose.onNodeWithTag("home_header").getUnclippedBoundsInRoot()
        val goal = compose.onNodeWithTag("home_goals").getUnclippedBoundsInRoot()
        screenshot("home-loading.png")
        compose.runOnIdle { loading.value = false }
        compose.onNodeWithTag("home_header").assertExists()
        assertEquals(header, compose.onNodeWithTag("home_header").getUnclippedBoundsInRoot())
        assertEquals(goal, compose.onNodeWithTag("home_goals").getUnclippedBoundsInRoot())
        screenshot("home-loaded.png")
    }

    @Test fun homeFollowsTheSameActiveReadingAndCurrentPageAsTheWidgetResolver() {
        val second = comic.copy(id = "second", externalId = "catalog-second", title = "Superman", currentPage = 65)
        val library = mutableStateOf(listOf(comic, second))
        val activeId = mutableStateOf(second.externalId)
        compose.setContent {
            val settings = AppSettings(showFloatingMenu = false, reduceMotion = true, reduceTransparency = true)
            QuadrinheiroTheme(settings) {
                HomeReadingNowTab(library.value, ReaderProfile(), settings, rememberLazyListState(),
                    {}, {}, {}, {}, {}, {}, { _, _ -> }, activeReadingId = activeId.value)
            }
        }
        fun checkActivePage(expectedPage: Int) {
            val pages = compose.onAllNodesWithText("Pág. $expectedPage de 160", useUnmergedTree = true)
            pages.onFirst().assertIsDisplayed()
            val heading = compose.onNodeWithText("Continuar lendo").getUnclippedBoundsInRoot()
            assertTrue(pages.onFirst().getUnclippedBoundsInRoot().top > heading.top)
            assertEquals(expectedPage, activeReadingComic(library.value, activeId.value)?.currentPage)
        }
        checkActivePage(65)
        compose.runOnIdle { library.value = listOf(comic, second.copy(currentPage = 70)) }
        checkActivePage(70)
        compose.runOnIdle { library.value = listOf(comic, second.copy(status = ReadingStatus.READ, currentPage = 160)) }
        checkActivePage(30)
    }

    @Test fun shaderPoolReturnsIndependentInstances() = kotlinx.coroutines.runBlocking {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.kyant.backdrop.LiquidGlassShaderPrewarmer.prewarmSynchronously()
        }
        assertTrue(com.kyant.backdrop.LiquidGlassShaderPrewarmer.isReady)
        val source = com.kyant.backdrop.InteractiveHighlightSource
        val first = com.kyant.backdrop.LiquidGlassShaderPrewarmer.obtain(source)
        val second = com.kyant.backdrop.LiquidGlassShaderPrewarmer.obtain(source)
        assertNotSame(first, second)
        first.setFloatUniform("radius", 5f)
        second.setFloatUniform("radius", 20f)
    }

    @Test fun lazyEditorCanScrollToSaveWithoutLosingTheDraft() {
        var saved: ComicEntry? = null
        val fixture = comic.copy(synopsisPtBr = "Uma história em português.")
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = "dark", reduceMotion = true)) {
                CatalogSheetModal(fixture, true, false, AppSettings(),
                    onUpdateComic = {}, onToggleExpand = {}, onSave = { saved = it },
                    onDelete = {}, onDismiss = {})
            }
        }
        compose.onNodeWithText("Editar HQ").assertIsDisplayed()
        compose.onNodeWithTag("editor_list", useUnmergedTree = true).performScrollToNode(hasText("Salvar"))
        compose.onNodeWithText("Salvar").performClick()
        compose.runOnIdle { assertEquals(fixture.id, saved?.id); assertEquals(30, saved?.currentPage) }
    }
}
