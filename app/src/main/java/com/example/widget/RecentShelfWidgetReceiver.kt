package com.example.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * GlanceAppWidgetReceiver que registra o Widget "Recentes da Estante / Fila" (4x1 ou 4x2) no sistema Android.
 */
class RecentShelfWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentShelfWidget()
}
