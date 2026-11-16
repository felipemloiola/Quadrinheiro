package com.example.data

import android.content.Context
import android.util.AtomicFile
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Compact edition reference, independent of reading/ownership status. Never contains account data. */
data class ListComic(val key: String, val id: String, val externalId: String, val title: String,
    val authors: String = "", val publisher: String = "", val cover: String = "", val isbn: String = "",
    val source: String = "", val language: String = "", val series: String = "", val issue: Int = 0,
    val format: String = "", val genre: String = "", val note: String = "") {
    fun comic() = ComicEntry(id, externalId, title, authors = authors, publisher = publisher,
        coverUrl = cover, isbn = isbn, sourceLabel = source, editionLanguage = language,
        seriesName = series, issueNumber = issue, format = format, genre = genre,
        pageCount = 0, status = ReadingStatus.WANT_TO_READ, isOnlineResult = true)
    companion object {
        fun from(c: ComicEntry): ListComic {
            val isbn = c.isbn.filter { it.isDigit() || it.equals('X', true) }.uppercase()
            val provider = when {
                c.sourceLabel.contains("Metron",true) -> "mt"
                c.sourceLabel.contains("Comic Vine",true) || c.sourceLabel.contains("ComicVine",true) -> "cv"
                c.sourceLabel.contains("Google",true) -> "gb"
                c.sourceLabel.contains("Open Library",true) -> "ol"
                c.sourceLabel.contains("Apple",true) -> "ab"
                c.sourceLabel.contains("Kitsu",true) -> "kitsu"
                c.sourceLabel.contains("MangaDex",true) -> "md"
                else -> c.sourceLabel
            }
            val edition = if (isbn.length in listOf(10, 13)) "isbn:$isbn" else
                "$provider:${c.externalId.ifBlank { c.id }}"
            return ListComic(CurationCodec.hash(edition), c.id.take(300), c.externalId.take(300), c.displayTitle.take(300),
                c.authors.take(500), c.publisher.take(200), c.coverUrl.take(2000), isbn.take(20), c.sourceLabel.take(100),
                c.editionLanguage.take(20), c.seriesName.take(300), c.issueNumber, c.format.take(100), c.genre.take(200))
        }
    }
}

data class ComicList(val id: String = UUID.randomUUID().toString(), val name: String = "",
    val description: String = "", val ranked: Boolean = false, val items: List<ListComic> = emptyList(),
    val revision: Long = 0, val deleted: Boolean = false,
    val coverKey: String = "", val coverX: Float = .5f, val coverY: Float = .5f,
    val coverZoom: Float = 1f, val featuredAt: Long = 0, val isPublic: Boolean = false) {
    fun coverItem(): ListComic? = items.firstOrNull { it.key == coverKey && it.cover.isNotBlank() }
        ?: items.firstOrNull { it.cover.isNotBlank() }
    fun publicSnapshot() = copy(items=items.map {it.copy(note="")},featuredAt=0,revision=0,deleted=false,isPublic=true)
}

/** One deterministic winner without cross-document writes; old backups remain valid. */
fun featuredComicList(lists: List<ComicList>): ComicList? {
    val active = lists.filterNot { it.deleted }
    return active.filter { it.featuredAt > 0 }.maxWithOrNull(compareBy<ComicList> { it.featuredAt }.thenBy { it.id })
        ?: active.firstOrNull { it.coverItem() != null } ?: active.firstOrNull()
}

data class RecommendationFeedback(val hidden: List<ListComic> = emptyList(), val boosted: List<ListComic> = emptyList(),
    val revision: Long = 0) {
    fun hides(c: ComicEntry) = hidden.any { PersonalizedDiscovery.recommendationWorkKey(it.comic()) == PersonalizedDiscovery.recommendationWorkKey(c) }
    fun canonical() = hidden.map { PersonalizedDiscovery.recommendationWorkKey(it.comic()) }.distinct().sorted().joinToString(";") +
        "|more:" + boosted.map { "${it.key}:${it.title}:${it.authors}:${it.publisher}" }.sorted().joinToString(";")
}

