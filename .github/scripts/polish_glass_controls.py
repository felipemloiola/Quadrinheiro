from pathlib import Path
import hashlib
import subprocess

# Preserve the already-applied Settings logo; revert only the shared gesture regression.
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
if current_blob == original_blob:
    status = 'SKIP: original shared gesture detector already restored'
elif current_blob == regression_blob:
    p.write_bytes(original)
    status = 'RESTORED: exact shared gesture detector from before the global drag patch'
else:
    raise RuntimeError('Gesture detector changed since review; refusing overwrite: ' + current_blob)

Path('.audit').mkdir(exist_ok=True)
Path('.audit/glass_controls_v8.txt').write_text(
    status + '\nSettings logo preserved. Global exclusive-drag override disabled.\n'
    'Device verification pending: hold and drag the bottom tab indicator; release to select; tap tabs.\n'
    'Button-only scroll locking is deferred until isolated from tab bar gesture handlers.\n',
    encoding='utf-8'
)
