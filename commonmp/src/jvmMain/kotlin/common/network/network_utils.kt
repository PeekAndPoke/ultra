package de.peekandpoke.ultra.common.network

import java.net.InetAddress
import java.net.UnknownHostException

fun InetAddress.getHostNameOrDefault(default: String = "unknown") = try {
    InetAddress.getLocalHost()?.hostName ?: default
} catch (e: UnknownHostException) {
    default
}
