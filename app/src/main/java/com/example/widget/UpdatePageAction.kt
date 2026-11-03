package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.MainActivity
import com.example.data.MultiApiSearchEngine
import com.example.data.QuadrinheiroCloudRepository
import com.example.data.SafeStorage
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import com.example.ui.theme.AppTheme
import com.example.ui.theme.getIOS27ThemeColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max

/**
 * Chaves de Intent para Deep-Linking dos Widgets Glance direto para telas específicas do Quadrinheiro.
 */
object QuadrinheiroWidgetIntents {
    const val EXTRA_WIDGET_ACTION = "com.example.widget.EXTRA_WIDGET_ACTION"
    const val EXTRA_WIDGET_COMIC_ID = "com.example.widget.EXTRA_WIDGET_COMIC_ID"

    const val ACTION_OPEN_READING_COMIC = "OPEN_READING_COMIC"
    const val ACTION_OPEN_STATISTICS = "OPEN_STATISTICS"
    const val ACTION_OPEN_COMIC_SYNOPSIS = "OPEN_COMIC_SYNOPSIS"

    fun buildOpenReadingComicIntent(context: Context, comicId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, ACTION_OPEN_READING_COMIC)
            putExtra(EXTRA_WIDGET_COMIC_ID, comicId)
        }
    }

    fun buildOpenStatisticsIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, ACTION_OPEN_STATISTICS)
        }
    }

    fun buildOpenComicSynopsisIntent(context: Context, comicId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, ACTION_OPEN_COMIC_SYNOPSIS)
            putExtra(EXTRA_WIDGET_COMIC_ID, comicId)
        }
    }
}

/**
 * Barramento reativo para sincronizar instantaneamente o ViewModel em memória
 * quando o usuário altera páginas diretamente pelo Widget na tela inicial.
 */
object QuadrinheiroWidgetStateBus {
    private val _externalUpdates = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val externalUpdates: SharedFlow<Long> = _externalUpdates.asSharedFlow()

    fun notifyExternalStateChanged() {
        _externalUpdates.tryEmit(System.currentTimeMillis())
    }
}

/**
 * Coordenador central de sincronização em tempo real dos 3 Widgets Glance.
 * Atualiza o estado PreferencesGlanceStateDefinition + StateFlow para forçar recomposição
 * imediata de sessões ativas do Jetpack Glance na tela inicial.
 */
object QuadrinheiroWidgetsSync {
    val PREF_WIDGET_REVISION = longPreferencesKey("widget_sync_revision")
    val PREF_WIDGET_PAGE = intPreferencesKey("widget_active_page")

    private val _widgetRevisionFlow = MutableStateFlow(System.currentTimeMillis())
    val widgetRevisionFlow: StateFlow<Long> = _widgetRevisionFlow.asStateFlow()

    val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Synchronized
    fun bumpInMemoryRevision(): Long {
        val now = maxOf(System.currentTimeMillis(), _widgetRevisionFlow.value + 1L)
        _widgetRevisionFlow.value = now
        return now
    }

