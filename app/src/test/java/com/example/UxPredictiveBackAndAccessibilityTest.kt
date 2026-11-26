package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.CatalogCredit
import com.example.data.CatalogDetails
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.QuadrinheiroViewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UxPredictiveBackAndAccessibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun TestSurface(
        dark: Boolean = false,
        reduceMotion: Boolean = false,
        reduceTransparency: Boolean = false,
        fontScale: Float = 1f,
        menuHost: LiquidGlassMenuHostState = remember { LiquidGlassMenuHostState() },
        content: @Composable () -> Unit
    ) {
        val settings = AppSettings(
            colorScheme = if (dark) "dark" else "light",
            reduceMotion = reduceMotion,
            reduceTransparency = reduceTransparency
        )
        val backdrop = rememberLayerBackdrop()
        QuadrinheiroTheme(settings = settings) {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale = fontScale),
                LocalLiquidGlassBackdrop provides backdrop,
                LocalLiquidGlassMenuHost provides menuHost
            ) {
                Box(Modifier.fillMaxSize()) {
                    content()
                    LiquidGlassMenuOverlayHost(state = menuHost, backdrop = backdrop)
                }
            }
        }
    }

    @Test
    fun predictiveBackTransformAppliesSubtleParallaxAndRespectsReducedMotion() {
        val normal = computePredictiveBackTransform(progress = 0.5f, widthPx = 1000f, reducedMotion = false)
        assertTrue("Foreground should shift right on left-edge swipe", normal.foregroundTranslationX > 400f)
        assertTrue("Background should have subtle parallax shift", normal.backgroundTranslationX < -100f && normal.backgroundTranslationX > -120f)
        assertTrue("Foreground scale should gently decrease", normal.foregroundScale < 1f && normal.foregroundScale > 0.95f)
        assertTrue("Background scrim should stay subtle", normal.backgroundScrimAlpha in 0.05f..0.12f)

        val reduced = computePredictiveBackTransform(progress = 0.5f, widthPx = 1000f, reducedMotion = true)
        assertEquals(0f, reduced.foregroundTranslationX, 0.001f)
        assertEquals(0f, reduced.backgroundTranslationX, 0.001f)
        assertEquals(1f, reduced.foregroundScale, 0.001f)
        assertTrue("Reduced motion should fade gently without spatial movement", reduced.foregroundAlpha < 1f)

        val rootNormal = computePredictiveRootExitTransform(progress = 0.5f, widthPx = 1000f, reducedMotion = false)
        val rootReduced = computePredictiveRootExitTransform(progress = 0.5f, widthPx = 1000f, reducedMotion = true)
        assertTrue(rootNormal.foregroundScale < 1f)
        assertEquals(1f, rootReduced.foregroundScale, 0.001f)
        assertEquals(0f, rootReduced.foregroundTranslationX, 0.001f)
    }

    @Test
    fun comicDetailPredictiveBackPreviewCancelAndConfirmRestoresTabAndRespectsOverlays() {
        val rootComic = ComicEntry(
            id = "hq-root-1",
            externalId = "cv-issue-1",
            title = "Batman: Ano Um",
            authors = "Frank Miller",
            publisher = "DC Comics",
            releaseYear = 1987,
            pageCount = 144,
            synopsisPtBr = "A origem clássica do Cavaleiro das Trevas em Gotham."
        )
        val rootDetails = CatalogDetails(
            id = "cv-issue-1",
            type = "issue",
            title = "Batman: Ano Um",
            image = "",
            banner = "",
            description = "A origem clássica do Cavaleiro das Trevas em Gotham.",
            publisher = "DC Comics",
            date = "1987",
            url = "",
            covers = emptyList(),
            characters = listOf(
                CatalogCredit(id = "char-batman", name = "Batman", role = "Protagonista"),
                CatalogCredit(id = "char-gordon", name = "James Gordon", role = "Aliado")
            ),
            creators = listOf(
                CatalogCredit(id = "person-miller", name = "Frank Miller", role = "writer")
            ),
            issues = emptyList(),
            pageCount = 144
        )
        val batmanDetails = CatalogDetails(
            id = "char-batman",
            type = "character",
            title = "Batman",
            image = "",
            banner = "",
            description = "O detetive de Gotham City.",
            publisher = "DC Comics",
            date = "1939",
            url = "",
            covers = emptyList(),
            characters = emptyList(),
            creators = emptyList(),
            issues = emptyList(),
            realName = "Bruce Wayne"
        )

        val menuHost = LiquidGlassMenuHostState()
        var exitedRoot = false

        composeRule.setContent {
            TestSurface(dark = true, reduceMotion = false, menuHost = menuHost) {
                ComicDetailScreen(
                    initialComic = rootComic,
                    profile = ReaderProfile(name = "Leitor Teste"),
                    library = emptyList(),
                    onBack = { exitedRoot = true },
                    onRegister = {},
                    onCover = {},
                    onLogin = {},
                    onShare = {},
                    initialDetailOverride = rootDetails,
                    detailLoader = { _, entity ->
                        if (entity?.first == "character" && entity.second == "char-batman") batmanDetails
                        else rootDetails
                    }
                )
            }
        }

        // 1. Switch to "Personagens" tab (index 1) and open Batman character page
        composeRule.onNodeWithTag("detail_tab_1").performClick()
        composeRule.onNodeWithTag("detail_tab_1").assertIsSelected()
        composeRule.onNodeWithTag("credit_row_character_char-batman").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("comic_detail_title").assertTextEquals("Batman")

        // 2. If a menu overlay is open, first Back must dismiss only the menu (not pop detail history)
        composeRule.runOnIdle {
            menuHost.showMenu(
                LiquidGlassMenuRequest(
                    id = "detail_overlay_menu",
                    anchorBounds = androidx.compose.ui.unit.IntRect(100, 100, 260, 150),
                    items = listOf(LiquidGlassMenuItemData(label = "Opção 1", selected = true, onClick = {})),
                    onDismiss = {}
                )
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("retained_glass_menu").assertExists()

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertNull("Menu should be closed on first Back", menuHost.currentRequest)
        composeRule.onNodeWithTag("comic_detail_title").assertTextEquals("Batman")

        // 3. Start Predictive Back -> preview of previous route appears underneath; then CANCEL -> stays on Batman
        val dispatcher = composeRule.activity.onBackPressedDispatcher
        composeRule.runOnIdle {
            dispatcher.dispatchOnBackStarted(BackEventCompat(10f, 300f, 0.1f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(120f, 300f, 0.45f, BackEventCompat.EDGE_LEFT))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("comic_detail_preview_page", useUnmergedTree = true).assertExists()

        composeRule.runOnIdle {
            dispatcher.dispatchOnBackCancelled()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("comic_detail_title").assertTextEquals("Batman")
        assertFalse(exitedRoot)

        // 4. Start Predictive Back again and CONFIRM -> returns to root comic AND restores "Personagens" tab
        composeRule.runOnIdle {
            dispatcher.dispatchOnBackStarted(BackEventCompat(10f, 300f, 0.15f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(220f, 300f, 0.72f, BackEventCompat.EDGE_LEFT))
            dispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("comic_detail_title").assertTextEquals("Batman: Ano Um")
        composeRule.onNodeWithTag("detail_tab_1").assertIsSelected()
        assertFalse(exitedRoot)
    }

    @Test
    fun appleMenuPickerReopensCleanlyTwiceInARowWithReducedMotionAndTransparency() {
        val options = listOf(
            SegmentOption("RECENT", "Mais Recentes"),
            SegmentOption("RATING", "Melhor Avaliadas"),
            SegmentOption("TITLE", "Ordem Alfabética")
        )
        var selected by mutableStateOf("RECENT")

        composeRule.setContent {
            TestSurface(dark = false, reduceMotion = true, reduceTransparency = true) {
                AppleMenuPicker(
                    options = options,
                    value = selected,
                    onChange = { selected = it },
                    modifier = Modifier.testTag("sort_picker_under_test")
                )
            }
        }

        // First open
        composeRule.onNodeWithTag("sort_picker_under_test").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("retained_glass_menu").assertExists()

        // Choose second option -> closes menu
        composeRule.onNodeWithText("Melhor Avaliadas").performClick()
        composeRule.waitForIdle()
        assertEquals("RATING", selected)
        composeRule.onNodeWithTag("retained_glass_menu").assertDoesNotExist()

        // Second consecutive open of the exact same selector -> must open cleanly!
        composeRule.onNodeWithTag("sort_picker_under_test").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("retained_glass_menu").assertExists()
        composeRule.onNodeWithText("Ordem Alfabética").performClick()
        composeRule.waitForIdle()
        assertEquals("TITLE", selected)
    }

    @Test
    fun savingOrDeletingRegistrationOverDetailPreservesDetailModalAndOriginList() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = QuadrinheiroViewModel(app)
        val initialCount = vm.comics.value.size
        val originComic = ComicEntry(
            id = "origin-list-1",
            externalId = "cv-origin-1",
            title = "Watchmen",
            authors = "Alan Moore",
            publisher = "DC Comics",
            status = ReadingStatus.READ,
            rating = 5f
        )
        vm.saveComic(originComic)
        assertEquals(initialCount + 1, vm.comics.value.size)
        assertTrue(vm.comics.value.any { it.id == originComic.id })

        val detailComic = ComicEntry(
            id = "detail-target-2",
            externalId = "cv-detail-2",
            title = "Sandman: Prelúdios e Noturnos",
            authors = "Neil Gaiman",
            publisher = "Vertigo",
            pageCount = 240
        )

        // Open comic detail modal, then open registration overlay over it
        vm.openInfoSynopsisModal(detailComic)
        assertNotNull(vm.infoModalComic.value)

        vm.openCatalogSheetForComic(detailComic, ReadingStatus.READ, preserveInfoModal = true)
        assertNotNull("Registration sheet should be active", vm.selectedComic.value)
        assertNotNull("Detail sheet must remain open underneath", vm.infoModalComic.value)

        // Save registration -> closes editor, keeps detail sheet and origin list intact
        val savedEntry = vm.selectedComic.value!!.copy(rating = 4.5f, review = "Clássico indispensável")
        vm.saveComic(savedEntry)
        assertNull("Editor sheet should close after save", vm.selectedComic.value)
        assertNotNull("Detail sheet must survive save without being lost", vm.infoModalComic.value)
        assertEquals(detailComic.id, vm.infoModalComic.value!!.id)
        assertTrue("Origin list item must remain intact", vm.comics.value.any { it.id == originComic.id })
        assertTrue("Saved comic must be in library", vm.comics.value.any { it.id == detailComic.id && it.rating == 4.5f })

        // Reopen registration over detail and delete -> closes editor, keeps detail sheet & origin list intact
        vm.selectComicForSheet(savedEntry, preserveInfoModal = true)
        assertNotNull(vm.selectedComic.value)
        assertNotNull(vm.infoModalComic.value)

        vm.deleteComic(savedEntry.id)
        assertNull("Editor sheet should close after delete", vm.selectedComic.value)
        assertNotNull("Detail sheet must survive delete without being lost", vm.infoModalComic.value)
        assertFalse("Deleted comic must be removed", vm.comics.value.any { it.id == detailComic.id })
        assertTrue("Origin list item must still be present", vm.comics.value.any { it.id == originComic.id })
    }

    @Test
    fun toolbarBackButtonCropControlsAndStatesMeet48dpAndSemanticsAcrossThemesAndLargeFont() {
        var zoom by mutableFloatStateOf(1f)
        var nudges by mutableIntStateOf(0)
        var resets by mutableIntStateOf(0)

        composeRule.setContent {
            TestSurface(dark = true, reduceMotion = true, reduceTransparency = true, fontScale = 1.5f) {
                androidx.compose.foundation.layout.Column {
                    SheetToolbar(
                        title = "Editar HQ",
                        closeTag = "test_close_btn",
                        shareTag = "test_share_btn",
                        onClose = {},
                        onShare = {},
                        onSave = {}
                    )
                    SettingsBackButton(onBack = {}, collapsed = true, label = "Perfil")
                    CropPositionControls(
                        zoom = zoom,
                        onZoomChange = { zoom = it },
                        onNudge = { _, _ -> nudges++ },
                        onReset = { zoom = 1f; resets++ }
                    )
                    IOSLoadingIndicator(modifier = Modifier.testTag("test_loader"))
                }
            }
        }

        // 48dp touch targets on SheetToolbar and SettingsBackButton (even when collapsed!)
        listOf("test_close_btn", "test_share_btn", "modal_header_save_button", "settings_back_button").forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag width must be >= 48dp (was ${bounds.right - bounds.left})", (bounds.right - bounds.left) >= 47.5.dp)
            assertTrue("$tag height must be >= 48dp (was ${bounds.bottom - bounds.top})", (bounds.bottom - bounds.top) >= 47.5.dp)
        }
        composeRule.onNodeWithTag("settings_back_button")
            .assertContentDescriptionContains("Voltar para Perfil")

        // CropPositionControls 48dp targets & interactive behavior
        listOf("crop_zoom_in", "crop_zoom_out", "crop_pan_left", "crop_pan_up", "crop_pan_down", "crop_pan_right", "crop_reset_position").forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag width >= 48dp", (bounds.right - bounds.left) >= 47.5.dp)
            assertTrue("$tag height >= 48dp", (bounds.bottom - bounds.top) >= 47.5.dp)
        }
        composeRule.onNodeWithTag("crop_zoom_in").performClick()
        composeRule.onNodeWithTag("crop_pan_up").performClick()
        composeRule.waitForIdle()
        assertEquals(1.25f, zoom, 0.01f)
        assertEquals(1, nudges)
        composeRule.onNodeWithTag("crop_reset_position").performClick()
        composeRule.waitForIdle()
        assertEquals(1f, zoom, 0.01f)
        assertEquals(1, resets)

        // Loading indicator exposes accessible description
        composeRule.onNodeWithTag("test_loader").assertContentDescriptionContains("Carregando")
    }

    @Test
    fun bannerCropModalAndOfflineStatesExposeAccessibleControlsInLightAndLargeFont() {
        val sampleBitmap = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        var retryClicks by mutableIntStateOf(0)
        var showCrop by mutableStateOf(true)

        composeRule.setContent {
            TestSurface(dark = false, reduceMotion = true, reduceTransparency = true, fontScale = 1.5f) {
                if (showCrop) {
                    BannerCropModal(
                        bitmap = sampleBitmap,
                        onDismiss = { showCrop = false },
                        onConfirm = { showCrop = false }
                    )
                } else {
                    OfflineRequiredScreen(onRetry = { retryClicks++ })
                }
            }
        }

        // BannerCropModal exposes accessible heading + crop controls + 48dp actions
        composeRule.onNodeWithTag("banner_crop").assertExists()
        composeRule.onNodeWithTag("crop_position_controls").assertExists()
        val cancelBounds = composeRule.onNodeWithTag("banner_crop_cancel").getUnclippedBoundsInRoot()
        assertTrue((cancelBounds.bottom - cancelBounds.top) >= 47.5.dp)
        composeRule.onNodeWithTag("banner_crop_cancel").performClick()
        composeRule.waitForIdle()

        // OfflineRequiredScreen exposes heading and >= 48dp retry button
        composeRule.onNodeWithTag("offline_required_screen").assertExists()
        composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading) and hasText("Sem conexão com a internet")
        ).assertExists()
        val retryBounds = composeRule.onNodeWithTag("retry_connection_button").getUnclippedBoundsInRoot()
        assertTrue((retryBounds.bottom - retryBounds.top) >= 47.5.dp)
        composeRule.onNodeWithTag("retry_connection_button").performClick()
        composeRule.waitForIdle()
        assertEquals(1, retryClicks)
        sampleBitmap.recycle()
    }
}
