from pathlib import Path

report = []
pending = {}
def once(t, old, new):
    count = t.count(old)
    if count != 1:
        raise RuntimeError('Expected one anchor, found %d; no source files written: %s' % (count, old[:110]))
    return t.replace(old, new, 1)

def edit(path, marker, fn):
    p = Path(path)
    raw = p.read_bytes().decode('utf-8')
    crlf = '\r\n' in raw
    t = raw.replace('\r\n', '\n')
    if marker in t:
        report.append('SKIP: ' + path)
        return
    result = fn(t)
    pending[p] = result.replace('\n', '\r\n').encode('utf-8') if crlf else result.encode('utf-8')
    report.append('OK: ' + path)

def models(t):
    return once(t, '''data class AppSettings(
    val themeId: String = "midnightSlate",
    val colorScheme: String = "dark",''', '''data class AppSettings(
    // SYSTEM_MONO_DEFAULT_V7: only new or missing preferences use these defaults.
    val themeId: String = "SYSTEM_MONO",
    val colorScheme: String = "system",''')

def theme(t):
    t = once(t, '''    val isDark = settings.colorScheme != "light"
    val appTheme = remember(settings.themeId) { AppTheme.fromId(settings.themeId) }''', '''    // SYSTEM_MONO_THEME_V7: explicit user choices always take precedence.
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (settings.colorScheme.lowercase()) {
        "system" -> systemDark
        "light" -> false
        else -> true
    }
    val appTheme = remember(settings.themeId, isDark) {
        if (settings.themeId == "SYSTEM_MONO") {
            if (isDark) AppTheme.WHITE else AppTheme.BLACK
        } else AppTheme.fromId(settings.themeId)
    }''')
    return once(t, 'val themeDef = remember(settings.themeId) { IOS27ThemeDef.fromId(settings.themeId) }', 'val themeDef = remember(settings.themeId) { if (settings.themeId == "SYSTEM_MONO") IOS27ThemeDef.MONOCHROME else IOS27ThemeDef.fromId(settings.themeId) }')

def storage(t):
    return once(t, '            val settings = root.optJSONObject("settings")?.let { parseSettings(it) } ?: AppSettings()', '''            // STORAGE_SYSTEM_DEFAULTS_V7: preserve saved choices and default only missing preferences.
            val settings = root.optJSONObject("settings")?.let { saved ->
                val parsed = parseSettings(saved)
                val defaults = AppSettings()
                val savedMode = saved.optString("colorScheme", "").lowercase()
                parsed.copy(
                    themeId = if (!saved.has("themeId") || saved.optString("themeId", "").isBlank()) defaults.themeId else parsed.themeId,
                    colorScheme = when (savedMode) {
                        "system", "light", "dark" -> savedMode
                        "" -> defaults.colorScheme
                        else -> parsed.colorScheme
                    }
                )
            } ?: AppSettings()''')

def main(t):
    return once(t, '    val profile by viewModel.profile.collectAsState()', '''    val storedProfile by viewModel.profile.collectAsState()
    // GUEST_PRESENTATION_V7: presentation only; never overwrite account data.
    val profile = if (authUiState == AuthUiState.Guest) storedProfile.copy(
        name = "Convidado", handle = "", bio = "", favoriteUniverse = "",
        avatarUrl = "", avatarPreset = "theme", showGoalOnHome = false
    ) else storedProfile''')

def settings(t):
    start = t.index('fun SettingsTabScreen(')
    end = t.find('\n@Composable', start)
    if end < 0:
        end = len(t)
    body = t[start:end]
    body = once(body, '    val mat = LocalAppleMaterials.current\n    val s = mat.scheme\n    val themeColors = LocalIOS27Theme.current', '''    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val themeColors = LocalIOS27Theme.current
    // GUEST_SETTINGS_V7: no personal profile or goal editors for guests.
    val sessionState by viewModel.authUiState.collectAsStateWithLifecycle()
    val isGuest = sessionState == AuthUiState.Guest
    if (isGuest && settingsSubPage == "profile") {
        GuestProfilePanel(
            onBack = viewModel::goBackSettings,
            onLogin = { viewModel.openAuthModalForLinking(AuthMode.LOGIN) },
            onRegister = { viewModel.openAuthModalForLinking(AuthMode.REGISTER) }
        )
        return
    }''')
    body = once(body, '''icon = { Icon(LucideIcons.SlidersHorizontal, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Aparência",''', '''icon = { Icon(Icons.Outlined.Palette, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Aparência",''')
    body = once(body, '''icon = { Icon(LucideIcons.Maximize2, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Acessibilidade e Contraste",''', '''icon = { Icon(Icons.Outlined.AccessibilityNew, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Acessibilidade e Contraste",''')
    body = once(body, '''                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeColors.accent.copy(alpha = 0.09f))''', '''                            if (!isGuest) Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeColors.accent.copy(alpha = 0.09f))''')
    body = once(body, '''                        Text(
                            text = profile.bio.ifBlank { "Meta: $readComicsCount/${profile.yearlyGoal} no ano" },''', '''                        if (!isGuest) Text(
                            text = profile.bio.ifBlank { "Meta: $readComicsCount/${profile.yearlyGoal} no ano" },''')
    body = body.replace('text = profile.name.ifBlank { "Leitor" },', 'text = if (isGuest) "Convidado" else profile.name.ifBlank { "Leitor" },')
    t = t[:start] + body + t[end:]
    return once(t, 'package com.example.ui.screens\n', '''package com.example.ui.screens

import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.AccessibilityNew
''')

def auth_manager(t):
    if '"Leitor Convidado"' not in t:
        raise RuntimeError('Guest label not found; no source files written')
    return t.replace('"Leitor Convidado"', '"Convidado"') + '\n// GUEST_LABEL_V7\n'

edit('app/src/main/java/com/example/model/Models.kt', 'SYSTEM_MONO_DEFAULT_V7', models)
edit('app/src/main/java/com/example/ui/theme/Theme.kt', 'SYSTEM_MONO_THEME_V7', theme)
edit('app/src/main/java/com/example/data/SafeStorage.kt', 'STORAGE_SYSTEM_DEFAULTS_V7', storage)
edit('app/src/main/java/com/example/MainActivity.kt', 'GUEST_PRESENTATION_V7', main)
edit('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'GUEST_SETTINGS_V7', settings)
edit('app/src/main/java/com/example/data/QuadrinheiroAuthManager.kt', 'GUEST_LABEL_V7', auth_manager)

# Validate every transformation before writing any Kotlin source file.
for p, content in pending.items():
    p.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/default_theme_guest.txt').write_text('\n'.join(report), encoding='utf-8')
