from pathlib import Path

# LEGACY_PATCHES_RETIRED_V14: these transformations were integrated previously.
# Never reconstruct old Kotlin implementations over newer AI Studio refactors.
path = Path('app/src/main/java/com/example/data/SafeStorage.kt')
text = path.read_text(encoding='utf-8').replace('\r\n', '\n')
checks = {
    'refactored state parser': 'private fun parseStoredStateJson(',
    'default settings source': 'val defaults = AppSettings()',
    'missing theme fallback': 'themeId = if (!saved.has("themeId") || saved.optString("themeId", "").isBlank()) defaults.themeId else parsed.themeId',
    'missing color mode fallback': '"" -> defaults.colorScheme',
    'saved color mode retained': 'else -> parsed.colorScheme',
    'guest state loader': 'fun loadGuestState()',
    'per-account state loader': 'fun loadAccountState(uid: String)'
}
missing = [name for name, anchor in checks.items() if anchor not in text]
if missing:
    raise RuntimeError('Refactored storage requires review; no Kotlin files changed. Missing checks: ' + ', '.join(missing))
Path('.audit').mkdir(exist_ok=True)
Path('.audit/default_theme_guest.txt').write_text(
    'Legacy V7 mutating transformations retired; no Kotlin files changed by this script.\n'
    'CHECK: refactored SafeStorage retains missing-preference defaults and isolated state loaders.\n'
    'Guest UI, theme, typography and auth implementations are not reapplied or overwritten.\n',
    encoding='utf-8'
)
