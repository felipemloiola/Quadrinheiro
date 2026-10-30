from pathlib import Path
import re

report = []
def once(t, old, new):
    if t.count(old) != 1:
        raise RuntimeError('Expected one anchor; this file not written: ' + old[:100])
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
    p.write_bytes(result.replace('\n', '\r\n').encode('utf-8') if crlf else result.encode('utf-8'))
    report.append('OK: ' + path)

def models(t):
    return once(t, '''data class AppSettings(
    val themeId: String = "midnightSlate",
    val colorScheme: String = "dark",''', '''data class AppSettings(
    // SYSTEM_MONO_DEFAULT_V7: only new or missing preferences use these defaults.
    val themeId: String = "SYSTEM_MONO",
    val colorScheme: String = "system",''')

def theme(t):
    old = '''    val isDark = settings.colorScheme != "light"
    val appTheme = remember(settings.themeId) { AppTheme.fromId(settings.themeId) }'''
    new = '''    // SYSTEM_MONO_THEME_V7: explicit user choices always take precedence.
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
    }'''
    t = once(t, old, new)
    t = once(t, 'val themeDef = remember(settings.themeId) { IOS27ThemeDef.fromId(settings.themeId) }', 'val themeDef = remember(settings.themeId) { if (settings.themeId == "SYSTEM_MONO") IOS27ThemeDef.MONOCHROME else IOS27ThemeDef.fromId(settings.themeId) }')
    return t

def storage(t):
    for key, value in [('themeId', 'SYSTEM_MONO'), ('colorScheme', 'system')]:
        pattern = r'optString\("' + key + r'",\s*"[^"]*"\)'
        if len(re.findall(pattern, t)) != 1:
            raise RuntimeError('Storage default anchor ambiguous: ' + key)
        t = re.sub(pattern, 'optString("' + key + '", "' + value + '")', t)
    return t + '\n// STORAGE_SYSTEM_DEFAULTS_V7\n'

def main(t):
    return once(t, '    val profile by viewModel.profile.collectAsState()', '''    val storedProfile by viewModel.profile.collectAsState()
    // GUEST_PRESENTATION_V7: presentation only; never overwrite account data.
    val profile = if (authUiState == AuthUiState.Guest) storedProfile.copy(
        name = "Convidado", handle = "", bio = "", favoriteUniverse = "",
        avatarUrl = "", avatarPreset = "theme", showGoalOnHome = false
    ) else storedProfile''')

def settings(t):
    t = once(t, 'package com.example.ui.screens\n', '''package com.example.ui.screens

import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.AccessibilityNew
''')
    t = once(t, '    val themeColors = LocalIOS27Theme.current', '''    val themeColors = LocalIOS27Theme.current
    // GUEST_SETTINGS_V7: no identifiers, biography, favorites or goal editors for guests.
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
    t = once(t, '''icon = { Icon(LucideIcons.SlidersHorizontal, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Aparência",''', '''icon = { Icon(Icons.Outlined.Palette, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Aparência",''')
    t = once(t, '''icon = { Icon(LucideIcons.Maximize2, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Acessibilidade e Contraste",''', '''icon = { Icon(Icons.Outlined.AccessibilityNew, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Acessibilidade e Contraste",''')
    t = once(t, '''                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeColors.accent.copy(alpha = 0.09f))''', '''                            if (!isGuest) Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeColors.accent.copy(alpha = 0.09f))''')
    t = once(t, '''                        Text(
                            text = profile.bio.ifBlank { "Meta: $readComicsCount/${profile.yearlyGoal} no ano" },''', '''                        if (!isGuest) Text(
                            text = profile.bio.ifBlank { "Meta: $readComicsCount/${profile.yearlyGoal} no ano" },''')
    t = t.replace('text = profile.name.ifBlank { "Leitor" },', 'text = if (isGuest) "Convidado" else profile.name.ifBlank { "Leitor" },')
    return t

def auth_manager(t):
    if '"Leitor Convidado"' not in t:
        raise RuntimeError('Guest label not found')
    return t.replace('"Leitor Convidado"', '"Convidado"') + '\n// GUEST_LABEL_V7\n'

edit('app/src/main/java/com/example/model/Models.kt', 'SYSTEM_MONO_DEFAULT_V7', models)
edit('app/src/main/java/com/example/ui/theme/Theme.kt', 'SYSTEM_MONO_THEME_V7', theme)
edit('app/src/main/java/com/example/data/SafeStorage.kt', 'STORAGE_SYSTEM_DEFAULTS_V7', storage)
edit('app/src/main/java/com/example/MainActivity.kt', 'GUEST_PRESENTATION_V7', main)
edit('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'GUEST_SETTINGS_V7', settings)
edit('app/src/main/java/com/example/data/QuadrinheiroAuthManager.kt', 'GUEST_LABEL_V7', auth_manager)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/default_theme_guest.txt').write_text('\n'.join(report), encoding='utf-8')
