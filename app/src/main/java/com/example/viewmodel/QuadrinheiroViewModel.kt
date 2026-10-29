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
import com.example.data.QuadrinheiroCloudRepository
import com.example.data.SafeStorage
import com.example.model.ApiDiagnosticsReport
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
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
    private val searchEngine = MultiApiSearchEngine()
    private val cloudRepo: QuadrinheiroCloudRepository? by lazy {
        runCatching { QuadrinheiroCloudRepository(appContext) }.getOrNull()
    }
    private val initial = storage.loadState()

    // Cloud Authentication & Firestore Sync state
    private val _cloudUser = MutableStateFlow<CloudAuthUser?>(null)
    val cloudUser: StateFlow<CloudAuthUser?> = _cloudUser.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private var cloudProfileJob: Job? = null
    private var cloudComicsJob: Job? = null

    // 'home' | 'shelf' | 'diary' | 'settings' | 'search'
    // SavedStateHandle preserves tab & sub-page when minimizing/backgrounding, resetting to 'home' only if the app is closed.
    private val _activeTab = MutableStateFlow(savedStateHandle.get<String>("activeTab") ?: "home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Memoriza o último índice válido da cápsula (0..3)
    private val _lastCapsuleIndex = MutableStateFlow(savedStateHandle.get<Int>("lastCapsuleIndex") ?: 0)
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

    // Shelf (Biblioteca) states: "shelf" (Estante inicial) vs "diary" (Diário)
    private val _shelfSection = MutableStateFlow(savedStateHandle.get<String>("shelfSection") ?: "shelf")
    val shelfSection: StateFlow<String> = _shelfSection.asStateFlow()

    // Trigger para animação suave de rolagem até o topo ao clicar novamente no ícone da aba ativa na raiz
    private val _scrollToTopTrigger = MutableStateFlow(0L)
    val scrollToTopTrigger: StateFlow<Long> = _scrollToTopTrigger.asStateFlow()

    private val _shelfStatus = MutableStateFlow(savedStateHandle.get<String>("shelfStatus") ?: "ALL")
    val shelfStatus: StateFlow<String> = _shelfStatus.asStateFlow()

    private val _shelfGenre = MutableStateFlow(savedStateHandle.get<String>("shelfGenre") ?: "Todos")
    val shelfGenre: StateFlow<String> = _shelfGenre.asStateFlow()

    private val _shelfLayout = MutableStateFlow(savedStateHandle.get<String>("shelfLayout") ?: "grid")
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
    private val _activeReadingComicId = MutableStateFlow<String?>(savedStateHandle.get<String>("activeReadingComicId"))
    val activeReadingComicId: StateFlow<String?> = _activeReadingComicId.asStateFlow()

    private val _toast = MutableStateFlow<ToastNotice?>(null)
    val toast: StateFlow<ToastNotice?> = _toast.asStateFlow()

    private val _onlineSequencePool = MutableStateFlow<List<ComicEntry>>(emptyList())
    val onlineSequencePool: StateFlow<List<ComicEntry>> = _onlineSequencePool.asStateFlow()

    private var searchJob: Job? = null
    private var sequenceJob: Job? = null
    private var toastJob: Job? = null

    fun selectActiveReadingComic(id: String) {
        _activeReadingComicId.value = id
        savedStateHandle["activeReadingComicId"] = id
    }

    fun cycleActiveReadingComic(delta: Int = 1) {
        val readingList = _comics.value.filter { it.status == ReadingStatus.READING }
        if (readingList.size <= 1) return
        val currentIdx = readingList.indexOfFirst { it.id == _activeReadingComicId.value }.let { if (it < 0) 0 else it }
        val nextIdx = (currentIdx + delta + readingList.size) % readingList.size
        val nextId = readingList[nextIdx].id
        _activeReadingComicId.value = nextId
        savedStateHandle["activeReadingComicId"] = nextId
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Defer non-critical background work until after initial Activity SurfaceSyncGroup transaction completes
            delay(600)
            cloudRepo?.let { repo ->
                _cloudUser.value = repo.currentAuthUser()
                repo.observeAuthState().collectLatest { user ->
                    _cloudUser.value = user
                    startOrStopCloudListeners(user)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Let the first frame and SurfaceSyncGroup complete without CPU/IO contention
            delay(1200)

            // Auto-heal any existing user comics that still have missing or slow OpenLibrary cover URLs
            var updatedAny = false
            val healedList = _comics.value.map { comic ->
                val isCurated = comic.id.startsWith("cat-") || comic.id.startsWith("init-") || comic.externalId.startsWith("cat-")
                val catalogHit = CuratedCatalog.curatedComicsCatalog.firstOrNull {
                    it.externalId == comic.externalId ||
                        it.id == comic.id ||
                        it.id == comic.id.replace("init-", "cat-") ||
                        (comic.isbn.isNotBlank() && it.isbn == comic.isbn) ||
                        it.title.equals(comic.title, ignoreCase = true)
                }
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
                val localized = PortugueseComicTranslator.localizeComic(withCover)
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

            if (updatedAny) {
                _comics.value = healedList
                persistAll()
            }
            prefetchCovers(_comics.value.take(10))
            delay(800)
            prefetchCovers(CuratedCatalog.curatedComicsCatalog.take(12))
            searchEngine.prewarmPopularSearches()
            if (_searchQuery.value.isNotBlank()) {
                triggerOnlineSearchIfNeeded()
            }
        }
    }

    private fun startOrStopCloudListeners(user: CloudAuthUser?) {
        cloudProfileJob?.cancel()
        cloudComicsJob?.cancel()
        val repo = cloudRepo ?: return
        if (user == null) return

        cloudProfileJob = viewModelScope.launch(Dispatchers.IO) {
            repo.observeUserProfile(user.uid)
                .catch { }
                .collectLatest { remoteProfile ->
                    if (remoteProfile != null && remoteProfile.ownerId == user.uid) {
                        val merged = remoteProfile.toReaderProfile(_profile.value.avatarUrl)
                        if (merged != _profile.value) {
                            _profile.value = merged
                            persistAll()
                        }
                    } else {
                        runCatching { repo.saveUserProfile(user.uid, _profile.value) }
                    }
                }
        }

        cloudComicsJob = viewModelScope.launch(Dispatchers.IO) {
            repo.observeUserComics(user.uid)
                .catch { }
                .collectLatest { remoteComics ->
                    if (remoteComics.isNotEmpty()) {
                        if (remoteComics != _comics.value) {
                            _comics.value = remoteComics
                            persistAll()
                        }
                    } else if (_comics.value.isNotEmpty()) {
                        runCatching { repo.syncAllComicsBatch(user.uid, _comics.value) }
                    }
                }
        }
    }

    fun trySilentGoogleSignIn(activityContext: Context) {
        val repo = cloudRepo ?: return
        if (_cloudUser.value != null) return
        viewModelScope.launch {
            repo.trySilentGoogleSignIn(activityContext).onSuccess { user ->
                if (user != null) {
                    _cloudUser.value = user
                }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        val repo = cloudRepo ?: run {
            showNotice("Serviço Firebase indisponível neste dispositivo")
            return
        }
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val result = repo.signInWithGoogleInteractive(activityContext)
            _isSyncingCloud.value = false
            result.onSuccess { user ->
                _cloudUser.value = user
                if (_profile.value.name == "Felipe" && !user.displayName.isNullOrBlank()) {
                    updateProfile { it.copy(name = user.displayName) }
                }
                syncNowWithCloud()
                showNotice("Conectado como ${user.displayName ?: user.email ?: "Leitor"}")
            }.onFailure { err ->
                if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    showNotice("Não foi possível entrar com o Google")
                }
            }
        }
    }

    fun signOutFromCloud(activityContext: Context) {
        val repo = cloudRepo ?: return
        viewModelScope.launch {
            repo.signOut(activityContext)
            _cloudUser.value = null
            showNotice("Conta desconectada da nuvem")
        }
    }

    fun syncNowWithCloud() {
        val repo = cloudRepo ?: return
        val user = _cloudUser.value ?: return
        if (_isSyncingCloud.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingCloud.value = true
            runCatching {
                repo.saveUserProfile(user.uid, _profile.value)
                repo.syncAllComicsBatch(user.uid, _comics.value)
            }.onSuccess {
                showNotice("Acervo sincronizado na nuvem (${_comics.value.size} obras)")
            }.onFailure {
                showNotice("Falha ao sincronizar com a nuvem")
            }
            _isSyncingCloud.value = false
        }
    }

    private fun prefetchCovers(items: List<ComicEntry>) {
        val loader = appContext.imageLoader
        items.forEach { item ->
            val url = item.coverUrl.trim().replace("http://", "https://")
            if (url.startsWith("https://")) {
                val req = ImageRequest.Builder(appContext)
                    .data(url)
                    .size(240, 360)
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

    private fun persistAll() {
        val comicsSnapshot = _comics.value
        val profileSnapshot = _profile.value
        val settingsSnapshot = _appSettings.value
        viewModelScope.launch(Dispatchers.IO) {
            storage.saveState(comicsSnapshot, profileSnapshot, settingsSnapshot)
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
        scrollMemory["settings:$page"] = 0 to 0
        _settingsParentPage.value = parent
        _settingsSubPage.value = page
        savedStateHandle["settingsParentPage"] = parent
        savedStateHandle["settingsSubPage"] = page
    }

    fun goBackSettings() {
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

    fun updateProfile(transform: (ReaderProfile) -> ReaderProfile) {
        val next = transform(_profile.value)
        _profile.value = next
        persistAll()
        val uid = _cloudUser.value?.uid
        val repo = cloudRepo
        if (uid != null && repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repo.saveUserProfile(uid, next) }
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _appSettings.value = transform(_appSettings.value)
        persistAll()
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
        _shelfLayout.value = next
        savedStateHandle["shelfLayout"] = next
    }

    fun setShelfLayout(layout: String) {
        _shelfLayout.value = layout
        savedStateHandle["shelfLayout"] = layout
    }

    fun setShelfSort(sort: String) {
        _shelfSort.value = sort
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
        val localized = comic?.let { PortugueseComicTranslator.localizeComic(it, allowNetworkTranslation = false) }
        _infoModalComic.value = localized
        loadSequenceIssuesForComic(localized)
        if (localized == null) {
            _isSheetExpanded.value = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val enriched = PortugueseComicTranslator.localizeComic(localized, allowNetworkTranslation = true)
                if (enriched != localized && _infoModalComic.value?.id == localized.id) {
                    _infoModalComic.value = enriched
                }
            }
        }
    }

    fun selectComicForSheet(comic: ComicEntry?) {
        _infoModalComic.value = null
        val localized = comic?.let { PortugueseComicTranslator.localizeComic(it, allowNetworkTranslation = false) }
        _selectedComic.value = localized
        loadSequenceIssuesForComic(localized)
        if (localized == null) {
            _isSheetExpanded.value = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val enriched = PortugueseComicTranslator.localizeComic(localized, allowNetworkTranslation = true)
                if (enriched != localized && _selectedComic.value?.id == localized.id) {
                    _selectedComic.value = enriched
                }
            }
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

        val entry = itemData.copy(
            id = uniqueId,
            externalId = extId,
            title = normTitle,
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
            addedAt = itemData.addedAt.ifBlank { nowIso }
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

        val uid = _cloudUser.value?.uid
        val repo = cloudRepo
        if (uid != null && repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repo.saveComic(uid, entry) }
            }
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
        val uid = _cloudUser.value?.uid
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
        var updatedComic: ComicEntry? = null
        _comics.value = _comics.value.map { c ->
            if (c.id == comicItem.id) {
                c.copy(currentPage = clamped, status = nextStatus).also { updatedComic = it }
            } else {
                c
            }
        }
        persistAll()
        val uid = _cloudUser.value?.uid
        val repo = cloudRepo
        val target = updatedComic
        if (uid != null && repo != null && target != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repo.saveComic(uid, target) }
            }
        }
    }

    fun clearAllComics() {
        _comics.value = emptyList()
        persistAll()
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
