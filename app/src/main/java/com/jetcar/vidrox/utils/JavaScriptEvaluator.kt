package com.jetcar.vidrox.utils

import android.webkit.WebView

class JavaScriptEvaluator(
    private val webViewProvider: () -> WebView?,
) {
    fun evaluate(script: String, callback: ((String?) -> Unit)? = null) {
        val webView = webViewProvider() ?: run {
            callback?.invoke(null)
            return
        }

        webView.post {
            webView.evaluateJavascript(script) { result ->
                callback?.invoke(result)
            }
        }
    }
}