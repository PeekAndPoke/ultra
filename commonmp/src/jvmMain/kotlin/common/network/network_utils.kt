package de.peekandpoke.ultra.common.network

import java.net.InetAddress
import java.net.UnknownHostException

object NetworkUtils {
    fun getHostNameOrDefault(default: String = "unknown") = try {
        InetAddress.getLocalHost()?.hostName ?: default
    } catch (e: UnknownHostException) {
        default
    }

    fun getLocalhostIp() = try {
        InetAddress.getLocalHost()?.hostAddress ?: "unknown"
    } catch (E: UnknownHostException) {
        "unknown"
    }
}

