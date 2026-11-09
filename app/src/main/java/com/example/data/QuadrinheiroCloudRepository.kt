package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ComicMoment
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.model.UsernameAvailabilityStatus
import com.example.model.UsernameValidator
import com.example.model.UsernameVerificationState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

@Immutable
data class PaginatedComics(
    val items: List<ComicEntry> = emptyList(),
    val lastSnapshot: DocumentSnapshot? = null,
    val hasMore: Boolean = false
)

@Immutable
data class CloudAuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

@IgnoreExtraProperties
data class CloudUserPreferences(
    val profileBannerUrl: String = "",
    val profileShowcaseDetails: Map<String, String> = emptyMap(),
    val profileShowcaseUpdatedAt: Long = 0L,
    val profileFavoriteComicIds: List<String> = emptyList(),
    val profileFavoriteCharacters: List<String> = emptyList(),
    val themeId: String = "SYSTEM_MONO",
    val colorScheme: String = "system",
    val glassLevel: Int = 26,
    val glassVariant: String = "regular",
    val edgeRefraction: Boolean = true,
    val glassIntensity: Double = 1.0,
    val glassBlurIntensity: Double = 1.0,
    val enableLensRefraction: Boolean = true,
    val enableChromaticAberration: Boolean = true,
    val glassOpacity: Double = 1.0,
    val backgroundExtension: Boolean = true,
    val showBottomAccessory: Boolean = true,
    val showReadingNowBar: Boolean = false,
    val showFloatingMenu: Boolean = true,
    val minimizeTabBarOnScroll: Boolean = true,
    val reduceTransparency: Boolean = false,
    val increaseContrast: Boolean = false,
    val boldText: Boolean = false,
    val dynamicTypeScale: Int = 100,
    val reduceMotion: Boolean = false,
    val onOffLabels: Boolean = false,
    val shelfColumns: Int = 3,
    val showProgressOnCovers: Boolean = true,
    val showRatingsOnGrid: Boolean = true,
    val spine3dEffect: Boolean = true,
    val defaultShelfSort: String = "RECENT",
    val shelfLayout: String = "grid",
    val defaultFormat: String = "Encadernado (TPB)",
    val searchLanguage: String = "ALL",
    val searchProvider: String = "MULTI",
    val hdCovers: Boolean = true,
    val spoilerProtection: Boolean = false,
    val halfStarRatings: Boolean = true,
    val allowRereads: Boolean = true,
    val autoFillReadDate: Boolean = true,
    val showPagesInDiary: Boolean = true,
    val shareStarsMatchTheme: Boolean = true,
    val cachedRecommendationsPayload: String = "",
    val quickReviewTags: List<String> = listOf(
        "Roteiro impecável",
        "Arte incrível",
        "Clássico absoluto",
        "Final surpreendente"
    )
) {
    @get:com.google.firebase.firestore.Exclude
    val recommendationTasteHash: String
        get() = cachedRecommendationsPayload.substringBefore('\n', "").substringBefore('\t').take(64)

    @get:com.google.firebase.firestore.Exclude
    val settingsUpdatedAt: Long
        get() {
            val header = cachedRecommendationsPayload.substringBefore('\n', "")
            val parts = header.split('\t')
            return parts.getOrNull(1)?.toLongOrNull() ?: 0L
        }

    @get:com.google.firebase.firestore.Exclude
    val launcherIconMode: String
        get() {
            val header = cachedRecommendationsPayload.substringBefore('\n', "")
            val parts = header.split('\t')
            return parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "original"
        }

    @get:com.google.firebase.firestore.Exclude
    val cachedRecommendationsJson: String
        get() = if ('\n' in cachedRecommendationsPayload) cachedRecommendationsPayload.substringAfter('\n').take(12288) else ""

    fun toAppSettings(): AppSettings {
        val cleanFavComics = profileFavoriteComicIds.map { it.take(200) }.filter { it.isNotBlank() }.distinct().take(4)
        val cleanFavChars = profileFavoriteCharacters.map { it.take(60) }.filter { it.isNotBlank() }.distinct().take(4)
        return AppSettings(
            profileBannerUrl = profileBannerUrl.take(200000),
            profileShowcaseDetails = ProfileShowcase.sanitizeDetails(profileShowcaseDetails, cleanFavComics, cleanFavChars),
            profileShowcaseUpdatedAt = profileShowcaseUpdatedAt.coerceAtLeast(0L),
            profileFavoriteComicIds = cleanFavComics,
            profileFavoriteCharacters = cleanFavChars,
        themeId = themeId.ifBlank { "SYSTEM_MONO" },
        colorScheme = colorScheme.ifBlank { "system" },
        glassLevel = glassLevel.coerceIn(0, 100),
        glassVariant = glassVariant.ifBlank { "regular" },
        edgeRefraction = edgeRefraction,
        glassIntensity = glassIntensity.toFloat().coerceIn(0.1f, 3f),
        glassBlurIntensity = glassBlurIntensity.toFloat().coerceIn(0.1f, 3f),
        enableLensRefraction = enableLensRefraction,
        enableChromaticAberration = enableChromaticAberration,
        glassOpacity = glassOpacity.toFloat().coerceIn(0.1f, 2f),
        backgroundExtension = backgroundExtension,
        showBottomAccessory = showBottomAccessory,
        showReadingNowBar = showReadingNowBar,
        showFloatingMenu = showFloatingMenu,
        minimizeTabBarOnScroll = minimizeTabBarOnScroll,
        reduceTransparency = reduceTransparency,
        increaseContrast = increaseContrast,
        boldText = boldText,
        dynamicTypeScale = dynamicTypeScale.coerceIn(85, 115),
        reduceMotion = reduceMotion,
        onOffLabels = onOffLabels,
        shelfColumns = shelfColumns.coerceIn(2, 4),
        showProgressOnCovers = showProgressOnCovers,
        showRatingsOnGrid = showRatingsOnGrid,
        spine3dEffect = spine3dEffect,
        defaultShelfSort = defaultShelfSort.ifBlank { "RECENT" },
        shelfLayout = shelfLayout.ifBlank { "grid" },
        defaultFormat = defaultFormat.ifBlank { "Encadernado (TPB)" },
        searchLanguage = searchLanguage.ifBlank { "ALL" },
        searchProvider = searchProvider.ifBlank { "MULTI" },
        hdCovers = hdCovers,
        spoilerProtection = spoilerProtection,
        halfStarRatings = halfStarRatings,
        allowRereads = allowRereads,
        autoFillReadDate = autoFillReadDate,
        showPagesInDiary = showPagesInDiary,
        shareStarsMatchTheme = shareStarsMatchTheme,
        cachedRecommendationsJson = cachedRecommendationsJson,
        recommendationTasteHash = recommendationTasteHash,
        settingsUpdatedAt = settingsUpdatedAt,
        launcherIconMode = launcherIconMode.ifBlank { "original" },
        quickReviewTags = quickReviewTags.map { it.trim().take(80) }.filter { it.isNotEmpty() }.take(40)
        )
    }

    companion object {
        fun fromAppSettings(s: AppSettings): CloudUserPreferences {
            val cleanHash = s.recommendationTasteHash.replace("\n", "").replace("\t", "").take(64)
            val cleanJson = s.cachedRecommendationsJson.take(12288)
            val cleanMode = s.launcherIconMode.replace("\n", "").replace("\t", "").ifBlank { "original" }.take(30)
            val cleanUpdatedAt = s.settingsUpdatedAt.coerceAtLeast(0L)
            val hasMeta = cleanUpdatedAt > 0L || cleanMode != "original"
            val header = if (hasMeta) "$cleanHash\t$cleanUpdatedAt\t$cleanMode" else cleanHash
            val payload = if (header.isEmpty() && cleanJson.isEmpty()) "" else "$header\n$cleanJson"
            val cleanFavComics = s.profileFavoriteComicIds.map { it.take(200) }.filter { it.isNotBlank() }.distinct().take(4)
            val cleanFavChars = s.profileFavoriteCharacters.map { it.take(60) }.filter { it.isNotBlank() }.distinct().take(4)
            return CloudUserPreferences(
                profileBannerUrl = s.profileBannerUrl.take(200000),
                profileShowcaseDetails = ProfileShowcase.sanitizeDetails(s.profileShowcaseDetails, cleanFavComics, cleanFavChars),
                profileShowcaseUpdatedAt = s.profileShowcaseUpdatedAt.coerceAtLeast(0L),
                profileFavoriteComicIds = cleanFavComics,
                profileFavoriteCharacters = cleanFavChars,
                themeId = s.themeId.ifBlank { "SYSTEM_MONO" }.take(60),
                colorScheme = s.colorScheme.ifBlank { "system" }.take(30),
                glassLevel = s.glassLevel.coerceIn(0, 100),
                glassVariant = s.glassVariant.ifBlank { "regular" }.take(30),
                edgeRefraction = s.edgeRefraction,
                glassIntensity = s.glassIntensity.toDouble().coerceIn(0.0, 5.0),
                glassBlurIntensity = s.glassBlurIntensity.toDouble().coerceIn(0.0, 5.0),
                enableLensRefraction = s.enableLensRefraction,
                enableChromaticAberration = s.enableChromaticAberration,
                glassOpacity = s.glassOpacity.toDouble().coerceIn(0.0, 5.0),
                backgroundExtension = s.backgroundExtension,
                showBottomAccessory = s.showBottomAccessory,
                showReadingNowBar = s.showReadingNowBar,
                showFloatingMenu = s.showFloatingMenu,
                minimizeTabBarOnScroll = s.minimizeTabBarOnScroll,
                reduceTransparency = s.reduceTransparency,
                increaseContrast = s.increaseContrast,
                boldText = s.boldText,
                dynamicTypeScale = s.dynamicTypeScale.coerceIn(70, 140),
                reduceMotion = s.reduceMotion,
                onOffLabels = s.onOffLabels,
                shelfColumns = s.shelfColumns.coerceIn(2, 6),
                showProgressOnCovers = s.showProgressOnCovers,
                showRatingsOnGrid = s.showRatingsOnGrid,
                spine3dEffect = s.spine3dEffect,
                defaultShelfSort = s.defaultShelfSort.ifBlank { "RECENT" }.take(40),
                shelfLayout = s.shelfLayout.ifBlank { "grid" }.take(30),
                defaultFormat = s.defaultFormat.ifBlank { "Encadernado (TPB)" }.take(80),
                searchLanguage = s.searchLanguage.ifBlank { "ALL" }.take(20),
                searchProvider = s.searchProvider.ifBlank { "MULTI" }.take(30),
                hdCovers = s.hdCovers,
                spoilerProtection = s.spoilerProtection,
                halfStarRatings = s.halfStarRatings,
                allowRereads = s.allowRereads,
                autoFillReadDate = s.autoFillReadDate,
                showPagesInDiary = s.showPagesInDiary,
                shareStarsMatchTheme = s.shareStarsMatchTheme,
                cachedRecommendationsPayload = payload,
                quickReviewTags = s.quickReviewTags.map { it.trim().take(80) }.filter { it.isNotEmpty() }.take(40)
            )
        }
    }
}

