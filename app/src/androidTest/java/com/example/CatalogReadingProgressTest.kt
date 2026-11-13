package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.*
import com.example.ui.components.CatalogSheetModal
import com.example.ui.theme.QuadrinheiroTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CatalogReadingProgressTest {
    @get:Rule val compose = createComposeRule()
    private val settings = AppSettings(reduceMotion = true, reduceTransparency = true)
    private fun show(pages: Int, onSave: (ComicEntry) -> Unit = {}) {
        val comic = ComicEntry("page-fixture", "page-fixture", "HQ de teste", authors = "Autor", publisher = "Editora",
            pageCount = pages, currentPage = 0, status = ReadingStatus.READING, synopsisPtBr = "Sinopse de teste.")
        compose.setContent {
            QuadrinheiroTheme(settings) {
                CatalogSheetModal(comic, true, true, settings, onUpdateComic = {}, onToggleExpand = {},
                    onSave = onSave, onDelete = {}, onDismiss = {})
            }
        }
    }

    @Test fun knownApiTotalShowsTheSliderAndAcceptsReadingProgress() {
        var saved: ComicEntry? = null
        show(160) { saved = it }
        compose.onNodeWithTag("editor_list", useUnmergedTree = true).performScrollToIndex(3)
        compose.onNodeWithTag("reading_page_slider", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(64f)) }
        compose.onNodeWithText("Página atual (64 de 160)").assertIsDisplayed()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "catalog-reading-slider.png")
            .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithContentDescription("Salvar").performClick()
        compose.runOnIdle { assertEquals(64, saved?.currentPage); assertEquals(160, saved?.pageCount) }
    }

    @Test fun singlePageEditionAlsoShowsTheSlider() {
        show(1)
        compose.onNodeWithTag("editor_list", useUnmergedTree = true).performScrollToIndex(3)
        compose.onNodeWithTag("reading_page_slider", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Página atual (0 de 1)").assertIsDisplayed()
    }

    @Test fun unavailableApiTotalHasAnExplanationAndRetryWithoutManualPageInput() {
        show(0)
        compose.onNodeWithTag("editor_list", useUnmergedTree = true).performScrollToNode(hasTestTag("reading_pages_unavailable"))
        compose.onNodeWithText("A API não informou o total de páginas desta edição.").assertIsDisplayed()
        compose.onNodeWithTag("retry_reading_pages").assertIsDisplayed().performClick()
        compose.onNodeWithText("A API não informou o total de páginas desta edição.").assertIsDisplayed()
        compose.onNodeWithTag("reading_page_slider", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Total de páginas").assertDoesNotExist()
    }
}
