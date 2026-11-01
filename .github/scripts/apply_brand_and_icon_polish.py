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

def patch_ic_launcher_bg(text):
    # Fundo do ícone do app no modo escuro totalmente preto (#FF000000)
    text = text.replace('#FFF8FAFC', '#FF000000')
    text = text.replace('#FFEEF2F7', '#FF000000')
    text = text.replace('#FFDCE4EF', '#FF000000')
    return text

def patch_social_share(text):
    # Alinhamento vertical perfeito da logo da marca com o nome Quadrinheiro no card de compartilhamento
    # Desce levemente a logo (de topY + 4f para topY + 14f em Stories e de topY + 3f para topY + 11f em Feed)
    text = text.replace(
        'val logoTop = topY + (if (isStories) 4f else 3f)',
        'val logoTop = topY + (if (isStories) 13f else 10f)'
    )
    return text

def patch_welcome(text):
    # Padroniza a tipografia da marca "Quadrinheiro" para o estilo oficial com QuadrinheiroBrandText
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

plan('app/src/main/res/values/ic_launcher_colors.xml', '#FF000000', patch_ic_launcher_bg)
plan('app/src/main/java/com/example/ui/components/SocialShareCardSheet.kt', 'topY + (if (isStories) 13f else 10f)', patch_social_share)
plan('app/src/main/java/com/example/ui/screens/WelcomeIntroScreen.kt', 'com.example.ui.components.QuadrinheiroBrandText', patch_welcome)

for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)

Path('.audit').mkdir(exist_ok=True)
Path('.audit/ui_brand_and_icon_polish_v21.txt').write_text('\n'.join(report) + '\nLauncher icon background darkened to pure black in dark theme, brand typography unified across screens, and social share brand mark aligned.\n', encoding='utf-8')
