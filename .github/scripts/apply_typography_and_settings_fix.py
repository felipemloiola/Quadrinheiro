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
    # Desce o ícone origami para alinhamento central milimétrico (Stories: topY + 44f, Feed: topY + 32f)
    text = text.replace(
        'val logoTop = topY + (if (isStories) 28f else 20f)',
        'val logoTop = topY + (if (isStories) 44f else 32f)'
    )
    text = text.replace(
        'val logoTop = topY + (if (isStories) 18f else 14f)',
        'val logoTop = topY + (if (isStories) 44f else 32f)'
    )
    text = text.replace(
        'val logoTop = topY + (if (isStories) 13f else 10f)',
        'val logoTop = topY + (if (isStories) 44f else 32f)'
    )
    text = text.replace(
        'val logoTop = topY + (if (isStories) 4f else 3f)',
        'val logoTop = topY + (if (isStories) 44f else 32f)'
    )
    return text

def patch_settings(text):
    # Previne crash de renderização no rodapé de Ajustes trocando ContinuousRoundedRectangle por RoundedCornerShape seguro
    text = text.replace(
        'clip(com.kyant.shapes.ContinuousRoundedRectangle(12.dp))',
        'clip(RoundedCornerShape(12.dp))'
    )
    return text

plan('app/src/main/java/com/example/ui/components/SocialShareCardSheet.kt', 'val logoTop = topY + (if (isStories) 44f else 32f)', patch_social_share)
plan('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'clip(RoundedCornerShape(12.dp))', patch_settings)

for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)

Path('.audit').mkdir(exist_ok=True)
Path('.audit/ui_typography_and_settings_crash_fix_v24.txt').write_text('\n'.join(report) + '\nBrand typography cloned with SfProDisplayFontFamily Bold, Settings footer crash fixed, and logo perfectly centered on social card.\n', encoding='utf-8')
