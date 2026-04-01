package com.jetcar.vidrox.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.http.SslError
import android.os.Debug
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DISABLE_AUTO_UPDATE_WHEN_DEBUGGER_ATTACHED = true
private const val ALLOW_SSL_ERRORS_WHEN_DEBUGGER_ATTACHED = true
private const val WEBVIEW_DEBUG_TAG = "YoutubeWV"
private const val TV_TOUCH_DRAG_THRESHOLD = 8
private const val YT_TV_WIDTH = 7000f
private const val YT_TV_HEIGHT = 4000f
private const val TV_USER_AGENT = "Mozilla/5.0 Cobalt/25 (Sony, PS4, Wired)"

internal fun shouldCheckForUpdate(): Boolean {
    val isDebuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    return !DISABLE_AUTO_UPDATE_WHEN_DEBUGGER_ATTACHED || !isDebuggerAttached
}

internal fun configureCookies(webView: WebView) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)
    cookieManager.flush()
}

internal fun configureWebSettings(webView: WebView) {
    webView.settings.apply {
        userAgentString = TV_USER_AGENT
        javaScriptEnabled = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = false
    }
}

internal fun createLoggingWebViewClient(
    onPageNavigated: () -> Unit,
    onMainFrameError: () -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
): WebViewClient {
    return object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            onPageStarted()
            onPageNavigated()
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            onPageFinished()
            onPageNavigated()
            super.onPageFinished(view, url)
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
        ): WebResourceResponse? {
            val url = request?.url?.toString()
            if (!url.isNullOrBlank()) {
                Log.d(WEBVIEW_DEBUG_TAG, "request ${request?.method ?: "GET"} $url")
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            if (request?.isForMainFrame == true) {
                onMainFrameError()
            }
            Log.e(
                WEBVIEW_DEBUG_TAG,
                "load error url=${request?.url} code=${error?.errorCode} description=${error?.description}",
            )
            super.onReceivedError(view, request, error)
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?,
        ) {
            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 500) {
                onMainFrameError()
            }
            Log.e(
                WEBVIEW_DEBUG_TAG,
                "http error url=${request?.url} status=${errorResponse?.statusCode}",
            )
            super.onReceivedHttpError(view, request, errorResponse)
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?,
        ) {
            onMainFrameError()
            val debuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
            Log.e(
                WEBVIEW_DEBUG_TAG,
                "ssl error url=${error?.url} primaryError=${error?.primaryError}",
            )
            if (ALLOW_SSL_ERRORS_WHEN_DEBUGGER_ATTACHED && debuggerAttached) {
                Log.w(
                    WEBVIEW_DEBUG_TAG,
                    "proceeding despite ssl error because debugger is attached",
                )
                handler?.proceed()
                return
            }

            handler?.cancel()
        }
    }
}

internal fun WebView.configureFocusAndTouch(onUserInteraction: () -> Unit) {
    isFocusable = true
    isFocusableInTouchMode = true
    requestFocus()

    setOnKeyListener { _, _, event ->
        if (event?.action == KeyEvent.ACTION_DOWN) {
            onUserInteraction()
        }
        false
    }

    setOnGenericMotionListener { _, event ->
        if (event != null) {
            onUserInteraction()
        }
        false
    }

    var lastTouchY = 0f
    var lastTouchX = 0f
    var isDragging = false

    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onUserInteraction()
                lastTouchY = event.y
                lastTouchX = event.x
                isDragging = false
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }

            MotionEvent.ACTION_MOVE -> {
                onUserInteraction()
                val deltaY = (lastTouchY - event.y).toInt()
                val deltaX = (lastTouchX - event.x).toInt()
                if (abs(deltaY) > TV_TOUCH_DRAG_THRESHOLD || abs(deltaX) > TV_TOUCH_DRAG_THRESHOLD) {
                    isDragging = true
                    scrollBy(deltaX, deltaY)
                    lastTouchY = event.y
                    lastTouchX = event.x
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                isDragging
            }

            else -> false
        }
    }
}

internal fun WebView.configureScale(isTvDevice: Boolean) {
    // YouTube TV renders at 1920×1080.  We compute the exact zoom so
    // the page fills the screen without overflow in either direction.
    //
    // useWideViewPort = true  → let the page use its own wide layout
    // loadWithOverviewMode = false → do NOT auto-zoom (it zooms based on
    //   the viewport meta, not the content, which causes ~2× over-zoom)
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = false

    val screenW = resources.displayMetrics.widthPixels.coerceAtLeast(1)
    val screenH = resources.displayMetrics.heightPixels.coerceAtLeast(1)

    val scaleW = screenW / YT_TV_WIDTH * 100f
    val scaleH = screenH / YT_TV_HEIGHT * 100f
    val fitScale = minOf(scaleW, scaleH).roundToInt().coerceAtLeast(1)

    setInitialScale(fitScale)
    overScrollMode = View.OVER_SCROLL_NEVER
}

internal fun dispatchDpadKey(webView: WebView?, keyCode: Int) {
    webView ?: return
    webView.requestFocus()
    webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}

internal fun hasInternetConnection(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
