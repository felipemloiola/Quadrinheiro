package com.example.data

import android.content.Context
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ComicMoment
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
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

    data class StoredState(
        val comics: List<ComicEntry>,
        val profile: ReaderProfile,
        val settings: AppSettings
    )

    fun loadState(): StoredState {
        val raw = getItem(STORAGE_KEY) ?: return StoredState(
            comics = CuratedCatalog.initialUserCollection,
            profile = ReaderProfile(),
            settings = AppSettings()
        )
        return try {
            val root = JSONObject(raw)
            val comicsArr = root.optJSONArray("comics")
            val comics = if (comicsArr != null && comicsArr.length() > 0) parseComics(comicsArr) else CuratedCatalog.initialUserCollection
            val profile = root.optJSONObject("profile")?.let { parseProfile(it) } ?: ReaderProfile()
            val settings = root.optJSONObject("settings")?.let { parseSettings(it) } ?: AppSettings()
            StoredState(comics, profile, settings)
        } catch (_: Throwable) {
            StoredState(CuratedCatalog.initialUserCollection, ReaderProfile(), AppSettings())
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
            out.add(
                ComicEntry(
                    id = idVal,
                    externalId = extIdVal,
                    title = titleVal,
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
                    synopsis = obj.optString("synopsis", ""),
                    sourceLabel = obj.optString("sourceLabel", "Catálogo Base"),
                    moments = momentsList,
                    seriesName = obj.optString("seriesName", ""),
                    issueNumber = obj.optInt("issueNumber", 0)
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
            showGoalOnHome = obj.optBoolean("showGoalOnHome", def.showGoalOnHome)
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
