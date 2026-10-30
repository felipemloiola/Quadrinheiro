from pathlib import Path

pending = {}
def patch(path, marker, replacements):
    p = Path(path)
    raw = p.read_bytes()
    text = raw.decode('utf-8').replace('\r\n', '\n')
    if marker in text:
        return
    for old, new in replacements:
        if text.count(old) != 1:
            raise RuntimeError('Release integration anchor not unique: ' + path + ': ' + old[:80])
        text = text.replace(old, new, 1)
    pending[p] = (text.replace('\n', '\r\n') if b'\r\n' in raw else text).encode('utf-8')

patch('app/build.gradle.kts', 'SIGNED_RELEASE_VERSION_V1', [
    ('    versionCode = 1\n    versionName = "1.0"', '    // SIGNED_RELEASE_VERSION_V1: CI supplies monotonically increasing release versions.\n    versionCode = providers.gradleProperty("releaseVersionCode").orNull?.toInt() ?: 1\n    versionName = providers.gradleProperty("releaseVersionName").orNull ?: "1.0"'),
    ('      storeFile = file(keystorePath)', '      storeFile = file(keystorePath)\n      storeType = System.getenv("KEYSTORE_TYPE") ?: "JKS"')
])
patch('app/src/main/java/com/example/MainActivity.kt', 'APP_UPDATES_HOST_V1', [
    ('    QuadrinheiroTheme(settings = appSettings) {', '    QuadrinheiroTheme(settings = appSettings) {\n        // APP_UPDATES_HOST_V1: isolated from navigation and gesture handlers.\n        com.example.updates.AppUpdatePrompt()')
])
patch('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'APP_UPDATES_SETTINGS_V1', [
    ('                    QuadrinheiroLogo(\n                        // SETTINGS_LOGO_ADAPTIVE_V8', '                    // APP_UPDATES_SETTINGS_V1\n                    com.example.updates.CheckForUpdatesButton()\n                    QuadrinheiroLogo(\n                        // SETTINGS_LOGO_ADAPTIVE_V8')
])

gradle_path = Path('app/build.gradle.kts')
gradle_raw = pending.get(gradle_path, gradle_path.read_bytes())
gradle_text = gradle_raw.decode('utf-8').replace('\r\n', '\n')
if 'FULL_TEST_EXCEPTION_LOG_V1' not in gradle_text:
    gradle_text += '''
// FULL_TEST_EXCEPTION_LOG_V1: diagnostic only; preserve test failure gates.
tasks.withType<Test>().configureEach {
  testLogging {
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showExceptions = true
    showCauses = true
    showStackTraces = true
  }
}
'''
    pending[gradle_path] = (gradle_text.replace('\n', '\r\n') if b'\r\n' in gradle_raw else gradle_text).encode('utf-8')

for p, content in pending.items():
    p.write_bytes(content)
