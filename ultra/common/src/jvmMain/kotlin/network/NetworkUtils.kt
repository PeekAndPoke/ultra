package io.peekandpoke.ultra.common.network

import io.peekandpoke.ultra.common.md5
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.time.Instant

/**
 * JVM network utility functions for host identification and fingerprinting.
 */
object NetworkUtils {

    /**
     * Returns the local host name, or [default] if it cannot be determined.
     */
    fun getHostNameOrDefault(default: String = "unknown") = try {
        InetAddress.getLocalHost()?.hostName ?: default
    } catch (e: UnknownHostException) {
        e.printStackTrace()
        default
    }

    /**
     * Returns an MD5 fingerprint derived from all network interface names and addresses.
     *
     * The fingerprint tracks the current network setup, not the installation: it changes whenever an
     * interface or address changes - a new dhcp lease, a container restart, a docker bridge coming
     * up. Do not use it where an identity must survive a restart.
     *
     * Falls back to a time-based - and therefore per-call unique - fingerprint if the network
     * interfaces cannot be read.
     */
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
