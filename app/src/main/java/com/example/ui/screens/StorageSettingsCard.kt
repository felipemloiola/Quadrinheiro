package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StorageMaintenance
import com.example.data.StorageUsage
import com.example.ui.components.contentMaterialSurface
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.readableForeground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun StorageSettingsCard() {
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val theme = LocalIOS27Theme.current
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf<StorageUsage?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var diagnosticCopied by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(context) {
        try {
            usage = withContext(Dispatchers.IO) {
                StorageMaintenance.autoCompactIfNeeded(context)
                StorageMaintenance.usage(context)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            error = "Não foi possível medir o armazenamento."
        }
    }
    val readableAccent = readableForeground(theme.accent, theme.bgSecondary, 4.5f)
    Column(
        Modifier
            .fillMaxWidth()
            .contentMaterialSurface(RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Neste aparelho",
            color = theme.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() }
        )
        Text("Cache: ${usage?.let { StorageMaintenance.formatBytes(it.cacheBytes) } ?: "Calculando…"}", color = theme.textSecondary, fontSize = 13.sp)
        Text("Dados locais: ${usage?.let { StorageMaintenance.formatBytes(it.localBytes) } ?: "Calculando…"}", color = theme.textSecondary, fontSize = 13.sp)
        if (usage != null) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        stateDescription = if (expanded) "Expandido" else "Recolhido"
                    }
                    .testTag("storage_details"),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(if (expanded) "Ocultar detalhes" else "Ver detalhes", color = readableAccent)
            }
            if (expanded) {
                usage?.categories?.forEach { (label, size) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, Modifier.weight(1f), color = theme.textSecondary, fontSize = 12.sp)
                        Text(StorageMaintenance.formatBytes(size), color = theme.textPrimary, fontSize = 12.sp)
                    }
                }
                if (!usage?.largestItems.isNullOrEmpty()) {
                    Text(
                        "Maiores itens locais",
                        color = theme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
                    )
                    usage?.largestItems.orEmpty().forEach { item ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(StorageMaintenance.formatBytes(item.bytes), color = theme.textPrimary, fontSize = 12.sp)
                            Text(item.path, color = theme.textSecondary, fontSize = 10.sp, maxLines = 2)
                        }
                    }
                    Text("A lista contém apenas caminhos e tamanhos; nenhum arquivo foi aberto ou enviado.", color = theme.textSecondary, fontSize = 11.sp)
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            val current = usage ?: return@launch
                            val report = withContext(Dispatchers.IO) {
                                StorageMaintenance.buildSanitizedDiagnosticReport(context, current)
                            }
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(report))
                            diagnosticCopied = true
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { if (diagnosticCopied) liveRegion = LiveRegionMode.Polite }
                        .testTag("copy_storage_diagnostic"),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        if (diagnosticCopied) "Diagnóstico copiado (sem dados pessoais)" else "Copiar diagnóstico técnico",
                        color = readableAccent,
                        fontSize = 13.sp
                    )
                }
                Text("Os valores podem mudar durante a sincronização. O aplicativo instalado não está incluído.", color = theme.textSecondary, fontSize = 12.sp)
            }
        }
        Text(
            error ?: "A coleção, suas contas e os backups são preservados.",
            color = theme.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.semantics { if (error != null) liveRegion = LiveRegionMode.Polite }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .focusable(enabled = !busy)
                .testTag("clear_recoverable_cache")
                .clickable(
                    enabled = !busy,
                    role = Role.Button,
                    onClickLabel = "Limpar cache recuperável"
                ) {
                    scope.launch {
                        busy = true; error = null
                        try {
                            usage = withContext(Dispatchers.IO) {
                                StorageMaintenance.clearRecoverable(context)
                                StorageMaintenance.usage(context)
                            }
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            error = "Não foi possível concluir a limpeza. Tente novamente."
                        } finally {
                            busy = false
                        }
                    }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                if (busy) "Limpando…" else "Limpar cache",
                color = readableAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
