package com.ycngmn.notubetv.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Debug
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient
import android.net.http.SslError
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.ycngmn.notubetv.R
import com.ycngmn.notubetv.ui.YoutubeVM
import com.ycngmn.notubetv.ui.components.UpdateAppScreen
import com.ycngmn.notubetv.utils.ExitBridge
import com.ycngmn.notubetv.utils.NetworkBridge
import com.ycngmn.notubetv.utils.fetchScripts
import com.ycngmn.notubetv.utils.getUpdate
import com.ycngmn.notubetv.utils.permHandler
import com.ycngmn.notubetv.utils.readRaw
import kotlin.math.roundToInt

private const val DISABLE_AUTO_UPDATE_WHEN_DEBUGGER_ATTACHED = true
private const val ALLOW_SSL_ERRORS_WHEN_DEBUGGER_ATTACHED = true
private const val WEBVIEW_DEBUG_TAG = "YoutubeWV"

@Composable
fun YoutubeWV(youtubeVM: YoutubeVM = viewModel()) {

    val context = LocalContext.current
    val activity = context as Activity
    val isTvDevice = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val lifecycleOwner = LocalLifecycleOwner.current

    val state = rememberWebViewState("https://www.youtube.com/tv")
    val navigator = rememberWebViewNavigator()

    val jsScript = youtubeVM.scriptData
    val updateData = youtubeVM.updateData

    val loadingState = state.loadingState
    val exitTrigger = remember { mutableStateOf(false) }

    fun checkForUpdate() {
        val isDebuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
        val shouldCheckForUpdate =
            !DISABLE_AUTO_UPDATE_WHEN_DEBUGGER_ATTACHED || !isDebuggerAttached

        if (shouldCheckForUpdate) {
            getUpdate(context, navigator) { update ->
                if (update != null) youtubeVM.setUpdate(update)
            }
        }
    }

    // Translate native back-presses to 'escape' button press
    BackHandler {
        if (state.loadingState is LoadingState.Finished)
            navigator.evaluateJavaScript(readRaw(context, R.raw.back_bridge))
        else exitTrigger.value = true
    }

    // Fetch scripts and updates at launch
    LaunchedEffect(Unit) {
        youtubeVM.setScript(fetchScripts(context))
        checkForUpdate()
    }

    DisposableEffect(lifecycleOwner, context, navigator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkForUpdate()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (loadingState == LoadingState.Finished && jsScript != null)
        navigator.evaluateJavaScript(jsScript)
    // Auto-update immediately when a newer GitHub release is detected.
    if (updateData != null) UpdateAppScreen(updateData.tagName, updateData.downloadUrl)
    // If exit button is pressed, 'finish the activity' aka 'exit the app'.
    if (exitTrigger.value) activity.finish()

    // This is the loading screen
    val loading = state.loadingState as? LoadingState.Loading
    if (loading != null) SplashLoading(loading.progress)

    WebView(
        modifier = Modifier.fillMaxSize(),
        state = state,
        navigator = navigator,
        platformWebViewParams = permHandler(context),
        captureBackPresses = false,
        onCreated = { webView ->

            (activity.window).setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )

            // Set up cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            cookieManager.flush()

            state.webSettings.apply {
                // This user agent provides native like experience.
                // "PS4" for 4K. "Wired" for previews.
                customUserAgentString = "Mozilla/5.0 Cobalt/25 (Sony, PS4, Wired)"
                isJavaScriptEnabled = true

                androidWebSettings.apply {
                    //isDebugInspectorInfoEnabled = true
                    useWideViewPort = true
                    domStorageEnabled = true
                    hideDefaultVideoPoster = true
                    mediaPlaybackRequiresUserGesture = false
                }
            }

            webView.apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: android.webkit.WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString()
                        if (!url.isNullOrBlank()) {
                            Log.d(WEBVIEW_DEBUG_TAG, "request ${request?.method ?: "GET"} $url")
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onReceivedError(
                        view: android.webkit.WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e(
                            WEBVIEW_DEBUG_TAG,
                            "load error url=${request?.url} code=${error?.errorCode} description=${error?.description}"
                        )
                        super.onReceivedError(view, request, error)
                    }

                    override fun onReceivedHttpError(
                        view: android.webkit.WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        Log.e(
                            WEBVIEW_DEBUG_TAG,
                            "http error url=${request?.url} status=${errorResponse?.statusCode}"
                        )
                        super.onReceivedHttpError(view, request, errorResponse)
                    }

                    override fun onReceivedSslError(
                        view: android.webkit.WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        val debuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
                        Log.e(
                            WEBVIEW_DEBUG_TAG,
                            "ssl error url=${error?.url} primaryError=${error?.primaryError}"
                        )
                        if (ALLOW_SSL_ERRORS_WHEN_DEBUGGER_ATTACHED && debuggerAttached) {
                            Log.w(
                                WEBVIEW_DEBUG_TAG,
                                "proceeding despite ssl error because debugger is attached"
                            )
                            handler?.proceed()
                            return
                        }

                        handler?.cancel()
                    }
                }

                // TV emulators rely on focused views for D-pad navigation.
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()

                 if (isTvDevice) {
                    var lastTouchY = 0f
                    var lastTouchX = 0f
                    var isDragging = false

                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                lastTouchY = event.y
                                lastTouchX = event.x
                                isDragging = false
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                false
                            }

                            MotionEvent.ACTION_MOVE -> {
                                val deltaY = (lastTouchY - event.y).toInt()
                                val deltaX = (lastTouchX - event.x).toInt()
                                if (kotlin.math.abs(deltaY) > 8 || kotlin.math.abs(deltaX) > 8) {
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

                // Bridges the exit button click on the website to handle it natively.
                addJavascriptInterface(ExitBridge(exitTrigger), "ExitBridge")

                /*
                Youtube's content security policy doesn't allow calling fetch on
                3rd party websites (eg. SponsorBlock api). This bridge counters that
                handling the requests on the native side. */
                addJavascriptInterface(NetworkBridge(navigator), "NetworkBridge")

                // Enables hardware acceleration
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                if (isTvDevice) {
                    // Keep existing TV rendering behavior.
                    setInitialScale(25)
                } else {
                    // Fit the spoofed 3840px viewport into the actual screen width.
                    val screenWidthPx = resources.displayMetrics.widthPixels.coerceAtLeast(1)
                    val fitScale = ((screenWidthPx / 3840f) * 100f).roundToInt().coerceIn(10, 100)
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    setInitialScale(fitScale)
                    // Disable over-scroll glow so touch scrolling feels native
                    overScrollMode = View.OVER_SCROLL_NEVER
                }

                // Hide scrollbars
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = true
            }
        }
    )
}