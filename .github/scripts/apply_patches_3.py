from pathlib import Path

p = Path('app/src/main/java/com/example/ui/components/LiquidGlassComponents.kt')
raw = p.read_bytes().decode('utf-8')
crlf = '\r\n' in raw
t = raw.replace('\r\n', '\n')
marker = 'SORT_PICKER_SINGLE_TOUCH_HANDLER'
report = Path('.audit/patch_report_3.txt')
report.parent.mkdir(exist_ok=True)

if marker in t:
    report.write_text('SKIP: sort picker touch patch already present\n', encoding='utf-8')
else:
    start_anchor = 'fun <T> AppleMenuPicker('
    if t.count(start_anchor) != 1:
        raise RuntimeError('Expected exactly one generic AppleMenuPicker; nothing written')
    start = t.index(start_anchor)
    end = t.find('\n@Composable', start)
    if end < 0:
        end = len(t)
    section = t[start:end]
    edits = [
        (
            '.then(triggerHighlight.gestureModifier)',
            '// SORT_PICKER_SINGLE_TOUCH_HANDLER: clickable owns touch; no competing decorative gesture'
        ),
        (
            '        Row(\n            modifier = Modifier\n                .graphicsLayer {',
            '        Row(\n            modifier = Modifier\n                .heightIn(min = 48.dp)\n                .graphicsLayer {'
        )
    ]
    for old, new in edits:
        if section.count(old) != 1:
            raise RuntimeError('Picker anchor missing or ambiguous; nothing written: ' + old)
        section = section.replace(old, new, 1)
    result = t[:start] + section + t[end:]
    imp = 'import androidx.compose.foundation.layout.heightIn'
    if imp not in result and 'import androidx.compose.foundation.layout.*' not in result:
        package = 'package com.example.ui.components\n'
        if result.count(package) != 1:
            raise RuntimeError('Unexpected package; nothing written')
        result = result.replace(package, package + '\n' + imp + '\n', 1)
    if crlf:
        result = result.replace('\n', '\r\n')
    p.write_bytes(result.encode('utf-8'))
    report.write_text('OK: removed competing decorative gesture from AppleMenuPicker; clickable retained; minimum touch height 48dp. Runtime touch test pending.\n', encoding='utf-8')
