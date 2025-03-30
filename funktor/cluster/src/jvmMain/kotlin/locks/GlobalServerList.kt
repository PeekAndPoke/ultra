package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.ktorfx.cluster.locks.domain.ServerBeacon

class GlobalServerList {

    private var alive: List<ServerBeacon> = emptyList()
    private var all: List<ServerBeacon> = emptyList()

    internal fun setAlive(servers: List<ServerBeacon>) {
        this.alive = servers
    }

    internal fun setAll(servers: List<ServerBeacon>) {
        this.all = servers
    }

    fun getAlive(): List<ServerBeacon> = alive

    fun getAll(): List<ServerBeacon> = all
}