    suspend fun updateAllWidgets(context: Context, latestPage: Int? = null, sessionChanged: Boolean = false) {
        val appCtx = context.applicationContext
        com.example.reading.ReadingLiveActivity.refreshAsync(appCtx)
        val rev = bumpInMemoryRevision()
        val manager = runCatching { GlanceAppWidgetManager(appCtx) }.getOrNull()

        if (manager != null) {
            runCatching {
                val readingWidget = ReadingNowWidget()
                val ids = manager.getGlanceIds(ReadingNowWidget::class.java)
                for (gid in ids) {
                    updateAppWidgetState(appCtx, PreferencesGlanceStateDefinition, gid) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[PREF_WIDGET_REVISION] = rev
                            if (sessionChanged) remove(PREF_WIDGET_PAGE)
                            if (latestPage != null) {
                                this[PREF_WIDGET_PAGE] = latestPage
                            }
                        }
                    }
                    readingWidget.update(appCtx, gid)
                }
            }
            runCatching {
                val ringsWidget = ActivityRingsWidget()
                val ids = manager.getGlanceIds(ActivityRingsWidget::class.java)
                for (gid in ids) {
                    updateAppWidgetState(appCtx, PreferencesGlanceStateDefinition, gid) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[PREF_WIDGET_REVISION] = rev
                        }
                    }
                    ringsWidget.update(appCtx, gid)
                }
            }
            runCatching {
                val shelfWidget = RecentShelfWidget()
                val ids = manager.getGlanceIds(RecentShelfWidget::class.java)
                for (gid in ids) {
                    updateAppWidgetState(appCtx, PreferencesGlanceStateDefinition, gid) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[PREF_WIDGET_REVISION] = rev
                        }
                    }
                    shelfWidget.update(appCtx, gid)
                }
            }
        } else {
            runCatching { ReadingNowWidget().updateAll(appCtx) }
            runCatching { ActivityRingsWidget().updateAll(appCtx) }
            runCatching { RecentShelfWidget().updateAll(appCtx) }
        }
    }

    private var debounceJob: kotlinx.coroutines.Job? = null

    fun refreshSessionWidgetsAsync(context: Context) {
        com.example.reading.ReadingLiveActivity.stop(context)
        debounceJob?.cancel()
        // Clear visible data immediately; persist the invalidation for inactive Glance sessions too.
        bumpInMemoryRevision()
        widgetScope.launch { updateAllWidgets(context.applicationContext, sessionChanged = true) }
    }

    fun refreshAllWidgetsAsync(context: Context) {
        val appCtx = context.applicationContext
        debounceJob?.cancel()
        debounceJob = widgetScope.launch {
            kotlinx.coroutines.delay(1200L)
            updateAllWidgets(appCtx)
        }
    }
}

/**
 * ActionCallback oficial do Jetpack Glance para os botões nativos de stepper (- e +) do Widget "Lendo Agora".
 * Atualiza imediatamente o SafeStorage local, dispara a recomposição em tempo real do widget na tela inicial
 * e sincroniza com o Firestore em segundo plano sem bloquear a UI do widget.
 */
class UpdatePageAction : ActionCallback {

