from pathlib import Path
import re

p = Path('app/src/main/java/com/example/ui/components/LiquidGlassComponents.kt')
raw = p.read_bytes()
t = raw.decode('utf-8').replace('\r\n', '\n')
marker = 'PICKER_STABLE_PRESS_COLOR_V10'
if marker not in t:
    start = t.index('fun <T> AppleMenuPicker(')
    end = t.find('\n@Composable', start)
    if end < 0:
        end = len(t)
    body = t[start:end]
    overlay = re.compile(r'                \.drawWithContent \{\n                    drawContent\(\)\n                    val light = pickerPressProgress\.coerceIn\(0f, 1f\)\n.*?\n                \}\n', re.S)
    matches = list(overlay.finditer(body))
    if len(matches) != 1 or 'Color.White.copy(alpha = 0.24f * light)' not in matches[0].group():
        raise RuntimeError('Picker white reflection not uniquely identified; source not written')
    body = overlay.sub('                // PICKER_STABLE_PRESS_COLOR_V10: retain the resting surface color while held.\n', body, count=1)
    highlight = re.compile(r'alpha\s*=\s*lerp\(\s*if \(isLight\) 0\.80f else 0\.64f,\s*0\.96f,\s*pickerPressProgress\s*\)\s*\*\s*triggerMorphAlpha')
    if len(highlight.findall(body)) != 1:
        raise RuntimeError('Picker press highlight not uniquely identified; source not written')
    body = highlight.sub('alpha = (if (isLight) 0.80f else 0.64f) * triggerMorphAlpha', body, count=1)
    result = t[:start] + body + t[end:]
    if result.count('.clickable(') != t.count('.clickable('):
        raise RuntimeError('Click handler preservation check failed; source not written')
    p.write_bytes((result.replace('\n', '\r\n') if b'\r\n' in raw else result).encode('utf-8'))
    status = 'OK: removed picker white press overlay; retained resting edge-highlight strength, base surface color, click handlers and scale animation.'
else:
    status = 'SKIP: stable picker press color already applied'

Path('.audit').mkdir(exist_ok=True)
Path('.audit/menu_press_color_v10.txt').write_text(status + '\nDevice verification pending: compare stationary tap, held touch and expanded-menu item press in light/dark themes. Shared gesture detector and tab bar not modified by this patch.\n', encoding='utf-8')
# Preserve an inspectable excerpt of the expanded menu for follow-up verification.
source = p.read_text(encoding='utf-8').replace('\r\n', '\n')
host = source.find('fun LiquidGlassMenuOverlayHost(')
if host >= 0:
    stop = source.find('\n@Composable', host)
    Path('.audit/menu_overlay_source.txt').write_text(source[host:stop if stop >= 0 else len(source)], encoding='utf-8')
