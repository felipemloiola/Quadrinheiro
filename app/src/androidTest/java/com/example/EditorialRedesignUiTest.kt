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

    @Test fun detailUsesExistingRegistrationAndKeepsReadingStateUntouched() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val image=java.io.File(context.getExternalFilesDir(null),"redesign-covers/batman.jpg")
        val base=ComicEntry("redesign-fixture","","Batman: Ano Um",authors="Frank Miller · David Mazzucchelli",publisher="Panini Comics",
            pageCount=144,currentPage=32,status=ReadingStatus.READING,coverUrl=if(image.exists()) image.toURI().toString() else "",
            synopsisPtBr="Uma história sobre os primeiros passos do Batman em Gotham.",releaseYear=2019)
        var shown by mutableStateOf(base)
        var registered:ComicEntry?=null
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(colorScheme="dark",reduceMotion=true,reduceTransparency=true)) {
            ComicDetailDialog(shown,ReaderProfile(),listOf(shown),{}, {registered=it},{},{},{})
        }}
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
