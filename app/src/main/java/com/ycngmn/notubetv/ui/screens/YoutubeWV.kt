package com.ycngmn.notubetv.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.ycngmn.notubetv.R
import com.ycngmn.notubetv.ui.YoutubeVM
import com.ycngmn.notubetv.ui.components.UpdateDialog
import com.ycngmn.notubetv.utils.ExitBridge
import com.ycngmn.notubetv.utils.NetworkBridge
import com.ycngmn.notubetv.utils.fetchScripts
import com.ycngmn.notubetv.utils.getUpdate
import com.ycngmn.notubetv.utils.permHandler
import com.ycngmn.notubetv.utils.readRaw
import kotlin.math.roundToInt

@Composable
fun YoutubeWV(youtubeVM: YoutubeVM = viewModel()) {

    val context = LocalContext.current
    val activity = context as Activity
    val isTvDevice = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    val state = rememberWebViewState("https://www.youtube.com/tv")
    val navigator = rememberWebViewNavigator()

    val jsScript = youtubeVM.scriptData
    val updateData = youtubeVM.updateData

    val loadingState = state.loadingState
    val exitTrigger = remember { mutableStateOf(false) }

    // Translate native back-presses to 'escape' button press
    BackHandler {
        if (state.loadingState is LoadingState.Finished)
            navigator.evaluateJavaScript(readRaw(context, R.raw.back_bridge))
        else exitTrigger.value = true
    }

    // Fetch scripts and updates at launch
    LaunchedEffect(Unit) {
        youtubeVM.setScript(fetchScripts(context))
        getUpdate(context, navigator) { update ->
            if (update != null) youtubeVM.setUpdate(update)
        }
    }

    if (loadingState == LoadingState.Finished && jsScript != null)
        navigator.evaluateJavaScript(jsScript)
    // If any update found, show the dialog.
    if (updateData != null) UpdateDialog(updateData, navigator)
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