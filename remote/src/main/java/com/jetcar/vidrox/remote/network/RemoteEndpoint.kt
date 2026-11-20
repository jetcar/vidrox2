package com.jetcar.vidrox.remote.network

data class RemoteEndpoint(
    val serviceName: String,
    val hostAddress: String,
    val port: Int,
) {
    val label: String get() = serviceName.ifBlank { "VidroX" }
}
