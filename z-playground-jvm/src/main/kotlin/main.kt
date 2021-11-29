package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.common.network.NetworkUtils

fun main() {

    println("${NetworkUtils.getHostNameOrDefault()}-${NetworkUtils.getNetworkFingerPrint()}")

}
