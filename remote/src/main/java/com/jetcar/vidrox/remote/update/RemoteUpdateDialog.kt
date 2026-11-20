package com.jetcar.vidrox.remote.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlinx.coroutines.delay

private const val AUTO_UPDATE_COUNTDOWN_SECONDS = 10

@Composable
fun RemoteUpdateDialog(release: RemoteRelease, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: RemoteUpdateViewModel = viewModel()
    val progress by viewModel.downloadProgress.collectAsState()
    var countdown by remember(release) { mutableIntStateOf(AUTO_UPDATE_COUNTDOWN_SECONDS) }
    var isDownloading by remember(release) { mutableStateOf(false) }
    var downloadedApk by remember(release) { mutableStateOf<File?>(null) }
    var isInstalling by remember(release) { mutableStateOf(false) }

    LaunchedEffect(release) {
        while (countdown > 0 && !isDownloading) {
            delay(1000)
            countdown--
        }
        if (countdown == 0) isDownloading = true
    }

    LaunchedEffect(isDownloading, release) {
        if (!isDownloading || downloadedApk != null) return@LaunchedEffect
        viewModel.downloadApk(
            context = context,
            release = release,
            onDownloaded = {
                downloadedApk = it
                isDownloading = false
            },
            onError = onDismiss,
        )
    }

    Dialog(onDismissRequest = { if (!isDownloading) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    isDownloading -> {
                        Text("Скачивание ${release.tagName}")
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    downloadedApk != null -> {
                        Text("Обновление ${release.tagName} готово")
                        Text("Android попросит подтвердить установку.")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(onClick = onDismiss) { Text("Позже") }
                            Button(
                                enabled = !isInstalling,
                                onClick = {
                                    val file = downloadedApk ?: return@Button
                                    isInstalling = true
                                    viewModel.installApk(context, file) { committed ->
                                        isInstalling = false
                                        if (committed) onDismiss()
                                    }
                                },
                            ) { Text(if (isInstalling) "Запуск…" else "Установить") }
                        }
                    }
                    else -> {
                        Text("Доступно обновление VidroX Remote ${release.tagName}")
                        Text(release.changelog.take(700))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(onClick = onDismiss) { Text("Позже") }
                            Button(onClick = { isDownloading = true }) {
                                Text("Обновить ($countdown)")
                            }
                        }
                    }
                }
            }
        }
    }
}
