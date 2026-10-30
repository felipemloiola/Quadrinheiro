from pathlib import Path
import hashlib

pending = {}
report = []
def once(text, old, new):
    count = text.count(old)
    if count != 1:
        raise RuntimeError('Glass polish: expected one anchor, found %d: %s' % (count, old[:100]))
    return text.replace(old, new, 1)

def plan(path, marker, transform):
    p = Path(path)
    raw = p.read_bytes()
    text = raw.decode('utf-8').replace('\r\n', '\n')
    if marker in text:
        report.append('SKIP ' + path)
        return
    result = transform(text, raw)
    if b'\r\n' in raw:
        result = result.replace('\n', '\r\n')
    pending[p] = result.encode('utf-8')
    report.append('OK ' + path)

def logo(t, raw):
    t = once(t, 'private fun LogoMark(variant: QuadrinheiroLogoVariant, modifier: Modifier) {', '// ADAPTIVE_LOGO_BADGE_V8: opt-in for Settings only.\nprivate fun LogoMark(variant: QuadrinheiroLogoVariant, modifier: Modifier, adaptiveBackground: Boolean = false) {')
    t = once(t, 'sized.background(theme.bgPrimary, RoundedCornerShape(12.dp))', '''sized.background(
            if (adaptiveBackground && theme.isDark) {
                androidx.compose.ui.graphics.lerp(theme.bgPrimary, theme.textPrimary, 0.08f)
            } else theme.bgPrimary,
            RoundedCornerShape(12.dp)
        )''')
    t = once(t, '    leadingIcon: ImageVector? = null\n)', '    leadingIcon: ImageVector? = null,\n    adaptiveBadgeBackground: Boolean = false\n)')
    return once(t, '        LogoMark(variant, modifier)', '        LogoMark(variant, modifier, adaptiveBadgeBackground)')

def settings(t, raw):
    return once(t, '''QuadrinheiroLogo(
                        modifier = Modifier
                            .size(42.dp)''', '''QuadrinheiroLogo(
                        // SETTINGS_LOGO_ADAPTIVE_V8
                        adaptiveBadgeBackground = true,
                        modifier = Modifier
                            .size(42.dp)''')

GESTURES = '''package com.kyant.backdrop.catalog.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope

// EXCLUSIVE_GLASS_DRAG_V8: preserve taps; reserve held/dragged movement before list scrolling.
suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    val slop = viewConfiguration.touchSlop
    val holdTimeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var ended = false
        var ownsMovement = false
        onDragStart(down)
        try {
            onDrag(down, Offset.Zero)
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    onDragEnd(change)
                    ended = true
                    break
                }
                if (change.isConsumed) break
                val delta = change.position - change.previousPosition
                val distance = (change.position - down.position).getDistance()
                val held = change.uptimeMillis - down.uptimeMillis >= holdTimeout
                ownsMovement = ownsMovement || distance > slop || held
                if (delta != Offset.Zero) {
                    if (ownsMovement) change.consume()
                    onDrag(change, delta)
                }
            }
        } finally {
            if (!ended) onDragCancel()
        }
    }
}
'''

def gestures(t, raw):
    blob = hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()
    if blob != '74a58e67de81ccfb9bb2d7b0bcc3925ad5952049':
        raise RuntimeError('DragGestureInspector changed since review; refusing replacement: ' + blob)
    return GESTURES

plan('app/src/main/java/com/example/ui/components/QuadrinheiroLogo.kt', 'ADAPTIVE_LOGO_BADGE_V8', logo)
plan('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'SETTINGS_LOGO_ADAPTIVE_V8', settings)
plan('app/src/main/java/com/kyant/backdrop/catalog/utils/DragGestureInspector.kt', 'EXCLUSIVE_GLASS_DRAG_V8', gestures)
for p, content in pending.items():
    p.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/glass_controls_v8.txt').write_text('\n'.join(report) + '\nManual verification required: stationary tap opens menu; held diagonal drag moves only control; release/cancel restores animation; Settings logo in light/dark themes.\n', encoding='utf-8')