@IgnoreExtraProperties
@Immutable
data class CloudUsernameReservation(
    val ownerId: String = "",
    val handle: String = "",
    val normalizedHandle: String = "",
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
@Immutable
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "Felipe",
    val handle: String = "@felipe",
    val normalizedHandle: String = "felipe",
    val bio: String = "Colecionador de encadernados, graphic novels e mangás.",
    val favoriteUniverse: String = "DC / Vertigo",
    val avatarUrl: String = "",
    val avatarPreset: String = "indigo",
    val yearlyGoal: Int = 24,
    val dailyPagesGoal: Int = 30,
    val showGoalOnHome: Boolean = true,
    val activeReadingComicId: String = "",
    val preferences: CloudUserPreferences? = null,
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedUpdatedAtMillis(): Long {
        // O timestamp gravado pelo servidor e a fonte de verdade (monotonico e imune a relogio
        // do aparelho); o valor do cliente so e usado como fallback para documentos locais.
        val serverMillis = updatedAt?.toDate()?.time ?: 0L
        return if (serverMillis > 0L) serverMillis else updatedAtMillis
    }

    fun toReaderProfile(existingAvatarUrl: String = ""): ReaderProfile {
        val resolvedHandle = when {
            handle.isNotBlank() -> UsernameValidator.formatDisplayHandle(handle)
            normalizedHandle.isNotBlank() -> "@$normalizedHandle"
            else -> "@leitor"
        }
        return ReaderProfile(
            name = name.ifBlank { "Leitor" },
            handle = resolvedHandle,
            bio = bio,
            favoriteUniverse = favoriteUniverse.ifBlank { "DC / Vertigo" },
            avatarUrl = avatarUrl.ifBlank { existingAvatarUrl },
            avatarPreset = avatarPreset.ifBlank { "indigo" },
            yearlyGoal = yearlyGoal.coerceIn(1, 1000),
            dailyPagesGoal = dailyPagesGoal.coerceIn(1, 2000),
            showGoalOnHome = showGoalOnHome,
            activeReadingComicId = activeReadingComicId,
            updatedAt = resolvedUpdatedAtMillis(),
            isDirty = false
        )
    }

    fun toAppSettings(fallback: AppSettings = AppSettings()): AppSettings {
        return preferences?.toAppSettings() ?: fallback
    }

    companion object {
        fun fromReaderProfile(
            uid: String,
            profile: ReaderProfile,
            settings: AppSettings? = null
        ): CloudUserProfile {
            val tsMillis = if (profile.updatedAt > 0L) profile.updatedAt else System.currentTimeMillis()
            val normalizedKey = UsernameValidator.normalizeHandleKey(profile.handle).let { key ->
                if (UsernameValidator.isValidHandleKey(key)) key else "leitor_${uid.take(6).lowercase()}"
            }
            val displayHandle = "@$normalizedKey"
            return CloudUserProfile(
                id = uid,
                ownerId = uid,
                name = profile.name.trim().ifBlank { "Leitor" }.take(80),
                handle = displayHandle.take(60),
                normalizedHandle = normalizedKey.take(30),
                bio = profile.bio.take(400),
                favoriteUniverse = profile.favoriteUniverse.take(80),
                avatarUrl = profile.avatarUrl.take(250_000),
                avatarPreset = profile.avatarPreset.ifBlank { "indigo" }.take(40),
                yearlyGoal = profile.yearlyGoal.coerceIn(1, 1000),
                dailyPagesGoal = profile.dailyPagesGoal.coerceIn(1, 2000),
                showGoalOnHome = profile.showGoalOnHome,
                activeReadingComicId = profile.activeReadingComicId.take(200),
                preferences = settings?.let { CloudUserPreferences.fromAppSettings(it) },
                updatedAtMillis = tsMillis,
                updatedAt = null
            )
        }
    }
}

