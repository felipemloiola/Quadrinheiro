package com.example.reading

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import com.example.R
import com.example.data.SafeStorage
import com.example.model.*
import com.example.widget.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicLong

/** User-started reading session. Standard templates remain eligible for system Live Updates. */
object ReadingLiveActivity {
    internal const val ID = 4201
    internal const val CHANNEL = "reading_session"
    private val generation = AtomicLong()
    private val publicationLock = Any()
    private var refreshJob: Job? = null
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val active = MutableStateFlow(false)
    private fun preferences(context: Context) = context.getSharedPreferences("reading_live_session", Context.MODE_PRIVATE)
    private fun currentOwner(context: Context): String? {
        val storage = SafeStorage(context)
        return storage.loadAuthSession()?.uid ?: if (storage.isGuestMode()) "guest_local" else null
    }

    fun isActive(context: Context): Boolean = preferences(context).contains("owner")
    fun stop(context: Context) = synchronized(publicationLock) {
        generation.incrementAndGet()
        preferences(context).edit().clear().apply()
        active.value = false
        NotificationManagerCompat.from(context).cancel(ID)
    }

    @Synchronized
    fun refreshAsync(context: Context) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            try { refresh(context.applicationContext) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* A later persisted-state refresh retries the session. */ }
        }
    }

    suspend fun start(context: Context, comicId: String) = withContext(Dispatchers.IO) {
        val session = SafeStorage(context).loadWidgetSessionSnapshot()
        val owner = session.ownerUid ?: return@withContext
        val comic = session.state.comics.firstOrNull { it.id == comicId && it.status == ReadingStatus.READING } ?: return@withContext
        preferences(context).edit().putString("owner", owner).putString("comic", comicId).apply()
        publish(context, session, comic, generation.incrementAndGet())
    }

    suspend fun refresh(context: Context) = withContext(Dispatchers.IO) {
        val preferences = preferences(context)
        val owner = preferences.getString("owner", null) ?: return@withContext
        val snapshot = SafeStorage(context).loadWidgetSessionSnapshot()
        if (snapshot.ownerUid != owner) { stop(context); return@withContext }
        val previous = snapshot.state.comics.firstOrNull { it.id == preferences.getString("comic", null) }
        if (previous == null || previous.status != ReadingStatus.READING) { stop(context); return@withContext }
        val comic = activeReadingComic(snapshot.state.comics, snapshot.state.profile.activeReadingComicId)
        if (comic == null) { stop(context); return@withContext }
        publish(context, snapshot, comic, generation.incrementAndGet())
    }

    @Suppress("MissingPermission")
    private suspend fun publish(context: Context, snapshot: SafeStorage.WidgetSessionSnapshot, comic: ComicEntry, revision: Long) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val owner = snapshot.ownerUid ?: return
        val palette = WidgetVisualRenderer.resolvePaletteForThemeId(snapshot.state.settings.themeId)
        val cover = WidgetVisualRenderer.loadOrRenderCoverBitmap(context, comic, palette, 120, 180)
        if (revision != generation.get() || preferences(context).getString("owner", null) != owner || currentOwner(context) != owner) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Sessão de leitura", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Leitura iniciada por você, com progresso e controles de páginas"; setShowBadge(false)
        })
        val open = PendingIntent.getActivity(context, ID, QuadrinheiroWidgetIntents.buildOpenReadingComicIntent(context, comic.id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val total = comic.pageCount.coerceAtLeast(1)
        val page = comic.currentPage.coerceIn(0, total)
        val percentage = page * 100 / total
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_reading_notification).setLargeIcon(cover)
            .setContentTitle(comic.displayTitle).setContentText("Pág. $page de $total · $percentage%")
            .setSubText("Lendo agora").setContentIntent(open)
            .setProgress(total, page, false).setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(palette.outerRingColor.toArgb()).setRequestPromotedOngoing(true)
            .setShortCriticalText("$percentage%")
            .addAction(0, "− 1 pág.", action(context, owner, comic.id, -1))
            .addAction(0, "+ 1 pág.", action(context, owner, comic.id, 1))
            .addAction(0, "Encerrar", action(context, owner, comic.id, 0))
            .setDeleteIntent(action(context, owner, comic.id, 0)).build()
        synchronized(publicationLock) {
            if (revision != generation.get() || preferences(context).getString("owner", null) != owner || currentOwner(context) != owner) return
            preferences(context).edit().putString("comic", comic.id).apply()
            try { NotificationManagerCompat.from(context).notify(ID, notification) }
            catch (_: SecurityException) { stop(context); return }
            active.value = true
        }
    }

    private fun action(context: Context, owner: String, comic: String, delta: Int): PendingIntent =
        PendingIntent.getBroadcast(context, ID + delta, Intent(context, ReadingSessionAction::class.java)
            .setAction("reading.session.$delta").putExtra("owner", owner).putExtra("comic", comic).putExtra("delta", delta),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    internal suspend fun handle(context: Context, owner: String?, comic: String?, delta: Int) {
        if (owner == null || owner != preferences(context).getString("owner", null) || owner != SafeStorage(context).loadWidgetSessionSnapshot().ownerUid) return
        if (delta == 0) { stop(context); return }
        if (comic != preferences(context).getString("comic", null) || delta !in listOf(-1, 1)) return
        UpdatePageAction.applyPageDelta(context, comic, delta, owner)
        refresh(context)
        QuadrinheiroWidgetsSync.updateAllWidgets(context)
    }
}

class ReadingSessionAction : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        ReadingLiveActivity.scope.launch {
            try { ReadingLiveActivity.handle(context.applicationContext, intent.getStringExtra("owner"), intent.getStringExtra("comic"), intent.getIntExtra("delta", 0)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Keep the persisted session available for the next refresh. */ }
            finally { result.finish() }
        }
    }
}
