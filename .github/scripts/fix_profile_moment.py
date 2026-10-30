from pathlib import Path
import re

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
                raise RuntimeError('Unbalanced profile group; no source files written')
            if not stack:
                return token.end()
    raise RuntimeError('Unclosed profile group; no source files written')

def profile(text):
    launches = list(re.finditer(r'\bavatarPickerLauncher\.launch\s*\(', text))
    if len(launches) != 3:
        raise RuntimeError('Expected camera plus two redundant photo actions; found %d' % len(launches))
    ranges = []
    for launch in launches[1:]:
        rows = list(re.finditer(r'\bRow\s*\(', text[:launch.start()]))
        if not rows:
            raise RuntimeError('Photo action Row not found')
        start = rows[-1].start()
        signature_end = end_group(text, text.index('(', start))
        body_start = signature_end
        while body_start < len(text) and text[body_start].isspace():
            body_start += 1
        if text[body_start:body_start + 1] != '{':
            raise RuntimeError('Photo action Row content not found')
        end = end_group(text, body_start)
        block = text[start:end]
        if not start < launch.start() < end or block.count('avatarPickerLauncher.launch') != 1:
            raise RuntimeError('Photo action is not isolated')
        if any(value in block for value in ('UserAvatar(', 'Remover foto', 'Icons.Default.DeleteOutline')):
            raise RuntimeError('Refusing to remove avatar or remove-photo control')
        ranges.append((start, end))
    ranges.sort()
    if len(ranges) != 2 or ranges[0][1] > ranges[1][0]:
        raise RuntimeError('Photo action ranges overlap')
    result = text
    for start, end in reversed(ranges):
        result = result[:start] + '// PROFILE_SINGLE_PHOTO_ACTION_V11: use the camera beside the avatar.' + result[end:]
    if len(re.findall(r'\bavatarPickerLauncher\.launch\s*\(', result)) != 1:
        raise RuntimeError('Camera preservation check failed')
    if result.count('text = "Remover foto"') != text.count('text = "Remover foto"'):
        raise RuntimeError('Remove-photo preservation check failed')
    return result

def moment(text):
    old = 'onChangePage = { v -> draftComic = draftComic.copy(currentPage = v) }'
    if text.count(old) != 1:
        raise RuntimeError('Expected one reading-progress callback; no source files written')
    new = '''onChangePage = { v ->
                                    // MOMENT_PAGE_ONE_WAY_V11: progress updates the note page, never the reverse.
                                    val nextPage = v.coerceIn(0, draftComic.pageCount.coerceAtLeast(1))
                                    draftComic = draftComic.copy(currentPage = nextPage)
                                    momentPageInput = nextPage.toString()
                                }'''
    return text.replace(old, new, 1)

plan('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'PROFILE_SINGLE_PHOTO_ACTION_V11', profile)
plan('app/src/main/java/com/example/ui/components/CatalogModal.kt', 'MOMENT_PAGE_ONE_WAY_V11', moment)
for p, content in pending.items():
    p.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/profile_moment_fix_v11.txt').write_text('\n'.join(report) + '\nFont and shared gestures unchanged. Runtime checks pending: camera works with/without photo; removal still works; slider and stepper update moment page; typing moment page does not change reading progress; saving preserves both values.\n', encoding='utf-8')
