package com.example

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.ComicList
import com.example.data.ListComic
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.ui.components.ListArtworkEditor
import com.example.ui.components.ListCoverCropDialog
import com.example.ui.theme.QuadrinheiroTheme
import java.io.File
import org.junit.Assert.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurationDesignReviewTest {
    @get:Rule val compose=createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val fixtureImages = listOf("light", "dark", "neutral").map {
        File(instrumentation.targetContext.cacheDir, "curation-design-cover-$it.png")
    }
    @After fun removeFixtures() { fixtureImages.forEach { it.delete() } }

    @Test fun narrowDarkWindowKeepsAGridAtNormalScale() = checkCards("dark", 360, 1f, useGrid = true)
    @Test fun narrowLightWindowKeepsAGridAtNormalScale() = checkCards("light", 360, 1f, useGrid = true)
    @Test fun narrowDarkWindowShowsBothEditionsAtLargeScale() = checkCards("dark", 360, 1.6f, useGrid = false)
    @Test fun narrowLightWindowShowsBothEditionsAtLargeScale() = checkCards("light", 360, 1.6f, useGrid = false)
    @Test fun wideDarkWindowKeepsAGridAtNormalScale() = checkCards("dark", 720, 1f, useGrid = true)
    @Test fun wideLightWindowKeepsAGridAtNormalScale() = checkCards("light", 720, 1f, useGrid = true)
    @Test fun wideDarkWindowKeepsAGridAtLargeScale() = checkCards("dark", 720, 1.6f, useGrid = true)
    @Test fun wideLightWindowKeepsAGridAtLargeScale() = checkCards("light", 720, 1.6f, useGrid = true)

    @Test fun selectionIsReachableWithReducedTransparency() = checkCards(
        "light", 360, 1.6f, useGrid = false, settings = AppSettings(colorScheme = "light", reduceTransparency = true)
    )

    @Test fun selectionIsReachableWithIncreasedContrast() = checkCards(
        "light", 360, 1.6f, useGrid = false, settings = AppSettings(colorScheme = "light", increaseContrast = true)
    )

    @Test fun cropCanMoveUpAndLeftWithoutPointerGestures() {
        val entry=ListComic.from(ComicEntry("crop-design","","Capa acessível",authors="",publisher="",coverUrl="file:///fixture.png"))
        val initial=ComicList(name="Recorte",items=listOf(entry),coverKey=entry.key,coverZoom=2f)
        var applied:ComicList?=null
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true)) {
            ListCoverCropDialog(initial,{applied=it},{})
        }}
        val actions=compose.onNodeWithContentDescription("Enquadramento da capa.",substring=true)
            .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
        compose.runOnIdle {
            assertEquals(4,actions.size)
            assertTrue(actions.first {it.label=="Mover para cima"}.action())
            assertTrue(actions.first {it.label=="Mover para a esquerda"}.action())
        }
        compose.onNodeWithText("Usar capa").performClick()
        compose.waitUntil(5000){applied!=null}
        // Image translation is (.5-focus)*(scaledSize-cropSize): increasing focus moves it up/left.
        assertTrue(applied!!.coverX>initial.coverX)
        assertTrue(applied!!.coverY>initial.coverY)
    }

    private fun checkCards(
        scheme: String,
        widthDp: Int,
        fontScale: Float,
        useGrid: Boolean,
        settings: AppSettings = AppSettings(colorScheme = scheme, reduceMotion = true)
    ) {
        val coverColors = listOf(Color.rgb(226, 214, 190), Color.rgb(28, 36, 52), Color.rgb(97, 52, 64))
        fixtureImages.zip(coverColors).forEach { (file, color) ->
            val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(color)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        val entries = listOf("Batman Absoluto (2024) 1", "Batman Absoluto (2024) 2", "Maus")
            .mapIndexed { index, title ->
                ListComic.from(ComicEntry("design-$index", "", title, authors = "", publisher = "",
                    coverUrl = fixtureImages[index].toURI().toString()))
            }
        val list = ComicList(name = "Escolha de capa", coverKey = entries[1].key, items = entries)
        var applied = false
        compose.setContent {
            QuadrinheiroTheme(settings) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val testDensity = Density(constraints.maxWidth / widthDp.toFloat(), fontScale)
                    CompositionLocalProvider(LocalDensity provides testDensity) {
                        ListArtworkEditor(list, { applied = true }, {})
                    }
                }
            }
        }

        val first = compose.onNodeWithTag("list_cover_choice_${entries[0].key}")
        val second = compose.onNodeWithTag("list_cover_choice_${entries[1].key}")
        first.assertIsDisplayed().assertIsNotSelected()
        second.assertIsDisplayed().assertIsSelected()
        if (settings.reduceMotion) {
            val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(instrumentation.targetContext.cacheDir, "design-cover-$scheme-$widthDp-$fontScale.png")
                .outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
        }

        entries.take(2).forEach { entry ->
            compose.onNodeWithText(entry.title, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    assertTrue(getLayout(layouts))
                    val layout = layouts.single()
                    assertEquals("Escala efetiva no diálogo", fontScale, layout.layoutInput.density.fontScale, .01f)
                    assertFalse("A edição precisa estar visível por inteiro: ${entry.title}", layout.hasVisualOverflow)
                }
        }
        first.assertHeightIsAtLeast(48.dp).assertHasClickAction()
        second.assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Capa atual"))
        val firstBounds = first.fetchSemanticsNode().boundsInRoot
        val secondBounds = second.fetchSemanticsNode().boundsInRoot
        assertEquals(firstBounds.width, secondBounds.width, 1.1f)
        if (useGrid) {
            assertEquals(firstBounds.top, secondBounds.top, 1.1f)
            assertTrue(secondBounds.left > firstBounds.right)
            val shortCard = compose.onNodeWithTag("list_cover_choice_${entries[2].key}").performScrollTo()
            assertEquals(firstBounds.height, secondBounds.height, 1.1f)
            assertEquals(firstBounds.height, shortCard.fetchSemanticsNode().boundsInRoot.height, 1.1f)
        } else {
            assertEquals(firstBounds.left, secondBounds.left, 1.1f)
            assertTrue(secondBounds.top > firstBounds.bottom)
        }
        compose.runOnIdle { assertFalse("Visualizar opções não aplica a capa", applied) }
    }
}
