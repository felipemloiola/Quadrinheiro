from pathlib import Path
import hashlib
import json
import sys

EDITOR = 'app/src/main/java/com/example/ui/components/CatalogModal.kt'
NATIVE = 'app/src/androidTest/java/com/example/ReaderNavigationTest.kt'
EXPECTED = {EDITOR: 'cdc9391e3091b1af9e2c7e1a080938d88396586f', NATIVE: '7906306c812ccb2d8be50833c4c5978c42b40b8d'}
MANIFEST = Path('.git/editor-fix-manifest.json')

def blob(raw):
    return hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()

def transform_editor(raw):
    if blob(raw) != EXPECTED[EDITOR]:
        raise RuntimeError('Editor base changed; refusing to overwrite it')
    text = raw.decode('utf-8')
    marker = '/**\r\n * Modal de Informações'
    tail = text[text.index(marker):]
    def replace(old, new):
        nonlocal text
        if text.count(old) != 1:
            raise RuntimeError('Patch anchor missing or ambiguous: ' + old[:70])
        text = text.replace(old, new, 1)
    replace('import com.example.model.ComicEntry\r\n', 'import com.example.model.ComicEntry\r\nimport com.example.model.CatalogPagePolicy\r\n')
    replace('    var synopsisExpanded by remember(selectedComic.id){mutableStateOf(false)}\r\n', '    var pageEditorOpen by remember(selectedComic.id) { mutableStateOf(false) }\r\n')
    replace('        val count=totalPagesInput.toIntOrNull()?.coerceIn(1,20000) ?: 0\r\n        var finalComic = draftComic.copy(pageCount=count,currentPage=if(draftComic.status==ReadingStatus.READ)count else draftComic.currentPage.coerceIn(0,count))', '        val count = CatalogPagePolicy.count(totalPagesInput) ?: 0\r\n        var finalComic = CatalogPagePolicy.withCount(draftComic, count)')
    start = text.index('    LaunchedEffect(selectedComic.externalId) {')
    end = text.index('    // Prefetching strategy', start)
    metadata = """    fun saveDraft() {
        if (totalPagesInput.isNotBlank() && CatalogPagePolicy.count(totalPagesInput) == null) {
            pagesError = true
            pageEditorOpen = true
            editorScope.launch { sheetScrollState.animateScrollToItem(0) }
        } else onSave(buildFinalComicForSave())
    }
    LaunchedEffect(draftComic.id, draftComic.externalId) {
        val requested = draftComic
        remoteInfo = null
        metadataLoading = true
        try {
            val details = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.ComicVineCatalog.resolve(requested)
            }
            if (details != null && draftComic.id == requested.id && draftComic.externalId == requested.externalId) {
                remoteInfo = details
                CatalogPagePolicy.remoteCount(draftComic.pageCount, isExistingInShelf, pagesEdited, details.pageCount)?.let { count ->
                    totalPagesInput = count.toString()
                    draftComic = CatalogPagePolicy.withCount(draftComic, count)
                    pagesError = false
                }
                draftComic = draftComic.copy(
                    publisher = draftComic.publisher.ifBlank { details.publisher },
                    authors = draftComic.authors.ifBlank { details.creators.filter { it.role.contains("writer", true) || it.role.contains("artist", true) }.take(4).joinToString(", ") { it.name } },
                    isbn = draftComic.isbn.ifBlank { details.isbn },
                    synopsis = details.description.ifBlank { draftComic.synopsis },
                    coverUrl = draftComic.coverUrl.ifBlank { details.image },
                    releaseYear = if (!isExistingInShelf) details.date.take(4).toIntOrNull() ?: draftComic.releaseYear else draftComic.releaseYear
                )
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { }
        finally { metadataLoading = false }
    }
    LaunchedEffect(remoteInfo?.id, remoteInfo?.type) {
        val info = remoteInfo ?: return@LaunchedEffect
        val requestedId = draftComic.id
        val requestedExternalId = draftComic.externalId
        if (info.description.isNotBlank()) try {
            val translated = com.example.data.ComicVineCatalog.translated(info)
            if (remoteInfo?.id == info.id && remoteInfo?.type == info.type && draftComic.id == requestedId &&
                draftComic.externalId == requestedExternalId && translated.description.isNotBlank()) {
                draftComic = draftComic.copy(synopsis = translated.description, synopsisPtBr = translated.description)
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { }
    }
""".replace('\n', '\r\n')
    text = text[:start] + metadata + text[end:]
    replace('title = if (isExistingInShelf) "Editar Quadrinho" else "Registrar",', 'title = if (isExistingInShelf) "Editar HQ" else "Registrar HQ",')
    old_save = '                    onSave = {\r\n                        if (totalPagesInput.isNotBlank() && totalPagesInput.toIntOrNull()?.let { it in 1..20000 } != true){pagesError=true;editorScope.launch {sheetScrollState.animateScrollToItem(0)}}\r\n                        else onSave(buildFinalComicForSave())\r\n                    }'
    replace(old_save, '                    onSave = { saveDraft() }')
    start = text.index('                    androidx.compose.material3.TextButton(onClick = { fullPage = true }')
    end = text.index('// Item 1.5:', start)
    overview = """                    CatalogEditionMetadata(
                        entryId = draftComic.id,
                        synopsis = draftComic.synopsisPtBr.ifBlank { draftComic.synopsis },
                        pages = totalPagesInput,
                        metadataLoading = metadataLoading,
                        pageEditorOpen = pageEditorOpen,
                        pagesError = pagesError,
                        onOpenPageEditor = { pageEditorOpen = true },
                        onChangePages = { value ->
                            pagesEdited = true
                            totalPagesInput = value.filter(Char::isDigit).take(5)
                            CatalogPagePolicy.count(totalPagesInput)?.let { count ->
                                draftComic = CatalogPagePolicy.withCount(draftComic, count)
                                pagesError = false
                            }
                        }
                    )
                    androidx.compose.material3.TextButton(onClick = { fullPage = true }, modifier = Modifier.testTag("open_comic_page"), contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text("Ver página da HQ", color = s.accentColor, fontSize = 14.sp)
                    }
""".replace('\n', '\r\n')
    text = text[:start] + overview + text[end:]
    replace('.clickable { if(totalPagesInput.isBlank() || totalPagesInput.toIntOrNull()?.let {it in 1..20000}==true)onSave(buildFinalComicForSave()) else {pagesError=true;editorScope.launch {sheetScrollState.animateScrollToItem(0)}} }', '.clickable { saveDraft() }')
    for old, new in [('Na sua Fila de Leitura (Quero Ler)', 'Na fila de leitura'), ('Esta edição ficará salva na aba Fila da sua Biblioteca sem avaliação em estrelas até que você a leia.', 'Registre o progresso quando começar a leitura.'), ('text = "Salvar Registro"', 'text = "Salvar"'), ('Excluir Quadrinho da Estante', 'Excluir da biblioteca')]:
        replace(old, new)
    if text[text.index(marker):] != tail:
        raise RuntimeError('Unrelated modal or sidebar changed')
    return text.encode('utf-8')

