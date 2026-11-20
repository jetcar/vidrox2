package com.jetcar.vidrox.remote.update

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import org.json.JSONArray
import org.json.JSONObject

private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/jetcar/vidrox2/releases/latest"

data class RemoteRelease(
    val tagName: String,
    val changelog: String,
    val downloadUrl: String,
    val expectedSize: Long,
    val sha256: String?,
)

suspend fun getRemoteUpdate(context: Context): RemoteRelease? = runCatching {
    val release = fetchRemoteRelease()
    val remoteVersion = release.tagName.removePrefix("v")
    if (compareVersions(remoteVersion, localVersion(context)) > 0) release else null
}.getOrNull()

internal suspend fun fetchRemoteRelease(): RemoteRelease {
    val client = HttpClient(OkHttp)
    return try {
        val response = JSONObject(client.get(LATEST_RELEASE_URL).body<String>())
        val tagName = response.getString("tag_name")
        val asset = findRemoteApkAsset(
            response.getJSONArray("assets"),
            expectedName = remoteAssetName(tagName),
        )
        RemoteRelease(
            tagName = tagName,
            changelog = response.optString("body").ifBlank { "Новая версия VidroX Remote" },
            downloadUrl = asset.getString("browser_download_url"),
            expectedSize = asset.optLong("size", -1L),
            sha256 = asset.optString("digest")
                .takeIf { it.startsWith("sha256:") }
                ?.removePrefix("sha256:")
                ?.lowercase(),
        )
    } finally {
        client.close()
    }
}

internal fun remoteAssetName(tagName: String): String = "VidroX-$tagName.remote.apk"

private fun findRemoteApkAsset(assets: JSONArray, expectedName: String): JSONObject {
    for (index in 0 until assets.length()) {
        val asset = assets.getJSONObject(index)
        if (asset.optString("name").equals(expectedName, ignoreCase = true)) {
            return asset
        }
    }
    throw IllegalStateException("Latest release has no $expectedName asset")
}

internal fun compareVersions(left: String, right: String): Int {
    val leftParts = left.split('.').mapNotNull(String::toIntOrNull)
    val rightParts = right.split('.').mapNotNull(String::toIntOrNull)
    val maxSize = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until maxSize) {
        val result = leftParts.getOrElse(index) { 0 }
            .compareTo(rightParts.getOrElse(index) { 0 })
        if (result != 0) return result
    }
    return 0
}

private fun localVersion(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.toString()
