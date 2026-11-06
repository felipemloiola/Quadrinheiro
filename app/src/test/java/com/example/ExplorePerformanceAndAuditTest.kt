package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CloudUserPreferences
import com.example.data.ModernCatalog
import com.example.data.PersonalizedDiscovery
import com.example.data.RecommendedComic
import com.example.data.SafeStorage
import com.example.data.StorageMaintenance
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.ui.screens.ExploreShowcaseBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExplorePerformanceAndAuditTest {

    private lateinit var context: Context
    private lateinit var storage: SafeStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = SafeStorage(context)
        storage.clearModernCatalogSnapshot()
        ModernCatalog.resetForTests()
        ExploreShowcaseBuilder.resetMemoForTests()
    }

    @After
    fun tearDown() {
        storage.clearModernCatalogSnapshot()
        ModernCatalog.resetForTests()
        ExploreShowcaseBuilder.resetMemoForTests()
    }

    private fun sampleCatalog(count: Int = 48): List<ComicEntry> = (1..count).map { idx ->
        val publisher = when {
            idx % 3 == 0 -> "Marvel"
            idx % 2 == 0 -> "DC Comics"
            else -> "Vertigo"
        }
        val title = when {
            idx == 1 -> "Batman: Ano Um"
            idx == 2 -> "Watchmen Edição Definitiva"
            idx == 3 -> "Sandman: Prelúdios e Noturnos"
            idx % 3 == 0 -> "Homem-Aranha #$idx"
            else -> "Batman Especial #$idx"
        }
        ComicEntry(
            id = "cat-perf-$idx",
            externalId = "cv-perf-$idx",
            title = title,
            authors = if (idx % 2 == 0) "Frank Miller" else "Alan Moore",
            publisher = publisher,
            genre = if (publisher == "Marvel") "Marvel" else "DC / Vertigo",
            releaseYear = 1986 + (idx % 35),
            pageCount = 128 + idx,
            coverUrl = "https://example.com/covers/$idx.jpg",
            synopsis = "Sinopse editorial completa da edição $idx."
        )
    }

    @Test
    fun diskSnapshotPreparationAndRestorationCompletesWithin150ms() {
        val sample = sampleCatalog(48)
        val savedAt = 1_700_000_000_000L

        // Warm up class loading once before measuring strict cache serialization + restoration budget
        storage.saveModernCatalogSnapshot(sample.take(2), savedAt)
        storage.loadModernCatalogSnapshot()
        storage.clearModernCatalogSnapshot()
        ModernCatalog.resetForTests()

        val startNanos = System.nanoTime()
        storage.saveModernCatalogSnapshot(sample, savedAt)
        val restored = ModernCatalog.restoreFromDiskCache(storage)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)

        assertTrue("Cache local do ModernCatalog deve ser restaurado", restored)
        assertEquals(48, ModernCatalog.current.size)
        assertTrue(ModernCatalog.isRestoredFromDiskCache)
        assertTrue(
            "Preparação e restauração do cache levou ${elapsedMs}ms (limite objetivo: <= 150ms)",
            elapsedMs <= 150L
        )
        assertTrue(ModernCatalog.isSnapshotFresh(savedAt + 60_000L))
    }

    @Test
    fun emptyCatalogValidCacheExpiredTtlAndFailedNetworkBehaveDeterministically() = runBlocking {
        // 1. Catálogo vazio e sem cache em disco
        storage.clearModernCatalogSnapshot()
        ModernCatalog.resetForTests()
        assertFalse(ModernCatalog.restoreFromDiskCache(storage))
        assertTrue(ModernCatalog.current.isEmpty())
        assertFalse(ModernCatalog.isSnapshotFresh())
        assertTrue(ExploreShowcaseBuilder.filterStarter(ModernCatalog.current).isEmpty())
        assertTrue(ExploreShowcaseBuilder.filterDc(ModernCatalog.current).isEmpty())
        assertTrue(ExploreShowcaseBuilder.filterMarvel(ModernCatalog.current).isEmpty())

        // 2. Cache válido dentro do TTL de 6h: abre instantaneamente sem chamar a rede
        val baseTimeMs = 1_700_000_000_000L
        val cachedItems = sampleCatalog(12)
        storage.saveModernCatalogSnapshot(cachedItems, savedAtEpochMs = baseTimeMs)
        ModernCatalog.resetForTests()

        var networkCalls = 0
        val withinTtlMs = baseTimeMs + (2L * 60L * 60L * 1000L) // 2h (< 6h TTL)
        ModernCatalog.load(
            context = context,
            forceRefresh = false,
            nowEpochMs = withinTtlMs,
            networkFetcher = {
                networkCalls++
                listOf(
                    ComicEntry(
                        id = "net-should-not-run",
                        externalId = "cv-ignore",
                        title = "Rede Ignorada",
                        authors = "Autor",
                        publisher = "DC"
                    )
                )
            }
        )
        assertEquals("Cache dentro do TTL não deve disparar chamada de rede", 0, networkCalls)
        assertEquals(12, ModernCatalog.current.size)
        assertTrue(ModernCatalog.isRestoredFromDiskCache)
        assertTrue(ModernCatalog.isSnapshotFresh(withinTtlMs))

        // 3. Cache expirado (após 6h TTL) + falha de rede: tenta atualizar mas preserva o cache em memória
        val expiredTtlMs = baseTimeMs + ModernCatalog.SNAPSHOT_TTL_MS + 10_000L
        assertFalse("Cache após 6h deve ser considerado expirado", ModernCatalog.isSnapshotFresh(expiredTtlMs))
        ModernCatalog.load(
            context = context,
            forceRefresh = false,
            nowEpochMs = expiredTtlMs,
            networkFetcher = {
                networkCalls++
                throw IllegalStateException("Falha simulada de rede")
            }
        )
        assertEquals("Cache expirado deve tentar atualizar silenciosamente em background", 1, networkCalls)
        assertEquals("Falha de rede nunca deve apagar o cache já carregado", 12, ModernCatalog.current.size)

        // 4. Cache expirado + sucesso de rede: mescla novos itens e renova o timestamp
        val freshApiEntry = ComicEntry(
            id = "cat-live-api-999",
            externalId = "cv-999",
            title = "Demolidor: A Queda de Murdock",
            authors = "Frank Miller",
            publisher = "Marvel",
            coverUrl = "https://example.com/covers/999.jpg",
            synopsis = "Clássico absoluto."
        )
        ModernCatalog.load(
            context = context,
            forceRefresh = false,
            nowEpochMs = expiredTtlMs + 5_000L,
            networkFetcher = {
                networkCalls++
                listOf(freshApiEntry)
            }
        )
        assertEquals(2, networkCalls)
        assertTrue(ModernCatalog.current.any { it.id == "cat-live-api-999" })
        assertFalse("Após resposta viva da API, flag de apenas-cache deve ser limpa", ModernCatalog.isRestoredFromDiskCache)
        assertTrue(ModernCatalog.isSnapshotFresh())
    }

    @Test
    fun progressiveShowcaseSectionsRespectMaxRecompositionBudgetAndSkipIdenticalUpdates() {
        val catalog = sampleCatalog(36)
        val userLibrary = listOf(catalog.first())

        var starterEmissions = 0
        var dcEmissions = 0
        var marvelEmissions = 0
        var recommendationEmissions = 0

        var currentStarter = ExploreShowcaseBuilder.memoStarter
        var currentDc = ExploreShowcaseBuilder.memoDc
        var currentMarvel = ExploreShowcaseBuilder.memoMarvel
        var currentRecs = ExploreShowcaseBuilder.memoRecommendations

        // Simula abertura inicial do Explorar com publicação progressiva por seção
        val starter1 = ExploreShowcaseBuilder.filterStarter(catalog)
        if (!ExploreShowcaseBuilder.hasSameComicIds(currentStarter, starter1)) {
            currentStarter = starter1
            starterEmissions++
        }
        val dc1 = ExploreShowcaseBuilder.filterDc(catalog)
        if (!ExploreShowcaseBuilder.hasSameComicIds(currentDc, dc1)) {
            currentDc = dc1
            dcEmissions++
        }
        val marvel1 = ExploreShowcaseBuilder.filterMarvel(catalog)
        if (!ExploreShowcaseBuilder.hasSameComicIds(currentMarvel, marvel1)) {
            currentMarvel = marvel1
            marvelEmissions++
        }
        val recs1 = PersonalizedDiscovery.recommendWithReasons(
            catalog = catalog,
            library = userLibrary,
            favoriteUniverse = "DC / Vertigo",
            settings = AppSettings()
        )
        if (!ExploreShowcaseBuilder.hasSameRecommendations(currentRecs, recs1)) {
            currentRecs = recs1
            recommendationEmissions++
        }

        // Segunda passagem com o mesmo catálogo/recomendações (ex: re-entrada na aba ou fim de sync sem mudanças)
        val starter2 = ExploreShowcaseBuilder.filterStarter(catalog.toList())
        if (!ExploreShowcaseBuilder.hasSameComicIds(currentStarter, starter2)) {
            currentStarter = starter2
            starterEmissions++
        }
        val dc2 = ExploreShowcaseBuilder.filterDc(catalog.toList())
        if (!ExploreShowcaseBuilder.hasSameComicIds(currentDc, dc2)) {
            currentDc = dc2
            dcEmissions++
        }
        val marvel2 = ExploreShowcaseBuilder.filterMarvel(catalog.toList())
        if (!ExploreShowcaseBuilder.hasSameComicIds(currentMarvel, marvel2)) {
            currentMarvel = marvel2
            marvelEmissions++
        }
        val recs2: List<RecommendedComic> = recs1.map { it.copy() }
        if (!ExploreShowcaseBuilder.hasSameRecommendations(currentRecs, recs2)) {
            currentRecs = recs2
            recommendationEmissions++
        }

        assertEquals("Seção 'Para começar' deve recompor apenas 1 vez para o mesmo estado", 1, starterEmissions)
        assertEquals("Seção 'Universo DC' deve recompor apenas 1 vez para o mesmo estado", 1, dcEmissions)
        assertEquals("Seção 'Universo Marvel' deve recompor apenas 1 vez para o mesmo estado", 1, marvelEmissions)
        assertEquals("Seção 'Para você' deve recompor apenas 1 vez para o mesmo estado", 1, recommendationEmissions)
        assertTrue("Orçamento máximo de recomposições por seção na abertura (<= 3) respeitado", starterEmissions <= 3 && recommendationEmissions <= 3)
        assertTrue(currentStarter.isNotEmpty())
        assertTrue(currentDc.isNotEmpty())
        assertTrue(currentMarvel.isNotEmpty())
        assertTrue(currentRecs.isNotEmpty())
    }

    @Test
    fun cloudPreferencesFieldBudgetAndStorageMaintenanceNullSafetyHold() {
        val declaredFieldCount = CloudUserPreferences::class.java.declaredFields
            .count { !it.isSynthetic && it.name != "Companion" }
        assertTrue(
            "CloudUserPreferences possui $declaredFieldCount campos declarados (limite de segurança Firestore: <= 45)",
            declaredFieldCount <= 45
        )
        val settings = AppSettings(
            cachedRecommendationsJson = """[{"id":"cat-perf-1","title":"Batman: Ano Um","reason":"Porque você leu Watchmen"}]""",
            recommendationTasteHash = "abc123hash:1700000000000"
        )
        val dto = CloudUserPreferences.fromAppSettings(settings)
        val restored = dto.toAppSettings()
        assertEquals(settings.cachedRecommendationsJson, restored.cachedRecommendationsJson)
        assertEquals(settings.recommendationTasteHash, restored.recommendationTasteHash)

        val usage = StorageMaintenance.usage(context)
        assertNotNull(usage)
        assertTrue(usage.totalBytes >= 0L)
    }

    @Test
    fun heifAndAvifStreamsAreRejectedBeforeBitmapFactory() {
        val fakeHeic = byteArrayOf(
            0x00, 0x00, 0x00, 0x18,
            0x66, 0x74, 0x79, 0x70, // 'ftyp'
            0x68, 0x65, 0x69, 0x63, // 'heic'
            0x00, 0x00, 0x00, 0x00
        )
        val fakeAvif = byteArrayOf(
            0x00, 0x00, 0x00, 0x1C,
            0x66, 0x74, 0x79, 0x70, // 'ftyp'
            0x61, 0x76, 0x69, 0x66, // 'avif'
            0x00, 0x00, 0x00, 0x00
        )
        val validPngHeader = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        )
        val validJpegHeader = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46,
            0x49, 0x46, 0x00, 0x01
        )

        assertTrue(com.example.data.SafeImageFormats.isHeifOrAvifHeader(fakeHeic))
        assertTrue(com.example.data.SafeImageFormats.isHeifOrAvifHeader(fakeAvif))
        assertFalse(com.example.data.SafeImageFormats.isSupportedRasterHeader(fakeHeic))
        assertFalse(com.example.data.SafeImageFormats.isSupportedRasterHeader(fakeAvif))
        assertFalse(com.example.data.ComicBannerResolver.landscape(fakeHeic))
        assertFalse(com.example.data.ComicBannerResolver.landscape(fakeAvif))

        assertTrue(com.example.data.SafeImageFormats.isSupportedRasterHeader(validPngHeader))
        assertTrue(com.example.data.SafeImageFormats.isSupportedRasterHeader(validJpegHeader))
    }
}
