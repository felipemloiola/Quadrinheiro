package com.example.widget

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
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.size
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
import kotlin.math.roundToInt

private data class ReadingNowSnapshot(
    val ownerUid: String?,
    val comic: ComicEntry?,
    val coverBitmap: Bitmap?,
    val progressBarBitmap: Bitmap,
    val palette: WidgetThemePalette
)

/**
 * A. Widget "Lendo Agora" (Interativo - Tamanho 2x2 ou 4x2) construído com Jetpack Glance.
 * - Exibe a capa em alta resolução na proporção original de HQ (2:3), título, autor e progresso em tempo real.
 * - Dois botões nativos de stepper (- e +) integrados ao widget via UpdatePageAction:
 *   Ao tocar em + ou -, atualiza em tempo real tanto o widget na tela inicial quanto o SafeStorage e o Firestore.
 * - Ao tocar na capa ou no título, abre o app direto na tela de leitura daquela HQ.
 */
class ReadingNowWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appCtx = context.applicationContext
        val storage = SafeStorage(appCtx)
        provideContent {
            val prefs = currentState<Preferences>()
            val prefRevision = prefs[QuadrinheiroWidgetsSync.PREF_WIDGET_REVISION] ?: 0L
            val memRevision by QuadrinheiroWidgetsSync.widgetRevisionFlow.collectAsState()
            // A new revision cancels old work and never exposes another account's snapshot.
            androidx.compose.runtime.key(prefRevision, memRevision) {
                val snapshot by androidx.compose.runtime.produceState<ReadingNowSnapshot?>(null) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val sessionSnapshot = storage.loadWidgetSessionSnapshot()
                        val stored = sessionSnapshot.state
                        val palette = WidgetVisualRenderer.resolvePaletteForThemeId(stored.settings.themeId)
                        val currentComic = com.example.model.activeReadingComic(stored.comics, stored.profile.activeReadingComicId)

                        val coverBitmap = currentComic?.let {
                            WidgetVisualRenderer.loadOrRenderCoverBitmap(
                                context = appCtx,
                                comic = it,
                                palette = palette,
                                widthPx = 204,
                                heightPx = 300
                            )
                        }

                        val totalPages = (currentComic?.pageCount ?: 1).coerceAtLeast(1)
                        val curPage = (currentComic?.currentPage ?: 0).coerceIn(0, totalPages)
                        val progressFraction = (curPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
                        val progressBarBitmap = WidgetVisualRenderer.renderProgressBarBitmap(
                            progress = progressFraction,
                            palette = palette,
                            widthPx = 320,
                            heightPx = 12
                        )

                        ReadingNowSnapshot(
                            ownerUid = sessionSnapshot.ownerUid,
                            comic = currentComic,
                            coverBitmap = coverBitmap,
                            progressBarBitmap = progressBarBitmap,
                            palette = palette
                        )
                    }
                }
                snapshot?.let { snapshot ->
                    GlanceTheme {
                        ReadingNowWidgetContent(
                            ownerUid = snapshot.ownerUid,
                            context = appCtx,
                            comic = snapshot.comic,
                            coverBitmap = snapshot.coverBitmap,
                            progressBarBitmap = snapshot.progressBarBitmap,
                            palette = snapshot.palette
                        )
                    }
                } ?: Box(GlanceModifier.fillMaxSize().background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF14151B)))) {}
            }
        }
    }
}

