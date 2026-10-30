from pathlib import Path

pending = {}
report = []
def once(text, old, new):
    if text.count(old) != 1:
        raise RuntimeError('Hotspot anchor not unique; no source files written: ' + old[:100])
    return text.replace(old, new, 1)

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

def covers(text):
    text = once(text, '        val bmp = withContext(Dispatchers.IO) {\n            runCatching {\n                val req = ImageRequest.Builder(context)\n                    .data(rawUrl)\n                    .size(64, 64)\n                    .allowHardware(false)\n                    .build()\n                val result = Coil.imageLoader(context).execute(req)\n                (result.drawable as? BitmapDrawable)?.bitmap\n            }.getOrNull()\n        } ?: return null\n\n        // Cálculo de média ponderada e saturação de cores obrigatório em Dispatchers.Default\n        val computedColor = calculateAverageCoverColor(bmp, dispatcher)', '''        // AMBIENT_COVER_EXTRACTION_V17: hardware-rendered ambient bitmap, memory caching, non-blocking Default pool.
        val computedColor = withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(context)
                    .data(rawUrl)
                    .size(64, 64)
                    .allowHardware(false)
                    .build()
                val result = Coil.imageLoader(context).execute(req)
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
                calculateAverageCoverColor(bmp, dispatcher)
            }.getOrNull()
        }''')
    return text

def lists(text):
    text = once(text, '            val issueNum = remember(comic.id, comic.title, comic.issueNumber) {\n                SeriesSequenceEngine.inferIssueNumber(comic)\n            }', '''            // COMIC_DERIVED_STATE_V17: remember expensive sequence inference per stable item key.
            val issueNum = remember(comic.id, comic.issueNumber, comic.title) {
                SeriesSequenceEngine.inferIssueNumber(comic)
            }''')
    return text

plan('app/src/main/java/com/example/ui/components/AmbientImageProcessor.kt', 'AMBIENT_COVER_EXTRACTION_V17', covers)
plan('app/src/main/java/com/example/ui/screens/MainTabsScreens.kt', 'COMIC_DERIVED_STATE_V17', lists)

for path, content in pending.items():
    path.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/frametime_hotspots_v17.txt').write_text('\n'.join(report) + '\nOptimizations keep full visual Liquid Glass, backdrop layers, and current layout trees. On-device frame timing still requires real S23 testing with the signed release build.\n', encoding='utf-8')
