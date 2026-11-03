package com.example.reading

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppleToggle
import com.example.ui.theme.LocalIOS27Theme

/** Lives only in Library and Display preferences, never in the Home content. */
@Composable
fun ReadingNowBarPreference(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current
    val currentOnChange by rememberUpdatedState(onChange)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) currentOnChange(true)
        else Toast.makeText(context, "Permita notificações para acompanhar a leitura na tela bloqueada.", Toast.LENGTH_LONG).show()
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Leitura na Now Bar", color = theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Capa e progresso da HQ atual na barra de status e na tela bloqueada", color = theme.textSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
            AppleToggle(checked = enabled, modifier = Modifier.semantics { contentDescription = "Leitura na Now Bar" }, onChange = { next ->
                if (!next) { ReadingLiveActivity.stop(context); currentOnChange(false) }
                else if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else currentOnChange(true)
            })
        }
        if (enabled) {
            Text("Permissões da tela de bloqueio e Now Bar ›", color = theme.accent, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { openReadingNotificationSettings(context) }.padding(horizontal = 16.dp, vertical = 14.dp))
        }
    }
}

internal fun readingNotificationSettingsIntent(context: Context): Intent {
    val action = if (Build.VERSION.SDK_INT >= 36 && !context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications())
        Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS else Settings.ACTION_APP_NOTIFICATION_SETTINGS
    return Intent(action).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun openReadingNotificationSettings(context: Context) {
    try { context.startActivity(readingNotificationSettingsIntent(context)) }
    catch (_: Exception) {
        runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
