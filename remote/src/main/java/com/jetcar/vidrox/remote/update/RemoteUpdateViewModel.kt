package com.jetcar.vidrox.remote.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val UPDATE_LOG_TAG = "VidroXRemoteUpdate"

private class InvalidRemoteUpdate(message: String) : IOException(message)

class RemoteUpdateViewModel : ViewModel() {
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()

    fun downloadApk(
        context: Context,
        release: RemoteRelease,
        onDownloaded: (File) -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = downloadFile(context.applicationContext, release)
                withContext(Dispatchers.Main) { onDownloaded(file) }
            } catch (error: Exception) {
                Log.w(UPDATE_LOG_TAG, "update download failed", error)
                val reason = (error as? InvalidRemoteUpdate)?.message ?: "ошибка сети"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось скачать обновление: $reason", Toast.LENGTH_LONG).show()
                    onError()
                }
            }
        }
    }

    private suspend fun downloadFile(context: Context, release: RemoteRelease): File {
        val directory = File(context.filesDir, "updates").apply(File::mkdirs)
        val file = File(directory, "VidroX-Remote-${release.tagName}.apk")
        val partFile = File(directory, "${file.name}.part")
        val client = HttpClient(OkHttp)

        try {
            _downloadProgress.value = 0
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            client.prepareGet(release.downloadUrl).execute { response ->
                if (!response.status.isSuccess()) {
                    throw InvalidRemoteUpdate("HTTP ${response.status.value}")
                }
                val total = release.expectedSize.takeIf { it > 0 }
                    ?: response.contentLength()
                    ?: -1L
                response.bodyAsChannel().toInputStream().use { input ->
                    partFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            downloaded += count
                            if (total > 0) {
                                _downloadProgress.value = ((downloaded * 100) / total).toInt()
                            }
                        }
                    }
                }
            }

            if (release.expectedSize > 0 && downloaded != release.expectedSize) {
                throw InvalidRemoteUpdate("файл загружен не полностью")
            }
            release.sha256?.let { expected ->
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (actual != expected) throw InvalidRemoteUpdate("неверная контрольная сумма")
            }
            if (!isInstallableUpdate(context, partFile)) {
                throw InvalidRemoteUpdate("получен неверный APK")
            }

            file.delete()
            if (!partFile.renameTo(file)) throw IOException("could not move update into place")
            _downloadProgress.value = 100
            return file
        } catch (error: Exception) {
            partFile.delete()
            file.delete()
            throw error
        } finally {
            client.close()
        }
    }

    private fun isInstallableUpdate(context: Context, apkFile: File): Boolean {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                apkFile.path,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apkFile.path, 0)
        }
        return info?.packageName == context.packageName
    }

    fun installApk(context: Context, apkFile: File, onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            Toast.makeText(
                context,
                "Разрешите установку из этого источника и нажмите «Установить» снова",
                Toast.LENGTH_LONG,
            ).show()
            onResult(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val committed = runCatching {
                commitInstallSession(context.applicationContext, apkFile)
            }.onFailure { Log.w(UPDATE_LOG_TAG, "install session failed", it) }.isSuccess
            withContext(Dispatchers.Main) {
                if (!committed) {
                    Toast.makeText(context, "Не удалось запустить установку", Toast.LENGTH_LONG).show()
                }
                onResult(committed)
            }
        }
    }

    private fun commitInstallSession(context: Context, apkFile: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apkFile.length())
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                session.openWrite("VidroX-Remote.apk", 0, apkFile.length()).use { output ->
                    apkFile.inputStream().use { input -> input.copyTo(output) }
                    session.fsync(output)
                }
                val statusIntent = Intent(context, RemoteUpdateInstallReceiver::class.java)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val statusReceiver = PendingIntent.getBroadcast(context, sessionId, statusIntent, flags)
                session.commit(statusReceiver.intentSender)
            }
        } catch (error: Exception) {
            installer.abandonSession(sessionId)
            throw error
        }
    }
}
