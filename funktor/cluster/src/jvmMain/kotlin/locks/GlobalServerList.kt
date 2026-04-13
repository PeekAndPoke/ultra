package io.peekandpoke.funktor.cluster.locks

import io.peekandpoke.funktor.cluster.locks.domain.ServerBeacon

/** Tracks the list of alive and all known servers in the cluster, updated by [ServerBeaconCleanupWorker]. */
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
