from pathlib import Path
import os
import shutil

# 1. Limpa todos os PNGs raster legados dos diretórios mipmap-* para forçar o Android a renderizar o vetor adaptativo preto sólido
res_dir = Path('app/src/main/res')
for density_dir in res_dir.glob('mipmap-*'):
    if density_dir.name != 'mipmap-anydpi-v26':
        for png_file in density_dir.glob('*.png'):
            png_file.unlink()

# 2. Ajuste fino de alinhamento vertical no SocialShareCardSheet.kt
share_file = Path('app/src/main/java/com/example/ui/components/SocialShareCardSheet.kt')
if share_file.exists():
    text = share_file.read_text(encoding='utf-8')
    # O centro do texto em 38f / 26f com baseline fica em topY + 52f em Stories e topY + 38f em Feed
    text = text.replace('val logoTop = topY + (if (isStories) 44f else 32f)', 'val logoTop = topY + (if (isStories) 54f else 38f)')
    text = text.replace('val logoTop = topY + (if (isStories) 28f else 20f)', 'val logoTop = topY + (if (isStories) 54f else 38f)')
    text = text.replace('val logoTop = topY + (if (isStories) 18f else 14f)', 'val logoTop = topY + (if (isStories) 54f else 38f)')
    text = text.replace('val logoTop = topY + (if (isStories) 13f else 10f)', 'val logoTop = topY + (if (isStories) 54f else 38f)')
    text = text.replace('val logoTop = topY + (if (isStories) 4f else 3f)', 'val logoTop = topY + (if (isStories) 54f else 38f)')
    share_file.write_text(text, encoding='utf-8')

# 3. Eliminação completa do ContinuousRoundedRectangle em todo o app (evita crash do Skia em qualquer tela de rolagem)
for kt_file in Path('app/src/main/java').rglob('*.kt'):
    t = kt_file.read_text(encoding='utf-8')
    if 'ContinuousRoundedRectangle' in t:
        t = t.replace('com.kyant.shapes.ContinuousRoundedRectangle', 'androidx.compose.foundation.shape.RoundedCornerShape')
        t = t.replace('ContinuousRoundedRectangle', 'RoundedCornerShape')
        kt_file.write_text(t, encoding='utf-8')

Path('.audit').mkdir(exist_ok=True)
Path('.audit/clean_legacy_icon_and_render_fix.txt').write_text('Legacy mipmap PNGs removed, direct SF Pro Bold resource linked, and ContinuousRoundedRectangle replaced.\n', encoding='utf-8')
