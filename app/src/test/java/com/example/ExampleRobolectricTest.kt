package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CuratedCatalog
import com.example.data.SafeStorage
import com.example.model.ReadingStatus
import com.example.viewmodel.QuadrinheiroViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and SafeStorage CRC32 zip generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Quadrinheiro", appName)

        val storage = SafeStorage(context)
        val state = storage.loadState()
        assertTrue(state.comics.isNotEmpty())

        val zipBytes = storage.buildStandaloneIOS27ZipBytes(
            comics = state.comics,
            profile = state.profile,
            settings = state.settings
        )
        // Verify PK\x03\x04 ZIP magic header
        assertTrue(zipBytes.size > 256)
        assertEquals(0x50.toByte(), zipBytes[0])
        assertEquals(0x4B.toByte(), zipBytes[1])
    }

    @Test
    fun `verify cataloging starts at zero stars and capsule index memory on search`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = QuadrinheiroViewModel(app)

        // Select Diary tab (capsuleIndex = 1), then Search Lupa
        vm.switchMainTab("diary")
        assertEquals(1, vm.lastCapsuleIndex.value)
        vm.switchMainTab("search")
        assertEquals("search", vm.activeTab.value)
        assertEquals(1, vm.lastCapsuleIndex.value) // Remembers Shelf/Diary slot!

        // Opening catalog modal for a new comic must initialize rating at 0.0f
        val candidate = CuratedCatalog.curatedComicsCatalog.last()
        vm.openCatalogSheetForComic(candidate, ReadingStatus.READ)
        val selected = vm.selectedComic.value
        assertNotNull(selected)
        assertEquals(0.0f, selected!!.rating, 0.001f)
    }

    @Test
    fun `verify all AppSettings options are persisted and take effect across ViewModel and storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SafeStorage(context)

        // 1. Test custom settings modification
        val modifiedSettings = com.example.model.AppSettings(
            themeId = "editorial-noir",
            colorScheme = "dark",
            glassLevel = 85,
            glassVariant = "clear",
            edgeRefraction = false,
            glassIntensity = 1.75f,
            glassBlurIntensity = 2.2f,
            enableLensRefraction = false,
            enableChromaticAberration = false,
            glassOpacity = 1.4f,
            backgroundExtension = false,
            showBottomAccessory = false,
            showFloatingMenu = false,
            minimizeTabBarOnScroll = false,
            reduceTransparency = true,
            increaseContrast = true,
            boldText = true,
            dynamicTypeScale = 110,
            reduceMotion = true,
            onOffLabels = true,
            shelfColumns = 4,
            showProgressOnCovers = false,
            showRatingsOnGrid = false,
            spine3dEffect = false,
            defaultShelfSort = "RATING",
            defaultFormat = "Manga Tankobon",
            searchLanguage = "PT",
            searchProvider = "MANGA",
            hdCovers = false,
            spoilerProtection = true,
            halfStarRatings = false,
            allowRereads = false,
            autoFillReadDate = false,
            showPagesInDiary = false,
            quickReviewTags = listOf("Clássico", "Obra-Prima", "Recomendado")
        )

        val currentState = storage.loadState()
        storage.saveState(currentState.comics, currentState.profile, modifiedSettings)

        // Reload from storage to verify 100% round-trip JSON serialization
        val reloadedState = storage.loadState()
        val s = reloadedState.settings

        assertEquals("editorial-noir", s.themeId)
        assertEquals("dark", s.colorScheme)
        assertEquals(85, s.glassLevel)
        assertEquals("clear", s.glassVariant)
        assertEquals(false, s.edgeRefraction)
        assertEquals(1.75f, s.glassIntensity, 0.01f)
        assertEquals(2.2f, s.glassBlurIntensity, 0.01f)
        assertEquals(false, s.enableLensRefraction)
        assertEquals(false, s.enableChromaticAberration)
        assertEquals(1.4f, s.glassOpacity, 0.01f)
        assertEquals(false, s.backgroundExtension)
        assertEquals(false, s.showBottomAccessory)
        assertEquals(false, s.showFloatingMenu)
        assertEquals(false, s.minimizeTabBarOnScroll)
        assertEquals(true, s.reduceTransparency)
        assertEquals(true, s.increaseContrast)
        assertEquals(true, s.boldText)
        assertEquals(110, s.dynamicTypeScale)
        assertEquals(true, s.reduceMotion)
        assertEquals(true, s.onOffLabels)
        assertEquals(4, s.shelfColumns)
        assertEquals(false, s.showProgressOnCovers)
        assertEquals(false, s.showRatingsOnGrid)
        assertEquals(false, s.spine3dEffect)
        assertEquals("RATING", s.defaultShelfSort)
        assertEquals("Manga Tankobon", s.defaultFormat)
        assertEquals("PT", s.searchLanguage)
        assertEquals("MANGA", s.searchProvider)
        assertEquals(false, s.hdCovers)
        assertEquals(true, s.spoilerProtection)
        assertEquals(false, s.halfStarRatings)
        assertEquals(false, s.allowRereads)
        assertEquals(false, s.autoFillReadDate)
        assertEquals(false, s.showPagesInDiary)
        assertEquals(listOf("Clássico", "Obra-Prima", "Recomendado"), s.quickReviewTags)

        // 2. Test ViewModel live updateSettings reactivity
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = QuadrinheiroViewModel(app)

        vm.updateSettings {
            it.copy(
                shelfColumns = 3,
                showRatingsOnGrid = true,
                spoilerProtection = false,
                autoFillReadDate = true,
                onOffLabels = true
            )
        }

        val vmSettings = vm.appSettings.value
        assertEquals(3, vmSettings.shelfColumns)
        assertEquals(true, vmSettings.showRatingsOnGrid)
        assertEquals(false, vmSettings.spoilerProtection)
        assertEquals(true, vmSettings.autoFillReadDate)
        assertEquals(true, vmSettings.onOffLabels)
    }

    @Test
    fun `verify comic moments creation and SafeStorage persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SafeStorage(context)
        val state = storage.loadState()
        val comic = state.comics.first()

        val moment1 = com.example.model.ComicMoment(
            id = "test-m-1",
            page = 42,
            tag = "⚡ Momento Épico",
            note = "Cena épica de confronto na chuva",
            date = "2026-10-22",
            isSpoiler = false
        )
        val moment2 = com.example.model.ComicMoment(
            id = "test-m-2",
            page = 120,
            tag = "🎭 Reviravolta",
            note = "O vilão era o mentor",
            date = "2026-10-22",
            isSpoiler = true
        )
        val updatedComic = comic.copy(moments = listOf(moment1, moment2))
        val updatedComics = state.comics.map { if (it.id == comic.id) updatedComic else it }
        storage.saveState(updatedComics, state.profile, state.settings)

        val reloaded = storage.loadState().comics.first { it.id == comic.id }
        assertEquals(2, reloaded.moments.size)
        assertEquals("test-m-1", reloaded.moments[0].id)
        assertEquals(42, reloaded.moments[0].page)
        assertEquals("⚡ Momento Épico", reloaded.moments[0].tag)
        assertEquals("Cena épica de confronto na chuva", reloaded.moments[0].note)
        assertEquals(false, reloaded.moments[0].isSpoiler)

        assertEquals("test-m-2", reloaded.moments[1].id)
        assertEquals(120, reloaded.moments[1].page)
        assertEquals("🎭 Reviravolta", reloaded.moments[1].tag)
        assertEquals("O vilão era o mentor", reloaded.moments[1].note)
        assertEquals(true, reloaded.moments[1].isSpoiler)
    }
}
