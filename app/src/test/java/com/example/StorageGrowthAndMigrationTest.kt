package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CloudUserPreferences
import com.example.data.ProfileShowcase
import com.example.data.QuadrinheiroCloudRepository
import com.example.data.RecoverableFilePolicy
import com.example.data.SafeStorage
import com.example.data.StorageMaintenance
import com.example.data.StorageWriteTelemetry
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.UserProfile
import org.json.JSONObject
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageGrowthAndMigrationTest {
    private lateinit var context: Context
    private lateinit var storage: SafeStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = SafeStorage(context)
        StorageWriteTelemetry.resetForTesting()
    }

    @After
    fun tearDown() {
        storage.clearAuthSession()
        storage.removeItem(SafeStorage.STORAGE_KEY)
        storage.removeItem(SafeStorage.GUEST_STATE_KEY)
        storage.removeItem(SafeStorage.TRANSLATION_CACHE_KEY)
        storage.removeItem(SafeStorage.MODERN_CATALOG_SNAPSHOT_KEY)
    }

    @Test
    fun showcaseRoundTripAndMergeAreIdempotentAndNeverTriggerInfinitePush() {
        val rawDetails = (1..14).associate { idx ->
            (if (idx <= 4) "comic:c$idx" else if (idx <= 8) "character:Hero$idx" else "comic:stale-$idx") to
                "detalhe-$idx-" + "x".repeat(200)
        }
        val local = AppSettings(
            profileBannerUrl = "https://cdn.example.com/banner.jpg",
            profileFavoriteComicIds = listOf("c1", "c2", "c3", "c4", "c5"),
            profileFavoriteCharacters = listOf("Hero5", "Hero6", "Hero7", "Hero8", "Hero9"),
            profileShowcaseDetails = rawDetails,
            profileShowcaseUpdatedAt = 1_750_000_000_000L,
            cachedRecommendationsJson = "[" + "\"x\",".repeat(5000) + "\"end\"]",
            recommendationTasteHash = "hash-123"
        )

        val cloudPrefs = CloudUserPreferences.fromAppSettings(local)
        val incomingSettings = cloudPrefs.toAppSettings()
        val remoteMerged = ProfileShowcase.merge(local, incomingSettings)

        assertEquals(8, incomingSettings.profileShowcaseDetails.size)
        assertEquals(incomingSettings.profileShowcaseDetails, remoteMerged.profileShowcaseDetails)
        assertFalse(
            "After pushing local showcase to CloudUserPreferences, merge must not report differs=true",
            ProfileShowcase.differs(remoteMerged, incomingSettings)
        )
    }

    @Test
    fun partialRemoteWindowDoesNotMarkCleanComicsOutsideWindowAsDirty() {
        val cleanLocal = (1..140).map { i ->
            ComicEntry(
                id = "hq-$i",
                externalId = "ext-$i",
                title = "HQ #$i",
                authors = "Autor",
                publisher = "Editora",
                updatedAt = 2_000_000_000_000L - i * 1000L,
                isDirty = false
            )
        }
        val dirtyLocal = listOf(
            ComicEntry(
                id = "hq-dirty-1",
                externalId = "ext-dirty-1",
                title = "HQ Editada Offline 1",
                authors = "Autor",
                publisher = "Editora",
                updatedAt = 2_000_000_050_000L,
                isDirty = true
            ),
            ComicEntry(
                id = "hq-dirty-2",
                externalId = "ext-dirty-2",
                title = "HQ Editada Offline 2",
                authors = "Autor",
                publisher = "Editora",
                updatedAt = 1_999_000_000_000L,
                isDirty = true
            )
        )
        val allLocal = cleanLocal + dirtyLocal

        // Simula observeUserComics(limit = 100) retornando apenas as 100 HQs mais recentes da nuvem
        val partialRemote100 = cleanLocal.take(100)

        val (merged, dirtyWinners) = QuadrinheiroCloudRepository.mergeLocalAndRemoteComics(
            localComics = allLocal,
            remoteComics = partialRemote100,
            isPartialRemoteWindow = true
        )

        assertEquals(142, merged.size)
        assertEquals(
            setOf("hq-dirty-1", "hq-dirty-2"),
            dirtyWinners.map { it.id }.toSet()
        )
        assertTrue(
            "As 40 HQs limpas fora da janela parcial de 100 itens devem continuar com isDirty=false",
            merged.filter { it.id.startsWith("hq-") && !it.id.startsWith("hq-dirty-") }.none { it.isDirty }
        )
    }

    @Test
    fun bloatedLegacyPreferencesAreCompactedAndExternalizedWhilePreservingAllAccounts() {
        val aliceUid = "migration_alice_uid"
        val bobUid = "migration_bob_uid"
        val aliceComic = ComicEntry(id = "alice-hq", externalId = "ext-alice", title = "Sandman", authors = "Neil Gaiman", publisher = "Vertigo")
        val bobComic = ComicEntry(id = "bob-hq", externalId = "ext-bob", title = "Watchmen", authors = "Alan Moore", publisher = "DC")
        val guestComic = ComicEntry(id = "guest-hq", externalId = "ext-guest", title = "Akira", authors = "Katsuhiro Otomo", publisher = "JBC")

        storage.saveState(listOf(aliceComic), ReaderProfile(name = "Alice"), AppSettings(), ownerUid = aliceUid)
        storage.saveState(listOf(bobComic), ReaderProfile(name = "Bob"), AppSettings(), ownerUid = bobUid)
        storage.saveState(listOf(guestComic), ReaderProfile(name = "Convidado"), AppSettings(), ownerUid = "guest_local")

        storage.saveAuthSession(
            UserProfile(uid = aliceUid, displayName = "Alice", email = "alice@example.com", photoUrl = null, isGuest = false, provider = "google")
        )

        // Simula instalação legada inchada: grava TRANSLATION_CACHE_KEY enorme e cópia duplicada bruta direto no SharedPreferences XML
        val rawPrefs = context.getSharedPreferences(SafeStorage.STORAGE_KEY, Context.MODE_PRIVATE)
        val hugeTranslations = JSONObject()
        for (i in 1..850) {
            hugeTranslations.put(
                "legacy-hq-$i",
                JSONObject()
                    .put("titlePtBr", "Título Traduzido $i")
                    .put("synopsisPtBr", "Sinopse longa em português ".repeat(35))
            )
        }
        val aliceFullJson = storage.getItem(SafeStorage.ACCOUNT_STATE_PREFIX + aliceUid)!!
        rawPrefs.edit()
            .putString(SafeStorage.TRANSLATION_CACHE_KEY, hugeTranslations.toString())
            .putString(SafeStorage.STORAGE_KEY, aliceFullJson)
            .commit()

        // Simula APKs de colisão do DownloadManager (-1.apk e .tmp)
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val activeApk = File(updatesDir, "quadrinheiro-999999.apk").apply { writeBytes(ByteArray(128)) }
        val collisionApk = File(updatesDir, "quadrinheiro-999999-1.apk").apply { writeBytes(ByteArray(128)) }
        val tempApk = File(updatesDir, "quadrinheiro-999999.apk.tmp").apply { writeBytes(ByteArray(128)) }

        RecoverableFilePolicy.pruneUpdates(
            roots = listOf(updatesDir),
            installedCode = 100L,
            activeCode = 999999L,
            now = System.currentTimeMillis()
        )
        assertTrue(activeApk.exists())
        assertFalse("Arquivo de colisão -1.apk do DownloadManager deve ser podado", collisionApk.exists())
        assertFalse("Arquivo temporário .apk.tmp deve ser podado", tempApk.exists())
        activeApk.delete()

        val compacted = StorageMaintenance.autoCompactIfNeeded(context)
        assertTrue(compacted)

        // 1. TRANSLATION_CACHE_KEY saiu do SharedPreferences XML e foi para arquivo em cacheDir/quadrinheiro_snapshots <= 512 KB
        assertFalse(rawPrefs.contains(SafeStorage.TRANSLATION_CACHE_KEY))
        val snapshotFile = storage.snapshotFile(SafeStorage.TRANSLATION_CACHE_KEY)
        assertTrue(snapshotFile.exists())
        assertTrue(snapshotFile.exists())
        assertTrue(
            "Cache de traduções deve respeitar o teto de 128 KB + overhead JSON",
            snapshotFile.length() <= SafeStorage.MAX_TRANSLATION_CACHE_BYTES + 16_384L
        )

        // 2. STORAGE_KEY no XML virou ponteiro leve (@ref:), mas getItem(STORAGE_KEY) resolve o JSON completo da conta ativa
        val rawMainOnDisk = rawPrefs.getString(SafeStorage.STORAGE_KEY, null)
        assertNotNull(rawMainOnDisk)
        assertTrue(rawMainOnDisk!!.startsWith("@ref:"))
        val resolvedMirror = JSONObject(storage.getItem(SafeStorage.STORAGE_KEY)!!)
        assertEquals("Alice", resolvedMirror.getJSONObject("profile").getString("name"))

        // 3. Bibliotecas de Alice, Bob e Convidado intactas
        assertEquals(listOf("Sandman"), storage.loadAccountState(aliceUid)!!.comics.map { it.title })
        assertEquals(listOf("Watchmen"), storage.loadAccountState(bobUid)!!.comics.map { it.title })
        assertEquals(listOf("Akira"), storage.loadGuestState().comics.map { it.title })
    }

    @Test
    fun codeCacheAndOrphanUpdatesAreClassifiedAsCacheNotLocalData() {
        val codeCacheDir = File(context.applicationInfo.dataDir, "code_cache").apply { mkdirs() }
        val testShaderFile = File(codeCacheDir, "test_shader.agsl").apply { writeText("shader code content".repeat(2000)) }
        val updatesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.resolve("updates")?.apply { mkdirs() }
        val orphanApk = updatesDir?.resolve("quadrinheiro-999.apk")?.apply { writeBytes(ByteArray(1024 * 50)) }

        val usage = StorageMaintenance.usage(context)
        assertTrue("code_cache deve estar categorizado em categories", usage.categories["Cache do sistema (code_cache)"] ?: 0L > 0L)
        assertTrue("cacheBytes deve incluir o tamanho de code_cache", usage.cacheBytes >= testShaderFile.length())
        if (orphanApk != null) {
            assertTrue("Atualizações baixadas devem estar presentes", (usage.categories["Atualizações baixadas"] ?: 0L) >= orphanApk.length())
        }

        testShaderFile.delete()
        orphanApk?.delete()
    }

    @Test
    fun catalogOnlyTranslationsDoNotRewriteAccountPreferencesAndDiagnosticReportIsSanitized() {
        storage.saveAuthSession(
            UserProfile(uid = "secret_uid_98765", displayName = "Leitor", email = "leitor.secreto@gmail.com", photoUrl = null, isGuest = false, provider = "google")
        )
        storage.saveState(
            listOf(ComicEntry(id = "shelf-1", externalId = "ext-shelf-1", title = "Batman: Ano Um", authors = "Frank Miller", publisher = "Panini")),
            ReaderProfile(name = "Leitor"),
            AppSettings(),
            ownerUid = "secret_uid_98765"
        )
        StorageWriteTelemetry.resetForTesting()

        // Simula 60 traduções de itens do catálogo/Explorar que não pertencem à estante
        for (i in 1..60) {
            storage.saveComicTranslationCacheOnly(
                ComicEntry(
                    id = "catalog-explore-$i",
                    externalId = "ext-explore-$i",
                    title = "Explore Comic #$i",
                    titlePtBr = "HQ do Explorar #$i",
                    synopsisPtBr = "Sinopse localizada para abertura rápida.",
                    authors = "Autor",
                    publisher = "Editora"
                )
            )
        }

        val telemetry = StorageWriteTelemetry.snapshot()
        assertEquals(
            "Traduções de itens fora da estante devem escrever 0 vezes em SharedPreferences",
            0L,
            telemetry.prefsWrites
        )
        assertTrue(telemetry.snapshotCacheWrites > 0L)

        val usage = StorageMaintenance.usage(context)
        assertTrue(usage.cacheBytes <= StorageMaintenance.RECOVERABLE_CACHE_BUDGET_BYTES)
        val report = StorageMaintenance.buildSanitizedDiagnosticReport(context, usage)
        assertTrue(report.contains("Diagnóstico de Armazenamento"))
        assertTrue(report.contains("Snapshots e traduções em cache"))
        assertFalse("Relatório não deve vazar UID bruto em caminhos", report.contains("secret_uid_98765"))
        assertFalse("Relatório não deve vazar e-mail em caminhos", report.contains("leitor.secreto@gmail.com"))
    }
}
