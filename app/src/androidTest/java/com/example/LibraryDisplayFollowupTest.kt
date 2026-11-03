package com.example

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.*
import com.example.reading.ReadingNowBarPreference
import com.example.ui.components.*
import com.example.ui.screens.WelcomeIntroScreen
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LibraryDisplayFollowupTest {
    @get:Rule val compose = createComposeRule()
    private val settings = AppSettings(colorScheme = "dark", reduceMotion = true)
    private val comic = ComicEntry("layout-fixture", "", "Uma história", authors = "Autor", publisher = "Editora", status = ReadingStatus.READ)

    private fun snapshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun compactActionsKeepComfortableTouchTargetsAndFitInOneNormalTextRow() {
        var chosen = ""
        compose.setContent {
            QuadrinheiroTheme(settings) {
                Row(Modifier.width(220.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("+ Lido", "Lendo", "Quero Ler").forEach { label ->
                        CompactReadingAction(label, prominent = label == "+ Lido", onClick = { chosen = label })
                    }
                }
            }
        }
        val faces = listOf("+ Lido", "Lendo", "Quero Ler").map {
            compose.onNodeWithTag("catalog-action-face-$it", useUnmergedTree = true).getUnclippedBoundsInRoot()
        }
        assertTrue(faces.all { it.bottom - it.top <= 34.dp })
        assertEquals(faces.first().top, faces.last().top)
        val target = compose.onNode(hasClickAction() and hasText("Quero Ler"))
        val touchBounds = target.getUnclippedBoundsInRoot()
        assertTrue(touchBounds.bottom - touchBounds.top >= 48.dp)
        target.performClick()
        compose.runOnIdle { assertEquals("Quero Ler", chosen) }
    }

    @Test fun welcomeMeasuresOnlyVisibleStageAndContinueRemainsReachable() {
        var continued = false
        compose.setContent { QuadrinheiroTheme(settings) { WelcomeIntroScreen { continued = true } } }
        compose.onNodeWithText("Sua Biblioteca de Quadrinhos").assertDoesNotExist()
        val brand = compose.onNodeWithTag("welcome_brand").getUnclippedBoundsInRoot()
        val button = compose.onNodeWithTag("welcome_start_button").getUnclippedBoundsInRoot()
        assertTrue(button.top > brand.bottom)
        assertTrue("The first screen must not reserve the hidden feature page", button.top - brand.bottom < 100.dp)
        snapshot("followup-welcome.png")
        compose.onNodeWithTag("welcome_start_button").performClick()
        compose.onNodeWithText("Sua Biblioteca de Quadrinhos").assertExists()
        compose.onNodeWithTag("welcome_primary_button").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(continued) }
    }

    @Test fun sharingOwnsItsBackdropAndAllCardTypesRemainSelectable() {
        compose.setContent {
            QuadrinheiroTheme(settings) {
                val outside = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.Magenta).layerBackdrop(outside)) { Text("FUNDO ANTERIOR") }
                    CompositionLocalProvider(LocalLiquidGlassBackdrop provides outside) {
                        SocialShareCardSheetModal(SocialShareRequest(comic = comic), listOf(comic), ReaderProfile(), settings, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Momento").assertIsDisplayed().performClick()
        compose.onNodeWithText("Metas").assertIsDisplayed().performClick()
        compose.onNodeWithText("Concluída").performClick()
        snapshot("followup-sharing.png")
    }

    @Test fun nowBarPreferenceTogglesAndRevealsSystemPermissions() {
        val enabled = mutableStateOf(false)
        compose.setContent { QuadrinheiroTheme(settings) { ReadingNowBarPreference(enabled.value) { enabled.value = it } } }
        compose.onNodeWithText("Leitura na Now Bar").assertIsDisplayed()
        compose.onNode(isToggleable()).performClick()
        compose.waitUntil { enabled.value }
        compose.onNodeWithText("Permissões da tela de bloqueio e Now Bar ›").assertIsDisplayed()
        compose.onNode(isToggleable()).performClick()
        compose.runOnIdle { assertFalse(enabled.value) }
    }
    @Test fun editorSharesThroughItsHeaderWithoutTheLargeRedundantCallout() {
        var shares = 0
        compose.setContent {
            QuadrinheiroTheme(settings) {
                CatalogSheetModal(comic, true, false, settings, onUpdateComic = {}, onToggleExpand = {},
                    onSave = {}, onDelete = {}, onDismiss = {}, onShareComic = { _, _, _ -> shares++ })
            }
        }
        compose.onNodeWithTag("editor_list", useUnmergedTree = true).performScrollToIndex(3)
        compose.onNodeWithText("Compartilhar Card de Leitura Concluída").assertDoesNotExist()
        compose.onNodeWithTag("modal_share_card_button").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, shares) }
    }
}
