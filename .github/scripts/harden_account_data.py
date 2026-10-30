from pathlib import Path
import json

pending = {}
report = []
def once(text, old, new):
    count = text.count(old)
    if count != 1:
        raise RuntimeError('Account hardening anchor count %d: %s' % (count, old[:100]))
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

def storage(text):
    helper = '''    // ACCOUNT_SCOPE_HARDENING_V13: inactive accounts never modify another account's mirror.
    private fun syncStateKeys(ownerUid: String?): List<String> {
        val session = loadAuthSession()
        if (ownerUid == "guest_local" ||
            (ownerUid == null && (isGuestMode() || session?.isGuest == true))) {
            return emptyList()
        }
        val resolvedUid = ownerUid?.takeIf { it.isNotBlank() }
            ?: session?.takeIf { !it.isGuest }?.uid
        return buildList {
            if (!resolvedUid.isNullOrBlank()) add(accountStateKey(resolvedUid))
            val isActiveOwner = session != null && !session.isGuest && session.uid == resolvedUid
            if (isActiveOwner || (ownerUid == null && resolvedUid.isNullOrBlank())) {
                add(STORAGE_KEY)
            }
        }
    }

'''
    text = once(text, '    @Synchronized\n    fun markComicsSynced(', helper + '    @Synchronized\n    fun markComicsSynced(')
    old_targets = '''        val resolvedUid = ownerUid?.takeIf { it.isNotBlank() && it != "guest_local" }
            ?: loadAuthSession()?.takeIf { !it.isGuest }?.uid
        val targetKeys = buildList {
            if (!resolvedUid.isNullOrBlank()) add(accountStateKey(resolvedUid))
            add(STORAGE_KEY)
        }'''
    if text.count(old_targets) != 2:
        raise RuntimeError('Expected two sync-target blocks; no files written')
    text = text.replace(old_targets, '        val targetKeys = syncStateKeys(ownerUid)')
    text = once(text, '''        if (!resolvedUid.isNullOrBlank()) {
            setItem(accountStateKey(resolvedUid), json)
            setItem(STORAGE_KEY, json)
            syncAccountMetadata(''', '''        if (!resolvedUid.isNullOrBlank()) {
            setItem(accountStateKey(resolvedUid), json)
            if (ownerUid == null || (activeSession != null && !activeSession.isGuest && activeSession.uid == resolvedUid)) {
                setItem(STORAGE_KEY, json)
            }
            syncAccountMetadata(''')
    text = once(text, '''                photoUrl = profile.avatarUrl.takeIf { it.isNotBlank() }
            )
        }
    }

    fun buildBackupJsonString(''', '''                photoUrl = profile.avatarUrl.takeIf { it.isNotBlank() }
            )
        } else {
            // Preserve legacy/local persistence when no account was selected.
            setItem(STORAGE_KEY, json)
        }
    }

    fun buildBackupJsonString(''')
    text = once(text, '                    if (displayName.isNotBlank()) {', '                    if (displayName.isNotBlank() && acctObj.optString("displayName", "") != displayName) {')
    text = once(text, '                    if (handle.isNotBlank()) {', '                    if (handle.isNotBlank() && acctObj.optString("handle", "") != handle) {')
    text = once(text, '                    if (!photoUrl.isNullOrBlank()) {', '                    if (!photoUrl.isNullOrBlank() && acctObj.optString("photoUrl", "") != photoUrl) {')
    return text

def viewmodel(text):
    return once(text, '''    private fun persistAll(ownerUid: String? = null) {
        val resolvedUid = ownerUid ?: activeOwnerUid()
        persistJob?.cancel()
        persistJob = viewModelScope.launch(Dispatchers.IO) {
            delay(80)
            storage.saveState(_comics.value, _profile.value, _appSettings.value, resolvedUid)
        }
    }''', '''    private fun persistAll(ownerUid: String? = null) {
        // PERSIST_SNAPSHOT_V13: bind the data snapshot to its owner before the debounce.
        val resolvedUid = ownerUid ?: activeOwnerUid()
        val comicsSnapshot = _comics.value
        val profileSnapshot = _profile.value
        val settingsSnapshot = _appSettings.value
        persistJob?.cancel()
        persistJob = viewModelScope.launch(Dispatchers.IO) {
            delay(80)
            storage.saveState(comicsSnapshot, profileSnapshot, settingsSnapshot, resolvedUid)
        }
    }''')

def worker(text):
    return once(text, '''        } catch (_: Throwable) {
            if (runAttemptCount < 3) {''', '''        } catch (cancel: kotlinx.coroutines.CancellationException) {
            // WORKER_CANCELLATION_V13: cancellation is not a retryable sync failure.
            throw cancel
        } catch (_: Exception) {
            if (runAttemptCount < 3) {''')

plan('app/src/main/java/com/example/data/SafeStorage.kt', 'ACCOUNT_SCOPE_HARDENING_V13', storage)
plan('app/src/main/java/com/example/viewmodel/QuadrinheiroViewModel.kt', 'PERSIST_SNAPSHOT_V13', viewmodel)
plan('app/src/main/java/com/example/data/QuadrinheiroSyncWorker.kt', 'WORKER_CANCELLATION_V13', worker)
ci_path = Path('firebase.ci.json')
ci = json.loads(ci_path.read_text(encoding='utf-8'))
if len(ci.get('firestore', [])) != 1 or ci['firestore'][0].get('rules') != 'firestore.rules':
    raise RuntimeError('Unexpected CI Firestore configuration; no files written')
