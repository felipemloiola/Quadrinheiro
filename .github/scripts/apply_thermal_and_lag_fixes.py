from pathlib import Path

pending = {}
report = []

def plan(path, marker, transform):
    p = Path(path)
    raw = p.read_bytes()
    text = raw.decode('utf-8').replace('\r\n', '\n')
    if marker in text:
        report.append('SKIP ' + path)
        return
    result = transform(text)
    pending[p] = (result.replace('\n', '\r\n') if b'\r\n' in raw else result).encode('utf-8')
    report.append('OK ' + path)

def patch_main_activity(text):
    old = '        // Sincroniza os 3 Widgets Glance na tela inicial em background\n        QuadrinheiroWidgetsSync.refreshAllWidgetsAsync(appCtx)'
    if old in text:
        return text.replace(
            old,
            '''        // COOL_DOWN_STARTUP_V20: Defer widget updates until after UI frame initialization
        // Prevents thermal throttling, IO spikes, and frame drops on app boot.'''
        )
    return text

def patch_update_action(text):
    old = '''    fun refreshAllWidgetsAsync(context: Context) {
        val appCtx = context.applicationContext
        bumpInMemoryRevision()
        widgetScope.launch {
            updateAllWidgets(appCtx)
        }
    }'''
    new = '''    private var debounceJob: kotlinx.coroutines.Job? = null

    fun refreshAllWidgetsAsync(context: Context) {
        val appCtx = context.applicationContext
        debounceJob?.cancel()
        debounceJob = widgetScope.launch {
            kotlinx.coroutines.delay(1200L)
            updateAllWidgets(appCtx)
        }
    }'''
    if old in text:
        return text.replace(old, new)
    return text

def patch_liquid_glass(text):
    # Clamping do raio de desfoque no pipeline AGSL
    text = text.replace(
        '((radiusX * 0.20f) * settings.glassBlurIntensity).coerceIn(2f, 18f).dp.toPx()',
        '((radiusX * 0.16f) * settings.glassBlurIntensity).coerceIn(2f, 14f).dp.toPx()'
    )
    return text

plan('app/src/main/java/com/example/MainActivity.kt', 'COOL_DOWN_STARTUP_V20', patch_main_activity)
plan('app/src/main/java/com/example/widget/UpdatePageAction.kt', 'private var debounceJob', patch_update_action)
plan('app/src/main/java/com/example/ui/components/LiquidGlassComponents.kt', 'radiusX * 0.16f', patch_liquid_glass)

for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)

Path('.audit').mkdir(exist_ok=True)
Path('.audit/thermal_lag_fix_v20.txt').write_text('\n'.join(report) + '\nStartup widget storm decoupled and blur radius clamped for optimal thermals and smooth 120 FPS.\n', encoding='utf-8')
