package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.common.datetime.DOOMSDAY_TIMESTAMP
import de.peekandpoke.ultra.common.datetime.GENESIS_TIMESTAMP
import de.peekandpoke.ultra.common.network.NetworkUtils
import java.time.Instant

fun main() {

    println("${NetworkUtils.getHostNameOrDefault()}-${NetworkUtils.getNetworkFingerPrint()}")

    println(
        Instant.ofEpochMilli(GENESIS_TIMESTAMP)
    )

    println(
        Instant.ofEpochMilli(DOOMSDAY_TIMESTAMP)
    )
}