    companion object {
        val PARAM_COMIC_ID = ActionParameters.Key<String>("widget_comic_id")
        val PARAM_OWNER_UID = ActionParameters.Key<String>("widget_owner_uid")
        val PARAM_PAGE_DELTA = ActionParameters.Key<Int>("widget_page_delta")

        /**
         * Executa a atualização de página imediatamente no SafeStorage e dispara a sincronização
         * em segundo plano com o Firestore sem atrasar a resposta visual do widget.
         */
        suspend fun applyPageDelta(
            context: Context,
            comicId: String?,
            delta: Int,
            expectedOwnerUid: String? = null
        ): ComicEntry? = withContext(Dispatchers.IO) {
            val appCtx = context.applicationContext
            val storage = SafeStorage(appCtx)
            val sessionSnapshot = storage.loadWidgetSessionSnapshot()
            val ownerUid = sessionSnapshot.ownerUid ?: return@withContext null
            if (expectedOwnerUid != null && expectedOwnerUid != ownerUid) return@withContext null
            val stored = sessionSnapshot.state
            val comics = stored.comics
            if (comics.isEmpty()) return@withContext null

            val targetComic = (if (!comicId.isNullOrBlank()) comics.firstOrNull { it.id == comicId }
                else com.example.model.activeReadingComic(comics, stored.profile.activeReadingComicId))
                ?: return@withContext null

            val totalPages = targetComic.pageCount.coerceAtLeast(1)
            val nextPage = (targetComic.currentPage + delta).coerceIn(0, totalPages)
            val nextStatus = when {
                nextPage >= totalPages -> ReadingStatus.READ
                nextPage > 0 -> ReadingStatus.READING
                else -> targetComic.status
            }
            val nowMs = System.currentTimeMillis()

            var updatedEntry: ComicEntry? = null
            val updatedList = comics.map { entry ->
                if (entry.id == targetComic.id) {
                    entry.copy(
                        currentPage = nextPage,
                        status = nextStatus,
                        updatedAt = nowMs,
                        isDirty = true
                    ).also { updatedEntry = it }
                } else {
                    entry
                }
            }

            if (storage.loadAuthSession()?.uid != ownerUid) return@withContext null
            storage.saveState(
                comics = updatedList,
                profile = stored.profile,
                settings = stored.settings,
                ownerUid = ownerUid
            )

            // Atualiza imediatamente a revisão em memória e notifica o ViewModel se o app estiver aberto
            QuadrinheiroWidgetsSync.bumpInMemoryRevision()
            QuadrinheiroWidgetStateBus.notifyExternalStateChanged()

            // Sincroniza em segundo plano com o Firestore sem bloquear o refresh imediato do widget
            val entryToSync = updatedEntry
            val session = storage.loadAuthSession()
            if (session != null && session.uid == ownerUid && !session.isGuest && entryToSync != null) {
                QuadrinheiroWidgetsSync.widgetScope.launch {
                    runCatching {
                        val repo = QuadrinheiroCloudRepository(appCtx)
                        repo.saveComic(ownerUid, entryToSync)
                        storage.markComicsSynced(setOf(entryToSync.id), nowMs, ownerUid)
                    }
                }
            }

            updatedEntry
        }
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appCtx = context.applicationContext
        val comicId = parameters[PARAM_COMIC_ID]
        val delta = parameters[PARAM_PAGE_DELTA] ?: 1
        val widgetOwnerUid = parameters[PARAM_OWNER_UID]
        // Old RemoteViews may survive an app update; refresh them before accepting edits.
        val updated = if (widgetOwnerUid == null) null else applyPageDelta(appCtx, comicId, delta, widgetOwnerUid)
        val rev = QuadrinheiroWidgetsSync.bumpInMemoryRevision()

        // Atualiza imediatamente o GlanceId clicado para resposta instantânea na tela inicial
        runCatching {
            updateAppWidgetState(appCtx, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[QuadrinheiroWidgetsSync.PREF_WIDGET_REVISION] = rev
                    if (updated != null) {
                        this[QuadrinheiroWidgetsSync.PREF_WIDGET_PAGE] = updated.currentPage
                    }
                }
            }
            ReadingNowWidget().update(appCtx, glanceId)
        }

        // Sincroniza também os demais widgets (ex.: Anéis de Metas e Recentes)
        QuadrinheiroWidgetsSync.updateAllWidgets(appCtx, latestPage = updated?.currentPage)
    }
}

/**
 * Paleta de cores adaptativa para os Widgets Glance baseada no AppTheme selecionado pelo usuário.
 */
data class WidgetThemePalette(
    val bgCard: Color,
    val bgSurfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outerRingColor: Color,
    val innerRingColor: Color,
    val thirdRingColor: Color = innerRingColor,
    val buttonTextOnAccent: Color
)

object WidgetVisualRenderer {

    private val memoryCoverCache = object : LruCache<String, Bitmap>(28) {}

    fun resolveWidgetPalette(storage: SafeStorage): WidgetThemePalette {
        val state = storage.loadWidgetState()
        return resolvePaletteForThemeId(state.settings.themeId, resolveWidgetDark(state.settings.colorScheme, storage.systemIsDark))
    }

    fun resolveWidgetDark(mode: String, systemDark: Boolean): Boolean = when(mode) { "dark" -> true; "light" -> false; else -> systemDark }

    fun resolvePaletteForThemeId(themeId: String, isDark: Boolean = true): WidgetThemePalette {
        // Widgets seguem fielmente a paleta de cores e anéis escolhida pelo usuário no app
        val appTheme = if (themeId == "SYSTEM_MONO") { if (isDark) AppTheme.WHITE else AppTheme.BLACK } else AppTheme.fromId(themeId)
        val themeColors = getIOS27ThemeColors(appTheme, isDark = isDark)
        val (moveColor, exerciseColor, standColor) = com.example.ui.theme.getAppleFitnessRingsColors(appTheme, isDark = isDark)
        val lum = 0.2126f * moveColor.red + 0.7152f * moveColor.green + 0.0722f * moveColor.blue
        val onAccent = if (lum > 0.52f) Color(0xFF0A0A0E) else Color.White

        return WidgetThemePalette(
            bgCard = if (isDark) Color(0xFF1C1C1E) else Color.White,
            bgSurfaceElevated = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
            textPrimary = if (isDark) Color(0xFFF5F5F7) else Color(0xFF101116),
            textSecondary = if (isDark) Color(0xFFA6A6B2) else Color(0xFF50525C),
            outerRingColor = moveColor,
            innerRingColor = exerciseColor,
            thirdRingColor = standColor,
            buttonTextOnAccent = onAccent
        )
    }

