package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
import org.junit.Assert.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurationDesignReviewTest {
    @get:Rule val compose=createComposeRule()
    private val image=java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"curation-design-cover.png")
    @After fun removeFixture() {image.delete()}

    @Test fun coverCardsHaveUniformBoundsInDarkTheme()=checkCards("dark",100)
    @Test fun coverCardsHaveUniformBoundsWithLargeTextInLightTheme()=checkCards("light",160)

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

    private fun checkCards(scheme:String,textScale:Int) {
        val bitmap=android.graphics.Bitmap.createBitmap(80,120,android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(45,68,82))
        image.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
        val entries=listOf("Batman Absoluto (2024) 2","Maus","Uma história de título bastante longo para ocupar mais de duas linhas","Saga")
            .mapIndexed {index,title -> ListComic.from(ComicEntry("design-$index","",title,authors="",publisher="",coverUrl=image.toURI().toString()))}
        val list=ComicList(name="Escolha de capa",coverKey=entries.first().key,items=entries)
        var applied=false
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(colorScheme=scheme,reduceMotion=true,reduceTransparency=true)) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,fontScale=textScale/100f)) {
            ListArtworkEditor(list,{applied=true},{})
            }
        }}
        val boxes=entries.map {entry ->
            val card=compose.onNodeWithTag("list_cover_choice_${entry.key}")
            card.performScrollTo().assertIsDisplayed()
            card.fetchSemanticsNode().boundsInRoot
        }
        boxes.forEach {box ->
            assertEquals(boxes.first().width,box.width,1.1f)
            assertEquals("Títulos curtos e longos usam o mesmo cartão",boxes.first().height,box.height,1.1f)
        }
        compose.onNodeWithTag("list_cover_choice_${entries.first().key}").performScrollTo().assertIsSelected()
        compose.onNodeWithText(entries.first().title,useUnmergedTree=true)
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {getLayout ->
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                assertTrue(getLayout(layouts))
                assertEquals("A escala deve chegar ao texto dentro do Dialog",textScale/100f,layouts.single().layoutInput.density.fontScale,.01f)
            }
        if(textScale==100)compose.onNodeWithText(entries.first().title,useUnmergedTree=true)
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {getLayout ->
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                assertTrue(getLayout(layouts))
                assertFalse("O marcador não deve tirar espaço do título/número da edição",layouts.single().hasVisualOverflow)
            }
        compose.onNodeWithTag("list_cover_choice_${entries[1].key}").assertIsNotSelected()
        assertFalse("Visualizar opções não aplica a capa",applied)
        val screenshot=checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io.File(image.parentFile,"design-cover-cards-$scheme-$textScale.png").outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
        screenshot.recycle()
    }
}
