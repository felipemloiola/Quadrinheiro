from pathlib import Path
import hashlib
import subprocess
import re

# Preserve the Settings logo and restore the original shared tab-bar gesture behavior.
path = 'app/src/main/java/com/kyant/backdrop/catalog/utils/DragGestureInspector.kt'
source_commit = '579b73a5e85090378951675fad40badd42c30d27'
original_blob = '74a58e67de81ccfb9bb2d7b0bcc3925ad5952049'
regression_blob = '9cc7000543ddead98be3b9c425d9c730b802771f'

def blob_sha(raw):
    return hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()

original = subprocess.check_output(['git', 'show', source_commit + ':' + path])
if blob_sha(original) != original_blob:
    raise RuntimeError('Historical gesture source failed verification; no file written')
p = Path(path)
current = p.read_bytes()
current_blob = blob_sha(current)
pending = {}
if current_blob == original_blob:
    status = 'SKIP: original shared gesture detector already restored'
elif current_blob == regression_blob:
    pending[p] = original
    status = 'RESTORED: exact shared gesture detector from before the global drag patch'
else:
    raise RuntimeError('Gesture detector changed since review; refusing overwrite: ' + current_blob)

# Match balanced Kotlin groups while ignoring string literals and comments.
tokens = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\x27(?:\\.|[^\x27\\])*\x27|//[^\n]*|/\*[\s\S]*?\*/|[(){}]')
def end_group(text, start):
    stack = []
    pairs = {')': '(', '}': '{'}
    for token in tokens.finditer(text, start):
        value = token.group()
        if value in ('(', '{'):
            stack.append(value)
        elif value in (')', '}'):
            if not stack or stack.pop() != pairs[value]:
                raise RuntimeError('Unbalanced Kotlin group; no source files written')
            if not stack:
                return token.end()
    raise RuntimeError('Unclosed Kotlin group; no source files written')

modal_path = Path('app/src/main/java/com/example/ui/components/CatalogModalSubcomponents.kt')
raw = modal_path.read_bytes()
text = raw.decode('utf-8').replace('\r\n', '\n')
marker = 'SINGLE_PAGE_PROGRESS_CONTROL_V9'
removed = ''
if marker not in text:
    declarations = list(re.finditer(r'(?m)^(?:(?:internal|private|public)\s+)?fun\s+CatalogReadingProgressSection\s*\(', text))
    if len(declarations) != 1:
        raise RuntimeError('Expected exactly one reading-progress section; no source files written')
    declaration = declarations[0]
    signature_end = end_group(text, text.index('(', declaration.start()))
    body_start = text.index('{', signature_end)
    body_end = end_group(text, body_start)
    body = text[body_start:body_end]
    if 'AppleSlider(' not in body:
        raise RuntimeError('Page slider not identified; no source files written')
    candidates = []
    for match in re.finditer(r'\bBox\s*\(', body):
        start = match.start()
        end = end_group(body, body.index('(', start))
        call = body[start:end]
        if '.drawWithCache' not in call or 'currentPageProvider' not in call:
            continue
        following = end
        while following < len(body) and body[following].isspace():
            following += 1
        if following < len(body) and body[following] == '{':
            content_end = end_group(body, following)
            if body[following + 1:content_end - 1].strip():
                raise RuntimeError('Progress Box contains other controls; refusing removal')
            end = content_end
        candidates.append((start, end))
    if len(candidates) != 1:
        raise RuntimeError('Expected one standalone deferred progress bar, found %d; no source files written' % len(candidates))
    start, end = candidates[0]
    removed = body[start:end]
    replacement = '// SINGLE_PAGE_PROGRESS_CONTROL_V9: keep only the interactive page slider.'
    new_body = body[:start] + replacement + body[end:]
    if new_body.count('AppleSlider(') != body.count('AppleSlider('):
        raise RuntimeError('Slider preservation check failed; no source files written')
    result = text[:body_start] + new_body + text[body_end:]
    pending[modal_path] = (result.replace('\n', '\r\n') if b'\r\n' in raw else result).encode('utf-8')
    progress_status = 'REMOVED: standalone duplicate progress bar; interactive slider preserved'
else:
    progress_status = 'SKIP: duplicate progress bar already removed'

for target, content in pending.items():
    target.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/glass_controls_v8.txt').write_text(status + '\nSettings logo preserved. Global exclusive-drag override disabled.\nDevice verification pending for tab-bar hold/drag behavior.\n', encoding='utf-8')
Path('.audit/page_progress_v9.txt').write_text(progress_status + '\nDevice verification pending: only one page slider; minus/plus and saving still work.\nRemoved source:\n' + removed, encoding='utf-8')
