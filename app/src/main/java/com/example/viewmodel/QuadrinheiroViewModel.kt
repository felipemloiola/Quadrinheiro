package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.CloudAuthUser
import com.example.data.CuratedCatalog
import com.example.data.MultiApiSearchEngine
import com.example.data.PortugueseComicTranslator
import com.example.data.QuadrinheiroAuthManager
import com.example.data.QuadrinheiroCloudRepository
import com.example.data.QuadrinheiroSyncWorker
import com.example.data.SafeStorage
import com.example.model.ApiDiagnosticsReport
import com.example.model.AppSettings
import com.example.model.AuthFeedbackBanner
import com.example.model.AuthFeedbackType
import com.example.model.AuthMode
import com.example.model.AuthScreenUiState
import com.example.model.AuthUiState
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.model.UserProfile
import com.example.model.UsernameAvailabilityStatus
import com.example.model.UsernameValidator
import com.example.model.UsernameVerificationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@androidx.compose.runtime.Immutable
data class ToastNotice(
    val id: Long,
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@androidx.compose.runtime.Immutable
data class ManualComicForm(
    val title: String = "",
    val authors: String = "",
    val publisher: String = "",
    val genre: String = "DC / Vertigo",
    val format: String = "Encadernado (TPB)",
    val releaseYear: Int = 2026,
    val pageCount: Int = 160,
    val coverUrl: String = "",
    val status: ReadingStatus = ReadingStatus.READ,
    val rating: Float = 0f,
    val tags: List<String> = emptyList(),
    val review: String = ""
)

class QuadrinheiroViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val storage = SafeStorage(appContext)
    private val searchEngine by lazy(LazyThreadSafetyMode.PUBLICATION) { MultiApiSearchEngine() }
    private val authManager = QuadrinheiroAuthManager(appContext, storage)
    private val cloudRepo: QuadrinheiroCloudRepository? by lazy {
        runCatching { QuadrinheiroCloudRepository(appContext) }.getOrNull()
    }
    private val initial = storage.loadState()

    // Estado de Autenticação inicializado de forma SÍNCRONA no boot (Zero Flash de Tela)
    private val _authUiState = MutableStateFlow<AuthUiState>(authManager.getInitialAuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(
        authManager.getInitialUserProfile(initial.profile.name)
    )
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    private val _isEnteringApp = MutableStateFlow(false)
    val isEnteringApp: StateFlow<Boolean> = _isEnteringApp.asStateFlow()

    // Pair(mensagemPtBr, isError)
    private val _authFeedback = MutableStateFlow<Pair<String, Boolean>?>(null)
    val authFeedback: StateFlow<Pair<String, Boolean>?> = _authFeedback.asStateFlow()

    // Permite que um usuário no Modo Convidado abra a AuthScreen pela tela de Ajustes para vincular/criar conta
    private val _showAuthModalFromSettings = MutableStateFlow(false)
    val showAuthModalFromSettings: StateFlow<Boolean> = _showAuthModalFromSettings.asStateFlow()

    private val _authModalInitialMode = MutableStateFlow(AuthMode.LOGIN)
    val authModalInitialMode: StateFlow<AuthMode> = _authModalInitialMode.asStateFlow()

    // Estado consolidado da tela de Autenticação para consumo declarativo em peça única
    val authScreenUiState: StateFlow<AuthScreenUiState> = combine(
        _authUiState,
        _currentUserProfile,
        _isAuthLoading,
        _authFeedback,
        combine(_showAuthModalFromSettings, _authModalInitialMode) { show, mode -> show to mode }
    ) { session, profile, loading, feedback, (showModal, modalMode) ->
        AuthScreenUiState(
            sessionState = session,
            userProfile = profile,
            isLoading = loading,
            banner = feedback?.let { (msg, isErr) ->
                AuthFeedbackBanner(
                    message = msg,
                    type = if (isErr) AuthFeedbackType.ERROR else AuthFeedbackType.SUCCESS
                )
            },
            showLinkModalFromSettings = showModal,
            modalInitialMode = modalMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AuthScreenUiState(
            sessionState = _authUiState.value,
            userProfile = _currentUserProfile.value,
            isLoading = false,
            banner = null,
            showLinkModalFromSettings = false,
            modalInitialMode = AuthMode.LOGIN
        )
    )

    // Cloud Authentication & Firestore Sync state
    private val _cloudUser = MutableStateFlow<CloudAuthUser?>(
        _currentUserProfile.value?.takeIf { !it.isGuest && it.provider == "google" }?.let {
            CloudAuthUser(
                uid = it.uid,
                displayName = it.displayName,
                email = it.email,
                photoUrl = it.photoUrl
            )
        }
    )
    val cloudUser: StateFlow<CloudAuthUser?> = _cloudUser.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private var cloudProfileJob: Job? = null
    private var cloudComicsJob: Job? = null

    // 'home' | 'shelf' | 'diary' | 'settings' | 'search'
    // SavedStateHandle preserves tab & sub-page when minimizing/backgrounding, resetting to 'home' only if the app is closed.
    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Memoriza o último índice válido da cápsula (0..3)
    private val _lastCapsuleIndex = MutableStateFlow(0)
    val lastCapsuleIndex: StateFlow<Int> = _lastCapsuleIndex.asStateFlow()

    // Sidebar state (NavigationSplitView / sidebarAdaptable)
    private val _isSidebarOpen = MutableStateFlow(false)
    val isSidebarOpen: StateFlow<Boolean> = _isSidebarOpen.asStateFlow()

    // Sub-menus dentro de Ajustes (null = raiz)
    private val _settingsSubPage = MutableStateFlow<String?>(savedStateHandle.get<String>("settingsSubPage"))
    val settingsSubPage: StateFlow<String?> = _settingsSubPage.asStateFlow()

    private val _settingsParentPage = MutableStateFlow<String?>(savedStateHandle.get<String>("settingsParentPage"))
    val settingsParentPage: StateFlow<String?> = _settingsParentPage.asStateFlow()

    // Scroll memory map: routeKey -> Pair(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    val scrollMemory = mutableMapOf<String, Pair<Int, Int>>()

    private val _comics = MutableStateFlow(initial.comics)
    val comics: StateFlow<List<ComicEntry>> = _comics.asStateFlow()

    private val _profile = MutableStateFlow(initial.profile)
    val profile: StateFlow<ReaderProfile> = _profile.asStateFlow()

    private val _appSettings = MutableStateFlow(initial.settings)
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private var baselineHandleKey: String = UsernameValidator.normalizeHandleKey(initial.profile.handle)

    private val _usernameVerification = MutableStateFlow(
        UsernameVerificationState(
            inputHandle = UsernameValidator.formatDisplayHandle(initial.profile.handle),
            normalizedHandle = UsernameValidator.normalizeHandleKey(initial.profile.handle),
            status = UsernameAvailabilityStatus.IDLE,
            message = ""
        )
    )
    val usernameVerification: StateFlow<UsernameVerificationState> = _usernameVerification.asStateFlow()
    private var handleCheckJob: Job? = null
    private var persistJob: Job? = null
    private var cloudProfileSaveJob: Job? = null

    // Shelf (Biblioteca) states: "shelf" (Estante inicial) vs "diary" (Diário)
    private val _shelfSection = MutableStateFlow(savedStateHandle.get<String>("shelfSection") ?: "shelf")
    val shelfSection: StateFlow<String> = _shelfSection.asStateFlow()

    // Trigger para animação suave de rolagem até o topo ao clicar novamente no ícone da aba ativa na raiz
    private val _scrollToTopTrigger = MutableStateFlow(0L)
    val scrollToTopTrigger: StateFlow<Long> = _scrollToTopTrigger.asStateFlow()

    // Trigger para focar e abrir a caixa de pesquisa ao clicar no ícone Buscar já estando no topo da aba Buscar
    private val _searchFocusTrigger = MutableStateFlow(0L)
    val searchFocusTrigger: StateFlow<Long> = _searchFocusTrigger.asStateFlow()

    private val _shelfStatus = MutableStateFlow(savedStateHandle.get<String>("shelfStatus") ?: "ALL")
    val shelfStatus: StateFlow<String> = _shelfStatus.asStateFlow()

    private val _shelfGenre = MutableStateFlow(savedStateHandle.get<String>("shelfGenre") ?: "Todos")
    val shelfGenre: StateFlow<String> = _shelfGenre.asStateFlow()

    private val _shelfLayout = MutableStateFlow(
        savedStateHandle.get<String>("shelfLayout") ?: initial.settings.shelfLayout.ifBlank { "grid" }
    )
    val shelfLayout: StateFlow<String> = _shelfLayout.asStateFlow()

    private val _shelfSort = MutableStateFlow(initial.settings.defaultShelfSort)
    val shelfSort: StateFlow<String> = _shelfSort.asStateFlow()

    private val _shelfFilterQuery = MutableStateFlow("")
    val shelfFilterQuery: StateFlow<String> = _shelfFilterQuery.asStateFlow()

    // Search states
    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>("searchQuery") ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _remoteResults = MutableStateFlow<List<ComicEntry>>(emptyList())
    val remoteResults: StateFlow<List<ComicEntry>> = _remoteResults.asStateFlow()

    private val _isSearchingApi = MutableStateFlow(false)
    val isSearchingApi: StateFlow<Boolean> = _isSearchingApi.asStateFlow()

    private val _searchSourceFilter = MutableStateFlow("ALL") // "ALL" | "ONLINE" | "LOCAL"
    val searchSourceFilter: StateFlow<String> = _searchSourceFilter.asStateFlow()

    // API Diagnostics
    private val _apiDiagnostics = MutableStateFlow<ApiDiagnosticsReport?>(null)
    val apiDiagnostics: StateFlow<ApiDiagnosticsReport?> = _apiDiagnostics.asStateFlow()

    private val _isTestingApis = MutableStateFlow(false)
    val isTestingApis: StateFlow<Boolean> = _isTestingApis.asStateFlow()

    // Modals (Half Sheet Inset -> Full Sheet Opaque)
    private val _selectedComic = MutableStateFlow<ComicEntry?>(null)
    val selectedComic: StateFlow<ComicEntry?> = _selectedComic.asStateFlow()

    // Modal de Informações e Sinopse (ao clicar em uma HQ na aba Buscar)
    private val _infoModalComic = MutableStateFlow<ComicEntry?>(null)
    val infoModalComic: StateFlow<ComicEntry?> = _infoModalComic.asStateFlow()

    private val _showManualModal = MutableStateFlow(false)
    val showManualModal: StateFlow<Boolean> = _showManualModal.asStateFlow()

    private val _isSheetExpanded = MutableStateFlow(false)
    val isSheetExpanded: StateFlow<Boolean> = _isSheetExpanded.asStateFlow()

    private val _manualForm = MutableStateFlow(ManualComicForm())
    val manualForm: StateFlow<ManualComicForm> = _manualForm.asStateFlow()

    // Active reading comic ID for the bottom accessory bar (when >1 comic is READING)
    private val _activeReadingComicId = MutableStateFlow<String?>(
        initial.profile.activeReadingComicId.takeIf { it.isNotBlank() }
            ?: savedStateHandle.get<String>("activeReadingComicId")
    )
    val activeReadingComicId: StateFlow<String?> = _activeReadingComicId.asStateFlow()

    private val _toast = MutableStateFlow<ToastNotice?>(null)
    val toast: StateFlow<ToastNotice?> = _toast.asStateFlow()

    private val _onlineSequencePool = MutableStateFlow<List<ComicEntry>>(emptyList())
    val onlineSequencePool: StateFlow<List<ComicEntry>> = _onlineSequencePool.asStateFlow()

    // Fluxo com debounce(800L) para evitar múltiplas escritas no Firestore/SafeStorage enquanto o usuário arrasta o LiquidSlider
    private val _debouncedReadingPageUpdates = MutableSharedFlow<ComicEntry>(extraBufferCapacity = 32)

    private var searchJob: Job? = null
    private var sequenceJob: Job? = null
    private var toastJob: Job? = null

    fun selectActiveReadingComic(id: String) {
        if (id.isBlank()) return
        _activeReadingComicId.value = id
        savedStateHandle["activeReadingComicId"] = id
        if (_profile.value.activeReadingComicId != id) {
            updateProfile { it.copy(activeReadingComicId = id) }
        } else {
            persistAll()
        }
    }

    fun cycleActiveReadingComic(delta: Int = 1) {
        val readingList = _comics.value.filter { it.status == ReadingStatus.READING }
        if (readingList.size <= 1) return
        val currentIdx = readingList.indexOfFirst {
            it.id == _activeReadingComicId.value ||
                (it.externalId.isNotBlank() && it.externalId == _activeReadingComicId.value)
        }.let { if (it < 0) 0 else it }
        val nextIdx = (currentIdx + delta + readingList.size) % readingList.size
        val nextId = readingList[nextIdx].id
        _activeReadingComicId.value = nextId
        savedStateHandle["activeReadingComicId"] = nextId
        updateProfile { it.copy(activeReadingComicId = nextId) }
    }

    init {
        PortugueseComicTranslator.onTranslationPersisted = { translatedComic ->
            persistTranslatedComic(translatedComic)
        }

        @OptIn(FlowPreview::class)
        viewModelScope.launch(Dispatchers.IO) {
            _debouncedReadingPageUpdates
                .debounce(800L)
                .collectLatest { target ->
                    val ownerId = activeOwnerUid()
                    storage.saveState(_comics.value, _profile.value, _appSettings.value, ownerId)
                    val uid = activeCloudUid()
                    val repo = cloudRepo
                    if (uid != null && repo != null) {
                        runCatching { repo.saveComic(uid, target) }.onSuccess {
                            markComicsCleanInMemoryAndStorage(setOf(target.id, target.externalId), target.updatedAt)
                        }.onFailure {
                            QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
                        }
                    } else if (!isCurrentSessionGuest()) {
                        QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Defer non-critical background work until after initial Activity SurfaceSyncGroup transaction completes
            delay(600)
            // Se houver itens sujos pendentes de sessão offline anterior e não estiver em modo convidado, agenda o WorkManager
            if (!storage.isGuestMode() && (initial.comics.any { it.isDirty } || initial.profile.isDirty)) {
                QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
            }
            cloudRepo?.let { repo ->
                val activeFbUser = repo.currentAuthUser()
                val initialSession = storage.loadAuthSession()
                if (activeFbUser != null &&
                    !_isLoggingOut.value &&
                    !storage.isGuestMode() &&
                    initialSession != null &&
                    !initialSession.isGuest &&
                    initialSession.uid == activeFbUser.uid
                ) {
                    _cloudUser.value = activeFbUser
                    val syncedProfile = UserProfile(
                        uid = activeFbUser.uid,
                        displayName = _profile.value.name.ifBlank { activeFbUser.displayName ?: "Leitor" },
                        email = activeFbUser.email,
                        photoUrl = _profile.value.avatarUrl.ifBlank { activeFbUser.photoUrl.orEmpty() }.ifBlank { null },
                        isGuest = false,
                        provider = "google"
                    )
                    storage.saveAuthSession(syncedProfile)
                    _currentUserProfile.value = syncedProfile
                    _authUiState.value = AuthUiState.Authenticated
                }
                repo.observeAuthState().collectLatest { user ->
                    if (_isLoggingOut.value) return@collectLatest
                    val existingSession = storage.loadAuthSession()
                    val isValidAuthenticatedUser = user != null &&
                        !storage.isGuestMode() &&
                        _authUiState.value != AuthUiState.Guest &&
                        existingSession != null &&
                        !existingSession.isGuest &&
                        existingSession.uid == user.uid

                    if (isValidAuthenticatedUser && user != null) {
                        _cloudUser.value = user
                        val syncedProfile = UserProfile(
                            uid = user.uid,
                            displayName = _profile.value.name.ifBlank { user.displayName ?: "Leitor" },
                            email = user.email,
                            photoUrl = _profile.value.avatarUrl.ifBlank { user.photoUrl.orEmpty() }.ifBlank { null },
                            isGuest = false,
                            provider = "google"
                        )
                        storage.saveAuthSession(syncedProfile)
                        _currentUserProfile.value = syncedProfile
                        _authUiState.value = AuthUiState.Authenticated
                        startOrStopCloudListeners(user)
                    } else {
                        _cloudUser.value = null
                        startOrStopCloudListeners(null)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Let the first frame and SurfaceSyncGroup complete without CPU/IO contention
            delay(1200)

            // Auto-heal any existing user comics that still have missing or slow OpenLibrary cover URLs
            // INDEXED_CATALOG_HEALING_V15: build lookup tables once, not one catalog scan per HQ.
            val healingOwner = activeOwnerUid()
            val healingComics = _comics.value
            val catalogIndex = com.example.data.CatalogMatchIndex(CuratedCatalog.curatedComicsCatalog)
            var updatedAny = false
            val healedList = healingComics.map { comic ->
                val isCurated = comic.id.startsWith("cat-") || comic.id.startsWith("init-") || comic.externalId.startsWith("cat-")
                val catalogHit = catalogIndex.findMatch(comic)
                val needsCoverUpdate = comic.coverUrl.isBlank() ||
                    comic.coverUrl.contains("covers.openlibrary.org") ||
                    (catalogHit != null && isCurated && comic.coverUrl != catalogHit.coverUrl) ||
                    comic.coverUrl.contains("BMY1_cover.jpg") ||
                    comic.coverUrl.contains("BMDKR_cover.jpg")

                val withCover = if (needsCoverUpdate && catalogHit != null) {
                    updatedAny = true
                    comic.copy(coverUrl = catalogHit.coverUrl)
                } else if (comic.coverUrl.isBlank() || comic.coverUrl.contains("covers.openlibrary.org")) {
                    val newUrl = searchEngine.resolveBestCoverUrl(comic.title, comic.authors)
                    if (newUrl.isNotBlank() && newUrl != comic.coverUrl) {
                        updatedAny = true
                        comic.copy(coverUrl = newUrl)
                    } else {
                        comic
                    }
                } else {
                    comic
                }
                val localized = PortugueseComicTranslator.localizeComicMetadataOnly(withCover)
                if (localized != withCover) {
                    updatedAny = true
                }
                localized
            }.toMutableList()

            // Ensure starter collection includes Matt Fraction's Batman #1 & #2 sequence if user has the starter set
            if (healedList.any { it.id == "init-watchmen" } && healedList.none { it.id == "init-batman-fraction-1" }) {
                val starterBatman = CuratedCatalog.initialUserCollection.filter {
                    it.id == "init-batman-fraction-1" || it.id == "init-batman-fraction-2"
                }
                if (starterBatman.isNotEmpty()) {
                    healedList.addAll(0, starterBatman)
                    updatedAny = true
                }
            }

            if (updatedAny && activeOwnerUid() == healingOwner && (_comics.value === healingComics)) {
                // Do not overwrite edits or another account while cover resolution was running.
                _comics.value = healedList
                persistAll(healingOwner)
            }
            // Prefetch leve e prioritário: apenas 4 capas do acervo ativo (em leitura ou topo)
            val initialCoversToWarm = _comics.value.filter { it.status == ReadingStatus.READING }.ifEmpty { _comics.value.take(4) }
            prefetchCovers(initialCoversToWarm.take(4))

            // Catálogo curado pré-aquecido apenas sob demanda e com limite de 4 itens após 3 segundos
            delay(3000)
            if (_comics.value.isEmpty()) {
                prefetchCovers(CuratedCatalog.curatedComicsCatalog.take(4))
            }
            searchEngine.prewarmPopularSearches()
            if (_searchQuery.value.isNotBlank()) {
                triggerOnlineSearchIfNeeded()
            }
        }
    }

    private fun markComicsCleanInMemoryAndStorage(syncedIds: Set<String>, syncedAtMillis: Long = System.currentTimeMillis()) {
        val validIds = syncedIds.filter { it.isNotBlank() }.toSet()
        if (validIds.isEmpty()) return
        var changed = false
        val cleaned = _comics.value.map { c ->
            if (c.isDirty && (validIds.contains(c.id) || validIds.contains(c.externalId))) {
                changed = true
                c.copy(
                    isDirty = false,
                    updatedAt = if (c.updatedAt > 0L) c.updatedAt else syncedAtMillis
                )
            } else {
                c
            }
        }
        if (changed) {
            _comics.value = cleaned
        }
        storage.markComicsSynced(validIds, syncedAtMillis)
    }

    private fun markProfileCleanInMemoryAndStorage(syncedAtMillis: Long = System.currentTimeMillis()) {
        val cur = _profile.value
        if (cur.isDirty) {
            _profile.value = cur.copy(
                isDirty = false,
                updatedAt = if (cur.updatedAt > 0L) cur.updatedAt else syncedAtMillis
            )
        }
        storage.markProfileSynced(syncedAtMillis)
    }

    private fun startOrStopCloudListeners(user: CloudAuthUser?) {
        cloudProfileJob?.cancel()
        cloudComicsJob?.cancel()
        val repo = cloudRepo ?: return
        if (user == null || isCurrentSessionGuest()) return

        cloudProfileJob = viewModelScope.launch(Dispatchers.IO) {
            repo.observeUserProfile(user.uid)
                .catch { }
                .collectLatest { remoteProfile ->
                    if (isCurrentSessionGuest() || activeCloudUid() != user.uid) return@collectLatest
                    if (remoteProfile != null && remoteProfile.ownerId == user.uid) {
                        val (resolvedProfile, shouldPushLocal) = QuadrinheiroCloudRepository.mergeLocalAndRemoteProfile(
                            localProfile = _profile.value,
                            remoteProfile = remoteProfile
                        )
                        val remoteSettings = remoteProfile.preferences?.toAppSettings()
                        var changedAny = false
                        if (resolvedProfile != _profile.value) {
                            _profile.value = resolvedProfile
                            changedAny = true
                        }
                        if (resolvedProfile.activeReadingComicId.isNotBlank() &&
                            _activeReadingComicId.value != resolvedProfile.activeReadingComicId
                        ) {
                            _activeReadingComicId.value = resolvedProfile.activeReadingComicId
                            savedStateHandle["activeReadingComicId"] = resolvedProfile.activeReadingComicId
                            changedAny = true
                        }
                        if (!shouldPushLocal && remoteSettings != null && remoteSettings != _appSettings.value) {
                            _appSettings.value = remoteSettings
                            val nextLayout = remoteSettings.shelfLayout.ifBlank { "grid" }
                            if (_shelfLayout.value != nextLayout) {
                                _shelfLayout.value = nextLayout
                                savedStateHandle["shelfLayout"] = nextLayout
                            }
                            if (_shelfSort.value != remoteSettings.defaultShelfSort) {
                                _shelfSort.value = remoteSettings.defaultShelfSort
                            }
                            changedAny = true
                        }
                        val normKey = UsernameValidator.normalizeHandleKey(resolvedProfile.handle)
                        if (UsernameValidator.isValidHandleKey(normKey)) {
                            storage.reserveLocalHandle(normKey, user.uid)
                            if (_usernameVerification.value.status == UsernameAvailabilityStatus.IDLE) {
                                baselineHandleKey = normKey
                                _usernameVerification.value = _usernameVerification.value.copy(
                                    inputHandle = UsernameValidator.formatDisplayHandle(resolvedProfile.handle),
                                    normalizedHandle = normKey
                                )
                            }
                        }
                        val curSession = _currentUserProfile.value
                        if (curSession != null && !curSession.isGuest && curSession.uid == user.uid) {
                            val updatedSession = curSession.copy(
                                displayName = resolvedProfile.name.ifBlank { curSession.displayName },
                                photoUrl = resolvedProfile.avatarUrl.ifBlank { curSession.photoUrl.orEmpty() }.ifBlank { null }
                            )
                            if (updatedSession != curSession) {
                                _currentUserProfile.value = updatedSession
                                storage.saveAuthSession(updatedSession)
                            }
                        }
                        if (changedAny) {
                            persistAll(user.uid)
                        }
                        if (shouldPushLocal) {
                            runCatching { repo.saveUserProfile(user.uid, resolvedProfile, _appSettings.value) }.onSuccess {
                                markProfileCleanInMemoryAndStorage(resolvedProfile.updatedAt)
                            }
                        }
                    } else {
                        val localProf = _profile.value
                        runCatching { repo.saveUserProfile(user.uid, localProf, _appSettings.value) }.onSuccess {
                            markProfileCleanInMemoryAndStorage(localProf.updatedAt)
                        }
                    }
                }
        }

        cloudComicsJob = viewModelScope.launch(Dispatchers.IO) {
            repo.observeUserComics(user.uid)
                .catch { }
                .collectLatest { remoteComics ->
                    if (isCurrentSessionGuest() || activeCloudUid() != user.uid) return@collectLatest
                    if (remoteComics.isNotEmpty()) {
                        PortugueseComicTranslator.seedPersistedTranslations(
                            remoteComics,
                            storage.loadTranslationCache()
                        )
                        val (mergedList, dirtyWinners) = QuadrinheiroCloudRepository.mergeLocalAndRemoteComics(
                            localComics = _comics.value,
                            remoteComics = remoteComics
                        )
                        if (mergedList != _comics.value) {
                            _comics.value = mergedList
                            persistAll(user.uid)
                        }
                        if (dirtyWinners.isNotEmpty()) {
                            runCatching {
                                repo.syncDirtyComicsBatch(user.uid, dirtyWinners)
                            }.onSuccess { syncedIds ->
                                markComicsCleanInMemoryAndStorage(syncedIds)
                            }
                        }
                    } else if (_comics.value.isNotEmpty()) {
                        val dirtyOnly = _comics.value.filter { it.isDirty }
                        val batchToPush = dirtyOnly.ifEmpty { _comics.value }
                        runCatching {
                            repo.syncAllComicsBatch(user.uid, batchToPush)
                        }.onSuccess { syncedIds ->
                            markComicsCleanInMemoryAndStorage(syncedIds)
                        }
                    }
                }
        }
    }

    private fun persistTranslatedComic(translated: ComicEntry) {
        val sPt = translated.synopsisPtBr.trim()
        val tPt = translated.titlePtBr.trim().ifBlank { translated.title.trim() }
        if (sPt.isEmpty() && tPt.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Persiste imediatamente no SafeStorage
            storage.saveComicTranslation(translated)

            // 2. Atualiza o cache em memória do MultiApiSearchEngine e a lista atual de resultados
            searchEngine.updateCachedComicTranslation(translated)
            val currentRemote = _remoteResults.value
            if (currentRemote.isNotEmpty()) {
                var remoteUpdated = false
                val nextRemote = currentRemote.map { item ->
                    if ((translated.id.isNotBlank() && item.id == translated.id) ||
                        (translated.externalId.isNotBlank() && item.externalId == translated.externalId)
                    ) {
                        if (item.synopsisPtBr != sPt || item.titlePtBr != tPt) {
                            remoteUpdated = true
                            item.copy(
                                title = tPt.ifBlank { item.title },
                                titlePtBr = tPt.ifBlank { item.titlePtBr },
                                synopsis = sPt.ifBlank { item.synopsis },
                                synopsisPtBr = sPt.ifBlank { item.synopsisPtBr }
                            )
                        } else {
                            item
                        }
                    } else {
                        item
                    }
                }
                if (remoteUpdated) {
                    _remoteResults.value = nextRemote
                }
            }

            // 3. Se o quadrinho pertence à estante do usuário, atualiza o objeto em _comics, SafeStorage e Firestore
            var matchedShelfComic: ComicEntry? = null
            val nowMs = System.currentTimeMillis()
            val currentShelf = _comics.value
            val nextShelf = currentShelf.map { c ->
                val isSame = (translated.id.isNotBlank() && c.id == translated.id) ||
                    (translated.externalId.isNotBlank() && c.externalId == translated.externalId)
                if (isSame && (c.synopsisPtBr != sPt || c.titlePtBr != tPt)) {
                    c.copy(
                        title = tPt.ifBlank { c.title },
                        titlePtBr = tPt.ifBlank { c.titlePtBr },
                        synopsis = sPt.ifBlank { c.synopsis },
                        synopsisPtBr = sPt.ifBlank { c.synopsisPtBr },
                        updatedAt = nowMs,
                        isDirty = true
                    ).also { matchedShelfComic = it }
                } else {
                    c
                }
            }
            val updatedShelfItem = matchedShelfComic
            if (updatedShelfItem != null) {
                _comics.value = nextShelf
                persistAll()
                val uid = activeCloudUid()
                val repo = cloudRepo
                if (uid != null && repo != null) {
                    runCatching { repo.saveComic(uid, updatedShelfItem) }.onSuccess {
                        markComicsCleanInMemoryAndStorage(
                            setOf(updatedShelfItem.id, updatedShelfItem.externalId),
                            nowMs
                        )
                    }
                }
            }
        }
    }

    private fun isCurrentSessionGuest(): Boolean {
        return _authUiState.value == AuthUiState.Guest ||
            _currentUserProfile.value?.isGuest == true ||
            storage.isGuestMode()
    }

    private fun activeCloudUid(): String? {
        if (isCurrentSessionGuest() || _authUiState.value != AuthUiState.Authenticated) {
            return null
        }
        val sessionUid = _currentUserProfile.value?.takeIf { !it.isGuest }?.uid
        val cloudUid = _cloudUser.value?.uid
        return if (cloudUid != null && (sessionUid == null || sessionUid == cloudUid)) {
            cloudUid
        } else {
            null
        }
    }

    private fun activeOwnerUid(): String {
        if (isCurrentSessionGuest()) return "guest_local"
        return _cloudUser.value?.uid
            ?: _currentUserProfile.value?.takeIf { !it.isGuest }?.uid
            ?: "local_default"
    }

    fun resetToReadingHome() {
        _activeTab.value = "home"
        savedStateHandle["activeTab"] = "home"
        _lastCapsuleIndex.value = 0
        savedStateHandle["lastCapsuleIndex"] = 0
        _shelfSection.value = "shelf"
        savedStateHandle["shelfSection"] = "shelf"
        _settingsSubPage.value = null
        _settingsParentPage.value = null
        savedStateHandle["settingsSubPage"] = null
        savedStateHandle["settingsParentPage"] = null
        _isSidebarOpen.value = false
    }

    private fun startSessionEntryTransition() {
        _isEnteringApp.value = true
        viewModelScope.launch {
            delay(700)
            _isEnteringApp.value = false
        }
    }

    fun clearAuthFeedback() {
        _authFeedback.value = null
    }

    fun openAuthModalForLinking(mode: AuthMode = AuthMode.LOGIN) {
        _authFeedback.value = null
        _authModalInitialMode.value = mode
        _showAuthModalFromSettings.value = true
    }

    fun dismissAuthModalForLinking() {
        _authFeedback.value = null
        _showAuthModalFromSettings.value = false
    }

    /**
     * Restaura exclusivamente o estado salvo da conta autenticada (`loadAccountState(uid)`),
     * mantendo a conta de Convidado e cada conta de usuário 100% independentes e separadas.
     */
    private fun applyAuthenticatedSessionAndMergeLocalData(
        userProfile: UserProfile,
        wasGuestBefore: Boolean = false
    ) {
        val startedFromSettingsModal = _showAuthModalFromSettings.value
        handleCheckJob?.cancel()
        cloudProfileJob?.cancel()
        cloudComicsJob?.cancel()
        cloudProfileSaveJob?.cancel()
        persistJob?.cancel()

        // Se estava em modo convidado antes de entrar na conta, salva o estado do convidado na chave isolada dele
        if (wasGuestBefore || isCurrentSessionGuest()) {
            val guestComicsSnap = _comics.value
            val guestProfileSnap = _profile.value
            val guestSettingsSnap = _appSettings.value
            storage.saveState(guestComicsSnap, guestProfileSnap, guestSettingsSnap, "guest_local")
        }

        val savedAccount = storage.loadAccountState(userProfile.uid)

        if (savedAccount != null) {
            val restoredProfile = savedAccount.profile.copy(
                name = savedAccount.profile.name.ifBlank { userProfile.displayName.ifBlank { "Leitor" } },
                avatarUrl = savedAccount.profile.avatarUrl.ifBlank { userProfile.photoUrl.orEmpty() }
            )
            _profile.value = restoredProfile
            _appSettings.value = savedAccount.settings
            val restoredLayout = savedAccount.settings.shelfLayout.ifBlank { "grid" }
            _shelfLayout.value = restoredLayout
            savedStateHandle["shelfLayout"] = restoredLayout
            _shelfSort.value = savedAccount.settings.defaultShelfSort
            _comics.value = savedAccount.comics

            val restoredActiveComicId = restoredProfile.activeReadingComicId.takeIf { it.isNotBlank() }
                ?: _comics.value.firstOrNull { it.status == ReadingStatus.READING }?.id
            _activeReadingComicId.value = restoredActiveComicId
            savedStateHandle["activeReadingComicId"] = restoredActiveComicId
        } else {
            val baseHandle = (userProfile.email?.substringBefore("@") ?: userProfile.displayName)
                .lowercase()
                .replace(Regex("[^a-z0-9_]"), "")
                .take(18)
            val defaultHandle = if (baseHandle.length >= 3) {
                "@$baseHandle"
            } else {
                "@leitor_${userProfile.uid.takeLast(4).lowercase()}"
            }
            val cleanAccountProfile = ReaderProfile(
                name = userProfile.displayName.ifBlank { "Leitor" },
                handle = defaultHandle,
                avatarUrl = userProfile.photoUrl.orEmpty(),
                activeReadingComicId = "",
                // Usa updatedAt = 0L para que os dados salvos no Firestore desta conta tenham prioridade total
                updatedAt = 0L,
                isDirty = false
            )
            val cleanSettings = AppSettings()
            _profile.value = cleanAccountProfile
            _appSettings.value = cleanSettings
            _shelfLayout.value = cleanSettings.shelfLayout.ifBlank { "grid" }
            savedStateHandle["shelfLayout"] = _shelfLayout.value
            _shelfSort.value = cleanSettings.defaultShelfSort
            _comics.value = emptyList()
            _activeReadingComicId.value = null
            savedStateHandle["activeReadingComicId"] = null
        }

        val resolvedSessionProfile = userProfile.copy(
            displayName = _profile.value.name.ifBlank { userProfile.displayName },
            photoUrl = _profile.value.avatarUrl.ifBlank { userProfile.photoUrl.orEmpty() }.ifBlank { null },
            isGuest = false
        )
        storage.saveAuthSession(resolvedSessionProfile)
        _currentUserProfile.value = resolvedSessionProfile
        _authUiState.value = AuthUiState.Authenticated
        _showAuthModalFromSettings.value = false
        _authFeedback.value = null

        if (!startedFromSettingsModal) {
            resetToReadingHome()
            startSessionEntryTransition()
        }

        resetUsernameVerification(cancelPendingJob = true)
        persistAll(userProfile.uid)

        if (userProfile.provider == "google") {
            val cloudAuthUser = CloudAuthUser(
                uid = userProfile.uid,
                displayName = resolvedSessionProfile.displayName,
                email = resolvedSessionProfile.email,
                photoUrl = resolvedSessionProfile.photoUrl
            )
            _cloudUser.value = cloudAuthUser
            startOrStopCloudListeners(cloudAuthUser)
        } else {
            _cloudUser.value = null
            startOrStopCloudListeners(null)
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        if (_isAuthLoading.value) return
        val wasGuest = isCurrentSessionGuest()
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authFeedback.value = null
            val result = authManager.signInWithEmail(email, pass)
            _isAuthLoading.value = false
            result.onSuccess { userProfile ->
                applyAuthenticatedSessionAndMergeLocalData(userProfile, wasGuest)
                showNotice("Bem-vindo de volta, ${userProfile.displayName}!")
            }.onFailure { err ->
                _authFeedback.value = (err.message ?: "Não foi possível entrar com E-mail e Senha.") to true
            }
        }
    }

    fun registerWithEmail(name: String, email: String, pass: String) {
        if (_isAuthLoading.value) return
        val wasGuest = isCurrentSessionGuest()
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authFeedback.value = null
            val result = authManager.signUpWithEmail(name, email, pass)
            _isAuthLoading.value = false
            result.onSuccess { userProfile ->
                applyAuthenticatedSessionAndMergeLocalData(userProfile, wasGuest)
                showNotice("Conta criada com sucesso! Bem-vindo, ${userProfile.displayName}!")
            }.onFailure { err ->
                _authFeedback.value = (err.message ?: "Não foi possível criar sua conta agora.") to true
            }
        }
    }

    fun loginWithGoogle(activityContext: Context) {
        if (_isAuthLoading.value) return
        val wasGuest = isCurrentSessionGuest()
        viewModelScope.launch {
            _isAuthLoading.value = true
            _isSyncingCloud.value = true
            _authFeedback.value = null
            val result = authManager.signInWithGoogle(activityContext)
            _isAuthLoading.value = false
            _isSyncingCloud.value = false
            result.onSuccess { userProfile ->
                applyAuthenticatedSessionAndMergeLocalData(userProfile, wasGuest)
                showNotice("Conectado como ${userProfile.displayName}")
            }.onFailure { err ->
                if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    val msg = authManager.translateAuthErrorToPtBr(err)
                    _authFeedback.value = msg to true
                    if (_authUiState.value != AuthUiState.Unauthenticated && !_showAuthModalFromSettings.value) {
                        showNotice(msg)
                    }
                }
            }
        }
    }

    fun enterAsGuest() {
        handleCheckJob?.cancel()
        cloudProfileJob?.cancel()
        cloudComicsJob?.cancel()
        cloudProfileSaveJob?.cancel()
        persistJob?.cancel()

        // Garante que nenhuma sessão Google anterior continue ativa em memória durante o Modo Convidado
        _cloudUser.value = null
        startOrStopCloudListeners(null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { cloudRepo?.signOut(appContext) }
        }

        val guestProfile = authManager.continueAsGuest("Leitor Convidado")
        _currentUserProfile.value = guestProfile
        _authUiState.value = AuthUiState.Guest
        _showAuthModalFromSettings.value = false
        _authFeedback.value = null

        // Carrega exclusivamente o ambiente isolado do Convidado (sem tocar na conta Google)
        val guestState = storage.loadGuestState()
        _comics.value = guestState.comics
        _profile.value = guestState.profile.copy(name = "Leitor Convidado", handle = "@convidado", isDirty = false)
        _appSettings.value = guestState.settings
        val guestLayout = guestState.settings.shelfLayout.ifBlank { "grid" }
        _shelfLayout.value = guestLayout
        savedStateHandle["shelfLayout"] = guestLayout
        _shelfSort.value = guestState.settings.defaultShelfSort
        _activeReadingComicId.value = guestState.profile.activeReadingComicId.takeIf { it.isNotBlank() }
            ?: guestState.comics.firstOrNull { it.status == ReadingStatus.READING }?.id
        savedStateHandle["activeReadingComicId"] = _activeReadingComicId.value

        resetUsernameVerification(cancelPendingJob = true)
        persistAll("guest_local")
        resetToReadingHome()
        startSessionEntryTransition()
        showNotice("Modo Convidado ativo")
    }

    fun resetPassword(email: String) {
        if (_isAuthLoading.value) return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authFeedback.value = null
            val result = authManager.sendPasswordReset(email)
            _isAuthLoading.value = false
            result.onSuccess { successMsg ->
                _authFeedback.value = successMsg to false
            }.onFailure { err ->
                _authFeedback.value = (err.message ?: "Informe um e-mail válido para redefinir a senha.") to true
            }
        }
    }

    fun logout(activityContext: Context? = null) {
        _isLoggingOut.value = true
        handleCheckJob?.cancel()
        cloudProfileJob?.cancel()
        cloudComicsJob?.cancel()
        cloudProfileSaveJob?.cancel()
        persistJob?.cancel()

        val wasGuestLogout = isCurrentSessionGuest()
        val outgoingUid = if (wasGuestLogout) {
            "guest_local"
        } else {
            _cloudUser.value?.uid ?: _currentUserProfile.value?.takeIf { !it.isGuest }?.uid
        }
        val outgoingCloudUid = if (wasGuestLogout) null else _cloudUser.value?.uid
        val comicsSnapshot = _comics.value
        val profileSnapshot = _profile.value.copy(
            activeReadingComicId = _activeReadingComicId.value?.takeIf { it.isNotBlank() }
                ?: _profile.value.activeReadingComicId
        )
        val settingsSnapshot = _appSettings.value.copy(
            shelfLayout = _shelfLayout.value.ifBlank { _appSettings.value.shelfLayout }
        )
        _profile.value = profileSnapshot
        _appSettings.value = settingsSnapshot

        // Encerra a sessão imediatamente na interface para que o usuário nunca fique preso
        storage.clearAuthSession()
        _cloudUser.value = null
        _currentUserProfile.value = null
        startOrStopCloudListeners(null)
        _showAuthModalFromSettings.value = false
        _authFeedback.value = null
        _isSyncingCloud.value = false
        _isAuthLoading.value = false
        resetUsernameVerification(cancelPendingJob = true)
        resetToReadingHome()
        _authUiState.value = AuthUiState.Unauthenticated
        _isLoggingOut.value = false
        showNotice("Sessão encerrada com segurança")

        // Persiste o estado final da conta que estava ativa (Convidado ou Conta) sem misturar
        viewModelScope.launch(Dispatchers.IO) {
            if (outgoingUid != null) {
                storage.saveState(comicsSnapshot, profileSnapshot, settingsSnapshot, outgoingUid)
            }
            val repo = cloudRepo
            if (outgoingCloudUid != null && repo != null) {
                kotlinx.coroutines.withTimeoutOrNull(1500L) {
                    runCatching {
                        repo.saveUserProfile(outgoingCloudUid, profileSnapshot, settingsSnapshot)
                    }
                    val dirtyComics = comicsSnapshot.filter { it.isDirty }
                    if (dirtyComics.isNotEmpty()) {
                        runCatching {
                            repo.syncDirtyComicsBatch(outgoingCloudUid, dirtyComics)
                        }
                    }
                }
            }
            runCatching {
                authManager.signOut(activityContext)
            }
        }
    }

    fun trySilentGoogleSignIn(activityContext: Context) {
        val repo = cloudRepo ?: return
        if (_cloudUser.value != null || storage.isGuestMode()) return
        viewModelScope.launch {
            repo.trySilentGoogleSignIn(activityContext).onSuccess { user ->
                if (user != null) {
                    _cloudUser.value = user
                }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        loginWithGoogle(activityContext)
    }

    fun signOutFromCloud(activityContext: Context) {
        logout(activityContext)
    }

    fun syncNowWithCloud() {
        val repo = cloudRepo ?: return
        val user = _cloudUser.value ?: run {
            if (_currentUserProfile.value?.provider == "email") {
                showNotice("Acervo salvo no dispositivo. Conecte o Google para sincronizar entre aparelhos.")
            }
            return
        }
        if (_isSyncingCloud.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingCloud.value = true
            val dirtyComics = _comics.value.filter { it.isDirty }
            runCatching {
                repo.saveUserProfile(user.uid, _profile.value, _appSettings.value)
                markProfileCleanInMemoryAndStorage()
                val syncedIds = if (dirtyComics.isNotEmpty()) {
                    repo.syncDirtyComicsBatch(user.uid, _comics.value)
                } else {
                    repo.syncAllComicsBatch(user.uid, _comics.value)
                }
                markComicsCleanInMemoryAndStorage(syncedIds)
            }.onSuccess {
                val countMsg = if (dirtyComics.isNotEmpty()) {
                    "${dirtyComics.size} alterações pendentes sincronizadas"
                } else {
                    "${_comics.value.size} obras sincronizadas"
                }
                showNotice("Conta sincronizada na nuvem ($countMsg)")
            }.onFailure {
                showNotice("Falha ao sincronizar com a nuvem")
            }
            _isSyncingCloud.value = false
        }
    }

    private fun prefetchCovers(items: List<ComicEntry>) {
        if (items.isEmpty()) return
        val loader = appContext.imageLoader
        val isHd = _appSettings.value.hdCovers
        val targetW = if (isHd) 200 else 140
        val targetH = (targetW * 1.5f).toInt()
        items.forEach { item ->
            val url = item.coverUrl.trim().replace("http://", "https://")
            if (url.startsWith("https://")) {
                val req = ImageRequest.Builder(appContext)
                    .data(url)
                    .size(targetW, targetH)
                    .precision(coil.size.Precision.INEXACT)
                    .allowRgb565(true)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                loader.enqueue(req)
            }
        }
    }

    private fun persistAll(ownerUid: String? = null) {
        // PERSIST_SNAPSHOT_V13: bind the data snapshot to its owner before the debounce.
        val resolvedUid = ownerUid ?: activeOwnerUid()
        val comicsSnapshot = _comics.value
        val profileSnapshot = _profile.value
        val settingsSnapshot = _appSettings.value
        persistJob?.cancel()
        persistJob = viewModelScope.launch(Dispatchers.IO) {
            delay(80)
            storage.saveState(comicsSnapshot, profileSnapshot, settingsSnapshot, resolvedUid)
        }
    }

    private fun persistAllImmediateAsync(ownerUid: String? = null) {
        val resolvedUid = ownerUid ?: activeOwnerUid()
        val comicsSnapshot = _comics.value
        val profileSnapshot = _profile.value
        val settingsSnapshot = _appSettings.value
        viewModelScope.launch(Dispatchers.IO) {
            storage.saveState(comicsSnapshot, profileSnapshot, settingsSnapshot, resolvedUid)
        }
    }

    fun getCurrentRouteKey(
        tab: String = _activeTab.value,
        sub: String? = _settingsSubPage.value,
        section: String = _shelfSection.value
    ): String {
        return when (tab) {
            "settings" -> "settings:${sub ?: "root"}"
            "shelf" -> "tab:shelf:$section"
            else -> "tab:$tab"
        }
    }

    fun requestScrollToTop() {
        _scrollToTopTrigger.value = System.nanoTime()
    }

    fun requestSearchFocus() {
        _searchFocusTrigger.value = System.nanoTime()
    }

    fun switchMainTab(nextTab: String) {
        if (nextTab == "diary") {
            if (_activeTab.value == "shelf" && _shelfSection.value == "diary") {
                requestScrollToTop()
                return
            }
            scrollMemory["tab:shelf:diary"] = 0 to 0
            _shelfSection.value = "diary"
            savedStateHandle["shelfSection"] = "diary"
            _activeTab.value = "shelf"
            savedStateHandle["activeTab"] = "shelf"
            _lastCapsuleIndex.value = 1
            savedStateHandle["lastCapsuleIndex"] = 1
            return
        }

        if (nextTab == "settings" && _activeTab.value == "settings") {
            if (_settingsSubPage.value != null) {
                _settingsSubPage.value = null
                _settingsParentPage.value = null
                savedStateHandle["settingsSubPage"] = null
                savedStateHandle["settingsParentPage"] = null
            } else {
                requestScrollToTop()
            }
            return
        }

        if (nextTab == "shelf" && (_activeTab.value == "shelf" || _activeTab.value == "diary")) {
            if (_shelfSection.value != "shelf" || _activeTab.value == "diary") {
                _shelfSection.value = "shelf"
                savedStateHandle["shelfSection"] = "shelf"
                _activeTab.value = "shelf"
                savedStateHandle["activeTab"] = "shelf"
            } else {
                requestScrollToTop()
            }
            return
        }

        if (nextTab == _activeTab.value) {
            requestScrollToTop()
            return
        }

        val capsuleIdx = listOf("home", "shelf", "diary", "settings").indexOf(nextTab)
        if (capsuleIdx >= 0) {
            _lastCapsuleIndex.value = capsuleIdx
            savedStateHandle["lastCapsuleIndex"] = capsuleIdx
        }
        _activeTab.value = nextTab
        savedStateHandle["activeTab"] = nextTab
    }

    fun openSettingsCategory(page: String, parent: String? = null) {
        if (page == "profile") {
            resetUsernameVerification(cancelPendingJob = false)
        }
        scrollMemory["settings:$page"] = 0 to 0
        _settingsParentPage.value = parent
        _settingsSubPage.value = page
        savedStateHandle["settingsParentPage"] = parent
        savedStateHandle["settingsSubPage"] = page
    }

    fun goBackSettings() {
        resetUsernameVerification(cancelPendingJob = false)
        val parent = _settingsParentPage.value
        if (parent != null) {
            _settingsSubPage.value = parent
            _settingsParentPage.value = null
            savedStateHandle["settingsSubPage"] = parent
            savedStateHandle["settingsParentPage"] = null
        } else {
            _settingsSubPage.value = null
            savedStateHandle["settingsSubPage"] = null
        }
    }

    fun setSidebarOpen(open: Boolean) {
        _isSidebarOpen.value = open
    }

    fun resetUsernameVerification(cancelPendingJob: Boolean = false) {
        if (cancelPendingJob) {
            handleCheckJob?.cancel()
        }
        val currentHandle = _profile.value.handle
        val norm = UsernameValidator.normalizeHandleKey(currentHandle)
        baselineHandleKey = norm
        _usernameVerification.value = UsernameVerificationState(
            inputHandle = UsernameValidator.formatDisplayHandle(currentHandle),
            normalizedHandle = norm,
            status = UsernameAvailabilityStatus.IDLE,
            message = ""
        )
    }

    fun onHandleInputChanged(rawInput: String) {
        handleCheckJob?.cancel()
        val key = UsernameValidator.normalizeHandleKey(rawInput)
        val formatted = UsernameValidator.formatDisplayHandle(rawInput)
        val activeUid = activeOwnerUid()
        val currentSavedKey = UsernameValidator.normalizeHandleKey(_profile.value.handle)

        // Se o usuário não alterou ou digitou novamente o mesmo @usuario que já estava salvo,
        // mantém o campo normal (IDLE) sem disparar ou exibir aviso de verificação.
        if (key == baselineHandleKey) {
            if (currentSavedKey != baselineHandleKey && UsernameValidator.isValidHandleKey(key)) {
                val nowMs = System.currentTimeMillis()
                val restoredProfile = _profile.value.copy(
                    handle = formatted,
                    updatedAt = nowMs,
                    isDirty = true
                )
                _profile.value = restoredProfile
                viewModelScope.launch(Dispatchers.IO) {
                    storage.reserveLocalHandle(key, activeUid, previousKey = currentSavedKey)
                    storage.saveState(_comics.value, restoredProfile, _appSettings.value, activeUid)
                    val repo = cloudRepo
                    val cloudUid = activeCloudUid()
                    if (repo != null && cloudUid != null && repo.currentAuthUser()?.uid == cloudUid) {
                        kotlinx.coroutines.withTimeoutOrNull(3500L) {
                            runCatching { repo.saveUserProfile(cloudUid, restoredProfile, _appSettings.value, strictHandleReservation = true) }.onSuccess {
                                markProfileCleanInMemoryAndStorage(nowMs)
                            }
                        }
                    }
                }
            }
            _usernameVerification.value = UsernameVerificationState(
                inputHandle = formatted,
                normalizedHandle = key,
                status = UsernameAvailabilityStatus.IDLE,
                message = ""
            )
            return
        }

        val validationError = UsernameValidator.validateHandle(rawInput, isGuest = isCurrentSessionGuest())
        if (validationError != null) {
            _usernameVerification.value = UsernameVerificationState(
                inputHandle = formatted,
                normalizedHandle = key,
                status = UsernameAvailabilityStatus.INVALID,
                message = validationError
            )
            return
        }

        val previousHandle = _profile.value.handle
        val previousKey = currentSavedKey
        _usernameVerification.value = UsernameVerificationState(
            inputHandle = formatted,
            normalizedHandle = key,
            status = UsernameAvailabilityStatus.CHECKING,
            message = "Verificando disponibilidade de $formatted..."
        )

        handleCheckJob = viewModelScope.launch(Dispatchers.IO) {
            delay(180)
            // 1. Verifica registro local de handles em background (sem travar a digitação)
            val localOwner = storage.getLocalHandleOwner(key)
            if (localOwner != null && localOwner != activeUid) {
                _usernameVerification.value = UsernameVerificationState(
                    inputHandle = formatted,
                    normalizedHandle = key,
                    status = UsernameAvailabilityStatus.TAKEN,
                    message = "Indisponível: $formatted já está em uso por outro leitor"
                )
                return@launch
            }

            // 2. Reserva e salva localmente em background imediatamente após o debounce
            val nowMs = System.currentTimeMillis()
            storage.reserveLocalHandle(key, activeUid, previousKey = previousKey)
            val updatedProfile = _profile.value.copy(
                handle = formatted,
                updatedAt = nowMs,
                isDirty = true
            )
            _profile.value = updatedProfile
            storage.saveState(_comics.value, updatedProfile, _appSettings.value, activeUid)

            // 3. Verifica disponibilidade na nuvem com timeout curto (nunca trava em "Verificando")
            val repo = cloudRepo
            val cloudUid = activeCloudUid()
            val hasCloudAuth = repo != null && cloudUid != null && repo.currentAuthUser()?.uid == cloudUid
            val verification = if (hasCloudAuth) {
                kotlinx.coroutines.withTimeoutOrNull(1500L) {
                    repo!!.checkUsernameAvailability(formatted, cloudUid!!)
                } ?: UsernameVerificationState(
                    inputHandle = formatted,
                    normalizedHandle = key,
                    status = UsernameAvailabilityStatus.AVAILABLE_OWNED,
                    message = "Disponível e salvo na sua conta ($formatted)"
                )
            } else {
                UsernameVerificationState(
                    inputHandle = formatted,
                    normalizedHandle = key,
                    status = UsernameAvailabilityStatus.AVAILABLE_OWNED,
                    message = "Disponível e salvo na sua conta ($formatted)"
                )
            }

            if (verification.isNegative) {
                storage.reserveLocalHandle(previousKey, activeUid, previousKey = key)
                val reverted = _profile.value.copy(handle = previousHandle, updatedAt = System.currentTimeMillis())
                _profile.value = reverted
                storage.saveState(_comics.value, reverted, _appSettings.value, activeUid)
                _usernameVerification.value = verification
                return@launch
            }

            // 4. Conclui a verificação visual imediatamente como Disponível!
            _usernameVerification.value = UsernameVerificationState(
                inputHandle = formatted,
                normalizedHandle = key,
                status = UsernameAvailabilityStatus.AVAILABLE_OWNED,
                message = "Disponível e salvo na sua conta ($formatted)"
            )

            // 5. Sincroniza com o Firestore em background sem prender o estado "Verificando"
            if (hasCloudAuth) {
                viewModelScope.launch(Dispatchers.IO) {
                    val result = kotlinx.coroutines.withTimeoutOrNull(3500L) {
                        runCatching {
                            repo!!.saveUserProfile(cloudUid!!, updatedProfile, _appSettings.value, strictHandleReservation = true)
                        }
                    }
                    if (result != null) {
                        result.onSuccess {
                            markProfileCleanInMemoryAndStorage(nowMs)
                        }.onFailure { err ->
                            val msg = err.message.orEmpty()
                            if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                                storage.reserveLocalHandle(previousKey, activeUid, previousKey = key)
                                val reverted = _profile.value.copy(handle = previousHandle, updatedAt = System.currentTimeMillis())
                                _profile.value = reverted
                                storage.saveState(_comics.value, reverted, _appSettings.value, activeUid)
                                _usernameVerification.value = UsernameVerificationState(
                                    inputHandle = formatted,
                                    normalizedHandle = key,
                                    status = UsernameAvailabilityStatus.TAKEN,
                                    message = "Indisponível: $formatted já está em uso por outro leitor"
                                )
                            } else {
                                QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
                            }
                        }
                    } else {
                        QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
                    }
                }
            }
        }
    }

    fun updateProfile(transform: (ReaderProfile) -> ReaderProfile) {
        val nowMs = System.currentTimeMillis()
        val candidate = transform(_profile.value)
        val candidateKey = UsernameValidator.normalizeHandleKey(candidate.handle)
        val activeUid = activeOwnerUid()
        val currentKey = UsernameValidator.normalizeHandleKey(_profile.value.handle)

        // Se o handle mudou via updateProfile, verifica unicidade antes de aplicar
        if (candidateKey != currentKey) {
            onHandleInputChanged(candidate.handle)
            val withoutHandleChange = candidate.copy(
                handle = _profile.value.handle,
                updatedAt = nowMs,
                isDirty = true
            )
            if (withoutHandleChange != _profile.value.copy(updatedAt = nowMs, isDirty = true)) {
                _profile.value = withoutHandleChange
                persistAll(activeUid)
            }
            return
        }

        val next = candidate.copy(
            handle = UsernameValidator.formatDisplayHandle(candidate.handle),
            updatedAt = nowMs,
            isDirty = true
        )
        _profile.value = next
        if (next.activeReadingComicId.isNotBlank() && _activeReadingComicId.value != next.activeReadingComicId) {
            _activeReadingComicId.value = next.activeReadingComicId
            savedStateHandle["activeReadingComicId"] = next.activeReadingComicId
        }
        val curSession = _currentUserProfile.value
        if (curSession != null && !curSession.isGuest) {
            val updatedSession = curSession.copy(
                displayName = next.name.ifBlank { curSession.displayName },
                photoUrl = next.avatarUrl.ifBlank { curSession.photoUrl.orEmpty() }.ifBlank { null }
            )
            if (updatedSession != curSession) {
                _currentUserProfile.value = updatedSession
                storage.saveAuthSession(updatedSession)
            }
        }
        persistAll(activeUid)
        scheduleCloudProfileSave(nowMs)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val nowMs = System.currentTimeMillis()
        val nextSettings = transform(_appSettings.value)
        _appSettings.value = nextSettings
        if (nextSettings.shelfLayout.isNotBlank() && _shelfLayout.value != nextSettings.shelfLayout) {
            _shelfLayout.value = nextSettings.shelfLayout
            savedStateHandle["shelfLayout"] = nextSettings.shelfLayout
        }
        if (_shelfSort.value != nextSettings.defaultShelfSort) {
            _shelfSort.value = nextSettings.defaultShelfSort
        }
        val nextProfile = _profile.value.copy(
            updatedAt = nowMs,
            isDirty = true
        )
        _profile.value = nextProfile
        persistAll()
        scheduleCloudProfileSave(nowMs)
    }

    private fun scheduleCloudProfileSave(updatedAtMs: Long) {
        cloudProfileSaveJob?.cancel()
        if (isCurrentSessionGuest()) return
        val uid = activeCloudUid()
        val repo = cloudRepo
        if (uid != null && repo != null) {
            cloudProfileSaveJob = viewModelScope.launch(Dispatchers.IO) {
                delay(350)
                if (isCurrentSessionGuest() || repo.currentAuthUser()?.uid != uid) return@launch
                val profileSnap = _profile.value
                val settingsSnap = _appSettings.value
                val activeKey = UsernameValidator.normalizeHandleKey(profileSnap.handle)
                if (UsernameValidator.isValidHandleKey(activeKey)) {
                    storage.reserveLocalHandle(activeKey, uid)
                }
                val res = kotlinx.coroutines.withTimeoutOrNull(3500L) {
                    runCatching { repo.saveUserProfile(uid, profileSnap, settingsSnap) }
                }
                if (res != null && res.isSuccess) {
                    markProfileCleanInMemoryAndStorage(updatedAtMs)
                } else {
                    QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
                }
            }
        } else {
            QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
        }
    }

    fun setShelfSection(section: String) {
        if (section != _shelfSection.value && section == "diary") {
            scrollMemory["tab:shelf:diary"] = 0 to 0
        }
        _shelfSection.value = section
        savedStateHandle["shelfSection"] = section
    }

    fun setShelfStatus(status: String) {
        _shelfStatus.value = status
        savedStateHandle["shelfStatus"] = status
    }

    fun setShelfGenre(genre: String) {
        _shelfGenre.value = genre
        savedStateHandle["shelfGenre"] = genre
    }

    fun toggleShelfLayout() {
        val next = when (_shelfLayout.value) {
            "grid" -> "list"
            "list" -> "sequences"
            else -> "grid"
        }
        setShelfLayout(next)
    }

    fun setShelfLayout(layout: String) {
        _shelfLayout.value = layout
        savedStateHandle["shelfLayout"] = layout
        if (_appSettings.value.shelfLayout != layout) {
            updateSettings { it.copy(shelfLayout = layout) }
        }
    }

    fun setShelfSort(sort: String) {
        _shelfSort.value = sort
        if (_appSettings.value.defaultShelfSort != sort) {
            updateSettings { it.copy(defaultShelfSort = sort) }
        }
    }

    fun setShelfFilterQuery(q: String) {
        _shelfFilterQuery.value = q
    }

    fun setSearchSourceFilter(f: String) {
        _searchSourceFilter.value = f
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        savedStateHandle["searchQuery"] = q
        val isQuickPill = CuratedCatalog.quickSearchSuggestions.any { it.equals(q.trim(), ignoreCase = true) }
        triggerOnlineSearchIfNeeded(immediate = isQuickPill || q.isBlank())
    }

    fun triggerImmediateSearch() {
        triggerOnlineSearchIfNeeded(immediate = true)
    }

    private fun triggerOnlineSearchIfNeeded(immediate: Boolean = false) {
        searchJob?.cancel()
        val q = _searchQuery.value.trim()
        if (q.isEmpty()) {
            _remoteResults.value = emptyList()
            _isSearchingApi.value = false
            return
        }
        val s = _appSettings.value
        val countryCode = if (s.searchLanguage == "PT") "BR" else "US"
        val cacheKey = "${s.searchProvider}:$countryCode:hd${s.hdCovers}:${q.lowercase()}"
        val cached = searchEngine.getCached(cacheKey)
        if (cached != null) {
            _remoteResults.value = cached
            _isSearchingApi.value = false
            prefetchCovers(cached.take(12))
            return
        }

        searchJob = viewModelScope.launch {
            _isSearchingApi.value = true
            if (!immediate) {
                delay(260)
            }
            searchEngine.searchOnlineProgressive(
                query = q,
                provider = s.searchProvider,
                searchLanguage = s.searchLanguage,
                hdCovers = s.hdCovers
            ).collectLatest { list ->
                _remoteResults.value = list
                if (list.isNotEmpty()) {
                    _isSearchingApi.value = false
                    prefetchCovers(list.take(12))
                }
            }
            _isSearchingApi.value = false
        }
    }

    fun runOnlineApiDiagnostics() {
        if (_isTestingApis.value) return
        viewModelScope.launch {
            _isTestingApis.value = true
            _apiDiagnostics.value = searchEngine.runOnlineApiDiagnostics()
            _isTestingApis.value = false
            showNotice("Diagnóstico de APIs concluído")
        }
    }

    fun showNotice(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        toastJob?.cancel()
        val notice = ToastNotice(System.currentTimeMillis(), message, actionLabel, onAction)
        _toast.value = notice
        toastJob = viewModelScope.launch {
            delay(3200)
            if (_toast.value?.id == notice.id) {
                _toast.value = null
            }
        }
    }

    fun clearToast() {
        _toast.value = null
    }

    private fun loadSequenceIssuesForComic(comic: ComicEntry?) {
        sequenceJob?.cancel()
        if (comic == null) {
            _onlineSequencePool.value = emptyList()
            return
        }
        sequenceJob = viewModelScope.launch(Dispatchers.IO) {
            val fetched = searchEngine.fetchSequenceIssuesOnline(comic, _appSettings.value.hdCovers)
            _onlineSequencePool.value = fetched
            if (fetched.isNotEmpty()) {
                prefetchCovers(fetched.take(8))
            }
        }
    }

    fun openInfoSynopsisModal(comic: ComicEntry?) {
        _selectedComic.value = null
        if (comic == null) {
            _infoModalComic.value = null
            loadSequenceIssuesForComic(null)
            _isSheetExpanded.value = false
            return
        }
        val localized = PortugueseComicTranslator.localizeComic(comic, allowNetworkTranslation = false)
        _infoModalComic.value = localized
        loadSequenceIssuesForComic(localized)
        if (localized.synopsisPtBr.isBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val enriched = PortugueseComicTranslator.localizeComic(comic, allowNetworkTranslation = true)
                persistTranslatedComic(enriched)
                if (_infoModalComic.value?.id == comic.id) {
                    _infoModalComic.value = enriched
                }
            }
        } else {
            persistTranslatedComic(localized)
        }
    }

    fun selectComicForSheet(comic: ComicEntry?) {
        _infoModalComic.value = null
        if (comic == null) {
            _selectedComic.value = null
            loadSequenceIssuesForComic(null)
            _isSheetExpanded.value = false
            return
        }
        val localized = PortugueseComicTranslator.localizeComic(comic, allowNetworkTranslation = false)
        _selectedComic.value = localized
        loadSequenceIssuesForComic(localized)
        if (localized.synopsisPtBr.isBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val enriched = PortugueseComicTranslator.localizeComic(comic, allowNetworkTranslation = true)
                persistTranslatedComic(enriched)
                if (_selectedComic.value?.id == comic.id) {
                    _selectedComic.value = enriched
                }
            }
        } else {
            persistTranslatedComic(localized)
        }
    }

    fun updateSelectedComic(transform: (ComicEntry) -> ComicEntry) {
        val current = _selectedComic.value ?: return
        _selectedComic.value = transform(current)
    }

    fun setSheetExpanded(expanded: Boolean) {
        _isSheetExpanded.value = expanded
    }

    fun setShowManualModal(show: Boolean) {
        _showManualModal.value = show
        if (!show) {
            _isSheetExpanded.value = false
        }
    }

    fun updateManualForm(transform: (ManualComicForm) -> ManualComicForm) {
        _manualForm.value = transform(_manualForm.value)
    }

    /**
     * Opens the cataloging sheet for a comic:
     * - If chosenStatus == WANT_TO_READ, saves directly to Queue (Fila) without asking for rating, review, or date!
     * - Otherwise opens the sheet with rating = 0 (unless already rated in collection).
     */
    fun openCatalogSheetForComic(itemData: ComicEntry, chosenStatus: ReadingStatus) {
        _infoModalComic.value = null
        val existing = _comics.value.firstOrNull { c ->
            (itemData.id.isNotBlank() && c.id == itemData.id) ||
                (itemData.externalId.isNotBlank() && c.externalId == itemData.externalId)
        }
        val base = existing ?: itemData
        val pageCount = base.pageCount.coerceAtLeast(1)
        val defaultFmt = _appSettings.value.defaultFormat

        if (chosenStatus == ReadingStatus.WANT_TO_READ) {
            val uid = base.id.ifBlank { base.externalId.ifBlank { "hq-${System.currentTimeMillis()}" } }
            val extId = base.externalId.ifBlank { uid }
            saveComic(
                base.copy(
                    id = uid,
                    externalId = extId,
                    status = ReadingStatus.WANT_TO_READ,
                    pageCount = pageCount,
                    currentPage = 0,
                    format = base.format.ifBlank { defaultFmt },
                    rating = 0f,
                    favorite = false,
                    rereadCount = 0,
                    review = "",
                    readDate = ""
                )
            )
            return
        }

        val currentPage = when (chosenStatus) {
            ReadingStatus.READ -> pageCount
            ReadingStatus.READING -> if (base.currentPage in 1..pageCount) base.currentPage else minOf(pageCount, 24)
            ReadingStatus.WANT_TO_READ -> 0
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val uid = base.id.ifBlank { base.externalId.ifBlank { "hq-${System.currentTimeMillis()}" } }
        val extId = base.externalId.ifBlank { uid }

        _selectedComic.value = base.copy(
            id = uid,
            externalId = extId,
            status = chosenStatus,
            pageCount = pageCount,
            currentPage = currentPage,
            format = base.format.ifBlank { defaultFmt },
            rating = existing?.rating ?: 0f, // 0 stars by default for new additions!
            tags = existing?.tags ?: emptyList(),
            review = existing?.review ?: "",
            favorite = existing?.favorite ?: false,
            rereadCount = existing?.rereadCount ?: 0,
            readDate = existing?.readDate?.ifBlank { if (_appSettings.value.autoFillReadDate) today else "" }
                ?: if (_appSettings.value.autoFillReadDate) today else ""
        )
    }

    fun saveComic(itemData: ComicEntry) {
        val uniqueId = itemData.id.ifBlank {
            itemData.externalId.ifBlank { "hq-${System.currentTimeMillis()}" }
        }
        val extId = itemData.externalId.ifBlank { uniqueId }
        val pageCount = itemData.pageCount.coerceAtLeast(1)
        val status = itemData.status
        val isWantToRead = status == ReadingStatus.WANT_TO_READ
        val isRead = status == ReadingStatus.READ
        val currentPage = when (status) {
            ReadingStatus.READ -> pageCount
            ReadingStatus.WANT_TO_READ -> 0
            ReadingStatus.READING -> itemData.currentPage.coerceIn(0, pageCount)
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val normTitle = PortugueseComicTranslator.translateTitleToPtBr(itemData.title.trim().ifBlank { "Sem Título" })
        val normPub = PortugueseComicTranslator.normalizePublisher(
            itemData.publisher.trim().ifBlank { "Editora" },
            normTitle,
            itemData.authors,
            itemData.genre
        )
        val normGenre = PortugueseComicTranslator.normalizeGenre(
            itemData.genre.ifBlank { "Graphic Novel" },
            normTitle,
            itemData.authors,
            normPub
        )

        val resolvedSynopsisPtBr = itemData.synopsisPtBr.ifBlank {
            val cached = PortugueseComicTranslator.getCachedTranslationForComicId(uniqueId)
                ?: PortugueseComicTranslator.getCachedTranslationForComicId(extId)
            cached?.synopsisPtBr?.ifBlank {
                if (itemData.synopsis.isNotBlank() && PortugueseComicTranslator.isPortugueseText(itemData.synopsis)) {
                    itemData.synopsis
                } else {
                    ""
                }
            } ?: if (itemData.synopsis.isNotBlank() && PortugueseComicTranslator.isPortugueseText(itemData.synopsis)) {
                itemData.synopsis
            } else {
                ""
            }
        }

        val nowMs = System.currentTimeMillis()
        val entry = itemData.copy(
            id = uniqueId,
            externalId = extId,
            title = normTitle,
            titlePtBr = normTitle,
            authors = itemData.authors.trim().ifBlank { "Autor Desconhecido" },
            publisher = normPub,
            genre = normGenre,
            format = itemData.format.ifBlank { _appSettings.value.defaultFormat },
            releaseYear = if (itemData.releaseYear > 0) itemData.releaseYear else 2026,
            pageCount = pageCount,
            currentPage = currentPage,
            status = status,
            favorite = if (isRead) itemData.favorite else false,
            rereadCount = if (isRead) itemData.rereadCount else 0,
            rating = if (isRead) itemData.rating else 0f,
            tags = itemData.tags,
            review = if (isRead) itemData.review else "",
            readDate = if (isRead) itemData.readDate.ifBlank { today } else "",
            addedAt = itemData.addedAt.ifBlank { nowIso },
            synopsis = resolvedSynopsisPtBr.ifBlank { itemData.synopsis },
            synopsisPtBr = resolvedSynopsisPtBr,
            updatedAt = nowMs,
            isDirty = true
        )

        val currentList = _comics.value.toMutableList()
        val idx = currentList.indexOfFirst { c ->
            c.id == entry.id || (c.externalId.isNotBlank() && c.externalId == entry.externalId)
        }
        if (idx >= 0) {
            currentList[idx] = entry
        } else {
            currentList.add(0, entry)
        }
        _comics.value = currentList
        persistAll()

        val uid = activeCloudUid()
        val repo = cloudRepo
        if (uid != null && repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repo.saveComic(uid, entry) }.onSuccess {
                    markComicsCleanInMemoryAndStorage(setOf(entry.id, entry.externalId), nowMs)
                }.onFailure {
                    QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
                }
            }
        } else if (!isCurrentSessionGuest()) {
            QuadrinheiroSyncWorker.enqueueDirtySync(appContext)
        }

        _selectedComic.value = null
        _infoModalComic.value = null
        _showManualModal.value = false
        _isSheetExpanded.value = false

        showNotice(
            message = if (isWantToRead) "\"${entry.title}\" adicionada à sua Fila (Quero Ler)"
            else "\"${entry.title}\" salva na Biblioteca",
            actionLabel = "Ver Biblioteca",
            onAction = {
                if (isWantToRead) {
                    _shelfStatus.value = "WANT_TO_READ"
                    savedStateHandle["shelfStatus"] = "WANT_TO_READ"
                }
                switchMainTab("shelf")
                clearToast()
            }
        )
    }

    fun deleteComic(id: String) {
        _comics.value = _comics.value.filterNot { it.id == id }
        persistAll()
        val uid = activeCloudUid()
        val repo = cloudRepo
        if (uid != null && repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repo.deleteComic(uid, id) }
            }
        }
        _selectedComic.value = null
        _infoModalComic.value = null
        showNotice("Edição removida da Biblioteca")
    }

    fun quickUpdateReadingPage(comicItem: ComicEntry, nextPage: Int) {
        val total = comicItem.pageCount.coerceAtLeast(1)
        val clamped = nextPage.coerceIn(0, total)
        val nextStatus = if (clamped >= total) ReadingStatus.READ else ReadingStatus.READING
        val nowMs = System.currentTimeMillis()
        var updatedComic: ComicEntry? = null
        _comics.value = _comics.value.map { c ->
            if (c.id == comicItem.id) {
                c.copy(
                    currentPage = clamped,
                    status = nextStatus,
                    updatedAt = nowMs,
                    isDirty = true
                ).also { updatedComic = it }
            } else {
                c
            }
        }
        persistAll()
        updatedComic?.let { target ->
            _debouncedReadingPageUpdates.tryEmit(target)
        }
    }

    fun clearAllComics() {
        _comics.value = emptyList()
        persistAll()
        val uid = activeCloudUid()
        val repo = cloudRepo
        if (uid != null && repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repo.deleteAllUserComics(uid) }
            }
        }
        showNotice("Acervo limpo")
    }

    fun resetAppearanceDefaults() {
        updateSettings { prev ->
            prev.copy(
                glassLevel = 26,
                glassVariant = "regular",
                themeId = "midnightSlate",
                colorScheme = "dark",
                edgeRefraction = true,
                reduceTransparency = false,
                increaseContrast = false,
                boldText = false,
                dynamicTypeScale = 100,
                shelfColumns = 3
            )
        }
        showNotice("Aparência original restaurada")
    }

    fun exportBackupJson(): String {
        return storage.buildBackupJsonString(_comics.value, _profile.value, _appSettings.value, pretty = true)
    }

    fun importBackupJson(rawJson: String): Boolean {
        val restored = storage.importBackupJsonString(rawJson)
        return if (restored != null) {
            _comics.value = restored.comics
            _profile.value = restored.profile
            _appSettings.value = restored.settings
            val uid = _cloudUser.value?.uid
            val repo = cloudRepo
            if (uid != null && repo != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        repo.saveUserProfile(uid, restored.profile, restored.settings)
                        repo.syncAllComicsBatch(uid, restored.comics)
                    }
                }
            }
            showNotice("Backup restaurado com sucesso")
            true
        } else {
            showNotice("Arquivo de backup inválido")
            false
        }
    }

    fun buildAndroidZipBytes(): ByteArray {
        return storage.buildStandaloneIOS27ZipBytes(_comics.value, _profile.value, _appSettings.value)
    }
}
