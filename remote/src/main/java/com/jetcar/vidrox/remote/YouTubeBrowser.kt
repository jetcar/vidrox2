package com.jetcar.vidrox.remote

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jetcar.vidrox.remote.core.YouTubeTarget

private const val YOUTUBE_HOME = "https://m.youtube.com/"
private const val PAUSE_LOCAL_VIDEO_SCRIPT =
    "document.querySelectorAll('video').forEach(function(video){video.pause();});"

@Composable
@SuppressLint("SetJavaScriptEnabled")
internal fun YouTubeBrowser(
    sharedTarget: String,
    receiverLabel: String?,
    status: String,
    onOpenRemote: () -> Unit,
    onSendVideo: (String, () -> Unit) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf(YOUTUBE_HOME) }
    var canGoBack by remember { mutableStateOf(false) }
    val videoUrl = remember(currentUrl) {
        runCatching { YouTubeTarget.canonicalWatchUrl(currentUrl) }.getOrNull()
    }

    fun updateLocation(view: WebView, url: String?) {
        val target = url.orEmpty()
        if (!Uri.parse(target).isTrustedWebNavigation()) {
            view.stopLoading()
            val fallback = currentUrl.takeIf { Uri.parse(it).isTrustedWebNavigation() } ?: YOUTUBE_HOME
            if (view.url != fallback) view.loadUrl(fallback)
            return
        }
        currentUrl = target
        canGoBack = view.canGoBack()
    }

    LaunchedEffect(sharedTarget, webView) {
        val target = sharedTarget.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        val canonical = runCatching { YouTubeTarget.canonicalWatchUrl(target) }.getOrNull()
        if (canonical != null && webView?.url != canonical) webView?.loadUrl(canonical)
    }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    DisposableEffect(lifecycleOwner, webView) {
        val currentWebView = webView
        if (currentWebView == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> currentWebView.onResume()
                    Lifecycle.Event.ON_PAUSE -> currentWebView.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                currentWebView.onResume()
            } else {
                currentWebView.onPause()
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.setSupportMultipleWindows(false)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                return request.isForMainFrame && !request.url.isTrustedWebNavigation()
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                updateLocation(view, url)
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                updateLocation(view, url)
                            }

                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                updateLocation(view, url)
                            }
                        }
                        webView = this
                        loadUrl(currentUrl)
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                onRelease = { view ->
                    view.stopLoading()
                    view.clearHistory()
                    view.removeAllViews()
                    view.destroy()
                    if (webView === view) webView = null
                },
            )
            BrowserControls(
                receiverLabel = receiverLabel,
                status = status,
                canGoBack = canGoBack,
                canSend = videoUrl != null,
                onBack = { webView?.goBack() },
                onSend = {
                    videoUrl?.let { target ->
                        onSendVideo(target) {
                            webView?.evaluateJavascript(PAUSE_LOCAL_VIDEO_SCRIPT, null)
                        }
                    }
                },
                onOpenRemote = onOpenRemote,
            )
        }
    }
}

private fun Uri.isTrustedWebNavigation(): Boolean {
    if (scheme != "https") return false
    val targetHost = host.orEmpty().lowercase()
    return targetHost == "youtube.com" ||
        targetHost.endsWith(".youtube.com") ||
        targetHost == "google.com" ||
        targetHost.endsWith(".google.com")
}

@Composable
private fun BrowserControls(
    receiverLabel: String?,
    status: String,
    canGoBack: Boolean,
    canSend: Boolean,
    onBack: () -> Unit,
    onSend: () -> Unit,
    onOpenRemote: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = receiverLabel?.let { "$it · $status" } ?: status,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.weight(1f)) {
                    Text("Назад")
                }
                Button(onClick = onSend, enabled = canSend, modifier = Modifier.weight(1.25f)) {
                    Text("На ТВ")
                }
                Button(
                    onClick = onOpenRemote,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text("Пульт")
                }
            }
        }
    }
}
