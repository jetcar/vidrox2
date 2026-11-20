package com.jetcar.vidrox.remote.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteProtocolTest {
    @Test
    fun `command round trips`() {
        val command = RemoteCommand("123456", RemoteAction.OPEN, "https://youtu.be/dQw4w9WgXcQ")
        assertEquals(command, RemoteProtocol.decode(RemoteProtocol.encode(command)))
    }

    @Test
    fun `control action round trips without argument`() {
        val command = RemoteCommand("654321", RemoteAction.PLAY_PAUSE)
        assertEquals(command, RemoteProtocol.decode(RemoteProtocol.encode(command)))
    }

    @Test
    fun `rejects malformed commands`() {
        listOf(
            "VIDROX/2\t123456\tPLAY_PAUSE\t",
            "VIDROX/1\t12345\tPLAY_PAUSE\t",
            "VIDROX/1\t123456\tUNKNOWN\t",
            "VIDROX/1\t123456\tPLAY_PAUSE\textra",
            "VIDROX/1\t123456\tOPEN\t",
            "VIDROX/1\t123456\tOPEN\thttps://example.com/watch?v=dQw4w9WgXcQ",
        ).forEach { line ->
            assertThrows(RemoteProtocolException::class.java) { RemoteProtocol.decode(line) }
        }
    }

    @Test
    fun `canonicalizes supported youtube targets`() {
        val expected = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals(expected, YouTubeTarget.canonicalWatchUrl("dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeTarget.canonicalWatchUrl("https://youtu.be/dQw4w9WgXcQ?t=2"))
        assertEquals(expected, YouTubeTarget.canonicalWatchUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeTarget.canonicalWatchUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `rejects oversized commands and embedded line breaks`() {
        assertThrows(RemoteProtocolException::class.java) {
            RemoteProtocol.decode("VIDROX/1\t123456\tOPEN\t" + "a".repeat(5000))
        }
        assertThrows(RemoteProtocolException::class.java) {
            RemoteProtocol.decode("VIDROX/1\t123456\tOPEN\tdQw4w9WgXcQ\nPLAY_PAUSE")
        }
    }
}
