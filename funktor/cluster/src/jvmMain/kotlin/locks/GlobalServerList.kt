package io.peekandpoke.funktor.cluster.locks

import io.peekandpoke.funktor.cluster.locks.domain.ServerBeacon
import io.peekandpoke.funktor.cluster.locks.workers.ServerBeaconCleanupWorker

/** Tracks the list of alive and all known servers in the cluster, updated by [ServerBeaconCleanupWorker]. */
class GlobalServerList {

    private var alive: List<ServerBeacon> = emptyList()
    private var all: List<ServerBeacon> = emptyList()

    // Volatile and written after `all`: whoever sees it true also sees the list it was set with.
    @Volatile
    private var populated = false

    /** Whether the beacons were read at least once. Until then, an empty list says nothing about dead servers. */
    val isPopulated: Boolean get() = populated

    internal fun setAlive(servers: List<ServerBeacon>) {
        this.alive = servers
    }

    internal fun setAll(servers: List<ServerBeacon>) {
        this.all = servers
        this.populated = true
    }

    fun getAlive(): List<ServerBeacon> = alive

    fun getAll(): List<ServerBeacon> = all
}
