import pathlib

LF = chr(10)
out = []
files = sorted(pathlib.Path('app/src').rglob('*.kt'))
cache = {f: f.read_text(encoding='utf-8', errors='ignore').splitlines() for f in files}

# 1) definicoes de controles segmentados / pilulas
n = 0
for f in files:
    lines = cache[f]
    for i, line in enumerate(lines):
        if 'fun ' in line and ('Segment' in line or 'Pill' in line or 'GlassButton' in line) and n < 4:
            out.append('=== DEF %s:%d' % (f, i + 1))
            out.extend(lines[max(0, i - 1):min(len(lines), i + 55)])
            n += 1

# 2) clickable logo depois de padding (area de toque menor que o visual)
hits = 0
for f in files:
    if 'ui' not in str(f):
        continue
    lines = cache[f]
    for i, line in enumerate(lines):
        if '.clickable' in line:
            prev = lines[max(0, i - 3):i]
            if any('.padding(' in p for p in prev) and hits < 25:
                out.append('=== PADDING-BEFORE-CLICKABLE %s:%d' % (f, i + 1))
                out.extend(lines[max(0, i - 4):min(len(lines), i + 3)])
                hits += 1

pathlib.Path('.audit').mkdir(exist_ok=True)
pathlib.Path('.audit/touch_targets.txt').write_text(LF.join(out)[:26000], encoding='utf-8')
