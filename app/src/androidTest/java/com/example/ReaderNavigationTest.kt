package com.example

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.SafeStorage
import com.example.model.*
import com.example.updates.InAppUpdateDownload
import com.example.viewmodel.QuadrinheiroViewModel
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import java.io.File

class ReaderNavigationTest {
    @get:Rule(order = 0) val preset = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            InAppUpdateDownload.load(context)?.let { InAppUpdateDownload.cancel(context, it) }
            SafeStorage(context).apply {
                removeItem("profile_showcase_guest_local")
                setCompletedWelcomeIntro(true); setGuestMode(true)
                saveState(listOf(ComicEntry("profile-test", "", "Minha HQ favorita", authors = "Autora", publisher = "Editora", status = ReadingStatus.READ)),
                    ReaderProfile(name = "Leitor", handle = "@leitor"), AppSettings(reduceMotion = true), ownerUid = "guest_local")
            }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: QuadrinheiroViewModel
    @Before fun loadViewModel() { vm = ViewModelProvider(compose.activity)[QuadrinheiroViewModel::class.java] }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bmp = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(compose.activity.getExternalFilesDir(null), "revision48-$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun profileOpensLibraryAndSettingsAndKeepsItsFourFavorites() {
        compose.runOnIdle { vm.switchMainTab("profile") }
        val page = compose.onNodeWithTag("reader_profile")
        page.performScrollToNode(hasTestTag("favorite-comic-0"))
        compose.onNodeWithContentDescription("Escolher HQ favorita 1").performClick()
        compose.onNodeWithText("Minha HQ favorita").performClick()
        compose.runOnIdle { assertEquals(listOf("profile-test"), vm.appSettings.value.profileFavoriteComicIds) }
        page.performScrollToNode(hasTestTag("favorite-character-0"))
        compose.onNodeWithTag("favorite-character-0").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Batman")
        compose.waitUntil(15000) { compose.onAllNodesWithText("Batman (", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Batman (", substring = true)[0].performClick()
        compose.runOnIdle { assertTrue(vm.appSettings.value.profileFavoriteCharacters.single().startsWith("Batman (")) }
        screenshot("profile-favorites")
        page.performScrollToNode(hasText("Biblioteca"))
        page.performScrollToIndex(4)
        compose.onNodeWithText("Biblioteca").performClick()
        compose.runOnIdle { assertEquals("shelf", vm.activeTab.value) }
        compose.onNodeWithTag("settings_back_button").performClick()
        compose.onNodeWithTag("reader_profile").performScrollToNode(hasTestTag("profile_settings"))
        compose.onNodeWithTag("profile_settings").performClick()
        compose.runOnIdle { assertEquals("settings", vm.activeTab.value) }
        compose.onNodeWithTag("settings_back_button").performClick()
        compose.onNodeWithTag("reader_profile").assertExists()
    }
    @Test fun exploreHasExpandedCollectionsAndSearchDoesNotDuplicateDiscovery() {
        compose.runOnIdle { vm.switchMainTab("explore") }
        val page = compose.onNodeWithTag("explore_page")
        page.performScrollToNode(hasText("Catálogo"))
        page.performScrollToIndex(5)
        screenshot("explore")
        compose.onNodeWithText("Catálogo").performClick()
        compose.onNodeWithTag("explore_collection").assertExists()
        compose.onNodeWithText("Todo o catálogo").assertExists()
        screenshot("collection")
        compose.runOnIdle { vm.setSearchQuery(""); vm.switchMainTab("search") }
        compose.onNodeWithText("Qual será sua próxima leitura?").assertExists()
        compose.onNodeWithText("Destaques do catálogo").assertDoesNotExist()
        compose.onNodeWithText("Para você").assertDoesNotExist()
        screenshot("search")
    }
    @Test fun editingProfileReturnsToProfileAndBannerStaysCompact() {
        compose.runOnIdle {
            vm.updateSettings { it.copy(profileBannerUrl = "https://comicvine.gamespot.com/a/uploads/scale_medium/11/110017/9911623-wwww.jpg") }
            vm.switchMainTab("profile")
        }
        compose.onNodeWithText("Editar perfil").performClick()
        compose.onNodeWithTag("profile_editor").assertExists()
        compose.onNodeWithTag("settings_back_button").performClick()
        compose.onNodeWithTag("reader_profile").assertExists()
        compose.runOnIdle { assertEquals("profile", vm.activeTab.value) }
        val banner = compose.onNodeWithTag("profile_banner").getUnclippedBoundsInRoot()
        assertTrue((banner.right - banner.left).value / (banner.bottom - banner.top).value > 2.9f)
        screenshot("banner-profile")
    }
    @Test fun previewOpensFullComicPageAndCommunityRequiresSignIn() {
        compose.runOnIdle { vm.openInfoSynopsisModal(ComicEntry("cv-1142807", "cv-1142807", "Batman", authors = "", publisher = "DC Comics", sourceLabel = "Comic Vine")) }
        compose.onNodeWithText("Ver página da HQ").performClick()
        compose.onNodeWithTag("comic_detail_page").assertExists()
        compose.onNodeWithText("Comunidade").performScrollTo().performClick()
        compose.onNodeWithText("Histórias para conversar").assertExists()
        compose.onNodeWithText("Entrar").assertExists()
        compose.onNodeWithTag("settings_back_button").performClick()
        compose.onNodeWithTag("synopsis_close_button").assertExists()
    }
}
