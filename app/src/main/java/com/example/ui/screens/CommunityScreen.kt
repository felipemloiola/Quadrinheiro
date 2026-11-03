package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommunityScreen(comics: List<ComicEntry>, profile: ReaderProfile, onLogin: () -> Unit, comic: ComicEntry? = null) {
    val context = LocalContext.current
    val repo = remember { CommunityRepository(context) }
    val uid = repo.currentUid()
    val t = LocalIOS27Theme.current
    if (uid == null) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeading("Histórias para conversar", "Entre para ver avaliações, publicar resenhas e conversar com outros leitores.")
            TextButton(onClick = onLogin) { Text("Entrar", color = t.accent) }
        }; return
    }
    key(uid) {
        val scope = rememberCoroutineScope()
        var posts by remember { mutableStateOf(emptyList<CommunityReview>()) }
        var cursor by remember { mutableStateOf<CommunityCursor?>(null) }
        var more by remember { mutableStateOf(true) }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var composing by remember { mutableStateOf(false) }
        var discussion by remember { mutableStateOf<CommunityReview?>(null) }
        fun load(refresh: Boolean = false) {
            if (busy) return
            busy = true; error = null
            scope.launch {
                try {
                    val page = repo.reviews(if (refresh) null else cursor, comic?.let(::communityComicKey))
                    if (repo.currentUid() == uid) {
                        posts = ((if (refresh) emptyList() else posts) + page.items).distinctBy { it.id }.takeLast(100)
                        cursor = page.cursor; more = page.hasMore
                    }
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { error = "Não foi possível carregar as publicações. Tente novamente." }
                finally { busy = false }
            }
        }
        LaunchedEffect(uid) { load(true) }
        LazyColumn(Modifier.fillMaxSize().testTag("community_feed"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 208.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { load(true) }, enabled = !busy) { Text("Atualizar", color = t.accent) }
                    TextButton(onClick = { composing = true }) { Text("Publicar", color = t.accent) }
                }
            }
            if (posts.isEmpty() && !busy && error == null) item {
                SectionHeading("A conversa começa com uma leitura", "Publique uma avaliação ou resenha da sua biblioteca.")
            }
            items(posts, key = { it.id }) { post ->
                Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(22.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BookCover(post.cover, post.title, spine3d = false, modifier = Modifier.width(56.dp).height(84.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(post.author, color = t.textSecondary, fontSize = 13.sp)
                            Text(post.title, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
                            if (post.rating > 0) Text("★ ${post.rating.toString().removeSuffix(".0")}", color = t.accent)
                        }
                    }
                    if (post.body.isNotBlank()) Text(post.body, color = t.textPrimary, lineHeight = 23.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { discussion = post }) {
                            Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(18.dp), tint = t.accent)
                            Spacer(Modifier.width(8.dp)); Text("Comentários", color = t.accent)
                        }
                        if (post.ownerId == uid) TextButton(onClick = {
                            scope.launch {
                                try { repo.withdraw(uid, post.id); posts = posts.filterNot { it.id == post.id } }
                                catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) { error = "Não foi possível retirar a publicação." }
                            }
                        }) { Text("Retirar", color = t.textSecondary) }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    error?.let { Text(it, color = t.textSecondary) }
                    if (busy) CircularProgressIndicator(Modifier.size(24.dp), color = t.accent, strokeWidth = 2.dp)
                    else if (more || error != null) TextButton(onClick = { load() }) { Text(if (error == null) "Ver mais" else "Tentar novamente", color = t.accent) }
                }
            }
        }
        if (composing) {
            var selected by remember { mutableStateOf<ComicEntry?>(comic) }
            var body by remember { mutableStateOf("") }
            var publishing by remember { mutableStateOf(false) }
            var failure by remember { mutableStateOf(false) }
            ModalBottomSheet(onDismissRequest = { composing = false }, containerColor = t.bgSecondary) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp).imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { SectionHeading("Publicar leitura", "Seu nome, avaliação e texto ficarão visíveis para outros leitores.") }
                    if (selected == null) {
                        if (comics.isEmpty()) item { Text("Adicione uma HQ à biblioteca para publicar.", color = t.textSecondary) }
                        items(comics, key = { it.id }) { comic ->
                            Row(Modifier.fillMaxWidth().clickable { selected = comic; body = comic.review }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                BookCover(comic.coverUrl, comic.displayTitle, spine3d = false, modifier = Modifier.width(40.dp).height(60.dp))
                                Text(comic.displayTitle, color = t.textPrimary)
                            }
                        }
                    } else item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            TextButton(onClick = { selected = null }) { Text(selected!!.displayTitle, color = t.accent) }
                            if (selected!!.rating > 0) Text("★ ${selected!!.rating}", color = t.accent)
                            OutlinedTextField(body, { body = it.take(5000) }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Resenha") })
                            if (failure) Text("Não foi possível publicar. Tente novamente.", color = t.textSecondary)
                            TextButton(enabled = !publishing && (body.isNotBlank() || selected!!.rating > 0), onClick = {
                                publishing = true; failure = false
                                scope.launch {
                                    try { repo.publish(uid, profile, selected!!, body, selected!!.rating.toDouble()); composing = false; load(true) }
                                    catch (cancel: CancellationException) { throw cancel }
                                    catch (_: Exception) { failure = true }
                                    finally { publishing = false }
                                }
                            }) { Text(if (publishing) "Publicando…" else "Publicar", color = t.accent) }
                        }
                    }
                }
            }
        }
        discussion?.let { post -> CommunityComments(repo, uid, profile, post) { discussion = null } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityComments(repo: CommunityRepository, uid: String, profile: ReaderProfile, post: CommunityReview, onClose: () -> Unit) {
    val t = LocalIOS27Theme.current
    val scope = rememberCoroutineScope()
    var comments by remember(post.id) { mutableStateOf(emptyList<CommunityComment>()) }
    var cursor by remember { mutableStateOf<CommunityCursor?>(null) }
    var more by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var body by remember { mutableStateOf("") }
    fun load(refresh: Boolean = false) {
        if (busy) return
        busy = true; error = false
        scope.launch {
            try {
                val page = repo.comments(post.id, if (refresh) null else cursor)
                if (repo.currentUid() == uid) {
                    comments = ((if (refresh) emptyList() else comments) + page.items).distinctBy { it.id }.takeLast(100)
                    cursor = page.cursor; more = page.hasMore
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { error = true }
            finally { busy = false }
        }
    }
    LaunchedEffect(post.id) { load(true) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = t.bgSecondary) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 600.dp).imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { SectionHeading("Comentários", post.title) }
            item {
                Column {
                    AppleTextField(body, { body = it.take(1200) }, "Adicione um comentário")
                    TextButton(enabled = body.isNotBlank() && !busy, onClick = {
                        busy = true
                        scope.launch {
                            try { repo.comment(uid, post.id, profile, body); body = ""; busy = false; load(true) }
                            catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { error = true; busy = false }
                        }
                    }) { Text("Enviar", color = t.accent) }
                }
            }
            items(comments, key = { it.id }) { comment ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(comment.author, color = t.textSecondary, fontSize = 13.sp)
                    Text(comment.body, color = t.textPrimary)
                    if (comment.ownerId == uid || post.ownerId == uid) TextButton(onClick = {
                        scope.launch {
                            try { repo.deleteComment(uid, post.id, comment.id); comments = comments.filterNot { it.id == comment.id } }
                            catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { error = true }
                        }
                    }) { Text("Excluir", color = t.textSecondary) }
                    HorizontalDivider(color = t.divider)
                }
            }
            item {
                if (error) Text("Não foi possível concluir. Tente novamente.", color = t.textSecondary)
                if (busy) CircularProgressIndicator(Modifier.size(24.dp), color = t.accent)
                else if (more || error) TextButton(onClick = { load() }) { Text("Carregar comentários", color = t.accent) }
                else if (comments.isEmpty()) Text("Ainda não há comentários.", color = t.textSecondary)
            }
        }
    }
}
