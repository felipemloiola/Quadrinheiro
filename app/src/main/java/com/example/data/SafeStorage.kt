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
        const val LOCAL_ACCOUNTS_KEY = "quadrinheiro_local_email_accounts_v1"
        private val memoryFallback = mutableMapOf<String, String>()
    }

    fun getItem(key: String = STORAGE_KEY): String? {
        return try {
            prefs?.getString(key, null) ?: memoryFallback[key]
        } catch (_: Throwable) {
            memoryFallback[key]
        }
    }

    fun setItem(key: String = STORAGE_KEY, value: String) {
        memoryFallback[key] = value
        try {
            prefs?.edit()?.putString(key, value)?.apply()
        } catch (_: Throwable) {
        }
    }

    fun removeItem(key: String) {
        memoryFallback.remove(key)
        try {
            prefs?.edit()?.remove(key)?.apply()
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
    fun saveComicTranslation(translated: ComicEntry) {
        val tPt = translated.titlePtBr.ifBlank { translated.title }
        val sPt = translated.synopsisPtBr.ifBlank { translated.synopsis }
        if (sPt.isBlank() && tPt.isBlank()) return

        // 1. Salva no mapa de cache persistente por comicId / externalId
        try {
            val rawCache = getItem(TRANSLATION_CACHE_KEY)
            val cacheRoot = if (!rawCache.isNullOrBlank()) JSONObject(rawCache) else JSONObject()
            val entryObj = JSONObject().apply {
                put("titlePtBr", tPt)
                put("synopsisPtBr", sPt)
            }
            if (translated.id.isNotBlank()) {
                cacheRoot.put(translated.id, entryObj)
            }
            if (translated.externalId.isNotBlank()) {
                cacheRoot.put(translated.externalId, entryObj)
            }
            setItem(TRANSLATION_CACHE_KEY, cacheRoot.toString())
        } catch (_: Throwable) {
        }

        // 2. Se o quadrinho já estiver salvo no estado principal do SafeStorage, atualiza diretamente o objeto
        try {
            val rawState = getItem(STORAGE_KEY) ?: return
            val root = JSONObject(rawState)
            val comicsArr = root.optJSONArray("comics") ?: return
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
                        obj.put("synopsis", sPt)
                        obj.put("updatedAt", System.currentTimeMillis())
                        obj.put("isDirty", true)
                        updatedAny = true
                    }
                    if (tPt.isNotBlank() && obj.optString("titlePtBr", "") != tPt) {
                        obj.put("titlePtBr", tPt)
                        obj.put("title", tPt)
                        obj.put("updatedAt", System.currentTimeMillis())
                        obj.put("isDirty", true)
                        updatedAny = true
                    }
                }
            }
            if (updatedAny) {
                setItem(STORAGE_KEY, root.toString())
            }
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun markComicsSynced(syncedIds: Set<String>, syncedAtMillis: Long = System.currentTimeMillis()) {
        if (syncedIds.isEmpty()) return
        try {
            val rawState = getItem(STORAGE_KEY) ?: return
            val root = JSONObject(rawState)
            val comicsArr = root.optJSONArray("comics") ?: return
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
                setItem(STORAGE_KEY, root.toString())
            }
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun markProfileSynced(syncedAtMillis: Long = System.currentTimeMillis()) {
        try {
            val rawState = getItem(STORAGE_KEY) ?: return
            val root = JSONObject(rawState)
            val profObj = root.optJSONObject("profile") ?: return
            if (profObj.optBoolean("isDirty", false)) {
                profObj.put("isDirty", false)
                if (profObj.optLong("updatedAt", 0L) <= 0L) {
                    profObj.put("updatedAt", syncedAtMillis)
                }
                setItem(STORAGE_KEY, root.toString())
            }
        } catch (_: Throwable) {
        }
    }

    fun loadState(): StoredState {
        val persistedTranslations = loadTranslationCache()
        val raw = getItem(STORAGE_KEY)
        if (raw == null) {
            val starter = CuratedCatalog.initialUserCollection
            PortugueseComicTranslator.seedPersistedTranslations(starter, persistedTranslations)
            return StoredState(
                comics = starter,
                profile = ReaderProfile(),
                settings = AppSettings()
            )
        }
        return try {
            val root = JSONObject(raw)
            val comicsArr = root.optJSONArray("comics")
            val comics = if (comicsArr != null && comicsArr.length() > 0) parseComics(comicsArr) else CuratedCatalog.initialUserCollection
            PortugueseComicTranslator.seedPersistedTranslations(comics, persistedTranslations)
            val profile = root.optJSONObject("profile")?.let { parseProfile(it) } ?: ReaderProfile()
            val settings = root.optJSONObject("settings")?.let { parseSettings(it) } ?: AppSettings()
            StoredState(comics, profile, settings)
        } catch (_: Throwable) {
            val starter = CuratedCatalog.initialUserCollection
            PortugueseComicTranslator.seedPersistedTranslations(starter, persistedTranslations)
            StoredState(starter, ReaderProfile(), AppSettings())
        }
    }

    fun saveState(comics: List<ComicEntry>, profile: ReaderProfile, settings: AppSettings) {
        val json = buildBackupJsonString(comics, profile, settings, pretty = false)
        setItem(STORAGE_KEY, json)
    }

    fun buildBackupJsonString(
        comics: List<ComicEntry>,
        profile: ReaderProfile,
        settings: AppSettings,
        pretty: Boolean = true
    ): String {
        val root = JSONObject()
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
            obj.put("titlePtBr", c.titlePtBr.ifBlank { c.title })
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
            obj.put("synopsis", c.synopsisPtBr.ifBlank { c.synopsis })
            obj.put("synopsisPtBr", c.synopsisPtBr)
            obj.put("sourceLabel", c.sourceLabel)
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
            val catalogMatch = CuratedCatalog.curatedComicsCatalog.firstOrNull {
                it.id == idVal ||
                    it.externalId == extIdVal ||
                    it.id == extIdVal ||
                    it.id == idVal.replace("init-", "cat-") ||
                    (isbnVal.isNotBlank() && it.isbn == isbnVal) ||
                    (it.title.equals(titleVal, ignoreCase = true) && (rawCover.isBlank() || rawCover.contains("covers.openlibrary.org")))
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
                    title = savedTitlePtBr.ifBlank { titleVal },
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
                    synopsis = savedSynopsisPtBr.ifBlank { savedSynopsis },
                    synopsisPtBr = savedSynopsisPtBr,
                    sourceLabel = obj.optString("sourceLabel", "Catálogo Base"),
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
            updatedAt = obj.optLong("updatedAt", 0L),
            isDirty = obj.optBoolean("isDirty", false)
        )
    }

    private fun serializeSettings(s: AppSettings): JSONObject = JSONObject().apply {
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
        put("defaultFormat", s.defaultFormat)
        put("searchLanguage", s.searchLanguage)
        put("searchProvider", s.searchProvider)
        put("hdCovers", s.hdCovers)
        put("spoilerProtection", s.spoilerProtection)
        put("halfStarRatings", s.halfStarRatings)
        put("allowRereads", s.allowRereads)
        put("autoFillReadDate", s.autoFillReadDate)
        put("showPagesInDiary", s.showPagesInDiary)
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
        return AppSettings(
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
            defaultFormat = obj.optString("defaultFormat", def.defaultFormat),
            searchLanguage = obj.optString("searchLanguage", def.searchLanguage),
            searchProvider = obj.optString("searchProvider", def.searchProvider),
            hdCovers = obj.optBoolean("hdCovers", def.hdCovers),
            spoilerProtection = obj.optBoolean("spoilerProtection", def.spoilerProtection),
            halfStarRatings = obj.optBoolean("halfStarRatings", def.halfStarRatings),
            allowRereads = obj.optBoolean("allowRereads", def.allowRereads),
            autoFillReadDate = obj.optBoolean("autoFillReadDate", def.autoFillReadDate),
            showPagesInDiary = obj.optBoolean("showPagesInDiary", def.showPagesInDiary),
            quickReviewTags = if (tagsArr != null) tagsList else def.quickReviewTags
        )
    }
}
