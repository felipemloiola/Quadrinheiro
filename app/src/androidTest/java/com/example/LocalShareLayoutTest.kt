package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.QuadrinheiroTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocalShareLayoutTest {
    @get:Rule val compose=createComposeRule()
    /** Local network smoke check; invoke explicitly on an online emulator. */
    @Test fun publicRasterCoverLoadsThroughTheActualImageLoader() {
        val urls=listOf("https://media.kitsu.app/manga/poster_images/40489/medium.jpg",
            "https://static.metron.cloud/media/issue/2026/09/15/2712_400x600.jpg")
        compose.setContent {
            QuadrinheiroTheme(settings=AppSettings(reduceMotion=true)) {
                androidx.compose.foundation.layout.Column {
                    urls.forEachIndexed { index,url -> BookCover(url,"Capa de teste $index",modifier=Modifier.size(112.dp,168.dp)) }
                }
            }
        }
        compose.waitForIdle()
        compose.waitUntil(25000) { urls.indices.all { compose.onAllNodesWithText("Capa de teste $it").fetchSemanticsNodes().isEmpty() } }
        urls.indices.forEach { compose.onNodeWithContentDescription("Capa de teste $it").assertIsDisplayed() }
    }
    @Test fun shareSheetOpensAtItsFullHeightWithVisibleActionsAndCanDrag() {
        val comic=ComicEntry("fixture","fixture","HQ de teste",authors="Autor",publisher="DC",pageCount=144)
        compose.setContent {
            QuadrinheiroTheme(settings=AppSettings(reduceMotion=true)) {
                SocialShareCardSheetModal(SocialShareRequest(comic=comic),listOf(comic),ReaderProfile(),
                    AppSettings(reduceMotion=true),{}, {})
            }
        }
        compose.onNodeWithTag("social_share_save_gallery_button").assertIsDisplayed()
        compose.onNodeWithTag("social_share_dispatch_button").assertIsDisplayed()
        val sheet=compose.onNodeWithTag("social_share_sheet").fetchSemanticsNode().boundsInRoot
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(sheet.height > root.height * .8f)
        compose.onNodeWithTag("social_share_grabber",useUnmergedTree=true).performTouchInput { swipeUp() }
        compose.onNodeWithTag("social_share_dispatch_button").assertIsDisplayed()
    }
}
