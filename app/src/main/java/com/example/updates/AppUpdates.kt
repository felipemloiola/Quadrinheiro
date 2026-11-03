package com.example.updates

import android.app.Activity
import android.app.DownloadManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.unit.dp
import java.io.File
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.BuildConfig
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val REPOSITORY = "felipemloiola/Quadrinheiro"
private const val CERTIFICATE = UPDATE_CERTIFICATE
private const val DOWNLOAD_PREFIX = "https://github.com/felipemloiola/Quadrinheiro/releases/download/"
private object UpdateSignals {
    val requests = MutableStateFlow(0L)
    var busy by mutableStateOf(false)
    var downloading by mutableStateOf(false)
}
private object ReleaseClient {
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private fun json(url: String): JSONObject? {
        val request = Request.Builder().url(url).header("User-Agent", "Quadrinheiro-Android")
            .header("Accept", "application/json").build()
        return http.newCall(request).execute().use { response ->
            if (response.code == 404) return@use null
            check(response.isSuccessful) { "Update server unavailable" }
            val body = response.body ?: error("Empty update response")
            check(body.contentLength() <= 1024 * 1024) { "Update response too large" }
            val text = body.string()
            check(text.length <= 1024 * 1024) { "Update response too large" }
            JSONObject(text)
        }
    }
    suspend fun latest(): AvailableUpdate? = withContext(Dispatchers.IO) {
        val release = json("https://api.github.com/repos/$REPOSITORY/releases/latest") ?: return@withContext null
        if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@withContext null
        val assets = release.getJSONArray("assets")
        val manifests = (0 until assets.length()).map { assets.getJSONObject(it) }
            .filter { it.optString("name") == "update.json" }
        if (manifests.size != 1) return@withContext null
        val manifestUrl = manifests.single().getString("browser_download_url")
        check(manifestUrl.startsWith(DOWNLOAD_PREFIX) && manifestUrl.endsWith("/update.json"))
        val metadata = json(manifestUrl) ?: return@withContext null
        check(metadata.getString("applicationId") == BuildConfig.APPLICATION_ID)
        check(metadata.getString("certificateSha256") == CERTIFICATE)
        if (metadata.getInt("minSdk") > Build.VERSION.SDK_INT) return@withContext null
        val code = metadata.getLong("versionCode")
        if (code <= BuildConfig.VERSION_CODE.toLong()) return@withContext null
        val apkUrl = metadata.getString("apkUrl")
        val tag = release.getString("tag_name")
        check(apkUrl == DOWNLOAD_PREFIX + tag + "/quadrinheiro.apk")
        check(metadata.getString("sha256").matches(Regex("[a-f0-9]{64}")))
        AvailableUpdate(code, metadata.getString("versionName").take(64), apkUrl, metadata.getString("sha256"))
    }
}

