package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.AppSettings
import com.example.ui.components.*
import com.example.ui.theme.QuadrinheiroTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurationMenuMotionTest {
    @get:Rule val compose=createComposeRule()

    @Test fun panelStaysAliveDuringExitAndClosesOnlyOnce() {
        compose.mainClock.autoAdvance=false
        var open by mutableStateOf(true)
        var callbacks=0
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=false,reduceTransparency=true)) {
            if(open) CurationSheet("Painel de teste",{callbacks++;open=false}) {androidx.compose.material3.Text("Conteúdo preservado")}
        }}
        compose.mainClock.advanceTimeBy(2000)
        compose.onNodeWithContentDescription("Fechar").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {assertTrue("A janela aguarda a animação de saída",open);assertEquals(0,callbacks)}
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle {assertFalse(open);assertEquals(1,callbacks)}
        compose.mainClock.autoAdvance=true
    }

    @Test fun visibilityUsesSelectionAndDefersTheChoiceUntilDismissal() {
        var open by mutableStateOf(true)
        var picked=""
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(colorScheme="light",reduceMotion=true,reduceTransparency=true)) {
            if(open) ListVisibilityMenu(false,{open=false},{picked="private"},{picked="public"})
        }}
        compose.onNodeWithText("Privada").assertIsSelected()
        compose.onNodeWithText("Pública").assertIsNotSelected().performClick()
        compose.runOnIdle {assertFalse(open);assertEquals("public",picked)}
    }
}
