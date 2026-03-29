package com.jetcar.vidrox.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Debug
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebView as AndroidWebView
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient
import android.net.http.SslError
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.jetcar.vidrox.R
import com.jetcar.vidrox.ui.YoutubeVM
import com.jetcar.vidrox.ui.components.UpdateAppScreen
import com.jetcar.vidrox.utils.ExitBridge
import com.jetcar.vidrox.utils.NetworkBridge
import com.jetcar.vidrox.utils.fetchScripts
import com.jetcar.vidrox.utils.getUpdate
import com.jetcar.vidrox.utils.permHandler
import com.jetcar.vidrox.utils.readRaw
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DISABLE_AUTO_UPDATE_WHEN_DEBUGGER_ATTACHED = true
private const val ALLOW_SSL_ERRORS_WHEN_DEBUGGER_ATTACHED = true
private const val WEBVIEW_DEBUG_TAG = "YoutubeWV"
private const val YOUTUBE_TV_URL = "https://www.youtube.com/tv"
private const val TV_TOUCH_DRAG_THRESHOLD = 8
private const val TV_INITIAL_SCALE = 25
private const val SPOOFED_VIEWPORT_WIDTH = 3840f
private const val TOUCH_SCALE_MIN = 10
private const val TOUCH_SCALE_MAX = 100
private const val TV_USER_AGENT = "Mozilla/5.0 Cobalt/25 (Sony, PS4, Wired)"
private val NAV_BUTTON_SIZE = 44.dp
private val NAV_PAD_PADDING = 20.dp
private val NAV_BUTTON_COLOR = Color.Black.copy(alpha = 0.22f)
private const val NAV_PAD_AUTO_HIDE_MILLIS = 4000L
private val OFFLINE_PANEL_COLOR = Color(0xCC101010)

@Composable
fun YoutubeWV(youtubeVM: YoutubeVM = viewModel()) {

    val context = LocalContext.current
    val activity = context as Activity
    val isTvDevice = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val lifecycleOwner = LocalLifecycleOwner.current
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
        catch (e: PackageManager.NameNotFoundException) { "" }
    }

    val state = rememberWebViewState(YOUTUBE_TV_URL)
    val navigator = rememberWebViewNavigator()

    val jsScript = youtubeVM.scriptData
    val updateData = youtubeVM.updateData
    val coroutineScope = rememberCoroutineScope()
    val webViewRef = remember { mutableStateOf<AndroidWebView?>(null) }
    val isDirectionPadVisible = remember { mutableStateOf(true) }
    val directionPadResetKey = remember { mutableStateOf(0) }
    val isOffline = remember { mutableStateOf(!hasInternetConnection(context)) }

    val loadingState = state.loadingState
    val exitTrigger = remember { mutableStateOf(false) }

    val checkForUpdate: () -> Unit = {
        if (shouldCheckForUpdate()) {
            coroutineScope.launch {
                getUpdate(context, navigator) { update ->
                    if (update != null) youtubeVM.setUpdate(update)
                }
            }
        }
    }

    val showDirectionPad: () -> Unit = {
        isDirectionPadVisible.value = true
        directionPadResetKey.value += 1
    }

    val refreshContent: () -> Unit = {
        isOffline.value = !hasInternetConnection(context)
        webViewRef.value?.reload()
        checkForUpdate()
        showDirectionPad()
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
        showDirectionPad()
    }

    LaunchedEffect(directionPadResetKey.value) {
        if (!isDirectionPadVisible.value) {
            return@LaunchedEffect
        }

        delay(NAV_PAD_AUTO_HIDE_MILLIS)
        isDirectionPadVisible.value = false
    }

    DisposableEffect(lifecycleOwner, context, navigator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOffline.value = !hasInternetConnection(context)
                checkForUpdate()
                showDirectionPad()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOffline.value = false
            }

            override fun onLost(network: Network) {
                isOffline.value = !hasInternetConnection(context)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                isOffline.value = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }

            override fun onUnavailable() {
                isOffline.value = true
            }
        }

        connectivityManager?.registerDefaultNetworkCallback(callback)
        onDispose {
            connectivityManager?.unregisterNetworkCallback(callback)
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

    Box(modifier = Modifier.fillMaxSize()) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = state,
            navigator = navigator,
            platformWebViewParams = permHandler(context),
            captureBackPresses = false,
            onCreated = { webView ->
                webViewRef.value = webView

                (activity.window).setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )

                configureCookies(webView)
                configureWebSettings(state)

                webView.apply {
                    webViewClient = createLoggingWebViewClient(
                        onPageNavigated = {
                            showDirectionPad()
                        },
                        onMainFrameError = {
                            isOffline.value = !hasInternetConnection(context)
                        }
                    )
                    configureFocusAndTouch(
                        onUserInteraction = showDirectionPad,
                    )

                    addJavascriptInterface(ExitBridge(exitTrigger), "ExitBridge")

                    /*
                    Youtube's content security policy doesn't allow calling fetch on
                    3rd party websites (eg. SponsorBlock api). This bridge counters that
                    handling the requests on the native side. */
                    addJavascriptInterface(NetworkBridge(navigator), "NetworkBridge")

                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    configureScale(isTvDevice)

                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = true
                }
            }
        )

        if (isDirectionPadVisible.value) {
            DirectionPadOverlay(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(NAV_PAD_PADDING),
                onUp = {
                    showDirectionPad()
                    dispatchDpadKey(webViewRef.value, KeyEvent.KEYCODE_DPAD_UP)
                },
                onDown = {
                    showDirectionPad()
                    dispatchDpadKey(webViewRef.value, KeyEvent.KEYCODE_DPAD_DOWN)
                },
                onLeft = {
                    showDirectionPad()
                    dispatchDpadKey(webViewRef.value, KeyEvent.KEYCODE_DPAD_LEFT)
                },
                onRight = {
                    showDirectionPad()
                    dispatchDpadKey(webViewRef.value, KeyEvent.KEYCODE_DPAD_RIGHT)
                },
            )
            Text(
                text = "v$versionName",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(NAV_PAD_PADDING),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
            )
        }

        if (isOffline.value) {
            OfflineOverlay(
                modifier = Modifier.align(Alignment.Center),
                onRefresh = refreshContent,
            )
        }
    }
}

