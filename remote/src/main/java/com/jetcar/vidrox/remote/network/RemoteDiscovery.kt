package com.jetcar.vidrox.remote.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jetcar.vidrox.remote.core.RemoteProtocol
import java.util.ArrayDeque

class RemoteDiscovery(
    context: Context,
    private val onEndpointsChanged: (List<RemoteEndpoint>) -> Unit,
    private val onStatus: (String) -> Unit,
) : AutoCloseable {
    private val nsdManager = context.applicationContext.getSystemService(NsdManager::class.java)
    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val endpoints = linkedMapOf<String, RemoteEndpoint>()
    private val serviceGenerations = mutableMapOf<String, Int>()
    private val resolveLock = Any()
    private val resolveQueue = ArrayDeque<ResolveRequest>()
    private var inFlightResolve: ResolveRequest? = null
    private var retryBlocked = false
    private var discovering = false
    @Volatile private var active = false
    private var multicastLock: WifiManager.MulticastLock? = null

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            discovering = true
            if (!active) {
                runCatching { nsdManager.stopServiceDiscovery(this) }
                return
            }
            postStatus("поиск устройств…")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            discovering = false
            active = false
            releaseMulticastLock()
            postStatus("поиск не запустился ($errorCode)")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "Stop discovery failed: $errorCode")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            discovering = false
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (!active) return
            if (!serviceInfo.serviceType.startsWith("_vidrox._tcp")) return
            scheduleResolve(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            if (!active) return
            synchronized(serviceGenerations) {
                serviceGenerations[serviceInfo.serviceName] =
                    (serviceGenerations[serviceInfo.serviceName] ?: 0) + 1
            }
            synchronized(resolveLock) {
                val iterator = resolveQueue.iterator()
                while (iterator.hasNext()) {
                    if (iterator.next().serviceInfo.serviceName == serviceInfo.serviceName) {
                        iterator.remove()
                    }
                }
            }
            synchronized(endpoints) {
                endpoints.remove(serviceInfo.serviceName)
                publishEndpoints()
            }
        }
    }

    fun start() {
        if (active) return
        active = true
        try {
            multicastLock = wifiManager?.createMulticastLock("VidroXRemoteNsd")?.apply {
                setReferenceCounted(false)
                acquire()
            }
            nsdManager.discoverServices(
                RemoteProtocol.SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener,
            )
        } catch (error: Exception) {
            active = false
            releaseMulticastLock()
            postStatus("поиск не запустился")
            Log.e(TAG, "Unable to start discovery", error)
        }
    }

    private fun scheduleResolve(serviceInfo: NsdServiceInfo) {
        val generation = synchronized(serviceGenerations) {
            serviceGenerations[serviceInfo.serviceName] ?: 0
        }
        synchronized(resolveLock) {
            val duplicateInFlight = inFlightResolve?.let { request ->
                request.serviceInfo.serviceName == serviceInfo.serviceName &&
                    request.generation == generation
            } == true
            val duplicateQueued = resolveQueue.any { request ->
                request.serviceInfo.serviceName == serviceInfo.serviceName &&
                    request.generation == generation
            }
            if (duplicateInFlight || duplicateQueued) {
                return
            }
            resolveQueue.addLast(ResolveRequest(serviceInfo, generation))
        }
        startNextResolve()
    }

    @Suppress("DEPRECATION")
    private fun startNextResolve() {
        val request = synchronized(resolveLock) {
            if (inFlightResolve != null || retryBlocked || !active) return
            resolveQueue.pollFirst()?.also { inFlightResolve = it } ?: return
        }
        try {
            nsdManager.resolveService(request.serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                    val generationIsCurrent = synchronized(serviceGenerations) {
                        (serviceGenerations[serviceInfo.serviceName] ?: 0) == request.generation
                    }
                    val shouldRetry = active && generationIsCurrent &&
                        errorCode == NsdManager.FAILURE_ALREADY_ACTIVE &&
                        request.attempt < MAX_RESOLVE_RETRIES
                    finishResolve(request, shouldRetry)
                }

                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    try {
                        if (!active) return
                        val isCurrent = synchronized(serviceGenerations) {
                            (serviceGenerations[resolved.serviceName] ?: 0) == request.generation
                        }
                        if (!isCurrent) return
                        val host = resolved.host?.hostAddress ?: return
                        synchronized(endpoints) {
                            endpoints[resolved.serviceName] = RemoteEndpoint(
                                serviceName = resolved.serviceName,
                                hostAddress = host,
                                port = resolved.port,
                            )
                            publishEndpoints()
                        }
                        postStatus("VidroX найден")
                    } finally {
                        finishResolve(request, retry = false)
                    }
                }
            })
        } catch (error: Exception) {
            Log.w(TAG, "Resolve start failed for ${request.serviceInfo.serviceName}", error)
            finishResolve(request, retry = false)
        }
    }

    private fun finishResolve(request: ResolveRequest, retry: Boolean) {
        synchronized(resolveLock) {
            if (inFlightResolve == request) inFlightResolve = null
            if (retry && active) {
                resolveQueue.addFirst(request.copy(attempt = request.attempt + 1))
                retryBlocked = true
            }
        }
        if (retry && active) {
            mainHandler.postDelayed(
                {
                    synchronized(resolveLock) { retryBlocked = false }
                    startNextResolve()
                },
                RESOLVE_RETRY_DELAY_MILLIS,
            )
        } else {
            startNextResolve()
        }
    }

    private fun publishEndpoints() {
        val snapshot = endpoints.values.sortedBy(RemoteEndpoint::label)
        mainHandler.post { onEndpointsChanged(snapshot) }
    }

    private fun postStatus(value: String) {
        mainHandler.post { onStatus(value) }
    }

    override fun close() {
        val wasActive = active
        active = false
        if (wasActive || discovering) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (error: IllegalArgumentException) {
                Log.w(TAG, "Discovery listener was not active", error)
            }
        }
        discovering = false
        releaseMulticastLock()
        synchronized(endpoints) {
            endpoints.clear()
            mainHandler.post { onEndpointsChanged(emptyList()) }
        }
        synchronized(serviceGenerations) { serviceGenerations.clear() }
        synchronized(resolveLock) {
            resolveQueue.clear()
            inFlightResolve = null
            retryBlocked = false
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { lock ->
            if (lock.isHeld) runCatching { lock.release() }
        }
        multicastLock = null
    }

    companion object {
        private const val TAG = "VidroXRemoteDiscovery"
        private const val MAX_RESOLVE_RETRIES = 10
        private const val RESOLVE_RETRY_DELAY_MILLIS = 400L
    }

    private data class ResolveRequest(
        val serviceInfo: NsdServiceInfo,
        val generation: Int,
        val attempt: Int = 0,
    )
}
