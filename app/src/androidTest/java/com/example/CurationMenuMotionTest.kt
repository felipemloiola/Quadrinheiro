package com.example

import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
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

    @Test fun visibilityUsesTheProfileGlassPickerInsideTheDialogWindow() {
        var picked=""
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(colorScheme="light",reduceMotion=true,reduceTransparency=true)) {
            CurationSheet("Editar lista",{},menus=true) {
                AppleMenuPicker(options=listOf(SegmentOption(false,"Privada",Icons.Default.Lock),SegmentOption(true,"Pública",Icons.Default.Public)),value=false,onChange={picked=if(it)"public" else "private"})
            }
        }}
        compose.onNodeWithText("Privada").performClick()
        compose.onNodeWithTag("retained_glass_menu").assertIsDisplayed()
        compose.onAllNodesWithText("Privada").onLast().assertIsSelected()
        compose.onNodeWithText("Pública",useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {getLayout ->
            val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            assertTrue(getLayout(layouts))
            assertEquals("O texto não deve quebrar entre os ícones",1,layouts.single().lineCount)
        }
        compose.onNodeWithText("Pública").assertIsNotSelected().performClick()
        compose.runOnIdle {assertEquals("public",picked)}
    }

    @Test fun closingACropPageReturnsToTheExistingCoverPicker() {
        val comic=com.example.data.ListComic.from(com.example.model.ComicEntry(id="navigation-test",externalId="",authors="",publisher="",title="HQ de navegação"))
        val list=com.example.data.ComicList(name="Navegação",items=listOf(comic))
        var dismissed=0
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true)) {
            ListArtworkEditor(list,{}, {dismissed++})
        }}
        compose.onNodeWithText("HQ de navegação").performClick()
        compose.onNodeWithText("Ajustar capa").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Fechar").onLast().performClick()
        compose.onNodeWithText("Escolher capa").assertIsDisplayed()
        compose.onNodeWithText("HQ de navegação").assertIsDisplayed()
        compose.runOnIdle {assertEquals(0,dismissed)}
        compose.onAllNodesWithContentDescription("Fechar").onLast().performClick()
        compose.runOnIdle {assertEquals(1,dismissed)}
    }

    @Test fun actionMenuExposesButtonsInsteadOfRadioOptions() {
        var invoked=false
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true)) {
            CurationSheet("Lista",{},menus=true) {
                CurationActionMenu("Opções da lista",listOf(LiquidGlassMenuItemData("Editar nota"){invoked=true}))
            }
        }}
        compose.onNodeWithContentDescription("Opções da lista").performClick()
        val item=compose.onNodeWithText("Editar nota")
        item.assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,androidx.compose.ui.semantics.Role.Button))
        item.assert(SemanticsMatcher.keyNotDefined(androidx.compose.ui.semantics.SemanticsProperties.Selected))
        item.assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f)).performClick()
        compose.runOnIdle {assertTrue(invoked)}
    }
}
