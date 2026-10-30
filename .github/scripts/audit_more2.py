import pathlib

LF = chr(10)
out = []
for f in sorted(pathlib.Path('app/src').rglob('*.kt')):
    lines = f.read_text(encoding='utf-8', errors='ignore').splitlines()
    for i, line in enumerate(lines):
        if 'fun ' in line and 'AppleMenuPicker' in line:
            out.append('=== %s:%d (lines +120..+300)' % (f, i + 1))
            out.extend(lines[i + 120:min(len(lines), i + 300)])
pathlib.Path('.audit').mkdir(exist_ok=True)
pathlib.Path('.audit/menu_picker_2.txt').write_text(LF.join(out)[:40000], encoding='utf-8')
