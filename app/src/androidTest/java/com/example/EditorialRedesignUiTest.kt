package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.*
import com.example.ui.screens.ComicDetailDialog
import com.example.ui.theme.QuadrinheiroTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorialRedesignUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun expandedCoverZoomsFromItsSourceAndReversesOnClose() {
        compose.mainClock.autoAdvance=false
        var open by mutableStateOf(true)
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=false,reduceTransparency=true)) {
            if(open) com.example.ui.components.ExpandedComicCover("","Capa em transição",androidx.compose.ui.geometry.Rect(40f,120f,140f,270f)){open=false}
        }}
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(60)
        val midway=compose.onNodeWithContentDescription("Capa em transição").fetchSemanticsNode().boundsInRoot.width
        compose.mainClock.advanceTimeBy(260)
        val expanded=compose.onNodeWithContentDescription("Capa em transição").fetchSemanticsNode().boundsInRoot.width
        assertTrue("O zoom aumenta a capa a partir da origem",expanded>midway)
        compose.onNodeWithContentDescription("Fechar capa").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle {assertFalse(open)}
        compose.mainClock.autoAdvance=true
    }

    @Test fun detailUsesExistingRegistrationAndKeepsReadingStateUntouched() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val image=java.io.File(context.cacheDir,"detail-cover-fixture.png")
        val bitmap=android.graphics.Bitmap.createBitmap(120,180,android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(38,83,104))
        image.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
        val base=ComicEntry("redesign-fixture","","Batman: Ano Um",authors="Frank Miller · David Mazzucchelli",publisher="Panini Comics",
            pageCount=144,currentPage=32,status=ReadingStatus.READING,coverUrl=if(image.exists()) image.toURI().toString() else "",
            synopsisPtBr="Uma história sobre os primeiros passos do Batman em Gotham.",releaseYear=2019)
        var shown by mutableStateOf(base)
        var registered:ComicEntry?=null
        var resolved=false
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(colorScheme="dark",reduceMotion=true,reduceTransparency=true)) {
            ComicDetailDialog(shown,ReaderProfile(),listOf(shown),{}, {registered=it},{},{},{},resolveDetails={resolved=true;null})
        }}
        compose.waitUntil(5000){resolved}
        val page=compose.onNodeWithTag("comic_detail_page").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription(base.displayTitle).performClick()
        val expanded=compose.onAllNodesWithContentDescription(base.displayTitle).onLast().fetchSemanticsNode().boundsInRoot
        assertTrue("A capa ampliada preserva contexto nas laterais",expanded.width<page.width*.9f)
        compose.onNodeWithContentDescription("Fechar capa").assertIsDisplayed().performClick()
        compose.onNodeWithText("Atualizar leitura").assertIsDisplayed().performClick()
        assertEquals(base,registered)
        capture("redesign-hq-lendo.png")
        compose.runOnIdle {shown=base.copy(status=ReadingStatus.WANT_TO_READ);registered=null}
        compose.onNodeWithText("Iniciar leitura").assertIsDisplayed().performClick()
        assertEquals(ReadingStatus.WANT_TO_READ,registered!!.status)
        assertEquals(32,registered!!.currentPage)
        compose.runOnIdle {shown=base.copy(status=ReadingStatus.READ,rating=4f,readDate="2026-11-12")}
        compose.onNodeWithText("Editar registro").assertIsDisplayed()
        compose.onNodeWithText("Lido").assertIsDisplayed()
        capture("redesign-hq-lido.png")
    }

    private fun capture(name:String) {
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val bitmap=checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        java.io.File(instrumentation.targetContext.cacheDir,name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
}