@IgnoreExtraProperties
@Immutable
data class CloudComicMoment(
    val id: String = "",
    val page: Int = 0,
    val tag: String = "",
    val note: String = "",
    val date: String = "",
    val isSpoiler: Boolean = false
) {
    fun toComicMoment(): ComicMoment = ComicMoment(
        id = id,
        page = page,
        tag = tag.ifBlank { "⚡ Momento Épico" },
        note = note,
        date = date,
        isSpoiler = isSpoiler
    )

    companion object {
        fun fromComicMoment(m: ComicMoment): CloudComicMoment = CloudComicMoment(
            id = m.id,
            page = m.page,
            tag = m.tag.take(60),
            note = m.note.take(2000),
            date = m.date.take(30),
            isSpoiler = m.isSpoiler
        )
    }
}

@IgnoreExtraProperties
@Stable
@Immutable
data class CloudComicDocument(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val externalId: String = "",
    val title: String = "",
    val titlePtBr: String = "",
    val authors: String = "",
    val publisher: String = "",
    val genre: String = "Graphic Novel",
    val format: String = "Encadernado (TPB)",
    val releaseYear: Int = 2026,
    val pageCount: Int = 160,
    val currentPage: Int = 0,
    val status: String = "READ",
    val rating: Double = 0.0,
    val favorite: Boolean = false,
    val rereadCount: Int = 0,
    val tags: List<String> = emptyList(),
    val review: String = "",
    val readDate: String = "",
    val addedAt: String = "",
    val isbn: String = "",
    val coverUrl: String = "",
    val synopsis: String = "",
    val synopsisPtBr: String = "",
    val sourceLabel: String = "Catálogo Base",
    val moments: List<CloudComicMoment> = emptyList(),
    val seriesName: String = "",
    val issueNumber: Int = 0,
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedUpdatedAtMillis(): Long {
        // O timestamp gravado pelo servidor e a fonte de verdade (monotonico e imune a relogio
        // do aparelho); o valor do cliente so e usado como fallback para documentos locais.
        val serverMillis = updatedAt?.toDate()?.time ?: 0L
        val clientMillis = if (updatedAtMillis > 0L) updatedAtMillis else 0L
        val resolved = if (serverMillis > 0L) serverMillis else clientMillis
        return if (resolved > 0L) resolved else (CatalogEpoch.RESET_AT + 1L)
    }

    fun toComicEntry(): ComicEntry {
        val docId = id.ifBlank { externalId.ifBlank { "hq-cloud" } }
        val resolvedTitle = title.ifBlank { titlePtBr }.ifBlank { "Sem Título" }
        val resolvedTitlePtBr = titlePtBr.ifBlank { title }
        val resolvedSynopsisPtBr = synopsisPtBr.ifBlank {
            if (synopsis.isNotBlank() && PortugueseComicTranslator.isPortugueseText(synopsis)) synopsis else ""
        }
        val safePages = pageCount.coerceIn(0, 20000)
        return ComicEntry(
            id = docId,
            externalId = externalId.ifBlank { docId },
            title = resolvedTitle,
            titlePtBr = resolvedTitlePtBr,
            authors = authors.ifBlank { "Autor Desconhecido" },
            publisher = publisher.ifBlank { "Editora" },
            genre = genre.ifBlank { "Graphic Novel" },
            format = format.ifBlank { "Encadernado (TPB)" },
            releaseYear = releaseYear.coerceIn(1800, 2100),
            pageCount = safePages,
            currentPage = currentPage.coerceIn(0, safePages),
            status = ReadingStatus.fromKey(status),
            rating = rating.toFloat().coerceIn(0f, 5f),
            favorite = favorite,
            rereadCount = rereadCount.coerceIn(0, 500),
            tags = tags.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(30),
            review = review,
            readDate = readDate,
            addedAt = addedAt,
            isbn = isbn,
            coverUrl = coverUrl,
            synopsis = synopsis.ifBlank { resolvedSynopsisPtBr },
            synopsisPtBr = resolvedSynopsisPtBr,
            sourceLabel = sourceLabel.ifBlank { "Catálogo Base" },
            isOnlineResult = false,
            moments = moments.map { it.toComicMoment() },
            seriesName = seriesName,
            issueNumber = issueNumber.coerceIn(0, 100000),
            updatedAt = resolvedUpdatedAtMillis(),
            isDirty = false
        )
    }

    companion object {
        fun fromComicEntry(uid: String, comic: ComicEntry): CloudComicDocument {
            val safeId = comic.id.ifBlank { comic.externalId.ifBlank { "hq-${System.currentTimeMillis()}" } }
                .replace("/", "_")
            val safePages = comic.pageCount.coerceIn(0, 20000)
            val resolvedOriginalTitle = comic.title.trim().ifBlank { comic.titlePtBr.trim() }.ifBlank { "Sem Título" }.take(300)
            val resolvedTitlePtBr = comic.titlePtBr.trim().ifBlank { resolvedOriginalTitle }.take(300)
            val resolvedSynopsisPtBr = comic.synopsisPtBr.ifBlank {
                if (comic.synopsis.isNotBlank() && PortugueseComicTranslator.isPortugueseText(comic.synopsis)) {
                    comic.synopsis
                } else {
                    ""
                }
            }.take(5000)
            // Relógio atrasado ou item migrado com updatedAt=0 ficaria abaixo de RESET_AT e sumiria
            // da nuvem por causa do filtro `afterReset`. Só reescreve o carimbo nesses casos; itens
            // posteriores ao reset preservam o valor original.
            val tsMillis = if (comic.updatedAt > CatalogEpoch.RESET_AT) comic.updatedAt else System.currentTimeMillis()
            return CloudComicDocument(
                id = safeId,
                ownerId = uid,
                externalId = comic.externalId.ifBlank { safeId }.take(200),
                title = resolvedOriginalTitle,
                titlePtBr = resolvedTitlePtBr,
                authors = comic.authors.trim().take(300),
                publisher = comic.publisher.trim().take(160),
                genre = comic.genre.take(120),
                format = comic.format.take(120),
                releaseYear = comic.releaseYear.coerceIn(1800, 2100),
                pageCount = safePages,
                currentPage = comic.currentPage.coerceIn(0, safePages),
                status = comic.status.key,
                rating = comic.rating.toDouble().coerceIn(0.0, 5.0),
                favorite = comic.favorite,
                rereadCount = comic.rereadCount.coerceIn(0, 500),
                tags = comic.tags.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(30),
                review = comic.review.take(5000),
                readDate = comic.readDate.take(30),
                addedAt = comic.addedAt.take(60),
                isbn = comic.isbn.take(60),
                coverUrl = comic.coverUrl.take(2000),
                synopsis = comic.synopsis.ifBlank { resolvedSynopsisPtBr }.take(5000),
                synopsisPtBr = resolvedSynopsisPtBr,
                sourceLabel = comic.sourceLabel.take(120),
                moments = comic.moments.map { CloudComicMoment.fromComicMoment(it) },
                seriesName = comic.seriesName.take(200),
                issueNumber = comic.issueNumber.coerceIn(0, 100000),
                updatedAtMillis = tsMillis,
                updatedAt = null
            )
        }
    }
}

