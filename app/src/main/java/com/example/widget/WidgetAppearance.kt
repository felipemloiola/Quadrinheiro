package com.example.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.data.SafeStorage

internal fun isSystemNight(context: Context) = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
internal fun currentWidgetOwner(storage: SafeStorage): String? = storage.loadAuthSession()?.uid ?: if (storage.isGuestMode()) "guest_local" else null

@Composable
internal fun WidgetLoadingContent() {
    Box(GlanceModifier.fillMaxSize().background(R.color.widget_background).padding(20.dp), contentAlignment = Alignment.Center) {
        Text("Atualizando sua leitura…", style = TextStyle(color = ColorProvider(R.color.widget_text_secondary), fontSize = 13.sp))
    }
}
