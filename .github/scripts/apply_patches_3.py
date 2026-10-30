from pathlib import Path

report = []
def once(t, old, new):
    if t.count(old) != 1:
        raise RuntimeError('Expected one anchor; nothing written for this file: ' + old[:100])
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

def subcomponents(t):
    t = once(t, 'modifier = Modifier.size(104.dp)', 'modifier = Modifier.size(120.dp)')
    t = once(t, 'Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp))', 'Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp))')
    for imp in ['androidx.compose.animation.AnimatedVisibility', 'androidx.compose.animation.fadeIn', 'androidx.compose.animation.fadeOut', 'androidx.compose.animation.expandVertically', 'androidx.compose.animation.shrinkVertically', 'androidx.compose.animation.core.spring', 'androidx.compose.animation.core.tween']:
        if 'import ' + imp + '\n' not in t:
            t = once(t, 'package com.example.ui.screens\n', 'package com.example.ui.screens\n\nimport ' + imp + '\n')
    anchor = 'if (registering) OutlinedTextField('
    if t.count(anchor) != 2:
        raise RuntimeError('Expected exactly two registration-only fields; nothing written')
    for _ in range(2):
        a = t.index(anchor)
        call_start = a + len('if (registering) ')
        pos = call_start + len('OutlinedTextField(')
        depth = 1
        quoted = False
        escaped = False
        while depth and pos < len(t):
            c = t[pos]
            if quoted:
                if escaped:
                    escaped = False
                elif c == '\\':
                    escaped = True
                elif c == '"':
                    quoted = False
            elif c == '"':
                quoted = True
            elif c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
            pos += 1
        if depth:
            raise RuntimeError('Unbalanced field call; nothing written')
        replacement = '''AnimatedVisibility(
            visible = registering,
            enter = fadeIn(tween(220)) + expandVertically(animationSpec = spring(dampingRatio = 0.9f, stiffness = 320f)),
            exit = fadeOut(tween(140)) + shrinkVertically(animationSpec = tween(220))
        ) {
            ''' + t[call_start:pos] + '\n        }'
        t = t[:a] + replacement + t[pos:]
    return t + '\n// AUTH_FORM_MOTION_V4\n'

def screen(t):
    for imp in ['androidx.compose.foundation.gestures.awaitEachGesture', 'androidx.compose.foundation.gestures.awaitFirstDown', 'androidx.compose.foundation.gestures.waitForUpOrCancellation', 'androidx.compose.ui.input.pointer.PointerEventPass', 'androidx.compose.ui.input.pointer.pointerInput']:
        if 'import ' + imp + '\n' not in t:
            t = once(t, 'package com.example.ui.screens\n', 'package com.example.ui.screens\n\nimport ' + imp + '\n')
    t = once(t, '    val focusManager = LocalFocusManager.current', '    val focusManager = LocalFocusManager.current\n    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current')
    t = once(t, '.testTag("auth_screen_root")', '''.testTag("auth_screen_root")
                // AUTH_OUTSIDE_FOCUS_V4: observe unconsumed taps, never steal field/button taps.
                .pointerInput(focusManager, keyboard) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        if (!down.isConsumed) {
                            val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                            if (up != null && !up.isConsumed) {
                                focusManager.clearFocus()
                                keyboard?.hide()
                            }
                        }
                    }
                }''')
    t = once(t, '''                    onSelectMode = { nextMode ->
                        authMode = nextMode''', '''                    onSelectMode = { nextMode ->
                        focusManager.clearFocus()
                        keyboard?.hide()
                        authMode = nextMode''')
    t = once(t, '''                AuthBrandHeader(isLinkingFromGuest = isLinkingFromGuest)

                Spacer(modifier = Modifier.height(10.dp))''', '''                AuthBrandHeader(isLinkingFromGuest = isLinkingFromGuest)

                Spacer(modifier = Modifier.height(16.dp))''')
    return t

edit('app/src/main/java/com/example/ui/screens/AuthSubcomponents.kt', 'AUTH_FORM_MOTION_V4', subcomponents)
edit('app/src/main/java/com/example/ui/screens/AuthScreen.kt', 'AUTH_OUTSIDE_FOCUS_V4', screen)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/auth_polish.txt').write_text('\n'.join(report), encoding='utf-8')
