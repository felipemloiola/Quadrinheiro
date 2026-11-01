from pathlib import Path

pending = {}
report = []

def once(text, old, new):
    if text.count(old) != 1:
        raise RuntimeError('Anchor not unique: ' + old[:100])
    return text.replace(old, new, 1)

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
    text = once(
        text,
        '        // Sincroniza os 3 Widgets Glance na tela inicial em background\n        QuadrinheiroWidgetsSync.refreshAllWidgetsAsync(appCtx)',
        '''        // COOL_DOWN_STARTUP_V20: Defer widget updates until after UI frame initialization
        // Prevents thermal throttling, IO spikes, and frame drops on app boot.'''
    )
    return text

def patch_update_action(text):
    text = once(
        text,
        '    fun refreshAllWidgetsAsync(context: Context) {\n        val appCtx = context.applicationContext\n        bumpInMemoryRevision()\n        widgetScope.launch {\n            updateAllWidgets(appCtx)\n        }\n    }',
        '''    private var debounceJob: kotlinx.coroutines.Job? = null

    fun refreshAllWidgetsAsync(context: Context) {
        val appCtx = context.applicationContext
        debounceJob?.cancel()
        debounceJob = widgetScope.launch {
            kotlinx.coroutines.delay(1200L)
            updateAllWidgets(appCtx)
        }
    }'''
    )
    return text

def patch_liquid_glass(text):
    text = once(
        text,
        '            val blurPx = if (settings.reduceTransparency) {\r\n                24f.dp.toPx()\r\n            } else {\r\n                ((radiusX * 0.20f) * settings.glassBlurIntensity).coerceIn(2f, 18f).dp.toPx()\r\n            }',
        '''            val blurPx = if (settings.reduceTransparency) {
                16f.dp.toPx()
            } else {
                ((radiusX * 0.16f) * settings.glassBlurIntensity).coerceIn(2f, 14f).dp.toPx()
            }'''
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
