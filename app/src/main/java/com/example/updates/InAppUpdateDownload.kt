package com.example.updates

import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

internal const val UPDATE_CERTIFICATE = "ec5ec8852a218db2171959717d72771abac8535f848506da66cb7180874a761f"
internal const val UPDATE_DOWNLOAD_PREFIX = "https://github.com/felipemloiola/Quadrinheiro/releases/download/"
internal const val APK_MIME_TYPE = "application/vnd.android.package-archive"
internal const val MAX_UPDATE_BYTES = 250L * 1024 * 1024

internal data class AvailableUpdate(val code: Long, val name: String, val url: String, val sha256: String)
internal data class UpdateDownload(val id: Long, val update: AvailableUpdate)
internal data class DownloadProgress(val status: Int, val bytes: Long, val total: Long)

internal object UpdateVerifier {
    fun validateIdentity(packageName: String?, versionCode: Long, signers: List<String>, updateCode: Long, installedCode: Long) {
        check(packageName == BuildConfig.APPLICATION_ID) { "O arquivo não pertence ao Quadrinheiro." }
        check(versionCode == updateCode && versionCode > installedCode) { "A versão do arquivo não corresponde à atualização." }
        check(signers.size == 1 && signers.single() == UPDATE_CERTIFICATE) { "A assinatura da atualização não é a assinatura permanente do aplicativo." }
    }

    fun copyVerified(input: InputStream, destination: File, expectedHash: String, maxBytes: Long = MAX_UPDATE_BYTES) {
        require(expectedHash.matches(Regex("[a-f0-9]{64}")))
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            destination.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= maxBytes) { "O arquivo de atualização excede o tamanho permitido." }
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
                check(total > 0L) { "O download está vazio." }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(actual == expectedHash) { "O download está incompleto ou foi alterado. Baixe novamente." }
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }

    @Suppress("DEPRECATION")
    fun verifyArchive(context: Context, file: File, update: AvailableUpdate) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: error("O Android não reconheceu o arquivo da atualização.")
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val digests = signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        validateIdentity(info.packageName, code, digests, update.code, BuildConfig.VERSION_CODE.toLong())
    }
}

/** The system download service survives Activity/process recreation; only a verified private APK is shared. */
internal object InAppUpdateDownload {
    private fun preferences(context: Context) = context.getSharedPreferences("app_update_download", Context.MODE_PRIVATE)
    private fun manager(context: Context) = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    fun privateApk(context: Context, update: AvailableUpdate) = File(File(context.cacheDir, "updates"), "quadrinheiro-${update.code}.apk")

    fun load(context: Context): UpdateDownload? {
        val raw = preferences(context).getString("active", null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val update = AvailableUpdate(json.getLong("code"), json.getString("name"), json.getString("url"), json.getString("sha256"))
            require(update.url.startsWith(UPDATE_DOWNLOAD_PREFIX) && update.url.endsWith("/quadrinheiro.apk"))
            require(update.sha256.matches(Regex("[a-f0-9]{64}")))
            UpdateDownload(json.getLong("id"), update)
        }.getOrNull()
    }

    suspend fun start(context: Context, update: AvailableUpdate): UpdateDownload = withContext(Dispatchers.IO) {
        require(update.url.startsWith(UPDATE_DOWNLOAD_PREFIX) && update.url.endsWith("/quadrinheiro.apk"))
        require(update.sha256.matches(Regex("[a-f0-9]{64}")))
        load(context)?.let { cancel(context, it) }
        val relativePath = "updates/quadrinheiro-${update.code}.apk"
        val downloadDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: error("Não há espaço disponível para o download.")
        val target = File(downloadDirectory, relativePath)
        target.parentFile?.mkdirs()
        if (target.exists()) check(target.delete()) { "Não foi possível substituir o download anterior." }
        val request = DownloadManager.Request(Uri.parse(update.url))
            .setTitle("Quadrinheiro ${update.name}")
            .setDescription("Baixando atualização do aplicativo")
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, relativePath)
        val id = manager(context).enqueue(request)
        val json = JSONObject().put("id", id).put("code", update.code).put("name", update.name)
            .put("url", update.url).put("sha256", update.sha256)
        if (!preferences(context).edit().putString("active", json.toString()).commit()) {
            manager(context).remove(id)
            error("Não foi possível guardar o progresso do download.")
        }
        UpdateDownload(id, update)
    }

    suspend fun progress(context: Context, download: UpdateDownload): DownloadProgress = withContext(Dispatchers.IO) {
        manager(context).query(DownloadManager.Query().setFilterById(download.id)).use { cursor ->
            check(cursor != null && cursor.moveToFirst()) { "O download não está mais disponível. Tente novamente." }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            check(status != DownloadManager.STATUS_FAILED) { "Não foi possível baixar a atualização. Verifique sua conexão e o espaço disponível." }
            val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            if (bytes > MAX_UPDATE_BYTES || total > MAX_UPDATE_BYTES) {
                manager(context).remove(download.id)
                error("O arquivo de atualização excede o tamanho permitido.")
            }
            DownloadProgress(status, bytes, total)
        }
    }

    suspend fun prepare(context: Context, download: UpdateDownload): File = withContext(Dispatchers.IO) {
        val destination = privateApk(context, download.update)
        destination.parentFile?.mkdirs()
        try {
            manager(context).openDownloadedFile(download.id).use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                    UpdateVerifier.copyVerified(input, destination, download.update.sha256)
                }
            }
            UpdateVerifier.verifyArchive(context, destination, download.update)
            destination
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }

    fun cancel(context: Context, download: UpdateDownload) {
        manager(context).remove(download.id)
        privateApk(context, download.update).delete()
        preferences(context).edit().remove("active").apply()
    }

    fun canInstall(context: Context): Boolean = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun installIntent(context: Context, file: File): Intent {
        check(file.isFile) { "O arquivo da atualização não está mais disponível. Baixe novamente." }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri("Atualização do Quadrinheiro", uri) }
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
    }
}
