from pathlib import Path

report = []

def edit(path, marker, transform):
    p = Path(path)
    raw = p.read_bytes().decode('utf-8')
    crlf = '\r\n' in raw
    t = raw.replace('\r\n', '\n')
    if marker in t:
        report.append('SKIP: ' + path)
        return
    result = transform(t)
    if crlf:
        result = result.replace('\n', '\r\n')
    p.write_bytes(result.encode('utf-8'))
    report.append('OK: ' + path)

def once(t, old, new):
    if t.count(old) != 1:
        raise RuntimeError('Expected one anchor; no write: ' + old[:90])
    return t.replace(old, new, 1)

def picker(t):
    a = t.index('fun <T> AppleMenuPicker(')
    b = t.find('\n@Composable', a)
    if b < 0:
        b = len(t)
    s = t[a:b]
    s = once(s, '    val animationScope = rememberCoroutineScope()', '''    // GLASS_PRESS_FEEDBACK_V2: one touch handler for both click and press.
    val pickerInteractions = remember { MutableInteractionSource() }
    val pickerPressed by pickerInteractions.collectIsPressedAsState()
    val pickerPressProgress by animateFloatAsState(
        targetValue = if (pickerPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "pickerGlassPress"
    )
    val animationScope = rememberCoroutineScope()''')
    s = s.replace('triggerHighlight.pressProgress', 'pickerPressProgress')
    s = once(s, '.then(triggerHighlight.modifier)', '// Press animation is driven by the clickable interaction source.')
    s = once(s, 'interactionSource = remember { MutableInteractionSource() },', 'interactionSource = pickerInteractions,')
    if 'SORT_PICKER_SINGLE_TOUCH_HANDLER' not in s or '.heightIn(min = 48.dp)' not in s:
        raise RuntimeError('Previous touch-area patch missing; refusing to overwrite')
    return t[:a] + s + t[b:]

def auth(t):
    t = once(t, 'import com.kyant.backdrop.Backdrop', '''import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.graphics.Brush
import com.example.ui.components.liquidGlass''')
    t = once(t, '''    // Static background: no continuous animation, image decoding or blur.
    Box(Modifier.fillMaxSize().background(LocalIOS27Theme.current.bgPrimary))''', '''    // GLASS_AUTH_STYLE_V2: static theme gradient recorded for the glass surfaces.
    val theme = LocalIOS27Theme.current
    val layerModifier = if (backdrop is LayerBackdrop) Modifier.layerBackdrop(backdrop) else Modifier
    Box(layerModifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(
            theme.bgPrimary,
            androidx.compose.ui.graphics.lerp(theme.bgPrimary, theme.accent, if (reduceTransparency) 0.03f else 0.12f),
            theme.bgPrimary
        ))
    ))''')
    t = once(t, '    background(LocalIOS27Theme.current.bgPrimary, RoundedCornerShape(22.dp))', '    liquidGlass(shape = RoundedCornerShape(26.dp), elevation = 0.dp)')
    t = once(t, '    val focus = LocalFocusManager.current', '    val theme = LocalIOS27Theme.current\n    val focus = LocalFocusManager.current')
    t = t.replace('OutlinedTextField(\n', '''OutlinedTextField(
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedContainerColor = theme.textPrimary.copy(alpha = 0.05f),
                unfocusedContainerColor = theme.textPrimary.copy(alpha = 0.04f),
                focusedBorderColor = theme.accent.copy(alpha = 0.45f),
                unfocusedBorderColor = theme.textPrimary.copy(alpha = 0.12f),
                focusedLabelColor = theme.accent,
                unfocusedLabelColor = theme.textSecondary,
                cursorColor = theme.accent
            ),
''')
    t = once(t, 'CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = theme.textPrimary)', 'com.example.ui.components.IOSLoadingIndicator(Modifier.size(20.dp), color = theme.textPrimary)')
    return t

def loading(t):
    return once(t, 'androidx.compose.material3.CircularProgressIndicator(\n                    color = themeColors.accent,', 'com.example.ui.components.IOSLoadingIndicator(\n                    color = themeColors.textSecondary,') + '\n// IOS_SESSION_INDICATOR_V2\n'

edit('app/src/main/java/com/example/ui/components/LiquidGlassComponents.kt', 'GLASS_PRESS_FEEDBACK_V2', picker)
edit('app/src/main/java/com/example/ui/screens/AuthSubcomponents.kt', 'GLASS_AUTH_STYLE_V2', auth)
edit('app/src/main/java/com/example/MainActivity.kt', 'IOS_SESSION_INDICATOR_V2', loading)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/glass_restore.txt').write_text('\n'.join(report), encoding='utf-8')
