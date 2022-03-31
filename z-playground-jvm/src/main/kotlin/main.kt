package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.common.datetime.DOOMSDAY_TIMESTAMP
import de.peekandpoke.ultra.common.datetime.GENESIS_TIMESTAMP
import de.peekandpoke.ultra.common.network.NetworkUtils
import java.time.Instant
import java.time.ZoneId

fun main() {

    println("${NetworkUtils.getHostNameOrDefault()}-${NetworkUtils.getNetworkFingerPrint()}")

    println(
        Instant.ofEpochMilli(GENESIS_TIMESTAMP)
    )

    println(
        Instant.ofEpochMilli(DOOMSDAY_TIMESTAMP)
    )

    println("=======================================")

    ZoneId.getAvailableZoneIds()
        .sorted()
        .forEach { println("\"$it\",") }

    println("=======================================")
}
