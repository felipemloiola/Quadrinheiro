from pathlib import Path

report = []
def once(t, old, new):
    if t.count(old) != 1:
        raise RuntimeError('Expected one anchor; this file was not written: ' + old[:90])
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

def auth(t):
    old = '''AnimatedVisibility(
            visible = registering,
            enter = fadeIn(tween(220)) + expandVertically(animationSpec = spring(dampingRatio = 0.9f, stiffness = 320f)),
            exit = fadeOut(tween(140)) + shrinkVertically(animationSpec = tween(220))
        ) {'''
    if t.count(old) != 2:
        raise RuntimeError('Expected two animated registration fields')
    t = t.replace(old, 'if (registering) {')
    t = once(t, '    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {', '''    // AUTH_UNIFIED_HEIGHT_V5: a single size animation, not two competing field springs.
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(
            animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {''')
    if 'import androidx.compose.animation.animateContentSize\n' not in t:
        t = once(t, 'package com.example.ui.screens\n', 'package com.example.ui.screens\n\nimport androidx.compose.animation.animateContentSize\n')
    return t

def main(t):
    t = once(t, '''        setContent {
            QuadrinheiroApp()
        }''', '''        setContent {
            // APP_BACKGROUND_FOCUS_V5: settings and inline sheets inherit background tap dismissal.
            Box(Modifier.fillMaxSize().dismissFocusOnBackgroundTap()) {
                QuadrinheiroApp()
            }
        }''')
    return once(t, 'package com.example\n', 'package com.example\n\nimport com.example.ui.components.dismissFocusOnBackgroundTap\n')

edit('app/src/main/java/com/example/ui/screens/AuthSubcomponents.kt', 'AUTH_UNIFIED_HEIGHT_V5', auth)
edit('app/src/main/java/com/example/MainActivity.kt', 'APP_BACKGROUND_FOCUS_V5', main)

out = []
p = Path('app/src/main/java/com/example/ui/screens/SettingsScreen.kt')
lines = p.read_text(encoding='utf-8').splitlines()
for i, line in enumerate(lines):
    if ('profile.handle' in line or 'copy(handle' in line or '"profile" ->' in line or 'Dialog(' in line):
        out.append('=== SettingsScreen.kt:%d' % (i + 1))
        out.extend(lines[max(0, i - 18):min(len(lines), i + 32)])
p = Path('app/src/main/java/com/example/viewmodel/QuadrinheiroViewModel.kt')
lines = p.read_text(encoding='utf-8').splitlines()
for i, line in enumerate(lines):
    if ('fun updateProfile(' in line or 'fun applyAuthenticatedSessionAndMergeLocalData(' in line):
        out.append('=== QuadrinheiroViewModel.kt:%d' % (i + 1))
        out.extend(lines[max(0, i - 2):min(len(lines), i + 85)])
Path('.audit').mkdir(exist_ok=True)
Path('.audit/profile_editing.txt').write_text('\n'.join(out), encoding='utf-8')
Path('.audit/focus_and_motion.txt').write_text('\n'.join(report), encoding='utf-8')
