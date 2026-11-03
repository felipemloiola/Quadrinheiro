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
        android.os.SystemClock.sleep(80)
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
        page.performTouchInput { swipeUp() }
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
        compose.onNodeWithText("Catálogo").assertDoesNotExist()
        compose.runOnIdle {vm.switchMainTab("profile")}
        compose.onNodeWithTag("reader_profile").performScrollToNode(hasText("Catálogo"))
        compose.onNodeWithTag("reader_profile").performTouchInput {swipeUp()}
        compose.onNodeWithText("Catálogo").performClick()
        compose.onNodeWithTag("catalog_browser").assertExists()
        screenshot("catalog-profile")
        compose.onNodeWithTag("settings_back_button").performClick()
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
        val avatar=compose.onNodeWithTag("profile_avatar").getUnclippedBoundsInRoot()
        assertTrue((banner.right-banner.left).value/(banner.bottom-banner.top).value>1.7f)
        assertTrue(avatar.top<banner.bottom && avatar.bottom>banner.bottom)
        screenshot("banner-profile")
    }
    @Test fun exploreSelectorDoesNotJumpWhenOpeningCommunity() {
        compose.runOnIdle {vm.switchMainTab("explore")}
        val before=compose.onNodeWithText("Descobrir").getUnclippedBoundsInRoot()
        compose.onNodeWithText("Comunidade").performClick()
        val after=compose.onNodeWithText("Descobrir").getUnclippedBoundsInRoot()
        assertEquals(before.top.value,after.top.value,.5f)
        compose.onNodeWithText("Publicar").assertDoesNotExist()
        compose.onNodeWithText("Atualizar").assertDoesNotExist()
    }
    @Test fun previewOpensFullComicPageAndCommunityRequiresSignIn() {
        compose.runOnIdle { vm.openInfoSynopsisModal(ComicEntry("cv-1142807", "cv-1142807", "Batman", authors = "", publisher = "DC Comics", sourceLabel = "Comic Vine")) }
        compose.onNodeWithTag("open_comic_page").performClick()
        compose.onNodeWithTag("comic_detail_page").assertExists()
        compose.onNodeWithText("Comunidade").performScrollTo().performClick()
        compose.onNodeWithText("Histórias para conversar").assertExists()
        compose.onNodeWithText("Entrar").assertExists()
        compose.onNodeWithTag("settings_back_button").performClick()
        compose.onNodeWithTag("synopsis_close_button").assertExists()
    }
    @Test fun fullPageRegistrationOpensEditorForNewAndWantedComics() {
        val newComic = ComicEntry("cv-1142807", "cv-1142807", "Batman", authors = "Autor", publisher = "DC", sourceLabel = "Comic Vine")
        val wanted = newComic.copy(id = "owned-wanted", title = "HQ na fila", status = ReadingStatus.WANT_TO_READ)
        listOf(newComic, wanted).forEachIndexed { index, comic ->
            compose.runOnIdle {
                if (index == 1) vm.saveComic(comic)
                vm.openInfoSynopsisModal(comic)
            }
            compose.onNodeWithTag("open_comic_page").performClick()
            compose.onNodeWithText("Registrar leitura").performClick()
            compose.onNodeWithTag("modal_cancel_button").assertExists()
            compose.runOnIdle {
                assertNotNull(vm.selectedComic.value)
                assertEquals(if (index == 1) ReadingStatus.WANT_TO_READ else ReadingStatus.READ, vm.selectedComic.value!!.status)
                vm.selectComicForSheet(null); vm.openInfoSynopsisModal(null)
            }
        }
    }
    @Test fun confirmSavesEveryStatusWhenTheSourceDoesNotKnowThePageCount() {
        listOf(ReadingStatus.READ,ReadingStatus.READING,ReadingStatus.WANT_TO_READ).forEachIndexed {index,status->
            val comic=ComicEntry("unknown-$index","","Sem paginação $index",authors="Autora",publisher="Editora",pageCount=0,status=status,sourceLabel="Metron")
            compose.runOnIdle {vm.selectComicForSheet(comic)}
            compose.onNodeWithTag("modal_header_save_button").performClick()
            compose.runOnIdle {
                val saved=vm.comics.value.first {it.id==comic.id}
                assertEquals(status,saved.status);assertEquals(0,saved.pageCount);assertNull(vm.selectedComic.value)
            }
        }
    }
    @Test fun registrationLoadsTheActualPageCountForTheSelectedEdition() {
        compose.runOnIdle {vm.selectComicForSheet(ComicEntry("mt-184296","mt-184296","Batman: Ano Um",authors="",publisher="",pageCount=0,status=ReadingStatus.READ,sourceLabel="Metron"))}
        compose.waitUntil(25000){compose.onAllNodes(hasText("144") and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()}
        screenshot("registration52")
        compose.onNodeWithTag("modal_header_save_button").performClick()
        compose.runOnIdle {assertEquals(144,vm.comics.value.first{it.id=="mt-184296"}.pageCount)}
    }

    @Test fun iconMenuFitsItsLabelsAndProfileEditorUsesSharedGoalControls(){
        compose.runOnIdle {vm.switchMainTab("settings");vm.openSettingsCategory("appearance")}
        compose.onNodeWithTag("launcher_icon_style").performScrollTo().performClick()
        val menu=compose.onNodeWithTag("retained_glass_menu").getUnclippedBoundsInRoot()
        assertTrue((menu.right-menu.left).value<180f)
        screenshot("icon-menu52")
        compose.onNode(hasText("Original") and hasAnyAncestor(hasTestTag("retained_glass_menu"))).performClick()
        compose.runOnIdle {vm.switchMainTab("profile")}
        compose.onNodeWithText("Editar perfil").performClick()
        compose.onNodeWithTag("profile_editor").performScrollToNode(hasTestTag("profile_goal_toggle"))
        compose.onNodeWithTag("profile_editor").performScrollToIndex(8)
        val toggle=compose.onNodeWithTag("profile_goal_toggle")
        val expectedGoal=toggle.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState]!=androidx.compose.ui.state.ToggleableState.On
        toggle.performClick()
        if(expectedGoal)toggle.assertIsOn() else toggle.assertIsOff()
        screenshot("profile-editor52")
        compose.onNodeWithTag("profile_editor").performScrollToNode(hasTestTag("save_profile"))
        compose.onNodeWithTag("save_profile").performClick()
        compose.onNodeWithTag("profile_editor").assertDoesNotExist()
        compose.runOnIdle {assertEquals(expectedGoal,vm.profile.value.showGoalOnHome)}
    }

}


