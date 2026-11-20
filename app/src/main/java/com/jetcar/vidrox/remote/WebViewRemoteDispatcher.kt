package com.jetcar.vidrox.remote

import android.view.KeyEvent
import android.webkit.WebView
import com.jetcar.vidrox.remote.core.RemoteAction
import com.jetcar.vidrox.remote.core.RemoteCommand
import com.jetcar.vidrox.remote.core.YouTubeTarget
import com.jetcar.vidrox.ui.screens.dispatchDpadKey
import com.jetcar.vidrox.utils.JavaScriptEvaluator

class WebViewRemoteDispatcher(
    private val webViewProvider: () -> WebView?,
    private val readyProvider: () -> Boolean,
    private val javascriptEvaluator: JavaScriptEvaluator,
    private val backScriptProvider: () -> String,
) {
    fun dispatch(command: RemoteCommand): Boolean {
        if (!readyProvider() || webViewProvider() == null) return false
        when (command.action) {
            RemoteAction.OPEN -> open(command.argument)
            RemoteAction.PLAY_PAUSE -> javascriptEvaluator.evaluate(PLAY_PAUSE_SCRIPT)
            RemoteAction.SEEK_BACK -> javascriptEvaluator.evaluate(seekScript(-10))
            RemoteAction.SEEK_FORWARD -> javascriptEvaluator.evaluate(seekScript(10))
            RemoteAction.BACK -> javascriptEvaluator.evaluate(backScriptProvider())
            RemoteAction.UP -> dispatchKey(KeyEvent.KEYCODE_DPAD_UP)
            RemoteAction.DOWN -> dispatchKey(KeyEvent.KEYCODE_DPAD_DOWN)
            RemoteAction.LEFT -> dispatchKey(KeyEvent.KEYCODE_DPAD_LEFT)
            RemoteAction.RIGHT -> dispatchKey(KeyEvent.KEYCODE_DPAD_RIGHT)
            RemoteAction.SELECT -> dispatchKey(KeyEvent.KEYCODE_DPAD_CENTER)
        }
        return true
    }

    private fun open(target: String) {
        val url = YouTubeTarget.canonicalWatchUrl(target)
        val webView = webViewProvider() ?: return
        webView.post { webView.loadUrl(url) }
    }

    private fun dispatchKey(keyCode: Int) {
        val webView = webViewProvider() ?: return
        webView.post { dispatchDpadKey(webView, keyCode) }
    }

    private fun seekScript(seconds: Int): String = """
        (() => {
          const video = document.querySelector('video');
          if (video) video.currentTime = Math.max(0, video.currentTime + ($seconds));
        })();
    """.trimIndent()

    companion object {
        private val PLAY_PAUSE_SCRIPT = """
            (() => {
              const video = document.querySelector('video');
              if (!video) return;
              if (video.paused) video.play(); else video.pause();
            })();
        """.trimIndent()
    }
}
