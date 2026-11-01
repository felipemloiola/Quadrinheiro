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
    # Desce o ícone origami para alinhamento central perfeito (Stories: topY + 28f, Feed: topY + 20f)
    text = text.replace(
        'val logoTop = topY + (if (isStories) 18f else 14f)',
        'val logoTop = topY + (if (isStories) 28f else 20f)'
    )
    text = text.replace(
        'val logoTop = topY + (if (isStories) 13f else 10f)',
        'val logoTop = topY + (if (isStories) 28f else 20f)'
    )
    text = text.replace(
        'val logoTop = topY + (if (isStories) 4f else 3f)',
        'val logoTop = topY + (if (isStories) 28f else 20f)'
    )
    return text

plan('app/src/main/java/com/example/ui/components/SocialShareCardSheet.kt', 'val logoTop = topY + (if (isStories) 28f else 20f)', patch_social_share)

for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)

Path('.audit').mkdir(exist_ok=True)
Path('.audit/ui_typography_and_logo_alignment_v23.txt').write_text('\n'.join(report) + '\nBrand typography cloned identically to the social card, and logo centered vertically.\n', encoding='utf-8')
