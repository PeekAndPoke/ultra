package io.peekandpoke.funktor.cluster.locks.domain

import io.peekandpoke.funktor.cluster.locks.workers.GlobalLocksCleanupWorker
import io.peekandpoke.funktor.cluster.locks.workers.ServerBeaconUpdateWorker
import io.peekandpoke.ultra.datetime.MpInstant

/**
 * A server beacon that can be stored.
 *
 * Active servers in a cluster can update their beacon using the [ServerBeaconUpdateWorker].
 *
 * The beacons can the e.g. be used to clean up locks still held by servers that are no longer alive.
 * See [GlobalLocksCleanupWorker]
 */
data class ServerBeacon(
    val serverId: String,
    val serverVersion: String = "",
    val lastPing: MpInstant,
)
