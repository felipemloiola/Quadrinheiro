package com.example.widget

import androidx.glance.layout.Box
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import com.example.ui.screens.computeReadingStatistics
import kotlin.math.roundToInt

private data class ActivityRingsSnapshot(
    val ownerUid: String?,
    val ringsBitmap: Bitmap,
    val readComicsCount: Int,
    val yearlyGoal: Int,
    val dailyPagesPace: Int,
    val dailyPagesGoal: Int,
    val palette: WidgetThemePalette
)

/**
 * B. Widget "Anéis de Metas de Leitura" (Tamanho 2x2 - Estilo Apple Activity) construído com Jetpack Glance:
 * - Anel externo: Meta anual de quadrinhos concluídos (ex.: 20/24 lidos).
 * - Anel interno: Meta de páginas do dia.
 * - No centro dos anéis, a porcentagem de conclusão (ex.: 83%).
 * - Ação: Tocar no widget abre o aplicativo diretamente no painel de Estatísticas / Metas.
 */
class ActivityRingsWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appCtx = context.applicationContext
        val storage = SafeStorage(appCtx)
        provideContent {
            val prefs = currentState<Preferences>()
            val prefRevision = prefs[QuadrinheiroWidgetsSync.PREF_WIDGET_REVISION] ?: 0L
            val memRevision by QuadrinheiroWidgetsSync.widgetRevisionFlow.collectAsState()
            // A new revision cancels old work and never exposes another account's snapshot.
            val owner = currentWidgetOwner(storage)
            val night = isSystemNight(appCtx)
            run {
                val snapshot by androidx.compose.runtime.produceState<ActivityRingsSnapshot?>(null, prefRevision, memRevision, owner, night) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val sessionSnapshot = storage.loadWidgetSessionSnapshot()
                        val stored = sessionSnapshot.state
                        val palette = WidgetVisualRenderer.resolvePaletteForThemeId(stored.settings.themeId, night)

                        val stats = computeReadingStatistics(
                            comics = stored.comics,
                            profile = stored.profile,
                            accentColor = palette.outerRingColor,
                            secondaryColor = palette.innerRingColor
                        )

                        val outerRatio = (stats.readComicsCount.toFloat() / stats.yearlyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        val innerRatio = (stats.dailyPagesPace.toFloat() / stats.dailyPagesGoal.coerceAtLeast(5).toFloat()).coerceIn(0f, 1f)
                        val centerPct = (outerRatio * 100f).roundToInt().coerceIn(0, 999)

                        val ringsBitmap = WidgetVisualRenderer.renderActivityRingsBitmap(
                            outerProgress = outerRatio,
                            innerProgress = innerRatio,
                            centerPercentage = centerPct,
                            palette = palette,
                            sizePx = 210
                        )

                        ActivityRingsSnapshot(
                            ownerUid = sessionSnapshot.ownerUid,
                            ringsBitmap = ringsBitmap,
                            readComicsCount = stats.readComicsCount,
                            yearlyGoal = stats.yearlyGoal,
                            dailyPagesPace = stats.dailyPagesPace,
                            dailyPagesGoal = stats.dailyPagesGoal,
                            palette = palette
                        )
                    }
                }
                snapshot?.takeIf { it.ownerUid == owner }?.let { snapshot ->
                    GlanceTheme {
                        ActivityRingsWidgetContent(
                            context = appCtx,
                            ringsBitmap = snapshot.ringsBitmap,
                            readComicsCount = snapshot.readComicsCount,
                            yearlyGoal = snapshot.yearlyGoal,
                            dailyPagesPace = snapshot.dailyPagesPace,
                            dailyPagesGoal = snapshot.dailyPagesGoal,
                            palette = snapshot.palette
                        )
                    }
                } ?: WidgetLoadingContent()
            }
        }
    }
}

@Composable
private fun ActivityRingsWidgetContent(
    context: Context,
    ringsBitmap: Bitmap,
    readComicsCount: Int,
    yearlyGoal: Int,
    dailyPagesPace: Int,
    dailyPagesGoal: Int,
    palette: WidgetThemePalette
) {
    val openStatsIntent = QuadrinheiroWidgetIntents.buildOpenStatisticsIntent(context)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(com.example.R.color.widget_background)
            .clickable(actionStartActivity(openStatsIntent))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "METAS 2026",
                style = TextStyle(
                    color = ColorProvider(palette.outerRingColor),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Image(
            provider = ImageProvider(ringsBitmap),
            contentDescription = "Anéis de Metas de Leitura",
            modifier = GlanceModifier.size(78.dp)
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$readComicsCount/$yearlyGoal",
                    style = TextStyle(
                        color = ColorProvider(palette.outerRingColor),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "lidos no ano",
                    style = TextStyle(
                        color = ColorProvider(com.example.R.color.widget_text_secondary),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.height(1.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$dailyPagesPace/$dailyPagesGoal",
                    style = TextStyle(
                        color = ColorProvider(palette.innerRingColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "págs/dia",
                    style = TextStyle(
                        color = ColorProvider(com.example.R.color.widget_text_secondary),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
