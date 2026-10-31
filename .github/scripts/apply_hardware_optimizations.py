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

def patch_manifest(text):
    text = once(
        text,
        '<application\n        android:allowBackup="true"',
        '''<application
        android:hardwareAccelerated="true"
        android:largeHeap="true"
        android:allowBackup="true"'''
    )
    text = once(
        text,
        '<activity\n            android:name=".MainActivity"',
        '''<activity
            android:name=".MainActivity"
            android:hardwareAccelerated="true"'''
    )
    return text

def patch_main(text):
    text = once(
        text,
        '        enableEdgeToEdge()',
        '''        // HW_DISPLAY_MODE_V19: Maximize refresh rate (up to 120Hz) and lock window display mode
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val display = display
                if (display != null) {
                    val modes = display.supportedModes
                    val maxMode = modes.maxByOrNull { it.refreshRate }
                    if (maxMode != null && maxMode.refreshRate >= 90f) {
                        window.attributes = window.attributes.apply {
                            preferredDisplayModeId = maxMode.modeId
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }

        enableEdgeToEdge()'''
    )
    return text

plan('app/src/main/AndroidManifest.xml', 'android:hardwareAccelerated="true"', patch_manifest)
plan('app/src/main/java/com/example/MainActivity.kt', 'HW_DISPLAY_MODE_V19', patch_main)

for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)

Path('.audit').mkdir(exist_ok=True)
Path('.audit/hw_acceleration_v19.txt').write_text('\n'.join(report) + '\nHardware acceleration explicit flags, largeHeap, and high refresh rate window mode applied.\n', encoding='utf-8')
