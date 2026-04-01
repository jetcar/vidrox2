package com.jetcar.vidrox.utils

import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

@Composable
fun rememberWebChromeClient(
    context: Context,
    onProgressChanged: (Float) -> Unit,
): WebChromeClient {

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    return remember(context, onProgressChanged) {
        object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources && !hasPermission(context))
                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                request.grant(request.resources)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged((newProgress.coerceIn(0, 100)) / 100f)
                super.onProgressChanged(view, newProgress)
            }
        }
    }
}

fun hasPermission(context: Context) : Boolean  {
    return ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}