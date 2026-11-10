package com.example.data

import android.content.Context
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ComicMoment
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SafeStorage(context: Context) {
    val systemIsDark: Boolean get() = (appContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    private val appContext = context.applicationContext

    private val prefs = try {
        context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE)
    } catch (_: Throwable) {
        null
    }

    companion object {
        const val STORAGE_KEY = "quadrinheiro_apple_hig_liquid_v10"
        const val TRANSLATION_CACHE_KEY = "quadrinheiro_ptbr_translation_cache_v1"
        const val AUTH_SESSION_KEY = "quadrinheiro_auth_session_v1"
        const val GUEST_MODE_KEY = "quadrinheiro_guest_mode_v1"
        const val AUTH_ONBOARDING_KEY = "quadrinheiro_auth_onboarding_done_v1"
        const val WELCOME_INTRO_KEY = "quadrinheiro_welcome_intro_seen_v1"
        const val LOCAL_ACCOUNTS_KEY = "quadrinheiro_local_email_accounts_v1"
        const val LOCAL_HANDLES_KEY = "quadrinheiro_local_handles_registry_v1"
        const val ACCOUNT_STATE_PREFIX = "quadrinheiro_account_state_v1_"
        const val GUEST_STATE_KEY = "quadrinheiro_guest_isolated_state_v1"
        const val MODERN_CATALOG_SNAPSHOT_KEY = "quadrinheiro_modern_catalog_snapshot_v1"
        const val MAX_TRANSLATION_CACHE_ENTRIES = 256
        const val MAX_TRANSLATION_CACHE_BYTES = 128 * 1024
        private const val ACCOUNT_REF_PREFIX = "@ref:"
        private val EXTERNAL_SNAPSHOT_KEYS = setOf(TRANSLATION_CACHE_KEY, MODERN_CATALOG_SNAPSHOT_KEY)
        private val memoryFallback = java.util.concurrent.ConcurrentHashMap<String, String>()
        private val catalogMatchIndex by lazy(LazyThreadSafetyMode.PUBLICATION) {
            CatalogMatchIndex(CuratedCatalog.curatedComicsCatalog)
        }
    }

    private fun accountStateKey(uid: String): String {
        val clean = uid.trim().replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        return "$ACCOUNT_STATE_PREFIX$clean"
    }

    private fun snapshotDir(): java.io.File =
        java.io.File(appContext.cacheDir, "quadrinheiro_snapshots").apply { mkdirs() }

    internal fun snapshotFile(key: String): java.io.File {
        val safeName = key.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        return java.io.File(snapshotDir(), "$safeName.json")
    }

    private fun resolvePointerIfNeeded(key: String, rawValue: String?): String? {
        if (rawValue == null) return null
        if (key == STORAGE_KEY && rawValue.startsWith(ACCOUNT_REF_PREFIX)) {
            val targetKey = rawValue.removePrefix(ACCOUNT_REF_PREFIX).trim()
            if (targetKey.isNotEmpty() && targetKey != STORAGE_KEY) {
                return getItem(targetKey)
            }
        }
        return rawValue
    }

    fun getItem(key: String = STORAGE_KEY): String? {
        memoryFallback[key]?.let { cached ->
            return resolvePointerIfNeeded(key, cached)
        }
        if (key in EXTERNAL_SNAPSHOT_KEYS) {
            return try {
                val file = snapshotFile(key)
                if (file.isFile && file.exists()) {
                    val content = file.readText(Charsets.UTF_8)
                    memoryFallback[key] = content
                    content
                } else {
                    val legacyFromPrefs = prefs?.getString(key, null)
                    if (legacyFromPrefs != null) {
                        memoryFallback[key] = legacyFromPrefs
                        writeSnapshotFileAtomically(file, legacyFromPrefs)
                        runCatching { prefs?.edit()?.remove(key)?.apply() }
                    }
                    legacyFromPrefs
                }
            } catch (_: Throwable) {
                memoryFallback[key]
            }
        }
        return try {
            val fromDisk = prefs?.getString(key, null)
            if (fromDisk != null) {
                memoryFallback[key] = fromDisk
            }
            resolvePointerIfNeeded(key, fromDisk)
        } catch (_: Throwable) {
            resolvePointerIfNeeded(key, memoryFallback[key])
        }
    }

    private fun writeSnapshotFileAtomically(target: java.io.File, content: String) {
        try {
            target.parentFile?.mkdirs()
            val tmp = java.io.File(target.parentFile, "${target.name}.tmp")
            tmp.writeText(content, Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(content, Charsets.UTF_8)
                tmp.delete()
            }
        } catch (_: Throwable) {
            runCatching { target.writeText(content, Charsets.UTF_8) }
        }
    }

    fun setItem(key: String = STORAGE_KEY, value: String) {
        if (key in EXTERNAL_SNAPSHOT_KEYS) {
            val file = snapshotFile(key)
            if (memoryFallback[key] == value && file.isFile) return
            memoryFallback[key] = value
            writeSnapshotFileAtomically(file, value)
            runCatching {
                if (prefs?.contains(key) == true) {
                    prefs?.edit()?.remove(key)?.apply()
                }
            }
            StorageWriteTelemetry.recordSnapshotCacheWrite(key, value.toByteArray(Charsets.UTF_8).size)
            return
        }
        val previousMem = memoryFallback.put(key, value)
        try {
            val currentDisk = prefs?.getString(key, null)
            if (previousMem == value && currentDisk == value) return
            prefs?.edit()?.putString(key, value)?.apply()
            StorageWriteTelemetry.recordPrefsWrite(key, value.toByteArray(Charsets.UTF_8).size)
        } catch (_: Throwable) {
        }
    }

    fun removeItem(key: String) {
        memoryFallback.remove(key)
        if (key in EXTERNAL_SNAPSHOT_KEYS) {
            runCatching { snapshotFile(key).delete() }
            runCatching { java.io.File(snapshotDir(), "${snapshotFile(key).name}.tmp").delete() }
        }
        try {
            if (prefs?.contains(key) == true) {
                prefs?.edit()?.remove(key)?.apply()
            }
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun saveAuthSession(userProfile: UserProfile?) {
        if (userProfile == null) {
            removeItem(AUTH_SESSION_KEY)
            return
        }
        try {
            val obj = JSONObject().apply {
                put("uid", userProfile.uid)
                put("displayName", userProfile.displayName)
                put("email", userProfile.email ?: JSONObject.NULL)
                put("photoUrl", userProfile.photoUrl ?: JSONObject.NULL)
                put("isGuest", userProfile.isGuest)
                put("provider", userProfile.provider)
            }
            setItem(AUTH_SESSION_KEY, obj.toString())
            setCompletedAuthOnboarding(true)
            setGuestMode(userProfile.isGuest)
        } catch (_: Throwable) {
        }
    }

    fun loadAuthSession(): UserProfile? {
        val raw = getItem(AUTH_SESSION_KEY) ?: return null
        return try {
            val obj = JSONObject(raw)
            val uid = obj.optString("uid", "").trim()
            if (uid.isEmpty()) return null
            val emailVal = if (obj.isNull("email")) null else obj.optString("email", "").takeIf { it.isNotBlank() }
            val photoVal = if (obj.isNull("photoUrl")) null else obj.optString("photoUrl", "").takeIf { it.isNotBlank() }
            UserProfile(
                uid = uid,
                displayName = obj.optString("displayName", "Leitor").ifBlank { "Leitor" },
                email = emailVal,
                photoUrl = photoVal,
                isGuest = obj.optBoolean("isGuest", false),
                provider = obj.optString("provider", "guest").ifBlank { "guest" }
            )
        } catch (_: Throwable) {
            null
        }
    }

    @Synchronized
    fun setGuestMode(enabled: Boolean) {
        setItem(GUEST_MODE_KEY, if (enabled) "true" else "false")
        if (enabled) {
            setCompletedAuthOnboarding(true)
        }
    }

    fun isGuestMode(): Boolean {
        if (getItem(GUEST_MODE_KEY) == "true") return true
        val session = loadAuthSession()
        return session?.isGuest == true
    }

    @Synchronized
    fun setCompletedAuthOnboarding(completed: Boolean) {
        setItem(AUTH_ONBOARDING_KEY, if (completed) "true" else "false")
    }

    @Synchronized
    fun setCompletedWelcomeIntro(completed: Boolean) {
        setItem(WELCOME_INTRO_KEY, if (completed) "true" else "false")
    }

    fun hasCompletedWelcomeIntro(): Boolean {
        return getItem(WELCOME_INTRO_KEY) == "true"
    }

    fun hasCompletedAuthOnboarding(): Boolean {
        return getItem(AUTH_ONBOARDING_KEY) == "true"
    }

    fun hasActiveSession(): Boolean {
        return loadAuthSession() != null || isGuestMode() || hasCompletedAuthOnboarding()
    }

    @Synchronized
    fun clearAuthSession() {
        removeItem(AUTH_SESSION_KEY)
        setItem(GUEST_MODE_KEY, "false")
        setItem(AUTH_ONBOARDING_KEY, "false")
    }

    private fun hashPassword(email: String, password: String): String {
        val input = "${email.trim().lowercase()}:quadrinheiro_salt_v1:$password"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun registerLocalEmailAccount(name: String, email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
        return try {
            val raw = getItem(LOCAL_ACCOUNTS_KEY)
            val root = if (!raw.isNullOrBlank()) JSONObject(raw) else JSONObject()
            if (root.has(cleanEmail)) {
                return Result.failure(IllegalArgumentException("ERROR_EMAIL_ALREADY_IN_USE"))
            }
            val uid = "email_" + UUID.nameUUIDFromBytes(cleanEmail.toByteArray(StandardCharsets.UTF_8)).toString()
            val acctObj = JSONObject().apply {
                put("uid", uid)
                put("displayName", cleanName)
                put("email", cleanEmail)
                put("passwordHash", hashPassword(cleanEmail, password))
                put("createdAt", System.currentTimeMillis())
            }
            root.put(cleanEmail, acctObj)
            setItem(LOCAL_ACCOUNTS_KEY, root.toString())
            val profile = UserProfile(
                uid = uid,
                displayName = cleanName,
                email = cleanEmail,
                photoUrl = null,
                isGuest = false,
                provider = "email"
            )
            saveAuthSession(profile)
            val baseHandleKey = cleanEmail.substringBefore("@")
                .lowercase()
                .replace(Regex("[^a-z0-9_.-]"), "")
                .take(24)
                .let { if (it.length >= 3) it else "leitor_${uid.takeLast(6)}" }
            if (getLocalHandleOwner(baseHandleKey) == null) {
                reserveLocalHandle(baseHandleKey, uid)
            }
            Result.success(profile)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    @Synchronized
    fun authenticateLocalEmailAccount(email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()
        return try {
            val raw = getItem(LOCAL_ACCOUNTS_KEY)
            val root = if (!raw.isNullOrBlank()) JSONObject(raw) else JSONObject()
            val acctObj = root.optJSONObject(cleanEmail)
                ?: return Result.failure(IllegalArgumentException("ERROR_USER_NOT_FOUND"))
            val expectedHash = acctObj.optString("passwordHash", "")
            val actualHash = hashPassword(cleanEmail, password)
            if (expectedHash.isNotEmpty() && expectedHash != actualHash) {
                return Result.failure(IllegalArgumentException("ERROR_WRONG_PASSWORD"))
            }
            val profile = UserProfile(
                uid = acctObj.optString("uid", "email_$cleanEmail"),
                displayName = acctObj.optString("displayName", "Leitor").ifBlank { "Leitor" },
                email = cleanEmail,
                photoUrl = null,
                isGuest = false,
                provider = "email"
            )
            saveAuthSession(profile)
            Result.success(profile)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun hasLocalEmailAccount(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val raw = getItem(LOCAL_ACCOUNTS_KEY) ?: return false
        return try {
            JSONObject(raw).has(cleanEmail)
        } catch (_: Throwable) {
            false
        }
    }

    @Synchronized
    fun syncAccountMetadata(uid: String, displayName: String, handle: String, photoUrl: String? = null) {
        if (uid.isBlank() || uid == "guest_local") return
        try {
            val raw = getItem(LOCAL_ACCOUNTS_KEY) ?: return
            val root = JSONObject(raw)
            var changed = false
            val keys = root.keys()
            while (keys.hasNext()) {
                val emailKey = keys.next()
                val acctObj = root.optJSONObject(emailKey) ?: continue
                if (acctObj.optString("uid", "") == uid) {
                    if (displayName.isNotBlank() && acctObj.optString("displayName", "") != displayName) {
                        acctObj.put("displayName", displayName)
                        changed = true
                    }
                    if (handle.isNotBlank() && acctObj.optString("handle", "") != handle) {
                        acctObj.put("handle", handle)
                        changed = true
                    }
                    if (!photoUrl.isNullOrBlank() && acctObj.optString("photoUrl", "") != photoUrl) {
                        acctObj.put("photoUrl", photoUrl)
                        changed = true
                    }
                }
            }
            if (changed) {
                setItem(LOCAL_ACCOUNTS_KEY, root.toString())
            }
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun getLocalHandleOwner(normalizedKey: String): String? {
        val clean = normalizedKey.trim().lowercase().removePrefix("@")
        if (clean.isEmpty()) return null
        val raw = getItem(LOCAL_HANDLES_KEY) ?: return null
        return try {
            val root = JSONObject(raw)
            root.optString(clean, "").takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    @Synchronized
    fun reserveLocalHandle(normalizedKey: String, ownerUid: String, previousKey: String? = null): Boolean {
        val clean = normalizedKey.trim().lowercase().removePrefix("@")
        if (clean.length < 3 || ownerUid.isBlank()) return false
        return try {
            val raw = getItem(LOCAL_HANDLES_KEY)
            val root = if (!raw.isNullOrBlank()) JSONObject(raw) else JSONObject()
            val existingOwner = root.optString(clean, "")
            if (existingOwner.isNotBlank() && existingOwner != ownerUid) {
                return false
            }
            val prevClean = previousKey?.trim()?.lowercase()?.removePrefix("@").orEmpty()
            if (prevClean.isNotBlank() && prevClean != clean && root.optString(prevClean, "") == ownerUid) {
                root.remove(prevClean)
            }
            root.put(clean, ownerUid)
            setItem(LOCAL_HANDLES_KEY, root.toString())
            true
        } catch (_: Throwable) {
            false
        }
    }

    data class StoredState(
        val comics: List<ComicEntry>,
        val profile: ReaderProfile,
        val settings: AppSettings
    )

    fun loadTranslationCache(): Map<String, Pair<String, String>> {
        val raw = getItem(TRANSLATION_CACHE_KEY) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = mutableMapOf<String, Pair<String, String>>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val obj = root.optJSONObject(k) ?: continue
                val tPt = obj.optString("titlePtBr", "")
                val sPt = obj.optString("synopsisPtBr", "")
                if (tPt.isNotBlank() || sPt.isNotBlank()) {
                    out[k] = tPt to sPt
                }
            }
            out
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    @Synchronized
    internal fun trimTranslationCache() {
        val raw = getItem(TRANSLATION_CACHE_KEY) ?: return
        runCatching { setItem(TRANSLATION_CACHE_KEY, boundedTranslationCache(JSONObject(raw)).toString()) }
    }

    private fun boundedTranslationCache(source: JSONObject): JSONObject {
        val result = JSONObject()
        var bytes = 0
        val retained = mutableListOf<Pair<String, JSONObject>>()
        source.keys().asSequence().toList().takeLast(MAX_TRANSLATION_CACHE_ENTRIES).asReversed().forEach { key ->
            val value = source.optJSONObject(key) ?: return@forEach
            val size = key.toByteArray(Charsets.UTF_8).size + value.toString().toByteArray(Charsets.UTF_8).size
            if (bytes + size <= MAX_TRANSLATION_CACHE_BYTES) { retained.add(key to value); bytes += size }
        }
        retained.asReversed().forEach { (key, value) -> result.put(key, value) }
        return result
    }

    @Synchronized
    fun saveComicTranslationCacheOnly(translated: ComicEntry) {
        val tPt = translated.titlePtBr.ifBlank { translated.title }
        val sPt = translated.synopsisPtBr.ifBlank { translated.synopsis }
        if (sPt.isBlank() && tPt.isBlank()) return
        try {
            val rawCache = getItem(TRANSLATION_CACHE_KEY)
            val cacheRoot = if (!rawCache.isNullOrBlank()) JSONObject(rawCache) else JSONObject()
            var changed = false
            val entryObj = JSONObject().apply {
                put("titlePtBr", tPt.take(300))
                put("synopsisPtBr", sPt.take(5000))
            }
            if (translated.id.isNotBlank()) {
                val existing = cacheRoot.optJSONObject(translated.id)
                if (existing == null || existing.optString("titlePtBr") != tPt.take(300) || existing.optString("synopsisPtBr") != sPt.take(5000)) {
                    cacheRoot.remove(translated.id)
                    cacheRoot.put(translated.id, entryObj)
                    changed = true
                }
            }
            if (translated.externalId.isNotBlank() && translated.externalId != translated.id) {
                val existing = cacheRoot.optJSONObject(translated.externalId)
                if (existing == null || existing.optString("titlePtBr") != tPt.take(300) || existing.optString("synopsisPtBr") != sPt.take(5000)) {
                    cacheRoot.remove(translated.externalId)
                    cacheRoot.put(translated.externalId, entryObj)
                    changed = true
                }
            }
            if (changed) {
                setItem(TRANSLATION_CACHE_KEY, boundedTranslationCache(cacheRoot).toString())
            }
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun saveComicTranslation(translated: ComicEntry) {
        val tPt = translated.titlePtBr.ifBlank { translated.title }
        val sPt = translated.synopsisPtBr.ifBlank { translated.synopsis }
        if (sPt.isBlank() && tPt.isBlank()) return

        // 1. Salva no arquivo de cache de traduções (isolado de SharedPreferences, máx. 512 KB)
        saveComicTranslationCacheOnly(translated)

        // 2. Se o quadrinho já estiver salvo no estado da conta ativa ou no espelho local, atualiza
        val targetKeys = syncStateKeys(null).ifEmpty { listOf(STORAGE_KEY) }
        for (targetKey in targetKeys) {
            try {
                if (targetKey == STORAGE_KEY) {
                    val rawPtr = memoryFallback[STORAGE_KEY] ?: prefs?.getString(STORAGE_KEY, null)
                    if (rawPtr != null && rawPtr.startsWith(ACCOUNT_REF_PREFIX)) continue
                }
                val rawState = getItem(targetKey) ?: continue
                val root = JSONObject(rawState)
                val comicsArr = root.optJSONArray("comics") ?: continue
                var updatedAny = false
                for (i in 0 until comicsArr.length()) {
                    val obj = comicsArr.optJSONObject(i) ?: continue
                    val idVal = obj.optString("id", "")
                    val extIdVal = obj.optString("externalId", "")
                    val isMatch = (translated.id.isNotBlank() && idVal == translated.id) ||
                        (translated.externalId.isNotBlank() && extIdVal == translated.externalId)
                    if (isMatch) {
                        if (sPt.isNotBlank() && obj.optString("synopsisPtBr", "") != sPt) {
                            obj.put("synopsisPtBr", sPt)
                            if (obj.optString("synopsis", "").isBlank()) {
                                obj.put("synopsis", sPt)
                            }
                            obj.put("updatedAt", System.currentTimeMillis())
                            obj.put("isDirty", true)
                            updatedAny = true
                        }
                        if (tPt.isNotBlank() && obj.optString("titlePtBr", "") != tPt) {
                            obj.put("titlePtBr", tPt)
                            if (obj.optString("title", "").isBlank()) {
                                obj.put("title", tPt)
                            }
                            obj.put("updatedAt", System.currentTimeMillis())
                            obj.put("isDirty", true)
                            updatedAny = true
                        }
                    }
                }
                if (updatedAny) {
                    setItem(targetKey, root.toString())
                }
            } catch (_: Throwable) {
            }
        }
    }

    // ACCOUNT_SCOPE_HARDENING_V13: inactive accounts never modify another account's mirror.
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
            val rawStorageValue = memoryFallback[STORAGE_KEY] ?: prefs?.getString(STORAGE_KEY, null)
            val isPointerMirror = rawStorageValue != null && rawStorageValue.startsWith(ACCOUNT_REF_PREFIX)
            if (!isPointerMirror && (isActiveOwner || (ownerUid == null && resolvedUid.isNullOrBlank()))) {
                add(STORAGE_KEY)
            }
        }
    }

    @Synchronized
    fun markComicsSynced(
        syncedIds: Set<String>,
        syncedAtMillis: Long = System.currentTimeMillis(),
        ownerUid: String? = null
    ) {
        if (syncedIds.isEmpty()) return
        val targetKeys = syncStateKeys(ownerUid)
        for (targetKey in targetKeys) {
            try {
                val rawState = getItem(targetKey) ?: continue
                val root = JSONObject(rawState)
                val comicsArr = root.optJSONArray("comics") ?: continue
                var updatedAny = false
                for (i in 0 until comicsArr.length()) {
                    val obj = comicsArr.optJSONObject(i) ?: continue
                    val idVal = obj.optString("id", "")
                    val extIdVal = obj.optString("externalId", "")
                    if (syncedIds.contains(idVal) || syncedIds.contains(extIdVal)) {
                        if (obj.optBoolean("isDirty", false)) {
                            obj.put("isDirty", false)
                            if (obj.optLong("updatedAt", 0L) <= 0L) {
                                obj.put("updatedAt", syncedAtMillis)
                            }
                            updatedAny = true
                        }
                    }
                }
                if (updatedAny) {
                    setItem(targetKey, root.toString())
                }
            } catch (_: Throwable) {
            }
        }
    }

    @Synchronized
    fun markProfileSynced(
        syncedAtMillis: Long = System.currentTimeMillis(),
        ownerUid: String? = null
    ) {
        val targetKeys = syncStateKeys(ownerUid)
        for (targetKey in targetKeys) {
            try {
                val rawState = getItem(targetKey) ?: continue
                val root = JSONObject(rawState)
                val profObj = root.optJSONObject("profile") ?: continue
                if (profObj.optBoolean("isDirty", false) && profObj.optLong("updatedAt", 0L) <= syncedAtMillis) {
                    profObj.put("isDirty", false)
                    if (profObj.optLong("updatedAt", 0L) <= 0L) {
                        profObj.put("updatedAt", syncedAtMillis)
                    }
                    setItem(targetKey, root.toString())
                }
            } catch (_: Throwable) {
            }
        }
    }

    @Synchronized
    fun loadGuestState(): StoredState {
        val raw = getItem(GUEST_STATE_KEY)
        if (!raw.isNullOrBlank()) {
            parseStoredStateJson(raw, loadTranslationCache())?.let { parsed ->
                val result=parsed.copy(
                    settings = getItem("profile_showcase_guest_local")?.let(ProfileShowcase::decode)?.let { if(JSONObject(raw).optInt("catalogEpoch")<CatalogEpoch.VERSION) CatalogEpoch.settings(ProfileShowcase.merge(it,parsed.settings)) else ProfileShowcase.merge(it,parsed.settings) } ?: parsed.settings,
                    profile = parsed.profile.copy(
                        name = "Leitor Convidado",
                        handle = "@convidado",
                        isDirty = false
                    )
                )
                if(JSONObject(raw).optInt("catalogEpoch")<CatalogEpoch.VERSION){saveState(result.comics,result.profile,result.settings,"guest_local");saveShowcase("guest_local",result.settings)}
                return result
            }
        }
        return StoredState(
            comics = emptyList(),
            profile = ReaderProfile(name = "Leitor Convidado", handle = "@convidado", isDirty = false),
            settings = AppSettings()
        )
    }

    @Synchronized
    fun loadAccountState(uid: String): StoredState? {
        if (uid.isBlank()) return null
        if (uid == "guest_local") return loadGuestState()
        val raw = getItem(accountStateKey(uid)) ?: return null
        return parseStoredStateJson(raw, loadTranslationCache())?.let { state ->
            val showcase = getItem("profile_showcase_$uid")?.let(ProfileShowcase::decode)
            val merged=if (showcase == null) state else state.copy(settings = ProfileShowcase.merge(showcase, state.settings))
            if(JSONObject(raw).optInt("catalogEpoch")<CatalogEpoch.VERSION){
                val clean=merged.copy(settings=CatalogEpoch.settings(merged.settings));saveState(clean.comics,clean.profile,clean.settings,uid);saveShowcase(uid,clean.settings);clean
            }else merged
        }
    }

    @Synchronized
    private fun parseStoredStateJson(
        raw: String,
        persistedTranslations: Map<String, Pair<String, String>>
    ): StoredState? {
        return try {
            val root = JSONObject(raw)
            val comicsArr = root.optJSONArray("comics")
            val obsolete = root.optInt("catalogEpoch",0) < CatalogEpoch.VERSION
            val comics = if (!obsolete && comicsArr != null) parseComics(comicsArr) else emptyList()
            PortugueseComicTranslator.seedPersistedTranslations(comics, persistedTranslations)
            val profile = root.optJSONObject("profile")?.let { parseProfile(it) } ?: ReaderProfile()
            val settings = root.optJSONObject("settings")?.let { saved ->
                val parsed = parseSettings(saved)
                val defaults = AppSettings()
                val savedMode = saved.optString("colorScheme", "").lowercase()
                parsed.copy(
                    themeId = if (!saved.has("themeId") || saved.optString("themeId", "").isBlank()) defaults.themeId else parsed.themeId,
                    colorScheme = when (savedMode) {
                        "system", "light", "dark" -> savedMode
                        "" -> defaults.colorScheme
                        else -> parsed.colorScheme
                    }
                )
            } ?: AppSettings()
            StoredState(comics, if(obsolete) CatalogEpoch.profile(profile) else profile, if(obsolete) CatalogEpoch.settings(settings) else settings)
        } catch (_: Throwable) {
            null
        }
    }

    data class WidgetSessionSnapshot(val ownerUid: String?, val state: StoredState)

    /** Capture the owner with its data; logged-out widgets never read the legacy account cache. */
    fun loadWidgetSessionSnapshot(): WidgetSessionSnapshot {
        val session = loadAuthSession()
        val ownerUid = session?.uid ?: if (isGuestMode()) "guest_local" else null
        val state = when {
            ownerUid == "guest_local" -> loadGuestState()
            ownerUid != null -> loadAccountState(ownerUid) ?: StoredState(emptyList(), ReaderProfile(), AppSettings())
            else -> StoredState(emptyList(), ReaderProfile(), AppSettings())
        }
        return WidgetSessionSnapshot(ownerUid, state)
    }

    fun loadWidgetState(): StoredState = loadWidgetSessionSnapshot().state

    @Synchronized
    fun loadState(): StoredState {
        val activeSession = loadAuthSession()
        if (isGuestMode() || activeSession?.isGuest == true) {
            return loadGuestState()
        }
        if (activeSession != null && !activeSession.isGuest && activeSession.uid.isNotBlank()) {
            loadAccountState(activeSession.uid)?.let { accountState ->
                return accountState
            }
            // Conta autenticada sem estado local salvo ainda: inicia limpa para receber dados da conta sem misturar com convidado
            return StoredState(
                comics = emptyList(),
                profile = ReaderProfile(
                    name = activeSession.displayName.ifBlank { "Leitor" },
                    avatarUrl = activeSession.photoUrl.orEmpty(),
                    updatedAt = 0L,
                    isDirty = false
                ),
                settings = AppSettings()
            )
        }
        val persistedTranslations = loadTranslationCache()
        val raw = getItem(STORAGE_KEY)
        if (raw == null) {
            val starter = if (!hasCompletedAuthOnboarding()) emptyList() else CuratedCatalog.initialUserCollection
            if (starter.isNotEmpty() || persistedTranslations.isNotEmpty()) {
                PortugueseComicTranslator.seedPersistedTranslations(starter, persistedTranslations)
            }
            return StoredState(
                comics = starter,
                profile = ReaderProfile(),
                settings = AppSettings()
            )
        }
        return parseStoredStateJson(raw, persistedTranslations)
            ?: StoredState(emptyList(), ReaderProfile(), AppSettings())
    }

    @Synchronized
    fun saveState(
        comics: List<ComicEntry>,
        profile: ReaderProfile,
        settings: AppSettings,
        ownerUid: String? = null
    ) {
        val json = buildBackupJsonString(comics, profile, settings, pretty = false)
        val activeSession = loadAuthSession()
        val isGuestTarget = ownerUid == "guest_local" ||
            (ownerUid == null && (isGuestMode() || activeSession?.isGuest == true))

        if (isGuestTarget) {
            // Modo Convidado: salva exclusivamente na chave isolada de convidado, sem tocar na conta principal!
            setItem(GUEST_STATE_KEY, json)
            return
        }

        val resolvedUid = ownerUid?.takeIf { it.isNotBlank() && it != "guest_local" }
            ?: activeSession?.takeIf { !it.isGuest }?.uid

        if (!resolvedUid.isNullOrBlank()) {
            val targetKey = accountStateKey(resolvedUid)
            setItem(targetKey, json)
            if (ownerUid == null || (activeSession != null && !activeSession.isGuest && activeSession.uid == resolvedUid)) {
                setItem(STORAGE_KEY, "$ACCOUNT_REF_PREFIX$targetKey")
            }
            syncAccountMetadata(
                uid = resolvedUid,
                displayName = profile.name,
                handle = profile.handle,
                photoUrl = profile.avatarUrl.takeIf { it.isNotBlank() }
            )
        } else {
            // Preserve legacy/local persistence when no account was selected.
            setItem(STORAGE_KEY, json)
        }
    }

    /**
     * Auto-compacta de forma 100% segura instalações existentes que acumulavam caches
     * (`TRANSLATION_CACHE_KEY`, `MODERN_CATALOG_SNAPSHOT_KEY`) ou duplicação de biblioteca
     * dentro do arquivo XML de `SharedPreferences`, preservando integralmente todas as contas,
     * modo convidado, vitrines e filas de sincronização.
     */
    @Synchronized
    fun compactStorageIfNeeded(): Boolean {
        var compacted = false
        try {
            val p = prefs ?: return false
            for (extKey in EXTERNAL_SNAPSHOT_KEYS) {
                if (p.contains(extKey)) {
                    val raw = p.getString(extKey, null)
                    if (!raw.isNullOrBlank()) {
                        val file = snapshotFile(extKey)
                        if (!file.exists()) {
                            writeSnapshotFileAtomically(file, raw)
                        }
                        memoryFallback[extKey] = raw
                    }
                    p.edit().remove(extKey).apply()
                    compacted = true
                }
            }
            trimTranslationCache()
            val activeSession = loadAuthSession()
            if (activeSession != null && !activeSession.isGuest && activeSession.uid.isNotBlank()) {
                val targetKey = accountStateKey(activeSession.uid)
                val hasAccountState = p.contains(targetKey) || memoryFallback.containsKey(targetKey)
                val rawMain = p.getString(STORAGE_KEY, null)
                if (hasAccountState && !rawMain.isNullOrBlank() && !rawMain.startsWith(ACCOUNT_REF_PREFIX)) {
                    val refValue = "$ACCOUNT_REF_PREFIX$targetKey"
                    memoryFallback[STORAGE_KEY] = refValue
                    p.edit().putString(STORAGE_KEY, refValue).apply()
                    compacted = true
                }
            }
            if (compacted) {
                StorageWriteTelemetry.recordAutoCompaction()
            }
        } catch (_: Throwable) {
        }
        return compacted
    }

    fun buildBackupJsonString(
        comics: List<ComicEntry>,
        profile: ReaderProfile,
        settings: AppSettings,
        pretty: Boolean = true
    ): String {
        val root = JSONObject()
        root.put("catalogEpoch", CatalogEpoch.VERSION)
        root.put("comics", serializeComics(comics))
        root.put("profile", serializeProfile(profile))
        root.put("settings", serializeSettings(settings))
        return if (pretty) root.toString(2) else root.toString()
    }

    fun importBackupJsonString(rawJson: String): StoredState? {
        return try {
            val parsed = JSONObject(rawJson.trim())
            val comicsArr = parsed.optJSONArray("comics") ?: return null
            val comics = parseComics(comicsArr)
            val current = loadState()
            val profile = parsed.optJSONObject("profile")?.let { parseProfile(it) } ?: current.profile
            val settings = parsed.optJSONObject("settings")?.let { parseSettings(it) } ?: current.settings
            saveState(comics, profile, settings)
            StoredState(comics, profile, settings)
        } catch (_: Throwable) {
            null
        }
    }

    fun buildStandaloneIOS27ZipBytes(
        comics: List<ComicEntry>,
        profile: ReaderProfile,
        settings: AppSettings
    ): ByteArray {
        val htmlString = buildStandaloneIOS27ZipHtml(comics, profile, settings)
        val htmlBytes = htmlString.toByteArray(StandardCharsets.UTF_8)
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val crc = CRC32().apply { update(htmlBytes) }
            val entry = ZipEntry("index.html").apply {
                size = htmlBytes.size.toLong()
                this.crc = crc.value
            }
            zos.putNextEntry(entry)
            zos.write(htmlBytes)
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    fun buildStandaloneIOS27ZipHtml(
        comics: List<ComicEntry>,
        profile: ReaderProfile,
        settings: AppSettings
    ): String {
        val payloadObj = JSONObject().apply {
            put("comics", serializeComics(comics))
            put("profile", serializeProfile(profile))
            put("settings", serializeSettings(settings))
            put("catalog", serializeComics(CuratedCatalog.curatedComicsCatalog))
        }
        val payload = payloadObj.toString().replace("<", "\\u003c")

        return """<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover" />
<meta name="theme-color" content="#0E0F14" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="mobile-web-app-capable" content="yes" />
<title>Quadrinheiro</title>
<style>
  :root {
    --apple-ease: cubic-bezier(0.32, 0.72, 0, 1);
    --nav-glass-alpha: 0.34;
    --content-mat-alpha: 0.62;
    --glass-blur: 36px;
  }
  * {
    box-sizing: border-box;
    -webkit-tap-highlight-color: transparent;
    user-select: none;
    margin: 0;
    padding: 0;
  }
  html, body {
    font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Segoe UI", Roboto, sans-serif;
    min-height: 100vh;
    min-height: 100dvh;
    background: #0E0F14;
    color: #F5F6FA;
    overflow-x: hidden;
    -webkit-font-smoothing: antialiased;
  }
  input, textarea { user-select: text !important; font-family: inherit; }
  .tap-scale { transition: transform 0.2s var(--apple-ease); cursor: pointer; }
  .tap-scale:active { transform: scale(0.97); }
  .container { max-width: 500px; margin: 0 auto; padding: 24px 18px 132px 18px; position: relative; z-index: 2; }
  .content-material {
    background: rgba(28, 30, 40, var(--content-mat-alpha));
    backdrop-filter: blur(22px) saturate(150%);
    -webkit-backdrop-filter: blur(22px) saturate(150%);
    border: 0.5px solid rgba(255, 255, 255, 0.10);
    box-shadow: 0 10px 28px -10px rgba(0, 0, 0, 0.45);
  }
  .liquid-glass-nav {
    background: rgba(42, 45, 60, var(--nav-glass-alpha));
    backdrop-filter: blur(var(--glass-blur)) saturate(200%);
    -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(200%);
    border: 1px solid rgba(255, 255, 255, 0.24);
    box-shadow: inset 0 1.5px 1px rgba(255, 255, 255, 0.42), inset 0 -1px 1px rgba(255, 255, 255, 0.12), 0 16px 38px -8px rgba(0, 0, 0, 0.55);
  }
  .dock-bar {
    position: fixed;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    width: 92%;
    max-width: 430px;
    z-index: 40;
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .dock-pill-group {
    position: relative;
    flex: 1;
    height: 62px;
    border-radius: 999px;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    align-items: center;
    padding: 4px;
  }
  .dock-indicator {
    position: absolute;
    top: 4px;
    bottom: 4px;
    left: 4px;
    width: calc((100% - 8px) / 4);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.16);
    box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.45);
    transition: transform 0.36s var(--apple-ease), opacity 0.25s ease;
    z-index: 1;
  }
  .dock-btn {
    position: relative;
    z-index: 2;
    height: 100%;
    border: none;
    background: transparent;
    color: rgba(255,255,255,0.62);
    font-size: 10px;
    font-weight: 600;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    cursor: pointer;
  }
  .dock-btn.active { color: #FFFFFF; }
  .dock-search-btn {
    width: 62px;
    height: 62px;
    border-radius: 50%;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #FFFFFF;
    border: none;
    cursor: pointer;
    transition: all 0.28s var(--apple-ease);
  }
  .dock-search-btn.active {
    color: #0E0F14;
    background: #FFFFFF;
    box-shadow: 0 10px 26px rgba(255,255,255,0.28);
  }
</style>
</head>
<body>
  <div id="app" class="container"></div>
  <div class="dock-bar">
    <nav class="dock-pill-group liquid-glass-nav">
      <div id="dock-ind" class="dock-indicator"></div>
      <button id="tab-home" class="dock-btn active tap-scale" onclick="window.goTab('home')"><span>Lendo</span></button>
      <button id="tab-shelf" class="dock-btn tap-scale" onclick="window.goTab('shelf')"><span>Biblioteca</span></button>
      <button id="tab-diary" class="dock-btn tap-scale" onclick="window.goTab('diary')"><span>Diário</span></button>
      <button id="tab-settings" class="dock-btn tap-scale" onclick="window.goTab('settings')"><span>Ajustes</span></button>
    </nav>
    <button id="tab-search" class="dock-search-btn liquid-glass-nav tap-scale" onclick="window.goTab('search')">🔍</button>
  </div>
  <script>
    (function() {
      var KEY = 'quadrinheiro_apple_hig_liquid_v10';
      var initial = $payload;
      var state = { tab: 'home', comics: initial.comics || [], profile: initial.profile, settings: initial.settings, catalog: initial.catalog || [] };
      window.goTab = function(t) { state.tab = t; render(); };
      function render() {
        var root = document.getElementById('app');
        if (!root) return;
        root.innerHTML = '<h1 style="font-size:28px;font-weight:700;margin-bottom:14px;">Quadrinheiro (' + state.comics.length + ' HQs)</h1>';
      }
      render();
    })();
  </script>
</body>
</html>"""
    }

    private fun serializeComics(comics: List<ComicEntry>): JSONArray {
        val arr = JSONArray()
        comics.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("externalId", c.externalId)
            obj.put("title", c.title)
            obj.put("titlePtBr", c.titlePtBr)
            obj.put("authors", c.authors)
            obj.put("publisher", c.publisher)
            obj.put("genre", c.genre)
            obj.put("format", c.format)
            obj.put("releaseYear", c.releaseYear)
            obj.put("pageCount", c.pageCount)
            obj.put("currentPage", c.currentPage)
            obj.put("status", c.status.key)
            obj.put("rating", c.rating.toDouble())
            obj.put("favorite", c.favorite)
            obj.put("rereadCount", c.rereadCount)
            val tagsArr = JSONArray()
            c.tags.forEach { tagsArr.put(it) }
            obj.put("tags", tagsArr)
            obj.put("review", c.review)
            obj.put("readDate", c.readDate)
            obj.put("addedAt", c.addedAt)
            obj.put("isbn", c.isbn)
            obj.put("coverUrl", c.coverUrl)
            obj.put("synopsis", c.synopsis)
            obj.put("synopsisPtBr", c.synopsisPtBr)
            obj.put("sourceLabel", c.sourceLabel)
            obj.put("editionLanguage", c.editionLanguage)
            val momentsArr = JSONArray()
            c.moments.forEach { m ->
                val mObj = JSONObject()
                mObj.put("id", m.id)
                mObj.put("page", m.page)
                mObj.put("tag", m.tag)
                mObj.put("note", m.note)
                mObj.put("date", m.date)
                mObj.put("isSpoiler", m.isSpoiler)
                momentsArr.put(mObj)
            }
            obj.put("moments", momentsArr)
            obj.put("seriesName", c.seriesName)
            obj.put("issueNumber", c.issueNumber)
            obj.put("updatedAt", c.updatedAt)
            obj.put("isDirty", c.isDirty)
            arr.put(obj)
        }
        return arr
    }

    private fun parseComics(arr: JSONArray): List<ComicEntry> {
        val out = mutableListOf<ComicEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val tagsList = mutableListOf<String>()
            val tagsArr = obj.optJSONArray("tags")
            if (tagsArr != null) {
                for (t in 0 until tagsArr.length()) {
                    val v = tagsArr.optString(t).trim()
                    if (v.isNotEmpty()) tagsList.add(v)
                }
            }
            val momentsList = mutableListOf<ComicMoment>()
            val momentsArr = obj.optJSONArray("moments")
            if (momentsArr != null) {
                for (m in 0 until momentsArr.length()) {
                    val mObj = momentsArr.optJSONObject(m) ?: continue
                    momentsList.add(
                        ComicMoment(
                            id = mObj.optString("id", "m-$m"),
                            page = mObj.optInt("page", 0),
                            tag = mObj.optString("tag", ""),
                            note = mObj.optString("note", ""),
                            date = mObj.optString("date", ""),
                            isSpoiler = mObj.optBoolean("isSpoiler", false)
                        )
                    )
                }
            }
            val idVal = obj.optString("id", "hq-$i")
            val extIdVal = obj.optString("externalId", idVal)
            val titleVal = obj.optString("title", "Sem Título")
            val isbnVal = obj.optString("isbn", "")
            val rawCover = obj.optString("coverUrl", "").trim()
            val isCuratedItem = idVal.startsWith("cat-") || idVal.startsWith("init-") || extIdVal.startsWith("cat-")
            val needsCoverHealing = isCuratedItem ||
                rawCover.isBlank() ||
                rawCover.contains("covers.openlibrary.org") ||
                rawCover.contains("BMY1_cover.jpg") ||
                rawCover.contains("BMDKR_cover.jpg")
            val catalogMatch = if (needsCoverHealing) {
                catalogMatchIndex.findByFields(
                    idVal = idVal,
                    extIdVal = extIdVal,
                    isbnVal = isbnVal,
                    titleVal = titleVal,
                    rawCover = rawCover
                )
            } else {
                null
            }
            val resolvedCover = when {
                catalogMatch != null && isCuratedItem -> catalogMatch.coverUrl
                catalogMatch != null && (rawCover.isBlank() || rawCover.contains("covers.openlibrary.org") || rawCover.contains("BMY1_cover.jpg") || rawCover.contains("BMDKR_cover.jpg")) -> catalogMatch.coverUrl
                rawCover.isNotBlank() -> rawCover
                catalogMatch != null -> catalogMatch.coverUrl
                else -> ""
            }
            val savedTitlePtBr = obj.optString("titlePtBr", "")
            val savedSynopsis = obj.optString("synopsis", "")
            val savedSynopsisPtBr = obj.optString("synopsisPtBr", "").ifBlank {
                if (savedSynopsis.isNotBlank() && PortugueseComicTranslator.isPortugueseText(savedSynopsis)) {
                    savedSynopsis
                } else {
                    ""
                }
            }
            out.add(
                ComicEntry(
                    id = idVal,
                    externalId = extIdVal,
                    title = titleVal.ifBlank { savedTitlePtBr },
                    titlePtBr = savedTitlePtBr,
                    authors = obj.optString("authors", "Autor Desconhecido"),
                    publisher = obj.optString("publisher", "Editora"),
                    genre = obj.optString("genre", "Graphic Novel"),
                    format = obj.optString("format", "Encadernado (TPB)"),
                    releaseYear = obj.optInt("releaseYear", 2026),
                    pageCount = obj.optInt("pageCount", 160),
                    currentPage = obj.optInt("currentPage", 0),
                    status = ReadingStatus.fromKey(obj.optString("status", "READ")),
                    rating = obj.optDouble("rating", 0.0).toFloat(),
                    favorite = obj.optBoolean("favorite", false),
                    rereadCount = obj.optInt("rereadCount", 0),
                    tags = tagsList,
                    review = obj.optString("review", ""),
                    readDate = obj.optString("readDate", ""),
                    addedAt = obj.optString("addedAt", ""),
                    isbn = isbnVal,
                    coverUrl = resolvedCover,
                    synopsis = savedSynopsis.ifBlank { savedSynopsisPtBr },
                    synopsisPtBr = savedSynopsisPtBr,
                    sourceLabel = obj.optString("sourceLabel", "Catálogo Base"),
                    editionLanguage = obj.optString("editionLanguage"),
                    moments = momentsList,
                    seriesName = obj.optString("seriesName", ""),
                    issueNumber = obj.optInt("issueNumber", 0),
                    updatedAt = obj.optLong("updatedAt", 0L),
                    isDirty = obj.optBoolean("isDirty", false)
                )
            )
        }
        return out
    }

    private fun serializeProfile(p: ReaderProfile): JSONObject = JSONObject().apply {
        put("name", p.name)
        put("handle", p.handle)
        put("bio", p.bio)
        put("favoriteUniverse", p.favoriteUniverse)
        put("avatarUrl", p.avatarUrl)
        put("avatarPreset", p.avatarPreset)
        put("yearlyGoal", p.yearlyGoal)
        put("dailyPagesGoal", p.dailyPagesGoal)
        put("showGoalOnHome", p.showGoalOnHome)
        put("activeReadingComicId", p.activeReadingComicId)
        put("updatedAt", p.updatedAt)
        put("isDirty", p.isDirty)
    }

    private fun parseProfile(obj: JSONObject): ReaderProfile {
        val def = ReaderProfile()
        return ReaderProfile(
            name = obj.optString("name", def.name),
            handle = obj.optString("handle", def.handle),
            bio = obj.optString("bio", def.bio),
            favoriteUniverse = obj.optString("favoriteUniverse", def.favoriteUniverse),
            avatarUrl = obj.optString("avatarUrl", def.avatarUrl),
            avatarPreset = obj.optString("avatarPreset", def.avatarPreset),
            yearlyGoal = obj.optInt("yearlyGoal", def.yearlyGoal).coerceAtLeast(1),
            dailyPagesGoal = obj.optInt("dailyPagesGoal", def.dailyPagesGoal).coerceAtLeast(5),
            showGoalOnHome = obj.optBoolean("showGoalOnHome", def.showGoalOnHome),
            activeReadingComicId = obj.optString("activeReadingComicId", def.activeReadingComicId),
            updatedAt = obj.optLong("updatedAt", 0L),
            isDirty = obj.optBoolean("isDirty", false)
        )
    }

    @Synchronized
    fun saveShowcase(ownerUid: String, settings: AppSettings) {
        if (ownerUid.isNotBlank()) setItem("profile_showcase_$ownerUid", ProfileShowcase.encode(settings))
    }

    data class ModernCatalogCacheSnapshot(
        val savedAtEpochMs: Long,
        val entries: List<ComicEntry>
    )

    data class AccountRecommendationsSnapshot(
        val schemaVersion: Int,
        val ownerUid: String,
        val tasteFingerprint: String,
        val libraryCount: Int,
        val savedAtEpochMs: Long,
        val recommendations: List<RecommendedComic>,
        val isLegacyProvisional: Boolean = false
    )

    private val recommendationFileLocks = java.util.concurrent.ConcurrentHashMap<String, Any>()

    private fun lockForOwner(ownerUid: String): Any {
        val safeKey = recommendationFileKey(ownerUid)
        return recommendationFileLocks.computeIfAbsent(safeKey) { Any() }
    }

    internal fun recommendationFileKey(ownerUid: String): String {
        val normalizedUid = ownerUid.trim().ifEmpty { "guest_local" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedUid.toByteArray(StandardCharsets.UTF_8))
        val sha256Hex = digest.joinToString("") { "%02x".format(it) }
        return "recs_v2_$sha256Hex"
    }

    private fun durableRecommendationsDir(): java.io.File =
        java.io.File(appContext.filesDir, "quadrinheiro_recommendations").apply { mkdirs() }

    internal fun durableRecommendationsFile(ownerUid: String): java.io.File {
        val key = recommendationFileKey(ownerUid)
        return java.io.File(durableRecommendationsDir(), "$key.json")
    }

    /**
     * Salva o snapshot durável de recomendações da conta em filesDir com escrita atômica (.tmp -> fsync -> .bak -> renameTo).
     */
    fun saveAccountRecommendationsSnapshot(
        ownerUid: String,
        tasteFingerprint: String,
        items: List<RecommendedComic>,
        libraryCount: Int,
        savedAtEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        val targetUid = ownerUid.trim().ifEmpty { "guest_local" }
        val encodedItemsJson = PersonalizedDiscovery.encodeDurableAccountRecommendations(items)
        if (encodedItemsJson.isBlank()) return false

        synchronized(lockForOwner(targetUid)) {
            val root = JSONObject().apply {
                put("schemaVersion", 2)
                put("ownerUid", targetUid)
                put("tasteFingerprint", tasteFingerprint)
                put("libraryCount", libraryCount)
                put("savedAtEpochMs", savedAtEpochMs)
                put("items", JSONArray(encodedItemsJson))
            }
            val payloadString = root.toString()
            val payloadBytes = payloadString.toByteArray(StandardCharsets.UTF_8)
            if (payloadBytes.size > PersonalizedDiscovery.MAX_DURABLE_ENVELOPE_BYTES) {
                return false
            }

            val dir = durableRecommendationsDir()
            val targetFile = durableRecommendationsFile(targetUid)
            val tmpFile = java.io.File(dir, "${targetFile.name}.tmp")
            val bakFile = java.io.File(dir, "${targetFile.name}.bak")

            return try {
                java.io.FileOutputStream(tmpFile).use { fos ->
                    fos.write(payloadBytes)
                    fos.flush()
                    fos.fd.sync()
                }

                if (!tmpFile.exists() || tmpFile.length() != payloadBytes.size.toLong()) {
                    tmpFile.delete()
                    return false
                }

                if (targetFile.exists() && targetFile.length() > 0L) {
                    try {
                        java.io.FileInputStream(targetFile).use { input ->
                            java.io.FileOutputStream(bakFile).use { output ->
                                input.copyTo(output)
                                output.flush()
                                output.fd.sync()
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }

                if (!tmpFile.renameTo(targetFile)) {
                    java.io.FileInputStream(tmpFile).use { input ->
                        java.io.FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                            output.flush()
                            output.fd.sync()
                        }
                    }
                    tmpFile.delete()
                }
                true
            } catch (_: Throwable) {
                tmpFile.delete()
                false
            }
        }
    }

    /**
     * Carrega o snapshot durável da conta com validação estrita de schema e ownerUid,
     * suportando recuperação automática de .bak e migração honesta de dados legados.
     */
    fun loadAccountRecommendationsSnapshot(ownerUid: String): AccountRecommendationsSnapshot? {
        val targetUid = ownerUid.trim().ifEmpty { "guest_local" }
        synchronized(lockForOwner(targetUid)) {
            val targetFile = durableRecommendationsFile(targetUid)
            val dir = durableRecommendationsDir()
            val bakFile = java.io.File(dir, "${targetFile.name}.bak")
            val tmpFile = java.io.File(dir, "${targetFile.name}.tmp")
            if (tmpFile.exists()) runCatching { tmpFile.delete() }

            fun parseSnapshot(file: java.io.File): AccountRecommendationsSnapshot? {
                if (!file.isFile || !file.exists() || file.length() <= 0L) return null
                return try {
                    val raw = file.readText(StandardCharsets.UTF_8)
                    val root = JSONObject(raw)
                    val schema = root.optInt("schemaVersion", 0)
                    val fileOwner = root.optString("ownerUid", "")
                    if (schema != 2 || fileOwner != targetUid) return null
                    val fingerprint = root.optString("tasteFingerprint", "")
                    val libCount = root.optInt("libraryCount", 0)
                    val savedAt = root.optLong("savedAtEpochMs", 0L)
                    val itemsArr = root.optJSONArray("items") ?: return null
                    val recs = PersonalizedDiscovery.decodeCachedRecommendations(itemsArr.toString())
                    if (recs.isEmpty()) return null
                    AccountRecommendationsSnapshot(
                        schemaVersion = 2,
                        ownerUid = targetUid,
                        tasteFingerprint = fingerprint,
                        libraryCount = libCount,
                        savedAtEpochMs = savedAt,
                        recommendations = recs,
                        isLegacyProvisional = false
                    )
                } catch (_: Throwable) {
                    null
                }
            }

            val primary = parseSnapshot(targetFile)
            if (primary != null) return primary

            val recoveredBak = parseSnapshot(bakFile)
            if (recoveredBak != null) {
                runCatching {
                    bakFile.copyTo(targetFile, overwrite = true)
                }
                return recoveredBak
            }

            // Migração legada: tenta ler settings.cachedRecommendationsJson da conta ativa
            val legacySettings = loadAccountState(targetUid)?.settings ?: loadState().settings
            if (legacySettings.cachedRecommendationsJson.isNotBlank()) {
                val decoded = PersonalizedDiscovery.decodeCachedRecommendations(legacySettings.cachedRecommendationsJson)
                if (decoded.isNotEmpty()) {
                    return AccountRecommendationsSnapshot(
                        schemaVersion = 1,
                        ownerUid = targetUid,
                        tasteFingerprint = "", // Não comprovado pelo algoritmo canônico v2
                        libraryCount = 0,
                        savedAtEpochMs = 0L,
                        recommendations = decoded,
                        isLegacyProvisional = true
                    )
                }
            }

            return null
        }
    }

    @Synchronized
    fun saveModernCatalogSnapshot(
        entries: List<ComicEntry>,
        savedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        val bounded = entries
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
            .take(48)
        if (bounded.isEmpty()) return
        try {
            val root = JSONObject().apply {
                put("savedAtEpochMs", savedAtEpochMs)
                put("items", serializeComics(bounded))
            }
            setItem(MODERN_CATALOG_SNAPSHOT_KEY, root.toString())
        } catch (_: Throwable) {
        }
    }

    fun loadModernCatalogSnapshot(): ModernCatalogCacheSnapshot? {
        val raw = getItem(MODERN_CATALOG_SNAPSHOT_KEY) ?: return null
        if (raw.isBlank()) return null
        return try {
            val root = JSONObject(raw)
            val savedAt = root.optLong("savedAtEpochMs", 0L)
            val itemsArr = root.optJSONArray("items") ?: return null
            val parsed = parseComics(itemsArr)
                .filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }
                .take(48)
            if (parsed.isEmpty()) null else ModernCatalogCacheSnapshot(savedAt, parsed)
        } catch (_: Throwable) {
            null
        }
    }

    @Synchronized
    fun clearModernCatalogSnapshot() {
        removeItem(MODERN_CATALOG_SNAPSHOT_KEY)
    }

    private fun serializeSettings(s: AppSettings): JSONObject = JSONObject().apply {
        val cleanFavComics = s.profileFavoriteComicIds.map { it.take(200) }.filter { it.isNotBlank() }.distinct().take(4)
        val cleanFavChars = s.profileFavoriteCharacters.map { it.take(60) }.filter { it.isNotBlank() }.distinct().take(4)
        val cleanDetails = ProfileShowcase.sanitizeDetails(s.profileShowcaseDetails, cleanFavComics, cleanFavChars)
        put("profileBannerUrl", s.profileBannerUrl.take(200000))
        put("profileShowcaseDetails", JSONObject(cleanDetails))
        put("profileShowcaseUpdatedAt", s.profileShowcaseUpdatedAt)
        put("profileFavoriteComicIds", JSONArray(cleanFavComics))
        put("profileFavoriteCharacters", JSONArray(cleanFavChars))
        put("themeId", s.themeId)
        put("colorScheme", s.colorScheme)
        put("glassLevel", s.glassLevel)
        put("glassVariant", s.glassVariant)
        put("edgeRefraction", s.edgeRefraction)
        put("glassIntensity", s.glassIntensity.toDouble())
        put("glassBlurIntensity", s.glassBlurIntensity.toDouble())
        put("enableLensRefraction", s.enableLensRefraction)
        put("enableChromaticAberration", s.enableChromaticAberration)
        put("glassOpacity", s.glassOpacity.toDouble())
        put("backgroundExtension", s.backgroundExtension)
        put("showBottomAccessory", s.showBottomAccessory)
        put("showReadingNowBar", s.showReadingNowBar)
        put("showFloatingMenu", s.showFloatingMenu)
        put("minimizeTabBarOnScroll", s.minimizeTabBarOnScroll)
        put("reduceTransparency", s.reduceTransparency)
        put("increaseContrast", s.increaseContrast)
        put("boldText", s.boldText)
        put("dynamicTypeScale", s.dynamicTypeScale)
        put("reduceMotion", s.reduceMotion)
        put("onOffLabels", s.onOffLabels)
        put("shelfColumns", s.shelfColumns)
        put("showProgressOnCovers", s.showProgressOnCovers)
        put("showRatingsOnGrid", s.showRatingsOnGrid)
        put("spine3dEffect", s.spine3dEffect)
        put("defaultShelfSort", s.defaultShelfSort)
        put("shelfLayout", s.shelfLayout)
        put("defaultFormat", s.defaultFormat)
        put("searchLanguage", s.searchLanguage)
        put("searchProvider", s.searchProvider)
        put("hdCovers", s.hdCovers)
        put("spoilerProtection", s.spoilerProtection)
        put("halfStarRatings", s.halfStarRatings)
        put("allowRereads", s.allowRereads)
        put("autoFillReadDate", s.autoFillReadDate)
        put("showPagesInDiary", s.showPagesInDiary)
        put("shareStarsMatchTheme", s.shareStarsMatchTheme)
        val rawRecs = s.cachedRecommendationsJson
        val safeRecs = if (rawRecs.length > 12288) {
            PersonalizedDiscovery.encodeCachedRecommendations(PersonalizedDiscovery.decodeCachedRecommendations(rawRecs))
        } else {
            rawRecs
        }
        put("cachedRecommendationsJson", safeRecs)
        put("recommendationTasteHash", s.recommendationTasteHash.take(64))
        put("settingsUpdatedAt", s.settingsUpdatedAt)
        put("launcherIconMode", s.launcherIconMode.take(30))
        val tagsArr = JSONArray()
        s.quickReviewTags.forEach { tagsArr.put(it) }
        put("quickReviewTags", tagsArr)
    }

    private fun parseSettings(obj: JSONObject): AppSettings {
        val def = AppSettings()
        val tagsList = mutableListOf<String>()
        val tagsArr = obj.optJSONArray("quickReviewTags")
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val t = tagsArr.optString(i).trim()
                if (t.isNotEmpty()) tagsList.add(t)
            }
        }
        val rawCachedRecs = obj.optString("cachedRecommendationsJson", def.cachedRecommendationsJson)
        val validCachedRecs = if (rawCachedRecs.length > 12288) {
            PersonalizedDiscovery.encodeCachedRecommendations(PersonalizedDiscovery.decodeCachedRecommendations(rawCachedRecs))
        } else {
            rawCachedRecs
        }
        return AppSettings(
            profileBannerUrl = obj.optString("profileBannerUrl").take(200000),
            profileShowcaseUpdatedAt = obj.optLong("profileShowcaseUpdatedAt", 0L),
            profileShowcaseDetails = obj.optJSONObject("profileShowcaseDetails").let { d -> d?.keys()?.asSequence()?.take(8)?.associateWith { d.optString(it).take(5000) }.orEmpty() },
            profileFavoriteComicIds = obj.optJSONArray("profileFavoriteComicIds").let { a -> (0 until (a?.length() ?: 0)).map { a!!.optString(it).take(200) }.filter { it.isNotBlank() }.distinct().take(4) },
            profileFavoriteCharacters = obj.optJSONArray("profileFavoriteCharacters").let { a -> (0 until (a?.length() ?: 0)).map { a!!.optString(it).take(60) }.filter { it.isNotBlank() }.distinct().take(4) },
            themeId = obj.optString("themeId", def.themeId),
            colorScheme = obj.optString("colorScheme", def.colorScheme),
            glassLevel = obj.optInt("glassLevel", def.glassLevel).coerceIn(0, 100),
            glassVariant = obj.optString("glassVariant", def.glassVariant),
            edgeRefraction = obj.optBoolean("edgeRefraction", def.edgeRefraction),
            glassIntensity = obj.optDouble("glassIntensity", def.glassIntensity.toDouble()).toFloat().coerceIn(0.0f, 2.0f),
            glassBlurIntensity = obj.optDouble("glassBlurIntensity", def.glassBlurIntensity.toDouble()).toFloat().coerceIn(0.2f, 3.0f),
            enableLensRefraction = obj.optBoolean("enableLensRefraction", def.enableLensRefraction),
            enableChromaticAberration = obj.optBoolean("enableChromaticAberration", def.enableChromaticAberration),
            glassOpacity = obj.optDouble("glassOpacity", def.glassOpacity.toDouble()).toFloat().coerceIn(0.2f, 2.0f),
            backgroundExtension = obj.optBoolean("backgroundExtension", def.backgroundExtension),
            showBottomAccessory = obj.optBoolean("showBottomAccessory", def.showBottomAccessory),
            showReadingNowBar = obj.optBoolean("showReadingNowBar", def.showReadingNowBar),
            showFloatingMenu = obj.optBoolean("showFloatingMenu", def.showFloatingMenu),
            minimizeTabBarOnScroll = obj.optBoolean("minimizeTabBarOnScroll", def.minimizeTabBarOnScroll),
            reduceTransparency = obj.optBoolean("reduceTransparency", def.reduceTransparency),
            increaseContrast = obj.optBoolean("increaseContrast", def.increaseContrast),
            boldText = obj.optBoolean("boldText", def.boldText),
            dynamicTypeScale = obj.optInt("dynamicTypeScale", def.dynamicTypeScale).coerceIn(85, 115),
            reduceMotion = obj.optBoolean("reduceMotion", def.reduceMotion),
            onOffLabels = obj.optBoolean("onOffLabels", def.onOffLabels),
            shelfColumns = obj.optInt("shelfColumns", def.shelfColumns).coerceIn(2, 4),
            showProgressOnCovers = obj.optBoolean("showProgressOnCovers", def.showProgressOnCovers),
            showRatingsOnGrid = obj.optBoolean("showRatingsOnGrid", def.showRatingsOnGrid),
            spine3dEffect = obj.optBoolean("spine3dEffect", def.spine3dEffect),
            defaultShelfSort = obj.optString("defaultShelfSort", def.defaultShelfSort),
            shelfLayout = obj.optString("shelfLayout", def.shelfLayout).ifBlank { "grid" },
            defaultFormat = obj.optString("defaultFormat", def.defaultFormat),
            searchLanguage = obj.optString("searchLanguage", def.searchLanguage),
            searchProvider = obj.optString("searchProvider", def.searchProvider),
            hdCovers = obj.optBoolean("hdCovers", def.hdCovers),
            spoilerProtection = obj.optBoolean("spoilerProtection", def.spoilerProtection),
            halfStarRatings = obj.optBoolean("halfStarRatings", def.halfStarRatings),
            allowRereads = obj.optBoolean("allowRereads", def.allowRereads),
            autoFillReadDate = obj.optBoolean("autoFillReadDate", def.autoFillReadDate),
            showPagesInDiary = obj.optBoolean("showPagesInDiary", def.showPagesInDiary),
            shareStarsMatchTheme = obj.optBoolean("shareStarsMatchTheme", def.shareStarsMatchTheme),
            cachedRecommendationsJson = validCachedRecs,
            recommendationTasteHash = obj.optString("recommendationTasteHash", def.recommendationTasteHash).take(64),
            settingsUpdatedAt = obj.optLong("settingsUpdatedAt", 0L),
            launcherIconMode = obj.optString("launcherIconMode", def.launcherIconMode).take(30),
            quickReviewTags = if (tagsArr != null) tagsList else def.quickReviewTags
        )
    }
}
