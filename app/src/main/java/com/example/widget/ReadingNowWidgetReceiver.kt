package com.example.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * GlanceAppWidgetReceiver que registra o Widget "Lendo Agora" no sistema Android.
 */
class ReadingNowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReadingNowWidget()
}
