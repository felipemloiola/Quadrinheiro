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
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.ui.components.BookCover
import com.example.ui.components.contentMaterialSurface
import com.example.model.ComicEntry
import com.example.ui.theme.LocalIOS27Theme

/** Lives only in Library and Display preferences, never in the Home content. */
@Composable
fun ReadingNowBarPreference(enabled: Boolean, comic: ComicEntry? = null, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current
    val currentOnChange by rememberUpdatedState(onChange)
    val currentEnabled by rememberUpdatedState(enabled)
    var notificationsAllowed by remember { mutableStateOf(canShowReadingNotifications(context)) }
    var promotionAllowed by remember { mutableStateOf(canPromoteReadingNotifications(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = canShowReadingNotifications(context)
                promotionAllowed = canPromoteReadingNotifications(context)
                if (currentEnabled) ReadingLiveActivity.refreshAsync(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = canShowReadingNotifications(context)
        if (granted) { currentOnChange(true); ReadingLiveActivity.refreshAsync(context) }
        else Toast.makeText(context, "Permita notificações para acompanhar a leitura na tela bloqueada.", Toast.LENGTH_LONG).show()
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Live Updates de leitura", color = theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Sua leitura ao vivo na tela bloqueada e, em aparelhos compatíveis, na barra de status e Now Bar", color = theme.textSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
            AppleToggle(checked = enabled, modifier = Modifier.semantics { contentDescription = "Live Updates de leitura" }, onChange = { next ->
                if (!next) { ReadingLiveActivity.stop(context); currentOnChange(false) }
                else if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else currentOnChange(true)
            })
        }
        if (enabled || !notificationsAllowed) {
            if (enabled && comic != null) ReadingLiveUpdatePreview(comic)
            Text(when {
                !notificationsAllowed -> "Notificações bloqueadas. Toque abaixo para permitir."
                Build.VERSION.SDK_INT < 36 -> "Neste Android, a leitura aparece como uma notificação de progresso."
                !promotionAllowed -> "Permita Live Updates para solicitar destaque na barra de status."
                else -> "Live Updates permitidos. O destaque e a Now Bar dependem do aparelho."
            }, color = theme.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Text("Permissões dos Live Updates e da tela bloqueada ›", color = theme.accent, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { openReadingNotificationSettings(context) }.padding(horizontal = 16.dp, vertical = 14.dp))
        }
    }
}

internal fun readingNotificationSettingsIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT < 26) return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val action = if (canShowReadingNotifications(context) && Build.VERSION.SDK_INT >= 36 && !canPromoteReadingNotifications(context))
        Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS else Settings.ACTION_APP_NOTIFICATION_SETTINGS
    return Intent(action).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun canPromoteReadingNotifications(context: Context): Boolean = Build.VERSION.SDK_INT >= 36 &&
    context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications()

/** Glanceable, content-first preview; the actual system notification uses the native template. */
@Composable
private fun ReadingLiveUpdatePreview(comic: ComicEntry) {
    val theme = LocalIOS27Theme.current
    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth()
        .contentMaterialSurface(RoundedCornerShape(24.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        BookCover(comic.coverUrl, comic.displayTitle, spine3d = false, modifier = Modifier.size(44.dp, 66.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Lendo agora", color = theme.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(comic.displayTitle, color = theme.textPrimary, fontSize = 14.sp,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(readingProgressText(comic), color = theme.textSecondary, fontSize = 12.sp)
            if (comic.pageCount > 0) LinearProgressIndicator(
                progress = { (comic.currentPage.toFloat() / comic.pageCount).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(), color = theme.accent)
        }
    }
}

private fun openReadingNotificationSettings(context: Context) {
    try { context.startActivity(readingNotificationSettingsIntent(context)) }
    catch (_: Exception) {
        runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