    private fun buildCoverCacheKey(comic: ComicEntry, widthPx: Int, heightPx: Int): String {
        return "${comic.id}_${resolvedCoverUrl(comic)}_${widthPx}x${heightPx}"
    }

    /**
     * Carrega a capa em alta resolução (com cache em memória + disco para os widgets) ou gera um pôster estilizado.
     */
    suspend fun loadOrRenderCoverBitmap(
        context: Context,
        comic: ComicEntry,
        palette: WidgetThemePalette,
        widthPx: Int = 204,
        heightPx: Int = 300
    ): Bitmap = withContext(Dispatchers.IO) {
        val cacheKey = buildCoverCacheKey(comic, widthPx, heightPx)
        synchronized(memoryCoverCache) {
            memoryCoverCache.get(cacheKey)?.let { if (!it.isRecycled) return@withContext it }
        }

        val rawBitmap = try {
            val url = resolvedCoverUrl(comic)
            if (url.isBlank()) null else {
                val request = coil.request.ImageRequest.Builder(context.applicationContext)
                    .data(url).size(widthPx, heightPx).allowHardware(false).build()
                val result = kotlinx.coroutines.withTimeoutOrNull(4000L) { coil.Coil.imageLoader(context).execute(request) }
                (result?.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { null }
        if (rawBitmap == null) {
            // A temporary network failure must never poison the successful-image cache.
            return@withContext renderFallbackCoverBitmap(comic, palette, widthPx, heightPx)
        }
        val rendered = createRoundedCoverBitmap(rawBitmap, widthPx, heightPx, cornerRadiusPx = 22f)
        synchronized(memoryCoverCache) { memoryCoverCache.put(cacheKey, rendered) }
        rendered
    }

    internal fun resolvedCoverUrl(comic: ComicEntry): String {
        val url = comic.coverUrl.trim().replace("http://", "https://")
        return if (url.isBlank() || url.contains("covers.openlibrary.org")) {
            com.example.data.CuratedCatalog.findCoverUrlByTitle(comic.title) ?: url
        } else url
    }

    private fun createRoundedCoverBitmap(
        source: Bitmap,
        widthPx: Int,
        heightPx: Int,
        cornerRadiusPx: Float
    ): Bitmap {
        val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val path = Path().apply {
            addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
        }
        canvas.clipPath(path)

        // Center-crop mantendo a proporção natural de capa de HQ (2:3)
        val scale = max(widthPx.toFloat() / source.width.toFloat(), heightPx.toFloat() / source.height.toFloat())
        val scaledW = source.width * scale
        val scaledH = source.height * scale
        val left = (widthPx - scaledW) / 2f
        val top = (heightPx - scaledH) / 2f
        val destRect = RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(source, null, destRect, paint)

        // Borda sutil de vidro
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = android.graphics.Color.argb(55, 255, 255, 255)
        }
        canvas.drawRoundRect(
            RectF(1f, 1f, widthPx - 1f, heightPx - 1f),
            cornerRadiusPx,
            cornerRadiusPx,
            borderPaint
        )
        return output
    }

    private fun renderFallbackCoverBitmap(
        comic: ComicEntry,
        palette: WidgetThemePalette,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val cornerRadiusPx = 22f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, widthPx.toFloat(), heightPx.toFloat(),
                intArrayOf(
                    palette.outerRingColor.copy(alpha = 0.85f).toArgb(),
                    palette.bgSurfaceElevated.toArgb(),
                    palette.bgCard.toArgb()
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = palette.outerRingColor.copy(alpha = 0.55f).toArgb()
        }
        canvas.drawRoundRect(
            RectF(1.5f, 1.5f, widthPx - 1.5f, heightPx - 1.5f),
            cornerRadiusPx,
            cornerRadiusPx,
            borderPaint
        )

        val initials = comic.displayTitle
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "HQ" }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = widthPx * 0.30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val yPos = heightPx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initials, widthPx / 2f, yPos, textPaint)

        return output
    }

