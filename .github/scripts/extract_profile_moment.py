from pathlib import Path
import re

root = Path('app/src/main/java/com/example')
terms = [
    r'Escolher foto', r'avatarPickerLauncher', r'Acabamento do emblema',
    r'momentPageInput', r'Momentos? Marcantes?', r'currentPageProvider', r'CatalogReadingProgressSection'
]
out = []
for path in sorted(root.rglob('*.kt')):
    text = path.read_text(encoding='utf-8', errors='replace').replace('\r\n', '\n')
    lines = text.split('\n')
    hits = [i for i, line in enumerate(lines) if any(re.search(term, line) for term in terms)]
    used = set()
    for i in hits:
        start, end = max(0, i - 25), min(len(lines), i + 45)
        if any(n in used for n in range(start, end)):
            continue
        used.update(range(start, end))
        out.append('=== %s:%d-%d' % (path.as_posix(), start + 1, end))
        out.extend('%5d  %s' % (n + 1, lines[n]) for n in range(start, end))

typography = []
for path in [root / 'ui/theme/Type.kt', root / 'ui/theme/Theme.kt']:
    if path.exists():
        typography.append('=== ' + path.as_posix())
        typography.append(path.read_text(encoding='utf-8', errors='replace'))
Path('.audit').mkdir(exist_ok=True)
Path('.audit/profile_moment_excerpts.txt').write_text('\n'.join(out)[:180000], encoding='utf-8')
Path('.audit/typography_sources.txt').write_text('\n'.join(typography)[:80000], encoding='utf-8')