fun handleFirestoreError(
    error: Exception,
    operationType: OperationType,
    path: String?,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser = auth.currentUser
    val providerArray = JSONArray()
    currentUser?.providerData?.forEach { userInfo ->
        providerArray.put(
            JSONObject().apply {
                put("providerId", userInfo.providerId)
                put("displayName", userInfo.displayName ?: JSONObject.NULL)
                put("email", userInfo.email ?: JSONObject.NULL)
                put("photoUrl", userInfo.photoUrl?.toString() ?: JSONObject.NULL)
            }
        )
    }

    val authJson = JSONObject().apply {
        put("userId", currentUser?.uid ?: JSONObject.NULL)
        put("email", currentUser?.email ?: JSONObject.NULL)
        put("emailVerified", currentUser?.isEmailVerified ?: false)
        put("isAnonymous", currentUser?.isAnonymous ?: false)
        put("providerInfo", providerArray)
    }

    val errorInfo = JSONObject().apply {
        put("error", error.message ?: error.toString())
        if (error is FirebaseFirestoreException) {
            put("code", error.code.name)
        }
        put("operationType", operationType.name.lowercase())
        put("path", path ?: JSONObject.NULL)
        put("authInfo", authJson)
    }

    Log.e("FirestoreError", errorInfo.toString())
    throw error
}

