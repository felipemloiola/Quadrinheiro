import pathlib

CRLF = chr(13) + chr(10)
LF = chr(10)
MAIN = 'app/src/main/java/com/example/MainActivity.kt'
REPORT = []

p = pathlib.Path(MAIN)
raw = p.read_bytes().decode('utf-8')
crlf = CRLF in raw
t = raw.replace(CRLF, LF)
old = '.blur(14.dp)'
marker = 'LAG_FIX_NO_FULLSCREEN_BLUR'
if marker in t:
    REPORT.append('SKIP: blur already removed')
elif t.count(old) != 1:
    REPORT.append('FAIL: .blur(14.dp) found %d times' % t.count(old))
else:
    t = t.replace(old, '/* ' + marker + ': 28x42 source is already soft when upscaled */')
    if crlf:
        t = t.replace(LF, CRLF)
    p.write_bytes(t.encode('utf-8'))
    REPORT.append('OK: removed fullscreen blur in MainActivity.kt')
pathlib.Path('.audit').mkdir(exist_ok=True)
pathlib.Path('.audit/patch_report_2.txt').write_text(LF.join(REPORT), encoding='utf-8')
