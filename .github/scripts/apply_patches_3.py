from pathlib import Path

report = []
def once(t, old, new):
    if t.count(old) != 1:
        raise RuntimeError('Expected one anchor; nothing written for this file: ' + old[:90])
    return t.replace(old, new, 1)

def edit(path, marker, fn):
    p = Path(path)
    raw = p.read_bytes().decode('utf-8')
    crlf = '\r\n' in raw
    t = raw.replace('\r\n', '\n')
    if marker in t:
        report.append('SKIP: ' + path)
        return
    t = fn(t)
    p.write_bytes(t.replace('\n', '\r\n').encode('utf-8') if crlf else t.encode('utf-8'))
    report.append('OK: ' + path)

def auth(t):
    t = once(t, 'QuadrinheiroLogo(variant = QuadrinheiroLogoVariant.Primary, titleVisible = false)', 'QuadrinheiroLogo(variant = QuadrinheiroLogoVariant.Primary, titleVisible = false, modifier = Modifier.size(104.dp))')
    start = t.index('@Composable\nfun AuthModeSegmentedToggle(')
    end = t.index('@Composable\nfun AuthFeedbackBannerCard(', start)
    t = t[:start] + '''@Composable
fun AuthModeSegmentedToggle(selectedMode: AuthMode, onSelectMode: (AuthMode) -> Unit) {
    // AUTH_CIRCULAR_ACTIONS_V3: use the app's existing glass segmented picker.
    com.example.ui.components.AppleSegmentedPicker(
        options = AuthMode.values().map {
            com.example.ui.components.SegmentOption(it, if (it == AuthMode.LOGIN) "Entrar" else "Criar conta")
        },
        value = selectedMode,
        onChange = onSelectMode,
        modifier = Modifier.fillMaxWidth()
    )
}

''' + t[end:]
    old = '''        OutlinedButton(
            onClick = onLoginWithGoogle, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_google_button")
        ) { Text("Continuar com Google", color = theme.textPrimary) }
        if (!isLinkingFromGuest) TextButton(
            onClick = onContinueAsGuest, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_guest_button")
        ) { Text("Explorar como convidado", color = theme.textSecondary) }'''
    new = '''        Spacer(Modifier.height(6.dp))
        AuthAlternativeActions(
            enabled = !isLoading,
            showGuest = !isLinkingFromGuest,
            onGoogle = onLoginWithGoogle,
            onGuest = onContinueAsGuest
        )'''
    return once(t, old, new)

def shell(t):
    t = once(t, 'private fun AuthGlassScreenShell(\n', 'private fun AuthGlassScreenShellContent(\n')
    return t + '''

// AUTH_SPRING_ENTRANCE_V3
@Composable
private fun AuthGlassScreenShell(
    isLinkingFromGuest: Boolean,
    onDismissModal: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    AuthGlassEntrance {
        AuthGlassScreenShellContent(isLinkingFromGuest, onDismissModal, content)
    }
}
'''

edit('app/src/main/java/com/example/ui/screens/AuthSubcomponents.kt', 'AUTH_CIRCULAR_ACTIONS_V3', auth)
edit('app/src/main/java/com/example/ui/screens/AuthScreen.kt', 'AUTH_SPRING_ENTRANCE_V3', shell)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/auth_refinement.txt').write_text('\n'.join(report), encoding='utf-8')
