package com.jetcar.vidrox.remote.network

import android.os.Handler
import android.os.Looper
import com.jetcar.vidrox.remote.core.RemoteCommand
import com.jetcar.vidrox.remote.core.RemoteProtocol
import java.net.InetSocketAddress
import java.net.Socket
import java.io.BufferedReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RemoteCommandClient : AutoCloseable {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun send(endpoint: RemoteEndpoint, command: RemoteCommand, onResult: (Result<Unit>) -> Unit) {
        val encoded = try {
            RemoteProtocol.encode(command)
        } catch (error: Exception) {
            onResult(Result.failure(error))
            return
        }

        executor.execute {
            val result = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(endpoint.hostAddress, endpoint.port), TIMEOUT_MILLIS)
                    socket.soTimeout = TIMEOUT_MILLIS
                    val writer = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
                    writer.write(encoded)
                    writer.newLine()
                    writer.flush()
                    val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                    val response = readLimitedResponse(reader)
                    when {
                        response == RemoteProtocol.OK_RESPONSE -> Unit
                        response.startsWith("ERROR\t") -> {
                            val reason = response.substringAfter('\t')
                            if (!reason.matches(Regex("[a-z0-9_]{1,80}"))) {
                                throw IllegalStateException("invalid_response")
                            }
                            throw IllegalStateException(reason)
                        }
                        else -> throw IllegalStateException("invalid_response")
                    }
                }
            }
            mainHandler.post { onResult(result) }
        }
    }

    override fun close() {
        executor.shutdownNow()
    }

    private fun readLimitedResponse(reader: BufferedReader): String {
        val response = StringBuilder()
        while (true) {
            val next = reader.read()
            if (next == -1) throw IllegalStateException("incomplete_response")
            if (next == '\n'.code) return response.toString()
            if (next == '\r'.code) throw IllegalStateException("invalid_response")
            response.append(next.toChar())
            if (response.length > MAX_RESPONSE_LENGTH) {
                throw IllegalStateException("response_too_long")
            }
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 3500
        private const val MAX_RESPONSE_LENGTH = 96
    }
}