@Composable
private fun ReadingNowWidgetContent(
    ownerUid: String?,
    context: Context,
    comic: ComicEntry?,
    coverBitmap: Bitmap?,
    progressBarBitmap: Bitmap,
    palette: WidgetThemePalette
) {
    val size = LocalSize.current
    val isCompact = size.width < 205.dp

    if (comic == null) {
        val openAppIntent = QuadrinheiroWidgetIntents.buildOpenReadingComicIntent(context, "")
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(ColorProvider(palette.bgCard))
                .clickable(actionStartActivity(openAppIntent))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LENDO AGORA",
                    style = TextStyle(
                        color = ColorProvider(palette.outerRingColor),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Toque para iniciar uma leitura no Quadrinheiro",
                    style = TextStyle(
                        color = ColorProvider(palette.textPrimary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2
                )
            }
        }
        return
    }

    val totalPages = comic.pageCount.coerceAtLeast(1)
    val curPage = comic.currentPage.coerceIn(0, totalPages)
    val pct = ((curPage.toFloat() / totalPages.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    val openComicIntent = QuadrinheiroWidgetIntents.buildOpenReadingComicIntent(context, comic.id)

    // Proporção fiel de capa de quadrinho (2:3 -> largura = 68% da altura), sem comprimir lateralmente
    val rawCoverHeight = if (size.height > 60.dp) {
        (size.height - 24.dp).coerceIn(98.dp, 152.dp)
    } else {
        128.dp
    }
    val coverHeight = if (isCompact) rawCoverHeight.coerceAtMost(112.dp) else rawCoverHeight
    val coverWidth = (coverHeight * 0.68f).coerceIn(if (isCompact) 66.dp else 86.dp, 104.dp)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(palette.bgCard))
            .padding(horizontal = if (isCompact) 14.dp else 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Capa em alta resolução com proporção real de HQ (2:3)
        if (coverBitmap != null) {
            Image(
                provider = ImageProvider(coverBitmap),
                contentDescription = comic.displayTitle,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier
                    .size(width = coverWidth, height = coverHeight)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(openComicIntent))
            )
            Spacer(modifier = GlanceModifier.width(if (isCompact) 10.dp else 14.dp))
        }

        // Coluna de metadados + progresso em tempo real + botões nativos (- e +)
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openComicIntent))
            ) {
                Text(
                    text = "LENDO AGORA",
                    style = TextStyle(
                        color = ColorProvider(palette.outerRingColor),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                Text(
                    text = comic.displayTitle,
                    style = TextStyle(
                        color = ColorProvider(palette.textPrimary),
                        fontSize = if (isCompact) 13.sp else 14.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                if (!isCompact) {
                    Text(
                        text = comic.authors.ifBlank { comic.publisher.ifBlank { "Quadrinheiro" } },
                        style = TextStyle(
                            color = ColorProvider(palette.textSecondary),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Progresso da página atual em relação ao total (ex.: Pág. 120 de 240 • 50%)
            Text(
                text = "Pág. $curPage de $totalPages • $pct%",
                style = TextStyle(
                    color = ColorProvider(palette.textSecondary),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(5.dp))

            Image(
                provider = ImageProvider(progressBarBitmap),
                contentDescription = "Progresso $pct%",
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(5.dp)
            )

            Spacer(modifier = GlanceModifier.height(7.dp))

            // Botões nativos de stepper (- e +) integrados ao widget
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão (-) subtrai 1 página em tempo real
                Box(
                    modifier = GlanceModifier
                        .size(width = if (isCompact) 38.dp else 44.dp, height = 30.dp)
                        .cornerRadius(15.dp)
                        .background(ColorProvider(palette.bgSurfaceElevated))
                        .clickable(
                            actionRunCallback<UpdatePageAction>(
                                actionParametersOf(
                                    UpdatePageAction.PARAM_COMIC_ID to comic.id,
                                    UpdatePageAction.PARAM_OWNER_UID to (ownerUid ?: "signed_out"),
                                    UpdatePageAction.PARAM_PAGE_DELTA to -1
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        style = TextStyle(
                            color = ColorProvider(palette.textPrimary),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Botão (+) soma 1 página em tempo real no widget, no SafeStorage e no Firestore
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(30.dp)
                        .cornerRadius(15.dp)
                        .background(ColorProvider(palette.outerRingColor))
                        .clickable(
                            actionRunCallback<UpdatePageAction>(
                                actionParametersOf(
                                    UpdatePageAction.PARAM_COMIC_ID to comic.id,
                                    UpdatePageAction.PARAM_OWNER_UID to (ownerUid ?: "signed_out"),
                                    UpdatePageAction.PARAM_PAGE_DELTA to 1
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCompact) "+" else "+ 1 Pág.",
                        style = TextStyle(
                            color = ColorProvider(palette.buttonTextOnAccent),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
