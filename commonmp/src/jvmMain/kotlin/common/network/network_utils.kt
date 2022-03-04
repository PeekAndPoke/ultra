package de.peekandpoke.ultra.common.network

import de.peekandpoke.ultra.common.md5
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.time.Instant

object NetworkUtils {

    fun getHostNameOrDefault(default: String = "unknown") = try {
        InetAddress.getLocalHost()?.hostName ?: default
    } catch (e: UnknownHostException) {
        e.printStackTrace()
        default
    }

    fun getNetworkFingerPrint() = try {

        NetworkInterface.getNetworkInterfaces().toList()
            .filterNotNull()
            .flatMap { iface ->
                listOf(
                    iface.name
                ).plus(
                    iface.inetAddresses.toList().mapNotNull { it.hostAddress }
                )
            }
            .joinToString("")
            .md5()
    } catch (e: SocketException) {
        Instant.now().toString().md5()
    }
}
