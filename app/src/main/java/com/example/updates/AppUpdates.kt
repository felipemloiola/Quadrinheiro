package com.example.updates

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val REPOSITORY = "felipemloiola/Quadrinheiro"
private const val CERTIFICATE = "ec5ec8852a218db2171959717d72771abac8535f848506da66cb7180874a761f"
private const val DOWNLOAD_PREFIX = "https://github.com/felipemloiola/Quadrinheiro/releases/download/"
private object UpdateSignals {
    val requests = MutableStateFlow(0L)
    var busy by mutableStateOf(false)
}
private data class AvailableUpdate(val code: Long, val name: String, val url: String)
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
        AvailableUpdate(code, metadata.getString("versionName").take(64), apkUrl)
    }
}

@Composable
fun AppUpdatePrompt() {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current
    val request by UpdateSignals.requests.collectAsState()
    var available by remember { mutableStateOf<AvailableUpdate?>(null) }
    val prefs = remember(context) { context.getSharedPreferences("app_updates", 0) }
    LaunchedEffect(request) {
        val manual = request > 0L
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
        AlertDialog(
            onDismissRequest = postpone,
            containerColor = theme.bgPrimary,
            title = { Text("Atualização disponível", color = theme.textPrimary) },
            text = { Text("Quadrinheiro ${update.name}\n\nO APK será baixado pelo navegador. Depois, abra o arquivo e confirme a instalação no Android. Seus dados não precisam ser apagados.", color = theme.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.url)))
                        available = null
                    } catch (_: Exception) {
                        Toast.makeText(context, "Não foi possível abrir o navegador.", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Baixar atualização", color = theme.accent) }
            },
            dismissButton = { TextButton(onClick = postpone) { Text("Depois", color = theme.textSecondary) } }
        )
    }
}

@Composable
fun CheckForUpdatesButton() {
    val theme = LocalIOS27Theme.current
    Text(
        text = if (UpdateSignals.busy) "Verificando atualizações…" else "Versão ${BuildConfig.VERSION_NAME} · Verificar atualizações",
        color = theme.textSecondary,
        modifier = Modifier.clickable(enabled = !UpdateSignals.busy) {
            UpdateSignals.requests.value = UpdateSignals.requests.value + 1
        }
    )
}