internal object CurationCodec {
    const val MAX_BYTES = 256 * 1024
    fun hash(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    fun item(c: ListComic) = JSONObject().put("key", c.key).put("id", c.id).put("externalId", c.externalId)
        .put("title", c.title).put("authors", c.authors).put("publisher", c.publisher).put("cover", c.cover)
        .put("isbn", c.isbn).put("source", c.source).put("language", c.language).put("series", c.series)
        .put("issue", c.issue).put("format", c.format).put("genre", c.genre).put("note", c.note)
    private fun parseItem(o: JSONObject): ListComic {
        fun s(k: String, max: Int): String = o.optString(k).also { require(it.length <= max) }
        return ListComic(s("key",64).also { require(it.matches(Regex("[a-f0-9]{64}"))) }, s("id",300),s("externalId",300),
            s("title",300).also { require(it.isNotBlank()) },s("authors",500),s("publisher",200),s("cover",2000),s("isbn",20),
            s("source",100),s("language",20),s("series",300),o.optInt("issue"),s("format",100),s("genre",200),s("note",1000))
    }
    private fun entries(a: JSONArray, max: Int): List<ListComic> {
        require(a.length() <= max)
        return (0 until a.length()).map { parseItem(a.getJSONObject(it)) }.also { require(it.distinctBy { c -> c.key }.size == it.size) }
    }
    private fun bounded(o: JSONObject) = o.toString().also { require(it.toByteArray().size <= MAX_BYTES) { "A lista excede 256 KiB. Reduza itens ou notas." } }
    fun encode(list: ComicList): String {
        require(list.id.matches(Regex("[a-fA-F0-9-]{36}")))
        require(list.name.isNotBlank() && list.name.length <= 100) { "Use um nome de até 100 caracteres." }
        require(list.description.length <= 2000 && list.items.size <= 200) { "Use até 200 HQs e descrição de até 2000 caracteres." }
        require(list.items.all { it.note.length <= 1000 } && list.items.distinctBy { it.key }.size == list.items.size)
        require(list.coverKey.isEmpty() || list.coverKey.matches(Regex("[a-f0-9]{64}")))
        require(list.coverX.isFinite() && list.coverY.isFinite() && list.coverZoom.isFinite())
        require(list.coverX in 0f..1f && list.coverY in 0f..1f && list.coverZoom in 1f..3f && list.featuredAt >= 0)
        return bounded(JSONObject().put("id",list.id).put("name",list.name).put("description",list.description).put("ranked",list.ranked)
            .put("items",JSONArray(list.items.map(::item))).put("revision",list.revision).put("deleted",list.deleted)
            .put("coverKey",list.coverKey).put("coverX",list.coverX).put("coverY",list.coverY)
            .put("coverZoom",list.coverZoom).put("featuredAt",list.featuredAt).put("isPublic",list.isPublic))
    }
    fun list(raw: String): ComicList {
        require(raw.toByteArray().size <= MAX_BYTES)
        val o=JSONObject(raw)
        return ComicList(o.getString("id"),o.getString("name"),o.optString("description"),o.optBoolean("ranked"),
            entries(o.getJSONArray("items"),200),o.optLong("revision"),o.optBoolean("deleted"),
            o.optString("coverKey"),o.optDouble("coverX",.5).toFloat(),o.optDouble("coverY",.5).toFloat(),
            o.optDouble("coverZoom",1.0).toFloat(),o.optLong("featuredAt"),o.optBoolean("isPublic")).also { encode(it) }
    }
    fun encode(f: RecommendationFeedback) = bounded(JSONObject().put("hidden",JSONArray(f.hidden.map(::item)))
        .put("boosted",JSONArray(f.boosted.map(::item))).put("revision",f.revision).also {
            require(f.hidden.size <= 200 && f.boosted.size <= 30) { "Limite de preferências atingido. Remova uma preferência antes de adicionar outra." }
        })
    fun feedback(raw: String): RecommendationFeedback {
        require(raw.toByteArray().size <= MAX_BYTES)
        val o=JSONObject(raw)
        return RecommendationFeedback(entries(o.optJSONArray("hidden") ?: JSONArray(),200),
            entries(o.optJSONArray("boosted") ?: JSONArray(),30),o.optLong("revision"))
    }
}

internal data class CurationSnapshot(val lists: List<ComicList> = emptyList(), val feedback: RecommendationFeedback = RecommendationFeedback())

/** Account-scoped small durable snapshots/drafts. Not an offline editing mode. */
internal class CurationStorage(context: Context) {
    private val directory=File(context.filesDir,"personal-curation").apply { mkdirs() }
    private fun file(uid: String) = AtomicFile(File(directory,"${CurationCodec.hash(uid)}.json"))
    @Synchronized fun load(uid: String): CurationSnapshot {
        val f=file(uid)
        val input=try { f.openRead() } catch (_: java.io.FileNotFoundException) { return CurationSnapshot() }
        val text=input.use { require(f.baseFile.length() <= 4*1024*1024);it.bufferedReader().readText() }
        val root=JSONObject(text)
        require(root.getString("owner")==uid)
        return decode(root)
    }
    fun decode(root: JSONObject): CurationSnapshot {
        val lists=root.optJSONArray("lists") ?: JSONArray()
        require(lists.length() <= 2100)
        return CurationSnapshot((0 until lists.length()).map { CurationCodec.list(lists.getJSONObject(it).toString()) }
            .also { require(it.distinctBy { l -> l.id }.size == it.size && it.count {l -> !l.deleted}<=100) },
            root.optJSONObject("feedback")?.let { CurationCodec.feedback(it.toString()) } ?: RecommendationFeedback())
    }
    fun json(snapshot: CurationSnapshot) = JSONObject().put("schema",1).put("lists",JSONArray(snapshot.lists.map { JSONObject(CurationCodec.encode(it)) }))
        .put("feedback",JSONObject(CurationCodec.encode(snapshot.feedback)))
    @Synchronized fun save(uid: String, snapshot: CurationSnapshot) {
        require(uid.isNotBlank() && snapshot.lists.size<=2100 && snapshot.lists.count {!it.deleted}<=100)
        val bytes=json(snapshot).put("owner",uid).toString().toByteArray()
        require(bytes.size<=4*1024*1024) { "As listas excedem o limite de armazenamento." }
        val f=file(uid); val stream=f.startWrite()
        try { stream.write(bytes); f.finishWrite(stream) } catch(e: Exception) { f.failWrite(stream); throw e }
    }
    fun saveDraft(uid: String, list: ComicList) {
        val path=File(directory,"${CurationCodec.hash(uid)}-${list.id}-draft.json")
        val bytes=CurationCodec.encode(list).toByteArray()
        require(path.exists() || draftFiles(uid).size<100) { "Há 100 rascunhos. Conclua ou descarte um antes de criar outro." }
        val f=AtomicFile(path); val out=f.startWrite()
        try { out.write(bytes); f.finishWrite(out) } catch(e: Exception) { f.failWrite(out); throw e }
    }
    private fun draftFiles(uid: String): List<File> {
        val prefix=CurationCodec.hash(uid)+"-"
        return directory.listFiles().orEmpty().map {File(it.path.removeSuffix(".bak"))}.distinctBy {it.name}
            .filter {it.name.startsWith(prefix) && it.name.endsWith("-draft.json")}
            .sortedByDescending {it.lastModified()}
    }
    private fun readDraft(file: File): ComicList {
        val f=AtomicFile(file)
        return f.openRead().use {input -> require(f.baseFile.length()<=CurationCodec.MAX_BYTES);CurationCodec.list(input.bufferedReader().readText())}
    }
    fun drafts(uid: String): List<ComicList> = draftFiles(uid).take(100).mapNotNull {runCatching {readDraft(it)}.getOrNull()}.distinctBy {it.id}
    fun draft(uid: String): ComicList? = drafts(uid).firstOrNull()
    fun clearDraft(uid: String, id: String?=null) {
        draftFiles(uid).forEach { file ->
            if(id==null || runCatching {readDraft(file).id==id}.getOrDefault(false)) AtomicFile(file).delete()
        }
    }
}
