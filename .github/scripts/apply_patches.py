import pathlib

REPORT = []
CRLF = chr(13) + chr(10)
LF = chr(10)


def patch(path, edits, marker, append=None):
    p = pathlib.Path(path)
    if not p.exists():
        REPORT.append('MISSING: ' + path)
        return
    raw = p.read_bytes().decode('utf-8')
    crlf = CRLF in raw
    t = raw.replace(CRLF, LF)
    if marker in t:
        REPORT.append('SKIP (already patched): ' + path)
        return
    for i, (old, new) in enumerate(edits):
        c = t.count(old)
        if c != 1:
            REPORT.append('FAIL %s edit %d: anchor found %d times: %r' % (path, i, c, old[:80]))
            return
        t = t.replace(old, new)
    if append:
        t = t.rstrip(LF) + LF + LF + append
    if crlf:
        t = t.replace(LF, CRLF)
    p.write_bytes(t.encode('utf-8'))
    REPORT.append('OK: ' + path)


VM = 'app/src/main/java/com/example/viewmodel/QuadrinheiroViewModel.kt'
MAIN = 'app/src/main/java/com/example/MainActivity.kt'

VM_FUNCS = '''    fun resetToReadingHome() {
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

'''

vm_edits = [
    ('''    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()
''',
     '''    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    private val _isEnteringApp = MutableStateFlow(false)
    val isEnteringApp: StateFlow<Boolean> = _isEnteringApp.asStateFlow()
'''),
    ('MutableStateFlow(savedStateHandle.get<String>("activeTab") ?: "home")', 'MutableStateFlow("home")'),
    ('MutableStateFlow(savedStateHandle.get<Int>("lastCapsuleIndex") ?: 0)', 'MutableStateFlow(0)'),
    ('''    fun clearAuthFeedback() {
        _authFeedback.value = null
    }
''',
     VM_FUNCS + '''    fun clearAuthFeedback() {
        _authFeedback.value = null
    }
'''),
    ('''        _activeReadingComicId.value = null
        persistAll()
''',
     '''        _activeReadingComicId.value = null
        persistAll()
        resetToReadingHome()
        startSessionEntryTransition()
'''),
    ('''        val nowMs = System.currentTimeMillis()
        storage.saveAuthSession(userProfile)
''',
     '''        val startedFromSettingsModal = _showAuthModalFromSettings.value
        val nowMs = System.currentTimeMillis()
        storage.saveAuthSession(userProfile)
'''),
    ('''        // Atualiza o ReaderProfile com o nome/foto da conta se ainda estiver com o nome padrão
''',
     '''        if (!startedFromSettingsModal) {
            resetToReadingHome()
            startSessionEntryTransition()
        }

        // Atualiza o ReaderProfile com o nome/foto da conta se ainda estiver com o nome padrão
'''),
    ('''    fun logout(activityContext: Context? = null) {
        viewModelScope.launch {
''',
     '''    fun logout(activityContext: Context? = null) {
        _isLoggingOut.value = true
        viewModelScope.launch {
            delay(6000)
            _isLoggingOut.value = false
        }
        viewModelScope.launch {
'''),
    ('''            _authUiState.value = AuthUiState.Unauthenticated
            showNotice("Sessão encerrada com segurança")''',
     '''            delay(450)
            resetToReadingHome()
            _authUiState.value = AuthUiState.Unauthenticated
            _isLoggingOut.value = false
            showNotice("Sessão encerrada com segurança")'''),
]

patch(VM, vm_edits, 'fun resetToReadingHome')

MAIN_COMPOSABLE = '''@Composable
private fun SessionTransitionScreen(message: String) {
    val themeColors = LocalIOS27Theme.current
    val visibleState = remember {
        androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.bgPrimary),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visibleState = visibleState, enter = fadeIn(tween(300))) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = themeColors.accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = message,
                    color = themeColors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSansFamily
                )
            }
        }
    }
}
'''

main_edits = [
    ('''            AuthScreen(
                uiState = authScreenUiState,
''',
     '''            androidx.compose.animation.AnimatedVisibility(
                visibleState = remember {
                    androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
                },
                enter = fadeIn(tween(450))
            ) {
            AuthScreen(
                uiState = authScreenUiState,
'''),
    ('''                    onDismissModal = null
                )
            )
            return@QuadrinheiroTheme
''',
     '''                    onDismissModal = null
                )
            )
            }
            return@QuadrinheiroTheme
'''),
    ('    val toast by viewModel.toast.collectAsState()\n'.replace(chr(92) + 'n', LF),
     '    val toast by viewModel.toast.collectAsState()\n'.replace(chr(92) + 'n', LF)
     + '    val isLoggingOut by viewModel.isLoggingOut.collectAsState()\n'.replace(chr(92) + 'n', LF)
     + '    val isEnteringApp by viewModel.isEnteringApp.collectAsState()\n'.replace(chr(92) + 'n', LF)),
    ('        // ROTEAMENTO CONDICIONAL SÍNCRONO NO BOOT (SEM FLASH DE TELA):\n'.replace(chr(92) + 'n', LF),
     '''        if (isLoggingOut) {
            SessionTransitionScreen("Saindo da conta…")
            return@QuadrinheiroTheme
        }
        if (isEnteringApp && authUiState != AuthUiState.Unauthenticated) {
            SessionTransitionScreen("Preparando sua coleção…")
            return@QuadrinheiroTheme
        }

        // ROTEAMENTO CONDICIONAL SÍNCRONO NO BOOT (SEM FLASH DE TELA):
'''),
]

patch(MAIN, main_edits, 'SessionTransitionScreen', append=MAIN_COMPOSABLE)


def audit():
    out = []
    keys = ('QuadrinheiroLogo(', 'Mais recentes', 'Mais Recentes')
    for f in sorted(pathlib.Path('app/src').rglob('*.kt')):
        lines = f.read_text(encoding='utf-8', errors='ignore').splitlines()
        for i, line in enumerate(lines):
            if any(k in line for k in keys):
                lo, hi = max(0, i - 8), min(len(lines), i + 9)
                out.append('=== %s:%d' % (f, i + 1))
                out.extend(lines[lo:hi])
    pathlib.Path('.audit').mkdir(exist_ok=True)
    pathlib.Path('.audit/usages.txt').write_text(LF.join(out)[:60000], encoding='utf-8')
    pathlib.Path('.audit/patch_report.txt').write_text(LF.join(REPORT), encoding='utf-8')


audit()