class QuadrinheiroCloudRepository(
    context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    ),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val appContext = context.applicationContext

    init {
        check(!CloudCacheRecovery.needsReview(appContext)) {"A revisão da sincronização local está pendente"}
        // Target 10 MiB for evictable documents; pending mutations are not capped by this setting.
        val dbKey = System.identityHashCode(db)
        if (configuredDatabases.add(dbKey) &&
            db.firestoreSettings.cacheSettings !is com.google.firebase.firestore.MemoryCacheSettings
        ) {
            runCatching {
                val cacheSettings = PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FIRESTORE_OFFLINE_CACHE_BUDGET_BYTES)
                    .build()
                val settings = FirebaseFirestoreSettings.Builder(db.firestoreSettings)
                    .setLocalCacheSettings(cacheSettings)
                    .build()
                db.firestoreSettings = settings
            }
        }
    }

    fun currentAuthUser(): CloudAuthUser? {
        return auth.currentUser?.toCloudAuthUser()
    }

    fun observeAuthState(): Flow<CloudAuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toCloudAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    private fun FirebaseUser.toCloudAuthUser(): CloudAuthUser {
        return CloudAuthUser(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString()
        )
    }

    suspend fun signInWithGoogleInteractive(activityContext: Context): Result<CloudAuthUser> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val serverClientId = GoogleClientId.required(appContext) /* GOOGLE_CLIENT_ID_RUNTIME_V6 */
            val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            val customCredential = result.credential as? CustomCredential
                ?: return Result.failure(IllegalStateException("Credencial Google inválida"))
            if (customCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(IllegalStateException("Tipo de credencial inesperado"))
            }
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(customCredential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user?.toCloudAuthUser()
                ?: return Result.failure(IllegalStateException("Usuário Firebase nulo após login"))
            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("QuadrinheiroCloudRepo", "Erro no Google Sign-In", e)
            Result.failure(e)
        }
    }

    suspend fun trySilentGoogleSignIn(activityContext: Context): Result<CloudAuthUser?> {
        val existing = auth.currentUser
        if (existing != null) {
            return Result.success(existing.toCloudAuthUser())
        }
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val serverClientId = GoogleClientId.required(appContext) /* GOOGLE_CLIENT_ID_RUNTIME_V6 */
            val silentOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(silentOption)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            val customCredential = result.credential as? CustomCredential ?: return Result.success(null)
            if (customCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.success(null)
            }
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(customCredential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            Result.success(authResult.user?.toCloudAuthUser())
        } catch (_: NoCredentialException) {
            Result.success(null)
        } catch (_: Exception) {
            Result.success(null)
        }
    }

    suspend fun signOut(activityContext: Context? = null) {
        auth.signOut()
        if (activityContext != null) {
            try {
                CredentialManager.create(activityContext).clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
            }
        }
    }

    private fun DocumentSnapshot.toCloudUserProfileSafe(): CloudUserProfile? {
        if (!exists()) return null
        runCatching { toObject(CloudUserProfile::class.java) }.getOrNull()?.let { return it }
        return runCatching {
            val rawUpdatedAt = get("updatedAt")
            val tsMillis = getLong("updatedAtMillis") ?: when (rawUpdatedAt) {
                is Timestamp -> rawUpdatedAt.toDate().time
                is Number -> rawUpdatedAt.toLong()
                else -> 0L
            }
            val rawPrefs = get("preferences")
            val parsedPrefs = when (rawPrefs) {
                is Map<*, *> -> runCatching {
                    val map = rawPrefs as Map<String, Any?>
                    CloudUserPreferences(
                        profileBannerUrl = (map["profileBannerUrl"] as? String).orEmpty(),
                        profileShowcaseUpdatedAt = (map["profileShowcaseUpdatedAt"] as? Number)?.toLong() ?: 0L,
                        themeId = (map["themeId"] as? String).orEmpty().ifBlank { "SYSTEM_MONO" },
                        colorScheme = (map["colorScheme"] as? String).orEmpty().ifBlank { "system" },
                        glassLevel = (map["glassLevel"] as? Number)?.toInt() ?: 26,
                        glassVariant = (map["glassVariant"] as? String).orEmpty().ifBlank { "regular" },
                        edgeRefraction = (map["edgeRefraction"] as? Boolean) ?: true,
                        glassIntensity = (map["glassIntensity"] as? Number)?.toDouble() ?: 1.0,
                        glassBlurIntensity = (map["glassBlurIntensity"] as? Number)?.toDouble() ?: 1.0,
                        enableLensRefraction = (map["enableLensRefraction"] as? Boolean) ?: true,
                        enableChromaticAberration = (map["enableChromaticAberration"] as? Boolean) ?: true,
                        glassOpacity = (map["glassOpacity"] as? Number)?.toDouble() ?: 1.0,
                        backgroundExtension = (map["backgroundExtension"] as? Boolean) ?: true,
                        showBottomAccessory = (map["showBottomAccessory"] as? Boolean) ?: true,
                        showReadingNowBar = (map["showReadingNowBar"] as? Boolean) ?: false,
                        showFloatingMenu = (map["showFloatingMenu"] as? Boolean) ?: false,
                        minimizeTabBarOnScroll = (map["minimizeTabBarOnScroll"] as? Boolean) ?: true,
                        reduceTransparency = (map["reduceTransparency"] as? Boolean) ?: false,
                        increaseContrast = (map["increaseContrast"] as? Boolean) ?: false,
                        boldText = (map["boldText"] as? Boolean) ?: false,
                        dynamicTypeScale = (map["dynamicTypeScale"] as? Number)?.toInt() ?: 100,
                        reduceMotion = (map["reduceMotion"] as? Boolean) ?: false,
                        onOffLabels = (map["onOffLabels"] as? Boolean) ?: false,
                        shelfColumns = (map["shelfColumns"] as? Number)?.toInt() ?: 3,
                        showProgressOnCovers = (map["showProgressOnCovers"] as? Boolean) ?: true,
                        showRatingsOnGrid = (map["showRatingsOnGrid"] as? Boolean) ?: true,
                        spine3dEffect = (map["spine3dEffect"] as? Boolean) ?: true,
                        defaultShelfSort = (map["defaultShelfSort"] as? String).orEmpty().ifBlank { "RECENT" },
                        shelfLayout = (map["shelfLayout"] as? String).orEmpty().ifBlank { "grid" },
                        defaultFormat = (map["defaultFormat"] as? String).orEmpty().ifBlank { "Encadernado (TPB)" },
                        searchLanguage = (map["searchLanguage"] as? String).orEmpty().ifBlank { "ALL" },
                        searchProvider = (map["searchProvider"] as? String).orEmpty().ifBlank { "MULTI" },
                        hdCovers = (map["hdCovers"] as? Boolean) ?: true,
                        spoilerProtection = (map["spoilerProtection"] as? Boolean) ?: false,
                        halfStarRatings = (map["halfStarRatings"] as? Boolean) ?: true,
                        allowRereads = (map["allowRereads"] as? Boolean) ?: true,
                        autoFillReadDate = (map["autoFillReadDate"] as? Boolean) ?: true,
                        showPagesInDiary = (map["showPagesInDiary"] as? Boolean) ?: true,
                        shareStarsMatchTheme = (map["shareStarsMatchTheme"] as? Boolean) ?: true,
                        cachedRecommendationsPayload = (map["cachedRecommendationsPayload"] as? String).orEmpty()
                    )
                }.getOrNull()
                else -> null
            }
            CloudUserProfile(
                id = id,
                ownerId = getString("ownerId").orEmpty().ifBlank { id },
                name = getString("name").orEmpty().ifBlank { "Leitor" },
                handle = getString("handle").orEmpty().ifBlank { "@leitor" },
                normalizedHandle = getString("normalizedHandle").orEmpty(),
                bio = getString("bio").orEmpty(),
                favoriteUniverse = getString("favoriteUniverse").orEmpty().ifBlank { "DC / Vertigo" },
                avatarUrl = getString("avatarUrl").orEmpty(),
                avatarPreset = getString("avatarPreset").orEmpty().ifBlank { "indigo" },
                yearlyGoal = (getLong("yearlyGoal") ?: 24L).toInt(),
                dailyPagesGoal = (getLong("dailyPagesGoal") ?: 30L).toInt(),
                showGoalOnHome = getBoolean("showGoalOnHome") ?: true,
                activeReadingComicId = getString("activeReadingComicId").orEmpty(),
                preferences = parsedPrefs,
                updatedAtMillis = tsMillis,
                updatedAt = rawUpdatedAt as? Timestamp
            )
        }.getOrNull()
    }

    private fun DocumentSnapshot.toCloudComicDocumentSafe(): CloudComicDocument? {
        if (!exists()) return null
        runCatching { toObject(CloudComicDocument::class.java) }.getOrNull()?.let { return it }
        return runCatching {
            val rawUpdatedAt = get("updatedAt")
            val tsMillis = getLong("updatedAtMillis") ?: when (rawUpdatedAt) {
                is Timestamp -> rawUpdatedAt.toDate().time
                is Number -> rawUpdatedAt.toLong()
                else -> 0L
            }
            val rawTags = (get("tags") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            val rawMoments = (get("moments") as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                CloudComicMoment(
                    id = (map["id"] as? String).orEmpty(),
                    page = (map["page"] as? Number)?.toInt() ?: 0,
                    tag = (map["tag"] as? String).orEmpty(),
                    note = (map["note"] as? String).orEmpty(),
                    date = (map["date"] as? String).orEmpty(),
                    isSpoiler = (map["isSpoiler"] as? Boolean) ?: false
                )
            }.orEmpty()
            CloudComicDocument(
                id = id,
                ownerId = getString("ownerId").orEmpty(),
                externalId = getString("externalId").orEmpty().ifBlank { id },
                title = getString("title").orEmpty(),
                titlePtBr = getString("titlePtBr").orEmpty(),
                authors = getString("authors").orEmpty(),
                publisher = getString("publisher").orEmpty(),
                genre = getString("genre").orEmpty().ifBlank { "Graphic Novel" },
                format = getString("format").orEmpty().ifBlank { "Encadernado (TPB)" },
                releaseYear = (getLong("releaseYear") ?: 2026L).toInt(),
                pageCount = (getLong("pageCount") ?: 160L).toInt(),
                currentPage = (getLong("currentPage") ?: 0L).toInt(),
                status = getString("status").orEmpty().ifBlank { "READ" },
                rating = getDouble("rating") ?: (getLong("rating")?.toDouble() ?: 0.0),
                favorite = getBoolean("favorite") ?: false,
                rereadCount = (getLong("rereadCount") ?: 0L).toInt(),
                tags = rawTags,
                review = getString("review").orEmpty(),
                readDate = getString("readDate").orEmpty(),
                addedAt = getString("addedAt").orEmpty(),
                isbn = getString("isbn").orEmpty(),
                coverUrl = getString("coverUrl").orEmpty(),
                synopsis = getString("synopsis").orEmpty(),
                synopsisPtBr = getString("synopsisPtBr").orEmpty(),
                sourceLabel = getString("sourceLabel").orEmpty().ifBlank { "Catálogo Base" },
                moments = rawMoments,
                seriesName = getString("seriesName").orEmpty(),
                issueNumber = (getLong("issueNumber") ?: 0L).toInt(),
                updatedAtMillis = tsMillis,
                updatedAt = rawUpdatedAt as? Timestamp
            )
        }.getOrNull()
    }

    fun observeUserProfile(userId: String): Flow<CloudUserProfile?> = callbackFlow {
        val path = "users/$userId"
        val registration = db.collection("users").document(userId)
            .addSnapshotListener(FirestoreSnapshotDispatch.executor, com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                try {
                    if (error != null) {
                        runCatching { handleFirestoreError(error, OperationType.GET, path, auth) }
                        close()
                        return@addSnapshotListener
                    }
                    // The UI already has this local revision. Republishing a pending
                    // SDK echo can cancel collectLatest's waiter and grow the write queue.
                    // INCLUDE guarantees a committed event when only metadata changes.
                    if (snapshot?.metadata?.hasPendingWrites() == true) return@addSnapshotListener
                    val profile = snapshot?.toCloudUserProfileSafe()
                    trySend(profile)
                } catch (t: Throwable) {
                    Log.e("QuadrinheiroCloudRepo", "Erro ao observar perfil na nuvem", t)
                }
            }
        awaitClose {
            runCatching { registration.remove() }
        }
    }

    /**
     * Consulta inicial ultra-rápida diretamente do CACHE LOCAL persistente do Firestore (0 leituras cobradas, 0 delay de rede).
     */
    suspend fun fetchComicsFromCache(userId: String, limit: Long = 100L): List<ComicEntry> {
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("comics")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get(Source.CACHE)
                .await()
            snapshot.documents.mapNotNull { it.toCloudComicDocumentSafe()?.toComicEntry() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Paginação de quadrinhos no Firestore: busca páginas de [pageSize] itens com cursor startAfter.
     * Reduz drasticamente as leituras no Firestore e otimiza a performance em acervos volumosos.
     */
    suspend fun fetchComicsPage(
        userId: String,
        pageSize: Long = 20L,
        startAfterDoc: DocumentSnapshot? = null,
        preferCache: Boolean = false
    ): Result<PaginatedComics> {
        val path = "users/$userId/comics"
        return try {
            var query = db.collection("users").document(userId)
                .collection("comics")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(pageSize)

            if (startAfterDoc != null) {
                query = query.startAfter(startAfterDoc)
            }

            val source = if (preferCache) Source.CACHE else Source.DEFAULT
            val snapshot = try {
                query.get(source).await()
            } catch (e: Exception) {
                if (preferCache) {
                    query.get(Source.DEFAULT).await()
                } else {
                    throw e
                }
            }

            val items = snapshot.documents.mapNotNull { doc ->
                doc.toCloudComicDocumentSafe()?.toComicEntry()
            }
            val last = snapshot.documents.lastOrNull()
            val hasMore = snapshot.size() >= pageSize.toInt()

            Result.success(
                PaginatedComics(
                    items = items,
                    lastSnapshot = last,
                    hasMore = hasMore
                )
            )
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.LIST, path, auth) }
            Result.failure(e)
        }
    }

    /**
     * Observa em tempo real os quadrinhos mais recentes do usuário com limite controlado (padrão 35).
     * Evita leitura irrestrita de coleções inteiras a cada mutação de documento.
     */
    fun observeUserComics(userId: String, limit: Long = 100L): Flow<List<ComicEntry>> = callbackFlow {
        val path = "users/$userId/comics"
        val registration = db.collection("users").document(userId)
            .collection("comics")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener(FirestoreSnapshotDispatch.executor, com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                try {
                    if (error != null) {
                        runCatching { handleFirestoreError(error, OperationType.LIST, path, auth) }
                        close()
                        return@addSnapshotListener
                    }
                    if (snapshot?.metadata?.hasPendingWrites() == true) return@addSnapshotListener
                    val entries = snapshot?.documents
                        ?.mapNotNull { it.toCloudComicDocumentSafe()?.toComicEntry() }
                        .orEmpty()
                    trySend(entries)
                } catch (t: Throwable) {
                    Log.e("QuadrinheiroCloudRepo", "Erro ao observar quadrinhos na nuvem", t)
                }
            }
        awaitClose {
            runCatching { registration.remove() }
        }
    }.conflate() // COLLECTION_SNAPSHOT_BACKGROUND_V15: retain the newest collection snapshot.

    suspend fun checkUsernameAvailability(
        rawHandle: String,
        currentUserId: String? = auth.currentUser?.uid
    ): UsernameVerificationState {
        val validationError = UsernameValidator.validateHandle(rawHandle)
        val key = UsernameValidator.normalizeHandleKey(rawHandle)
        val formatted = UsernameValidator.formatDisplayHandle(rawHandle)
        if (validationError != null) {
            return UsernameVerificationState(
                inputHandle = formatted,
                normalizedHandle = key,
                status = UsernameAvailabilityStatus.INVALID,
                message = validationError
            )
        }
        if (currentUserId.isNullOrBlank()) {
            return UsernameVerificationState(
                inputHandle = formatted,
                normalizedHandle = key,
                status = UsernameAvailabilityStatus.AVAILABLE_NEW,
                message = "Identificador $formatted disponível"
            )
        }
        val path = "usernames/$key"
        return try {
            val snap = kotlinx.coroutines.withTimeout(1500L) {
                db.collection("usernames").document(key).get().await()
            }
            if (!snap.exists()) {
                UsernameVerificationState(
                    inputHandle = formatted,
                    normalizedHandle = key,
                    status = UsernameAvailabilityStatus.AVAILABLE_NEW,
                    message = "Identificador $formatted disponível"
                )
            } else {
                val ownerId = snap.getString("ownerId").orEmpty()
                if (ownerId.isBlank() || ownerId == currentUserId) {
                    UsernameVerificationState(
                        inputHandle = formatted,
                        normalizedHandle = key,
                        status = UsernameAvailabilityStatus.AVAILABLE_OWNED,
                        message = "Identificador $formatted vinculado à sua conta"
                    )
                } else {
                    UsernameVerificationState(
                        inputHandle = formatted,
                        normalizedHandle = key,
                        status = UsernameAvailabilityStatus.TAKEN,
                        message = "Indisponível: $formatted já está em uso por outro leitor"
                    )
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            UsernameVerificationState(
                inputHandle = formatted,
                normalizedHandle = key,
                status = UsernameAvailabilityStatus.AVAILABLE_NEW,
                message = "Disponível! Identificador $formatted salvo na sua conta"
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.GET, path, auth) }
            UsernameVerificationState(
                inputHandle = formatted,
                normalizedHandle = key,
                status = UsernameAvailabilityStatus.AVAILABLE_NEW,
                message = "Disponível! Identificador $formatted salvo na sua conta"
            )
        }
    }

    suspend fun getUsernameReservation(handleKey: String): CloudUsernameReservation? {
        val cleanKey = UsernameValidator.normalizeHandleKey(handleKey)
        if (!UsernameValidator.isValidHandleKey(cleanKey)) return null
        val path = "usernames/$cleanKey"
        return try {
            val snap = kotlinx.coroutines.withTimeout(2000L) {
                db.collection("usernames").document(cleanKey).get().await()
            }
            snap.toObject(CloudUsernameReservation::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path, auth)
            null
        }
    }

    suspend fun saveUserProfile(
        userId: String,
        profile: ReaderProfile,
        settings: AppSettings? = null,
        strictHandleReservation: Boolean = true
    ) {
        val path = "users/$userId"
        var docData = CloudUserProfile.fromReaderProfile(userId, profile, settings)
        var handleKey = docData.normalizedHandle
        try {
            if (UsernameValidator.isValidHandleKey(handleKey)) {
                val userDocRef = db.collection("users").document(userId)
                val existingUserSnap = runCatching {
                    kotlinx.coroutines.withTimeoutOrNull(1200L) { userDocRef.get().await() }
                }.getOrNull()
                val oldHandleKey = existingUserSnap?.let { snap ->
                    snap.getString("normalizedHandle")?.takeIf { it.isNotBlank() }
                        ?: snap.getString("handle")?.let { UsernameValidator.normalizeHandleKey(it) }
                }.orEmpty()

                val targetHandleSnap = runCatching {
                    kotlinx.coroutines.withTimeoutOrNull(1200L) {
                        db.collection("usernames").document(handleKey).get().await()
                    }
                }.getOrNull()
                val targetOwnerId = targetHandleSnap?.getString("ownerId").orEmpty()
                val isTakenByAnotherUser = targetHandleSnap?.exists() == true &&
                    targetOwnerId.isNotBlank() &&
                    targetOwnerId != userId
                val isTemplateDefaultHandle = handleKey == "felipe_hqs" || handleKey == "felipe" || handleKey == "convidado"

                if (isTakenByAnotherUser && (!strictHandleReservation || isTemplateDefaultHandle)) {
                    // Se o handle padrão (ex: @felipe ou @felipe_hqs) já pertence a outra conta no Firestore,
                    // preserva o handle anterior do próprio usuário ou gera um identificador exclusivo
                    // para garantir que o perfil, metas, foto e activeReadingComicId sejam salvos!
                    val fallbackKey = if (oldHandleKey.isNotBlank() && oldHandleKey != handleKey) {
                        oldHandleKey
                    } else {
                        "${handleKey}_${userId.take(5).lowercase()}".take(30)
                    }
                    handleKey = fallbackKey
                    docData = docData.copy(
                        handle = "@$fallbackKey",
                        normalizedHandle = fallbackKey
                    )
                }

                val oldShouldDelete = if (
                    oldHandleKey.isNotBlank() &&
                    oldHandleKey != handleKey &&
                    UsernameValidator.isValidHandleKey(oldHandleKey)
                ) {
                    val oldSnap = runCatching {
                        kotlinx.coroutines.withTimeoutOrNull(1200L) {
                            db.collection("usernames").document(oldHandleKey).get().await()
                        }
                    }.getOrNull()
                    oldSnap?.exists() == true && oldSnap.getString("ownerId") == userId
                } else {
                    false
                }

                val reservation = CloudUsernameReservation(
                    ownerId = userId,
                    handle = docData.handle,
                    normalizedHandle = handleKey,
                    updatedAtMillis = docData.updatedAtMillis,
                    updatedAt = null
                )

                if (strictHandleReservation && !isTemplateDefaultHandle) {
                    awaitAccountWrite(userId, docData to true) { db.runBatch { batch ->
                            batch.set(db.collection("usernames").document(handleKey), reservation)
                            if (oldShouldDelete) {
                                batch.delete(db.collection("usernames").document(oldHandleKey))
                            }
                            batch.set(userDocRef, docData)
                        } }
                } else {
                    try {
                        awaitAccountWrite(userId, docData to false) { db.runBatch { batch ->
                                batch.set(db.collection("usernames").document(handleKey), reservation)
                                if (oldShouldDelete) {
                                    batch.delete(db.collection("usernames").document(oldHandleKey))
                                }
                                batch.set(userDocRef, docData)
                            } }
                    } catch (error: FirebaseFirestoreException) {
                        // Only a definitive reservation rejection can use the profile-only
                        // fallback. A timeout leaves the original SDK mutation pending.
                        if (error.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw error
                        awaitAccountWrite(userId, docData) { userDocRef.set(docData) }
                    }
                }
                StorageWriteTelemetry.recordFirestoreProfileWrite()
            } else {
                awaitAccountWrite(userId, docData) { db.collection("users").document(userId).set(docData) }
                StorageWriteTelemetry.recordFirestoreProfileWrite()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path, auth)
        }
    }

    suspend fun getUserProfile(userId: String): CloudUserProfile? {
        val path = "users/$userId"
        return try {
            val snap = db.collection("users").document(userId).get().await()
            snap.toCloudUserProfileSafe()
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.GET, path, auth) }
            null
        }
    }

    suspend fun saveComic(userId: String, comic: ComicEntry) {
        val cloudDoc = CloudComicDocument.fromComicEntry(userId, comic)
        val path = "users/$userId/comics/${cloudDoc.id}"
        try {
            awaitAccountWrite(userId, cloudDoc) { db.collection("users").document(userId)
                .collection("comics")
                .document(cloudDoc.id)
                .set(cloudDoc) }
            StorageWriteTelemetry.recordFirestoreComicWrite(1)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path, auth)
        }
    }

    suspend fun getComic(userId: String, comicId: String): ComicEntry? {
        val safeId = comicId.replace("/", "_")
        val path = "users/$userId/comics/$safeId"
        return try {
            val snap = db.collection("users").document(userId)
                .collection("comics")
                .document(safeId)
                .get()
                .await()
            snap.toCloudComicDocumentSafe()?.toComicEntry()
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.GET, path, auth) }
            null
        }
    }

    suspend fun deleteComic(userId: String, comicId: String) {
        val safeId = comicId.replace("/", "_")
        val path = "users/$userId/comics/$safeId"
        try {
            db.collection("users").document(userId)
                .collection("comics")
                .document(safeId)
                .delete()
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path, auth)
        }
    }

    suspend fun deleteAllUserComics(userId: String) {
        val path = "users/$userId/comics"
        try {
            val colRef = db.collection("users").document(userId).collection("comics")
            val snap = colRef.get().await()
            val docs = snap.documents
            if (docs.isEmpty()) return
            for (chunk in docs.chunked(450)) {
                db.runBatch { batch ->
                    for (doc in chunk) {
                        batch.delete(doc.reference)
                    }
                }.await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path, auth)
        }
    }

    suspend fun syncAllComicsBatch(userId: String, comics: List<ComicEntry>): Set<String> {
        if (comics.isEmpty()) return emptySet()
        val path = "users/$userId/comics"
        val syncedIds = mutableSetOf<String>()
        try {
            // Firestore suporta até 500 operações por lote em runBatch
            val chunks = comics.chunked(450)
            val colRef = db.collection("users").document(userId).collection("comics")
            for (chunk in chunks) {
                val documents=chunk.map { CloudComicDocument.fromComicEntry(userId,it) }
                awaitAccountWrite(userId, documents) { db.runBatch { batch ->
                    for (comic in chunk) {
                        val cloudDoc = CloudComicDocument.fromComicEntry(userId, comic)
                        batch.set(colRef.document(cloudDoc.id), cloudDoc)
                        syncedIds.add(comic.id)
                        if (comic.externalId.isNotBlank()) syncedIds.add(comic.externalId)
                        syncedIds.add(cloudDoc.id)
                    }
                } }
                StorageWriteTelemetry.recordFirestoreComicBatchWrite(chunk.size)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path, auth)
        }
        return syncedIds
    }

    /**
     * Sincronização Híbrida: envia apenas os quadrinhos marcados com `isDirty = true`.
     */
    suspend fun syncDirtyComicsBatch(userId: String, localComics: List<ComicEntry>): Set<String> {
        val dirtyItems = localComics.filter { it.isDirty }
        if (dirtyItems.isEmpty()) return emptySet()
        return syncAllComicsBatch(userId, dirtyItems)
    }

    private suspend fun awaitAccountWrite(userId: String, payload: Any, start: () -> com.google.android.gms.tasks.Task<Void>) {
        kotlinx.coroutines.withTimeout(3500L) { accountWrites.execute(db to userId,payload,start) }
    }

    companion object {
        const val FIRESTORE_OFFLINE_CACHE_BUDGET_BYTES = 10L * 1024L * 1024L
        private val accountWrites=FirestoreWriteGate()
        private val configuredDatabases = java.util.Collections.synchronizedSet(HashSet<Int>())

        /**
         * Resolve conflitos entre a lista local (`SafeStorage`) e a lista remota (`Firestore`)
         * mantendo sempre o registro com o maior `updatedAt` (timestamp).
         * Quando `isPartialRemoteWindow = true` (ex.: consulta paginada de 50 itens ou listener
         * com `limit = 100`), itens locais já limpos (`!local.isDirty`) que estão fora da janela
         * remota são preservados em `mergedList`, mas NÃO são marcados como `isDirty = true` nem
         * reenviados em loop para o Firestore.
         * Retorna `Pair(mergedList, dirtyWinnersToPushToCloud)`.
         */
        fun mergeLocalAndRemoteComics(
            localComics: List<ComicEntry>,
            remoteComics: List<ComicEntry>,
            isPartialRemoteWindow: Boolean = false
        ): Pair<List<ComicEntry>, List<ComicEntry>> {
            if (remoteComics.isEmpty()) {
                val dirtyOrAll = localComics.filter { it.isDirty }.ifEmpty {
                    if (isPartialRemoteWindow) emptyList() else localComics
                }
                return localComics to dirtyOrAll
            }

            val remoteById = LinkedHashMap<String, ComicEntry>()
            val remoteByExtId = HashMap<String, ComicEntry>()
            for (remote in remoteComics) {
                val cleanRemote = remote.copy(isDirty = false)
                remoteById[cleanRemote.id] = cleanRemote
                if (cleanRemote.externalId.isNotBlank()) {
                    remoteByExtId[cleanRemote.externalId] = cleanRemote
                }
            }

            val mergedById = LinkedHashMap<String, ComicEntry>(remoteById)
            val dirtyWinners = mutableListOf<ComicEntry>()

            for (local in localComics) {
                val safeId = local.id.replace("/", "_")
                val remoteMatch = remoteById[local.id]
                    ?: remoteById[safeId]
                    ?: (if (local.externalId.isNotBlank()) remoteByExtId[local.externalId] else null)

                if (remoteMatch == null) {
                    if (isPartialRemoteWindow && !local.isDirty) {
                        // A janela remota é parcial (ex.: top 50 ou top 100) e este item local já foi
                        // sincronizado antes: preserva na estante sem forçar re-upload em loop.
                        mergedById[local.id] = local
                    } else {
                        // Item existe apenas localmente (ou foi editado offline com isDirty = true):
                        // preserva integralmente no merge e agenda upload para o Firestore
                        val localToSync = if (local.isDirty) local else local.copy(isDirty = true)
                        mergedById[local.id] = localToSync
                        dirtyWinners.add(localToSync)
                    }
                } else {
                    // Conflito entre versão local e versão remota: vence o maior updatedAt
                    val localWins = local.isDirty && local.updatedAt >= remoteMatch.updatedAt
                    val localNewerTimestamp = local.updatedAt > remoteMatch.updatedAt && local.updatedAt > 0L
                    if (localWins || localNewerTimestamp) {
                        val winner = local.copy(
                            id = remoteMatch.id,
                            titlePtBr = local.titlePtBr.ifBlank { remoteMatch.titlePtBr },
                            synopsisPtBr = local.synopsisPtBr.ifBlank { remoteMatch.synopsisPtBr }
                        )
                        mergedById[remoteMatch.id] = winner
                        if (local.isDirty) {
                            dirtyWinners.add(winner)
                        }
                    } else {
                        // Remoto vence (maior updatedAt ou local não está sujo), preservando tradução local se já existir
                        val mergedRemote = remoteMatch.copy(
                            titlePtBr = remoteMatch.titlePtBr.ifBlank { local.titlePtBr },
                            synopsisPtBr = remoteMatch.synopsisPtBr.ifBlank { local.synopsisPtBr },
                            synopsis = remoteMatch.synopsisPtBr.ifBlank {
                                local.synopsisPtBr.ifBlank { remoteMatch.synopsis }
                            },
                            isDirty = false
                        )
                        mergedById[remoteMatch.id] = mergedRemote
                    }
                }
            }

            val mergedList = mergedById.values.sortedByDescending { it.updatedAt }
            return mergedList to dirtyWinners
        }

        /**
         * Resolve conflito de perfil entre `SafeStorage` e `Firestore` pelo maior `updatedAt`.
         * Retorna `Pair(resolvedProfile, shouldPushLocalToCloud)`.
         */
        fun mergeLocalAndRemoteProfile(
            localProfile: ReaderProfile,
            remoteProfile: CloudUserProfile
        ): Pair<ReaderProfile, Boolean> {
            val remoteMapped = remoteProfile.toReaderProfile(localProfile.avatarUrl)
            val localWins = (localProfile.isDirty && localProfile.updatedAt >= remoteMapped.updatedAt) ||
                (localProfile.updatedAt > remoteMapped.updatedAt && localProfile.updatedAt > 0L)
            return if (localWins) {
                val mergedWinner = localProfile.copy(
                    avatarUrl = localProfile.avatarUrl.ifBlank { remoteMapped.avatarUrl }
                )
                mergedWinner to localProfile.isDirty
            } else {
                remoteMapped.copy(isDirty = false) to false
            }
        }
    }
}