NEW = {
'app/src/main/java/com/example/model/CatalogPagePolicy.kt': """package com.example.model

internal object CatalogPagePolicy {
    const val MAX_PAGES = 20000
    fun count(raw: String): Int? = raw.toIntOrNull()?.takeIf { it in 1..MAX_PAGES }
    fun remoteCount(current: Int, saved: Boolean, manuallyEdited: Boolean, remote: Int): Int? =
        remote.takeIf { it in 1..MAX_PAGES && !manuallyEdited && (!saved || current <= 1) }
    fun withCount(comic: ComicEntry, count: Int): ComicEntry {
        require(count in 0..MAX_PAGES)
        val current = when (comic.status) {
            ReadingStatus.READ -> count
            ReadingStatus.WANT_TO_READ -> 0
            ReadingStatus.READING -> comic.currentPage.coerceIn(0, count)
        }
        return comic.copy(pageCount = count, currentPage = current)
    }
}
""",
'app/src/main/java/com/example/ui/components/CatalogEditionMetadata.kt': """package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CatalogPagePolicy
import com.example.ui.theme.LocalAppleMaterials

@Composable
internal fun CatalogEditionMetadata(entryId: String, synopsis: String, pages: String,
    metadataLoading: Boolean, pageEditorOpen: Boolean, pagesError: Boolean,
    onOpenPageEditor: () -> Unit, onChangePages: (String) -> Unit) {
    val s = LocalAppleMaterials.current.scheme
    val count = CatalogPagePolicy.count(pages)
    var expanded by remember(entryId) { mutableStateOf(false) }
    var overflowing by remember(synopsis) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (count != null) "$count ${if (count == 1) "página" else "páginas"}"
            else if (metadataLoading) "Buscando dados da edição…" else "Páginas não informadas",
            color = s.textSecondary, fontSize = 13.sp, modifier = Modifier.testTag("edition_page_count"))
        if (count == null && !pageEditorOpen) {
            TextButton(onClick = onOpenPageEditor, contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier.heightIn(min = 44.dp).testTag("inform_edition_pages")) {
                Text("Informar páginas", color = s.accentColor)
            }
        }
        if (pageEditorOpen) {
            AppleTextField(pages, onChangePages, "Páginas (opcional)", modifier = Modifier.testTag("edition_page_count_input"))
        }
        if (pagesError) Text("Use um total entre 1 e 20.000 páginas.", color = s.accentColor, fontSize = 12.sp)
        if (synopsis.isNotBlank()) {
            Text("Sinopse", color = s.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(synopsis, color = s.textSecondary, fontSize = 14.sp, lineHeight = 21.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow }, modifier = Modifier.testTag("edition_synopsis"))
            if (overflowing || expanded) TextButton(onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp), modifier = Modifier.heightIn(min = 44.dp)) {
                Text(if (expanded) "Menos" else "Mais", color = s.accentColor)
            }
        }
    }
}
""",
'app/src/test/java/com/example/CatalogPagePolicyTest.kt': """package com.example

import com.example.model.CatalogPagePolicy
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.Assert.*
import org.junit.Test

class CatalogPagePolicyTest {
    private fun book(status: ReadingStatus = ReadingStatus.READING, current: Int = 50) =
        ComicEntry("book", "mt-1", "HQ", authors = "Autora", publisher = "Editora", status = status,
            pageCount = 160, currentPage = current, rating = 4.5f, review = "Minha resenha", tags = listOf("Pessoal"))
    @Test fun realRemotePagesPopulateNewAndUnknownEditions() {
        assertEquals(144, CatalogPagePolicy.remoteCount(0, false, false, 144))
        assertEquals(144, CatalogPagePolicy.remoteCount(1, true, false, 144))
        assertEquals(1, CatalogPagePolicy.remoteCount(0, false, false, 1))
    }
    @Test fun unknownOrInvalidRemotePagesAreNeverInvented() {
        assertNull(CatalogPagePolicy.remoteCount(0, false, false, 0))
        assertNull(CatalogPagePolicy.remoteCount(0, false, false, 20001))
    }
    @Test fun remoteDataPreservesSavedManualCountsAndActiveUserEdits() {
        assertNull(CatalogPagePolicy.remoteCount(320, true, false, 144))
        assertNull(CatalogPagePolicy.remoteCount(0, false, true, 144))
    }
    @Test fun blankPagesRemainOptionalAndInvalidInputIsRejected() {
        listOf("", "0", "20001", "invalid").forEach { assertNull(CatalogPagePolicy.count(it)) }
        assertEquals(144, CatalogPagePolicy.count("144"))
    }
    @Test fun unknownTotalsDoNotCreateProgressInAnyStatus() {
        ReadingStatus.entries.forEach { status ->
            val saved = CatalogPagePolicy.withCount(book(status), 0)
            assertEquals(0, saved.pageCount); assertEquals(0, saved.currentPage)
        }
    }
    @Test fun applyingPagesPreservesPrivateReviewTagsAndRating() {
        val original = book()
        val edited = CatalogPagePolicy.withCount(original, 144)
        assertEquals(50, edited.currentPage)
        assertEquals(original.review, edited.review); assertEquals(original.tags, edited.tags); assertEquals(original.rating, edited.rating)
        assertEquals(144, CatalogPagePolicy.withCount(book(ReadingStatus.READ), 144).currentPage)
        assertEquals(0, CatalogPagePolicy.withCount(book(ReadingStatus.WANT_TO_READ), 144).currentPage)
        assertEquals(30, CatalogPagePolicy.withCount(book(), 30).currentPage)
    }
}
"""}
TARGETS = [EDITOR, NATIVE, *NEW]