    /**
     * Renderiza o gráfico dos Anéis de Atividade concêntricos (Estilo Apple Activity) em Bitmap
     * para o Widget B ("Anéis de Metas de Leitura" 2x2):
     * - Anel externo: Meta anual de quadrinhos concluídos (ex.: 20/24 lidos)
     * - Anel interno: Meta de páginas do dia
     * - No centro dos anéis: Porcentagem de conclusão (ex.: 83%)
     */
    fun renderActivityRingsBitmap(
        outerProgress: Float,
        innerProgress: Float,
        centerPercentage: Int,
        palette: WidgetThemePalette,
        sizePx: Int = 220
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val strokeOuter = sizePx * 0.125f
        val strokeInner = sizePx * 0.125f
        val gap = sizePx * 0.032f

        val outerInset = strokeOuter / 2f + 4f
        val outerRect = RectF(outerInset, outerInset, sizePx - outerInset, sizePx - outerInset)

        val innerInset = outerInset + strokeOuter + gap
        val innerRect = RectF(innerInset, innerInset, sizePx - innerInset, sizePx - innerInset)

        val outerTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeOuter
            strokeCap = Paint.Cap.ROUND
            color = palette.outerRingColor.copy(alpha = 0.24f).toArgb()
        }
        val outerProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeOuter
            strokeCap = Paint.Cap.ROUND
            color = palette.outerRingColor.toArgb()
        }

        val innerTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeInner
            strokeCap = Paint.Cap.ROUND
            color = palette.innerRingColor.copy(alpha = 0.24f).toArgb()
        }
        val innerProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeInner
            strokeCap = Paint.Cap.ROUND
            color = palette.innerRingColor.toArgb()
        }

        // Trilhos completos (360°)
        canvas.drawArc(outerRect, 0f, 360f, false, outerTrackPaint)
        canvas.drawArc(innerRect, 0f, 360f, false, innerTrackPaint)

        // Arcos de progresso uniformes iniciando às 12h (-90°)
        val clampedOuter = outerProgress.coerceIn(0f, 1f)
        if (clampedOuter > 0.005f) {
            val sweepOuter = (clampedOuter * 360f).coerceAtLeast(6f)
            canvas.drawArc(outerRect, -90f, sweepOuter, false, outerProgressPaint)
        }

        val clampedInner = innerProgress.coerceIn(0f, 1f)
        if (clampedInner > 0.005f) {
            val sweepInner = (clampedInner * 360f).coerceAtLeast(6f)
            canvas.drawArc(innerRect, -90f, sweepInner, false, innerProgressPaint)
        }

        // Porcentagem centralizada no núcleo dos anéis (ex.: "83%")
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary.toArgb()
            textSize = sizePx * 0.19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerY = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("${centerPercentage.coerceIn(0, 999)}%", sizePx / 2f, centerY, textPaint)

        return bitmap
    }

    /**
     * Renderiza uma barra horizontal de progresso com cantos arredondados na cor do tema ativo.
     */
    fun renderProgressBarBitmap(
        progress: Float,
        palette: WidgetThemePalette,
        widthPx: Int = 320,
        heightPx: Int = 14
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = heightPx / 2f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(45, 255, 255, 255)
        }
        canvas.drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), radius, radius, trackPaint)

        val fillW = (widthPx * progress.coerceIn(0.04f, 1f)).coerceAtLeast(heightPx.toFloat())
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = palette.outerRingColor.toArgb()
        }
        canvas.drawRoundRect(RectF(0f, 0f, fillW, heightPx.toFloat()), radius, radius, fillPaint)
        return bitmap
    }
}
