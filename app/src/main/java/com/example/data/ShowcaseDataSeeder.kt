package com.example.data

import android.content.Context
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ComicMoment
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ShowcaseDataSeeder {
    const val SHOWCASE_UID = "showcase_account_v1"
    private const val SEEDED_KEY = "showcase_account_seeded_v5"

    fun seedIfNecessary(context: Context) {
        val storage = SafeStorage(context)
        if (storage.getItem(SEEDED_KEY) == "true") {
            return
        }

        // Tenta restaurar catálogo existente do disco primeiro
        ModernCatalog.restoreFromDiskCache(context)

        // Estrutura inicial com HQs reais do catálogo do aplicativo
        val catalogItems = ModernCatalog.current

        val initialComics = if (catalogItems.isNotEmpty()) {
            catalogItems.take(12).mapIndexed { index, c ->
                when {
                    index < 2 -> c.copy(
                        status = ReadingStatus.READING,
                        currentPage = if (c.pageCount > 0) (c.pageCount * 0.7f).toInt() else 35,
                        favorite = true
                    )
                    index in 2..7 -> c.copy(
                        status = ReadingStatus.READ,
                        currentPage = if (c.pageCount > 0) c.pageCount else 160,
                        rating = 5.0f,
                        favorite = index < 5,
                        readDate = "2026-10-15"
                    )
                    else -> c.copy(
                        status = ReadingStatus.WANT_TO_READ,
                        currentPage = 0
                    )
                }
            }
        } else {
            // HQs reais padrão do catálogo ComicVine / Metron do aplicativo
            listOf(
                ComicEntry(
                    id = "cv-volume-10706",
                    externalId = "cv-volume-10706",
                    title = "Batman: Ano Um",
                    titlePtBr = "Batman: Ano Um",
                    authors = "Frank Miller, David Mazzucchelli",
                    publisher = "DC Comics",
                    genre = "Super-Heróis",
                    format = "Encadernado",
                    releaseYear = 1987,
                    pageCount = 144,
                    currentPage = 144,
                    status = ReadingStatus.READ,
                    rating = 5.0f,
                    favorite = true,
                    readDate = "2026-10-15",
                    addedAt = "2026-10-10T09:15:00Z",
                    coverUrl = "https://comicvine.gamespot.com/a/uploads/scale_super/6/67663/2026856-watchmen_01_00.jpg",
                    synopsis = "O primeiro ano de Bruce Wayne como Batman em Gotham City.",
                    sourceLabel = "Comic Vine",
                    isOnlineResult = true
                ),
                ComicEntry(
                    id = "cv-volume-18131",
                    externalId = "cv-volume-18131",
                    title = "Watchmen",
                    titlePtBr = "Watchmen",
                    authors = "Alan Moore, Dave Gibbons",
                    publisher = "DC Vertigo",
                    genre = "Graphic Novel",
                    format = "Encadernado",
                    releaseYear = 1986,
                    pageCount = 416,
                    currentPage = 416,
                    status = ReadingStatus.READ,
                    rating = 5.0f,
                    favorite = true,
                    readDate = "2026-09-28",
                    addedAt = "2026-09-20T11:00:00Z",
                    coverUrl = "https://comicvine.gamespot.com/a/uploads/scale_super/6/67663/2026856-watchmen_01_00.jpg",
                    synopsis = "Numa versão alternativa de 1985, o assassinato de um ex-vigilante revela uma conspiração global.",
                    sourceLabel = "Comic Vine",
                    isOnlineResult = true
                ),
                ComicEntry(
                    id = "cv-volume-3744",
                    externalId = "cv-volume-3744",
                    title = "Sandman",
                    titlePtBr = "Sandman",
                    authors = "Neil Gaiman",
                    publisher = "DC Vertigo",
                    genre = "Fantasia Sombria",
                    format = "Encadernado",
                    releaseYear = 1989,
                    pageCount = 560,
                    currentPage = 120,
                    status = ReadingStatus.READING,
                    rating = 5.0f,
                    favorite = true,
                    readDate = "2026-11-08",
                    addedAt = "2026-11-01T10:00:00Z",
                    coverUrl = "https://comicvine.gamespot.com/a/uploads/scale_super/6/67663/2026856-watchmen_01_00.jpg",
                    synopsis = "Morpheus libertado de seu cativeiro buscando seus totens de poder.",
                    sourceLabel = "Comic Vine",
                    isOnlineResult = true
                )
            )
        }

        val bannerUrl = initialComics.firstOrNull()?.coverUrl.orEmpty()
        val avatarUrl = initialComics.firstOrNull()?.coverUrl.orEmpty()

        val showcaseProfile = UserProfile(
            uid = SHOWCASE_UID,
            displayName = "Felipe Loiola",
            email = "felipe@quadrinheiro.app",
            photoUrl = avatarUrl,
            isGuest = false,
            provider = "email"
        )

        val readerProfile = ReaderProfile(
            name = "Felipe Loiola",
            handle = "@felipeloiola",
            bio = "Colecionador e crítico de encadernados de luxo e graphic novels.",
            favoriteUniverse = "DC / Vertigo",
            avatarUrl = avatarUrl,
            avatarPreset = "indigo",
            yearlyGoal = 36,
            dailyPagesGoal = 45,
            showGoalOnHome = true
        )

        val favIds = initialComics.filter { it.favorite }.map { it.id }.take(4)
        val favChars = listOf("Batman", "Sandman", "Demolidor", "Homem-Aranha")

        val showcaseDetails = mutableMapOf<String, String>()
        initialComics.filter { it.favorite }.take(4).forEach { c ->
            showcaseDetails["comic:${c.id}"] = ProfileShowcase.comicSnapshot(c)
        }

        val appSettings = AppSettings(
            profileBannerUrl = bannerUrl,
            profileFavoriteComicIds = favIds,
            profileFavoriteCharacters = favChars,
            profileShowcaseDetails = showcaseDetails,
            profileShowcaseUpdatedAt = System.currentTimeMillis(),
            themeId = "OCEAN_BLUE",
            colorScheme = "dark",
            glassLevel = 26,
            edgeRefraction = true,
            glassIntensity = 1.0f,
            glassBlurIntensity = 1.0f,
            enableLensRefraction = true,
            enableChromaticAberration = true,
            glassOpacity = 1.0f,
            showReadingNowBar = true,
            showFloatingMenu = false,
            minimizeTabBarOnScroll = true
        )

        val allUrls = initialComics.map { it.coverUrl } + listOfNotNull(bannerUrl, avatarUrl)
        LibraryDiskCacheRegistry.registerUrls(allUrls)

        storage.saveAuthSession(showcaseProfile)
        storage.saveState(initialComics, readerProfile, appSettings, ownerUid = SHOWCASE_UID)
        storage.saveShowcase(SHOWCASE_UID, appSettings)
        storage.setItem(SEEDED_KEY, "true")

        // Atualização em segundo plano com dados vivos do catálogo
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if (ModernCatalog.current.isEmpty()) {
                    ModernCatalog.load(context)
                }
                val freshCatalog = ModernCatalog.current
                if (freshCatalog.isNotEmpty()) {
                    val freshComics = freshCatalog.take(12).mapIndexed { index, c ->
                        when {
                            index < 2 -> c.copy(
                                status = ReadingStatus.READING,
                                currentPage = if (c.pageCount > 0) (c.pageCount * 0.7f).toInt() else 35,
                                favorite = true
                            )
                            index in 2..7 -> c.copy(
                                status = ReadingStatus.READ,
                                currentPage = if (c.pageCount > 0) c.pageCount else 160,
                                rating = 5.0f,
                                favorite = index < 5,
                                readDate = "2026-10-15"
                            )
                            else -> c.copy(
                                status = ReadingStatus.WANT_TO_READ,
                                currentPage = 0
                            )
                        }
                    }
                    val liveBanner = freshComics.getOrNull(1)?.coverUrl.orEmpty().ifBlank { freshComics.firstOrNull()?.coverUrl.orEmpty() }
                    val liveSettings = appSettings.copy(
                        profileBannerUrl = liveBanner,
                        profileFavoriteComicIds = freshComics.filter { it.favorite }.map { it.id }.take(4)
                    )
                    LibraryDiskCacheRegistry.registerUrls(freshComics.map { it.coverUrl } + listOfNotNull(liveBanner))
                    storage.saveState(freshComics, readerProfile, liveSettings, ownerUid = SHOWCASE_UID)
                    storage.saveShowcase(SHOWCASE_UID, liveSettings)
                }
            }
        }
    }
}