fixture = Path('app/src/test/java/com/example/FirestoreEmulatorTestBase.kt').read_text(encoding='utf-8')
if 'System.getenv("GITHUB_ACTIONS") == "true"' not in fixture or '"(default)"' not in fixture:
    raise RuntimeError('CI fixture changed; review its selected database before writing')
ci['firestore'][0]['database'] = '(default)'
pending[ci_path] = (json.dumps(ci, indent=2) + '\n').encode('utf-8')
report.append('OK CI database aligned to test fixture; production firebase.json/rules unchanged')

TEST = '''package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SafeStorage
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.UserProfile
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountPersistenceRegressionTest {
    private lateinit var storage: SafeStorage
    private val alice = "hardening_test_alice"
    private val bob = "hardening_test_bob"
    private val settings = AppSettings()

    @Before
    fun setup() {
        storage = SafeStorage(ApplicationProvider.getApplicationContext<Context>())
        clearTestState()
    }

    @After
    fun cleanup() = clearTestState()

    private fun clearTestState() {
        storage.clearAuthSession()
        storage.removeItem(SafeStorage.STORAGE_KEY)
        storage.removeItem(SafeStorage.GUEST_STATE_KEY)
        storage.removeItem(SafeStorage.ACCOUNT_STATE_PREFIX + alice)
        storage.removeItem(SafeStorage.ACCOUNT_STATE_PREFIX + bob)
    }

    private fun login(uid: String) {
        storage.saveAuthSession(UserProfile(uid = uid, displayName = uid, email = null,
            photoUrl = null, isGuest = false, provider = "email"))
    }

    private fun comic(title: String) = ComicEntry(id = "shared-id", title = title,
        authors = "Author", publisher = "Publisher", updatedAt = 100L, isDirty = true)

    @Test
    fun saveWithoutSessionPersistsInsteadOfSilentlyDroppingData() {
        storage.saveState(listOf(comic("Local")), ReaderProfile(name = "Local"), settings)
        assertEquals("Local", storage.loadState().comics.single().title)
    }

    @Test
    fun switchingAccountsKeepsTheirLibrariesSeparate() {
        login(alice)
        storage.saveState(listOf(comic("Alice")), ReaderProfile(name = "Alice"), settings)
        login(bob)
        assertTrue(storage.loadState().comics.isEmpty())
        storage.saveState(listOf(comic("Bob")), ReaderProfile(name = "Bob"), settings)
        login(alice)
        assertEquals("Alice", storage.loadState().comics.single().title)
        login(bob)
        assertEquals("Bob", storage.loadState().comics.single().title)
    }

    @Test
    fun lateSaveForAliceDoesNotReplaceBobsSharedMirror() {
        login(bob)
        storage.saveState(listOf(comic("Bob")), ReaderProfile(name = "Bob"), settings)
        storage.saveState(listOf(comic("Alice late")), ReaderProfile(name = "Alice"), settings, alice)
        val mirror = JSONObject(storage.getItem()!!)
        assertEquals("Bob", mirror.getJSONObject("profile").getString("name"))
        assertEquals("Alice late", storage.loadAccountState(alice)!!.comics.single().title)
        assertEquals("Bob", storage.loadState().comics.single().title)
    }

    @Test
    fun syncAcknowledgementForAliceDoesNotClearBobsDirtyFlags() {
        login(alice)
        storage.saveState(listOf(comic("Alice")), ReaderProfile(name = "Alice", isDirty = true), settings)
        login(bob)
        storage.saveState(listOf(comic("Bob")), ReaderProfile(name = "Bob", isDirty = true), settings)
        storage.markComicsSynced(setOf("shared-id"), 100L, ownerUid = alice)
        storage.markProfileSynced(100L, ownerUid = alice)
        assertFalse(storage.loadAccountState(alice)!!.comics.single().isDirty)
        assertFalse(storage.loadAccountState(alice)!!.profile.isDirty)
        assertTrue(storage.loadAccountState(bob)!!.comics.single().isDirty)
        assertTrue(storage.loadAccountState(bob)!!.profile.isDirty)
        val mirror = JSONObject(storage.getItem()!!)
        assertTrue(mirror.getJSONArray("comics").getJSONObject(0).getBoolean("isDirty"))
        assertTrue(mirror.getJSONObject("profile").getBoolean("isDirty"))
    }

    @Test
    fun guestLibraryRemainsSeparateFromAccounts() {
        login(alice)
        storage.saveState(listOf(comic("Alice")), ReaderProfile(name = "Alice"), settings)
        storage.setGuestMode(true)
        storage.saveState(listOf(comic("Guest")), ReaderProfile(name = "Convidado"), settings)
        assertEquals("Guest", storage.loadGuestState().comics.single().title)
        assertEquals("Alice", storage.loadAccountState(alice)!!.comics.single().title)
    }
}
'''
test_path = Path('app/src/test/java/com/example/AccountPersistenceRegressionTest.kt')
if test_path.exists() and test_path.read_text(encoding='utf-8') != TEST:
    raise RuntimeError('Regression test file already exists with different content; no files written')
pending[test_path] = TEST.encode('utf-8')
for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/account_hardening_v13.txt').write_text('\n'.join(report) + '\nNo UI, gesture, signing-key or production security-rule changes. Tests must pass before committing. Remaining review items: logout/login races, same-account edits during in-flight sync, username verification on network failure, and legacy-state migration.\n', encoding='utf-8')
