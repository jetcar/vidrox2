package com.jetcar.vidrox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.jetcar.vidrox.ui.UpdateViewModel
import java.io.File


@Composable
fun UpdateAppScreen(tagName: String, downloadUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val viewModel: UpdateViewModel = viewModel()
    val progress = viewModel.downloadProgress.collectAsState()

    val isShowDialog = remember { mutableStateOf(true) }
    val downloadedApk = remember { mutableStateOf<File?>(null) }
    val isDownloading = remember { mutableStateOf(true) }

    LaunchedEffect(downloadUrl, tagName) {
        viewModel.downloadApk(
            context = context,
            url = downloadUrl,
            tagName = tagName,
            onDownloaded = {
                downloadedApk.value = it
                isDownloading.value = false
            },
            onError = {
                isDownloading.value = false
                isShowDialog.value = false
                onDismiss()
            }
        )
    }

    if (isShowDialog.value && isDownloading.value) {
        Dialog(
            onDismissRequest = { }
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(2.dp, Color.White, RoundedCornerShape(10.dp))
            ) {

                Column(
                    modifier = Modifier.background(Color(0XFF201c1c))
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Downloading $tagName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LinearProgressIndicator(
                        progress = { progress.value / 100F },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = Color.Black,
                        color = Color.White
                    )
                }
            }
        }
    } else if (isShowDialog.value && downloadedApk.value != null) {
        Dialog(
            onDismissRequest = {
                isShowDialog.value = false
                onDismiss()
            }
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(2.dp, Color.White, RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.background(Color(0XFF201c1c))
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Download completed for $tagName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        "Install the downloaded update now?",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    Row {
                        YTButton("Cancel") {
                            isShowDialog.value = false
                            onDismiss()
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                            YTButton("Install") {
                                val apkFile = downloadedApk.value ?: run {
                                    isShowDialog.value = false
                                    onDismiss()
                                    return@YTButton
                                }
                                if (viewModel.installApk(context, apkFile)) {
                                    // finishAffinity removes the task from recents cleanly,
                                    // then exitProcess kills all threads immediately so the
                                    // WebView GL renderer cannot race against EGL teardown.
                                    activity.finishAffinity()
                                    kotlin.system.exitProcess(0)
                                }
                            }
                    }
                }
            }
        }
    }
}