def verify():
    manifest = json.loads(MANIFEST.read_text())
    if set(manifest) != set(TARGETS):
        raise RuntimeError('Unexpected patch targets')
    for name, expected in manifest.items():
        if hashlib.sha256(Path(name).read_bytes()).hexdigest() != expected:
            raise RuntimeError('Patched source changed after preparation: ' + name)

def apply():
    for name, expected in EXPECTED.items():
        if blob(Path(name).read_bytes()) != expected:
            raise RuntimeError('Base changed: ' + name)
    for name in NEW:
        if Path(name).exists():
            raise RuntimeError('New target already exists: ' + name)
    editor = transform_editor(Path(EDITOR).read_bytes())
    native = Path(NATIVE).read_bytes().decode('utf-8')
    old = '        compose.waitUntil(25000) { compose.onAllNodes(hasText("144") and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }'
    new = '        compose.waitUntil(25000) { compose.onAllNodes(hasTestTag("edition_page_count") and hasText("144 páginas")).fetchSemanticsNodes().isNotEmpty() }\n        compose.onNodeWithTag("edition_page_count_input").assertDoesNotExist()'
    if native.count(old) != 1:
        raise RuntimeError('Android test anchor missing or ambiguous')
    contents = {EDITOR: editor, NATIVE: native.replace(old, new, 1).encode('utf-8')}
    contents.update({name: text.encode('utf-8') for name, text in NEW.items()})
    for name, data in contents.items():
        path = Path(name); path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(data)
    MANIFEST.write_text(json.dumps({name: hashlib.sha256(data).hexdigest() for name, data in contents.items()}, indent=2))
    verify()
    print('Prepared exactly five editor/test files; unrelated modal and sidebar content preserved.')

if __name__ == '__main__':
    if sys.argv[1:] == ['apply']:
        apply()
    elif sys.argv[1:] == ['verify']:
        verify()
    else:
        raise SystemExit('Expected apply or verify')
