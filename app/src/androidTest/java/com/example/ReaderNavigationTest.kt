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
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
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
        compose.onNodeWithText("Batman").performClick()
        compose.runOnIdle { assertEquals(listOf("Batman"), vm.appSettings.value.profileFavoriteCharacters) }
        screenshot("profile-favorites")
        page.performScrollToNode(hasText("Biblioteca"))
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
}
