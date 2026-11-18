package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private object CommunityMemory {
    data class Snapshot(val page:CommunityPage<CommunityReview>,val revision:Long,val savedAt:Long=System.currentTimeMillis())
    private val pages=object:LinkedHashMap<String,Snapshot>(8,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,Snapshot>?)=size>8}
    @Synchronized fun get(key:String)=pages[key]?.takeIf {System.currentTimeMillis()-it.savedAt<60000}
    @Synchronized fun put(key:String,page:CommunityPage<CommunityReview>,revision:Long){pages[key]=Snapshot(page,revision)}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommunityScreen(comics: List<ComicEntry>, profile: ReaderProfile, onLogin: () -> Unit, comic: ComicEntry? = null) {
    val context = LocalContext.current
    val storage = remember { SafeStorage(context) }
    val repo = remember { runCatching { CommunityRepository(context) }.getOrNull() }
    val uid = repo?.activeAccountUid()
    val t = LocalIOS27Theme.current
    if (repo == null || uid == null) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeading("Histórias para conversar", "Entre para ver avaliações, publicar resenhas e conversar com outros leitores.")
            TextButton(onClick = onLogin) { Text("Entrar", color = t.accent) }
        }; return
    }
    val comicFilterKey = comic?.let(::communityComicKey)
    key(uid, comicFilterKey) {
        val scope = rememberCoroutineScope()
        val cacheKey = uid + ":" + (comicFilterKey ?: "feed")
        val publicationRevision by CommunityAutoSync.revision.collectAsState()
        val localRatedPosts = remember(comics, profile, uid, publicationRevision, comicFilterKey) {
            CommunityAutoSync.localReviews(storage, uid, comics, profile, comicFilterKey)
        }
        val cached = remember(cacheKey) { CommunityMemory.get(cacheKey) }
        var cloudPosts by remember(cacheKey) { mutableStateOf(cached?.page?.items.orEmpty()) }
        val posts = remember(localRatedPosts, cloudPosts) {
            (cloudPosts + localRatedPosts)
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt.toDate().time }
                .take(100)
        }
        var cursor by remember { mutableStateOf<CommunityCursor?>(null) }
        var more by remember { mutableStateOf(false) }
        var attempted by remember { mutableStateOf(cached != null || localRatedPosts.isNotEmpty()) }
        var busy by remember { mutableStateOf(false) }
        var failedRefresh by remember { mutableStateOf<Boolean?>(null) }
        var discussion by remember { mutableStateOf<CommunityReview?>(null) }

        fun load(refresh: Boolean = false) {
            if (busy) return
            attempted = true
            val firebaseUid = repo.currentUid()
            if (firebaseUid == null) {
                failedRefresh = null
                more = false
                return
            }
            busy = true
            failedRefresh = null
            scope.launch {
                try {
                    if (refresh) {
                        try {
                            CommunityAutoSync.flush(context, firebaseUid, storage)
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            // The feed can still be fetched while a pending publication waits.
                        }
                    }
                    val page = withTimeoutOrNull(8000) {
                        repo.reviews(if (refresh) null else cursor, comicFilterKey)
                    }
                    if (page == null) {
                        failedRefresh = refresh
                        return@launch
                    }
                    if (refresh) CommunityMemory.put(cacheKey, page, publicationRevision)
                    if (repo.activeAccountUid() == uid) {
                        cloudPosts = ((if (refresh) emptyList() else cloudPosts) + page.items).filter { it.id.isNotBlank() }.distinctBy { it.id }.takeLast(100)
                        cursor = page.cursor
                        more = page.hasMore
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    failedRefresh = refresh
                } finally {
                    busy = false
                }
            }
        }
        LaunchedEffect(cacheKey, publicationRevision) {
            if (cached == null || cached.revision != publicationRevision) load(true)
            else { cursor = cached.page.cursor; more = cached.page.hasMore }
        }
        LazyColumn(Modifier.fillMaxSize().testTag("community_feed"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 208.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (posts.isEmpty() && attempted && !busy && failedRefresh == null) item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = t.textSecondary, modifier = Modifier.size(32.dp))
                    Text("Sem avaliações por enquanto", color = t.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("As avaliações e resenhas aparecem aqui quando uma leitura é avaliada.", color = t.textSecondary, fontSize = 14.sp, lineHeight = 21.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            if (posts.isEmpty() && (!attempted || busy)) items(3) {
                Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(22.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(56.dp, 84.dp).background(t.textTertiary.copy(alpha = .10f), RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.fillMaxWidth(.5f).height(12.dp).background(t.textTertiary.copy(alpha = .12f), RoundedCornerShape(6.dp)))
                            Box(Modifier.fillMaxWidth(.85f).height(18.dp).background(t.textTertiary.copy(alpha = .12f), RoundedCornerShape(6.dp)))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(12.dp).background(t.textTertiary.copy(alpha = .10f), RoundedCornerShape(6.dp)))
                }
            }
            items(posts, key = { it.id }) { post ->
                Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(22.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BookCover(post.cover, post.title, spine3d = false, modifier = Modifier.width(56.dp).height(84.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            val authorLabel = if (post.handle.isNotBlank() && !post.author.contains(post.handle)) "${post.author} • ${post.handle}" else post.author
                            Text(authorLabel, color = t.textSecondary, fontSize = 13.sp)
                            Text(post.title, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
                            if (post.rating > 0) Text("★ ${post.rating.toString().removeSuffix(".0")}", color = t.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (post.body.isNotBlank()) Text(post.body, color = t.textPrimary, lineHeight = 23.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { discussion = post }) {
                            Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(18.dp), tint = t.accent)
                            Spacer(Modifier.width(8.dp)); Text("Comentários", color = t.accent)
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (failedRefresh != null) Text("Não foi possível carregar as publicações.", color = t.textSecondary)
                    if (busy && posts.isNotEmpty()) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingSpinner(Modifier.size(24.dp), color = t.accent, strokeWidth = 2.dp) }
                    else if (!busy && (more || failedRefresh != null)) TextButton(onClick = { load(failedRefresh ?: false) }) {
                        Text(if (failedRefresh == null) "Ver mais" else "Tentar novamente", color = t.accent)
                    }
                }
            }
        }
        discussion?.let { post -> CommunityComments(repo, storage, uid, profile, post) { discussion = null } }
    }
}

@Composable
private fun CommunityComments(
    repo: CommunityRepository,
    storage: SafeStorage,
    uid: String,
    profile: ReaderProfile,
    post: CommunityReview,
    onClose: () -> Unit
) {
    val t = LocalIOS27Theme.current
    val scope = rememberCoroutineScope()
    var comments by remember(post.id) {
        mutableStateOf(CommunityAutoSync.localComments(storage, post.id).filter { it.ownerId == uid })
    }
    var cursor by remember { mutableStateOf<CommunityCursor?>(null) }
    var more by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var commentLoadFailure by remember { mutableStateOf<Boolean?>(null) }
    var deleteFailed by remember { mutableStateOf(false) }
    fun load(refresh: Boolean = false) {
        if (loading) return
        val localList = CommunityAutoSync.localComments(storage, post.id).filter { it.ownerId == uid }
        if (repo.currentUid() == null) {
            comments = localList
            more = false
            return
        }
        loading = true
        commentLoadFailure = null
        scope.launch {
            try {
                val page = withTimeoutOrNull(8000) { repo.comments(post.id, if (refresh) null else cursor) }
                if (page == null) {
                    commentLoadFailure = refresh
                    return@launch
                }
                if (repo.activeAccountUid() == uid) {
                    comments = ((if (refresh) emptyList() else comments) + page.items + localList)
                        .distinctBy { it.id }
                        .sortedByDescending { it.createdAt.toDate().time }
                        .takeLast(100)
                    cursor = page.cursor; more = page.hasMore
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                commentLoadFailure = refresh
            } finally {
                loading = false
            }
        }
    }
    LaunchedEffect(post.id) { load(true) }
    CurationSheet("Comentários", onClose, glass = true) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(post.title, style = MaterialTheme.typography.titleSmall, color = t.textSecondary)
            key(uid, post.id) {
                CommunityCommentComposer(enabled = !loading, onRefresh = { load(true) }) { text ->
                    if (repo.currentUid() != null) {
                        repo.comment(uid, post.id, profile, text)
                    } else {
                        check(repo.activeAccountUid() == uid)
                        CommunityAutoSync.addLocalComment(storage, uid, post.id, profile, text)
                    }
                }
            }
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(comments, key = { it.id }) { comment ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(comment.author, color = t.textSecondary, fontSize = 13.sp)
                        Text(comment.body, color = t.textPrimary)
                        if (comment.ownerId == uid || post.ownerId == uid) CurationGlassAction("Excluir", onClick = {
                            scope.launch {
                                try {
                                    deleteFailed = false
                                    if (comment.id.startsWith("local_c_")) {
                                        check(repo.activeAccountUid() == uid)
                                        CommunityAutoSync.deleteLocalComment(storage, post.id, comment.id)
                                    } else {
                                        repo.deleteComment(uid, post.id, comment.id)
                                    }
                                    comments = comments.filterNot { it.id == comment.id }
                                } catch (_: TimeoutCancellationException) {
                                    deleteFailed = true
                                    load(true)
                                } catch (cancel: CancellationException) {
                                    throw cancel
                                } catch (_: Exception) {
                                    deleteFailed = true
                                }
                            }
                        })
                        HorizontalDivider(color = t.divider)
                    }
                }
                item {
                    if (deleteFailed) Text("Não foi possível excluir o comentário.", color = t.textSecondary)
                    if (loading) LoadingSpinner(Modifier.size(24.dp), color = t.accent)
                    else if (commentLoadFailure != null) {
                        Text("Não foi possível carregar os comentários.", color = t.textSecondary)
                        LiquidGlassPillButton("Tentar novamente", onClick = { load(commentLoadFailure ?: false) })
                    } else if (more) LiquidGlassPillButton("Carregar comentários", onClick = { load() })
                    else if (comments.isEmpty()) Text("Ainda não há comentários.", color = t.textSecondary)
                }
            }
        }
    }
}

@Composable
internal fun CommunityCommentComposer(enabled: Boolean, onRefresh: () -> Unit, onSend: suspend (String) -> Unit) {
    val theme = LocalIOS27Theme.current
    val scope = rememberCoroutineScope()
    var body by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    Column {
        AppleTextField(body, {
            body = it.take(1200)
            sendError = null
        }, "Adicione um comentário", usePlaceholderAsLabel = true)
        sendError?.let { Text(it, color = theme.textSecondary) }
        LiquidGlassPillButton(
            label = when {
                sending -> "Enviando…"
                sendError != null -> "Tentar enviar novamente"
                else -> "Enviar"
            },
            prominent = true,
            enabled = enabled && body.isNotBlank() && !sending,
            onClick = {
                val submittedBody = body
                sending = true
                sendError = null
                scope.launch {
                    try {
                        onSend(submittedBody.trim())
                        if (body == submittedBody) body = ""
                        onRefresh()
                    } catch (_: TimeoutCancellationException) {
                        sendError = "O envio não foi confirmado. Confira os comentários antes de tentar novamente."
                        onRefresh()
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        sendError = "Não foi possível enviar. Seu texto foi mantido."
                    } finally {
                        sending = false
                    }
                }
            }
        )
    }
}
