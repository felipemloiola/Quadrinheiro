from pathlib import Path

report = []
def once(t, old, new):
    if t.count(old) != 1:
        raise RuntimeError('Expected one anchor; this file not written: ' + old[:100])
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

def picker(t):
    a = t.index('fun <T> AppleMenuPicker(')
    b = t.find('\n@Composable', a)
    if b < 0:
        b = len(t)
    s = t[a:b]
    s = once(s, '    val pickerInteractions = remember { MutableInteractionSource() }', '''    // PICKER_PRESS_LIGHT_V6: observe finger position without consuming clicks.
    var pickerLightOrigin by remember { mutableStateOf(Offset.Zero) }
    val pickerInteractions = remember { MutableInteractionSource() }''')
    old = '''                .clip(ContinuousCapsule)
                .clickable('''
    new = '''                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        pickerLightOrigin = down.position
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.firstOrNull()?.let { pickerLightOrigin = it.position }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    val light = pickerPressProgress.coerceIn(0f, 1f)
                    if (light > 0.001f) {
                        val origin = Offset(
                            pickerLightOrigin.x.coerceIn(0f, size.width),
                            pickerLightOrigin.y.coerceIn(0f, size.height)
                        )
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.24f * light), Color.Transparent),
                                center = origin,
                                radius = size.maxDimension.coerceAtLeast(1f) * 0.75f
                            ),
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }
                }
                .clip(ContinuousCapsule)
                .clickable('''
    s = once(s, old, new)
    return t[:a] + s + t[b:]

def auth_id(t):
    return once(t, 'appContext.getString(R.string.default_web_client_id).trim()', 'GoogleClientId.optional(appContext) /* GOOGLE_CLIENT_ID_RUNTIME_V6 */')

def repo_id(t):
    old = 'appContext.getString(R.string.default_web_client_id)'
    if t.count(old) != 2:
        raise RuntimeError('Expected two Google client ID uses; repository not written')
    return t.replace(old, 'GoogleClientId.required(appContext) /* GOOGLE_CLIENT_ID_RUNTIME_V6 */')

edit('app/src/main/java/com/example/ui/components/LiquidGlassComponents.kt', 'PICKER_PRESS_LIGHT_V6', picker)
edit('app/src/main/java/com/example/data/QuadrinheiroAuthManager.kt', 'GOOGLE_CLIENT_ID_RUNTIME_V6', auth_id)
edit('app/src/main/java/com/example/data/QuadrinheiroCloudRepository.kt', 'GOOGLE_CLIENT_ID_RUNTIME_V6', repo_id)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/press_icon_build.txt').write_text('\n'.join(report), encoding='utf-8')