@Composable
private fun OfflineOverlay(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(OFFLINE_PANEL_COLOR)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Offline",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Connection was lost. Check your network and refresh.",
                color = Color.White.copy(alpha = 0.88f),
            )
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text(text = "Refresh")
            }
        }
    }
}

@Composable
private fun DirectionPadOverlay(
    modifier: Modifier = Modifier,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavigationButton(symbol = "^", onClick = onUp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavigationButton(symbol = "<", onClick = onLeft)
            NavigationButton(symbol = ">", onClick = onRight)
        }
        NavigationButton(symbol = "v", onClick = onDown)
    }
}

@Composable
private fun NavigationButton(
    symbol: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(NAV_BUTTON_SIZE)
            .clip(CircleShape)
            .background(NAV_BUTTON_COLOR)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, color = Color.White)
    }
}

private fun shouldCheckForUpdate(): Boolean {
    val isDebuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    return !DISABLE_AUTO_UPDATE_WHEN_DEBUGGER_ATTACHED || !isDebuggerAttached
}

private fun configureCookies(webView: AndroidWebView) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)
    cookieManager.flush()
}

private fun configureWebSettings(state: com.multiplatform.webview.web.WebViewState) {
    state.webSettings.apply {
        customUserAgentString = TV_USER_AGENT
        isJavaScriptEnabled = true

        androidWebSettings.apply {
            useWideViewPort = true
            domStorageEnabled = true
            hideDefaultVideoPoster = true
            mediaPlaybackRequiresUserGesture = false
        }
    }
}

private fun createLoggingWebViewClient(
    onPageNavigated: () -> Unit,
    onMainFrameError: () -> Unit,
): WebViewClient {
    return object : WebViewClient() {
        override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: android.graphics.Bitmap?) {
            onPageNavigated()
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: AndroidWebView?, url: String?) {
            onPageNavigated()
            super.onPageFinished(view, url)
        }

        override fun shouldInterceptRequest(
            view: AndroidWebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url?.toString()
            if (!url.isNullOrBlank()) {
                Log.d(WEBVIEW_DEBUG_TAG, "request ${request?.method ?: "GET"} $url")
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onReceivedError(
            view: AndroidWebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if (request?.isForMainFrame == true) {
                onMainFrameError()
            }
            Log.e(
                WEBVIEW_DEBUG_TAG,
                "load error url=${request?.url} code=${error?.errorCode} description=${error?.description}"
            )
            super.onReceivedError(view, request, error)
        }

        override fun onReceivedHttpError(
            view: AndroidWebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 500) {
                onMainFrameError()
            }
            Log.e(
                WEBVIEW_DEBUG_TAG,
                "http error url=${request?.url} status=${errorResponse?.statusCode}"
            )
            super.onReceivedHttpError(view, request, errorResponse)
        }

        override fun onReceivedSslError(
            view: AndroidWebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            onMainFrameError()
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
}

private fun AndroidWebView.configureFocusAndTouch(onUserInteraction: () -> Unit) {
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

private fun AndroidWebView.configureScale(isTvDevice: Boolean) {
    if (isTvDevice) {
        setInitialScale(TV_INITIAL_SCALE)
        return
    }

    val screenWidthPx = resources.displayMetrics.widthPixels.coerceAtLeast(1)
    val fitScale = ((screenWidthPx / SPOOFED_VIEWPORT_WIDTH) * 100f)
        .roundToInt()
        .coerceIn(TOUCH_SCALE_MIN, TOUCH_SCALE_MAX)
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    setInitialScale(fitScale)
    overScrollMode = View.OVER_SCROLL_NEVER
}

private fun dispatchDpadKey(webView: AndroidWebView?, keyCode: Int) {
    webView ?: return
    webView.requestFocus()
    webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}

private fun hasInternetConnection(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}