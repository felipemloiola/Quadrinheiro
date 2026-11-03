package com.example.reading

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.ComicEntry
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.launch

@Composable
fun ReadingNowBarControl(comic: ComicEntry, onSelect: (String) -> Unit) {
    if (com.example.ui.components.LocalHomeSkeleton.current != null) {
        com.example.ui.components.HomeSkeletonBlock(Modifier.width(160.dp).height(48.dp))
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val active by ReadingLiveActivity.active.collectAsState()
    val theme = LocalIOS27Theme.current
    val begin = {
        onSelect(comic.id)
        scope.launch {
            try { ReadingLiveActivity.start(context, comic.id) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { Toast.makeText(context, "Não foi possível iniciar a sessão. Tente novamente.", Toast.LENGTH_LONG).show() }
        }
        Unit
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) begin() else Toast.makeText(context, "Permita notificações para acompanhar a leitura na tela bloqueada.", Toast.LENGTH_LONG).show()
    }
    TextButton(onClick = {
        if (active || ReadingLiveActivity.isActive(context)) ReadingLiveActivity.stop(context)
        else if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else begin()
    }, modifier = Modifier.heightIn(min = 48.dp)) {
        Icon(Icons.Default.NotificationsActive, null, tint = theme.accent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (active) "Encerrar sessão" else "Iniciar Now Bar", color = theme.accent)
    }
}
