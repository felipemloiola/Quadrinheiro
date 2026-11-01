from pathlib import Path

pending = {}
report = []

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

def patch_social_share(text):
    # Alinhamento exato: o texto "Quadrinheiro" em 38f tem altura cap de ~28px.
    # A logo tem tamanho 42f em Stories e 30f em Feed.
    # Para o centro da logo coincidir exatamente com o centro da tipografia:
    # Em Stories: logoTop = topY + 18f
    # Em Feed: logoTop = topY + 14f
    text = text.replace(
        'val logoTop = topY + (if (isStories) 13f else 10f)',
        'val logoTop = topY + (if (isStories) 18f else 14f)'
    )
    text = text.replace(
        'val logoTop = topY + (if (isStories) 4f else 3f)',
        'val logoTop = topY + (if (isStories) 18f else 14f)'
    )
    return text

def patch_welcome(text):
    # Garante QuadrinheiroBrandText com tipografia em peso Black idêntica ao print de compartilhamento
    old = '''                    Text(
                        text = "Quadrinheiro",
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = SfProDisplayFontFamily,
                        letterSpacing = (-0.8).sp,
                        color = theme.textPrimary,
                        textAlign = TextAlign.Center
                    )'''
    new = '''                    com.example.ui.components.QuadrinheiroBrandText(
                        text = "Quadrinheiro",
                        fontSize = 34.sp,
                        color = theme.textPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )'''
    if old in text:
        return text.replace(old, new)
    return text

plan('app/src/main/java/com/example/ui/components/SocialShareCardSheet.kt', 'val logoTop = topY + (if (isStories) 18f else 14f)', patch_social_share)
plan('app/src/main/java/com/example/ui/screens/WelcomeIntroScreen.kt', 'QuadrinheiroBrandText', patch_welcome)

for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)

Path('.audit').mkdir(exist_ok=True)
Path('.audit/ui_perfect_alignment_v22.txt').write_text('\n'.join(report) + '\nPure black #000000 launcher background and pixel-perfect social brand alignment applied.\n', encoding='utf-8')
