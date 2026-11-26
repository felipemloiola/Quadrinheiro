package com.example

import android.graphics.Bitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
        File(compose.activity.getExternalFilesDir(null), "profile-revision-$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun profileOpensLibraryAndSettings() {
        compose.runOnIdle { vm.switchMainTab("profile") }
        val page = compose.onNodeWithTag("reader_profile")
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
    @Test fun profileKeepsOnlyLibraryAndSearchDoesNotDuplicateDiscovery() {
        compose.runOnIdle { vm.switchMainTab("explore") }
        compose.onNodeWithText("Catálogo").assertDoesNotExist()
        compose.runOnIdle { vm.switchMainTab("profile") }
        compose.onNodeWithTag("reader_profile").performScrollToNode(hasText("Biblioteca"))
        compose.onNodeWithText("Biblioteca").assertExists()
        compose.onNodeWithText("Catálogo").assertDoesNotExist()
        screenshot("library-profile")
        compose.runOnIdle { vm.setSearchQuery(""); vm.switchMainTab("search") }
        compose.onNodeWithText("Qual será sua próxima leitura?").assertExists()
        compose.onNodeWithText("Destaques do catálogo").assertDoesNotExist()
        compose.onNodeWithText("Para você").assertDoesNotExist()
        screenshot("search")
    }
    @Test fun editingProfileReturnsToProfileAndHeaderSharesTheBanner() {
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
        val avatar = compose.onNodeWithTag("profile_avatar").getUnclippedBoundsInRoot()
        val title = compose.onNode(hasText("Perfil") and hasAnyAncestor(hasTestTag("reader_profile"))).getUnclippedBoundsInRoot()
        val settings = compose.onNodeWithTag("profile_settings").getUnclippedBoundsInRoot()
        assertEquals(144f, (banner.bottom - banner.top).value, 1f)
        assertTrue(avatar.top < banner.bottom && avatar.bottom > banner.bottom)
        assertTrue(title.top >= banner.top && title.bottom <= banner.bottom)
        assertTrue(settings.top >= banner.top && settings.bottom <= banner.bottom)
        screenshot("banner-profile")
    }
    @Test fun exploreSelectorDoesNotJumpWhenOpeningCommunity() {
        compose.runOnIdle { vm.switchMainTab("explore") }
        val before = compose.onNodeWithText("Descobrir").getUnclippedBoundsInRoot()
        compose.onNodeWithText("Comunidade").performClick()
        val after = compose.onNodeWithText("Descobrir").getUnclippedBoundsInRoot()
        assertEquals(before.top.value, after.top.value, .5f)
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
    @Test fun fullPageRegistrationKeepsSearchDetailForNewAndWantedComics() {
        val newComic = registrationFixture("new-registration", "Expedição às estrelas")
        val wanted = registrationFixture("wanted-registration", "HQ na fila", ReadingStatus.WANT_TO_READ)
        listOf(newComic, wanted).forEachIndexed { index, comic ->
            compose.runOnIdle {
                vm.switchMainTab("search")
                if (index == 1) vm.saveComic(comic)
                vm.openInfoSynopsisModal(comic)
            }
            openFullComicPage()
            compose.onNodeWithTag("comic_detail_register_button").assertTextEquals("Registrar leitura")
            openReadingRegistration()
            val expectedStatusTab = statusTab(if (index == 1) "Quero Ler" else "Lido")
            registrationNode(hasTestTag("editor_list")).performScrollToNode(expectedStatusTab)
            registrationNode(expectedStatusTab).assertIsSelected()
            if (index == 0) {
                registrationNode(hasTestTag("modal_cancel_button")).performClick()
            } else {
                pressBack()
            }
            compose.onNodeWithTag("comic_detail_registration").assertDoesNotExist()
            compose.onNodeWithTag("comic_detail_title").assertTextEquals(comic.displayTitle)
            compose.runOnIdle {
                assertNull(vm.selectedComic.value)
                assertEquals(comic.id, vm.infoModalComic.value?.id)
                assertEquals("search", vm.activeTab.value)
            }
            openReadingRegistration()
            if (index == 0) screenshot("detail-reading-registration")
            registrationNode(hasTestTag("modal_header_save_button")).performClick()
            compose.onNodeWithTag("comic_detail_registration").assertDoesNotExist()
            compose.onNodeWithTag("comic_detail_title").assertTextEquals(comic.displayTitle)
            compose.runOnIdle {
                assertEquals(comic.id, vm.infoModalComic.value?.id)
                assertEquals(comic.status, vm.comics.value.single { it.id == comic.id }.status)
            }
            pressBack()
            compose.onNodeWithTag("comic_detail_page").assertDoesNotExist()
            compose.onNodeWithTag("synopsis_close_button").assertIsDisplayed().performClick()
        }
    }

    @Test fun registrationOverLibraryEditorPreservesDetailScrollAndUpdatesOriginalDraft() {
        val comic = registrationFixture("saved-registration", "Travessia do Atlântico")
        compose.runOnIdle {
            vm.saveComic(comic)
            vm.switchMainTab("shelf")
            vm.selectComicForSheet(comic)
        }
        openFullComicPage()
        val detailList = compose.onNodeWithTag("comic_detail_list")
        detailList.performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(0f, 32f) }
        val offset = detailList.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue(offset > 0f)

        openReadingRegistration()
        pressBack()
        compose.onNodeWithTag("comic_detail_registration").assertDoesNotExist()
        val returnedOffset = detailList.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(offset, returnedOffset, 0.00001f)

        openReadingRegistration()
        registrationNode(hasTestTag("editor_list")).performScrollToNode(statusTab("Lendo"))
        registrationNode(statusTab("Lendo")).performClick()
        registrationNode(hasTestTag("modal_header_save_button")).performClick()
        compose.onNodeWithTag("comic_detail_registration").assertDoesNotExist()
        compose.onNodeWithTag("comic_detail_register_button").assertTextContains("Em leitura", substring = true)
        compose.runOnIdle {
            assertEquals("shelf", vm.activeTab.value)
            assertEquals(comic.id, vm.selectedComic.value?.id)
            assertEquals(ReadingStatus.READING, vm.comics.value.single { it.id == comic.id }.status)
        }
        pressBack()
        compose.onNodeWithTag("comic_detail_page").assertDoesNotExist()
        compose.onNodeWithTag("editor_list").performScrollToNode(statusTab("Lendo"))
        compose.onNode(statusTab("Lendo") and hasAnyAncestor(hasTestTag("editor_list"))).assertIsSelected()
    }

    private fun registrationFixture(id: String, title: String, status: ReadingStatus = ReadingStatus.READ) = ComicEntry(
        id = id,
        externalId = id,
        title = title,
        titlePtBr = title,
        authors = "Autora",
        publisher = "Editora",
        coverUrl = "https://example.invalid/fixture.png",
        pageCount = 120,
        status = status,
        synopsisPtBr = "Uma viagem ilustrada por lugares desconhecidos."
    )

    private fun openFullComicPage() {
        compose.onNodeWithTag("editor_list").performScrollToNode(hasTestTag("open_comic_page"))
        compose.onNodeWithTag("open_comic_page").assertIsDisplayed().performClick()
        compose.onNodeWithTag("comic_detail_page").assertIsDisplayed()
    }

    private fun openReadingRegistration() {
        compose.onNodeWithTag("comic_detail_register_button").assertIsDisplayed().performClick()
        compose.onNodeWithTag("comic_detail_registration").assertIsDisplayed()
        compose.onNodeWithTag("comic_detail_page").assertExists()
    }

    private fun registrationNode(matcher: SemanticsMatcher) =
        compose.onNode(matcher and hasAnyAncestor(hasTestTag("comic_detail_registration")))

    private fun statusTab(label: String) =
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun pressBack() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }
    @Test fun confirmSavesEveryStatusWhenTheSourceDoesNotKnowThePageCount() {
        listOf(ReadingStatus.READ, ReadingStatus.READING, ReadingStatus.WANT_TO_READ).forEachIndexed { index, status ->
            val comic = ComicEntry("unknown-$index", "", "Sem paginação $index", authors = "Autora", publisher = "Editora", pageCount = 0, status = status, sourceLabel = "Metron")
            compose.runOnIdle { vm.selectComicForSheet(comic) }
            compose.onNodeWithTag("modal_header_save_button").performClick()
            compose.runOnIdle {
                val saved = vm.comics.value.first { it.id == comic.id }
                assertEquals(status, saved.status); assertEquals(0, saved.pageCount); assertNull(vm.selectedComic.value)
            }
        }
    }
    @Test fun registrationDisplaysAndSavesTheCatalogPageCountForTheSelectedEdition() {
        val edition = registrationFixture("numbered-edition", "Edição com paginação do catálogo").copy(pageCount = 144)
        compose.runOnIdle { vm.selectComicForSheet(edition) }
        compose.onNodeWithText("144 págs", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("edition_page_count_input").assertDoesNotExist()
        screenshot("registration")
        compose.onNodeWithTag("modal_header_save_button").performClick()
        compose.runOnIdle { assertEquals(144, vm.comics.value.single { it.id == edition.id }.pageCount) }
    }
    @Test fun iconMenuFitsAndProfileDoesNotOfferGoalVisibility() {
        compose.runOnIdle { vm.switchMainTab("settings"); vm.openSettingsCategory("appearance") }
        compose.onNodeWithTag("launcher_icon_style").performScrollTo().performClick()
        val menu = compose.onNodeWithTag("retained_glass_menu").getUnclippedBoundsInRoot()
        assertTrue((menu.right - menu.left).value < 180f)
        screenshot("icon-menu")
        compose.onNode(hasText("Original") and hasAnyAncestor(hasTestTag("retained_glass_menu"))).performClick()
        compose.runOnIdle { vm.updateProfile { it.copy(showGoalOnHome = false) }; vm.switchMainTab("profile") }
        compose.onNodeWithText("Editar perfil").performClick()
        compose.onNodeWithTag("profile_editor").performScrollToNode(hasTestTag("favorite_universe_picker"))
        compose.onNodeWithTag("favorite_universe_picker").assertIsDisplayed()
        compose.onNodeWithTag("profile_editor").performScrollToNode(hasText("Meta diária"))
        compose.onNodeWithTag("profile_goal_toggle").assertDoesNotExist()
        compose.onNodeWithText("Meta no Início").assertDoesNotExist()
        screenshot("profile-editor")
        compose.onNodeWithTag("save_profile").assertIsDisplayed().performClick()
        compose.onNodeWithTag("profile_editor").assertDoesNotExist()
        compose.runOnIdle { assertFalse(vm.profile.value.showGoalOnHome) }
    }
}
