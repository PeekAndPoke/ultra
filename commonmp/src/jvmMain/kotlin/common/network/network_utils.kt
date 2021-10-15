package de.peekandpoke.ultra.common.network

import java.net.InetAddress
import java.net.UnknownHostException

object NetworkUtils {
    fun getHostNameOrDefault(default: String = "unknown") = try {
        InetAddress.getLocalHost()?.hostName ?: default
    } catch (e: UnknownHostException) {
        default
    }
}

