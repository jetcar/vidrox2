package com.jetcar.vidrox.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView as AndroidWebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.jetcar.vidrox.R
import com.jetcar.vidrox.ui.YoutubeVM
import com.jetcar.vidrox.ui.components.UpdateDialog
import com.jetcar.vidrox.utils.ExitBridge
import com.jetcar.vidrox.utils.NetworkBridge
import com.jetcar.vidrox.utils.fetchScripts
import com.jetcar.vidrox.utils.getUpdate
import com.jetcar.vidrox.utils.permHandler
import com.jetcar.vidrox.utils.readRaw
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val YOUTUBE_TV_URL = "https://www.youtube.com/tv"
private val NAV_PAD_PADDING = 20.dp
private const val NAV_PAD_AUTO_HIDE_MILLIS = 4000L

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
    // Offer the update first, then continue only after explicit confirmation.
    if (updateData != null) UpdateDialog(updateData) { youtubeVM.clearUpdate() }
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
