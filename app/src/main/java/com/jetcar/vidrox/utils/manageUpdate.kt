package com.jetcar.vidrox.utils

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import org.json.JSONArray
import org.json.JSONObject

data class ReleaseData (
    val tagName: String,
    val changelog: String,
    val downloadUrl: String
)

suspend fun fetchUpdate() : ReleaseData {
    val fetchUrl = "https://api.github.com/repos/jetcar/vidrox/releases/latest"
    val client = HttpClient(OkHttp)
    val req = client.get(fetchUrl)
    val res = JSONObject(req.body() as String)

    val commitSHA = Regex("\\b[a-fA-F0-9]{40}\\b")

    val assets = res.getJSONArray("assets")
    val apkUrl = findApkAssetUrl(assets)

    return ReleaseData(
        tagName = res.getString("tag_name"),
        changelog = res.getString("body")
            .substringAfter("</ins>").replace(commitSHA, "").replace(Regex("\\s{2,}"), " "),
        downloadUrl = apkUrl
    )
}


suspend fun getUpdate(
    context: Context,
    javascriptEvaluator: JavaScriptEvaluator,
    callback: (ReleaseData?) -> Unit,
) {
    try {
        val remoteRelease = fetchUpdate()
        val remoteVersion = remoteRelease.tagName.removePrefix("v")

        if (compareVersions(remoteVersion, getLocalVersion(context)) > 0) {
            getSkipVersion(javascriptEvaluator) {
                val skipVersion = it?.removeSurrounding("\"")?.removePrefix("v")
                if (compareVersions(remoteVersion, skipVersion ?: "0") > 0)
                    callback(remoteRelease)
                else callback(null)
            }
        }
        else callback(null)

    } catch (_: Exception) { callback(null) }
}

private fun getLocalVersion(context: Context): String {
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return pInfo.versionName.toString()
}

private fun findApkAssetUrl(assets: JSONArray): String {
    for (index in 0 until assets.length()) {
        val asset = assets.getJSONObject(index)
        val url = asset.optString("browser_download_url")
        if (url.endsWith(".apk", ignoreCase = true)) {
            return url
        }
    }

    if (assets.length() > 0) {
        return assets.getJSONObject(0).getString("browser_download_url")
    }

    throw IllegalStateException("Latest release has no downloadable assets")
}

private fun compareVersions(left: String, right: String): Int {
    val leftParts = left.split('.').mapNotNull { it.toIntOrNull() }
    val rightParts = right.split('.').mapNotNull { it.toIntOrNull() }
    val maxSize = maxOf(leftParts.size, rightParts.size)

    for (i in 0 until maxSize) {
        val l = leftParts.getOrElse(i) { 0 }
        val r = rightParts.getOrElse(i) { 0 }
        if (l != r) return l.compareTo(r)
    }

    return 0
}

fun getSkipVersion(javascriptEvaluator: JavaScriptEvaluator, callback: (String?) -> Unit) {
    javascriptEvaluator.evaluate("configRead('skipVersionName')") {
        callback(it)
    }
}