@Composable
fun AppUpdatePrompt() {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current
    val request by UpdateSignals.requests.collectAsState()
    var available by remember { mutableStateOf<AvailableUpdate?>(null) }
    var transfer by remember { mutableStateOf(InAppUpdateDownload.load(context)) }
    var showTransfer by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf<DownloadProgress?>(null) }
    var readyFile by remember { mutableStateOf<File?>(null) }
    var transferError by remember { mutableStateOf<String?>(null) }
    var automaticInstallationAttempted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            transfer?.let { InAppUpdateDownload.cancel(context, it) }
            transfer = null
        }
    }
    val launchInstaller: (File) -> Unit = { file ->
        try { installer.launch(InAppUpdateDownload.installIntent(context, file)) }
        catch (_: Exception) { transferError = "Não foi possível abrir a instalação do Android. Tente novamente." }
    }
    val installationPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (InAppUpdateDownload.canInstall(context)) readyFile?.let(launchInstaller)
        else transferError = "Autorize o Quadrinheiro a instalar atualizações para continuar."
    }
    val install: () -> Unit = {
        val file = readyFile
        if (file != null) {
            automaticInstallationAttempted = true
            transferError = null
            if (InAppUpdateDownload.canInstall(context)) launchInstaller(file)
            else try {
                installationPermission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName)))
            } catch (_: Exception) { transferError = "Não foi possível abrir a permissão de instalação do Android." }
        }
    }
    LaunchedEffect(transfer?.id) {
        val active = transfer ?: return@LaunchedEffect
        readyFile = null
        progress = null
        transferError = null
        automaticInstallationAttempted = false
        if (active.update.code <= BuildConfig.VERSION_CODE.toLong()) {
            InAppUpdateDownload.cancel(context, active)
            transfer = null
            return@LaunchedEffect
        }
        UpdateSignals.downloading = true
        try {
            while (true) {
                val current = InAppUpdateDownload.progress(context, active)
                progress = current
                if (current.status == DownloadManager.STATUS_SUCCESSFUL) break
                delay(500)
            }
            readyFile = InAppUpdateDownload.prepare(context, active)
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { transferError = error.message ?: "Não foi possível baixar a atualização. Tente novamente." }
        finally { UpdateSignals.downloading = false }
    }
    val lifecycleState by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(readyFile, showTransfer, lifecycleState) {
        if (readyFile != null && showTransfer && !automaticInstallationAttempted &&
            lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) install()
    }
    val prefs = remember(context) { context.getSharedPreferences("app_updates", 0) }
    LaunchedEffect(request) {
        val manual = request > 0L
        if (transfer != null) { if (manual) showTransfer = true; return@LaunchedEffect }
        if (BuildConfig.DEBUG) {
            if (manual) Toast.makeText(context, "Atualizações disponíveis no APK de release.", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }
        val now = System.currentTimeMillis()
        if (!manual && now - prefs.getLong("last_check", 0L) in 0L until 6L * 60 * 60 * 1000) return@LaunchedEffect
        if (!manual) delay(2500)
        UpdateSignals.busy = true
        try {
            prefs.edit().putLong("last_check", now).apply()
            val update = ReleaseClient.latest()
            val snoozed = update != null && prefs.getLong("snoozed_code", -1) == update.code &&
                now - prefs.getLong("snoozed_at", 0L) in 0L until 24L * 60 * 60 * 1000
            if (update != null && (manual || !snoozed)) available = update
            else if (manual) Toast.makeText(context, "Você já está na versão mais recente compatível.", Toast.LENGTH_LONG).show()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            if (manual) Toast.makeText(context, "Não foi possível verificar atualizações. Tente novamente com internet.", Toast.LENGTH_LONG).show()
        } finally {
            UpdateSignals.busy = false
        }
    }
    available?.let { update ->
        val postpone = {
            prefs.edit().putLong("snoozed_code", update.code).putLong("snoozed_at", System.currentTimeMillis()).apply()
            available = null
        }
        AppUpdateDialog(
            onDismissRequest = postpone,
            containerColor = theme.bgPrimary,
            title = { Text("Atualização disponível", color = theme.textPrimary) },
            text = { Text("Quadrinheiro ${update.name}\n\nA atualização será baixada aqui no aplicativo. Ao terminar, confirme a instalação na tela do Android. Seus dados serão mantidos.", color = theme.textSecondary) },
            confirmButton = {
                UpdateDialogAction(onClick = {
                    scope.launch {
                        UpdateSignals.downloading = true
                        try {
                            transfer = InAppUpdateDownload.start(context, update)
                            showTransfer = true
                            available = null
                        } catch (_: Exception) {
                            Toast.makeText(context, "Não foi possível iniciar o download. Verifique sua conexão e o espaço disponível.", Toast.LENGTH_LONG).show()
                        } finally { UpdateSignals.downloading = false }
                    }
                }, enabled = !UpdateSignals.downloading) { Text("Atualizar") }
            },
            dismissButton = { UpdateDialogAction(onClick = postpone) { Text("Depois", color = theme.textSecondary) } }
        )
    }
    transfer?.takeIf { showTransfer }?.let { download ->
        val ready = readyFile != null
        val downloading = !ready && transferError == null
        val total = progress?.total ?: -1L
        val fraction = if (total > 0L) ((progress?.bytes ?: 0L).toFloat() / total).coerceIn(0f, 1f) else null
        AppUpdateDialog(
            onDismissRequest = { if (!downloading) showTransfer = false },
            containerColor = theme.bgPrimary,
            title = { Text("Atualização ${download.update.name}", color = theme.textPrimary) },
            text = {
                Column {
                    Text(when {
                        transferError != null -> transferError!!
                        ready -> "A atualização está pronta. Confirme a instalação no Android."
                        progress?.status == DownloadManager.STATUS_SUCCESSFUL -> "Conferindo a atualização…"
                        progress?.status == DownloadManager.STATUS_PAUSED -> "Download pausado. Aguardando conexão…"
                        fraction != null -> "Baixando atualização: " + (fraction * 100).toInt() + "%"
                        else -> "Baixando atualização…"
                    }, color = theme.textSecondary)
                    if (downloading) {
                        Spacer(Modifier.height(12.dp))
                        if (fraction != null) LinearProgressIndicator(progress = { fraction }, color = theme.accent)
                        else LinearProgressIndicator(color = theme.accent)
                    }
                }
            },
            confirmButton = {
                if (ready) UpdateDialogAction(onClick = install) { Text("Instalar") }
                else if (transferError != null) UpdateDialogAction(onClick = {
                    scope.launch {
                        try { transfer = InAppUpdateDownload.start(context, download.update) }
                        catch (_: Exception) { transferError = "Não foi possível reiniciar o download." }
                    }
                }) { Text("Tentar novamente") }
            },
            dismissButton = {
                UpdateDialogAction(onClick = {
                    if (ready) showTransfer = false
                    else {
                        InAppUpdateDownload.cancel(context, download)
                        transfer = null
                    }
                }) { Text(if (ready) "Depois" else "Cancelar download", color = theme.textSecondary) }
            }
        )
    }

}

@Composable
fun CheckForUpdatesButton() {
    val theme = LocalIOS27Theme.current
    Text(
        text = if (UpdateSignals.downloading) "Baixando atualização…" else if (UpdateSignals.busy) "Verificando atualizações…" else "Versão ${BuildConfig.VERSION_NAME} · Verificar atualizações",
        color = theme.textSecondary,
        modifier = Modifier.clickable(enabled = !UpdateSignals.busy && !UpdateSignals.downloading) {
            UpdateSignals.requests.value = UpdateSignals.requests.value + 1
        }
    )
}
