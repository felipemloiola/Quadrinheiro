import pathlib

LF = chr(10)
out = []


def dump(pattern, before, after, limit):
    n = 0
    for f in sorted(pathlib.Path('app/src').rglob('*.kt')):
        lines = f.read_text(encoding='utf-8', errors='ignore').splitlines()
        for i, line in enumerate(lines):
            if pattern in line:
                out.append('=== %s:%d [%s]' % (f, i + 1, pattern))
                out.extend(lines[max(0, i - before):min(len(lines), i + after)])
                n += 1
                if n >= limit:
                    return


dump('fun AppleMenuPicker', 3, 110, 1)
dump('hasReadingCoverBg', 2, 45, 3)
pathlib.Path('.audit').mkdir(exist_ok=True)
pathlib.Path('.audit/more.txt').write_text(LF.join(out)[:40000], encoding='utf-8')
