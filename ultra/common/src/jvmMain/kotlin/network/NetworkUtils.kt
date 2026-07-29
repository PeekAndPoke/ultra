package io.peekandpoke.ultra.common.network

import io.peekandpoke.ultra.common.md5
import io.peekandpoke.ultra.common.toHex
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException

/**
 * JVM network utility functions for host identification and fingerprinting.
 */
object NetworkUtils {

    /** Stand-in fingerprint for a machine whose network interfaces cannot be read. */
    const val UNKNOWN_FINGERPRINT = "unknown-network"

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
     * Fingerprint of the machine's network hardware, as an MD5 hex string.
     *
     * Built from the hardware (MAC) addresses of the physical, non-loopback interfaces that are up,
     * sorted so the result does not depend on the order the OS reports them in. Interfaces without a
     * hardware address contribute their name instead, so a machine that exposes none still yields
     * something.
     *
     * This is meant to be STABLE across restarts of the same machine — `GlobalServerId` relies on
     * that to reclaim its own locks on boot. It is not stable across hardware changes, and a
     * container that gets a fresh MAC per run will not be recognised as the same server.
     *
     * @return the fingerprint, or [UNKNOWN_FINGERPRINT] when no interface could be read. That value
     *   is a constant on purpose: a per-call one would give a machine a new identity every time and
     *   defeat anything built on top of it.
     */
    fun getNetworkFingerPrint(): String = try {
        val parts = NetworkInterface.getNetworkInterfaces().toList()
            .filterNotNull()
            .filter { iface ->
                // virtual and down interfaces come and go; loopback is identical everywhere
                runCatching { iface.isUp && !iface.isLoopback && !iface.isVirtual }.getOrDefault(false)
            }
            .mapNotNull { iface ->
                // the MAC survives a reboot, an ip address does not
                val hardware = runCatching { iface.hardwareAddress }.getOrNull()

                when {
                    hardware != null && hardware.isNotEmpty() -> hardware.toHex()
                    else -> iface.name
                }
            }
            // the OS makes no promise about enumeration order, so the same box could hash differently
            .sorted()

        when {
            parts.isEmpty() -> UNKNOWN_FINGERPRINT
            else -> parts.joinToString("").md5()
        }
    } catch (e: SocketException) {
        UNKNOWN_FINGERPRINT
    }
}
