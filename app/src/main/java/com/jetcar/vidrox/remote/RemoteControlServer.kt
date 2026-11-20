package com.jetcar.vidrox.remote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.jetcar.vidrox.remote.core.RemoteCommand
import com.jetcar.vidrox.remote.core.RemoteProtocol
import com.jetcar.vidrox.remote.core.RemoteProtocolException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class RemoteControlServer(
    context: Context,
    val pairingCode: String,
    private val onCommand: (RemoteCommand) -> Boolean,
    private val onStatus: (String) -> Unit,
) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val nsdManager = applicationContext.getSystemService(NsdManager::class.java)
    private val wifiManager = applicationContext.getSystemService(WifiManager::class.java)
    private val running = AtomicBoolean(false)
    private val acceptExecutor = Executors.newSingleThreadExecutor()
    private val clientExecutor = ThreadPoolExecutor(
        MAX_CLIENTS,
        MAX_CLIENTS,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_CLIENTS),
        ThreadPoolExecutor.AbortPolicy(),
    )
    private val activeClients = ConcurrentHashMap.newKeySet<Socket>()
    private val commandLock = Any()
    private var serverSocket: ServerSocket? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        onStatus("запуск…")
        try {
            multicastLock = wifiManager?.createMulticastLock("VidroXReceiverNsd")?.apply {
                setReferenceCounted(false)
                acquire()
            }
            val socket = ServerSocket(0).also {
                it.reuseAddress = true
                serverSocket = it
            }
            registerService(socket.localPort)
            acceptExecutor.execute { acceptLoop(socket) }
        } catch (error: Exception) {
            running.set(false)
            runCatching { serverSocket?.close() }
            serverSocket = null
            releaseMulticastLock()
            acceptExecutor.shutdownNow()
            clientExecutor.shutdownNow()
            onStatus("ошибка сети")
            Log.e(TAG, "Unable to start remote server", error)
        }
    }

    private fun registerService(port: Int) {
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                if (!running.get()) {
                    runCatching { nsdManager.unregisterService(this) }
                    return
                }
                onStatus("готов")
                Log.i(TAG, "Registered ${serviceInfo.serviceName} on port $port")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onStatus("поиск недоступен")
                Log.e(TAG, "NSD registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD unregistration failed: $errorCode")
            }
        }
        registrationListener = listener
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "VidroX"
            serviceType = RemoteProtocol.SERVICE_TYPE
            setPort(port)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running.get()) {
            try {
                val client = socket.accept()
                activeClients.add(client)
                try {
                    clientExecutor.execute { handleClient(client) }
                } catch (_: RejectedExecutionException) {
                    activeClients.remove(client)
                    client.close()
                }
            } catch (_: SocketException) {
                if (running.get()) onStatus("ошибка соединения")
                return
            } catch (error: IOException) {
                if (running.get()) {
                    Log.w(TAG, "Accept failed", error)
                    onStatus("ошибка соединения")
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { client ->
                client.soTimeout = CLIENT_TIMEOUT_MILLIS
                val response = try {
                    val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                    val line = readLimitedLine(reader)
                    val command = RemoteProtocol.decode(line)
                    if (command.pairingCode != pairingCode) {
                        throw RemoteProtocolException("wrong_pairing_code")
                    }
                    synchronized(commandLock) {
                        if (!running.get()) throw RemoteProtocolException("receiver_stopped")
                        if (!onCommand(command)) {
                            throw RemoteProtocolException("receiver_not_ready")
                        }
                    }
                    RemoteProtocol.OK_RESPONSE
                } catch (error: RemoteProtocolException) {
                    RemoteProtocol.errorResponse(error.message.orEmpty())
                } catch (error: Exception) {
                    Log.w(TAG, "Remote command failed", error)
                    RemoteProtocol.errorResponse("command_failed")
                }
                val writer = client.getOutputStream().bufferedWriter(Charsets.UTF_8)
                writer.write(response)
                writer.newLine()
                writer.flush()
            }
        } catch (error: IOException) {
            if (running.get()) Log.d(TAG, "Client disconnected before response", error)
        } finally {
            activeClients.remove(socket)
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { lock ->
            if (lock.isHeld) {
                runCatching { lock.release() }
            }
        }
        multicastLock = null
    }

    private fun readLimitedLine(reader: BufferedReader): String {
        val result = StringBuilder()
        while (true) {
            val next = reader.read()
            if (next == -1) throw RemoteProtocolException("missing_terminator")
            if (next == '\n'.code) return result.toString()
            result.append(next.toChar())
            if (result.length > MAX_COMMAND_LENGTH) {
                throw RemoteProtocolException("command_too_long")
            }
        }
    }

    override fun close() {
        val wasRunning = running.getAndSet(false)
        synchronized(commandLock) { Unit }
        registrationListener?.let { listener ->
            try {
                nsdManager.unregisterService(listener)
            } catch (error: Exception) {
                Log.w(TAG, "NSD listener was not registered", error)
            }
        }
        registrationListener = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        activeClients.forEach { client -> runCatching { client.close() } }
        activeClients.clear()
        acceptExecutor.shutdownNow()
        clientExecutor.shutdownNow()
        releaseMulticastLock()
        if (wasRunning) onStatus("выключен")
    }

    companion object {
        private const val TAG = "VidroXRemoteServer"
        private const val CLIENT_TIMEOUT_MILLIS = 4000
        private const val MAX_COMMAND_LENGTH = 4096
        private const val MAX_CLIENTS = 4
        private const val MAX_QUEUED_CLIENTS = 4
        private val secureRandom = SecureRandom()

        fun generatePairingCode(): String = "%06d".format(secureRandom.nextInt(1_000_000))
    }
}
