package io.peekandpoke.funktor.cluster.locks.workers

import io.peekandpoke.funktor.cluster.locks.GlobalServerList
import io.peekandpoke.funktor.cluster.locks.ServerBeaconRepository
import io.peekandpoke.funktor.cluster.workers.StateProvider
import io.peekandpoke.funktor.cluster.workers.Worker
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.vault.value
import kotlin.time.Duration.Companion.seconds

class ServerBeaconCleanupWorker(
    private val serverBeaconRepository: ServerBeaconRepository,
    private val servers: GlobalServerList,
    private val log: Log,
) : Worker {

    override val shouldRun: (lastRun: MpInstant, now: MpInstant) -> Boolean = Worker.Every.seconds(10)

    override suspend fun execute(state: StateProvider) {

        val expireAt = MpInstant.now().minus(180.seconds)

        val current = serverBeaconRepository.list()

        // Split into dead and alive servers
        val (dead, alive) = current.partition { it.value.lastPing < expireAt }

        // Remember the active servers we know
        servers.setAll(current.map { it.value })
        servers.setAlive(alive.map { it.value })

        // Remove all expired servers
        val removed = dead.mapNotNull { beacon ->
            try {
                beacon.also { serverBeaconRepository.remove(it) }
            } catch (_: Throwable) {
                null
            }
        }

        if (removed.isNotEmpty()) {
            log.warning("Removed ${removed.size} Server Beacons that where expired: ${removed.map { it.value.serverId }}")
        }
    }
}
