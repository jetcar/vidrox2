package com.jetcar.vidrox.utils

import android.util.Log
import android.webkit.JavascriptInterface
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class NetworkBridge(
    private val javascriptEvaluator: JavaScriptEvaluator,
) {
    companion object {
        private const val TAG = "NetworkBridge"
    }

    private val client = HttpClient(OkHttp)

    @JavascriptInterface
    fun fetch(url: String, videoId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "fetch url=$url videoId=$videoId")
                val body = client.get(url).body<String>()
                val filteredBody =
                    if (body.startsWith("[")) filterSponsorBlock(body, videoId)
                    else body
                Log.d(TAG, "fetch success url=$url responseLength=${filteredBody.length}")
                val js = "window.onNetworkBridgeResponse(${JSONObject.quote(filteredBody)});"
                withContext(Dispatchers.Main) { javascriptEvaluator.evaluate(js) }
            } catch (exception: Exception) {
                Log.e(TAG, "fetch failed url=$url", exception)
                withContext(Dispatchers.Main) {
                    javascriptEvaluator.evaluate("window.onNetworkBridgeResponse(null);")
                }
            }
        }
    }

    private fun filterSponsorBlock(body: String, videoId: String): String {
        val json = JSONArray(body)

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            if (item.getString("videoID") == videoId) {
                return item.toString()
            }
        }
        return "{}"
    }
}