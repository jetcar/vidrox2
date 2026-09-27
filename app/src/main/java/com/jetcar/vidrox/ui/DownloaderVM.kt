package com.jetcar.vidrox.ui

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
import com.jetcar.vidrox.utils.UpdateInstallReceiver
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

private const val UPDATE_DIRECTORY = "updates"
private const val UPDATE_LOG_TAG = "VidroXUpdate"

/** A downloaded update that failed verification; [message] is shown to the user. */
class InvalidUpdateException(message: String) : IOException(message)

class UpdateViewModel : ViewModel() {

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()

    fun downloadApk(
        context: Context,
        url: String,
        tagName: String,
        expectedSize: Long,
        sha256: String?,
        onDownloaded: (File) -> Unit,
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apkFile = downloadFile(context, url, tagName, expectedSize, sha256)
                withContext(Dispatchers.Main) {
                    onDownloaded(apkFile)
                }
            } catch (error: Exception) {
                Log.w(UPDATE_LOG_TAG, "update download failed for $url", error)
                val reason = (error as? InvalidUpdateException)?.message ?: "network error"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update download failed: $reason", Toast.LENGTH_LONG).show()
                    onError()
                }
            }
        }
    }

    private suspend fun downloadFile(
        context: Context,
        url: String,
        tagName: String,
        expectedSize: Long,
        sha256: String?,
    ): File {
        val client = HttpClient(OkHttp)
        val updatesDirectory = File(context.filesDir, UPDATE_DIRECTORY).apply {
            mkdirs()
        }
        val file = File(updatesDirectory, "VidroX_$tagName.apk")
        // Stream into a temporary file so a failed or partial download can
        // never be mistaken for a complete APK.
        val partFile = File(updatesDirectory, "${file.name}.part")

        try {
            _downloadProgress.value = 0
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L

            client.prepareGet(url).execute { response ->
                if (!response.status.isSuccess()) {
                    throw InvalidUpdateException("server returned HTTP ${response.status.value}")
                }
                val total = expectedSize.takeIf { it > 0 } ?: response.contentLength() ?: -1L

                response.bodyAsChannel().toInputStream().use { input ->
                    partFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            digest.update(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (total > 0) {
                                _downloadProgress.value = ((downloaded * 100) / total).toInt()
                            }
                        }
                    }
                }
            }

            if (expectedSize > 0 && downloaded != expectedSize) {
                throw InvalidUpdateException("incomplete download ($downloaded of $expectedSize bytes)")
            }
            if (sha256 != null) {
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (actual != sha256) {
                    throw InvalidUpdateException("checksum mismatch")
                }
            }
            if (!isInstallableUpdate(context, partFile)) {
                throw InvalidUpdateException("downloaded file is not a valid VidroX APK")
            }

            file.delete()
            if (!partFile.renameTo(file)) {
                throw IOException("could not move update into place")
            }
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

    // Parse the archive the same way the installer will, so a broken file is
    // reported here instead of as "There was a problem parsing the package".
    private fun isInstallableUpdate(context: Context, apkFile: File): Boolean {
        val packageManager = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(apkFile.path, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(apkFile.path, 0)
        }
        return info?.packageName == context.packageName
    }

    /**
     * Installs [apkFile] through a PackageInstaller session. The APK bytes are
     * written into the session from this process before commit, so nothing
     * depends on the installer reading the file back from this app later.
     * The system's confirmation screen is launched by [UpdateInstallReceiver].
     * [onResult] is true once the session was committed.
     */
    fun installApk(context: Context, apkFile: File, onResult: (Boolean) -> Unit) {
        if (!apkFile.exists()) {
            Toast.makeText(
                context,
                "Downloaded update file is no longer available. Please download the update again.",
                Toast.LENGTH_LONG
            ).show()
            onResult(false)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            Toast.makeText(
                context,
                "Allow installs from this app, then try update again.",
                Toast.LENGTH_LONG
            ).show()
            onResult(false)
            return
        }

        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val committed = try {
                commitInstallSession(appContext, apkFile)
                true
            } catch (error: Exception) {
                Log.w(UPDATE_LOG_TAG, "install session failed", error)
                false
            }
            withContext(Dispatchers.Main) {
                if (!committed) {
                    Toast.makeText(context, "Could not start the update install.", Toast.LENGTH_LONG).show()
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
                session.openWrite("VidroX.apk", 0, apkFile.length()).use { output ->
                    apkFile.inputStream().use { input -> input.copyTo(output) }
                    session.fsync(output)
                }

                val statusIntent = Intent(context, UpdateInstallReceiver::class.java)
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
