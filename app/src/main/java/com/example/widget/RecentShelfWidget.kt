package com.example.widget

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.data.SafeStorage
import com.example.model.ComicEntry
import com.example.model.ReadingStatus

private data class RecentShelfSnapshot(
    val items: List<Pair<ComicEntry, Bitmap>>,
    val isQueueMode: Boolean,
    val totalShelfCount: Int,
    val palette: WidgetThemePalette
)

/**
 * C. Widget "Recentes da Estante / Fila" (Tamanho 4x1 ou 4x2) construído com Jetpack Glance:
 * - Exibe um carrossel/grade com as últimas 3 ou 4 capas adicionadas à estante ou no status Quero Ler (Fila).
 * - Tocar em uma capa específica abre o modal de detalhes e sinopse daquela edição no Quadrinheiro.
 */
class RecentShelfWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appCtx = context.applicationContext
        val storage = SafeStorage(appCtx)
        provideContent {
            val prefs = currentState<Preferences>()
            val prefRevision = prefs[QuadrinheiroWidgetsSync.PREF_WIDGET_REVISION] ?: 0L
            val memRevision by QuadrinheiroWidgetsSync.widgetRevisionFlow.collectAsState()
            // A new revision cancels old work and never exposes another account's snapshot.
            androidx.compose.runtime.key(prefRevision, memRevision) {
                val snapshot by androidx.compose.runtime.produceState<RecentShelfSnapshot?>(null) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val stored = storage.loadWidgetState()
                        val palette = WidgetVisualRenderer.resolvePaletteForThemeId(stored.settings.themeId)

                        val queueComics = stored.comics.filter { it.status == ReadingStatus.WANT_TO_READ }
                        val featuredList = (if (queueComics.size >= 2) queueComics else stored.comics).take(4)
                        val isQueueMode = queueComics.size >= 2
                        val itemsWithBitmaps = kotlinx.coroutines.coroutineScope { featuredList.map { comic -> async {
                            comic to WidgetVisualRenderer.loadOrRenderCoverBitmap(
                                context = appCtx,
                                comic = comic,
                                palette = palette,
                                widthPx = 144,
                                heightPx = 210
                            )
                        } }.awaitAll() }

                        RecentShelfSnapshot(
                            items = itemsWithBitmaps,
                            isQueueMode = isQueueMode,
                            totalShelfCount = stored.comics.size,
                            palette = palette
                        )
                    }
                }
                snapshot?.let { snapshot ->
                    GlanceTheme {
                        RecentShelfWidgetContent(
                            context = appCtx,
                            items = snapshot.items,
                            isQueueMode = snapshot.isQueueMode,
                            totalShelfCount = snapshot.totalShelfCount,
                            palette = snapshot.palette
                        )
                    }
                } ?: Box(GlanceModifier.fillMaxSize().background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF14151B)))) {}
            }
        }
    }
}

@Composable
private fun RecentShelfWidgetContent(
    context: Context,
    items: List<Pair<ComicEntry, Bitmap>>,
    isQueueMode: Boolean,
    totalShelfCount: Int,
    palette: WidgetThemePalette
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(palette.bgCard))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isQueueMode) "FILA DE LEITURA • QUERO LER" else "RECENTES DA ESTANTE",
                style = TextStyle(
                    color = ColorProvider(palette.outerRingColor),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "$totalShelfCount no acervo",
                style = TextStyle(
                    color = ColorProvider(palette.textSecondary),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (items.isEmpty()) {
            val openAppIntent = QuadrinheiroWidgetIntents.buildOpenReadingComicIntent(context, "")
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(openAppIntent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Adicione quadrinhos à sua Estante ou Fila para vê-los aqui",
                    style = TextStyle(
                        color = ColorProvider(palette.textSecondary),
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { idx, (comic, bmp) ->
                    if (idx > 0) {
                        Spacer(modifier = GlanceModifier.width(8.dp))
                    }
                    val openSynopsisIntent = QuadrinheiroWidgetIntents.buildOpenComicSynopsisIntent(
                        context = context,
                        comicId = comic.id
                    )
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(actionStartActivity(openSynopsisIntent)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            provider = ImageProvider(bmp),
                            contentDescription = comic.displayTitle,
                            contentScale = ContentScale.Fit,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight()
                                .cornerRadius(12.dp)
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = comic.displayTitle,
                            style = TextStyle(
                                color = ColorProvider(palette.textPrimary),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
