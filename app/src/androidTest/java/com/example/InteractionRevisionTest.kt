package com.example

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class InteractionRevisionTest {
    @get:Rule val compose = createComposeRule()
    private val settings = AppSettings(colorScheme = "dark", reduceMotion = true)
    private val comic = ComicEntry("layout45", "", "Uma história", authors = "Autor", publisher = "Editora", synopsisPtBr = "Uma história em quadrinhos.")
    private fun snapshot(name: String) {
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun shareActionsFollowPreviewWithoutAnEmptyFixedFooter() {
        var dismissed = false
        compose.setContent { QuadrinheiroTheme(settings) {
            SocialShareCardSheetModal(SocialShareRequest(comic = comic), listOf(comic), ReaderProfile(), settings, {}, { dismissed = true })
        } }
        val preview = compose.onNodeWithTag("social_share_card_preview_canvas", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val save = compose.onNodeWithTag("social_share_save_gallery_button").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(save.top - preview.bottom <= 20.dp)
        snapshot("revision45-sharing.png")
        compose.onNodeWithTag("social_share_close_button").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test fun editorFooterActionsRemainAdjacent() {
        compose.setContent { QuadrinheiroTheme(settings) {
            CatalogSheetModal(comic, true, false, settings, onUpdateComic = {}, onToggleExpand = {}, onSave = {}, onDelete = {}, onDismiss = {})
        } }
        compose.onNodeWithTag("editor_list", useUnmergedTree = true).performScrollToIndex(5)
        val save = compose.onNodeWithText("Salvar Registro").getUnclippedBoundsInRoot()
        val delete = compose.onNodeWithText("Excluir Quadrinho da Estante").getUnclippedBoundsInRoot()
        assertTrue(delete.top - save.bottom <= 12.dp)
        snapshot("revision45-editor.png")
    }

    @Test fun textClearSymbolIsNearTheTrailingEdgeAndStillHasALargeTarget() {
        compose.setContent { QuadrinheiroTheme(settings) {
            Box(Modifier.width(300.dp).testTag("field-host")) { AppleTextField("Felipe", {}, "Nome de exibição") }
        } }
        val root = compose.onNodeWithTag("field-host").getUnclippedBoundsInRoot()
        val symbol = compose.onNodeWithContentDescription("Limpar", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(root.right - symbol.right <= 13.dp)
        snapshot("revision45-text-field.png")
    }

    @Test fun menuSeparatorsAreRestored() {
        val host = LiquidGlassMenuHostState()
        compose.setContent { QuadrinheiroTheme(settings) {
            Box(Modifier.fillMaxSize().background(Color(0xFF344050))) { LiquidGlassMenuOverlayHost(host, emptyBackdrop()) }
        } }
        compose.runOnIdle { host.showMenu(LiquidGlassMenuRequest("original-menu", IntRect(30, 100, 260, 180), items = listOf(
            LiquidGlassMenuItemData("Mais Recentes", selected = true, onClick = {}),
            LiquidGlassMenuItemData("Melhor Avaliação", onClick = {}),
            LiquidGlassMenuItemData("Título", onClick = {})
        ), onDismiss = {})) }
        compose.onNodeWithTag("menu-divider-0").assertIsDisplayed()
        compose.onNodeWithTag("menu-divider-1").assertIsDisplayed()
        snapshot("revision45-menu.png")
    }

    @Test fun rapidInterruptedNavigationKeepsAtMostTwoLivePages() {
        val page = mutableStateOf(AppPage("settings", null, null, false))
        var live = 0
        var peak = 0
        compose.setContent { QuadrinheiroTheme(AppSettings()) {
            AppPageTransition(page.value) { destination ->
                DisposableEffect(destination) { live++; peak = maxOf(peak, live); onDispose { live-- } }
                Column { Text(destination.settings ?: destination.tab); AppleToggle(false) }
            }
        } }
        compose.mainClock.autoAdvance = false
        repeat(60) { index ->
            compose.runOnIdle { page.value = AppPage(if (index % 4 == 0) "shelf" else "settings", "page-$index", null, false) }
            compose.mainClock.advanceTimeBy(48)
        }
        compose.mainClock.advanceTimeBy(1600)
        compose.runOnIdle { assertTrue("Old page trees must be disposed on interruption: $peak", peak <= 2); assertEquals(1, live) }
        compose.onNodeWithText("page-59").assertIsDisplayed()
    }
}
