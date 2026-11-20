package com.jetcar.vidrox.remote.core

import java.net.URI
import java.net.URLDecoder

object YouTubeTarget {
    private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")
    private val allowedHosts = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
    )

    fun canonicalWatchUrl(input: String): String {
        return "https://www.youtube.com/watch?v=${validatedVideoId(input)}"
    }

    fun tvWatchUrl(input: String): String =
        "https://www.youtube.com/tv#/watch?v=${validatedVideoId(input)}"

    private fun validatedVideoId(input: String): String {
        val value = input.trim()
        val videoId = if (videoIdPattern.matches(value)) value else extractFromUrl(value)
        if (!videoIdPattern.matches(videoId)) throw RemoteProtocolException("invalid_youtube_target")
        return videoId
    }

    private fun extractFromUrl(value: String): String {
        val uri = try {
            URI(value)
        } catch (_: Exception) {
            throw RemoteProtocolException("invalid_youtube_target")
        }
        if (uri.scheme?.lowercase() != "https") throw RemoteProtocolException("invalid_youtube_target")
        val host = uri.host?.lowercase() ?: throw RemoteProtocolException("invalid_youtube_target")
        if (host !in allowedHosts) throw RemoteProtocolException("invalid_youtube_target")

        if (host == "youtu.be") return uri.path.orEmpty().trim('/').substringBefore('/')

        val segments = uri.path.orEmpty().trim('/').split('/').filter(String::isNotBlank)
        if (segments.firstOrNull() in setOf("shorts", "live", "embed")) {
            return segments.getOrNull(1).orEmpty()
        }
        return parseQuery(uri.rawQuery)["v"].orEmpty()
    }

    private fun parseQuery(query: String?): Map<String, String> = query.orEmpty()
        .split('&')
        .mapNotNull { item ->
            if (item.isBlank()) return@mapNotNull null
            val parts = item.split('=', limit = 2)
            @Suppress("DEPRECATION")
            val key = URLDecoder.decode(parts[0], "UTF-8")
            @Suppress("DEPRECATION")
            val value = URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
            key to value
        }
        .toMap()
}
