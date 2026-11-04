package com.example

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Revision11ChangesTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun item2_noSyntheticSynopsisOrFakePaniniPublisherAndSeparateTranslationFields() {
        // When synopsis cannot be translated, translateSynopsisToPtBr must return empty string (no fabricated text)
        val untranslated = PortugueseComicTranslator.translateSynopsisToPtBr(
            "An obscure English-only paragraph with no dictionary match whatsoever.",
            "Mystery Comic",
            "John Doe",
            "Publisher",
            2020,
            120
        )
        assertEquals("", untranslated)

        // Foreign edition keeps original publisher and original title/synopsis separate from PT-BR fields
        val foreign = ComicEntry(
            id = "cv-9991",
            externalId = "cv-9991",
            title = "Spider-Man: Blue",
            titlePtBr = "",
            authors = "Jeph Loeb, Tim Sale",
            publisher = "Marvel Comics",
            synopsis = "Peter Parker remembers Gwen Stacy.",
            synopsisPtBr = ""
        )
        val localized = PortugueseComicTranslator.localizeComic(foreign)
        assertEquals("Spider-Man: Blue", localized.title)
        assertEquals("Marvel Comics", localized.publisher)
        assertFalse(CatalogSearchPolicy.isPortugueseEdition(localized))

        // Saving translation updates active account state and keeps original title/synopsis intact
        val storage = SafeStorage(context)
        val uid = "rev11-translation-user"
        try {
            storage.saveAuthSession(
                UserProfile(
                    uid = uid,
                    displayName = "User",
                    email = "user@example.com",
                    photoUrl = null,
                    isGuest = false,
                    provider = "email"
                )
            )
            storage.saveState(listOf(foreign), ReaderProfile(), AppSettings(), uid)
            storage.saveComicTranslation(
                foreign.copy(
                    titlePtBr = "Homem-Aranha: Azul",
                    synopsisPtBr = "Peter Parker relembra Gwen Stacy."
                )
            )
            val reloaded = storage.loadAccountState(uid)!!.comics.single()
            assertEquals("Spider-Man: Blue", reloaded.title)
            assertEquals("Homem-Aranha: Azul", reloaded.titlePtBr)
            assertEquals("Peter Parker remembers Gwen Stacy.", reloaded.synopsis)
            assertEquals("Peter Parker relembra Gwen Stacy.", reloaded.synopsisPtBr)
        } finally {
            storage.clearAuthSession()
            storage.removeItem(SafeStorage.ACCOUNT_STATE_PREFIX + uid)
        }
    }

    @Test
    fun item3_favoritePortraitsResolvesCanonicalAndComplementaryVerifiedPortraits() = runBlocking {
        // Canonical IDs and civil-name disambiguation never confuse different characters
        val candidates = listOf(
            CharacterResult(
                id = "cv-1699",
                name = "Batman (Bruce Wayne)",
                originalName = "Batman",
                aliases = listOf("Bruce Wayne"),
                image = "https://comicvine.gamespot.com/a/uploads/scale_small/1/1699/bruce.jpg"
            ),
            CharacterResult(
                id = "cv-2020",
                name = "Batman (Terry McGinnis)",
                originalName = "Batman",
                aliases = listOf("Terry McGinnis"),
                image = "https://comicvine.gamespot.com/a/uploads/scale_small/1/2020/terry.jpg"
            )
        )
        val verifiedBruce = FavoritePortraits.matchVerifiedCharacterPortraits("Batman (Bruce Wayne)", "Batman", candidates)
        assertEquals(listOf("https://comicvine.gamespot.com/a/uploads/scale_small/1/1699/bruce.jpg"), verifiedBruce)

        val detail = CatalogDetails(
            id = "11202",
            type = "character",
            title = "Green Lantern (Hal Jordan)",
            image = "https://comicvine.gamespot.com/a/uploads/original/1/11202/hal1.jpg",
            banner = "",
            description = "",
            publisher = "DC Comics",
            date = "2004",
            url = "",
            covers = listOf("https://comicvine.gamespot.com/a/uploads/scale_medium/1/11202/hal2.jpg"),
            characters = emptyList(),
            creators = emptyList(),
            issues = listOf(
                ComicEntry(
                    id = "cv-10",
                    externalId = "cv-10",
                    title = "Green Lantern: Rebirth #1",
                    authors = "Geoff Johns",
                    publisher = "DC Comics",
                    coverUrl = "https://comicvine.gamespot.com/a/uploads/scale_small/1/10/hal_issue.jpg"
                )
            )
        )
        val combined = (FavoritePortraits.resourceImages(detail, "11202") + FavoritePortraits.issuePortraits(detail, "11202")).distinct()
        assertEquals(3, combined.size)
    }

    @Test
    fun item4_favoriteComicsSearchSupportsHyphensSpacesAccentsBilingualAliasesAndTypos() {
        val pool = listOf(
            ComicEntry("sp-1", "cv-1", "Spider-Man: Blue", titlePtBr = "Homem-Aranha: Azul", authors = "Jeph Loeb", publisher = "Marvel"),
            ComicEntry("sp-2", "cv-2", "O Espetacular Homem-Aranha", authors = "Stan Lee", publisher = "Panini"),
            ComicEntry("sw-1", "cv-3", "Saga of the Swamp Thing", titlePtBr = "A Saga do Monstro do Pântano", authors = "Alan Moore", publisher = "Vertigo"),
            ComicEntry("unrelated", "cv-4", "Garfield Vol. 1", authors = "Jim Davis", publisher = "PAWS")
        )
        for (query in listOf("Homem Aranha", "Homem-Aranha", "Spider Man", "Spider-Man", "Homen Aranha")) {
            val ranked = CatalogSearchPolicy.filterAndRankForQuery(pool, query)
            assertTrue("Expected Spider-Man results for '$query'", ranked.any { it.id == "sp-1" || it.id == "sp-2" })
            assertFalse("Unrelated comic should not match '$query'", ranked.any { it.id == "unrelated" })
        }
        val swamp = CatalogSearchPolicy.filterAndRankForQuery(pool, "monstro do pantano")
        assertEquals("sw-1", swamp.first().id)
    }

    @Test
    fun item5_openLibrarySelectsMultipleEditionsAndDeduplicatesKeepDistinctEditions() {
        val root = JSONObject().put(
            "docs",
            JSONArray(
                """[
                    {"key":"/works/OL1W","title":"Batman: Year One","author_name":["Frank Miller"],"publisher":["DC Comics"],"first_publish_year":1987,"number_of_pages_median":144,"cover_i":101,"subject":["Comics","Graphic Novels"],"editions":{"docs":[{"key":"/books/OL101M"}]}},
                    {"key":"/works/OL2W","title":"Batman: The Dark Knight Returns","author_name":["Frank Miller"],"publisher":["DC Comics"],"first_publish_year":1986,"number_of_pages_median":224,"cover_i":102,"subject":["Comics","Graphic Novels"],"editions":{"docs":[{"key":"/books/OL102M"}]}}
                ]"""
            )
        )
        val selected = AdditionalCatalogSources.selectOpenLibraryEditions(root, 3)
        assertEquals(2, selected.size)
        assertEquals("OL101M", selected[0].second)
        assertEquals("OL102M", selected[1].second)

        val candidates = listOf(
            ComicEntry("gb-pt-1", "gb-1", "Batman: Ano Um", titlePtBr = "Batman: Ano Um", authors = "Frank Miller", publisher = "Panini Comics", releaseYear = 1987, pageCount = 144, isbn = "9788573516609", format = "Encadernado"),
            ComicEntry("ol-dup-1", "ol-1", "Batman: Year One", titlePtBr = "Batman: Ano Um", authors = "Frank Miller", publisher = "Panini", releaseYear = 1987, pageCount = 0, isbn = "978-85-7351-660-9", format = "Encadernado"),
            ComicEntry("cv-issue-2", "cv-2", "Batman #405", issueNumber = 405, authors = "Frank Miller", publisher = "DC Comics", releaseYear = 1987, pageCount = 32, format = "Edição mensal")
        )
        val deduped = CatalogSearchPolicy.unique(candidates)
        assertEquals(2, deduped.size)
        assertEquals("gb-pt-1", deduped.first().id)
    }

    @Test
    fun item6_and_item7_reframeSupportsDataUrlAndBannerSearchDistinguishesErrors() = runBlocking {
        val bmp = Bitmap.createBitmap(800, 300, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val dataUrl = "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        val decoded = CatalogBanners.bitmapFromBannerValue(dataUrl)
        assertEquals(800, decoded.width)
        assertEquals(300, decoded.height)
    }

    @Test
    fun item9_storageMaintenanceNeverDoubleCountsOverlappingCacheAndPrunesUpdatesInFilesDir() {
        val filesUpdateDir = File(context.filesDir, "updates").apply { mkdirs() }
        val orphanApk = File(filesUpdateDir, "quadrinheiro-${BuildConfig.VERSION_CODE - 1}.apk").apply {
            writeBytes(ByteArray(2048))
        }
        val cacheSample = File(context.cacheDir, "overlap-check.bin").apply {
            writeBytes(ByteArray(4096))
        }
        try {
            val usage = StorageMaintenance.usage(context)
            val matching = usage.largestItems.filter { it.path.endsWith("overlap-check.bin") }
            assertTrue("Cache file inside dataDir must not be duplicated in largest files", matching.size <= 1)

            StorageMaintenance.clearRecoverable(context)
            assertFalse("Installed/old update APK in filesDir/updates should be pruned", orphanApk.exists())
        } finally {
            orphanApk.delete()
            cacheSample.delete()
        }
    }

    @Test
    fun item10_realWorld2024To2026TimestampsSurviveEpochAndLogoutLoginCycle() {
        val storage = SafeStorage(context)
        val uid = "rev11-cloud-user"
        // Real-world 2025 timestamp (previously < Nov 2026 virtual clock, which caused CatalogEpoch to wipe all user comics!)
        val real2025Millis = 1741000000000L
        val comic = ComicEntry(
            id = "batman-year-one",
            externalId = "cv-4050-123",
            title = "Batman: Year One",
            titlePtBr = "Batman: Ano Um",
            authors = "Frank Miller",
            publisher = "DC Comics",
            status = ReadingStatus.READ,
            rating = 5f,
            review = "Obra-prima absoluta.",
            tags = listOf("Clássico", "Noir"),
            pageCount = 144,
            currentPage = 144,
            updatedAt = real2025Millis,
            isDirty = false
        )
        try {
            assertTrue("Real 2025 timestamp must pass CatalogEpoch.afterReset", CatalogEpoch.afterReset(comic))
            val cloudDoc = CloudComicDocument.fromComicEntry(uid, comic).toComicEntry()
            assertNotNull("CloudComicDocument with 2025 timestamp must not be discarded", cloudDoc)
            assertEquals("Batman: Year One", cloudDoc!!.title)
            assertEquals("Batman: Ano Um", cloudDoc.titlePtBr)
            assertEquals(5f, cloudDoc.rating)
            assertEquals("Obra-prima absoluta.", cloudDoc.review)

            // Save to account, simulate logout + login restoration
            storage.saveAuthSession(
                UserProfile(
                    uid = uid,
                    displayName = "Leitor",
                    email = "leitor@quadrinheiro.app",
                    photoUrl = null,
                    isGuest = false,
                    provider = "google"
                )
            )
            storage.saveState(listOf(comic), ReaderProfile(name = "Leitor", updatedAt = real2025Millis), AppSettings(), uid)
            storage.clearAuthSession()

            val restored = storage.loadAccountState(uid)
            assertNotNull(restored)
            assertEquals(1, restored!!.comics.size)
            assertEquals("batman-year-one", restored.comics.single().id)
            assertEquals("Obra-prima absoluta.", restored.comics.single().review)
        } finally {
            storage.clearAuthSession()
            storage.removeItem(SafeStorage.ACCOUNT_STATE_PREFIX + uid)
            storage.removeItem("profile_showcase_$uid")
        }
    }
}
