package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StorageMaintenance
import com.example.data.StorageUsage
import com.example.ui.components.contentMaterialSurface
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun StorageSettingsCard() {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf<StorageUsage?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(context) { usage = withContext(Dispatchers.IO) { StorageMaintenance.usage(context) } }
    Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Neste aparelho", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Cache: ${usage?.let { StorageMaintenance.formatBytes(it.cacheBytes) } ?: "Calculando…"}", color = theme.textSecondary, fontSize = 13.sp)
        Text("Dados locais: ${usage?.let { StorageMaintenance.formatBytes(it.localBytes) } ?: "Calculando…"}", color = theme.textSecondary, fontSize = 13.sp)
        Text(if (error) "Não foi possível concluir a limpeza. Tente novamente." else "A coleção, suas contas e os backups são preservados.", color = theme.textSecondary, fontSize = 12.sp)
        Text(if (busy) "Limpando…" else "Limpar cache", color = theme.accent, fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("clear_recoverable_cache")
                .clickable(enabled = !busy, role = Role.Button) {
                    scope.launch {
                        busy = true; error = false
                        try { usage = withContext(Dispatchers.IO) { StorageMaintenance.clearRecoverable(context); StorageMaintenance.usage(context) } }
                        catch (e: Exception) { error = true }
                        finally { busy = false }
                    }
                }.padding(vertical = 12.dp))
    }
}
