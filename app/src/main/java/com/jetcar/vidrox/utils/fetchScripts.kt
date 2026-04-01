package com.jetcar.vidrox.utils

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import com.jetcar.vidrox.R

const val SCRIPTS_URL = "https://raw.githubusercontent.com/jetcar/vidrox2/refs/heads/main/assets/userscripts.js"
suspend fun fetchScripts(context: Context): String {
    // Try to load from local raw resources first
    try {
        return readRaw(context, R.raw.userscripts)
    } catch (e: Exception) {
        // Fallback to GitHub if local file is not available
        val httpClient = HttpClient(OkHttp)
        while (true) {
            try {
                val response: HttpResponse = httpClient.get(SCRIPTS_URL)
                return response.body()
            } catch (_: Exception) { /* retry */ }
        }
    }
}